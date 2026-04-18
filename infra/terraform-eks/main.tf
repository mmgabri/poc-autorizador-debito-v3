#------------------------------------------------------------------------------
# Cria vpc, subnets, router tables, igtw, nat, vpc endpoint dynamodb
#------------------------------------------------------------------------------
module "base" {
  source       = "./modules/base"
  region       = var.region
  cluster_name = var.eks_cluster_name
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
# Cria Cluster EKS
#------------------------------------------------------------------------------
module "eks" {
  source = "./modules/eks"

  #region              = var.region
  cluster_name        = var.eks_cluster_name
  kubernetes_version  = var.eks_kubernetes_version
  vpc_id              = module.base.vpc_id
  private_subnet_ids  = module.base.private_subnets
  node_instance_types = var.eks_node_instance_types
  node_ami_type       = var.eks_node_ami_type
  node_disk_size      = var.eks_node_disk_size

  tags = {
    Project = "autorizador-debito"
    IaC     = "terraform"
    Env     = var.environment
  }
}


#------------------------------------------------------------------------------
# Cria EKS Addons (AWS Load Balancer Controller)
#------------------------------------------------------------------------------
module "eks_addons" {
  source = "./modules/eks-addons"
  count  = var.enable_eks_addons ? 1 : 0

  cluster_name               = module.eks.cluster_name
  region                     = var.region
  vpc_id                     = module.base.vpc_id
  aws_lb_controller_role_arn = var.aws_lb_controller_role_arn

  providers = {
    kubernetes = kubernetes
    helm       = helm
  }
}