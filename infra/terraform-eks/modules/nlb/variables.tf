variable "vpc_id" {
  description = "ID da VPC onde o NLB será criado"
  type        = string
}

variable "public_subnets" {
  description = "Lista de subnets públicas para o NLB"
  type        = list(string)
}

variable "security_groups" {
  description = "IDs dos Security Groups para o NLB"
  type        = list(string)  # ou set(string)
}
