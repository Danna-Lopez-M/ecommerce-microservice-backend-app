provider "digitalocean" {
  token = var.do_token
}

# Data sources
data "digitalocean_kubernetes_versions" "available" {
  version_prefix = "1.31."
}

# VPC Module
module "vpc" {
  source = "./modules/vpc"
  
  name        = "${var.project_name}-vpc-${var.environment}"
  region      = var.region
  ip_range    = var.vpc_ip_range
  description = "VPC for ${var.project_name} ${var.environment} environment"
}

# DOKS Cluster Module
module "kubernetes_cluster" {
  source = "./modules/doks-cluster"
  
  name                = "${var.cluster_name}-${var.environment}"
  region              = var.region
  kubernetes_version  = var.kubernetes_version
  vpc_uuid            = module.vpc.id
  
  # Node pool configuration
  node_pool           = var.node_pool
  
  # Features
  enable_monitoring   = var.enable_monitoring
  enable_auto_upgrade = var.enable_auto_upgrade
  enable_surge_upgrade = var.enable_surge_upgrade
  enable_ha           = var.enable_ha
  
  tags                = concat(var.tags, [var.environment])
}

# Configure Kubernetes provider with cluster credentials
# NOTE: Comment this out for initial deployment, uncomment after cluster is created
provider "kubernetes" {
  host  = module.kubernetes_cluster.endpoint
  token = module.kubernetes_cluster.token
  cluster_ca_certificate = base64decode(
    module.kubernetes_cluster.cluster_ca_certificate
  )
}

provider "helm" {
  kubernetes {
    host  = module.kubernetes_cluster.endpoint
    token = module.kubernetes_cluster.token
    cluster_ca_certificate = base64decode(
      module.kubernetes_cluster.cluster_ca_certificate
    )
  }
}

# RBAC Module
# NOTE: Uncomment after cluster is created and providers are configured
module "rbac" {
  source = "./modules/rbac"
  
  depends_on = [module.kubernetes_cluster]
  
  environment = var.environment
  namespaces  = ["ecommerce-${var.environment}", "monitoring", "ingress-nginx"]
}

# Cert Manager Module (for TLS)
# NOTE: Uncomment after cluster is created and providers are configured
module "cert_manager" {
  source = "./modules/cert-manager"
  
  count = var.enable_cert_manager ? 1 : 0
  
  depends_on = [module.kubernetes_cluster]
  
  letsencrypt_email = var.letsencrypt_email
  environment       = var.environment
  domain_name       = var.domain_name
}

# Ingress NGINX Controller
# NOTE: Uncomment after cluster is created and providers are configured
module "ingress_nginx" {
  source = "./modules/ingress-nginx"
  
  depends_on = [module.kubernetes_cluster]
  
  environment = var.environment
  enable_tls  = var.enable_cert_manager
}
