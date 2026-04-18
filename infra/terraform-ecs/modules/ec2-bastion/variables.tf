variable "public_subnet" {
  description = "ID da subnet pública para o Bastion"
  type        = string
}

variable "vpc_id" {
  description = "ID da VPC onde o cluster EKS está rodando"
  type        = string
}