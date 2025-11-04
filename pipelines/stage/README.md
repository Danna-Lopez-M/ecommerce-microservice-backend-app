# Pipelines de CI/CD para Stage Environment

Este directorio contiene los pipelines de Jenkins para la construcción y despliegue de los microservicios en el entorno de **stage** (staging).

## Microservicios con Pipelines de Stage

Los siguientes microservicios tienen pipelines de CI/CD configurados para stage:

1. **user-service** - `Jenkinsfile-user-service-stage.groovy`
2. **product-service** - `Jenkinsfile-product-service-stage.groovy`
3. **payment-service** - `Jenkinsfile-payment-service-stage.groovy`
4. **order-service** - `Jenkinsfile-order-service-stage.groovy`
5. **shipping-service** - `Jenkinsfile-shipping-service-stage.groovy`
6. **proxy-client** - `Jenkinsfile-proxy-client-stage.groovy`

## Estructura de los Pipelines (Stage Environment)

Todos los pipelines siguen la misma estructura:

### 1. Checkout
- Clona el repositorio desde la rama `develop`
- URL: `https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git`

### 2. Build Application
- Ejecuta `mvn clean package -DskipTests`
- Compila la aplicación sin ejecutar tests

### 3. Unit Tests
- Ejecuta tests unitarios: `mvn test -Dtest=**/*UnitTest`
- Publica resultados con JUnit

### 4. Integration Tests
- Ejecuta tests de integración: `mvn test -Dtest=**/*IntegrationTest`
- Publica resultados con JUnit

### 5. Code Quality Analysis - SonarQube
- Ejecuta análisis de calidad de código con SonarQube
- Project key: `${SERVICE_NAME}-stage`
- Analiza código fuente y tests
- Genera reportes de calidad y cobertura

### 6. Security Scan
- **OWASP Dependency Check**: Escanea dependencias Maven en busca de vulnerabilidades conocidas
- **Trivy**: Escanea la imagen Docker en busca de vulnerabilidades
- Los reportes se archivan como artefactos en Jenkins

### 7. Build and Push Docker Image
- Construye la imagen Docker del microservicio
- Publica la imagen en Docker Registry
- Tags: `${VERSION}` y `stage-latest`

### 8. Deploy to Kubernetes Stage
- Despliega en Kubernetes usando kubectl
- Namespace: `ecommerce-stage`
- Context: `stage` (configurable)
- Actualiza el deployment con la nueva imagen
- Espera el rollout completado (timeout: 5 minutos)

### 9. Wait for Deployment Ready
- Espera a que el deployment esté completamente disponible
- Timeout: 300 segundos
- Espera adicional de 30 segundos para que el servicio esté listo

### 10. Get Service URL
- Obtiene la URL del servicio desplegado en Kubernetes
- Soporta LoadBalancer, NodePort y ClusterIP
- Guarda la URL en `service-url.env` para uso en etapas siguientes

### 11. Smoke Tests Against Deployed Application
- Ejecuta smoke tests contra la aplicación **desplegada en Kubernetes**
- Health check: `/actuator/health`
- Info check: `/actuator/info`
- Usa la URL del servicio desplegado

### 12. E2E Tests Against Deployed Application
- Ejecuta tests end-to-end contra la aplicación **desplegada en Kubernetes**
- Usa la URL del servicio desplegado
- Publica resultados con JUnit

### 13. Performance Tests Against Deployed Application
- Ejecuta tests de performance usando Locust contra la aplicación **desplegada**
- Configuración:
  - Usuarios: 20
  - Spawn rate: 4 usuarios/segundo
  - Duración: 120 segundos
- Genera reportes HTML y CSV
- Analiza resultados con `analyze-performance.py` si está disponible
- Publica reporte HTML en Jenkins

### 14. Validate Performance Metrics
- Valida métricas de performance contra thresholds:
  - Average Response Time: máximo 2000ms
  - Error Rate: máximo 5%
  - Requests per Second: mínimo 10
- Falla el pipeline si los thresholds no se cumplen

## Diferencias con Pipelines de Dev

| Aspecto | Dev Environment | Stage Environment |
|---------|----------------|-------------------|
| **Kubernetes** | Minikube local | Cluster Kubernetes real |
| **Docker Registry** | No requerido (minikube image load) | Requerido (push a registry) |
| **Tests contra aplicación** | Local | **Desplegada en Kubernetes** |
| **Performance Tests** | 10 usuarios, 60s | 20 usuarios, 120s |
| **Validación de métricas** | Opcional | **Obligatoria con thresholds** |
| **Namespace** | `ecommerce-dev` | `ecommerce-stage` |

## Configuración Requerida

### Variables de Entorno

1. **DOCKER_REGISTRY**: URL del Docker Registry
   ```groovy
   DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'your-registry'}"
   ```

2. **KUBERNETES_CONTEXT**: Contexto de Kubernetes (por defecto: `stage`)
   ```groovy
   KUBERNETES_CONTEXT = "${env.KUBERNETES_CONTEXT ?: 'stage'}"
   ```

3. **KUBERNETES_NAMESPACE**: Namespace de Kubernetes (por defecto: `ecommerce-stage`)
   ```groovy
   KUBERNETES_NAMESPACE = 'ecommerce-stage'
   ```

4. **SERVICE_PORT**: Puerto específico de cada microservicio:
   - `user-service`: 8700
   - `product-service`: 8500
   - `payment-service`: 8400
   - `order-service`: 8300
   - `shipping-service`: 8600
   - `proxy-client`: 8900

5. **SONAR_HOST_URL**: URL de SonarQube (por defecto: `http://localhost:9000`)

### Credenciales en Jenkins

