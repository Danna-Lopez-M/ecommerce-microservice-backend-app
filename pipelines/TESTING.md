# Guía de Pruebas de Pipelines Jenkins

Esta guía explica cómo probar los pipelines de Jenkins configurados para usar minikube.

## Prerequisitos

Antes de probar los pipelines, asegúrate de tener instalado:

1. **Minikube**
   ```bash
   minikube version
   ```

2. **Docker**
   ```bash
   docker --version
   ```

3. **kubectl**
   ```bash
   kubectl version --client
   ```

4. **Maven**
   ```bash
   mvn -version
   ```

5. **Python 3 y Locust** (para tests de performance)
   ```bash
   python3 --version
   pip install locust
   ```

6. **Jenkins** (con plugins necesarios)
   - Docker Pipeline
   - Kubernetes CLI
   - JUnit
   - SonarQube Scanner
   - HTML Publisher
   - OWASP Dependency Check (opcional)

## Opción 1: Probar Localmente (Sin Jenkins)

Puedes probar los comandos que ejecutan los pipelines directamente en tu máquina local:

### 1. Preparar Minikube

```bash
# Iniciar minikube si no está corriendo
minikube status || minikube start

# Verificar que minikube esté corriendo
minikube status

# Configurar kubectl para usar minikube
minikube update-context

# Crear namespace
kubectl create namespace ecommerce-dev --dry-run=client -o yaml | kubectl apply -f -

# Verificar namespace
kubectl get namespaces | grep ecommerce-dev
```

### 2. Construir y Cargar Imagen Docker

```bash
# Ir al directorio del microservicio (ejemplo: user-service)
cd user-service

# Construir imagen Docker
docker build -t user-service:1.0 .

# Cargar imagen en minikube
minikube image load user-service:1.0

# Verificar que la imagen esté en minikube
minikube image list | grep user-service
```

### 3. Crear Deployment (si no existe)

Primero necesitas crear un deployment YAML. Ejemplo básico:

```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: ecommerce-dev
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: user-service:1.0
        imagePullPolicy: Never
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: ecommerce-dev
spec:
  type: NodePort
  selector:
    app: user-service
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30001
```

Aplicar el deployment:

```bash
# Crear deployment
kubectl apply -f user-service-deployment.yaml

# Verificar deployment
kubectl get deployments -n ecommerce-dev

# Verificar pods
kubectl get pods -n ecommerce-dev
```

### 4. Actualizar Deployment (como lo hace el pipeline)

```bash
# Actualizar imagen del deployment
kubectl set image deployment/user-service \
    user-service=user-service:1.0 \
    -n ecommerce-dev

# Verificar rollout
kubectl rollout status deployment/user-service -n ecommerce-dev
```

### 5. Probar Smoke Tests

```bash
# Obtener URL del servicio usando minikube
SERVICE_URL=$(minikube service user-service -n ecommerce-dev --url 2>/dev/null || echo "")

if [ -z "$SERVICE_URL" ]; then
    # Si minikube service no funciona, usar NodePort
    NODE_PORT=$(kubectl get service user-service -n ecommerce-dev -o jsonpath='{.spec.ports[0].nodePort}')
    MINIKUBE_IP=$(minikube ip)
    SERVICE_URL=http://${MINIKUBE_IP}:${NODE_PORT}
fi

echo "Service URL: ${SERVICE_URL}"

# Health check
curl -f ${SERVICE_URL}/user-service/actuator/health || echo "Health check failed"
```

## Opción 2: Probar en Jenkins

### 1. Configurar Jenkins

#### A. Crear un Pipeline Job

1. En Jenkins, ve a **New Item**
2. Selecciona **Pipeline**
3. Nombre: `user-service-dev` (o el nombre del microservicio)
4. Click en **OK**

#### B. Configurar el Pipeline

En la configuración del job:

1. En **Pipeline Definition**, selecciona **Pipeline script from SCM**
2. **SCM**: Git
3. **Repository URL**: `https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git`
4. **Branch**: `develop`
5. **Script Path**: `pipelines/dev/Jenkinsfile-user-service-dev.groovy`

#### C. Configurar Herramientas (si es necesario)

En **Build Environment**:
- Asegúrate de que Docker, Maven, kubectl y minikube estén disponibles en el PATH del agente Jenkins

