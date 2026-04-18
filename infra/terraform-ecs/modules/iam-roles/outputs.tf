output "ecs_execution_role_arn" {
  description = "ARN da Role de Execução"
  value       = aws_iam_role.ecs_execution_role.arn
}

output "ecs_task_role_arn" {
  description = "ARN da Role de Tarefa"
  value       = aws_iam_role.ecs_task_role.arn
}