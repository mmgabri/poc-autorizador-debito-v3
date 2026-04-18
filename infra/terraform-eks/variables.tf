variable "enable_eks_addons" {
  description = "Habilita instalação de addons Kubernetes/Helm (ex: AWS Load Balancer Controller)"
  type        = bool
  default     = false
}

variable "region" {
  type        = string
  description = "AWS region"
  default     = "us-east-1"
}

variable "ecr_repository_names" {
  description = "Nome dos repositórios ECR"
  type        = list(string)
}

variable "micro_services" {
  description = "Micro serviços da autorização de débito"
  type        = list(string)
}

variable "data_enrichment_ecr_repository" {
  description = "Nome do repositório ECR para o serviço data-enrichment"
  type        = string
}

variable "formatador_ecr_repository" {
  description = "Nome do repositório ECR para o serviço formatador"
  type        = string
}

variable "seguranca_ecr_repository" {
  description = "Nome do repositório ECR para o serviço seguranca"
  type        = string
}

variable "limite_portador_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limite-portador"
  type        = string
}

variable "limite_ecr_repository" {
  description = "Nome do repositório ECR para o serviço limite"
  type        = string
}

variable "lancamento_conta_ecr_repository" {
  description = "Nome do repositório ECR para o serviço lancamento-conta"
  type        = string
}

variable "fraudes_ecr_repository" {
  description = "Nome do repositório ECR para o serviço fraudes"
  type        = string
}

variable "autorizador_ecr_repository" {
  description = "Nome do repositório ECR para o autorizador"
  type        = string
}

variable "datadog_api_key" {
  description = "Api key datadog"
  type        = string
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "eks_cluster_name" {
  type    = string
  default = "autorizador-debito-eks"
}

variable "eks_kubernetes_version" {
  type    = string
  default = "1.31"
}

variable "eks_node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}

variable "eks_node_ami_type" {
  type    = string
  default = "AL2_x86_64"
}

variable "eks_node_disk_size" {
  type    = number
  default = 50
}

variable "aws_lb_controller_role_arn" {
  type        = string
  description = "ARN da role IRSA do AWS Load Balancer Controller"
  default     = ""
}

