# conta

Microsserviço da **PoC Autorizador Cartão de Débito v3**.

Atua como **bridge assíncrona** entre a fila SQS e o serviço `ledger-service`: consome comandos de lançamento, executa a lógica de conta (com latência simulável) e devolve o resultado ao `ledger-service` via gRPC, destravando o fluxo de autorização.

---

## Responsabilidade no fluxo de autorização

```
autorizador-debito
  └─► ledger-service  ──► SQS [queue-comando-conta]
                                        │
                                    conta (este serviço)
                                        │
                          gRPC TrataRetornoConta ──► ledger-service
                                                          │
                                              CompletableFuture.complete()
                                                          │
                                              autorização retorna ao cliente
```

O `ledger-service` publica uma mensagem na fila e aguarda (blocking em virtual thread) o callback gRPC. Este serviço fecha esse ciclo.

---

## Fluxo interno

| Passo | Componente | Ação |
|-------|-----------|------|
| 1 | `ComandoContaSqsAdapter` | Long-poll na fila `queue-comando-conta` com N threads de polling |
| 2 | `ComandoContaSqsAdapter` | Deserializa a mensagem para `ComandoContaRequest`; extrai `instanceId` do atributo SQS |
| 3 | `ContaService` | Aplica sleep configurável (simulação de latência) e monta `RetornoContaRequest` |
| 4 | `LedgerServiceGrpcClient` | Chama `ledger-service.TrataRetornoConta` via gRPC |
| 5 | `ComandoContaSqsAdapter` | Deleta a mensagem da fila após processamento bem-sucedido |

---

## Payload da fila SQS

**Fila:** `queue-comando-conta`

```json
{
  "correlationId": "corr-abc-123",
  "customReturnConta": "000",
  "sleepConta": 100
}
```

| Campo | Descrição |
|-------|-----------|
| `correlationId` | Identificador da transação para correlação com o `ledger-service` |
| `customReturnConta` | Código de retorno simulado (ver tabela abaixo) |
| `sleepConta` | Latência artificial em ms (para testes de degradação) |

O campo `instanceId` é lido dos **atributos da mensagem SQS** (não do body), garantindo roteamento correto em deployments multi-instância.

---

## Códigos de retorno simulados

| Código | Descrição |
|--------|-----------|
| `000` | Aprovado |
| `SDO` | Saldo insuficiente |
| `LIM` | Limite insuficiente |
| `SEN` | Senha inválida |
| `CHP` | Erro na autenticação do chip |
| `CVV` | CVV inválido |
| `CNE` | Cartão inválido |
| `IND` | Sistema indisponível |
| `TIM` | Timeout na aplicação |
| `EIN` / `ERR` | Erro de sistema |

---

## Configuração

### Local (`application.yml`)

```yaml
server:
  port: 9098

grpc:
  ledger-service-client:
    host: localhost
    port: 59096
    timeout: 3600000   # tolerante para debug local

aws:
  sqs:
    queue-name: queue-comando-conta
    poll-threads: 5
    consumer-threads: 50

custom:
  sleep: 1             # ms de latência artificial padrão
```

### Produção (`application-prod.yml`)

```yaml
grpc:
  ledger-service-client:
    host: ledger-service-svc.autorizador-debito.local
    timeout: 5000

aws:
  sqs:
    poll-threads: 20
    consumer-threads: 500
    visibility-timeout-seconds: 120

logging:
  level:
    br.com.mmgabri: ${LOGGING_LEVEL:INFO}
```

**Variáveis de ambiente em produção:**

| Variável | Descrição |
|----------|-----------|
| `DD_API_KEY` | Chave da API do Datadog |
| `LOGGING_LEVEL` | Nível de log (default: `INFO`) |

---

## Portas

| Protocolo | Porta | Uso |
|-----------|-------|-----|
| REST (Actuator) | `9098` | Health, métricas (`/actuator/health`, `/actuator/metrics`) |
| gRPC (cliente) | `59096` | Callback para `ledger-service` |

---

## Concorrência

Utiliza **Java Virtual Threads** em dois pools independentes:

- **Poll executor:** `poll-threads` virtual threads fazem long-polling na fila SQS
- **Message executor:** cada mensagem recebida é despachada para uma nova virtual thread (`newVirtualThreadPerTaskExecutor`)

Isso garante que o bloqueio na chamada gRPC de callback não afete o throughput do polling.

---

## Build e execução

```bash
# Build
mvn clean package -DskipTests

# Execução local
mvn spring-boot:run

# Docker
docker build -t conta .
```

---

## Observabilidade

**Métricas Micrometer → Datadog:**

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `app_conta_msg_received_ledger` | Counter | Mensagens recebidas da fila SQS |
| `app_conta_msg_send_ledger` | Counter | Callbacks gRPC enviados com sucesso para `ledger-service` |
| `app_conta_duration_transaction` | Timer (histogram) | Latência de ponta a ponta do processamento da mensagem |

Percentis p50/p95/p99 habilitados via `percentiles-histogram: all: true`.

**Logs estruturados:**  
Cada mensagem processada registra `messageId`, `correlationId` e resultado.  
Nível `DEBUG` recomendado para desenvolvimento; `INFO` em produção.
