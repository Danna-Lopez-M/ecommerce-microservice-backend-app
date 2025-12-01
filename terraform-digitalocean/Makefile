.PHONY: help init plan apply destroy output kubeconfig clean validate fmt

# Default environment
ENV ?= stage

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)DigitalOcean Kubernetes Infrastructure$(NC)"
	@echo ""
	@echo "$(GREEN)Available targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(GREEN)Usage:$(NC)"
	@echo "  make <target> ENV=<environment>"
	@echo ""
	@echo "$(GREEN)Environments:$(NC)"
	@echo "  - stage (default)"
	@echo "  - production"

init: ## Initialize Terraform
	@echo "$(BLUE)Initializing Terraform for $(ENV)...$(NC)"
	@if [ -z "$$DO_TOKEN" ]; then \
		echo "$(RED)Error: DO_TOKEN environment variable not set$(NC)"; \
		echo "$(YELLOW)Run: export DO_TOKEN='your-digitalocean-token'$(NC)"; \
		exit 1; \
	fi
	terraform init -backend-config=environments/$(ENV)/backend.hcl

validate: ## Validate Terraform configuration
	@echo "$(BLUE)Validating Terraform configuration...$(NC)"
	terraform validate
	terraform fmt -check

fmt: ## Format Terraform files
	@echo "$(BLUE)Formatting Terraform files...$(NC)"
	terraform fmt -recursive

plan: ## Plan infrastructure changes
	@echo "$(BLUE)Planning infrastructure for $(ENV)...$(NC)"
	@if [ -z "$$DO_TOKEN" ]; then \
		echo "$(RED)Error: DO_TOKEN environment variable not set$(NC)"; \
		exit 1; \
	fi
	terraform plan \
		-var="do_token=$$DO_TOKEN" \
		-var-file=environments/$(ENV)/terraform.tfvars \
		-out=tfplan-$(ENV)

apply: ## Apply infrastructure changes
	@echo "$(BLUE)Applying infrastructure for $(ENV)...$(NC)"
	@if [ -z "$$DO_TOKEN" ]; then \
		echo "$(RED)Error: DO_TOKEN environment variable not set$(NC)"; \
		exit 1; \
	fi
	@if [ ! -f tfplan-$(ENV) ]; then \
		echo "$(YELLOW)No plan file found. Running plan first...$(NC)"; \
		$(MAKE) plan ENV=$(ENV); \
	fi
	terraform apply tfplan-$(ENV)
	@rm -f tfplan-$(ENV)
	@echo "$(GREEN)✓ Infrastructure deployed successfully!$(NC)"
	@echo ""
	@$(MAKE) output ENV=$(ENV)

destroy: ## Destroy infrastructure
	@echo "$(RED)WARNING: This will destroy all infrastructure for $(ENV)!$(NC)"
	@read -p "Are you sure? (yes/no): " confirm && [ "$$confirm" = "yes" ]
	@if [ -z "$$DO_TOKEN" ]; then \
		echo "$(RED)Error: DO_TOKEN environment variable not set$(NC)"; \
		exit 1; \
	fi
	terraform destroy \
		-var="do_token=$$DO_TOKEN" \
		-var-file=environments/$(ENV)/terraform.tfvars

output: ## Show Terraform outputs
	@echo "$(BLUE)Terraform Outputs for $(ENV):$(NC)"
	@terraform output

kubeconfig: ## Configure kubectl for the cluster
	@echo "$(BLUE)Configuring kubectl for $(ENV)...$(NC)"
	@CLUSTER_ID=$$(terraform output -raw cluster_id 2>/dev/null); \
	if [ -z "$$CLUSTER_ID" ]; then \
		echo "$(RED)Error: Cluster not found. Run 'make apply' first.$(NC)"; \
		exit 1; \
	fi; \
	doctl kubernetes cluster kubeconfig save $$CLUSTER_ID
	@echo "$(GREEN)✓ kubectl configured successfully!$(NC)"
	@echo ""
	@echo "$(YELLOW)Test connection:$(NC)"
	@kubectl get nodes

kubeconfig-manual: ## Save kubeconfig manually (without doctl)
	@echo "$(BLUE)Saving kubeconfig for $(ENV)...$(NC)"
	@terraform output -raw kubeconfig > ~/.kube/config-do-$(ENV)
	@echo "$(GREEN)✓ Kubeconfig saved to ~/.kube/config-do-$(ENV)$(NC)"
	@echo "$(YELLOW)To use it, run:$(NC)"
	@echo "  export KUBECONFIG=~/.kube/config-do-$(ENV)"

clean: ## Clean up temporary files
	@echo "$(BLUE)Cleaning up...$(NC)"
	@rm -f tfplan-*
	@rm -rf .terraform.lock.hcl
	@echo "$(GREEN)✓ Cleanup complete$(NC)"

status: ## Show cluster status
	@echo "$(BLUE)Cluster Status for $(ENV):$(NC)"
	@echo ""
	@echo "$(YELLOW)Nodes:$(NC)"
	@kubectl get nodes -o wide
	@echo ""
	@echo "$(YELLOW)Namespaces:$(NC)"
	@kubectl get namespaces
	@echo ""
	@echo "$(YELLOW)Load Balancer IP:$(NC)"
	@terraform output load_balancer_ip

logs: ## Show recent Terraform logs
	@echo "$(BLUE)Recent Terraform operations:$(NC)"
	@ls -lt terraform.tfstate* | head -5

.DEFAULT_GOAL := help
