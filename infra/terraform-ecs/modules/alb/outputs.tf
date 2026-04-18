output "alb_arn" {
  description = "ARN do Application Load Balancer (ALB)"
  value       = aws_lb.alb.arn
}

output "alb_dns_name" {
  description = "DNS publico do ALB para acessar a aplicação"
  value       = aws_lb.alb.dns_name
}

output "target_group_arn" {
  description = "ARN do Target Group"
  value       = aws_lb_target_group.formatador.arn
}