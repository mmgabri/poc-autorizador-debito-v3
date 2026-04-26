# PoC — Autorizador de Cartão de Débito

**Java 25 · Spring Boot 3.5.6 · gRPC 1.76 · AWS (ECS/EKS) · Datadog**

Prova de Conceito de um **autorizador de transações de débito** com foco em **minimizar latência end-to-end** e **maximizar throughput**. A PoC valida comparativos entre gRPC e REST, paralelismo de chamadas e padrões de concorrência sob carga real na AWS.

---

## Objetivos

- Medir e comparar o impacto de **gRPC vs REST** em comunicação interna entre microserviços
- Avaliar estratégias de **paralelismo** (chamadas sequenciais vs paralelas) na latência total
- Validar o padrão **fire-and-wait assíncrono** com SQS + Redis + `CompletableFuture`
- Estabelecer baseline de **TPS, p95 e p99** sob carga sustentada na AWS

**KPI principal:** latência end-to-end (p95/p99).

---

## Arquitetura

```
Cliente
  │
  └─► autorizador-debito  [REST :9091 | gRPC :59091]
           │
           ├─► enrichment-service  (gRPC, timeout 1500ms)
           ├─► security-service    (gRPC, timeout 15000ms)
           ├─► rules-service       (gRPC, timeout 15000ms)
           ├─► limit-service       (gRPC, timeout 1500ms)
           ├─► antifraud-service   (gRPC, timeout 1500ms)
           └─► ledger-service      (gRPC, timeout 1500ms)
                    │
                    │  1. publica SQS queue-compensation-transaction
                    │  2. registra CompletableFuture (30s timeout)
                    │  3. aguarda callback
                    ▼
               conta-service  (consome SQS, chama TrataRetornoConta via gRPC)
                    │
                    └─► Redis pub/sub  channel:{instanceId}:{correlationId}
                              │
                              └─► ledger-service resolve future → retorna gRPC
```

O diagrama completo está em [`Desenho Arquitetura.drawio`](Desenho%20Arquitetura.drawio).

---

## Serviços

| Serviço | Função | REST | gRPC |
|---|---|---|---|
| `autorizador-debito` | Orquestrador principal | 9091 | 59091 |
| `enrichment-service` | Enriquecimento de dados da transação | 9092 | 59092 |
| `rules-service` | Validação de regras do portador | 9093 | 59093 |
| `security-service` | Validação de segurança (senha, chip, CVV) | 9094 | 59094 |
| `limit-service` | Verificação de limite de débito | 9095 | 59095 |
| `ledger-service` | Lançamento contábil (async SQS + Redis) | 9096 | 59096 |
| `antifraud-service` | Detecção de fraude | 9097 | 59097 |
| `conta` | Gerenciamento de conta (consome SQS) | 9098 | — |
| `formatador-bandeiras` | Formatação por bandeira + roteamento | 9090 | — |

---

## Padrão Async do Ledger (fire-and-wait)

O `ledger-service` implementa request-response assíncrono entre três transportes:

1. Recebe gRPC `GerarLancamento` com `correlationId`
2. Publica comando JSON na fila SQS `queue-compensation-transaction` (inclui `instanceId` do container)
3. Registra `CompletableFuture` keyed por `correlationId` com timeout de 30s
4. Thread gRPC bloqueia em `future.get(30, SECONDS)`
5. `conta` consome a fila e chama `TrataRetornoConta` no `ledger-service` roteando pelo `instanceId`
6. `ledger-service` publica resultado no Redis `channel:{instanceId}:{correlationId}`
7. Subscriber Redis completa o future → chamada gRPC retorna

O `instanceId` é um UUID gerado na inicialização de cada container — garante isolamento em deploys multi-instância.

---

## Catálogo de Produtos

O `autorizador-debito` usa `products_config.yml` para determinar quais serviços executar por tipo de transação:

| Produto | Serviços Executados |
|---|---|
| `COMPRA_NACIONAL_COM_CHIP_SENHA` | enrichment → security → rules → limit → ledger → antifraud |
| `COMPRA_NACIONAL_CONTACTLESS_COM_SENHA` | enrichment → security → rules → ledger |
| `COMPRA_NACIONAL_CONTACTLESS_SEM_SENHA` | enrichment → security → rules → ledger |

---

