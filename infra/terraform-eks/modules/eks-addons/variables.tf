variable "cluster_name" {
  type = string
}

variable "region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "aws_lb_controller_role_arn" {
  type = string
}

variable "aws_lb_controller_chart_version" {
  type    = string
  default = "1.11.0"
}
