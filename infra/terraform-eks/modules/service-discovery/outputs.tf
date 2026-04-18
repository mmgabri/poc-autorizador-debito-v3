output "namespace_id" {
  description = "ID do namespace DNS privado criado"
  value       = aws_service_discovery_private_dns_namespace.this.id
}