## Stack Tecnológica

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 3.5.6 |
| Comunicação interna | gRPC 1.76.0 (protobuf) |
| Comunicação externa | REST (Spring MVC) |
| Concorrência | Java Virtual Threads (Project Loom) |
| Banco local | H2 (in-memory) |
| Banco produção | DynamoDB (idempotência e contexto) |
| Mensageria | AWS SQS |
| Cache/Pub-sub | AWS ElastiCache (Valkey/Redis) |
| Observabilidade | Datadog (Micrometer + DogStatsD) |
| Infra | ECS e EKS via Terraform |
| Testes de carga | k6 |
| Build | Maven (multi-stage Docker com Eclipse Temurin) |

---

## Como Executar Localmente

### Pré-requisitos

- Java 25
- Maven 3.9+
- Docker (opcional, para Redis local)
- AWS CLI configurado (para SQS/DynamoDB em modo prod)

### Build

```bash
# Em qualquer diretório de serviço (ex: apps/autorizador-debito)
mvn clean package -DskipTests
```

### Iniciar os Serviços

Use os scripts em [`apps_run/`](apps_run/) — cada script inicia o respectivo serviço com JVM tunada:

```bat
apps_run\start_autorizador.bat       # autorizador-debito (4 GB heap)
apps_run\start_lancamento-conta.bat  # ledger-service
apps_run\start_seguranca.bat         # security-service
apps_run\start_limite.bat            # limit-service
apps_run\start_limite-portador.bat   # rules-service
apps_run\data_enrichment.bat         # enrichment-service
apps_run\start_formatador.bat        # formatador-bandeiras
apps_run\start_fraudes.bat           # antifraud-service
```

### Regenerar Stubs gRPC

```bash
mvn protobuf:compile protobuf:compile-custom
```

### Build Docker

```bash
docker build -t autorizador-debito apps/autorizador-debito/
```

---

## Configuração

| Arquivo | Ambiente | Observações |
|---|---|---|
| `application.yml` | Local | H2, Redis em `127.0.0.1:6379`, hosts gRPC em `localhost` |
| `application-prod.yml` | AWS | Lê env vars: `REDIS_HOST`, `LOGGING_LEVEL`, `DD_API_KEY`, `DATABASE_MODE_ASYNC` |
| `products_config.yml` | Todos | Catálogo de produtos e serviços por tipo de transação |

**Timeouts por dependência** (em ms, configurados em `application-prod.yml`):

| Dependência | Timeout |
|---|---|
| enrichment-service | 1500 |
| security-service | 15000 |
| rules-service | 15000 |
| limit-service | 1500 |
| ledger-service | 1500 |
| antifraud-service | 1500 |

---

## Endpoints

### REST

```
POST /authorization          # solicita autorização (principal)
GET  /actuator/health        # health check
GET  /actuator/metrics       # métricas Micrometer
GET  /actuator/prometheus    # scrape Prometheus
```

### gRPC

Os contratos estão em [`proto/`](proto/) e duplicados em `src/main/proto/` de cada serviço.

Header compartilhado (`comuns/header_message_grpc.proto`): `transactionId`, `correlationId`, `bandeira`.

---

## Simulação / Testes Controlados

Todos os serviços aceitam campos de controle nos requests para simular cenários:

| Campo | Efeito |
|---|---|
| `sleep<Servico>` | Injeta delay artificial (ms) na dependência |
| `customReturn<Servico> = "000"` | Retorna aprovado |
| `customReturn<Servico> = "999"` | Lança `RuntimeException` → gRPC `INTERNAL` |
| `customReturn<Servico> = "SDO"/"LIM"/...` | Retorna `approved=false` com `errorCode` |

Exemplo de payload k6:

```json
{
  "messageIso": { "mti": "0200", "002": "5899168602146263", "003": "002000", ... },
  "sleepDataEnrichment": 50,
  "sleepSeguranca": 50,
  "customReturnLancamentoConta": "000"
}
```

---

## Testes de Carga (k6)

Os scripts estão em [`tests/k6/`](tests/k6/).

### Execução local

```bash
k6 run tests/k6/test01.js   # Bateria 1 — rampa até 900 TPS, latência 300ms
k6 run tests/k6/test02.js   # Bateria 2
k6 run tests/k6/grpc_autorizador.js  # gRPC direto
```

### Execução na EC2 (maior throughput)

```bash
# 1. Instalar k6 na EC2 (Ubuntu)
sudo apt-get update && sudo apt-get install -y gpg ca-certificates
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /etc/apt/keyrings/k6.gpg
echo "deb [signed-by=/etc/apt/keyrings/k6.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install -y k6

# 2. Ajustes do kernel para alto TPS
ulimit -n 100000
sudo sysctl -w net.ipv4.ip_local_port_range="10240 65535"
sudo sysctl -w net.core.somaxconn=65535
sudo sysctl -w net.core.netdev_max_backlog=250000
sudo sysctl -w net.ipv4.tcp_tw_reuse=1

# 3. Upload do script (PowerShell local)
scp -i keypair.pem tests/k6/test01.js ubuntu@<ec2-host>:/home/ubuntu/

# 4. Executar
k6 run test01.js
```

