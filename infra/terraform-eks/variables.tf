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

variable "conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço conta"
  type        = string
}

variable "autorizador_debito_ecr_repository" {
  description = "Nome do repositório ECR para o autorizador-debito"
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
