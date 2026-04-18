resource "aws_cloudwatch_log_group" "logs" {
  for_each = toset(var.micro_services)

  name = "/ecs/${each.value}-logs"
}