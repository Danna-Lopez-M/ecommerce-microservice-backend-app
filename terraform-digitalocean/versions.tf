terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.34"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  # Backend configuration for remote state
  # Values provided via -backend-config flag
  # Commented out for local development - uncomment when ready to use remote state
  # backend "s3" {
  #   # DigitalOcean Spaces is S3-compatible
  #   # Configuration in environments/{env}/backend.hcl
  # }
}
