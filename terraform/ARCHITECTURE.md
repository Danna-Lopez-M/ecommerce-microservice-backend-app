# Arquitectura de Infraestructura - Ecommerce Microservices

Este documento describe la arquitectura de infraestructura implementada con Terraform para el proyecto de microservicios de e-commerce.

## 📋 Tabla de Contenidos

1. [Visión General](#visión-general)
2. [Arquitectura de Alto Nivel](#arquitectura-de-alto-nivel)
3. [Componentes de Infraestructura](#componentes-de-infraestructura)
4. [Estructura Modular](#estructura-modular)
5. [Gestión de Ambientes](#gestión-de-ambientes)
6. [Backend Remoto](#backend-remoto)
7. [Seguridad](#seguridad)
8. [Monitoreo y Observabilidad](#monitoreo-y-observabilidad)

## 🎯 Visión General

La infraestructura está diseñada para soportar una arquitectura de microservicios desplegada en Azure Kubernetes Service (AKS). La infraestructura se gestiona completamente como código usando Terraform, siguiendo las mejores prácticas de IaC.

### Características Principales

- ✅ **Infraestructura como Código**: Toda la infraestructura está definida en Terraform
- ✅ **Estructura Modular**: Módulos reutilizables para cada componente
- ✅ **Múltiples Ambientes**: Configuración separada para stage y production
- ✅ **Backend Remoto**: Estado de Terraform almacenado en Azure Storage
- ✅ **Seguridad**: RBAC, Network Policies, y mejores prácticas de seguridad
- ✅ **Escalabilidad**: Auto-scaling configurado en node pools
- ✅ **Monitoreo**: Integración con Azure Monitor y Log Analytics

## 🏗️ Arquitectura de Alto Nivel

```
┌─────────────────────────────────────────────────────────────────┐
│                      Azure Subscription                          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Resource Group: rg-ecommerce-{env}              │  │
│  │                                                           │  │
│  │  ┌────────────────────────────────────────────────────┐   │  │
│  │  │  Virtual Network (VNet)                           │   │  │
│  │  │  ┌──────────────┐  ┌──────────────┐              │   │  │
│  │  │  │ AKS Subnet   │  │ AppGW Subnet │              │   │  │
│  │  │  │ 10.x.1.0/24  │  │ 10.x.2.0/24  │              │   │  │
│  │  │  └──────────────┘  └──────────────┘              │   │  │
│  │  └────────────────────────────────────────────────────┘   │  │
│  │                                                           │  │
│  │  ┌────────────────────────────────────────────────────┐   │  │
│  │  │  Azure Kubernetes Service (AKS)                     │   │  │
│  │  │  ┌──────────────────────────────────────────────┐  │   │  │
│  │  │  │  System Node Pool                             │  │  │
│  │  │  │  - Kubernetes System Pods                     │  │  │
│  │  │  └──────────────────────────────────────────────┘  │  │  │
│  │  │  ┌──────────────────────────────────────────────┐  │  │  │
│  │  │  │  User Service Node Pool                      │  │  │
│  │  │  │  - user-service pods                         │  │  │
│  │  │  └──────────────────────────────────────────────┘  │  │  │
│  │  │  ┌──────────────────────────────────────────────┐  │  │  │
│  │  │  │  Product Service Node Pool                    │  │  │
│  │  │  │  - product-service pods                      │  │  │
│  │  │  └──────────────────────────────────────────────┘  │  │  │
│  │  │  ┌──────────────────────────────────────────────┐  │  │  │
│  │  │  │  Order Service Node Pool                     │  │  │
│  │  │  │  - order-service pods                         │  │  │
│  │  │  └──────────────────────────────────────────────┘  │  │  │
│  │  │  ┌──────────────────────────────────────────────┐  │  │  │
│  │  │  │  Payment Service Node Pool                    │  │  │
│  │  │  │  - payment-service pods                       │  │  │
│  │  │  └──────────────────────────────────────────────┘  │  │  │
│  │  └────────────────────────────────────────────────────┘   │  │
│  │                                                           │  │
│  │  ┌────────────────────────────────────────────────────┐   │  │
│  │  │  Log Analytics Workspace                           │   │  │
│  │  │  - Container Insights                              │   │  │
│  │  │  - Application Logs                                │   │  │
│  │  │  - Metrics Collection                             │   │  │
│  │  └────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Terraform State Backend (Azure Storage)                 │  │
│  │  - Separate state files per environment                  │  │
│  │  - State locking enabled                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 🔧 Componentes de Infraestructura

### 1. Resource Group

**Módulo**: `modules/resource-group`

- Agrupa todos los recursos relacionados
- Aplica tags consistentes
- Facilita la gestión y limpieza de recursos

**Configuración**:
- Nombre: `rg-ecommerce-{environment}`
- Location: Configurable por ambiente
- Tags: Environment, ManagedBy, Project, Team

### 2. Networking

**Módulo**: `modules/networking`

- **Virtual Network (VNet)**: Red privada para todos los recursos
- **Subnets**:
  - `aks-subnet`: Para los nodos del cluster AKS
  - `appgw-subnet`: Para Application Gateway (futuro)
- **Network Security Groups (NSG)**: Reglas de firewall
- **Network Policies**: Políticas de red para Kubernetes

**Configuración por Ambiente**:
- **Stage**: `10.1.0.0/16` (VNet), `10.1.1.0/24` (AKS), `10.1.2.0/24` (AppGW)
- **Production**: `10.0.0.0/16` (VNet), `10.0.1.0/24` (AKS), `10.0.2.0/24` (AppGW)

### 3. Kubernetes Cluster (AKS)

**Módulo**: `modules/kubernetes-cluster`

- **Cluster AKS**: Cluster de Kubernetes gestionado
- **Node Pools**:
  - **System Pool**: Para pods del sistema de Kubernetes
  - **User Pool**: Para user-service
  - **Product Pool**: Para product-service
  - **Order Pool**: Para order-service (solo production)
  - **Payment Pool**: Para payment-service (solo production)
- **Auto-scaling**: Habilitado en todos los node pools
- **RBAC**: Kubernetes RBAC habilitado
- **Network Plugin**: Azure CNI

**Configuración por Ambiente**:

| Configuración | Stage | Production |
|--------------|-------|------------|
| Kubernetes Version | 1.28 | 1.28 |
| System Nodes | 2x Standard_B2s | 3x Standard_D2s_v3 |
| User Nodes | 2x Standard_B2s | 3x Standard_D2s_v3 |
| Product Nodes | 2x Standard_B2s | 3x Standard_D2s_v3 |
| Order Nodes | - | 2x Standard_D2s_v3 |
| Payment Nodes | - | 2x Standard_D2s_v3 |

### 4. Monitoring

**Módulo**: `modules/monitoring`

- **Log Analytics Workspace**: Almacenamiento centralizado de logs
- **Container Insights**: Monitoreo de contenedores
- **Metrics Collection**: Métricas de Kubernetes y aplicaciones
- **Alert Rules**: Reglas de alerta (configurables)

## Estructura Modular

La infraestructura está organizada en módulos reutilizables:

```
terraform/
├── main.tf                    # Configuración principal
├── variables.tf               # Variables globales
├── outputs.tf                 # Outputs del módulo principal
├── versions.tf               # Versiones de Terraform y providers
├── backend.example.hcl       # Ejemplo de configuración de backend
├── environments/             # Configuraciones por entorno
│   ├── stage/
│   │   ├── terraform.tfvars  # Variables para stage
│   │   └── backend.hcl       # Backend config para stage
│   └── production/
│       ├── terraform.tfvars  # Variables para production
│       └── backend.hcl       # Backend config para production
└── modules/                  # Módulos reutilizables
    ├── resource-group/       # Resource Group
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    ├── networking/           # VNet y Subnets
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    ├── kubernetes-cluster/  # Cluster AKS
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    └── monitoring/          # Log Analytics
        ├── main.tf
        ├── variables.tf
        └── outputs.tf
```

### Ventajas de la Estructura Modular

1. **Reutilización**: Los módulos pueden reutilizarse en otros proyectos
2. **Mantenibilidad**: Cambios en un módulo se propagan automáticamente
3. **Testabilidad**: Cada módulo puede probarse independientemente
4. **Claridad**: Separación clara de responsabilidades
5. **Escalabilidad**: Fácil agregar nuevos módulos

## Gestión de Ambientes

### Ambientes Soportados

1. **Stage**: Ambiente de pre-producción para testing
2. **Production**: Ambiente de producción (main branch)

### Separación de Estados

Cada ambiente tiene su propio archivo de estado en Azure Storage:

```
terraform-state/
├── ecommerce-aks/
│   ├── stage/
│   │   └── terraform.tfstate
│   └── production/
│       └── terraform.tfstate
```

### Configuración por Ambiente

Las diferencias principales entre ambientes:

| Aspecto | Stage | Production |
|---------|-------|------------|
| **VM Size** | Standard_B2s (2 vCPU, 4GB RAM) | Standard_D2s_v3 (2 vCPU, 8GB RAM) |
| **Node Count** | 2-4 nodos | 3-5 nodos |
| **Node Pools** | 3 pools | 5 pools |
| **Disk Size** | 30 GB | 50 GB |
| **Network** | 10.1.0.0/16 | 10.0.0.0/16 |
| **Tags** | Environment=stage | Environment=production, Criticality=High |

### Workflow de Despliegue

```
┌─────────────┐
│   Code      │
│  (GitHub)   │
└──────┬──────┘
       │
       ▼
┌─────────────┐      ┌─────────────┐
│   Stage     │─────▶│ Production  │
│  (Testing)  │      │   (Main)    │
└─────────────┘      └─────────────┘
       │                    │
       ▼                    ▼
┌─────────────┐      ┌─────────────┐
│ Terraform   │      │ Terraform   │
│   Apply     │      │   Apply     │
└─────────────┘      └─────────────┘
```

## Backend Remoto

### Configuración

El estado de Terraform se almacena en Azure Storage Account:

- **Resource Group**: `rg-terraform-state`
- **Storage Account**: `stterraformstatetaller2`
- **Container**: `terraform-state`
- **State Keys**:
  - Stage: `ecommerce-aks/stage/terraform.tfstate`
  - Production: `ecommerce-aks/production/terraform.tfstate`

### Ventajas del Backend Remoto

1. **Colaboración**: Múltiples desarrolladores pueden trabajar simultáneamente
2. **State Locking**: Previene conflictos al aplicar cambios
3. **Seguridad**: El estado se almacena de forma segura en Azure
4. **Versionado**: Azure Storage mantiene versiones del estado
5. **Backup**: El estado está respaldado automáticamente

### Configuración del Backend

Cada ambiente tiene su propio archivo `backend.hcl`:

```hcl
# environments/stage/backend.hcl
resource_group_name  = "rg-terraform-state"
storage_account_name = "stterraformstatetaller2"
container_name       = "terraform-state"
key                  = "ecommerce-aks/stage/terraform.tfstate"
```

```hcl
# environments/production/backend.hcl
resource_group_name  = "rg-terraform-state"
storage_account_name = "stterraformstatetaller2"
container_name       = "terraform-state"
key                  = "ecommerce-aks/production/terraform.tfstate"
```

## 🔒 Seguridad

### Kubernetes RBAC

- **RBAC Habilitado**: Control de acceso basado en roles
- **Service Accounts**: Cuentas de servicio para cada microservicio
- **Role Bindings**: Permisos específicos por namespace

### Network Security

- **Network Security Groups**: Reglas de firewall a nivel de red
- **Network Policies**: Políticas de red dentro del cluster
- **Subnet Isolation**: Subnets separadas para diferentes componentes

### Azure RBAC

- **Managed Identity**: Identidades gestionadas para recursos de Azure
- **Access Control**: Control de acceso a recursos de Azure
- **Key Vault Integration**: (Futuro) Para gestión de secrets

### Mejores Prácticas Implementadas

- ✅ Secrets no se almacenan en código
- ✅ Tags consistentes para auditoría
- ✅ Network isolation entre componentes
- ✅ RBAC habilitado en todos los niveles
- ✅ State remoto con acceso controlado

## 📊 Monitoreo y Observabilidad

### Log Analytics Workspace

- **Container Insights**: Monitoreo de contenedores y pods
- **Application Logs**: Logs de aplicaciones
- **Kubernetes Metrics**: Métricas del cluster
- **Node Metrics**: Métricas de nodos

### Métricas Recolectadas

- CPU y memoria por pod
- Network I/O
- Disk I/O
- Kubernetes events
- Application-specific metrics

### Alertas (Configurables)

- Alta utilización de CPU
- Alta utilización de memoria
- Pods en estado de error
- Node failures
- Network issues

## Uso

### Inicializar Terraform

```bash
# Para stage
make init ENV=stage

# Para production
make init ENV=production
```

### Planear Cambios

```bash
# Para stage
make plan ENV=stage

# Para production
make plan ENV=production
```

### Aplicar Cambios

```bash
# Para stage
make apply ENV=stage

# Para production
make apply ENV=production
```

## Escalabilidad

### Auto-scaling

- **Cluster Autoscaler**: Escala nodos automáticamente
- **Horizontal Pod Autoscaler**: Escala pods basado en métricas
- **Vertical Pod Autoscaler**: (Futuro) Ajusta recursos de pods

### Capacidad

**Stage**:
- Mínimo: 6 nodos (2 system + 2 user + 2 product)
- Máximo: 12 nodos (con auto-scaling)

**Production**:
- Mínimo: 13 nodos (3 system + 3 user + 3 product + 2 order + 2 payment)
- Máximo: 26 nodos (con auto-scaling)

## Mejoras Futuras

- [ ] Azure Key Vault para gestión de secrets
- [ ] Application Gateway para ingress
- [ ] Azure Container Registry (ACR)
- [ ] Azure Policy para compliance
- [ ] Disaster Recovery configuration
- [ ] Multi-region deployment
- [ ] GitOps con ArgoCD o Flux
- [ ] Service Mesh (Istio/Linkerd)

## 📚 Referencias

- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Azure Kubernetes Service Documentation](https://docs.microsoft.com/azure/aks/)
- [Terraform Best Practices](https://www.terraform.io/docs/cloud/guides/recommended-practices/index.html)

---

**Última actualización**: 2025-11-23

