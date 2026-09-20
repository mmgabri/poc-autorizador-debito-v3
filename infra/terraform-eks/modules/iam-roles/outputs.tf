output "eks_cluster_role_arn" {
  description = "ARN da Role do Control Plane do EKS"
  value       = aws_iam_role.eks_cluster_role.arn
}

output "eks_node_role_arn" {
  description = "ARN da Role dos nós (Managed Node Group)"
  value       = aws_iam_role.eks_node_role.arn
}
