#!/bin/bash

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Default environment
ENV=${1:-stage}

# Validate environment
if [[ ! "$ENV" =~ ^(stage|production)$ ]]; then
    echo -e "${RED}❌ Error: Environment must be 'stage' or 'production'${NC}"
    echo "Usage: $0 [stage|production]"
    exit 1
fi

# Paths
TFVARS="environments/${ENV}/terraform.tfvars"
BACKEND_CONFIG="environments/${ENV}/backend.hcl"

# Check if files exist
if [ ! -f "$TFVARS" ]; then
    echo -e "${RED}❌ Error: Terraform vars file not found: $TFVARS${NC}"
    exit 1
fi

if [ ! -f "$BACKEND_CONFIG" ]; then
    echo -e "${RED}❌ Error: Backend config file not found: $BACKEND_CONFIG${NC}"
    exit 1
fi

echo -e "${BLUE}🚀 Deploying infrastructure for environment: ${ENV}${NC}"
echo -e "${BLUE}=============================================${NC}"

# Check Azure CLI
if ! command -v az &> /dev/null; then
    echo -e "${RED}❌ Azure CLI not found. Please install Azure CLI${NC}"
    exit 1
fi

# Check Azure login
echo -e "${YELLOW}🔍 Checking Azure authentication...${NC}"
if ! az account show &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not logged in to Azure. Please run: az login${NC}"
    exit 1
fi

SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo -e "${GREEN}✅ Azure authentication OK${NC}"
echo -e "${BLUE}Subscription ID: ${SUBSCRIPTION_ID}${NC}"

# Check Terraform
if ! command -v terraform &> /dev/null; then
    echo -e "${RED}❌ Terraform not found. Please install Terraform${NC}"
    exit 1
fi

TERRAFORM_VERSION=$(terraform version -json | jq -r '.terraform_version')
echo -e "${GREEN}✅ Terraform found: ${TERRAFORM_VERSION}${NC}"

# Initialize Terraform
echo -e "${BLUE}📦 Initializing Terraform...${NC}"
terraform init -backend-config="$BACKEND_CONFIG"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Terraform initialization failed${NC}"
    exit 1
fi

# Validate configuration
echo -e "${BLUE}✅ Validating Terraform configuration...${NC}"
terraform validate

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Terraform validation failed${NC}"
    exit 1
fi

# Format check
echo -e "${BLUE}🔍 Checking Terraform formatting...${NC}"
terraform fmt -check -recursive

# Plan
echo -e "${BLUE}📋 Planning infrastructure changes...${NC}"
terraform plan -var-file="$TFVARS" -out="tfplan-${ENV}"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Terraform plan failed${NC}"
    exit 1
fi

# Ask for confirmation
echo -e "${YELLOW}⚠️  Review the plan above.${NC}"
read -p "Do you want to apply these changes? [y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Deployment cancelled${NC}"
    exit 0
fi

# Apply
echo -e "${BLUE}🚀 Applying infrastructure changes...${NC}"
terraform apply "tfplan-${ENV}"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Terraform apply failed${NC}"
    exit 1
fi

# Show outputs
echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo -e "${BLUE}📊 Infrastructure outputs:${NC}"
terraform output

# Get kubeconfig
echo -e "${BLUE}📝 Getting kubeconfig...${NC}"
KUBECONFIG_PATH="$HOME/.kube/config-aks-${ENV}"
terraform output -raw kube_config > "$KUBECONFIG_PATH" 2>/dev/null || echo -e "${YELLOW}⚠️  Could not get kubeconfig from Terraform output${NC}"

if [ -f "$KUBECONFIG_PATH" ]; then
    echo -e "${GREEN}✅ Kubeconfig saved to: ${KUBECONFIG_PATH}${NC}"
    echo -e "${BLUE}To use: export KUBECONFIG=${KUBECONFIG_PATH}${NC}"
fi

echo -e "${GREEN}🎉 All done!${NC}"
