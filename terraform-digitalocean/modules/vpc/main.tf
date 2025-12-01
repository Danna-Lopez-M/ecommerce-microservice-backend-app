terraform {
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.34"
    }
  }
}

resource "digitalocean_vpc" "main" {
  name        = var.name
  region      = var.region
  ip_range    = var.ip_range
  description = var.description
}
