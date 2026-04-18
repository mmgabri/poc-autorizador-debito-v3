enable_eks_addons = false

region = "us-east-1"

micro_services = [
  "data-enrichment",
  "formatador",
  "autorizador",
  "seguranca",
  "limite-portador",
  "limite",
  "lancamento-conta",
  "fraudes",
  "datadog-agent"
]

ecr_repository_names = [
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/autorizador",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/formatador",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/fraudes",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/lancamento-conta",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limite",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limite-portador",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/data-enrichment",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/seguranca",
  "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/fraudes"
]

autorizador_ecr_repository      = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/autorizador"
formatador_ecr_repository       = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/formatador"
lancamento_conta_ecr_repository = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/lancamento-conta"
limite_ecr_repository           = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limite"
limite_portador_ecr_repository  = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/limite-portador"
data_enrichment_ecr_repository  = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/data-enrichment"
seguranca_ecr_repository        = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/seguranca"
fraudes_ecr_repository          = "140023369634.dkr.ecr.us-east-1.amazonaws.com/autorizador-debito/fraudes"


datadog_api_key = "c735d4be91ef0ab5e9292c5a50a6ee6b"