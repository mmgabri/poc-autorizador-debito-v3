variable "region" {
  type = string
}

variable "project_name" {
  type    = string
  default = "autorizador-debito"
}

variable "cluster_name" {
  description = "Nome do cluster EKS, usado nas tags de subnet exigidas para autodiscovery de load balancers"
  type        = string
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.11.0/24", "10.0.12.0/24"]
}

# false = 1 NAT (mais barato). true = NAT em cada AZ (mais resiliente).
variable "nat_per_az" {
  type    = bool
  default = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
