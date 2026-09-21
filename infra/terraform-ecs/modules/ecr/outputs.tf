output "repository_urls" {
  description = "Mapa nome-do-repositório -> URL completa (pra copiar direto no terraform.tfvars)."
  value       = { for name, repo in aws_ecr_repository.this : name => repo.repository_url }
}
