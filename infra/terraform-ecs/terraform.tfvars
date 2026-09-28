region = "us-east-1"

micro_services = [
  "enrichment",
  "rules-engine",
  "limit",
  "account-posting",
  "conta",
  "antifraud",
  "security",
  "debit-authorizer",
  "message-parser",
  "datadog-agent"
]

ecr_repository_names = [
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-engine",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/account-posting",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/conta",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/debit-authorizer",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/message-parser"
]

enrichment_ecr_repository       = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment"
rules_engine_ecr_repository     = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-engine"
limit_ecr_repository            = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit"
account_posting_ecr_repository  = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/account-posting"
conta_ecr_repository            = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/conta"
antifraud_ecr_repository        = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud"
security_ecr_repository         = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security"
debit_authorizer_ecr_repository = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/debit-authorizer"
message_parser_ecr_repository   = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/message-parser"

datadog_api_key = "e1ed6f0aa9f90d5889ab3fc1ff269b9d"

logging_level = "INFO"