### 2. Ejecutar el Pipeline

#### Ejecución Manual

1. Ve al job creado
2. Click en **Build Now**
3. Ve al build en ejecución
4. Click en **Console Output** para ver el progreso

#### Ejecución Automática

El pipeline se ejecutará automáticamente cuando:
- Hay cambios en la rama `develop`
- Se configura un webhook en GitHub

### 3. Verificar Resultados

#### Console Output

Revisa el console output para ver:
- Estado de cada etapa
- Logs de construcción
- Logs de tests
- Errores (si los hay)

#### Test Results

1. Ve a la página del build
2. Busca **Test Result** en el menú lateral
3. Revisa los resultados de:
   - Unit Tests
   - Integration Tests
   - E2E Tests

#### Estado del Deployment

```bash
# En la máquina donde está corriendo minikube
minikube status
kubectl get pods -n ecommerce-dev
kubectl get deployments -n ecommerce-dev
kubectl get services -n ecommerce-dev
```

## Opción 3: Script de Prueba Rápida

Crea un script para probar todo el flujo:

```bash
#!/bin/bash
# test-pipeline.sh

SERVICE_NAME=${1:-user-service}
VERSION=${2:-1.0}

echo "Testing pipeline for ${SERVICE_NAME} v${VERSION}"

# 1. Verificar minikube
echo "1. Checking minikube..."
if ! minikube status > /dev/null 2>&1; then
    echo "Starting minikube..."
    minikube start
else
    echo "Minikube is running"
fi
minikube update-context

# 2. Crear namespace
echo "2. Creating namespace..."
kubectl create namespace ecommerce-dev --dry-run=client -o yaml | kubectl apply -f -

# 3. Construir y cargar imagen
echo "3. Building and loading Docker image..."
cd ${SERVICE_NAME}
docker build -t ${SERVICE_NAME}:${VERSION} .
minikube image load ${SERVICE_NAME}:${VERSION}
cd ..

# 4. Verificar deployment existe
echo "4. Checking deployment..."
if ! kubectl get deployment ${SERVICE_NAME} -n ecommerce-dev > /dev/null 2>&1; then
    echo "ERROR: Deployment ${SERVICE_NAME} does not exist!"
    echo "Please create it first using kubectl apply -f <deployment-file>.yaml"
    exit 1
fi

# 5. Actualizar deployment
echo "5. Updating deployment..."
kubectl set image deployment/${SERVICE_NAME} \
    ${SERVICE_NAME}=${SERVICE_NAME}:${VERSION} \
    -n ecommerce-dev
kubectl rollout status deployment/${SERVICE_NAME} -n ecommerce-dev

# 6. Smoke test
echo "6. Running smoke tests..."
SERVICE_URL=$(minikube service ${SERVICE_NAME} -n ecommerce-dev --url 2>/dev/null || echo "")
if [ -z "$SERVICE_URL" ]; then
    NODE_PORT=$(kubectl get service ${SERVICE_NAME} -n ecommerce-dev -o jsonpath='{.spec.ports[0].nodePort}')
    MINIKUBE_IP=$(minikube ip)
    SERVICE_URL=http://${MINIKUBE_IP}:${NODE_PORT}
fi
echo "Service URL: ${SERVICE_URL}"
curl -f ${SERVICE_URL}/${SERVICE_NAME}/actuator/health && echo "Smoke test passed!" || echo "Smoke test failed!"

echo "Pipeline test completed!"
```

Uso:

```bash
chmod +x test-pipeline.sh
./test-pipeline.sh user-service 1.0
```

## Troubleshooting

### Error: minikube not found

```bash
# Instalar minikube (Linux)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

### Error: kubectl not found

```bash
# Instalar kubectl (Linux)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

### Error: Deployment does not exist

Necesitas crear el deployment primero. Ejemplo:

```bash
kubectl create deployment user-service \
    --image=user-service:1.0 \
    --namespace=ecommerce-dev \
    --dry-run=client -o yaml | kubectl apply -f -
```

### Error: minikube image load failed

```bash
# Verificar que minikube esté corriendo
minikube status

# Verificar que la imagen Docker existe
docker images | grep user-service

# Verificar permisos de Docker
docker ps
```

### Error: Health check failed

