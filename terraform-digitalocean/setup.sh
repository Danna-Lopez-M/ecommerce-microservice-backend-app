#!/bin/bash

set -e

echo "=========================================="
echo "DigitalOcean Infrastructure Setup"
echo "=========================================="
echo ""

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if DO_TOKEN is set
if [ -z "$DO_TOKEN" ]; then
    echo -e "${RED}Error: DO_TOKEN environment variable not set${NC}"
    echo -e "${YELLOW}Please set your DigitalOcean API token:${NC}"
    echo "  export DO_TOKEN='your-digitalocean-token'"
    echo ""
    echo "Get your token from: https://cloud.digitalocean.com/account/api/tokens"
    exit 1
fi

# Check if doctl is installed
if ! command -v doctl &> /dev/null; then
    echo -e "${YELLOW}doctl not found. Installing...${NC}"
    
    if command -v snap &> /dev/null; then
        sudo snap install doctl
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        brew install doctl
    else
        echo -e "${RED}Please install doctl manually:${NC}"
        echo "  https://docs.digitalocean.com/reference/doctl/how-to/install/"
        exit 1
    fi
fi

# Authenticate doctl
echo -e "${BLUE}Authenticating doctl...${NC}"
doctl auth init --access-token "$DO_TOKEN"

# Check if Terraform is installed
if ! command -v terraform &> /dev/null; then
    echo -e "${RED}Error: Terraform not installed${NC}"
    echo "Please install Terraform: https://www.terraform.io/downloads"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}"
echo ""

# Ask for environment
echo -e "${BLUE}Select environment:${NC}"
echo "  1) stage (default)"
echo "  2) production"
read -p "Enter choice [1]: " env_choice
env_choice=${env_choice:-1}

if [ "$env_choice" = "2" ]; then
    ENV="production"
else
    ENV="stage"
fi

echo -e "${BLUE}Selected environment: ${YELLOW}$ENV${NC}"
echo ""

# Ask for email (for Let's Encrypt)
read -p "Enter your email for Let's Encrypt (optional): " letsencrypt_email

if [ -n "$letsencrypt_email" ]; then
    echo -e "${BLUE}Updating terraform.tfvars with your email...${NC}"
    sed -i "s/letsencrypt_email   = \"\"/letsencrypt_email   = \"$letsencrypt_email\"/" \
        environments/$ENV/terraform.tfvars
fi

# Initialize Terraform
echo -e "${BLUE}Initializing Terraform...${NC}"
terraform init -backend-config=environments/$ENV/backend.hcl

# Validate configuration
echo -e "${BLUE}Validating configuration...${NC}"
terraform validate

# Format files
terraform fmt -recursive

# Plan
echo -e "${BLUE}Planning infrastructure...${NC}"
terraform plan \
    -var="do_token=$DO_TOKEN" \
    -var-file=environments/$ENV/terraform.tfvars \
    -out=tfplan-$ENV

echo ""
echo -e "${GREEN}=========================================="
echo "Setup complete!"
echo "==========================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Review the plan above"
echo "  2. Run: make apply ENV=$ENV"
echo "  3. Configure kubectl: make kubeconfig ENV=$ENV"
echo ""
echo -e "${YELLOW}Or use the Makefile:${NC}"
echo "  make help"
