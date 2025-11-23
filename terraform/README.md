# Terraform Infrastructure as Code for Kubernetes Cluster

Esta carpeta contiene la infraestructura como código (IaC) para desplegar un cluster de Kubernetes en Azure AKS siguiendo las mejores prácticas de DevOps.

## Estructura

```
terraform/
├── main.tf                    # Configuración principal
├── variables.tf               # Variables globales
├── outputs.tf                 # Outputs del módulo principal
├── versions.tf                # Versiones de Terraform y providers
├── backend.example.hcl        # Ejemplo de configuración de backend
├── ARCHITECTURE.md            # Documentación de arquitectura
├── deploy.sh                  # Script de despliegue automatizado
├── setup-backend.sh           # Script para configurar backend
├── Makefile                   # Comandos Make para Terraform
├── environments/              # Configuraciones por entorno
│   ├── stage/
│   │   ├── terraform.tfvars   # Variables para stage
│   │   └── backend.hcl        # Backend config para stage
│   └── production/
│       ├── terraform.tfvars   # Variables para production
│       └── backend.hcl        # Backend config para production
└── modules/                   # Módulos reutilizables
    ├── resource-group/        # Resource Group
    ├── networking/            # VNet y Subnets
    ├── kubernetes-cluster/    # Cluster AKS
    └── monitoring/           # Log Analytics
```

**Documentación completa de arquitectura**: Ver [ARCHITECTURE.md](./ARCHITECTURE.md)

## Prerequisitos

1. **Azure CLI** instalado y configurado
   ```bash
   az login
   az account set --subscription "your-subscription-id"
   ```

2. **Terraform** >= 1.5.0
   ```bash
   terraform version
   ```

3. **Permisos de Azure**:
   - Owner o Contributor en la suscripción
   - Permisos para crear Resource Groups, VNets, AKS, etc.

## Configuración Inicial

### 1. Configurar Backend (Azure Storage)

Crea un storage account para el estado de Terraform:

```bash
# Crear resource group para el estado
az group create --name rg-terraform-state --location eastus

# Crear storage account
az storage account create \
  --name stterraformstate \
  --resource-group rg-terraform-state \
  --location eastus \
  --sku Standard_LRS

# Crear container
az storage container create \
  --name terraform-state \
  --account-name stterraformstate
```

Copia el archivo de ejemplo y configura tus valores:

```bash
cp backend.example.hcl backend.hcl
# Edita backend.hcl con tus valores
```

### 2. Configurar Variables por Entorno

Las variables específicas de cada entorno están en `environments/{env}/terraform.tfvars`.

Cada ambiente tiene su propia configuración:
- **Stage**: `environments/stage/terraform.tfvars` - Ambiente de pre-producción
- **Production**: `environments/production/terraform.tfvars` - Ambiente de producción (main branch)

Edita según tus necesidades antes de desplegar.

## Uso

### Opción 1: Usando Make (Recomendado)

```bash
# Inicializar para stage
make init ENV=stage

# Planear cambios para stage
make plan ENV=stage

# Aplicar cambios para stage
make apply ENV=stage

# Para production
make init ENV=production
make plan ENV=production
make apply ENV=production
```

### Opción 2: Usando Script de Despliegue

```bash
# Desplegar en stage
./deploy.sh stage

# Desplegar en production
./deploy.sh production
```

### Opción 3: Comandos Terraform Directos

```bash
# Inicializar para stage
terraform init -backend-config=environments/stage/backend.hcl

# Planear cambios
terraform plan -var-file=environments/stage/terraform.tfvars

# Aplicar cambios
terraform apply -var-file=environments/stage/terraform.tfvars
```

### Ambientes Soportados

- **stage**: Ambiente de pre-producción para testing
- **production**: Ambiente de producción (main branch)

Cada ambiente tiene:
- ✅ Configuración separada (`terraform.tfvars`)
- ✅ Backend separado (`backend.hcl`)
- ✅ Estado de Terraform aislado

### Obtener Configuración de Kubernetes

Después del despliegue, obtén el kubeconfig:

```bash
az aks get-credentials --resource-group rg-ecommerce-dev --name aks-ecommerce-dev
```

O desde Terraform:

```bash
terraform output -raw kube_config > ~/.kube/config-aks-dev
export KUBECONFIG=~/.kube/config-aks-dev
```

## Módulos

### Resource Group
Crea un Resource Group con tags estándar.

### Networking
- Crea VNet con subnets
- Configura Network Security Groups
- Asocia NSG con subnets de AKS

### Kubernetes Cluster
- Crea cluster AKS con configuración optimizada
- Node pools con auto-scaling
- RBAC habilitado
- Integración con Azure Monitor

### Monitoring
- Log Analytics Workspace
- Integración con Container Insights

## Mejores Prácticas Implementadas

✅ **Separación por entornos**: Configuraciones independientes para dev/staging/prod  
✅ **Módulos reutilizables**: Código DRY y mantenible  
✅ **State remoto**: Backend en Azure Storage para colaboración  
✅ **Versionado**: Versiones fijas de providers y Terraform  
✅ **Tags consistentes**: Tagging estándar en todos los recursos  
✅ **RBAC**: Kubernetes RBAC y Azure RBAC configurados  
✅ **Network Policies**: Azure CNI con network policies  
✅ **Auto-scaling**: Node pools con auto-scaling habilitado  
✅ **Monitoring**: Log Analytics integrado  
✅ **Security**: NSG configurado, subnets segregadas  
✅ **Naming conventions**: Nombres consistentes y descriptivos  

## Variables Importantes

- `environment`: `stage` o `production`
- `kubernetes_version`: Versión de Kubernetes (default: 1.28)
- `default_node_pool`: Configuración del node pool por defecto
- `additional_node_pools`: Node pools adicionales para workloads específicos
- `rbac_enabled`: Habilita Kubernetes RBAC (default: true)
- `location`: Región de Azure (default: East US)

### Diferencias entre Ambientes

| Configuración | Stage | Production |
|--------------|-------|------------|
| VM Size | Standard_B2s | Standard_D2s_v3 |
| Node Count (System) | 2 | 3 |
| Node Pools | 3 | 5 |
| Disk Size | 30 GB | 50 GB |
| Network CIDR | 10.1.0.0/16 | 10.0.0.0/16 |

## Outputs

Después del despliegue, puedes obtener:

- `cluster_name`: Nombre del cluster
- `cluster_fqdn`: FQDN del cluster
- `kube_config`: Configuración de Kubernetes (sensitive)
- `resource_group_name`: Nombre del resource group
- `vnet_id`: ID de la VNet

## Destruir Infraestructura

**CUIDADO**: Esto eliminará todos los recursos

```bash
# Usando Make
make destroy ENV=stage

# O directamente
terraform destroy -var-file=environments/stage/terraform.tfvars
```

## Troubleshooting

### Error: Backend configuration not found
- Asegúrate de tener `backend.hcl` configurado
- O configura las variables de entorno del backend

### Error: Authentication failed
- Ejecuta `az login`
- Verifica permisos con `az account show`

### Error: Resource group already exists
- Usa un nombre único o elimina el resource group existente

## Mejoras Futuras

- [ ] Integración con Azure Key Vault para secrets
- [ ] Application Gateway para ingress
- [ ] Azure Container Registry (ACR)
- [ ] Azure Policy para compliance
- [ ] Disaster Recovery configuration
- [ ] Multi-region deployment

