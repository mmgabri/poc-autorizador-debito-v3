variable "name" {
  description = "Nome do microsserviço (usado como nome do Deployment/labels; o Service vira <name>-svc)"
  type        = string
}

variable "namespace" {
  type = string
}

variable "image" {
  description = "Imagem completa, ex.: <ecr_repository>:latest"
  type        = string
}

variable "container_port" {
  description = "Porta REST (server.port / actuator health)"
  type        = number
}

variable "grpc_port" {
  description = "Porta do servidor gRPC, se o serviço expuser um (null quando não expõe)"
  type        = number
  default     = null
}

variable "replicas" {
  type    = number
  default = 1
}

variable "env" {
  description = "Variáveis de ambiente adicionais do container"
  type        = map(string)
  default     = {}
}

variable "service_account_name" {
  type = string
}

variable "service_type" {
  description = "ClusterIP (padrão, comunicação interna) ou LoadBalancer (exposição externa via NLB)"
  type        = string
  default     = "ClusterIP"
}

variable "datadog_apm_enabled" {
  description = "Se true, adiciona labels de Unified Service Tagging e aponta o pod para o Datadog Agent do node (DaemonSet)"
  type        = bool
  default     = false
}

variable "datadog_env" {
  type    = string
  default = "producao"
}

variable "datadog_version" {
  type    = string
  default = "1.0.0"
}

variable "cpu_request" {
  type    = string
  default = "500m"
}

variable "cpu_limit" {
  type    = string
  default = "1"
}

variable "memory_request" {
  type    = string
  default = "2Gi"
}

variable "memory_limit" {
  type    = string
  default = "4Gi"
}
