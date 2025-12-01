output "cluster_id" {
  description = "Kubernetes cluster ID"
  value       = module.kubernetes_cluster.id
}

output "cluster_name" {
  description = "Kubernetes cluster name"
  value       = module.kubernetes_cluster.name
}

output "cluster_endpoint" {
  description = "Kubernetes cluster endpoint"
  value       = module.kubernetes_cluster.endpoint
  sensitive   = true
}

output "cluster_region" {
  description = "Kubernetes cluster region"
  value       = module.kubernetes_cluster.region
}

output "cluster_version" {
  description = "Kubernetes cluster version"
  value       = module.kubernetes_cluster.version
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.id
}

output "vpc_urn" {
  description = "VPC URN"
  value       = module.vpc.urn
}

output "kubeconfig" {
  description = "Kubernetes config file contents"
  value       = module.kubernetes_cluster.kubeconfig
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "Cluster CA certificate"
  value       = module.kubernetes_cluster.cluster_ca_certificate
  sensitive   = true
}

output "node_pool_id" {
  description = "Default node pool ID"
  value       = module.kubernetes_cluster.node_pool_id
}

output "load_balancer_ip" {
  description = "Load balancer external IP (available after ingress deployment)"
  value       = try(module.ingress_nginx.load_balancer_ip, "Not yet assigned")
}

output "cert_manager_installed" {
  description = "Whether cert-manager is installed"
  value       = var.enable_cert_manager
}

output "environment" {
  description = "Current environment"
  value       = var.environment
}

output "connection_command" {
  description = "Command to configure kubectl"
  value       = "doctl kubernetes cluster kubeconfig save ${module.kubernetes_cluster.id}"
}

output "next_steps" {
  description = "Next steps after cluster creation"
  value       = <<-EOT
    Cluster created successfully!
    
    Next steps:
    1. Configure kubectl: export KUBECONFIG=~/.kube/config-do-stage
    2. Verify cluster: kubectl get nodes
    3. Deploy microservices: cd ../k8s && ./deploy-stage.sh
  EOT
}
