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
module "ec2_bastion" {
  source        = "./modules/ec2-bastion"
  public_subnet = module.base.public_subnets[0]
  vpc_id        = module.base.vpc_id
}

#------------------------------------------------------------------------------
# Cria Cluster ECS
#------------------------------------------------------------------------------
module "cluster_ecs" {
  source = "./modules/cluster-ecs"
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - Formatador
#------------------------------------------------------------------------------
module "ecs_formatador" {
  source                    = "./modules/ecs-com-datadog"
  micro_service_name        = "formatador"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.formatador_ecr_repository
  datadog_api_key           = var.datadog_api_key
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = [module.alb.target_group_arn]
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8080
  host_port                 = 8080
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 10
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - Autorizador
#------------------------------------------------------------------------------
module "ecs_autorizador" {
  source                    = "./modules/ecs-com-datadog"
  depends_on                = [module.ecs_formatador]
  micro_service_name        = "autorizador"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.autorizador_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  datadog_api_key           = var.datadog_api_key
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 4096
  memory                    = 8192
  region                    = var.region
  container_port            = 8081
  host_port                 = 8081
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 2
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - data-enrichment
#------------------------------------------------------------------------------
module "ecs_data_enrichment" {
  source                    = "./modules/ecs-sem-datadog"
  depends_on                = [module.ecs_autorizador]
  micro_service_name        = "data-enrichment"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.data_enrichment_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8082
  host_port                 = 8082
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}


#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - seguranca
#------------------------------------------------------------------------------
module "ecs_seguranca" {
  source                    = "./modules/ecs-com-datadog"
  depends_on                = [module.ecs_data_enrichment]
  micro_service_name        = "seguranca"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.seguranca_ecr_repository
  datadog_api_key           = var.datadog_api_key
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8083
  host_port                 = 8083
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}



#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - limite-portador
#------------------------------------------------------------------------------
module "ecs_limite_portador" {
  source                    = "./modules/ecs-sem-datadog"
  depends_on                = [module.ecs_seguranca]
  micro_service_name        = "limite-portador"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.limite_portador_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8084
  host_port                 = 8084
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1

}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - limite
#------------------------------------------------------------------------------
module "ecs_limite" {
  source                    = "./modules/ecs-sem-datadog"
  depends_on                = [module.ecs_limite_portador]
  micro_service_name        = "limite"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.limite_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8085
  host_port                 = 8085
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 180
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - lancamento-conta
#------------------------------------------------------------------------------
module "ecs_lancamento_conta" {
  source = "./modules/ecs-sem-datadog"
  depends_on                = [module.ecs_limite]
  micro_service_name        = "lancamento-conta"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.lancamento_conta_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8086
  host_port                 = 8086
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}


#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - conta
#------------------------------------------------------------------------------
module "ecs_conta" {
  source                    = "./modules/ecs-sem-datadog"
  depends_on                = [module.ecs_lancamento_conta]
  micro_service_name        = "conta"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.conta_ecr_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8089
  host_port                 = 8089
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 180
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - async-bridge
#------------------------------------------------------------------------------
module "ecs_async_bridge" {
  redis_host                = module.redis_valkey.valkey_endpoint
  source                    = "./modules/ecs-async-bridge"
  depends_on                = [module.ecs_conta]
  micro_service_name        = "async-bridge"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.async_bridge_repository
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8088
  host_port                 = 8088
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 180
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}

#------------------------------------------------------------------------------
# Cria Service / Task / Scaling - Fraudes
#------------------------------------------------------------------------------
module "ecs_fraudes" {
  source                    = "./modules/ecs-com-datadog"
  depends_on                = [module.ecs_data_enrichment]
  micro_service_name        = "fraudes"
  private_subnets           = module.base.private_subnets
  ecr_repository            = var.fraudes_ecr_repository
  datadog_api_key           = var.datadog_api_key
  execution_role_arn        = module.iam_roles.ecs_execution_role_arn
  task_role_arn             = module.iam_roles.ecs_task_role_arn
  ecs_cluster_name          = module.cluster_ecs.ecs_cluster_autorizador
  security_groups           = [module.security_group.ecs_sg_id]
  target_group_arn          = []
  namespace_id              = module.service_discovery.namespace_id
  cpu                       = 2048
  memory                    = 4096
  region                    = var.region
  container_port            = 8087
  host_port                 = 8087
  max_cpu_threshold         = 70
  min_cpu_threshold         = 30
  max_cpu_period            = 30
  min_cpu_period            = 30
  scale_target_min_capacity = 1
  scale_target_max_capacity = 2
  cooldown                  = 60
  min_cpu_evaluation_period = 3
  max_cpu_evaluation_period = 3
  desired_count             = 1
}