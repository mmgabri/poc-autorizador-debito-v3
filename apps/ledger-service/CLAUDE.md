# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Debit Card Authorization PoC** (`poc-autorizador-debito-v2`) — a Java microservices monorepo focused on minimizing end-to-end latency and maximizing throughput. The working directory (`lancamento-conta`) is a mock service for account posting during purchase authorization.

All services share: Java 25, Spring Boot 3.5.6, gRPC 1.76.0, Maven, Docker.

## Build & Run Commands

```bash
# Build (from service root)
mvn clean package -DskipTests

# Build and run locally
mvn spring-boot:run

# Regenerate protobuf/gRPC stubs
mvn protobuf:compile protobuf:compile-custom

# Build Docker image
docker build -t lancamento-conta .
```

No tests exist in this service yet. Test infra would require JUnit 5 + TestContainers for Redis/SQS.

## Key Architecture

### Async Request-Response Pattern

The core flow is a **fire-and-wait** pattern over three transports:

1. **Inbound:** gRPC call to `GerarLancamento` arrives with a `correlationId`
2. **Outbound:** Service publishes a command JSON (including `instanceId`) to AWS SQS queue (`queue-comando-conta`)
3. **Register:** A `CompletableFuture` is stored in `CompletableFutureService` (keyed by `correlationId`) with a 30s timeout
4. **Wait:** The gRPC thread blocks on `future.get(responseTimeoutSeconds, SECONDS)`
5. **Callback (gRPC):** The downstream `conta` service calls `TrataRetornoConta` gRPC on this service (using the `instanceId` from step 2 to route back)
6. **Callback (Redis):** `RetornoContaControllerGrpc` → `RetornoContaService` publishes result to Redis channel `channel:{instanceId}:{correlationId}`
7. **Resume:** `RetornoContaRedisSubscriber` (subscribed to `channel:{instanceId}:*`) extracts the `correlationId`, calls `CompletableFutureService.complete()`, and the gRPC call returns

Each container has a unique `instanceId` (a UUID generated at startup in `AppConfig.instanceId()`) so Redis subscriptions are instance-scoped — important in multi-instance deployments.

### Port Layout

| Protocol | Port |
|----------|------|
| REST (health) | 8086 |
| gRPC | 58086 |

### Package Structure (`br.com.mmgabri`)

- `adapters/grpc/server/` — gRPC controller implementations
- `adapters/redis/` — Redis pub-sub publish/subscribe adapters
- `adapters/sqs/` — AWS SQS command publisher
- `services/` — Business logic: `LedgerService`, `RetornoContaService`, `CompletableFutureService` (pending-request registry), `LedgerPendingRequestService` (newer variant of the same registry — both currently coexist)
- `domain/` — Records and POJOs (`ComandoContaRequest`, `ComandoContaResponse`, `RedisCallbackMessage`)
- `config/` — `AppConfig`: beans for gRPC, virtual thread executors, SQS client, Redis listener

### Proto Files

Located in `src/main/proto/`:
- `lancamento_conta.proto` — `GerarLancamento` RPC
- `retorno_conta.proto` — `TrataRetornoConta` RPC
- `comuns/header_message_grpc.proto` — Shared header with `transactionId`, `correlationId`, `bandeira`, etc.

### Testing/Simulation Fields

`ComandoContaRequest` carries two fields for controlled testing:
- `sleepLedger` — inject artificial delay (ms) forwarded to `conta` via SQS
- `customReturnLedger` — controls the gRPC response:
  - `"000"` → `approved=true`
  - `"999"` → throws `RuntimeException` (triggers gRPC `INTERNAL` error)
  - Other codes (e.g. `"SDO"`, `"LIM"`) → set as `errorCode` in response, `approved=false`

### Configuration

- `application.yml` — local dev (Redis: `127.0.0.1:6379`, SQS: `queue-comando-conta`, timeout: 30s)
- `application-prod.yml` — prod (reads `${REDIS_HOST}`, `${LOGGING_LEVEL}` from env)
- `app.async.response-timeout-seconds` — how long `LedgerService` blocks waiting for the Redis callback (default 30s)
- `custom.sleep` — default delay injected into commands (default 1ms)

## Monorepo Context

The broader monorepo at `../../` contains:

| Service | Role |
|---------|------|
| `autorizador` | Main orchestrator — routes calls to all dependencies |
| `lancamento-conta` | This service — mock account posting |
| `conta` | Account data management |
| `seguranca` | Security validation |
| `fraudes` | Fraud detection |
| `limite` / `limite-portador` | Credit/cardholder limit checks |
| `autorizador-call-parallel` | Alternate orchestrator with parallel calls |

Infrastructure configs are in `../../infra/` (ECS + EKS Terraform).

## Virtual Threads

All services use Java 21+ virtual threads (configured in `AppConfig`), both for Tomcat (REST) and gRPC executors. Do not introduce blocking thread-local assumptions — virtual thread scheduling assumes non-blocking behavior.
