variable "cluster_name" {
  type = string
}

variable "cluster_version" {
  type    = string
  default = "1.31"
}

variable "cluster_role_arn" {
  type = string
}

variable "node_role_arn" {
  type = string
}

variable "private_subnets" {
  type = list(string)
}

variable "public_subnets" {
  type = list(string)
}

variable "node_instance_types" {
  type    = list(string)
  default = ["m5.xlarge"]
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
