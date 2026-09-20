output "app_irsa_role_arn" {
  description = "ARN da IRSA Role usada pelo ServiceAccount das aplicações"
  value       = aws_iam_role.app_irsa_role.arn
}
