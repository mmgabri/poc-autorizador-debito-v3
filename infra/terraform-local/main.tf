terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# Stack de recursos AWS standalone, sem dependência de VPC/ECS/EKS/ALB.
# Serve dois propósitos: (1) o que os serviços precisam pra rodar localmente
# (mvn spring-boot:run) contra recursos reais, e (2) bootstrap de recursos que
# nenhuma outra stack consegue criar sozinha - o ECR, por exemplo, precisa
# existir ANTES do terraform-eks aplicar, mas o terraform-eks não consegue
# nem fazer plan até o cluster existir (os providers kubernetes/helm dependem
# do cluster_endpoint). Reaproveita os módulos de terraform-ecs pra não
# duplicar definições (evita as stacks divergirem com o tempo).

module "dynamodb" {
  source = "../terraform-ecs/modules/dynamodb"
}

module "sqs" {
  source = "../terraform-ecs/modules/sqs"
}

module "ecr" {
  source = "../terraform-ecs/modules/ecr"
}
