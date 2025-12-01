output "ingress_nginx_version" {
  description = "Ingress NGINX version"
  value       = helm_release.ingress_nginx.version
}

output "ingress_nginx_namespace" {
  description = "Ingress NGINX namespace"
  value       = helm_release.ingress_nginx.namespace
}

output "load_balancer_ip" {
  description = "Load Balancer external IP"
  value       = try(data.kubernetes_service.ingress_nginx.status[0].load_balancer[0].ingress[0].ip, "pending")
}

output "load_balancer_hostname" {
  description = "Load Balancer hostname"
  value       = try(data.kubernetes_service.ingress_nginx.status[0].load_balancer[0].ingress[0].hostname, "")
}
