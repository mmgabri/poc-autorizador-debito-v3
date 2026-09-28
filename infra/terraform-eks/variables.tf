variable "region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
}

variable "cluster_name" {
  description = "Nome do cluster EKS"
  type        = string
  default     = "autorizador-debito-cluster"
}

variable "cluster_version" {
  description = "Versão do Kubernetes do control plane"
  type        = string
  default     = "1.31"
}

variable "node_instance_types" {
  description = "Tipos de instância EC2 do Managed Node Group"
  type        = list(string)
  default     = ["m5.xlarge"]
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_min_size" {
  type    = number
  default = 2
}

variable "node_max_size" {
  type    = number
  default = 6
}

variable "app_namespace" {
  description = "Namespace k8s onde os microsserviços e o Datadog rodam"
  type        = string
  default     = "autorizador-debito"
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

variable "conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço conta"
  type        = string
}

variable "debit_authorizer_ecr_repository" {
  description = "Nome do repositório ECR para o debit-authorizer"
  type        = string
}

variable "datadog_api_key" {
  description = "Api key datadog"
  type        = string
  sensitive   = true
}

variable "logging_level" {
  description = "Nível de log"
  type        = string
}
