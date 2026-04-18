data "aws_eks_cluster" "this" {
  count = var.enable_eks_addons ? 1 : 0
  name  = module.eks.cluster_name
}

data "aws_eks_cluster_auth" "this" {
  count = var.enable_eks_addons ? 1 : 0
  name  = module.eks.cluster_name
}

provider "kubernetes" {
  host                   = var.enable_eks_addons ? data.aws_eks_cluster.this[0].endpoint : "https://example.invalid"
  cluster_ca_certificate = var.enable_eks_addons ? base64decode(data.aws_eks_cluster.this[0].certificate_authority[0].data) : ""
  token                  = var.enable_eks_addons ? data.aws_eks_cluster_auth.this[0].token : ""
}

provider "helm" {
  kubernetes {
    host                   = var.enable_eks_addons ? data.aws_eks_cluster.this[0].endpoint : "https://example.invalid"
    cluster_ca_certificate = var.enable_eks_addons ? base64decode(data.aws_eks_cluster.this[0].certificate_authority[0].data) : ""
    token                  = var.enable_eks_addons ? data.aws_eks_cluster_auth.this[0].token : ""
  }
}
