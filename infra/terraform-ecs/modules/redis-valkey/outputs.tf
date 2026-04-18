output "valkey_endpoint" {
  description = "Endpoint primario do Valkey para configurar no Java"
  value       = aws_elasticache_replication_group.valkey_cluster.primary_endpoint_address
}

output "valkey_port" {
  value = 6379
}