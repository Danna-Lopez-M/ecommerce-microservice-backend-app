output "microservices_service_account" {
  description = "Microservices service account name"
  value       = kubernetes_service_account.microservices.metadata[0].name
}

output "monitoring_service_account" {
  description = "Monitoring service account name"
  value       = kubernetes_service_account.monitoring.metadata[0].name
}

output "namespaces" {
  description = "Created namespaces"
  value       = [for ns in kubernetes_namespace.namespaces : ns.metadata[0].name]
}
