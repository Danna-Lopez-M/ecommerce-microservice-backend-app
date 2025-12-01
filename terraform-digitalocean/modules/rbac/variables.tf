variable "environment" {
  description = "Environment name"
  type        = string
}

variable "namespaces" {
  description = "List of namespaces to create"
  type        = list(string)
  default     = []
}
