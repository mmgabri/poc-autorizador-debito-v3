Projeto: POC Autorizador Cartão de Débito

Micro-serviço: lancamento-conta

Função: [Mock] Executa as funções de segurança para autorização de compra com cartão

## Valkey

- Endpoint configurado: `lancamento-conta-sync-valkey-dev.3xiorg.ng.0001.use1.cache.amazonaws.com:6379`
- Adapter: `br.com.mmgabri.adapter.valkey.ComandoContaValkeyAdapter`
- Endpoint de teste: `POST /valkey/mensagens`

Exemplo de payload:

```json
{
  "message": "mensagem de teste"
}
```

---

Projeto: POC Autorizador Cartão de Débito

Micro-serviço: conta

Função: recebe comando da fila SQS e encaminha para o serviço `lancamento-conta` via gRPC.

## Fluxo

- `ComandoContaSqsAdapter` lê a mensagem da fila `queue-comando-conta-dev`
- monta `ComandoContaPayload(correlationId, customReturnLancamentoConta, sleepLancamentoConta)`
- chama `ContaService.execute(...)`
- `ContaGrpcClient` envia a requisição para `lancamento-conta`

## Payload esperado na SQS

```json
{
  "headerMessageGrpc": {
    "correlationId": "corr-123"
  },
  "customReturnLancamentoConta": "000",
  "sleepLancamentoConta": 100
}
```

## Configuração gRPC

- `grpc.lancamento-conta.host`
- `grpc.lancamento-conta.port`
- `grpc.lancamento-conta.timeout-millis`
