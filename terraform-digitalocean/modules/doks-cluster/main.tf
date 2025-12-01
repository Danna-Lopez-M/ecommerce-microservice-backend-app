terraform {
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.34"
    }
  }
}

resource "digitalocean_kubernetes_cluster" "main" {
  name    = var.name
  region  = var.region
  version = var.kubernetes_version
  vpc_uuid = var.vpc_uuid
  
  # High availability control plane (optional, costs extra)
  ha = var.enable_ha
  
  # Auto-upgrade settings
  auto_upgrade = var.enable_auto_upgrade
  surge_upgrade = var.enable_surge_upgrade
  
  # Maintenance window
  maintenance_policy {
    day        = "sunday"
    start_time = "04:00"
  }
  
  # Default node pool
  node_pool {
    name       = var.node_pool.name
    size       = var.node_pool.size
    node_count = var.node_pool.node_count
    auto_scale = var.node_pool.auto_scale
    min_nodes  = var.node_pool.min_nodes
    max_nodes  = var.node_pool.max_nodes
    tags       = var.node_pool.tags
    labels     = {
      environment = var.node_pool.name
      managed-by  = "terraform"
    }
  }
  
  tags = var.tags
}
