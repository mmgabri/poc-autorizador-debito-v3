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

# Stack mínima só com o que os serviços precisam pra rodar localmente (mvn
# spring-boot:run) contra recursos AWS reais, sem VPC/ECS/EKS/ALB. Reaproveita
# os mesmos módulos usados em terraform-ecs/terraform-eks pra não duplicar as
# definições de tabelas/filas (evita as duas stacks divergirem com o tempo).

module "dynamodb" {
  source = "../terraform-ecs/modules/dynamodb"
}

module "sqs" {
  source = "../terraform-ecs/modules/sqs"
}

module "ttl_reconciliation_pipe" {
  source            = "../terraform-ecs/modules/ttl-reconciliation-pipe"
  source_stream_arn = module.dynamodb.comando_conta_table_stream_arn
  target_queue_arn  = module.sqs.transactions_pending_queue_arn
}
