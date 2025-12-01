output "id" {
  description = "Kubernetes cluster ID"
  value       = digitalocean_kubernetes_cluster.main.id
}

output "name" {
  description = "Kubernetes cluster name"
  value       = digitalocean_kubernetes_cluster.main.name
}

output "endpoint" {
  description = "Kubernetes API endpoint"
  value       = digitalocean_kubernetes_cluster.main.endpoint
}

output "region" {
  description = "Cluster region"
  value       = digitalocean_kubernetes_cluster.main.region
}

output "version" {
  description = "Kubernetes version"
  value       = digitalocean_kubernetes_cluster.main.version
}

output "cluster_subnet" {
  description = "Cluster subnet CIDR"
  value       = digitalocean_kubernetes_cluster.main.cluster_subnet
}

output "service_subnet" {
  description = "Service subnet CIDR"
  value       = digitalocean_kubernetes_cluster.main.service_subnet
}

output "ipv4_address" {
  description = "Cluster IPv4 address"
  value       = digitalocean_kubernetes_cluster.main.ipv4_address
}

output "kubeconfig" {
  description = "Kubernetes config file contents"
  value       = digitalocean_kubernetes_cluster.main.kube_config[0].raw_config
  sensitive   = true
}

output "token" {
  description = "Kubernetes authentication token"
  value       = digitalocean_kubernetes_cluster.main.kube_config[0].token
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "Cluster CA certificate"
  value       = digitalocean_kubernetes_cluster.main.kube_config[0].cluster_ca_certificate
  sensitive   = true
}

output "node_pool_id" {
  description = "Default node pool ID"
  value       = digitalocean_kubernetes_cluster.main.node_pool[0].id
}

output "status" {
  description = "Cluster status"
  value       = digitalocean_kubernetes_cluster.main.status
}
