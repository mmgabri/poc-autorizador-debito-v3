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

variable "enrichment_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço enrichment-service"
  type        = string
}

variable "formatador_bandeiras_ecr_repository" {
  description = "Nome do repositório ECR para o serviço formatador-bandeiras"
  type        = string
}

variable "security_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço security-service"
  type        = string
}

variable "rules_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço rules-service"
  type        = string
}

variable "limit_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limit-service"
  type        = string
}

variable "ledger_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço ledger-service"
  type        = string
}

variable "antifraud_service_ecr_repository" {
  description = "Nome do repositório ECR para o serviço antifraud-service"
  type        = string
}

variable "autorizador_debito_ecr_repository" {
  description = "Nome do repositório ECR para o autorizador-debito"  
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

