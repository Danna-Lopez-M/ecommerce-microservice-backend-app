# DigitalOcean Kubernetes Infrastructure

Infraestructura como código (IaC) para desplegar microservicios de e-commerce en DigitalOcean Kubernetes (DOKS) usando Terraform.

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Inicio Rápido](#inicio-rápido)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Configuración](#configuración)
- [Despliegue](#despliegue)
- [RBAC y Seguridad](#rbac-y-seguridad)
- [TLS/Certificados](#tlscertificados)
- [Múltiples Ambientes](#múltiples-ambientes)
- [Costos](#costos)
- [Troubleshooting](#troubleshooting)

## 🏗️ Arquitectura

### Diagrama de Infraestructura

```mermaid
graph TB
    subgraph "DigitalOcean Cloud"
        subgraph "VPC 10.10.0.0/16"
            subgraph "DOKS Cluster"
                LB[Load Balancer<br/>DigitalOcean LB]
                
                subgraph "Ingress Layer"
                    NGINX[NGINX Ingress<br/>Controller]
                end
                
                subgraph "Application Layer"
                    API[API Gateway]
                    USER[User Service]
                    PROD[Product Service]
                    ORDER[Order Service]
                    PAY[Payment Service]
                    SHIP[Shipping Service]
                    FAV[Favourite Service]
                    PROXY[Proxy Client]
                end
                
                subgraph "Infrastructure Layer"
                    SD[Service Discovery<br/>Eureka]
                    CONFIG[Cloud Config]
                    ZIPKIN[Zipkin<br/>Tracing]
                end
                
                subgraph "Data Layer"
                    MYSQL[(MySQL<br/>StatefulSet)]
                end
                
                subgraph "Security Layer"
                    CM[Cert Manager<br/>Let's Encrypt]
                    RBAC[RBAC Policies<br/>Network Policies]
                end
            end
        end
        
        SPACES[DigitalOcean Spaces<br/>Terraform State]
    end
    
    INTERNET((Internet)) --> LB
    LB --> NGINX
    NGINX --> API
    API --> USER
    API --> PROD
    API --> ORDER
    API --> PAY
    API --> SHIP
    API --> FAV
    
    USER -.-> SD
    PROD -.-> SD
    ORDER -.-> SD
    PAY -.-> SD
    SHIP -.-> SD
    FAV -.-> SD
    
    USER -.-> CONFIG
    PROD -.-> CONFIG
    
    USER --> MYSQL
    PROD --> MYSQL
    ORDER --> MYSQL
    FAV --> MYSQL
    
    CM -.-> NGINX
    RBAC -.-> API
    
    style LB fill:#0080ff
    style NGINX fill:#009900
    style CM fill:#ff6600
    style RBAC fill:#cc0000
    style MYSQL fill:#4479a1
    style SPACES fill:#0080ff
```

### Diagrama de Flujo de Despliegue

```mermaid
flowchart TD
    START([Inicio]) --> CHECK{¿Terraform<br/>instalado?}
    CHECK -->|No| INSTALL[Instalar Terraform]
    CHECK -->|Sí| TOKEN{¿DO_TOKEN<br/>configurado?}
    INSTALL --> TOKEN
    
    TOKEN -->|No| SETTOKEN[export DO_TOKEN=xxx]
    TOKEN -->|Sí| SETUP[./setup.sh]
    SETTOKEN --> SETUP
    
    SETUP --> INIT[terraform init]
    INIT --> PLAN[terraform plan]
    PLAN --> REVIEW{¿Revisar<br/>plan?}
    
    REVIEW -->|Cambios| MODIFY[Modificar tfvars]
    REVIEW -->|OK| APPLY[terraform apply]
    MODIFY --> PLAN
    
    APPLY --> WAIT[Esperar creación<br/>del clúster<br/>~10 minutos]
    WAIT --> KUBE[Configurar kubectl]
    KUBE --> DEPLOY[Desplegar<br/>microservicios]
    DEPLOY --> VERIFY[Verificar pods]
    VERIFY --> END([Fin])
    
    style START fill:#90EE90
    style END fill:#90EE90
    style APPLY fill:#FFD700
    style WAIT fill:#FFA500
```

### Diagrama de Componentes

```mermaid
graph LR
    subgraph "Terraform Modules"
        VPC[VPC Module<br/>Network Isolation]
        DOKS[DOKS Module<br/>Kubernetes Cluster]
        RBAC_MOD[RBAC Module<br/>Security Policies]
        CERT[Cert Manager<br/>TLS Automation]
        ING[Ingress NGINX<br/>Load Balancing]
    end
    
    subgraph "Environments"
        STAGE[Stage<br/>3 nodes<br/>s-2vcpu-2gb]
        PROD[Production<br/>3 nodes<br/>s-4vcpu-8gb<br/>HA enabled]
    end
    
    subgraph "Backend"
        SPACES_BACKEND[DigitalOcean Spaces<br/>S3-compatible<br/>Remote State]
    end
    
    VPC --> DOKS
    DOKS --> RBAC_MOD
    DOKS --> CERT
    DOKS --> ING
    
    STAGE -.uses.-> VPC
    STAGE -.uses.-> DOKS
    PROD -.uses.-> VPC
    PROD -.uses.-> DOKS
    
    STAGE -.stores state.-> SPACES_BACKEND
    PROD -.stores state.-> SPACES_BACKEND
    
    style VPC fill:#e1f5ff
    style DOKS fill:#b3e5fc
    style RBAC_MOD fill:#ffccbc
    style CERT fill:#c8e6c9
    style ING fill:#f8bbd0
```

## 📦 Requisitos

### Software Requerido

- **Terraform** >= 1.5.0
- **doctl** (DigitalOcean CLI) - Opcional pero recomendado
- **kubectl** >= 1.28
- **make** (para usar el Makefile)

### Cuenta de DigitalOcean

1. Crear cuenta en [DigitalOcean](https://www.digitalocean.com/)
2. Aplicar para créditos de estudiante (opcional): [GitHub Student Pack](https://www.digitalocean.com/github-students)
3. Generar API Token: [API Tokens](https://cloud.digitalocean.com/account/api/tokens)

## 🚀 Inicio Rápido

### 1. Configurar Token de API

```bash
export DO_TOKEN="tu-token-de-digitalocean"
```

### 2. Ejecutar Setup Automático

```bash
cd terraform-digitalocean
./setup.sh
```

### 3. Aplicar Infraestructura

```bash
make apply ENV=stage
```

### 4. Configurar kubectl

```bash
make kubeconfig ENV=stage
```

### 5. Verificar Clúster

```bash
kubectl get nodes
kubectl get namespaces
```

## 📁 Estructura del Proyecto

```
terraform-digitalocean/
├── main.tf                      # Configuración principal
├── variables.tf                 # Variables globales
├── outputs.tf                   # Outputs del root module
├── versions.tf                  # Providers y versiones
├── Makefile                     # Automatización de comandos
├── setup.sh                     # Script de setup interactivo
├── .gitignore                   # Archivos ignorados
│
├── modules/                     # Módulos reutilizables
│   ├── vpc/                     # VPC para aislamiento de red
│   ├── doks-cluster/            # Kubernetes cluster
│   ├── rbac/                    # RBAC y Network Policies
│   ├── cert-manager/            # Gestión de certificados TLS
│   └── ingress-nginx/           # Ingress controller
│
└── environments/                # Configuraciones por ambiente
    ├── stage/
    │   ├── terraform.tfvars     # Variables de stage
    │   └── backend.hcl          # Backend config de stage
    └── production/
        ├── terraform.tfvars     # Variables de production
        └── backend.hcl          # Backend config de production
```

## ⚙️ Configuración

### Variables Principales

Editar `environments/stage/terraform.tfvars`:

```hcl
environment        = "stage"
region             = "nyc1"
cluster_name       = "ecommerce-k8s"
kubernetes_version = "1.31.1-do.4"

node_pool = {
  name       = "worker-pool"
  size       = "s-2vcpu-2gb"
  node_count = 3
  auto_scale = true
  min_nodes  = 2
  max_nodes  = 5
  tags       = ["worker", "stage"]
}

enable_cert_manager = true
letsencrypt_email   = "tu-email@example.com"
```

## 🔧 Despliegue

### Usando Makefile (Recomendado)

```bash
make help              # Ver ayuda
make init ENV=stage    # Inicializar
make plan ENV=stage    # Planificar cambios
make apply ENV=stage   # Aplicar infraestructura
make kubeconfig ENV=stage  # Configurar kubectl
make status ENV=stage  # Ver estado del clúster
```

## 🔐 RBAC y Seguridad

El módulo RBAC crea automáticamente:

- **Service Accounts**: microservices-sa, monitoring-sa
- **Roles**: Permisos para ConfigMaps, Secrets, Services, Pods
- **Network Policies**: Aislamiento de namespaces

## 🔒 TLS/Certificados

Cert-manager configura tres ClusterIssuers:

1. **letsencrypt-staging**: Para pruebas
2. **letsencrypt-prod**: Para producción
3. **selfsigned-issuer**: Certificados autofirmados

## 🌍 Múltiples Ambientes

| Característica | Stage | Production |
|----------------|-------|------------|
| Node Size | s-2vcpu-2gb | s-4vcpu-8gb |
| Node Count | 3 | 3 |
| High Availability | No | Sí |
| Costo Mensual | ~$53 | ~$201 |

## 💰 Costos

**Stage**: ~$53/mes (DOKS gratis + 3 droplets + LB + Spaces)
**Production**: ~$201/mes (DOKS HA + 3 droplets grandes + LB + Spaces)

> 💡 Con $200 en créditos gratuitos, puedes correr stage por ~4 meses gratis.

## 🔍 Troubleshooting

Ver la documentación completa para soluciones a problemas comunes.

## 📚 Referencias

- [DigitalOcean Kubernetes](https://docs.digitalocean.com/products/kubernetes/)
- [Terraform DO Provider](https://registry.terraform.io/providers/digitalocean/digitalocean/latest/docs)
- [Cert-Manager](https://cert-manager.io/docs/)
- [NGINX Ingress](https://kubernetes.github.io/ingress-nginx/)
