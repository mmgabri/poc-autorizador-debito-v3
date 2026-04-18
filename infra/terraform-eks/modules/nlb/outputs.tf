output "nlb_arn" {
  description = "ARN do NLB"
  value       = aws_lb.nlb.arn
}

output "target_group_arn" {
  description = "ARN do Target Group"
  value       = aws_lb_target_group.formatador.arn
}