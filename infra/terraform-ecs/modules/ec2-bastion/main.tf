# Busca automaticamente a AMI mais recente do Amazon Linux 2023 na sua região atual
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }
}

resource "aws_instance" "bastion" {
  ami           = data.aws_ami.amazon_linux_2023.id # Usa o ID encontrado dinamicamente
  instance_type = "t3.micro"
  subnet_id     = var.public_subnet 
  key_name      = "key-pair-us-east-1"

  vpc_security_group_ids = [aws_security_group.bastion_sg.id]

  # Garante que a instância tenha um IP público para você conectar
  associate_public_ip_address = true 

  tags = { Name = "bastion-valkey-tunnel" }
}

# 2. Security Group do Bastion (Libera SSH para você)
resource "aws_security_group" "bastion_sg" {
  name   = "bastion-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] 
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}