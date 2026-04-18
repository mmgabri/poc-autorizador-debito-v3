#------------------------------------------------------------------------------
# AWS Service Discovery (Cloud Map)
#------------------------------------------------------------------------------
resource "aws_service_discovery_service" "service_discovery" {
  name = "${var.micro_service_name}-service"

  dns_config {
    namespace_id = var.namespace_id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

#------------------------------------------------------------------------------
# AWS Task Definition
#------------------------------------------------------------------------------
resource "aws_ecs_task_definition" "service_task" {
  family = "${var.micro_service_name}-task"
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  cpu = var.cpu
  memory = var.memory
  execution_role_arn = var.execution_role_arn
  task_role_arn = var.task_role_arn

  container_definitions = jsonencode([
    {
      name = var.micro_service_name
      image = "${var.ecr_repository}:latest"
      portMappings = [
        {
          containerPort = var.container_port
          hostPort = var.host_port
        }
      ]
      environment = [
          { name = "SLEEP", value = "100" },
          { name = "REDIS_HOST", value = var.redis_host },
          { name = "LOGGING_LEVEL", value = "DEBUG" }
        ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group" = "/ecs/${var.micro_service_name}-logs"
          "awslogs-region" = var.region
          "awslogs-stream-prefix" = var.micro_service_name
        }
      }
      healthCheck = {
        command = [
          "CMD-SHELL",
          "curl --silent --fail http://localhost:${var.container_port}/actuator/health || exit 1"
        ]
        interval = 30
        retries = 3
        startPeriod = 60
        timeout = 10
      }
    }
  ])
}


#------------------------------------------------------------------------------
# AWS Service
#------------------------------------------------------------------------------
resource "aws_ecs_service" "service_task" {
  name          = "${var.micro_service_name}-service"
  cluster = var.ecs_cluster_name
  task_definition = aws_ecs_task_definition.service_task.arn
  launch_type = "FARGATE"
  desired_count = var.desired_count

  network_configuration {
    subnets = var.private_subnets
    security_groups = var.security_groups
    assign_public_ip = true
  }

  dynamic "load_balancer" {
    for_each = length(var.target_group_arn) > 0 ? [var.target_group_arn[0]] : []
    content {
      target_group_arn = var.target_group_arn[0]
      container_name   = var.micro_service_name
      container_port   = var.container_port
    }
  }

  service_registries {
    registry_arn = aws_service_discovery_service.service_discovery.arn
  }
}