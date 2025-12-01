# Environment configuration for Production
environment        = "production"
project_name       = "ecommerce"
region             = "nyc1"  # New York
cluster_name       = "ecommerce-k8s"
kubernetes_version = "1.33.6-do.0"  # Updated to available version

# VPC Configuration
vpc_ip_range = "172.17.0.0/16"  # Different from stage, using safe 172.x range

# Node Pool Configuration
node_pool = {
  name       = "worker-pool"
  size       = "s-4vcpu-8gb"  # Larger nodes for production
  node_count = 3
  auto_scale = true
  min_nodes  = 3
  max_nodes  = 10
  tags       = ["worker", "production"]
}

# Features
enable_monitoring    = true
enable_auto_upgrade  = true   # Auto-upgrade for production
enable_surge_upgrade = true
enable_ha            = true   # High availability for production

# TLS/Certificates
enable_cert_manager = true
letsencrypt_email   = ""  # Add your email here for Let's Encrypt
domain_name         = ""  # Add your production domain here

# Tags
tags = ["ecommerce", "microservices", "production", "terraform"]
