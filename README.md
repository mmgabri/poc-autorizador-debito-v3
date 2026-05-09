# PoC — Autorizador de Cartão de Débito v3

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=springboot)
![gRPC](https://img.shields.io/badge/gRPC-1.76.0-blue?logo=grpc)
![AWS](https://img.shields.io/badge/AWS-ECS%20%20-yellow?logo=amazonaws)
![Datadog](https://img.shields.io/badge/Observability-Datadog-purple?logo=datadog)
![Terraform](https://img.shields.io/badge/Infra-Terraform-7B42BC?logo=terraform)

Prova de Conceito de um **autorizador de transações de cartão de débito** com foco em **minimizar latência end-to-end** e **maximizar throughput**. A PoC valida estratégias de **paralelismo de chamadas** vs sequencial, padrões de concorrência com **Virtual Threads** (Project Loom) e o padrão **fire-and-wait assíncrono** (SQS + Redis) sob carga real na AWS com até **1.200 TPS**.

---

## Sumário

- [Objetivos](#objetivos)
- [Arquitetura](#arquitetura)
- [Serviços](#serviços)
- [Padrão Async do Ledger](#padrão-async-do-ledger-fire-and-wait)
- [Catálogo de Produtos](#catálogo-de-produtos)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura do Repositório](#estrutura-do-repositório)
- [Como Executar Localmente](#como-executar-localmente)
- [Configuração](#configuração)
- [Contratos gRPC (Protobuf)](#contratos-grpc-protobuf)
- [Endpoints](#endpoints)
- [Simulação e Testes Controlados](#simulação-e-testes-controlados)
- [Testes de Carga (k6)](#testes-de-carga-k6)
- [Observabilidade](#observabilidade)
- [Infraestrutura AWS](#infraestrutura-aws)
- [Decisões de Design](#decisões-de-design)
- [Resultados](#resultados)

---

## Objetivos

| # | Objetivo | Métrica |
|---|---|---|
| 1 | Avaliar **paralelismo vs sequencial** nas chamadas aos microsserviços | Redução de latência total |
| 2 | Validar **Virtual Threads** (Project Loom) sob alta concorrência | TPS sustentado sem degradação de cauda |
| 3 | Validar o padrão **fire-and-wait assíncrono** com SQS + Redis | Overhead do padrão async |
| 4 | Estabelecer baseline de **TPS sustentado** na AWS | Throughput máximo sem degradação |

**KPI principal:** latência end-to-end (p95 e p99).

---

## Arquitetura

```
                        ┌──────────────────────────────────────────────────────────┐
                        │                     AWS VPC                              │
                        │                                                          │
  Cliente               │  ┌─────────────────────────────────────────────────┐    │
  (k6 / REST / gRPC)    │  │             autorizador-debito                  │    │
     │                  │  │          REST :9091  │  gRPC :59091             │    │
     └──── ALB :9090 ───┼──►                      │                          │    │
                        │  └──────────┬───────────┘                          │    │
                        │             │                                       │    │
                        │    ┌────────▼────────────────────────────────┐     │    │
                        │    │         Orquestração de Serviços        │     │    │
                        │    │                                         │     │    │
                        │    │  ┌─────────────────┐  timeout: 1500ms  │     │    │
                        │    │  │ enrichment-svc  │◄──────────────────┤     │    │
                        │    │  └─────────────────┘                   │     │    │
                        │    │  ┌─────────────────┐  timeout: 15000ms │     │    │
                        │    │  │  security-svc   │◄──────────────────┤     │    │
                        │    │  └─────────────────┘                   │     │    │
                        │    │  ┌─────────────────┐  timeout: 15000ms │     │    │
                        │    │  │   rules-svc     │◄──────────────────┤     │    │
                        │    │  └─────────────────┘                   │     │    │
                        │    │  ┌─────────────────┐  timeout: 1500ms  │     │    │
                        │    │  │   limit-svc     │◄──────────────────┤     │    │
                        │    │  └─────────────────┘                   │     │    │
                        │    │  ┌─────────────────┐  timeout: 1500ms  │     │    │
                        │    │  │  antifraud-svc  │◄──────────────────┤     │    │
                        │    │  └─────────────────┘                   │     │    │
                        │    │  ┌─────────────────┐  timeout: 1500ms  │     │    │
                        │    │  │  ledger-svc     │◄──────────────────┘     │    │
                        │    │  └────────┬────────┘                         │    │
                        │    └───────────┼─────────────────────────────────-┘    │
                        │                │                                        │
                        │          1. SQS queue-compensation-transaction          │
                        │                │                                        │
                        │          ┌─────▼──────┐                                │
                        │          │ conta-svc  │  (consome SQS)                 │
                        │          └─────┬──────┘                                │
                        │                │  2. TrataRetornoConta (gRPC)          │
                        │                │  3. Redis pub/sub                     │
                        │                │     channel:{instanceId}:{corrId}     │
                        │          ┌─────▼──────┐                                │
                        │          │  ledger-svc│  (future.complete → retorna)   │
                        │          └────────────┘                                │
                        │                                                        │
                        └────────────────────────────────────────────────────────┘
```

> O diagrama completo está em [`Desenho Arquitetura.drawio`](Desenho%20Arquitetura.drawio).

Todos os serviços se comunicam internamente via **gRPC sobre HTTP/2** dentro da VPC, sem hops externos. O `formatador-bandeiras` atua como gateway de entrada — recebe o request ISO 8583, identifica o produto/bandeira e roteia para o `autorizador-debito`.

---

## Serviços

| Serviço | Função | REST | gRPC |
|---|---|:---:|:---:|
| `autorizador-debito` | Orquestrador principal — executa os serviços conforme o produto | 9091 | 59091 |
| `formatador-bandeiras` | Gateway de entrada — parseia ISO 8583, roteia por bandeira | 9090 | — |
| `enrichment-service` | Enriquecimento de dados da transação (dados do cliente, cartão, conta) | 9092 | 59092 |
| `rules-service` | Validação de regras do portador | 9093 | 59093 |
| `security-service` | Validação de segurança (senha, chip, CVV) | 9094 | 59094 |
| `limit-service` | Verificação de limite de débito | 9095 | 59095 |
| `ledger-service` | Lançamento contábil assíncrono via SQS + Redis | 9096 | 59096 |
| `antifraud-service` | Detecção de fraude | 9097 | 59097 |
| `conta` | Gerenciamento de conta — consome SQS e chama callback no ledger | 9098 | — |

**Service Discovery (AWS):** cada serviço é endereçável via DNS interno:
```
<service-name>-svc.autorizador-debito.local:<grpc-port>
```

---

## Padrão Async do Ledger (fire-and-wait)

O `ledger-service` implementa **request-response assíncrono entre três transportes** para desacoplar o lançamento contábil do caminho crítico de autorização:

```
autorizador-debito
      │
      │  gRPC GerarLancamento(correlationId)
      ▼
 ledger-service
      │
      ├─ 1. Publica JSON na fila SQS queue-compensation-transaction
      │       { correlationId, instanceId, ... }
      │
      ├─ 2. Registra CompletableFuture keyed por correlationId (timeout: 30s)
      │
      └─ 3. Bloqueia: future.get(30, SECONDS)
                          │
                    conta-service
                          │  consome SQS, processa, chama de volta:
                          │  gRPC TrataRetornoConta(instanceId, correlationId, resultado)
                          │
                    ledger-service
                          │  publica no Redis:
                          │  channel:{instanceId}:{correlationId}
                          │
                    Redis Subscriber
                          │  future.complete(resultado)
                          │
                    future.get() retorna ───► resposta gRPC ao autorizador
```

**Por que `instanceId`?** Em deploys multi-container, cada instância tem um UUID único gerado no startup (`AppConfig`). As inscrições Redis são isoladas por instância — sem cross-talk entre réplicas.

---

## Catálogo de Produtos

O `autorizador-debito` carrega `products_config.yml` para determinar quais serviços executar por tipo de transação. Isso permite adicionar novos produtos sem alterar código.

| Produto | Serviços Executados | roteiro Contábil |
|---|---|:---:|
| `COMPRA_NACIONAL_COM_CHIP_SENHA_MASTER` | enrichment → security → rules → limit → ledger → antifraud | 003005901 |
| `COMPRA_NACIONAL_CONTACTLESS_COM_SENHA_MASTER` | enrichment → security → rules → ledger | 003005902 |
| `COMPRA_NACIONAL_CONTACTLESS_SEM_SENHA_MASTER` | enrichment → security → rules → ledger | 003005902 |

> Cada produto define também `configSeguranca` (ex: `["SEN", "CHP", "CVV"]`) — os validadores que o `security-service` deve aplicar.

---

## Stack Tecnológica

| Componente | Tecnologia | Observação |
|---|---|---|
| Linguagem | **Java 25** | Preview features habilitadas |
| Framework | **Spring Boot 3.5.6** | |
| Comunicação interna | **gRPC 1.76.0** (protobuf) | HTTP/2, baixo overhead de serialização |
| Concorrência | **Virtual Threads** (Project Loom) | Todos os serviços — Tomcat + gRPC executor |
| Banco local | **H2** (in-memory) | Simula DynamoDB em desenvolvimento |
| Banco produção | **DynamoDB** (AWS SDK v2) | Idempotência e contexto de transação |
| Mensageria | **AWS SQS** | Fila `queue-compensation-transaction` |
| Cache / Pub-sub | **AWS ElastiCache** (Valkey/Redis) | Callback do ledger por correlationId |
| Observabilidade | **Datadog** (Micrometer + DogStatsD) | Export a cada 5s, percentis habilitados |
| Infraestrutura | **ECS** via Terraform | Módulos separados em `infra/` |
| Testes de carga | **k6** | Rampa até 1.200 TPS |
| Build | **Maven** + Docker multi-stage | Base image: Eclipse Temurin |

---

## Estrutura do Repositório

```
poc-autorizador-debito-v3/
│
├── apps/                              # Código-fonte dos microsserviços
│   ├── autorizador-debito/            # Orquestrador principal
│   │   ├── src/main/proto/            # Contratos .proto (cliente)
│   │   └── src/main/resources/
│   │       ├── application.yml        # Config local (H2, localhost)
│   │       ├── application-prod.yml   # Config AWS (env vars)
│   │       └── products_config.yml    # Catálogo de produtos
│   ├── enrichment-service/            # Enriquecimento de dados
│   ├── rules-service/                 # Regras do portador
│   ├── security-service/              # Segurança (senha, chip, CVV)
│   ├── limit-service/                 # Limite de débito
│   ├── ledger-service/                # Lançamento contábil async (SQS + Redis)
│   ├── antifraud-service/             # Antifraude
│   ├── conta/                         # Gerenciamento de conta (consome SQS)
│   └── formatador-bandeiras/          # Gateway de entrada — ISO 8583 + roteamento
│
├── collections/                       # Coleção Postman
│   └── poc-autorizador-debito-v3.zip
│
├── dashes_datadog/                    # Dashboards JSON do Datadog
│   ├── dash_metris_custom.json        # Dashboard métricas de negócio
│   └── dash_metris_grpc_http.json     # Dashboard métricas de protocolo (gRPC e HTTP)
│
├── infra/
│   └── terraform-ecs/                 # Infraestrutura ECS (produção)
│       ├── modules/alb/               # Application Load Balancer
│       ├── modules/cluster-ecs/       # ECS Cluster e Task Definitions
│       ├── modules/dynamodb/          # Tabelas DynamoDB
│       ├── modules/iam-roles/         # IAM Roles (ECS Task Role)
│       ├── modules/nlb/               # Network Load Balancer (gRPC)
│       ├── modules/security-group/    # Security Groups
│       ├── modules/service-discovery/ # AWS Cloud Map (DNS interno)
│       └── terraform.tfvars           # Variáveis (imagens ECR, região)
│
├── resultado_poc/                     # Relatórios das baterias de teste
│   ├── result_bateria1.docx
│   └── result_bateria2.docx
│
├── stress_test/
│   └── k6/
│       └── stress_test.js             # Bateria principal (rampa até 1.200 TPS)
│
├── Desenho Arquitetura.drawio         # Diagrama de arquitetura editável
└── CLAUDE.md                          # Instruções para Claude Code
```

---

## Como Executar Localmente

### Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java (JDK) | 25 |
| Maven | 3.9+ |
| Docker | 24+ (opcional — Redis local) |
| AWS CLI | 2.x (para modo prod com SQS/DynamoDB) |
| k6 | Qualquer recente (testes de carga) |

### 1. Build

Execute em cada diretório de serviço que deseja compilar:

```bash
# Exemplo: compilar o orquestrador
cd apps/autorizador-debito
mvn clean package -DskipTests
```

Para compilar todos de uma vez (PowerShell, a partir da raiz):

```powershell
Get-ChildItem apps -Directory | ForEach-Object {
    Push-Location $_.FullName
    mvn clean package -DskipTests -q
    Pop-Location
}
```

### 2. Regenerar Stubs gRPC

Necessário após editar arquivos `.proto`:

```bash
mvn protobuf:compile protobuf:compile-custom
```

### 3. Iniciar os Serviços

Cada serviço expõe suas portas REST e gRPC (ver tabela em [Serviços](#serviços)).

```bash
# Exemplo: iniciar o autorizador diretamente
cd apps/autorizador-debito
mvn spring-boot:run

# Ou via JAR buildado (JVM otimizada para alto throughput)
java -server \
     -Xms2g -Xmx4g \
     -XX:+UseZGC \
     -jar target/autorizador-debito-*.jar
```

> **Dica:** inicie os serviços na seguinte ordem para evitar falhas de conexão:
> `enrichment` → `security` → `rules` → `limit` → `antifraud` → `ledger` → `conta` → `autorizador-debito` → `formatador-bandeiras`

### 4. Build da Imagem Docker

```bash
# Cada serviço tem seu próprio Dockerfile multi-stage
docker build -t autorizador-debito:local apps/autorizador-debito/
docker build -t ledger-service:local      apps/ledger-service/
# ... demais serviços
```

---

## Configuração

### Variáveis de ambiente (modo `prod`)

| Variável | Serviço | Descrição |
|---|---|---|
| `REDIS_HOST` | ledger-service, conta | Host do ElastiCache |
| `DD_API_KEY` | todos | Chave da API do Datadog |
| `LOGGING_LEVEL` | todos | Nível de log (ex: `INFO`, `DEBUG`) |
| `DATABASE_MODE_ASYNC` | autorizador-debito | Habilita escrita async no DynamoDB |

### Arquivos de configuração por ambiente

| Arquivo | Ambiente | Detalhes |
|---|---|---|
| `application.yml` | Local | H2 in-memory, Redis `127.0.0.1:6379`, hosts gRPC em `localhost` |
| `application-prod.yml` | AWS | Lê env vars, hosts gRPC via DNS interno (`*.autorizador-debito.local`) |
| `products_config.yml` | Ambos | Catálogo de produtos e serviços por tipo de transação |

### Timeouts por dependência (produção)

| Dependência | Timeout | Justificativa |
|---|:---:|---|
| `enrichment-service` | 1.500 ms | Chamada rápida, dados em cache |
| `security-service` | 15.000 ms | Validação criptográfica (chip EMV) |
| `rules-service` | 15.000 ms | Consulta a regras complexas do portador |
| `limit-service` | 1.500 ms | Consulta de saldo/limite |
| `ledger-service` | 1.500 ms | Enfileiramento rápido no SQS |
| `antifraud-service` | 1.500 ms | Score de fraude em tempo real |

---

## Contratos gRPC (Protobuf)

Os arquivos `.proto` estão duplicados em cada `src/main/proto/` de cada serviço. O header compartilhado carrega os campos de rastreabilidade usados em todos os RPCs:

```protobuf
// comuns/header_message_grpc.proto
message HeaderMessageGrpc {
    string transactionId  = 1;
    string correlationId  = 2;
    string bandeira       = 3;
    // ...
}
```

| Arquivo Proto | RPC | Direção |
|---|---|---|
| `autorizador_debito.proto` | `AutorizarTransacao` | Cliente → autorizador-debito |
| `enrichment_service.proto` | `EnrichData` | autorizador → enrichment |
| `security_service.proto` | `ValidateSecurity` | autorizador → security |
| `rules_service.proto` | `ValidateRules` | autorizador → rules |
| `limit_service.proto` | `CheckLimit` | autorizador → limit |
| `ledger_service.proto` | `GerarLancamento` | autorizador → ledger |
| `antifraud_service.proto` | `CheckAntifraud` | autorizador → antifraud |
| `retorno_conta.proto` | `TrataRetornoConta` | conta → ledger (callback) |

---

## Endpoints

### REST (autorizador-debito)

```
POST /authorization          Autoriza uma transação de débito
GET  /actuator/health        Health check
GET  /actuator/metrics       Métricas Micrometer
GET  /actuator/prometheus    Scrape Prometheus
```

### REST (formatador-bandeiras)

```
POST /authorization          Recebe ISO 8583, formata e roteia para autorizador-debito
```

### gRPC

```protobuf
service AutorizadorService {
    rpc AutorizarTransacao (AutorizadorRequest) returns (AutorizadorResponse);
}
```

---

## Simulação e Testes Controlados

Todos os serviços aceitam campos de controle nos requests para simular cenários de degradação, aprovação/recusa e erros sem dependências externas reais.

### Campos de controle disponíveis

| Campo | Tipo | Efeito |
|---|---|---|
| `sleepDataEnrichment` | `int32` (ms) | Injeta delay artificial no enrichment |
| `sleepSeguranca` | `int32` (ms) | Injeta delay no security |
| `sleepRules` | `int32` (ms) | Injeta delay no rules |
| `sleepLimitSimulacao` / `sleepLimitEfetivacao` | `int32` (ms) | Injeta delay no limit (duas fases) |
| `sleepLedgerSimulacao` / `sleepLedgerEfetivacao` | `int32` (ms) | Injeta delay no ledger (duas fases) |
| `sleepFraude` | `int32` (ms) | Injeta delay no antifraud |
| `customReturn<Servico>` | `string` | Força o código de retorno do serviço |

### Códigos de retorno simulados

| Código | Comportamento |
|:---:|---|
| `"000"` | Aprovado |
| `"999"` | Lança `RuntimeException` → gRPC `INTERNAL` error |
| `"SDO"` | Recusado por saldo insuficiente (`approved=false`, `errorCode=SDO`) |
| `"LIM"` | Recusado por limite excedido (`approved=false`, `errorCode=LIM`) |

### Exemplo de payload

```json
{
  "messageIso": {
    "mti": "0200",
    "002": "5899168602146263",
    "003": "002000",
    "004": "000000010050",
    "022": "051",
    "043": "pao de acucar",
    "049": "986"
  },
  "customReturnDataEnrichment": "000",
  "customReturnSeguranca": "000",
  "customReturnFraude": "000",
  "customReturnLedger": "000",
  "customReturnLimit": "000",
  "customReturnRules": "000",
  "sleepDataEnrichment": 100,
  "sleepSeguranca": 400,
  "sleepLedgerSimulacao": 200,
  "sleepLedgerEfetivacao": 350,
  "sleepLimitSimulacao": 150,
  "sleepLimitEfetivacao": 200,
  "sleepFraude": 300,
  "sleepRules": 50
}
```

> A coleção Postman completa está em [`collections/poc-autorizador-debito-v3.zip`](collections/poc-autorizador-debito-v3.zip).

---

## Testes de Carga (k6)

Os scripts estão em [`stress_test/k6/`](stress_test/k6/).

### Perfil de carga (stress_test.js)

| Fase | TPS | Duração |
|---|:---:|---|
| Warm-up | 50 | 60s |
| Rampa gradual | 50 → 1.200 | ~11min (degraus de 100 TPS) |
| Sustentado | 1.200 | 2min |

```bash
# Execução local
k6 run stress_test/k6/stress_test.js

# Thresholds configurados
# dropped_iterations: count==0   (zero iterações descartadas)
# http_req_failed:    rate<0.01  (< 1% de erros HTTP)
```

### Execução na EC2 (throughput real)

```bash
# 1. Instalar k6 na EC2 (Ubuntu)
sudo apt-get update && sudo apt-get install -y gpg ca-certificates
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /etc/apt/keyrings/k6.gpg
echo "deb [signed-by=/etc/apt/keyrings/k6.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install -y k6

# 2. Ajustes de kernel para alto TPS
ulimit -n 100000
sudo sysctl -w net.ipv4.ip_local_port_range="10240 65535"
sudo sysctl -w net.core.somaxconn=65535
sudo sysctl -w net.core.netdev_max_backlog=250000
sudo sysctl -w net.ipv4.tcp_tw_reuse=1

# 3. Upload do script (PowerShell local)
scp -i keypair.pem stress_test/k6/stress_test.js ubuntu@<ec2-host>:/home/ubuntu/

# 4. Executar
k6 run stress_test.js
```

---

## Observabilidade

### Métricas (Datadog)

Configurado via Micrometer + DogStatsD, exportando a cada **5 segundos** para `us5.datadoghq.com`. Percentis habilitados globalmente (`percentiles-histogram: all: true`).

**Dashboards** em [`dashes_datadog/`](dashes_datadog/):

| Dashboard | Arquivo | Conteúdo |
|---|---|---|
| Métricas de negócio | `dash_metris_custom.json` | TPS, latência p95/p99, taxa de erro por produto |
| Métricas de protocolo | `dash_metris_grpc_http.json` | Latência e throughput por protocolo (gRPC e HTTP) |

**Métricas-chave:**

| Métrica | Tipo | Descrição |
|---|---|---|
| `app_duration_transaction` | Timer | Latência end-to-end por transação |
| `http_server_requests` | Timer | TPS e latência por endpoint REST |
| `grpc_server_calls` | Timer | TPS e latência por RPC gRPC |

### Logs Estruturados

Todos os logs incluem os campos:

```
transactionId | correlationId | latencyMs | statusCode | dependency
```

### Datadog Agent (local — Windows)

```powershell
Start-Service   -Name datadogagent   # Iniciar
Stop-Service    -Name datadogagent   # Parar
Restart-Service -Name datadogagent   # Reiniciar
Get-Service     -Name datadogagent   # Status

# Configuração: C:\ProgramData\Datadog
```

---

## Infraestrutura AWS

Configurações Terraform em [`infra/`](infra/). Dois modos de orquestração disponíveis:

### ECS (produção — `infra/terraform-ecs/`)

| Módulo Terraform | Recurso AWS | Descrição |
|---|---|---|
| `modules/base` | VPC, Subnets, IGW, NAT, VPC Endpoints | Rede base com endpoints para SQS/DynamoDB |
| `modules/dynamodb` | DynamoDB Tables | Idempotência e contexto de transação |
| `modules/cluster-ecs` | ECS Cluster, Task Definitions | Um task definition por microsserviço |
| `modules/alb` | Application Load Balancer | Entrada HTTP/REST (porta 9090) |
| `modules/nlb` | Network Load Balancer | Entrada gRPC (HTTP/2) |
| `modules/iam-roles` | IAM Task Role | Permissões SQS, DynamoDB, ECR |
| `modules/security-group` | Security Groups | Regras de ingress/egress por serviço |
| `modules/service-discovery` | AWS Cloud Map | DNS interno `*.autorizador-debito.local` |
| `modules/cloudwatch` | Log Groups | Um log group por microsserviço |
| `modules/auto-scaling` | ECS Auto Scaling | Scaling por CPU e latência |

### Boas práticas de latência aplicadas

- **VPC Endpoints** para SQS e DynamoDB — elimina hops via NAT Gateway
- **Comunicação intra-VPC** — todos os serviços na mesma VPC, sem tráfego externo
- **Service Discovery DNS** — latência mínima de lookup vs. API calls ao Service Registry
- **NLB para gRPC** — preserva conexões HTTP/2 (ALB termina HTTP/2)
- **Auto Scaling** baseado em latência p99 e CPU

---

## Decisões de Design

| Decisão | Motivação |
|---|---|
| **gRPC preferido** para comunicação interna | Menor overhead de serialização vs JSON, HTTP/2 multiplexing, contratos protobuf tipados |
| **Virtual Threads (Project Loom)** em todos os serviços | Suporte nativo a alto número de conexões bloqueantes sem pool fixo — ideal para o padrão fire-and-wait |
| **Timeouts explícitos por dependência** | Evita que uma dependência lenta (ex: security com 15s) degrade toda a cauda das demais |
| **`products_config.yml`** para catálogo de produtos | Adicionar novos produtos sem recompilação — configuração externalizada |
| **`instanceId` por container** | Redis pub-sub isolado por instância — sem cross-talk em deploys multi-container |
| **Observabilidade por padrão** | Métricas e logs estruturados em toda transação, sem instrumentação manual ad-hoc |
| **H2 local / DynamoDB prod** | Desenvolvimento sem dependências externas; DynamoDB em prod garante consistência e idempotência |
| **VPC Endpoints para SQS/DynamoDB** | Elimina NAT Gateway no caminho crítico — reduz latência e custo |

---

## Resultados

O laudo completo da PoC está em [`resultado_poc/Laudo_POC_Autorizador_Debito.pptx`](resultado_poc/Laudo_POC_Autorizador_Debito.pptx).

---

*Uso interno — PoC não destinada a produção.*
