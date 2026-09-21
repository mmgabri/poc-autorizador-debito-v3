# Repositórios ECR das imagens dos microsserviços. Recurso standalone (sem
# dependência de VPC/cluster) - aplicado via terraform-local, e referenciado
# como string simples (repository_url) pelos tfvars do terraform-ecs/terraform-eks,
# sem acoplamento de módulo, pra não haver risco de "recurso já existe" ao
# aplicar duas stacks diferentes sobre o mesmo repositório.

locals {
  repository_names = [
    "autorizador-debito/enrichment-service",
    "autorizador-debito/rules-service",
    "autorizador-debito/security-service",
    "autorizador-debito/limit-service",
    "autorizador-debito/ledger-service",
    "autorizador-debito/antifraud-service",
    "autorizador-debito/formatador-bandeiras",
    "autorizador-debito/autorizador-debito",
  ]
}

resource "aws_ecr_repository" "this" {
  for_each             = toset(local.repository_names)
  name                 = each.value
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}
