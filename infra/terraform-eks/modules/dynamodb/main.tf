# modules/dynamodb/main.tf

resource "aws_dynamodb_table" "idempotency" {
  name         = "idempotency"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "correlationId"
  attribute {
    name = "correlationId"
    type = "S" #
  }
}

resource "aws_dynamodb_table" "transaction_context" {
  name         = "transaction_context"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "transactionId"
  attribute {
    name = "transactionId"
    type = "S" #
  }
}

resource "aws_dynamodb_table" "service_context" {
  name         = "service_context"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "transactionId"
  range_key    = "serviceName"
  attribute {
    name = "transactionId"
    type = "S"
  }
  attribute {
    name = "serviceName"
    type = "S"
  }
}

# Rastreia o comando de efetivação assíncrono publicado pelo autorizador-debito
# na fila queue-comando-conta e concluído pelo ledger-service.
resource "aws_dynamodb_table" "comando_conta" {
  name         = "comando_conta"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "correlationId"
  attribute {
    name = "correlationId"
    type = "S"
  }

  # Safety net: se o registro nunca sair de PENDING/TIMEOUT (mensagem perdida,
  # ledger caiu antes de terminar), o TTL apaga sozinho e o Stream alimenta um
  # Pipe (ver modules/ttl-reconciliation-pipe) que encaminha pra uma fila SQS.
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"
}
