# Install cert-manager using Helm
resource "helm_release" "cert_manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.13.3"
  namespace        = "cert-manager"
  create_namespace = true
  
  set {
    name  = "installCRDs"
    value = "true"
  }
  
  set {
    name  = "global.leaderElection.namespace"
    value = "cert-manager"
  }
}

# ClusterIssuer for Let's Encrypt (staging)
# NOTE: Uncomment after cert-manager CRDs are installed (run terraform apply twice)
# resource "kubernetes_manifest" "letsencrypt_staging" {
#   count = var.letsencrypt_email != "" ? 1 : 0
#   
#   depends_on = [helm_release.cert_manager]
#   
#   manifest = {
#     apiVersion = "cert-manager.io/v1"
#     kind       = "ClusterIssuer"
#     metadata = {
#       name = "letsencrypt-staging"
#     }
#     spec = {
#       acme = {
#         server = "https://acme-staging-v02.api.letsencrypt.org/directory"
#         email  = var.letsencrypt_email
#         privateKeySecretRef = {
#           name = "letsencrypt-staging"
#         }
#         solvers = [
#           {
#             http01 = {
#               ingress = {
#                 class = "nginx"
#               }
#             }
#           }
#         ]
#       }
#     }
#   }
# }

# ClusterIssuer for Let's Encrypt (production)
# NOTE: Uncomment after cert-manager CRDs are installed (run terraform apply twice)
# resource "kubernetes_manifest" "letsencrypt_prod" {
#   count = var.letsencrypt_email != "" ? 1 : 0
#   
#   depends_on = [helm_release.cert_manager]
#   
#   manifest = {
#     apiVersion = "cert-manager.io/v1"
#     kind       = "ClusterIssuer"
#     metadata = {
#       name = "letsencrypt-prod"
#     }
#     spec = {
#       acme = {
#         server = "https://acme-v02.api.letsencrypt.org/directory"
#         email  = var.letsencrypt_email
#         privateKeySecretRef = {
#           name = "letsencrypt-prod"
#         }
#         solvers = [
#           {
#             http01 = {
#               ingress = {
#                 class = "nginx"
#               }
#             }
#           }
#         ]
#       }
#     }
#   }
# }

# Self-signed ClusterIssuer (fallback)
# NOTE: Uncomment after cert-manager CRDs are installed (run terraform apply twice)
# resource "kubernetes_manifest" "selfsigned_issuer" {
#   depends_on = [helm_release.cert_manager]
#   
#   manifest = {
#     apiVersion = "cert-manager.io/v1"
#     kind       = "ClusterIssuer"
#     metadata = {
#       name = "selfsigned-issuer"
#     }
#     spec = {
#       selfSigned = {}
#     }
#   }
# }
