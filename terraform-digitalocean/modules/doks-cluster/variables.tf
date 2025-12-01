variable "name" {
  description = "Kubernetes cluster name"
  type        = string
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
}

variable "vpc_uuid" {
  description = "VPC UUID to attach the cluster to"
  type        = string
}

variable "node_pool" {
  description = "Default node pool configuration"
  type = object({
    name       = string
    size       = string
    node_count = number
    auto_scale = bool
    min_nodes  = number
    max_nodes  = number
    tags       = list(string)
  })
}

variable "enable_monitoring" {
  description = "Enable DigitalOcean monitoring"
  type        = bool
  default     = true
}

variable "enable_auto_upgrade" {
  description = "Enable automatic Kubernetes version upgrades"
  type        = bool
  default     = false
}

variable "enable_surge_upgrade" {
  description = "Enable surge upgrade"
  type        = bool
  default     = true
}

variable "enable_ha" {
  description = "Enable high availability control plane"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags to apply to the cluster"
  type        = list(string)
  default     = []
}
