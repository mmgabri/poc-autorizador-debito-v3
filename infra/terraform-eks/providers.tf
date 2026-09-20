terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.33"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.16"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# Gera um token de autenticação de curta duração para o cluster recém-criado.
# Observação: como host/cluster_ca_certificate/token só ficam conhecidos após
# o `aws_eks_cluster` ser criado, o primeiro `terraform apply` cria o cluster
# e, na mesma execução, o Terraform aguarda esse resultado antes de inicializar
# os providers kubernetes/helm para aplicar os recursos do cluster.
data "aws_eks_cluster_auth" "this" {
  name = module.eks_cluster.cluster_name
}

provider "kubernetes" {
  host                   = module.eks_cluster.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks_cluster.cluster_certificate_authority_data)
  token                  = data.aws_eks_cluster_auth.this.token
}

provider "helm" {
  kubernetes {
    host                   = module.eks_cluster.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks_cluster.cluster_certificate_authority_data)
    token                  = data.aws_eks_cluster_auth.this.token
  }
}
