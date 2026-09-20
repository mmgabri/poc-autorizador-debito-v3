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
# Pipe: TTL/Stream do comando_conta -> queue-transactions-pending (sem Lambda)
#------------------------------------------------------------------------------
module "ttl_reconciliation_pipe" {
  source            = "./modules/ttl-reconciliation-pipe"
  source_stream_arn = module.dynamodb.comando_conta_table_stream_arn
  target_queue_arn  = module.sqs.transactions_pending_queue_arn
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
# Cria Service / Task formatador-bandeiras
#------------------------------------------------------------------------------
module "ecs_formatador_bandeiras" {
  source             = "./modules/ecs-com-datadog"
  micro_service_name = "formatador-bandeiras"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.formatador_bandeiras_ecr_repository
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
# Cria Service / Task autorizador-debito
#------------------------------------------------------------------------------
module "ecs_autorizador_debito" {
  source             = "./modules/ecs-com-datadog"
  depends_on         = [module.ecs_formatador_bandeiras]
  micro_service_name = "autorizador-debito"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.autorizador_debito_ecr_repository
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
# Cria Service / Task / Scaling - enrichment-service
#------------------------------------------------------------------------------
module "ecs_enrichment_service" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_autorizador_debito]
  micro_service_name = "enrichment-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.enrichment_service_ecr_repository
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
# Cria Service / Task  - rules-service
#------------------------------------------------------------------------------
module "ecs_rules_service" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_enrichment_service]
  micro_service_name = "rules-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.rules_service_ecr_repository
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
# Cria Service / Task - security-service
#------------------------------------------------------------------------------
module "ecs_security_service" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_rules_service]
  micro_service_name = "security-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.security_service_ecr_repository
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
# Cria Service / Task - limit-service
#------------------------------------------------------------------------------
module "ecs_limit_service" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_security_service]
  micro_service_name = "limit-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.limit_service_ecr_repository
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
# Cria Service / Task - ledger-service
#------------------------------------------------------------------------------
module "ecs_ledger_service" {
  source             = "./modules/ecs-com-datadog"
  depends_on         = [module.ecs_limit_service]
  micro_service_name = "ledger-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.ledger_service_ecr_repository
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
  depends_on         = [module.ecs_ledger_service]
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
# Cria Service / Task - antifraud-service
#------------------------------------------------------------------------------
module "ecs_antifraud_service" {
  source             = "./modules/ecs-sem-datadog"
  depends_on         = [module.ecs_conta]
  micro_service_name = "antifraud-service"
  private_subnets    = module.base.private_subnets
  ecr_repository     = var.antifraud_service_ecr_repository
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
