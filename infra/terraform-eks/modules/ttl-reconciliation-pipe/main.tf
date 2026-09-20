# EventBridge Pipe: DynamoDB Stream (comando_conta) -> SQS (queue-transactions-pending).
# Sem Lambda no meio — o filtro do próprio Pipe já seleciona só os deletes
# causados pelo TTL (userIdentity = dynamodb.amazonaws.com) em registros que
# nunca saíram de PENDING/TIMEOUT (nunca chegaram a "completed").

data "aws_iam_policy_document" "pipe_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["pipes.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "pipe_role" {
  name               = var.name
  assume_role_policy = data.aws_iam_policy_document.pipe_assume_role.json
}

data "aws_iam_policy_document" "pipe_permissions" {
  statement {
    sid    = "ReadComandoContaStream"
    effect = "Allow"
    actions = [
      "dynamodb:DescribeStream",
      "dynamodb:GetRecords",
      "dynamodb:GetShardIterator",
      "dynamodb:ListStreams",
    ]
    resources = [var.source_stream_arn]
  }

  statement {
    sid       = "SendToTransactionsPendingQueue"
    effect    = "Allow"
    actions   = ["sqs:SendMessage"]
    resources = [var.target_queue_arn]
  }
}

resource "aws_iam_role_policy" "pipe_permissions" {
  name   = "${var.name}-permissions"
  role   = aws_iam_role.pipe_role.id
  policy = data.aws_iam_policy_document.pipe_permissions.json
}

resource "aws_pipes_pipe" "comando_conta_ttl" {
  name     = var.name
  role_arn = aws_iam_role.pipe_role.arn
  source   = var.source_stream_arn
  target   = var.target_queue_arn

  source_parameters {
    dynamodb_stream_parameters {
      starting_position = "LATEST"
      batch_size        = 10
    }

    filter_criteria {
      filter {
        pattern = jsonencode({
          eventName = ["REMOVE"]
          userIdentity = {
            principalId = ["dynamodb.amazonaws.com"]
          }
          dynamodb = {
            OldImage = {
              status = {
                S = ["PENDING", "TIMEOUT"]
              }
            }
          }
        })
      }
    }
  }
}
