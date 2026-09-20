output "idempotency_table_name" {
  value = aws_dynamodb_table.idempotency.name
}

output "transaction_context_table_name" {
  value = aws_dynamodb_table.transaction_context.name
}

output "service_context_table_name" {
  value = aws_dynamodb_table.service_context.name
}

output "comando_conta_table_name" {
  value = aws_dynamodb_table.comando_conta.name
}

output "comando_conta_table_stream_arn" {
  value = aws_dynamodb_table.comando_conta.stream_arn
}
