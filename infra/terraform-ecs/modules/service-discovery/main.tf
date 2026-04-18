resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = var.namespace_name
  description = "Namespace para comunicação interna dos serviços do Autorizador Débito"
  vpc         = var.vpc_id
}