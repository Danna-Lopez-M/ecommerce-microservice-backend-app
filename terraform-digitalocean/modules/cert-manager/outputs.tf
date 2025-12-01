output "cert_manager_version" {
  description = "Cert-manager version"
  value       = helm_release.cert_manager.version
}

output "cert_manager_namespace" {
  description = "Cert-manager namespace"
  value       = helm_release.cert_manager.namespace
}

output "letsencrypt_staging_issuer" {
  description = "Let's Encrypt staging issuer name"
  value       = var.letsencrypt_email != "" ? "letsencrypt-staging" : null
}

output "letsencrypt_prod_issuer" {
  description = "Let's Encrypt production issuer name"
  value       = var.letsencrypt_email != "" ? "letsencrypt-prod" : null
}

output "selfsigned_issuer" {
  description = "Self-signed issuer name"
  value       = "selfsigned-issuer"
}
