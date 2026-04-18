data "aws_caller_identity" "current" {}

# Role de Execução (Execution Role)
resource "aws_iam_role" "ecs_execution_role" {
  name = "ecs-autorizador-debito-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

# Anexando a política de execução do ECS à Role de Execução
resource "aws_iam_policy_attachment" "ecs_execution_role_policy" {
  name       = "ecs-execution-role-policy-attachment"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  roles      = [aws_iam_role.ecs_execution_role.name]
}

# Adicionando permissão para acessar o ECR (caso o ECS precise puxar imagens do ECR)
resource "aws_iam_policy" "ecs_ecr_access" {
  name        = "ecs-ecr-access"
  description = "Permissões para o ECS acessar o ECR"
  policy      = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action   = ["ecr:GetAuthorizationToken", "ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage"]
        Effect   = "Allow"
        Resource = [
          for repo in var.ecr_repository_names :
          "arn:aws:ecr:${var.region}:${data.aws_caller_identity.current.account_id}:repository/${repo}"
        ]
      }
    ]
  })
}

resource "aws_iam_policy_attachment" "ecs_ecr_access_attachment" {
  name       = "ecs-ecr-access-attachment"
  policy_arn = aws_iam_policy.ecs_ecr_access.arn
  roles      = [aws_iam_role.ecs_execution_role.name]
}

# Role de Tarefa (Task Role) - Permite que o contêiner acesse recursos como S3, Secrets Manager e DynamoDB
resource "aws_iam_role" "ecs_task_role" {
  name = "ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

# Permissões para acessar o DynamoDB
resource "aws_iam_policy" "ecs_task_dynamodb_access" {
  name        = "ecs-task-dynamodb-access"
  description = "Permissões para o ECS acessar o DynamoDB"
  policy      = jsonencode({
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

resource "aws_iam_policy_attachment" "ecs_task_dynamodb_access_attachment" {
  name       = "ecs-task-dynamodb-access-attachment"
  policy_arn = aws_iam_policy.ecs_task_dynamodb_access.arn
  roles      = [aws_iam_role.ecs_task_role.name]
}
