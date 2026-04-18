# Criação do Network Load Balancer (NLB)
resource "aws_lb" "nlb" {
  name               = "autorizador-debito-nlb"  # Nome do Load Balancer
  internal           = false                     # Define se o NLB será interno (true) ou público (false)
  load_balancer_type = "network"                 # Define o tipo de Load Balancer como Network Load Balancer (NLB)
  security_groups    = var.security_groups       # Associa um Security Group ao NLB (não obrigatório para NLB)
  subnets            = var.public_subnets        # Lista de subnets públicas onde o NLB será criado
}

# Criação do Target Group para ECS
resource "aws_lb_target_group" "formatador" {
  name        = "formatador-tg"  # Nome do Target Group
  port        = 8080         # Porta na qual o serviço vai responder
  protocol    = "TCP"        # Protocolo usado (TCP para NLB)
  vpc_id      = var.vpc_id   # ID da VPC onde o Target Group será criado
  target_type = "ip"         # Define que os targets serão IPs (usado com Fargate)

  # Configuração da verificação de integridade (Health Check)
  health_check {
    interval            = 30      # Intervalo entre verificações de saúde (em segundos)
    path                = "/actuator/health/"     # Caminho usado para o Health Check (somente para HTTP/HTTPS)
    protocol            = "HTTP"  # Protocolo usado para o Health Check
    matcher             = "200"   # Código de resposta esperado para considerar saudável
    timeout             = 5       # Tempo máximo para resposta antes de considerar uma falha
    healthy_threshold   = 2       # Número de verificações bem-sucedidas antes de marcar como saudável
    unhealthy_threshold = 3       # Número de verificações malsucedidas antes de marcar como não saudável
  }
}

# Criação do Listener do Load Balancer
resource "aws_lb_listener" "nlb_listener_formatador" {
  load_balancer_arn = aws_lb.nlb.arn   # Referência ao Load Balancer criado acima
  port              = 8080             # Porta que o NLB escuta
  protocol          = "TCP"            # Protocolo do Listener (TCP para NLB)

  default_action {
    type             = "forward"                       # Ação padrão: encaminhar tráfego para o Target Group
    target_group_arn = aws_lb_target_group.formatador.arn  # Target Group para onde o tráfego será enviado
  }
}
