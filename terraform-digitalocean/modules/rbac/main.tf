# Create namespaces
resource "kubernetes_namespace" "namespaces" {
  for_each = toset(var.namespaces)
  
  metadata {
    name = each.value
    labels = {
      environment = var.environment
      managed-by  = "terraform"
    }
  }
}

# Service Account for microservices
resource "kubernetes_service_account" "microservices" {
  metadata {
    name      = "microservices-sa"
    namespace = "ecommerce-${var.environment}"
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}

# Role for microservices (namespace-scoped)
resource "kubernetes_role" "microservices" {
  metadata {
    name      = "microservices-role"
    namespace = "ecommerce-${var.environment}"
  }
  
  # Allow reading ConfigMaps and Secrets
  rule {
    api_groups = [""]
    resources  = ["configmaps", "secrets"]
    verbs      = ["get", "list", "watch"]
  }
  
  # Allow reading Services
  rule {
    api_groups = [""]
    resources  = ["services"]
    verbs      = ["get", "list"]
  }
  
  # Allow reading Pods (for service discovery)
  rule {
    api_groups = [""]
    resources  = ["pods"]
    verbs      = ["get", "list", "watch"]
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}

# RoleBinding for microservices
resource "kubernetes_role_binding" "microservices" {
  metadata {
    name      = "microservices-rolebinding"
    namespace = "ecommerce-${var.environment}"
  }
  
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = kubernetes_role.microservices.metadata[0].name
  }
  
  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.microservices.metadata[0].name
    namespace = "ecommerce-${var.environment}"
  }
}

# ClusterRole for monitoring (read-only access to metrics)
resource "kubernetes_cluster_role" "monitoring_reader" {
  metadata {
    name = "monitoring-reader-${var.environment}"
  }
  
  rule {
    api_groups = [""]
    resources  = ["nodes", "nodes/stats", "nodes/metrics", "services", "endpoints", "pods"]
    verbs      = ["get", "list", "watch"]
  }
  
  rule {
    api_groups = ["apps"]
    resources  = ["deployments", "daemonsets", "replicasets", "statefulsets"]
    verbs      = ["get", "list", "watch"]
  }
}

# Service Account for monitoring
resource "kubernetes_service_account" "monitoring" {
  metadata {
    name      = "monitoring-sa"
    namespace = "monitoring"
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}

# ClusterRoleBinding for monitoring
resource "kubernetes_cluster_role_binding" "monitoring_reader" {
  metadata {
    name = "monitoring-reader-binding-${var.environment}"
  }
  
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = kubernetes_cluster_role.monitoring_reader.metadata[0].name
  }
  
  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.monitoring.metadata[0].name
    namespace = "monitoring"
  }
}

# Network Policies for namespace isolation
resource "kubernetes_network_policy" "default_deny_ingress" {
  metadata {
    name      = "default-deny-ingress"
    namespace = "ecommerce-${var.environment}"
  }
  
  spec {
    pod_selector {}
    policy_types = ["Ingress"]
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}

resource "kubernetes_network_policy" "allow_same_namespace" {
  metadata {
    name      = "allow-same-namespace"
    namespace = "ecommerce-${var.environment}"
  }
  
  spec {
    pod_selector {}
    
    ingress {
      from {
        pod_selector {}
      }
    }
    
    policy_types = ["Ingress"]
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}

resource "kubernetes_network_policy" "allow_ingress_traffic" {
  metadata {
    name      = "allow-ingress-traffic"
    namespace = "ecommerce-${var.environment}"
  }
  
  spec {
    pod_selector {
      match_labels = {
        "app.kubernetes.io/component" = "gateway"
      }
    }
    
    ingress {
      from {
        namespace_selector {
          match_labels = {
            name = "ingress-nginx"
          }
        }
      }
    }
    
    policy_types = ["Ingress"]
  }
  
  depends_on = [kubernetes_namespace.namespaces]
}