**Cenário padrão (test01.js):** rampa de 50 → 900 TPS em ~15min com degraus de 25 TPS, threshold de 0% dropped iterations e < 1% errors.

---

## Observabilidade

### Métricas (Datadog)

- Dashboards em [`dashes_datadog/`](dashes_datadog/): `dash_metris_custom.json` e `dash_metris_grpc_http.json`
- Exportação a cada 5s para `us5.datadoghq.com`
- Percentis habilitados por padrão (`percentiles-histogram: all: true`)

**Métricas-chave:**

| Métrica | Descrição |
|---|---|
| `app_duration_transaction` | Latência end-to-end por transação |
| `http_server_requests` | TPS e latência por endpoint REST |
| `grpc_server_calls` | TPS e latência por RPC gRPC |

### Logs Estruturados

Todos os logs incluem: `transactionId`, `correlationId`, `latencyMs`, `statusCode`, `dependency`.

### Collections REST

Coleção Postman em [`collections/poc-autorizador-debito-rest.postman_collection.json`](collections/poc-autorizador-debito-rest.postman_collection.json).

---

## Infraestrutura AWS

Configurações Terraform em [`infra/`](infra/):

| Diretório | Descrição |
|---|---|
| `infra/terraform-ecs/` | ECS com ALB, Security Groups, SQS, ECS task definitions |
| `infra/terraform-eks/` | EKS (alternativo ao ECS) |

**Serviços AWS utilizados:**

- **ECS / EKS** — orquestração de containers
- **SQS** — fila `queue-compensation-transaction` (async ledger)
- **ElastiCache (Valkey/Redis)** — pub-sub para callback do ledger
- **DynamoDB** — chaves de idempotência e contexto de transação
- **ALB / NLB** — load balancer (HTTP/2 para gRPC via NLB)

**Boas práticas de latência na AWS:**

- Comunicação intra-VPC sem hops externos
- VPC Endpoints para SQS e DynamoDB (evita NAT)
- Auto-scaling baseado em latência e CPU
- Service Discovery via DNS interno (`*.autorizador-debito.local`)

---

## Decisões de Design

| Decisão | Motivação |
|---|---|
| **gRPC preferido** internamente | Menor overhead de serialização, HTTP/2, contratos protobuf |
| **Virtual Threads (Loom)** | Suporte nativo a alto número de conexões bloqueantes sem pool fixo |
| **Timeouts explícitos por dependência** | Evita que uma dependência lenta degrade toda a cauda |
| **Chamadas paralelas onde possível** | Reduz latência total (autorizador-debito pode executar serviços independentes em paralelo) |
| **Observabilidade por padrão** | Métricas e logs estruturados em toda transação, sem instrumentação manual |
| **`instanceId` por container** | Redis pub-sub isolado por instância — essencial em deploys multi-container |

---

## Estrutura do Repositório

```
.
├── apps/
│   ├── autorizador-debito/       # Orquestrador principal
│   ├── enrichment-service/       # Enriquecimento de dados
│   ├── rules-service/            # Regras do portador
│   ├── security-service/         # Segurança (senha, chip, CVV)
│   ├── limit-service/            # Limite de débito
│   ├── ledger-service/           # Lançamento contábil async
│   ├── antifraud-service/        # Antifraude
│   ├── conta/                    # Gerenciamento de conta
│   └── formatador-bandeiras/     # Formatação por bandeira
├── apps_run/                     # Scripts .bat para execução local
├── collections/                  # Coleção Postman
├── dashes_datadog/               # Dashboards JSON do Datadog
├── infra/
│   ├── terraform-ecs/            # Infra ECS (produção)
│   └── terraform-eks/            # Infra EKS (alternativo)
├── proto/                        # Contratos .proto compartilhados
├── results_poc/                  # Resultados das baterias de teste
├── tests/
│   └── k6/                      # Scripts de carga k6
└── Desenho Arquitetura.drawio    # Diagrama de arquitetura
```

---

## Resultados

Os relatórios das baterias de teste estão em [`results_poc/`](results_poc/):

- `result_bateria1.docx` — Bateria 1 (900 TPS, latência 300ms)
- `result_bateria2.docx` — Bateria 2

---

*Uso interno — PoC não destinado a produção.*
