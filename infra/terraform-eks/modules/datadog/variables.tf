variable "namespace" {
  type = string
}

variable "datadog_api_key" {
  type      = string
  sensitive = true
}

variable "datadog_site" {
  type    = string
  default = "us5.datadoghq.com"
}

variable "datadog_env" {
  type    = string
  default = "producao"
}
