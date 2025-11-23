terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
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
  
  # Backend configuration is provided via -backend-config flag
  # Each environment has its own backend.hcl file with separate state keys
  # This allows for isolated state management per environment
  backend "azurerm" {
    # Values are provided via -backend-config=environments/{env}/backend.hcl
  }
}