1. **Docker Registry Credentials** (`docker-credentials`):
   - Tipo: Username with password
   - ID: `docker-credentials`
   - Descripción: Credenciales para Docker Registry

2. **SonarQube Token** (`sonar-token`):
   - Tipo: Secret text
   - ID: `sonar-token`
   - Descripción: Token de autenticación para SonarQube

3. **Kubernetes Config** (`kubeconfig-stage`):
   - Tipo: Secret file (opcional si se usa contexto configurado)
   - ID: `kubeconfig-stage`
   - Descripción: Archivo kubeconfig para acceso al cluster de stage

### Prerequisitos

- **Jenkins con plugins instalados**:
  - Docker Pipeline
  - Kubernetes CLI
  - JUnit
  - SonarQube Scanner
  - HTML Publisher
  
- **Herramientas instaladas en el agente Jenkins**:
  - Docker
  - kubectl (configurado con acceso al cluster de stage)
  - Maven
  - Python 3 (para tests de performance con Locust)
  - Locust (instalado vía pip: `pip install locust`)
  
- **Infraestructura**:
  - Cluster de Kubernetes (stage environment)
  - Docker Registry accesible
  - SonarQube accesible
  - Deployments creados previamente en el namespace `ecommerce-stage`

## Uso de los Pipelines

### Crear un Job en Jenkins

1. Crear un nuevo Pipeline Job
2. En "Pipeline Definition", seleccionar "Pipeline script from SCM"
3. Configurar:
   - SCM: Git
   - Repository URL: `https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git`
   - Branch: `develop`
   - Script Path: `pipelines/stage/Jenkinsfile-{service-name}-stage.groovy`

### Ejecutar Manualmente

Los pipelines se ejecutan automáticamente cuando hay cambios en la rama `develop`, pero también se pueden ejecutar manualmente desde Jenkins.

### Monitoreo

- **Jenkins Console Output**: Revisar el progreso del pipeline
- **Test Results**: Verificar resultados de tests
  - Unit Tests
  - Integration Tests
  - E2E Tests (contra aplicación desplegada)
- **SonarQube**: Revisar análisis de calidad de código
- **Security Reports**: Revisar reportes de seguridad
- **Performance Reports**: Revisar reportes de performance con validación de thresholds
- **Verificar despliegue en Kubernetes**:
  ```bash
  kubectl get pods -n ecommerce-stage
  kubectl get deployments -n ecommerce-stage
  kubectl get services -n ecommerce-stage
  ```

## Personalización

### Modificar Variables

Edita el archivo del pipeline correspondiente y modifica las variables en la sección `environment`:

```groovy
environment {
    SERVICE_NAME = 'user-service'
    SERVICE_DIR = 'user-service'
    SERVICE_PORT = '8700'
    DOCKER_REGISTRY = 'your-registry'
    KUBERNETES_NAMESPACE = 'ecommerce-stage'
    KUBERNETES_CONTEXT = 'stage'
}
```

### Ajustar Thresholds de Performance

Edita la etapa `Validate Performance Metrics` en el pipeline:

```groovy
thresholds = {
    'max_avg_response_time': 2000,  # ms
    'max_95_percentile': 3000,      # ms
    'max_error_rate': 5.0,          # percentage
    'min_rps': 10                   # requests per second
}
```

## Troubleshooting

### Error: Docker Registry authentication failed
- Verificar credenciales `docker-credentials` en Jenkins
- Verificar que el usuario tenga permisos para push al registry

### Error: kubectl context not found
- Verificar que el contexto `stage` esté configurado en kubectl
- Configurar el contexto: `kubectl config use-context stage`

### Error: Deployment does not exist
- Crear el deployment primero en el namespace `ecommerce-stage`
- Verificar que el nombre del deployment coincida con `SERVICE_NAME`

### Error: Service URL not found
- Verificar que el servicio esté expuesto (LoadBalancer, NodePort o ClusterIP)
- Verificar que el servicio esté en el namespace correcto

### Error: Performance tests failed thresholds
- Revisar métricas en el reporte de performance
- Ajustar thresholds si es necesario
- Revisar recursos del cluster (CPU, memoria)

### Error: E2E tests failed
- Verificar que el servicio esté completamente desplegado y accesible
- Revisar logs del pod: `kubectl logs -n ecommerce-stage -l app={service-name}`
- Verificar que las dependencias del servicio estén disponibles

## Notas

- Los pipelines están configurados para el entorno de **stage** usando un **cluster de Kubernetes real**
- Las imágenes Docker se publican en un **Docker Registry externo**
- Los tests E2E y de performance se ejecutan contra la **aplicación desplegada en Kubernetes**
- El despliegue requiere que los deployments existan previamente en el namespace `ecommerce-stage`
- Los tests de performance tienen thresholds obligatorios que deben cumplirse
- El pipeline falla si los thresholds de performance no se cumplen
- Para desarrollo local, usa los pipelines de `pipelines/dev/`
- Para producción, usa los pipelines de `pipelines/production/` (si existen)

## Próximos Pasos

1. **Crear Deployments**: Crear los deployments YAML para todos los microservicios en el namespace `ecommerce-stage`
2. **Configurar Jenkins**: Configurar los jobs en Jenkins con las credenciales necesarias
3. **Configurar Docker Registry**: Configurar el Docker Registry y las credenciales
4. **Configurar Kubernetes Context**: Configurar el contexto de Kubernetes para stage
5. **Probar Pipeline**: Ejecutar el pipeline manualmente para verificar que funciona
6. **Configurar Webhooks**: Configurar webhooks de GitHub para ejecución automática
7. **Monitoreo**: Configurar monitoreo y alertas para el entorno de stage



