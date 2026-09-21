output "dynamodb_tables" {
  description = "Tabelas DynamoDB criadas para testes locais."
  value = [
    module.dynamodb.idempotency_table_name,
    module.dynamodb.transaction_context_table_name,
    module.dynamodb.service_context_table_name,
    module.dynamodb.comando_conta_table_name,
  ]
}

output "sqs_queue_urls" {
  description = "URLs das filas SQS criadas para testes locais."
  value = {
    comando_conta            = module.sqs.comando_conta_queue_url
    compensation_transaction = module.sqs.compensation_transaction_queue_url
    reversal_notification    = module.sqs.reversal_notification_queue_url
  }
}

output "ecr_repository_urls" {
  description = "URLs dos repositórios ECR - usar pra docker tag/push e no terraform.tfvars do terraform-eks."
  value       = module.ecr.repository_urls
}
