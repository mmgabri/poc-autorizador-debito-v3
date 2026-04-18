# 1. Criação do Application Load Balancer (ALB)
resource "aws_lb" "alb" {
  name               = "autorizador-debito-alb"
  internal           = false                     # Público
  load_balancer_type = "application"             # Mudado de network para application
  security_groups    = var.security_groups       # OBRIGATÓRIO para ALB
  subnets            = var.public_subnets
}

# 2. Criação do Target Group para ECS (Ajustado para HTTP)
resource "aws_lb_target_group" "formatador" {
  name        = "formatador-tg"
  port        = 8080
  protocol    = "HTTP"                           # Mudado de TCP para HTTP
  vpc_id      = var.vpc_id
  target_type = "ip"                             # Mantido para Fargate/ECS

  health_check {
    enabled             = true
    interval            = 30
    path                = "/actuator/health"     # Removida a barra extra no final se necessário
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# 3. Criação do Listener do ALB (Ajustado para HTTP)
resource "aws_lb_listener" "alb_listener_formatador" {
  load_balancer_arn = aws_lb.alb.arn
  port              = 8080
  protocol          = "HTTP"                     # Mudado de TCP para HTTP

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.formatador.arn
  }
}