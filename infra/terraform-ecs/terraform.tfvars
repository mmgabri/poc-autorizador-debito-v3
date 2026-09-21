region = "us-east-1"

micro_services = [
  "enrichment-service",
  "rules-service",
  "limit-service",
  "ledger-service",
  "conta",
  "antifraud-service",
  "security-service",
  "autorizador-debito",
  "formatador-bandeiras",
  "datadog-agent"
]

ecr_repository_names = [
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/ledger-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/conta",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security-service",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/autorizador-debito",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/formatador-bandeiras"
]

enrichment_service_ecr_repository   = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment-service"
rules_service_ecr_repository        = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-service"
limit_service_ecr_repository        = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit-service"
ledger_service_ecr_repository       = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/ledger-service"
conta_ecr_repository                = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/conta"
antifraud_service_ecr_repository    = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud-service"
security_service_ecr_repository     = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security-service"
autorizador_debito_ecr_repository   = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/autorizador-debito"
formatador_bandeiras_ecr_repository = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/formatador-bandeiras"

datadog_api_key = "e1ed6f0aa9f90d5889ab3fc1ff269b9d"

logging_level = "INFO"