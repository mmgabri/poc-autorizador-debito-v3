variable "name" {
  description = "Nome do Pipe e do role associado."
  type        = string
  default     = "comando-conta-ttl-pipe"
}

variable "source_stream_arn" {
  description = "ARN do DynamoDB Stream da tabela comando_conta."
  type        = string
}

variable "target_queue_arn" {
  description = "ARN da fila SQS que recebe os registros órfãos (queue-transactions-pending)."
  type        = string
}
