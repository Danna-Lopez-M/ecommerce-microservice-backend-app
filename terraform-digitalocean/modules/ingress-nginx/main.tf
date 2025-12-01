# Install NGINX Ingress Controller using Helm
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = "4.8.3"
  namespace        = "ingress-nginx"
  create_namespace = true
  
  # Configure for DigitalOcean Load Balancer
  set {
    name  = "controller.service.type"
    value = "LoadBalancer"
  }
  
  set {
    name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/do-loadbalancer-name"
    value = "ecommerce-lb-${var.environment}"
  }
  
  set {
    name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/do-loadbalancer-protocol"
    value = "http"
  }
  
  # Enable SSL redirect if TLS is enabled
  set {
    name  = "controller.config.ssl-redirect"
    value = var.enable_tls ? "true" : "false"
  }
  
  set {
    name  = "controller.config.force-ssl-redirect"
    value = var.enable_tls ? "true" : "false"
  }
  
  # Resource limits
  set {
    name  = "controller.resources.requests.cpu"
    value = "100m"
  }
  
  set {
    name  = "controller.resources.requests.memory"
    value = "128Mi"
  }
  
  set {
    name  = "controller.resources.limits.cpu"
    value = "500m"
  }
  
  set {
    name  = "controller.resources.limits.memory"
    value = "512Mi"
  }
  
  # Metrics
  set {
    name  = "controller.metrics.enabled"
    value = "true"
  }
  
  # Autoscaling
  set {
    name  = "controller.autoscaling.enabled"
    value = "true"
  }
  
  set {
    name  = "controller.autoscaling.minReplicas"
    value = "2"
  }
  
  set {
    name  = "controller.autoscaling.maxReplicas"
    value = "5"
  }
}

# Data source to get the Load Balancer IP
data "kubernetes_service" "ingress_nginx" {
  metadata {
    name      = "ingress-nginx-controller"
    namespace = "ingress-nginx"
  }
  
  depends_on = [helm_release.ingress_nginx]
}
