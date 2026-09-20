# 1. Grupo de Subnets (Onde o Valkey vai "morar")
resource "aws_elasticache_subnet_group" "valkey_subnets" {
  name       = "valkey-subnets"
  subnet_ids = var.private_subnets
}

# 2. Security Group (Portão de entrada)
resource "aws_security_group" "valkey_sg" {
  name        = "valkey-sg"
  description = "Permitir acesso ao Valkey (sinal de conclusao da efetivacao do ledger)"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Acesso total para POC (Cuidado!)"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    cidr_blocks      = ["0.0.0.0/0"] 
    # security_groups = [] # Remova ou comente essa linha se usar cidr_blocks
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "valkey-sg"
  }
}

# 3. Cluster Valkey (Replication Group)
resource "aws_elasticache_replication_group" "valkey_cluster" {
  replication_group_id = "ledger-efetivacao-valkey"
  description          = "Sinalizacao de conclusao da efetivacao do ledger-service via Valkey"
  
  engine         = "valkey"
  engine_version = "7.2"
  node_type      = "cache.t4g.micro"
  num_cache_clusters = 1
  port           = 6379
    
  subnet_group_name  = aws_elasticache_subnet_group.valkey_subnets.name
  security_group_ids = [aws_security_group.valkey_sg.id]

  automatic_failover_enabled = false
  multi_az_enabled           = false

  apply_immediately = true
}