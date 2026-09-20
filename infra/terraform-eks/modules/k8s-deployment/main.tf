locals {
  service_name = "${var.name}-svc"

  datadog_labels = var.datadog_apm_enabled ? {
    "tags.datadoghq.com/service"      = var.name
    "tags.datadoghq.com/env"          = var.datadog_env
    "tags.datadoghq.com/version"      = var.datadog_version
    "admission.datadoghq.com/enabled" = "true"
  } : {}

  pod_labels = merge({ app = var.name }, local.datadog_labels)

  lb_annotations = var.service_type == "LoadBalancer" ? {
    "service.beta.kubernetes.io/aws-load-balancer-type"            = "nlb"
    "service.beta.kubernetes.io/aws-load-balancer-nlb-target-type" = "instance"
    "service.beta.kubernetes.io/aws-load-balancer-scheme"          = "internet-facing"
  } : {}
}

resource "kubernetes_deployment_v1" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels    = { app = var.name }
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = { app = var.name }
    }

    template {
      metadata {
        labels = local.pod_labels
      }

      spec {
        service_account_name = var.service_account_name

        container {
          name              = var.name
          image             = var.image
          image_pull_policy = "Always"

          port {
            name           = "rest"
            container_port = var.container_port
          }

          dynamic "port" {
            for_each = var.grpc_port == null ? [] : [var.grpc_port]
            content {
              name           = "grpc"
              container_port = port.value
            }
          }

          dynamic "env" {
            for_each = var.env
            content {
              name  = env.key
              value = env.value
            }
          }

          dynamic "env" {
            for_each = var.datadog_apm_enabled ? [1] : []
            content {
              name = "DD_AGENT_HOST"
              value_from {
                field_ref {
                  field_path = "status.hostIP"
                }
              }
            }
          }

          dynamic "env" {
            for_each = var.datadog_apm_enabled ? {
              DD_TRACE_AGENT_PORT = "8126"
              DD_DOGSTATSD_PORT   = "8125"
              DD_SERVICE          = var.name
              DD_ENV              = var.datadog_env
              DD_VERSION          = var.datadog_version
              DD_LOGS_INJECTION   = "true"
            } : {}
            content {
              name  = env.key
              value = env.value
            }
          }

          resources {
            requests = {
              cpu    = var.cpu_request
              memory = var.memory_request
            }
            limits = {
              cpu    = var.cpu_limit
              memory = var.memory_limit
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = var.container_port
            }
            initial_delay_seconds = 60
            period_seconds        = 30
            timeout_seconds       = 10
            failure_threshold     = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = var.container_port
            }
            initial_delay_seconds = 20
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "this" {
  metadata {
    name        = local.service_name
    namespace   = var.namespace
    annotations = local.lb_annotations
  }

  spec {
    selector = { app = var.name }
    type     = var.service_type

    port {
      name        = "rest"
      port        = var.container_port
      target_port = var.container_port
    }

    dynamic "port" {
      for_each = var.grpc_port == null ? [] : [var.grpc_port]
      content {
        name        = "grpc"
        port        = port.value
        target_port = port.value
      }
    }
  }
}
