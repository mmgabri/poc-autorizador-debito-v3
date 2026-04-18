
output "ecs_sg_id" {
  description = "ID do Security Group ECS"
  value       = aws_security_group.ecs_sg.id
}