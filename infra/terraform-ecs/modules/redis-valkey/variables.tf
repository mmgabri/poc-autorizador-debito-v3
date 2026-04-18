variable "vpc_id" {
  description = "ID da VPC onde o cluster EKS está rodando"
  type        = string
}

variable "private_subnets" {
  description = "Lista de IDs das subnets privadas para o Valkey"
  type        = list(string)
}

variable "public_subnet" {
  description = "ID da subnet publica para o Valkey"
  type        = string
}

