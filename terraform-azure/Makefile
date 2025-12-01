.PHONY: help init plan apply destroy output kubeconfig validate fmt

ENV ?= stage
TFVARS := environments/$(ENV)/terraform.tfvars
BACKEND_CONFIG := environments/$(ENV)/backend.hcl

# Validate environment
ifeq ($(filter $(ENV),stage production),)
  $(error ENV must be either 'stage' or 'production')
endif

help:
	@echo "Terraform Makefile for Kubernetes Cluster"
	@echo ""
	@echo "Usage:"
	@echo "  make init          Initialize Terraform (ENV=stage|production)"
	@echo "  make plan          Plan changes (ENV=stage|production)"
	@echo "  make apply         Apply changes (ENV=stage|production)"
	@echo "  make destroy       Destroy infrastructure (ENV=stage|production)"
	@echo "  make output        Show outputs"
	@echo "  make kubeconfig    Get kubeconfig"
	@echo "  make validate      Validate Terraform configuration"
	@echo "  make fmt           Format Terraform files"
	@echo ""
	@echo "Examples:"
	@echo "  make init ENV=stage"
	@echo "  make plan ENV=production"
	@echo "  make apply ENV=stage"

init:
	@echo "Initializing Terraform for environment: $(ENV)"
	@if [ ! -f "$(BACKEND_CONFIG)" ]; then \
		echo "Error: Backend config file not found: $(BACKEND_CONFIG)"; \
		exit 1; \
	fi
	@if [ ! -f "$(TFVARS)" ]; then \
		echo "Error: Terraform vars file not found: $(TFVARS)"; \
		exit 1; \
	fi
	terraform init -backend-config=$(BACKEND_CONFIG)

plan:
	@echo "Planning for environment: $(ENV)"
	@if [ ! -f "$(TFVARS)" ]; then \
		echo "Error: Terraform vars file not found: $(TFVARS)"; \
		exit 1; \
	fi
	terraform plan -var-file=$(TFVARS) -out=tfplan-$(ENV)

apply:
	@echo "Applying for environment: $(ENV)"
	@if [ ! -f "tfplan-$(ENV)" ]; then \
		echo "No plan file found. Running plan first..."; \
		$(MAKE) plan ENV=$(ENV); \
	fi
	terraform apply tfplan-$(ENV)

destroy:
	@echo "WARNING: This will destroy all infrastructure for $(ENV)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		terraform destroy -var-file=$(TFVARS); \
	fi

output:
	terraform output

kubeconfig:
	@echo "Getting kubeconfig for $(ENV)..."
	@terraform output -raw kube_config > ~/.kube/config-aks-$(ENV)
	@echo "Kubeconfig saved to ~/.kube/config-aks-$(ENV)"
	@echo "To use: export KUBECONFIG=~/.kube/config-aks-$(ENV)"

fmt:
	terraform fmt -recursive

validate:
	terraform validate

clean:
	rm -rf .terraform
	rm -f .terraform.lock.hcl

