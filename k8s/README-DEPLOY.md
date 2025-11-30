# Guía de Despliegue en Kubernetes (AKS)

Esta guía explica cómo desplegar los microservicios de ecommerce en Azure Kubernetes Service (AKS).

## Prerrequisitos

1. **Azure CLI configurado** con permisos adecuados
2. **kubectl** instalado y configurado para conectarse al cluster AKS
3. **Cluster AKS** creado y funcionando

## Configuración Inicial

### 1. Conectar kubectl al cluster AKS

```bash
az aks get-credentials --resource-group rg-ecommerce-stage --name aks-ecommerce-stage
```

### 2. Verificar conexión

```bash
kubectl cluster-info
kubectl get nodes
```

## Despliegue Automático

### Opción 1: Script de Despliegue (Recomendado)

El script `deploy-stage.sh` automatiza todo el proceso:

```bash
cd k8s
./deploy-stage.sh
```

El script:
- Crea el namespace `ecommerce-stage` si no existe
- Actualiza automáticamente los manifiestos para usar el entorno stage
- Despliega los servicios en el orden correcto de dependencias
- Espera a que los servicios de infraestructura estén listos antes de desplegar los microservicios

### Opción 2: Despliegue Manual

Si prefieres desplegar manualmente:

```bash
# 1. Crear namespace
kubectl apply -f 00-namespace.yaml

# 2. Desplegar MySQL
kubectl apply -f 00-mysql.yaml

# 3. Esperar a que MySQL esté listo
kubectl wait --for=condition=ready pod -l app=mysql -n ecommerce-stage --timeout=300s

# 4. Desplegar servicios de infraestructura
kubectl apply -f 01-configmap.yaml
kubectl apply -f 10-zipkin.yaml
kubectl apply -f 08-service-discovery.yaml
kubectl apply -f 09-cloud-config.yaml

# 5. Esperar a que los servicios de infraestructura estén listos
kubectl wait --for=condition=available --timeout=300s deployment/service-discovery -n ecommerce-stage
kubectl wait --for=condition=available --timeout=300s deployment/cloud-config -n ecommerce-stage

# 6. Desplegar microservicios
kubectl apply -f 02-user-service.yaml
kubectl apply -f 03-product-service.yaml
kubectl apply -f 04-order-service.yaml
kubectl apply -f 05-payment-service.yaml
kubectl apply -f 06-shipping-service.yaml
kubectl apply -f 11-favourite-service.yaml
kubectl apply -f 07-proxy-client.yaml
kubectl apply -f 12-api-gateway.yaml
```

## Configuración de Base de Datos

Los servicios están configurados para usar MySQL en el entorno stage. La configuración de MySQL está en `00-mysql.yaml`:

- **Host**: `mysql` (nombre del servicio Kubernetes)
- **Puerto**: `3306`
- **Base de datos**: `ecommerce_stage_db`
- **Usuario**: `ecommerce`
- **Contraseña**: `ecommerce123` (configurada en el Secret)

**Nota**: Para producción, deberías:
1. Usar Azure Database for MySQL o Azure SQL
2. Cambiar las contraseñas por valores seguros
3. Usar Secrets de Kubernetes o Azure Key Vault

## Verificación del Despliegue

### Ver estado de los pods

```bash
kubectl get pods -n ecommerce-stage
```

### Ver logs de un servicio

```bash
kubectl logs -f deployment/<service-name> -n ecommerce-stage
```

Ejemplo:
```bash
kubectl logs -f deployment/user-service -n ecommerce-stage
```

### Ver servicios expuestos

```bash
kubectl get svc -n ecommerce-stage
```

### Verificar health checks

```bash
# Service Discovery
kubectl get pods -n ecommerce-stage -l app=service-discovery
kubectl port-forward svc/service-discovery 8761:8761 -n ecommerce-stage
# Luego abrir http://localhost:8761 en el navegador

# API Gateway
kubectl get svc api-gateway -n ecommerce-stage
# Obtener la IP externa del LoadBalancer
```

## Orden de Dependencias

Los servicios deben desplegarse en este orden:

1. **Infraestructura Base**:
   - Namespace
   - MySQL
   - ConfigMap

2. **Servicios de Infraestructura**:
   - Zipkin (tracing)
   - Service Discovery (Eureka)
   - Cloud Config

3. **Microservicios de Negocio**:
   - User Service
   - Product Service
   - Order Service
   - Payment Service
   - Shipping Service
   - Favourite Service

4. **Servicios de Acceso**:
   - Proxy Client
   - API Gateway

## Troubleshooting

### Pods en estado CrashLoopBackOff

```bash
# Ver logs del pod
kubectl logs <pod-name> -n ecommerce-stage

# Describir el pod para ver eventos
kubectl describe pod <pod-name> -n ecommerce-stage
```

### Servicio no puede conectarse a MySQL

```bash
# Verificar que MySQL está corriendo
kubectl get pods -l app=mysql -n ecommerce-stage

# Verificar el servicio MySQL
kubectl get svc mysql -n ecommerce-stage

# Probar conexión desde un pod temporal
kubectl run -it --rm mysql-client --image=mysql:8.0 --restart=Never -- mysql -h mysql -u ecommerce -pecommerce123 ecommerce_stage_db
```

### Service Discovery no funciona

```bash
# Verificar que Service Discovery está corriendo
kubectl get pods -l app=service-discovery -n ecommerce-stage

# Ver logs
kubectl logs -f deployment/service-discovery -n ecommerce-stage

# Verificar que otros servicios se registran
kubectl port-forward svc/service-discovery 8761:8761 -n ecommerce-stage
# Abrir http://localhost:8761
```

### Actualizar configuración

Si necesitas actualizar la configuración de un servicio:

```bash
# Editar el deployment
kubectl edit deployment <service-name> -n ecommerce-stage

# O aplicar cambios desde archivo
kubectl apply -f <archivo-yaml> -n ecommerce-stage

# Forzar recreación de pods
kubectl rollout restart deployment <service-name> -n ecommerce-stage
```

## Escalado

### Escalar un servicio manualmente

```bash
kubectl scale deployment <service-name> --replicas=3 -n ecommerce-stage
```

### Configurar Auto-scaling (HPA)

```bash
kubectl autoscale deployment <service-name> --cpu-percent=70 --min=1 --max=5 -n ecommerce-stage
```

## Limpieza

Para eliminar todos los recursos desplegados:

```bash
kubectl delete namespace ecommerce-stage
```

**Advertencia**: Esto eliminará todos los datos, incluyendo la base de datos MySQL.

## Próximos Pasos

1. **Configurar Ingress** para exponer los servicios externamente
2. **Configurar certificados TLS/SSL** para HTTPS
3. **Configurar monitoring** con Prometheus y Grafana
4. **Configurar CI/CD** para despliegues automáticos
5. **Configurar backup** de la base de datos
6. **Implementar políticas de red** (Network Policies)


