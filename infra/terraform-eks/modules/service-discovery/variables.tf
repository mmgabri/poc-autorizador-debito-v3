variable "vpc_id" {
  description = "ID da VPC onde o NLB será criado"
  type        = string
}

variable "namespace_name" {
  description = "Nome do namespace DNS privado"
  type        = string
}