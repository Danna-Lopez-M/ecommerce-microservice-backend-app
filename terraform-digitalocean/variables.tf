variable "do_token" {
  description = "DigitalOcean API Token"
  type        = string
  sensitive   = true
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ecommerce"
}

variable "environment" {
  description = "Environment (stage/production)"
  type        = string
  validation {
    condition     = contains(["stage", "production"], var.environment)
    error_message = "Environment must be either 'stage' or 'production'."
  }
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
  default     = "nyc1"
}

variable "cluster_name" {
  description = "Kubernetes cluster name"
  type        = string
  default     = "ecommerce-k8s"
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.33.6-do.0"
}

variable "vpc_ip_range" {
  description = "VPC IP range (CIDR notation)"
  type        = string
  default     = "172.16.0.0/16"  # Safe range, not reserved by DigitalOcean
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
  default = {
    name       = "worker-pool"
    size       = "s-2vcpu-2gb"
    node_count = 3
    auto_scale = true
    min_nodes  = 2
    max_nodes  = 5
    tags       = ["worker"]
  }
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
  description = "Enable surge upgrade (adds extra node during upgrades)"
  type        = bool
  default     = true
}

variable "enable_ha" {
  description = "Enable high availability control plane"
  type        = bool
  default     = false
}

variable "domain_name" {
  description = "Domain name for TLS certificates (optional)"
  type        = string
  default     = ""
}

variable "enable_cert_manager" {
  description = "Enable cert-manager for automatic TLS certificates"
  type        = bool
  default     = true
}

variable "letsencrypt_email" {
  description = "Email for Let's Encrypt certificate notifications"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = list(string)
  default     = ["ecommerce", "microservices", "terraform"]
}
