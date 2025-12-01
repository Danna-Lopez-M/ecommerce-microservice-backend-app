# Environment configuration for Stage
# Updated: Testing Infracost cost estimation
environment        = "stage"
project_name       = "ecommerce"
region             = "nyc1"  # New York
cluster_name       = "ecommerce-k8s"
kubernetes_version = "1.33.6-do.0"  # Updated to available version

# VPC Configuration
vpc_ip_range = "172.16.0.0/16"  # Using 172.16.x.x range (safe, not reserved by DO)

# Node Pool Configuration
node_pool = {
  name       = "worker-pool"
  size       = "s-4vcpu-8gb"  # Upgraded from s-2vcpu-2gb for more capacity
  node_count = 3
  auto_scale = true
  min_nodes  = 2
  max_nodes  = 5
  tags       = ["worker", "stage"]
}

# Features
enable_monitoring    = true
enable_auto_upgrade  = false  # Manual upgrades for stage
enable_surge_upgrade = true
enable_ha            = false  # HA costs extra, not needed for stage

# TLS/Certificates
enable_cert_manager = true
letsencrypt_email   = ""  # Add your email here for Let's Encrypt
domain_name         = ""  # Add your domain here if you have one

# Tags
tags = ["ecommerce", "microservices", "stage", "terraform"]
