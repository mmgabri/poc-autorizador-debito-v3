variable "region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
}

variable "ecr_repository_names" {
  description = "Nome dos repositórios ECR"
  type        = list(string)
}