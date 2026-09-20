data "aws_caller_identity" "current" {}

#------------------------------------------------------------------------------
# OIDC Provider (necessário para IRSA) - criado depois do cluster existir
#------------------------------------------------------------------------------
data "tls_certificate" "eks_oidc" {
  url = var.cluster_oidc_issuer_url
}

resource "aws_iam_openid_connect_provider" "eks" {
  url             = var.cluster_oidc_issuer_url
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint]
}

locals {
  oidc_provider_host = replace(var.cluster_oidc_issuer_url, "https://", "")
}

#------------------------------------------------------------------------------
# IRSA Role da aplicação (equivalente à antiga ECS Task Role) - compartilhada
# por todos os pods via o ServiceAccount "app-service-account"
#------------------------------------------------------------------------------
resource "aws_iam_role" "app_irsa_role" {
  name = "eks-autorizador-debito-app-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringEquals = {
            "${local.oidc_provider_host}:sub" = "system:serviceaccount:${var.app_namespace}:${var.app_service_account_name}"
            "${local.oidc_provider_host}:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })
}

# Permissões para acessar o DynamoDB
resource "aws_iam_policy" "app_dynamodb_access" {
  name        = "eks-app-dynamodb-access"
  description = "Permissões para os pods acessarem o DynamoDB"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action   = ["dynamodb:Scan", "dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:BatchGetItem", "dynamodb:BatchWriteItem"]
        Effect   = "Allow"
        Resource = "arn:aws:dynamodb:${var.region}:${data.aws_caller_identity.current.account_id}:table/*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "app_dynamodb_access_attachment" {
  role       = aws_iam_role.app_irsa_role.name
  policy_arn = aws_iam_policy.app_dynamodb_access.arn
}

# Política para acesso ao SQS
resource "aws_iam_policy" "app_sqs_access" {
  name        = "eks-app-sqs-access"
  description = "Permissões para os pods acessarem as filas SQS"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "sqs:GetQueueUrl",
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:ChangeMessageVisibility"
        ]
        Effect = "Allow"
        # Para POC, liberamos todas as filas da conta na região.
        # Em prod, coloque o ARN da fila específica.
        Resource = "arn:aws:sqs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "app_sqs_access_attachment" {
  role       = aws_iam_role.app_irsa_role.name
  policy_arn = aws_iam_policy.app_sqs_access.arn
}
