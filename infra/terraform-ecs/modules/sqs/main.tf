#Fila 1
resource "aws_sqs_queue" "ms_ledger_dlq" {
  name = "queue-comando-conta-dlq"
}

resource "aws_sqs_queue" "ms_ledger_queue" {
  name                      = "queue-comando-conta"
  message_retention_seconds = 86400
  receive_wait_time_seconds = 20
  
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ms_ledger_dlq.arn
    maxReceiveCount     = 5
  })
}

#Fila 2
resource "aws_sqs_queue" "ms_autorizador_dlq" {
  name = "queue-compensation-transaction-dlq"
}

resource "aws_sqs_queue" "ms_autorizador_queue" {
  name                      = "queue-compensation-transaction"
  message_retention_seconds = 86400
  receive_wait_time_seconds = 20
  
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ms_autorizador_dlq.arn
    maxReceiveCount     = 5
  })
}

#Fila 3
resource "aws_sqs_queue" "ms_formatador_dlq" {
  name = "queue-reversal-notification-dlq"
}

resource "aws_sqs_queue" "ms_formatador_queue" {
  name                      = "queue-reversal-notification"
  message_retention_seconds = 86400
  receive_wait_time_seconds = 20

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ms_formatador_dlq.arn
    maxReceiveCount     = 5
  })
}

#Fila 4 - alimentada pelo Pipe do Stream do comando_conta (ver modules/ttl-reconciliation-pipe):
# registros que expiraram pelo TTL ainda em PENDING/TIMEOUT (mensagem perdida,
# ledger caiu antes de terminar).
resource "aws_sqs_queue" "transactions_pending_dlq" {
  name = "queue-transactions-pending-dlq"
}

resource "aws_sqs_queue" "transactions_pending_queue" {
  name                      = "queue-transactions-pending"
  message_retention_seconds = 1209600
  receive_wait_time_seconds = 20

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.transactions_pending_dlq.arn
    maxReceiveCount     = 5
  })
}