# modules/dynamodb/main.tf

resource "aws_dynamodb_table" "idempotency" {
  name           = "idempotency"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "correlationId"
  attribute {
    name = "correlationId"
    type = "S"  #
  }
}

resource "aws_dynamodb_table" "transaction_context" {
  name           = "transaction_context"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "transactionId"
  attribute {
    name = "transactionId"
    type = "S"  #
  }
}

resource "aws_dynamodb_table" "service_context" {
  name         = "service_context"
  billing_mode = "PAY_PER_REQUEST"
  hash_key  = "transactionId"
  range_key = "serviceName"
  attribute {
    name = "transactionId"
    type = "S"
  }
  attribute {
    name = "serviceName"
    type = "S"
  }
}

