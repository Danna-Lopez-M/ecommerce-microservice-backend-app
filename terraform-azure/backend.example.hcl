# Backend configuration for Azure Storage
# Copy this file to backend.hcl and fill in your values
# cp backend.example.hcl backend.hcl

resource_group_name  = "rg-terraform-state"
storage_account_name = "stterraformstate"
container_name       = "terraform-state"
key                  = "ecommerce-aks/terraform.tfstate"

