resource "kubernetes_secret" "datadog_api_key" {
  metadata {
    name      = "datadog-secret"
    namespace = var.namespace
  }

  data = {
    api-key = var.datadog_api_key
  }
}

resource "helm_release" "datadog" {
  name       = "datadog"
  repository = "https://helm.datadoghq.com"
  chart      = "datadog"
  namespace  = var.namespace

  values = [
    yamlencode({
      datadog = {
        site                 = var.datadog_site
        apiKeyExistingSecret = kubernetes_secret.datadog_api_key.metadata[0].name
        logLevel             = "INFO"
        apm = {
          portEnabled = true
        }
        logs = {
          enabled             = true
          containerCollectAll = true
        }
        dogstatsd = {
          nonLocalTraffic = true
        }
        env = [
          {
            name  = "DD_ENV"
            value = var.datadog_env
          }
        ]
      }
      agents = {
        containers = {
          agent = {
            env = [
              {
                name  = "DD_HISTOGRAM_PERCENTILES"
                value = "0.99 0.95 0.90 0.50"
              }
            ]
          }
        }
      }
      clusterAgent = {
        enabled = true
        metricsProvider = {
          enabled = false
        }
      }
    })
  ]
}
