resource "aws_sqs_queue" "ms_integration_dlq" {
  name = "queue-comando-conta-dlq"
}

resource "aws_sqs_queue" "ms_integration_queue" {
  name                      = "queue-comando-conta"
  message_retention_seconds = 86400
  receive_wait_time_seconds = 20
  
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ms_integration_dlq.arn
    maxReceiveCount     = 5
  })
}