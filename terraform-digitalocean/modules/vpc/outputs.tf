output "id" {
  description = "VPC ID"
  value       = digitalocean_vpc.main.id
}

output "urn" {
  description = "VPC URN"
  value       = digitalocean_vpc.main.urn
}

output "name" {
  description = "VPC name"
  value       = digitalocean_vpc.main.name
}

output "ip_range" {
  description = "VPC IP range"
  value       = digitalocean_vpc.main.ip_range
}

output "region" {
  description = "VPC region"
  value       = digitalocean_vpc.main.region
}
