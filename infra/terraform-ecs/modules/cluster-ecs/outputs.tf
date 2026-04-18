output "ecs_cluster_autorizador" {
  description = "Nome do cluster ECS"
  value       = aws_ecs_cluster.fargate_cluster.name
}