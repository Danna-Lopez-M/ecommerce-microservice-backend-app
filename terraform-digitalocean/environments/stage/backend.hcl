# Backend configuration for Stage environment
# DigitalOcean Spaces is S3-compatible

# Spaces endpoint (replace with your region)
endpoint = "https://nyc3.digitaloceanspaces.com"

# Spaces bucket name
bucket = "ecommerce-terraform-state"

# State file key
key = "stage/terraform.tfstate"

# Region
region = "us-east-1"  # Required for S3 compatibility, but not used by Spaces

# Skip credentials validation
skip_credentials_validation = true
skip_metadata_api_check     = true
skip_region_validation      = true

# Note: Set these environment variables before running terraform:
# export AWS_ACCESS_KEY_ID="your-spaces-access-key"
# export AWS_SECRET_ACCESS_KEY="your-spaces-secret-key"
