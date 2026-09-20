variable "region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
}

variable "cluster_oidc_issuer_url" {
  description = "URL do OIDC issuer do cluster EKS (module.eks_cluster.oidc_issuer_url)"
  type        = string
}

variable "app_namespace" {
  description = "Namespace k8s onde os microsserviços rodam"
  type        = string
  default     = "autorizador-debito"
}

variable "app_service_account_name" {
  description = "Nome do ServiceAccount k8s compartilhado pelos pods das aplicações"
  type        = string
  default     = "app-service-account"
}
