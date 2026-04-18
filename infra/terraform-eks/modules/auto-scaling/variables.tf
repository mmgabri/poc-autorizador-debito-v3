variable "name_prefix" {
  description = "Prexixo do nome da metrica"
  type        = string
}

variable "max_cpu_evaluation_period" {
  description = "Número de períodos consecutivos em que a condição deve ser atendida antes de acionar o alarme"
  type        = string
}

variable "min_cpu_evaluation_period" {
  description = "Número de períodos consecutivos em que a condição deve ser atendida antes de acionar o alarme"
  type        = string
}

variable "max_cpu_period" {
  description = "Define a duração (em segundos) de cada período de avaliação para o alarme de uso de CPU."
  type        = string
}

variable "min_cpu_period" {
  description = " tempo (em segundos) que o CloudWatch usa para agrupar os dados da métrica antes de avaliar a condição do alarme"
  type        = string
}

variable "max_cpu_threshold" {
  description = "define o limite (threshold) da CPU que, se for ultrapassado, acionará o alarme do CloudWatch"
  type        = string
}

variable "ecs_cluster_name" {
  description = "Lista de IDs dos Security Groups para o NLB"
  type        = string
}

variable "ecs_service_name" {
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

