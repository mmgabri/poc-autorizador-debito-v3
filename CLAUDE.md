# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

A **Debit Card Authorization PoC** (`poc-autorizador-debito-v2`) — a Java microservices monorepo focused on minimizing end-to-end latency and maximizing throughput. The primary KPI is **low latency**. The PoC compares gRPC vs REST, parallel vs sequential call strategies, and validates concurrency patterns under load on AWS.

All services share: **Java 25, Spring Boot 3.5.6, gRPC 1.76.0, Maven, Docker (multi-stage builds with Eclipse Temurin)**.

## Build & Run Commands

```bash
# Build (from any service directory)
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run

# Regenerate protobuf/gRPC stubs
mvn protobuf:compile protobuf:compile-custom

# Build Docker image
docker build -t <service-name> .
```

**Local startup:** Use the Windows batch scripts in `apps_run/` to start each service. No docker-compose at root level — services run independently.

**Load testing (k6):**
```bash
# From tests/k6/
k6 run grpc_autorizador.js
k6 run test01.js
```

No unit tests exist yet. Adding tests would require JUnit 5 + TestContainers for Redis/SQS.

## Services

| Service | Role | gRPC Port | REST Port |
|---------|------|-----------|-----------|
| `autorizador-poc` | Main orchestrator (primary working service) | 58081 | 8081 |
| `autorizador` | Alternate orchestrator | 58081 | 8081 |
| `autorizador-call-parallel` | Orchestrator using `StructuredTaskScope`/`CompletableFuture` parallel calls | 58081 | 8081 |
| `lancamento-conta` | Async account posting (gRPC → SQS → Redis callback) | 58086 | 8086 |
| `conta` | Account data management (consumes SQS, calls lancamento-conta via gRPC) | 58087 | 8087 |
| `data-enrichment` | Transaction data enrichment mock | 58082 | 8082 |
| `seguranca` | Security/validation mock | 58083 | 8083 |
| `limite-portador` | Cardholder limit check mock | 58084 | 8084 |
| `limite` | Debit limit check mock | 58085 | 8085 |
| `fraudes` | Fraud detection mock (REST/OpenFeign) | — | 8088 |

## Authorization Flow

```
Client → AutorizadorService [POST /api/autorizacoes or gRPC AutorizarTransacao]
  ├─ DataEnrichment (gRPC, 1500ms timeout)
  ├─ Seguranca (gRPC, 15000ms timeout)
  ├─ LimitePortador (gRPC, 15000ms timeout)
  ├─ Limite (gRPC, 1500ms timeout)
  ├─ LancamentoConta (gRPC, 1500ms timeout) ──► async SQS + Redis callback (see below)
  └─ Fraudes (REST via OpenFeign, 100ms connect / 500ms read)
→ Consolidate → Return response
```

Per-dependency timeouts are configured in `application.yml` under `grpc.<dependency>-client.timeout`.

## Async Request-Response Pattern (lancamento-conta ↔ conta)

The `lancamento-conta` service implements a **fire-and-wait** pattern:

1. **Inbound:** gRPC call `GerarLancamento` arrives with `correlationId`
2. **Outbound:** Publishes command JSON (with `instanceId`) to SQS queue `queue-comando-conta`
3. **Register:** Stores a `CompletableFuture` keyed by `correlationId` (30s timeout)
4. **Wait:** gRPC thread blocks on `future.get(responseTimeoutSeconds, SECONDS)`
5. **Callback:** `conta` service calls `TrataRetornoConta` gRPC back on `lancamento-conta` (routing via `instanceId`)
6. **Resume:** Result is published to Redis channel `channel:{instanceId}:{correlationId}`, subscriber completes the future, gRPC call returns

Each container has a unique `instanceId` (UUID generated at startup in `AppConfig`) so Redis subscriptions are instance-scoped — critical in multi-instance deployments.

## Virtual Threads

All services use **Java 21+ virtual threads** for both Tomcat (REST) and gRPC executors (configured in `AppConfig`). Do **not** introduce blocking thread-local assumptions — virtual thread scheduling assumes non-blocking behavior.

## Configuration

- `application.yml` — local dev (H2 in-memory DB, localhost Redis at `127.0.0.1:6379`, localhost gRPC addresses)
- `application-prod.yml` — production (reads env vars: `REDIS_HOST`, `LOGGING_LEVEL`, AWS endpoints, etc.)
- `custom.sleep` — default artificial delay injected per request (for testing degradation scenarios)
- Individual sleep/return-code fields in request objects control per-call behavior during testing

**Testing/simulation return codes** (used across services):
- `"000"` → approved
- `"999"` → throws `RuntimeException` (triggers gRPC `INTERNAL` error)
- Other codes (e.g., `"SDO"`, `"LIM"`) → set as `errorCode` in response, `aprovado=false`

## AWS Infrastructure

- **DynamoDB:** Idempotency keys and transaction context (AWS SDK v2)
- **SQS:** Async command queues (e.g., `queue-comando-conta`)
- **ElastiCache (Valkey/Redis):** Pub-sub callbacks
- **ECS/EKS:** Container orchestration — Terraform configs in `infra/terraform-ecs/` and `infra/terraform-eks/`
- Local dev uses H2 (in-memory) instead of DynamoDB

## Observability

- **Datadog:** `micrometer-registry-datadog` + `java-dogstatsd-client`; dashboards in `dashes_datadog/`
- **Structured logs:** Every log includes `transactionId`, `correlationId`, `latencyMs`, `statusCode`, `dependency`
- **Key metrics:** `app_duration_transaction`, TPS, p95/p99 latency per service
- **Tracing:** Spans per dependency via Micrometer/OpenTelemetry

## Proto Files

Shared proto contracts are in `proto/` at the repo root and also duplicated under each service's `src/main/proto/`. The shared header `comuns/header_message_grpc.proto` carries `transactionId`, `correlationId`, `bandeira`, and other cross-cutting fields used by all RPCs.

## Design Principles

- **gRPC preferred** for internal communication (lower serialization overhead, HTTP/2 multiplexing, protobuf contracts)
- **Explicit per-dependency timeouts** — never a single global timeout
- **Parallel calls where possible** to minimize total latency (see `autorizador-call-parallel`)
- **Observability by default** — metrics and structured logs on every transaction