variable "environment" {
  description = "Environment name"
  type        = string
}

variable "enable_tls" {
  description = "Enable TLS/SSL redirect"
  type        = bool
  default     = false
}
