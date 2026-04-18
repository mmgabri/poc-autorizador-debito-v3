Projeto: Microsserviço de Listagens / Inspeções

Comunicação: REST · gRPC · SOAP | Java 25 | Clean Architecture + Hexagonal

🎯 Visão geral

Este projeto demonstra um microsserviço Java 25 que apresenta três interfaces de comunicação:

REST (HTTP/JSON)

gRPC (unário, client-streaming, server-streaming, bidirecional)

SOAP (XML)

Ele adota a arquitetura Clean Architecture + Hexagonal, com separação explícita entre domínio, aplicação e infraestrutura. O objetivo é ilustrar, no contexto do artigo, como as novas fronteiras de comunicação entre microsserviços (em especial o gRPC) podem ser exploradas com Java 25, Virtual Threads, banco H2, além dos tradicionais REST e SOAP.

📦 Estrutura de pacotes
br.com.nicolebertolo
├── application
│   ├── service            ← Serviços de aplicação (ex: ListingService)
│   └── usecase            ← Casos de uso
├── domain
│   ├── model              ← Modelos de domínio (records)
│   ├── exception          ← Exceções de negócio
│   └── port
│       ├── inbound        ← Interfaces de entrada (ports)
│       └── outbound       ← Interfaces de saída para infra (ports)
├── infrastructure
│   ├── persistence
│   │   ├── entity         ← JPA Entities
│   │   ├── repository     ← Spring Data / JPA Repositorios
│   │   └── adapter        ← Implementação da porta outbound (adapters)
│   ├── rest
│   │   ├── ListingController.java
│   │   └── advice         ← Handler global de exceções REST
│   ├── grpc
│   │   ├── ListingGrpcService.java
│   │   └── advice         ← Interceptor/AOP para gRPC
│   └── soap
│       ├── ListingSoapEndpoint.java
│       └── config         ← Configuração Spring-WS (WSDL/XSD)
└── shared
├── mapper             ← Mappar entre entity ↔ domain ↔ proto ↔ dto
└── util               ← Conversores, RecordConverter, ProtoConverter

🛠 Tecnologias principais

Java 25 (com Virtual Threads para alta simultaneidade)

Spring Boot (versão compatível com Java 25)

gRPC (Protocol Buffers + HTTP/2)

REST via Spring MVC

SOAP via Spring Web Services (Spring-WS)

Banco em memória H2 + Flyway para migração e seed de dados

Arquitetura Clean / Hexagonal com Ports & Adapters

Virtual Threads (Executors.newVirtualThreadPerTaskExecutor())

Suporte aos quatro modos de operação gRPC: unário, server-streaming, client-streaming, bidi-streaming

✅ Funcionalidades implementadas

GET /api/listings/{id} → REST unário

GET /api/listings?city=&tags= → REST listagem (stream via JSON)

POST /api/listings/inspections → REST para upload de inspeções

Serviço gRPC definido via arquivo .proto com operações: GetListing, StreamListings, UploadInspections, LiveInspection

End-endpoint SOAP espelhando parte da API (GetListing, Listagem, UploadInspections)

Persistência com H2 via JPA + Flyway para criar schema + seed

Controle de exceções globais:

REST: @RestControllerAdvice para padronizar respostas de erro

gRPC: Interceptor/AOP para capturar exceções e converter para StatusRuntimeException

📋 Passo a passo para rodar

Clone o repositório.

Verifique que está usando Java 25 (ou superior) e Maven/Gradle compatível.

No arquivo src/main/resources/application.yml, configure o datasource H2 e Flyway:

spring:
datasource:
url: jdbc:h2:mem:listingdb;DB_CLOSE_DELAY=-1
driver-class-name: org.h2.Driver
username: sa
password:
jpa:
hibernate:
ddl-auto: none
defer-datasource-initialization: true
flyway:
enabled: true
locations: classpath:db/migration


Crie os scripts Flyway em src/main/resources/db/migration/:

V1__create_schema.sql → definindo tabelas property_listings, inspections, images

V2__seed_data.sql → inserindo pelo menos 30 propriedades + inspeções + imagens

Compile e execute:

mvn spring-boot:run


Verifique o console H2 (http://localhost:8080/h2-console
) usando JDBC URL jdbc:h2:mem:listingdb, user sa, sem password.

Acesse endpoints REST e SOAP:

REST: http://localhost:8080/api/listings/{id}

SOAP: http://localhost:8080/ws/listings.wsdl

Use cliente gRPC para testar os métodos definidos no proto (porta padrão 6565 ou configurada)

Explore os logs para entender como são tratadas as threads virtuais, streaming, e as diferentes interfaces.

🎥 Comparativo de comunicação (com base no artigo)

No artigo “Novas Fronteiras na Comunicação entre Microsserviços com gRPC e Java 25” é demonstrado como:

gRPC com Protocol Buffers reduz tamanho de payload e melhora latência.

Virtual Threads no Java 25 permitem lidar com grande número de conexões de forma eficiente.

A arquitetura hexagonal permite trocar facilmente os adaptadores (REST, SOAP, gRPC) mantendo o domínio intacto.

Testes de comparação entre REST, SOAP e gRPC destacam trade-offs entre interoperabilidade, performance e adoção.

🔍 Como o projeto aborda esses aspectos

Payloads grandes: usamos listas de propriedades com muitas imagens; podemos comparar tamanhos entre protobuf, JSON e SOAP.

Quatro modos gRPC:

Unário: GetListing

Server Streaming: StreamListings

Client Streaming: UploadInspections

Bidirecional: LiveInspection

Virtual Threads: tanto no controller REST (ExecutorService de virtual threads) quanto no servidor gRPC.

Arquitetura limpa: domínio, casos de uso, ports/adapters, infra.

Persistência simples com H2 + Flyway seed massivo — facilitando teste de escala.

Interoperabilidade: além de gRPC, temos REST e SOAP co-existindo.

🧪 Testes e métricas sugeridas

Meça o tamanho do payload para uma listagem com muitas imagens em:

JSON REST

Protobuf gRPC

XML SOAP

Meça latência média em cada interface (executando conexões simultâneas, p.ex., 1000 threads virtuais).

Verifique throughput (requests por segundo) em cada interface.

Analise uso de threads, latência por thread e escalabilidade com Virtual Threads no Java 25.

🧭 Evoluções futuras

Adicionar autenticação/mTLS para gRPC.

Balanceamento de carga, observabilidade (OpenTelemetry, Prometheus).

Suporte a múltiplas linguagens clientes para gRPC (Go, Python).

Migração progressiva de REST ou SOAP para gRPC em cenários reais.

Persistência real (PostgreSQL, MongoDB) e testes de comparação entre bancos.

Explorer payloads maiores, compressão, chunking, definição de limites máximos para streaming gRPC.

📝 Licença

MIT License – Sinta-se à vontade para usar, modificar e adaptar o projeto para suas experiências com microsserviços, gRPC e Java 25.