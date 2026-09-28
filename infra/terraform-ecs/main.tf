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
  region = "us-east-1"
}

#------------------------------------------------------------------------------
# Cria vpc, subnets, router tables, igtw, nat, vpc endpoint dynamodb
#------------------------------------------------------------------------------
module "base" {
  source = "./modules/base"
  region = var.region
}

#------------------------------------------------------------------------------
# Cria Tabelas DynamoDb
#------------------------------------------------------------------------------
module "dynamodb" {
  source = "./modules/dynamodb"
}

#------------------------------------------------------------------------------
# Cria Log Group Cloudwatch
#------------------------------------------------------------------------------
module "cloudwatch" {
  source         = "./modules/cloudwatch"
  micro_services = var.micro_services
}

#------------------------------------------------------------------------------
# Cria Security Group
#------------------------------------------------------------------------------
module "security_group" {
  source = "./modules/security-group"
  vpc_id = module.base.vpc_id
}

#------------------------------------------------------------------------------
# Cria Roles
#------------------------------------------------------------------------------
module "iam_roles" {
  source               = "./modules/iam-roles"
  region               = var.region
  ecr_repository_names = var.ecr_repository_names
}

#------------------------------------------------------------------------------
# Cria Application Load Balancer (ALB)
#------------------------------------------------------------------------------
module "alb" {
  source          = "./modules/alb"
  security_groups = [module.security_group.ecs_sg_id]
  public_subnets  = module.base.public_subnets
  vpc_id          = module.base.vpc_id
}

#------------------------------------------------------------------------------
# Cria Cloud Map
#------------------------------------------------------------------------------
module "service_discovery" {
  source         = "./modules/service-discovery"
  namespace_name = "autorizador-debito.local"
  vpc_id         = module.base.vpc_id
}

#------------------------------------------------------------------------------
# Cria Fila SQS
#------------------------------------------------------------------------------
module "sqs" {
  source = "./modules/sqs"
}

#------------------------------------------------------------------------------
# Cria Redis Vankey
#------------------------------------------------------------------------------
module "redis_valkey" {
  source          = "./modules/redis-valkey"
  public_subnet   = module.base.public_subnets[0]
  private_subnets = module.base.private_subnets
  vpc_id          = module.base.vpc_id
}

#---------------------------------------------------------------------------------------------------
# Cria EC2 Bastion - Necessário se for executar localmente o Valkey CLI para sincronizar as réplicas
#---------------------------------------------------------------------------------------------------
#module "ec2_bastion" {
#  source        = "./modules/ec2-bastion"
#  public_subnet = module.base.public_subnets[0]
#  vpc_id        = module.base.vpc_id
#}

#------------------------------------------------------------------------------
# Cria Cluster ECS
#------------------------------------------------------------------------------
module "cluster_ecs" {
  source = "./modules/cluster-ecs"
}

#------------------------------------------------------------------------------
# Cria Service / Task message-parser
#------------------------------------------------------------------------------
module "ecs_message_parser" {
  source             = "./modules/ecs-com-datadog"
  micro_service_name = "message-parser"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.message_parser_ecr_repository
  datadog_api_key    = var.datadog_api_key
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = [module.alb.target_group_arn]
  namespace_id       = module.service_discovery.namespace_id
  redis_host         = ""
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9090
  host_port          = 9090
  desired_count      = 1
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task debit-authorizer
#------------------------------------------------------------------------------
module "ecs_debit_authorizer" {
  source             = "./modules/ecs-com-datadog"
  depends_on         = [module.ecs_message_parser]
  micro_service_name = "debit-authorizer"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.debit_authorizer_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  datadog_api_key    = var.datadog_api_key
  redis_host         = module.redis_valkey.valkey_endpoint
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 4096
  memory             = 8192
  region             = var.region
  container_port     = 9091
  host_port          = 9091
  desired_count      = 2
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - enrichment
#------------------------------------------------------------------------------
module "ecs_enrichment" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_debit_authorizer]
  micro_service_name = "enrichment"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.enrichment_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9092
  host_port          = 9092
  desired_count      = 1
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task  - rules-engine
#------------------------------------------------------------------------------
module "ecs_rules_engine" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_enrichment]
  micro_service_name = "rules-engine"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.rules_engine_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9093
  host_port          = 9093
  desired_count      = 1
  logging_level      = var.logging_level
}


#------------------------------------------------------------------------------
# Cria Service / Task - security
#------------------------------------------------------------------------------
module "ecs_security" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_rules_engine]
  micro_service_name = "security"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.security_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9094
  host_port          = 9094
  desired_count      = 1
  logging_level      = var.logging_level
}


#------------------------------------------------------------------------------
# Cria Service / Task - limit
#------------------------------------------------------------------------------
module "ecs_limit" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_security]
  micro_service_name = "limit"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.limit_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9095
  host_port          = 9095
  desired_count      = 1
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task - account-posting
#------------------------------------------------------------------------------
module "ecs_account_posting" {
  source             = "./modules/ecs-com-datadog"
  depends_on         = [module.ecs_limit]
  micro_service_name = "account-posting"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.account_posting_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  datadog_api_key    = var.datadog_api_key
  redis_host         = module.redis_valkey.valkey_endpoint
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9096
  host_port          = 9096
  desired_count      = 10
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task - conta
#------------------------------------------------------------------------------
module "ecs_conta" {
  source             = "./modules/ecs-com-datadog"
  depends_on         = [module.ecs_account_posting]
  micro_service_name = "conta"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.conta_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  datadog_api_key    = var.datadog_api_key
  redis_host         = ""
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9098
  host_port          = 9098
  desired_count      = 3
  logging_level      = var.logging_level
}

#------------------------------------------------------------------------------
# Cria Service / Task - antifraud
#------------------------------------------------------------------------------
module "ecs_antifraud" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_conta]
  micro_service_name = "antifraud"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.antifraud_ecr_repository
  execution_role_arn = module.iam_roles.ecs_execution_role_arn
  task_role_arn      = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name   = module.cluster_ecs.ecs_cluster_autorizador
  security_groups    = [module.security_group.ecs_sg_id]
  target_group_arn   = []
  namespace_id       = module.service_discovery.namespace_id
  cpu                = 2048
  memory             = 4096
  region             = var.region
  container_port     = 9097
  host_port          = 9097
  desired_count      = 1
  logging_level      = var.logging_level
}
