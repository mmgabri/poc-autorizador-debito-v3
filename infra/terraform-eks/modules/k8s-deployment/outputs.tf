output "service_name" {
  value = kubernetes_service_v1.this.metadata[0].name
}

output "service_dns" {
  description = "FQDN do Service dentro do cluster"
  value       = "${kubernetes_service_v1.this.metadata[0].name}.${var.namespace}.svc.cluster.local"
}
