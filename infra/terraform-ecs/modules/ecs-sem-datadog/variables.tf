#variables.tf
variable "private_subnets" {
  description = "Lista de subnets privadas"
  type        = list(string)
}

variable "ecr_repository" {
  description = "Nome do repositório ECR"
  type        = string
}

variable "execution_role_arn" {
  description = "Role de Execução"
  type        = string
}

variable "task_role_arn" {
  description = " Role de Tarefa"
  type        = string
}

variable "ecs_cluster_name" {
  description = "Nome do cluster"
  type        = string
}

variable "security_groups" {
  description = "Lista de IDs dos Security Groups para o NLB"
  type        = list(string)
}

variable "target_group_arn" {
  description = "Target Group nlb"
  type        = list(string)
}

variable "micro_service_name" {
  description = "Nome do micro serviço"
  type        = string
}

variable "cpu" {
  description = "CPU task"
  type        = string
}

variable "memory" {
  description = "Memória Task"
  type        = string
}

variable "region" {
  description = "Region"
  type        = string
}

variable "container_port" {
  description = "Container Port"
  type        = number
}
variable "host_port" {
  description = "Host Port"
  type        = number
}

variable "max_cpu_threshold" {
  description = " "
  type        = string
}

variable "min_cpu_threshold" {
  description = " "
  type        = string
}

variable "scale_target_min_capacity" {
  description = " "
  type        = string
}

variable "scale_target_max_capacity" {
  description = " "
  type        = string
}

variable "cooldown" {
  description = " "
  type        = string
}

variable "min_cpu_evaluation_period" {
  description = " "
  type        = string
}

variable "max_cpu_evaluation_period" {
  description = " "
  type        = string
}

variable "max_cpu_period" {
  description = " "
  type        = string
}

variable "min_cpu_period" {
  description = " "
  type        = string
}

variable "namespace_id" {
  description = "ID do namespace DNS privado"
  type        = string
}

variable "desired_count" {
  description = "desired count tasks "
  type        = string
}
