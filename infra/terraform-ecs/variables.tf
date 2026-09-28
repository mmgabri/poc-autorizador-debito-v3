variable "region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
}

variable "ecr_repository_names" {
  description = "Nome dos repositórios ECR"
  type        = list(string)
}

variable "micro_services" {
  description = "Micro serviços da autorização de débito"
  type        = list(string)
}

variable "enrichment_ecr_repository" {
  description = "Nome do repositório ECR para o serviço enrichment"
  type        = string
}

variable "message_parser_ecr_repository" {
  description = "Nome do repositório ECR para o serviço message-parser"
  type        = string
}

variable "security_ecr_repository" {
  description = "Nome do repositório ECR para o serviço security"
  type        = string
}

variable "rules_engine_ecr_repository" {
  description = "Nome do repositório ECR para o serviço rules-engine"
  type        = string
}

variable "limit_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limit"
  type        = string
}

variable "account_posting_ecr_repository" {
  description = "Nome do repositório ECR para o serviço account-posting"
  type        = string
}

variable "antifraud_ecr_repository" {
  description = "Nome do repositório ECR para o serviço antifraud"
  type        = string
}

variable "debit_authorizer_ecr_repository" {
  description = "Nome do repositório ECR para o debit-authorizer"
  type        = string
}

variable "conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço conta"
  type        = string
}

variable "datadog_api_key" {
  description = "Api key datadog"
  type        = string
}

variable "logging_level" {
  description = "Nível de log"
  type        = string
}

