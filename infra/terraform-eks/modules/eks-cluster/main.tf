data "aws_caller_identity" "current" {}

#------------------------------------------------------------------------------
# Control Plane
#------------------------------------------------------------------------------
resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  role_arn = var.cluster_role_arn
  version  = var.cluster_version

  vpc_config {
    subnet_ids              = concat(var.private_subnets, var.public_subnets)
    endpoint_private_access = true
    endpoint_public_access  = true
  }

  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = false
  }
}

#------------------------------------------------------------------------------
# Addons gerenciados
#------------------------------------------------------------------------------
resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "vpc-cni"
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "kube-proxy"
}

resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "coredns"
  depends_on   = [aws_eks_node_group.default]
}

#------------------------------------------------------------------------------
# Managed Node Group (EC2)
#------------------------------------------------------------------------------
resource "aws_eks_node_group" "default" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.cluster_name}-ng-default"
  node_role_arn   = var.node_role_arn
  subnet_ids      = var.private_subnets

  instance_types = var.node_instance_types
  capacity_type  = "ON_DEMAND"

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [aws_eks_addon.vpc_cni]
}

#------------------------------------------------------------------------------
# Access Entries (autenticação via API, sem aws-auth configmap)
# Obs: se quem roda o `terraform apply` usa um IAM Role assumido (SSO/CI),
# data.aws_caller_identity.current.arn retorna o ARN da sessão STS
# (assumed-role/.../session), não o ARN do role em si — troque
# `principal_arn` pelo ARN do role (sem o sufixo de sessão) nesse caso.
#------------------------------------------------------------------------------
resource "aws_eks_access_entry" "admin_caller" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = data.aws_caller_identity.current.arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admin_caller" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = data.aws_caller_identity.current.arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.admin_caller]
}

#------------------------------------------------------------------------------
# Access Entry pro usuário root da conta - permite visualizar os recursos do
# cluster (namespaces, pods, etc.) pelo console AWS logado como root. IAM
# root != acesso ao cluster: são planos de permissão separados (ver comentário
# acima), então sem isso o console mostra "Unauthorized" na aba Resources
# mesmo o root podendo fazer qualquer coisa via API da AWS.
#------------------------------------------------------------------------------
resource "aws_eks_access_entry" "root" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "root" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.root]
}
