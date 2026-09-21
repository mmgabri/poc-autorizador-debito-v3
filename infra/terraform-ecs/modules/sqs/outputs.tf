output "comando_conta_queue_url" {
  value = aws_sqs_queue.ms_ledger_queue.url
}

output "compensation_transaction_queue_url" {
  value = aws_sqs_queue.ms_autorizador_queue.url
}

output "reversal_notification_queue_url" {
  value = aws_sqs_queue.ms_formatador_queue.url
}
