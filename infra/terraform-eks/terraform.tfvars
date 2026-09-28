region = "us-east-1"

cluster_name    = "autorizador-debito-cluster"
cluster_version = "1.31"

node_instance_types = ["m5.large"]
node_desired_size   = 8
node_min_size       = 2
node_max_size       = 8

app_namespace = "autorizador-debito"

enrichment_ecr_repository       = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/enrichment"
rules_engine_ecr_repository     = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/rules-engine"
limit_ecr_repository            = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limit"
account_posting_ecr_repository  = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/account-posting"
antifraud_ecr_repository        = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/antifraud"
security_ecr_repository         = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/security"
debit_authorizer_ecr_repository = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/debit-authorizer"
message_parser_ecr_repository   = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/message-parser"
conta_ecr_repository            = "330785366580.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/conta"

datadog_api_key = "e1ed6f0aa9f90d5889ab3fc1ff269b9d"

logging_level = "INFO"
