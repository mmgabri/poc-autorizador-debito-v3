#------------------------------------------------------------------------------
# Cria vpc, subnets, router tables, igtw, nat, vpc endpoint dynamodb
#------------------------------------------------------------------------------
module "base" {
  source       = "./modules/base"
  region       = var.region
  cluster_name = var.cluster_name
}

#------------------------------------------------------------------------------
# DynamoDB e SQS NÃO são criados aqui de propósito: já existem, criados pelo
# infra/terraform-local (que virou o dono permanente desses recursos - ver
# comentário em terraform-local/main.tf). Criar de novo aqui daria conflito de
# nome (DynamoDB) ou duas stacks gerenciando o mesmo recurso via states
# diferentes (SQS) - a app só precisa que existam e da permissão IAM (IRSA já
# libera dynamodb:*/sqs:* pra conta inteira, sem depender de ARN específico).
#------------------------------------------------------------------------------

#------------------------------------------------------------------------------
# Cria Roles do control plane e dos nós (não dependem do cluster existir)
#------------------------------------------------------------------------------
module "iam_roles" {
  source = "./modules/iam-roles"
}

#------------------------------------------------------------------------------
# Cria Cluster EKS (control plane + managed node group + addons)
#------------------------------------------------------------------------------
module "eks_cluster" {
  source = "./modules/eks-cluster"

  cluster_name     = var.cluster_name
  cluster_version  = var.cluster_version
  cluster_role_arn = module.iam_roles.eks_cluster_role_arn
  node_role_arn    = module.iam_roles.eks_node_role_arn

  private_subnets = module.base.private_subnets
  public_subnets  = module.base.public_subnets

  node_instance_types = var.node_instance_types
  node_desired_size   = var.node_desired_size
  node_min_size       = var.node_min_size
  node_max_size       = var.node_max_size
}

#------------------------------------------------------------------------------
# Cria o OIDC provider e a IRSA Role da aplicação (equivalente à ECS Task Role)
#------------------------------------------------------------------------------
module "irsa" {
  source = "./modules/irsa"

  region                   = var.region
  cluster_oidc_issuer_url  = module.eks_cluster.oidc_issuer_url
  app_namespace            = var.app_namespace
  app_service_account_name = "app-service-account"
}

#------------------------------------------------------------------------------
# Cria Redis Valkey (acesso restrito ao SG do cluster EKS)
#------------------------------------------------------------------------------
module "redis_valkey" {
  source = "./modules/redis-valkey"

  vpc_id                 = module.base.vpc_id
  private_subnets        = module.base.private_subnets
  node_security_group_id = module.eks_cluster.node_security_group_id
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
# Namespace k8s dos microsserviços + ServiceAccount com IRSA (DynamoDB/SQS)
#------------------------------------------------------------------------------
resource "kubernetes_namespace" "app" {
  metadata {
    name = var.app_namespace
  }

  depends_on = [module.eks_cluster]
}

resource "kubernetes_service_account" "app" {
  metadata {
    name      = "app-service-account"
    namespace = kubernetes_namespace.app.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" = module.irsa.app_irsa_role_arn
    }
  }
}

#------------------------------------------------------------------------------
# Datadog Agent (DaemonSet) + Cluster Agent via Helm
#------------------------------------------------------------------------------
module "datadog" {
  source = "./modules/datadog"

  namespace       = kubernetes_namespace.app.metadata[0].name
  datadog_api_key = var.datadog_api_key
}

#------------------------------------------------------------------------------
# Deployment/Service - message-parser (único serviço exposto externamente)
#------------------------------------------------------------------------------
module "k8s_message_parser" {
  source = "./modules/k8s-deployment"

  name                 = "message-parser"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.message_parser_ecr_repository}:latest"
  container_port       = 9090
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  service_type         = "LoadBalancer"
  datadog_apm_enabled  = true
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
    DD_API_KEY    = var.datadog_api_key
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - debit-authorizer (orquestrador principal)
#------------------------------------------------------------------------------
module "k8s_debit_authorizer" {
  source = "./modules/k8s-deployment"

  name                 = "debit-authorizer"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.debit_authorizer_ecr_repository}:latest"
  container_port       = 9091
  grpc_port            = 59091
  replicas             = 2
  service_account_name = kubernetes_service_account.app.metadata[0].name
  datadog_apm_enabled  = true
  cpu_request          = "500m"
  cpu_limit            = "4"
  memory_request       = "1Gi"
  memory_limit         = "8Gi"

  env = {
    LOGGING_LEVEL       = var.logging_level
    DATABASE_MODE_ASYNC = "false"
    DD_API_KEY          = var.datadog_api_key
    REDIS_HOST          = module.redis_valkey.valkey_endpoint
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - enrichment
#------------------------------------------------------------------------------
module "k8s_enrichment" {
  source = "./modules/k8s-deployment"

  name                 = "enrichment"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.enrichment_ecr_repository}:latest"
  container_port       = 9092
  grpc_port            = 59092
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - rules-engine
#------------------------------------------------------------------------------
module "k8s_rules_engine" {
  source = "./modules/k8s-deployment"

  name                 = "rules-engine"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.rules_engine_ecr_repository}:latest"
  container_port       = 9093
  grpc_port            = 59093
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - security
#------------------------------------------------------------------------------
module "k8s_security" {
  source = "./modules/k8s-deployment"

  name                 = "security"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.security_ecr_repository}:latest"
  container_port       = 9094
  grpc_port            = 59094
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - limit
#------------------------------------------------------------------------------
module "k8s_limit" {
  source = "./modules/k8s-deployment"

  name                 = "limit"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.limit_ecr_repository}:latest"
  container_port       = 9095
  grpc_port            = 59095
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - account-posting (10 réplicas, igual ao ECS)
#------------------------------------------------------------------------------
module "k8s_account_posting" {
  source = "./modules/k8s-deployment"

  name                 = "account-posting"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.account_posting_ecr_repository}:latest"
  container_port       = 9096
  grpc_port            = 59096
  replicas             = 10
  service_account_name = kubernetes_service_account.app.metadata[0].name
  datadog_apm_enabled  = true
  cpu_request          = "500m"
  cpu_limit            = "2"
  memory_request       = "1Gi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
    REDIS_HOST    = module.redis_valkey.valkey_endpoint
    DD_API_KEY    = var.datadog_api_key
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - antifraud
#------------------------------------------------------------------------------
module "k8s_antifraud" {
  source = "./modules/k8s-deployment"

  name                 = "antifraud"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.antifraud_ecr_repository}:latest"
  container_port       = 9097
  grpc_port            = 59097
  replicas             = 1
  service_account_name = kubernetes_service_account.app.metadata[0].name
  cpu_request          = "250m"
  cpu_limit            = "2"
  memory_request       = "512Mi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
  }
}

#------------------------------------------------------------------------------
# Deployment/Service - conta (consumidor SQS + callback gRPC pro account-posting;
# sem servidor gRPC próprio, por isso sem grpc_port)
#------------------------------------------------------------------------------
module "k8s_conta" {
  source = "./modules/k8s-deployment"

  name                 = "conta"
  namespace            = kubernetes_namespace.app.metadata[0].name
  image                = "${var.conta_ecr_repository}:latest"
  container_port       = 9098
  replicas             = 3
  service_account_name = kubernetes_service_account.app.metadata[0].name
  datadog_apm_enabled  = true
  cpu_request          = "500m"
  cpu_limit            = "2"
  memory_request       = "1Gi"
  memory_limit         = "4Gi"

  env = {
    LOGGING_LEVEL = var.logging_level
    DD_API_KEY    = var.datadog_api_key
  }
}