```bash
# Verificar que el pod esté corriendo
kubectl get pods -n ecommerce-dev

# Ver logs del pod
kubectl logs -n ecommerce-dev -l app=user-service

# Verificar que el servicio esté expuesto
kubectl get services -n ecommerce-dev

# Verificar puerto
minikube service list -n ecommerce-dev
```

## Verificación Paso a Paso

### 1. Verificar que Minikube está corriendo

```bash
minikube status
```

Salida esperada:
```
minikube
type: Control Plane
host: Running
kubelet: Running
apiserver: Running
kubeconfig: Configured
```

### 2. Verificar Namespace

```bash
kubectl get namespaces | grep ecommerce-dev
```

### 3. Verificar Imágenes en Minikube

```bash
minikube image list | grep user-service
```

### 4. Verificar Deployments

```bash
kubectl get deployments -n ecommerce-dev
```

### 5. Verificar Pods

```bash
kubectl get pods -n ecommerce-dev
```

### 6. Verificar Services

```bash
kubectl get services -n ecommerce-dev
```

### 7. Probar Acceso al Servicio

```bash
# Obtener URL
minikube service user-service -n ecommerce-dev --url

# O usar NodePort
NODE_PORT=$(kubectl get service user-service -n ecommerce-dev -o jsonpath='{.spec.ports[0].nodePort}')
MINIKUBE_IP=$(minikube ip)
curl http://${MINIKUBE_IP}:${NODE_PORT}/user-service/actuator/health
```

## Opción 3: Configurar Stack de Monitoreo

Antes de ejecutar los pipelines, es recomendable desplegar el stack de monitoreo:

### Desplegar Prometheus, Grafana y SonarQube

```bash
# Ejecutar el script de configuración
./kubernetes/monitoring/setup-monitoring.sh
```

Este script:
1. Crea el namespace `ecommerce-dev` si no existe
2. Despliega Prometheus con configuración para todos los microservicios
3. Despliega Grafana con datasource de Prometheus
4. Despliega SonarQube

### Verificar Servicios de Monitoreo

```bash
# Verificar que los pods estén corriendo
kubectl get pods -n ecommerce-dev | grep -E 'prometheus|grafana|sonarqube'

# Obtener URLs de acceso
MINIKUBE_IP=$(minikube ip)

echo "Prometheus: http://${MINIKUBE_IP}:30090"
echo "Grafana: http://${MINIKUBE_IP}:30300"
echo "SonarQube: http://${MINIKUBE_IP}:30000"
```

### Configurar SonarQube Token

1. Acceder a SonarQube: http://$(minikube ip):30000
2. Usuario/Contraseña inicial: `admin/admin`
3. Cambiar contraseña en el primer acceso
4. Ir a My Account > Security
5. Generar un nuevo token
6. Guardar el token en Jenkins como credencial `sonar-token`

### Verificar Prometheus Scraping

```bash
# Acceder a Prometheus
MINIKUBE_IP=$(minikube ip)
open http://${MINIKUBE_IP}:30090

# Verificar targets en Status > Targets
# Todos los microservicios deben aparecer como "UP"
```

### Verificar Grafana

```bash
# Acceder a Grafana
MINIKUBE_IP=$(minikube ip)
open http://${MINIKUBE_IP}:30300

# Credenciales: admin/admin
# Verificar que Prometheus aparezca como datasource
```

## Próximos Pasos

1. **Desplegar Stack de Monitoreo**: Ejecuta `./kubernetes/monitoring/setup-monitoring.sh`
2. **Configurar SonarQube Token**: Genera token y guárdalo en Jenkins
3. **Crear Deployments**: Crea los deployments YAML para todos los microservicios
4. **Configurar Jenkins**: Configura los jobs en Jenkins con las credenciales necesarias
5. **Probar Pipeline**: Ejecuta el pipeline manualmente
6. **Configurar Webhooks**: Configura webhooks de GitHub para ejecución automática
7. **Monitoreo**: Revisa métricas en Prometheus y Grafana

## Notas

- Los pipelines asumen que los deployments ya existen en minikube
- Las imágenes se cargan localmente, no se requiere Docker Registry
- Minikube se inicia automáticamente si no está corriendo
- El namespace se crea automáticamente si no existe
- Los smoke tests verifican el health endpoint del servicio

