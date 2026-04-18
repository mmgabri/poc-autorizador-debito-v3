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

variable "data_enrichment_ecr_repository" {
  description = "Nome do repositório ECR para o serviço data-enrichment"
  type        = string
}

variable "formatador_ecr_repository" {
  description = "Nome do repositório ECR para o serviço formatador"
  type        = string
}

variable "seguranca_ecr_repository" {
  description = "Nome do repositório ECR para o serviço seguranca"
  type        = string
}

variable "limite_portador_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limite-portador"
  type        = string
}

variable "limite_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limite"
  type        = string
}

variable "lancamento_conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço lancamento-conta"
  type        = string
}

variable "fraudes_ecr_repository" {
  description = "Nome do repositório ECR para o serviço fraudes"
  type        = string
}

variable "autorizador_ecr_repository" {
  description = "Nome do repositório ECR para o autorizador"
  type        = string
}

variable "conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço conta"
  type        = string
}

variable "async_bridge_repository" {
  description = "Nome do repositório ECR para o serviço async-bridge"
  type        = string
}

variable "datadog_api_key" {
  description = "Api key datadog"
  type        = string
}

