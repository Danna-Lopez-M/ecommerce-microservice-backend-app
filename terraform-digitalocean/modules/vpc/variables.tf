variable "name" {
  description = "VPC name"
  type        = string
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
}

variable "ip_range" {
  description = "VPC IP range in CIDR notation"
  type        = string
}

variable "description" {
  description = "VPC description"
  type        = string
  default     = ""
}
