variable "vpc_id" {
  description = "ID da VPC onde o cluster EKS está rodando"
  type        = string
}

variable "private_subnets" {
  description = "Lista de IDs das subnets privadas para o Valkey"
  type        = list(string)
}

variable "node_security_group_id" {
  description = "Security Group compartilhado pelo control plane/nós do EKS, origem permitida no SG do Valkey"
  type        = string
}
