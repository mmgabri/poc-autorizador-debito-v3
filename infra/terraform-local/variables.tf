variable "region" {
  description = "Região AWS onde as tabelas DynamoDB e filas SQS de teste local serão criadas."
  type        = string
  default     = "us-east-1"
}
