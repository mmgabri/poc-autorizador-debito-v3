region = "us-east-1"

cluster_name    = "autorizador-debito-cluster"
cluster_version = "1.31"

node_instance_types = ["m5.large"]
node_desired_size   = 6
node_min_size       = 2
node_max_size       = 6

app_namespace = "autorizador-debito"

enrichment_service_ecr_repository   = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment-service"
rules_service_ecr_repository        = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-service"
limit_service_ecr_repository        = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit-service"
ledger_service_ecr_repository       = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/ledger-service"
antifraud_service_ecr_repository    = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud-service"
security_service_ecr_repository     = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security-service"
autorizador_debito_ecr_repository   = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/autorizador-debito"
formatador_bandeiras_ecr_repository = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/formatador-bandeiras"

datadog_api_key = "e1ed6f0aa9f90d5889ab3fc1ff269b9d"

logging_level = "INFO"
