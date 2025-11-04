# Pipelines de CI/CD para Microservicios

Este directorio contiene los pipelines de Jenkins para la construcción y despliegue de los microservicios en diferentes entornos.

## Entornos Disponibles

### Desarrollo (Dev)
Pipelines para desarrollo local usando minikube:
- **Ubicación**: `pipelines/dev/`
- **Documentación**: Ver [README de Dev](dev/README.md) (si existe) o [README principal](README.md)

### Stage (Staging)
Pipelines para staging con despliegue en Kubernetes y pruebas contra la aplicación desplegada:
- **Ubicación**: `pipelines/stage/`
- **Documentación**: Ver [README de Stage](stage/README.md)

### Production (Producción)
Pipelines para producción con construcción, validación de pruebas de sistema, despliegue y generación automática de Release Notes:
- **Ubicación**: `pipelines/production/`
- **Documentación**: Ver [README de Production](production/README.md)

## Microservicios con Pipelines

Los siguientes microservicios tienen pipelines de CI/CD configurados para diferentes entornos:

### Desarrollo (Dev)
1. **user-service** - `dev/Jenkinsfile-user-service-dev.groovy`
2. **product-service** - `dev/Jenkinsfile-product-service-dev.groovy`
3. **payment-service** - `dev/Jenkinsfile-payment-service-dev.groovy`
4. **order-service** - `dev/Jenkinsfile-order-service-dev.groovy`
5. **shipping-service** - `dev/Jenkinsfile-shipping-service-dev.groovy`
6. **proxy-client** - `dev/Jenkinsfile-proxy-client-dev.groovy`

### Stage (Staging)
1. **user-service** - `stage/Jenkinsfile-user-service-stage.groovy`
2. **product-service** - `stage/Jenkinsfile-product-service-stage.groovy`
3. **payment-service** - `stage/Jenkinsfile-payment-service-stage.groovy`
4. **order-service** - `stage/Jenkinsfile-order-service-stage.groovy`
5. **shipping-service** - `stage/Jenkinsfile-shipping-service-stage.groovy`
6. **proxy-client** - `stage/Jenkinsfile-proxy-client-stage.groovy`

### Production (Producción)
1. **user-service** - `production/Jenkinsfile-user-service-production.groovy`
2. **product-service** - `production/Jenkinsfile-product-service-production.groovy`
3. **payment-service** - `production/Jenkinsfile-payment-service-production.groovy`
4. **order-service** - `production/Jenkinsfile-order-service-production.groovy`
5. **shipping-service** - `production/Jenkinsfile-shipping-service-production.groovy`
6. **proxy-client** - `production/Jenkinsfile-proxy-client-production.groovy`

> **Nota**: Esta documentación describe los pipelines de **desarrollo (dev)**. Para los pipelines de **stage**, consulta [README de Stage](stage/README.md). Para los pipelines de **producción**, consulta [README de Production](production/README.md).

## Comparación de Pipelines

Para una comparación detallada entre los tres tipos de pipelines (Dev, Stage, Production), consulta [COMPARISON.md](COMPARISON.md).

## Estructura de los Pipelines (Dev Environment)

> **Nota**: Esta sección describe los pipelines de **desarrollo (dev)**. Para los pipelines de **stage**, consulta [README de Stage](stage/README.md). Para los pipelines de **producción**, consulta [README de Production](production/README.md).

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

### 5. E2E Tests
- Ejecuta tests end-to-end: `mvn test -Dtest=**/*E2ETest`
- Publica resultados con JUnit

### 6. Code Quality Analysis - SonarQube
- Ejecuta análisis de calidad de código con SonarQube
- Configuración:
  - `sonar.projectKey`: Nombre del microservicio
  - `sonar.host.url`: URL de SonarQube (por defecto: `http://localhost:9000`)
  - `sonar.login`: Token de autenticación (credencial `sonar-token` en Jenkins)
- Analiza código fuente y tests
- Genera reportes de calidad y cobertura

### 7. Security Scan
- **OWASP Dependency Check**: Escanea dependencias Maven en busca de vulnerabilidades conocidas
  - Genera reporte HTML: `target/dependency-check-report.html`
  - No falla el pipeline si encuentra vulnerabilidades (`-DfailOnError=false`)
- **Trivy**: Escanea la imagen Docker en busca de vulnerabilidades
  - Escaneo de severidad HIGH y CRITICAL
  - Genera reporte JSON: `trivy-results/trivy-report.json`
  - No falla el pipeline si encuentra vulnerabilidades (`--exit-code 0`)
- Los reportes se archivan como artefactos en Jenkins

### 8. Setup Minikube
- Verifica si minikube está corriendo, si no, lo inicia
- Configura kubectl para usar minikube con `minikube update-context`
- Crea el namespace `ecommerce-dev` si no existe

### 9. Build and Load Docker Image to Minikube
- Construye la imagen Docker del microservicio localmente
- Carga la imagen directamente en minikube usando `minikube image load`
- Tags: `${VERSION}` y `dev-latest`
- No requiere Docker Registry externo

### 10. Deploy to Minikube
- Despliega en minikube usando `kubectl`
- Namespace: `ecommerce-dev`
- Actualiza el deployment con la nueva imagen

### 11. Smoke Tests
- Verifica que el servicio esté funcionando usando `minikube service`
- Si no funciona, usa NodePort con el IP de minikube
- Health check: `/actuator/health` (con context-path específico de cada servicio)
- Muestra el puerto del servicio en los logs

### 12. Performance Tests
- Ejecuta tests de performance usando Locust
- Configuración:
  - Usuarios: 10
  - Spawn rate: 2 usuarios/segundo
  - Duración: 60 segundos
- Genera reportes HTML y CSV en `performance-results/`
- Analiza resultados con `analyze-performance.py` si está disponible
- Publica reporte HTML en Jenkins

## Configuración Requerida

### Variables de Entorno

Antes de usar los pipelines, asegúrate de configurar:

1. **Minikube**: Debe estar instalado y configurado en el agente Jenkins
   ```bash
   minikube version
   ```

2. **MINIKUBE_NAMESPACE**: Namespace de Kubernetes (por defecto: `ecommerce-dev`)
   ```groovy
   MINIKUBE_NAMESPACE = 'ecommerce-dev'
   ```

3. **SERVICE_PORT**: Puerto específico de cada microservicio:
   - `user-service`: 8700
   - `product-service`: 8500
   - `payment-service`: 8400
   - `order-service`: 8300
   - `shipping-service`: 8600
   - `proxy-client`: 8900

4. **SONAR_HOST_URL**: URL de SonarQube (por defecto: `http://localhost:9000`)
   - Se puede configurar como variable de entorno global en Jenkins
   - O modificar en cada pipeline individual

5. **Kubernetes Context**: Se configura automáticamente usando `minikube update-context`

### Credenciales en Jenkins

1. **SonarQube Token** (`sonar-token`):
   - Tipo: Secret text
   - ID: `sonar-token`
   - Descripción: Token de autenticación para SonarQube
   - Cómo obtenerlo:
     1. Acceder a SonarQube (http://localhost:9000 por defecto)
     2. Ir a My Account > Security
     3. Generar un nuevo token
     4. Copiar el token y guardarlo en Jenkins como credencial secreta

2. **No se requieren otras credenciales** ya que los pipelines usan minikube local:
   - Las imágenes se cargan directamente en minikube usando `minikube image load`
   - No se requiere Docker Registry externo
   - No se requiere configuración de Kubernetes remoto

### Prerequisitos

- **Jenkins con plugins instalados**:
  - Docker Pipeline
  - Kubernetes CLI
  - JUnit
  - SonarQube Scanner
  - HTML Publisher (para reportes de performance)
  - OWASP Dependency Check (plugin opcional)
  
- **Herramientas instaladas en el agente Jenkins**:
  - Minikube
  - Docker
  - kubectl
  - Maven
  - Python 3 (para tests de performance con Locust)
  - Locust (instalado vía pip: `pip install locust`)
  
- **Servicios de Monitoreo** (opcionales pero recomendados):
  - SonarQube (desplegado en minikube o local)
  - Prometheus (desplegado en minikube para métricas)
  - Grafana (desplegado en minikube para visualización)
  
- Acceso local para ejecutar minikube (sin privilegios de root)

## Uso de los Pipelines

### Crear un Job en Jenkins

1. Crear un nuevo Pipeline Job
2. En "Pipeline Definition", seleccionar "Pipeline script from SCM"
3. Configurar:
   - SCM: Git
   - Repository URL: `https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git`
   - Branch: `develop`
   - Script Path: `pipelines/Jenkinsfile-{service-name}-dev.groovy`

### Ejecutar Manualmente

Los pipelines se ejecutan automáticamente cuando hay cambios en la rama `develop`, pero también se pueden ejecutar manualmente desde Jenkins.

### Monitoreo

- **Jenkins Console Output**: Revisar el console output en Jenkins para ver el progreso del pipeline
- **Test Results**: Verificar resultados de tests en la sección de Test Results
  - Unit Tests
  - Integration Tests
  - E2E Tests
- **SonarQube**: Revisar análisis de calidad de código en SonarQube
  - URL: http://localhost:9000 (o la configurada en `SONAR_HOST_URL`)
  - Buscar el proyecto por nombre del microservicio
- **Security Reports**: Revisar reportes de seguridad en Jenkins
  - OWASP Dependency Check: `target/dependency-check-report.html`
  - Trivy: `trivy-results/trivy-report.json`
- **Performance Reports**: Revisar reportes de performance en Jenkins
  - HTML Report: Publicado en la sección "Performance Test Report"
  - CSV files: Disponibles como artefactos
- **Verificar despliegue en minikube**:
  ```bash
  minikube status
  kubectl get pods -n ecommerce-dev
  kubectl get deployments -n ecommerce-dev
  minikube service list -n ecommerce-dev
  ```
- **Prometheus y Grafana**: Ver métricas de los microservicios
  - Prometheus: http://$(minikube ip):30090
  - Grafana: http://$(minikube ip):30300 (admin/admin)

## Personalización

### Modificar Variables

Edita el archivo del pipeline correspondiente y modifica las variables en la sección `environment`:

```groovy
environment {
    DOCKER_REGISTRY = 'your-registry'
    SERVICE_NAME = 'user-service'
    SERVICE_DIR = 'user-service'
    VERSION = "${env.BUILD_NUMBER}"
    MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
}
```

### Añadir Etapas Adicionales

Puedes añadir etapas adicionales antes o después de las existentes:

```groovy
stage('Nueva Etapa') {
    steps {
        sh '''
            # Comandos aquí
        '''
    }
}
```

## Troubleshooting

### Error: Docker build failed
- Verificar que Docker esté instalado en el agente
- Verificar permisos de Docker
- Verificar que el Dockerfile existe en el directorio del servicio

### Error: minikube not found
- Instalar minikube en el agente Jenkins
- Verificar que minikube esté en el PATH
- Verificar permisos para ejecutar minikube

### Error: kubectl not found
- Instalar kubectl en el agente Jenkins
- Minikube configurará automáticamente kubectl con `minikube update-context`

### Error: minikube start failed
- Verificar que el agente tenga recursos suficientes (CPU, RAM)
- Verificar que Docker esté corriendo
- Verificar permisos del usuario para ejecutar minikube

### Error: minikube image load failed
- Verificar que minikube esté corriendo (`minikube status`)
- Verificar que la imagen Docker se haya construido correctamente
- Verificar permisos de Docker

### Error: Tests failed
- Revisar los logs de los tests
- Verificar que las dependencias estén disponibles
- Verificar configuración de base de datos/test containers

## Stack de Monitoreo

### Desplegar Prometheus, Grafana y SonarQube

Para desplegar el stack completo de monitoreo en minikube:

```bash
# Ejecutar el script de configuración
./kubernetes/monitoring/setup-monitoring.sh
```

Este script:
1. Crea el namespace `ecommerce-dev` si no existe
2. Despliega Prometheus con configuración para todos los microservicios
3. Despliega Grafana con datasource de Prometheus
4. Despliega SonarQube

### Acceder a los Servicios de Monitoreo

Una vez desplegados, puedes acceder a:

- **Prometheus**: http://$(minikube ip):30090
  - Scraping configurado para todos los microservicios
  - Métricas disponibles en `/actuator/prometheus` de cada servicio

- **Grafana**: http://$(minikube ip):30300
  - Usuario: `admin`
  - Contraseña: `admin`
  - Datasource: Prometheus (http://prometheus:9090)

- **SonarQube**: http://$(minikube ip):30000
  - Usuario por defecto: `admin`
  - Contraseña por defecto: `admin`
  - Se requiere cambiar la contraseña en el primer acceso

### Puertos de Microservicios

Cada microservicio usa un puerto específico:

| Microservicio | Puerto | Context Path |
|--------------|--------|--------------|
| user-service | 8700 | /user-service |
| product-service | 8500 | /product-service |
| payment-service | 8400 | /payment-service |
| order-service | 8300 | /order-service |
| shipping-service | 8600 | /shipping-service |
| proxy-client | 8900 | /app |

Los pipelines usan estos puertos para:
- Health checks en smoke tests
- Performance tests con Locust
- Configuración de Prometheus scraping

## Notas

- Los pipelines están configurados para el entorno de desarrollo usando **minikube local**
- Las imágenes Docker se cargan directamente en minikube sin necesidad de un registry externo
- Minikube se inicia automáticamente si no está corriendo
- El namespace `ecommerce-dev` se crea automáticamente si no existe
- Los servicios se exponen usando NodePort o `minikube service`
- SonarQube debe estar desplegado y accesible antes de ejecutar los pipelines
- Los tests de performance pueden fallar si el servicio no está completamente desplegado
- Los tests pueden fallar si los servicios dependientes no están disponibles
- El despliegue requiere que los deployments existan previamente en minikube
- Para producción o staging, usa los pipelines correspondientes (`Jenkinsfile-*-production.groovy` o `Jenkinsfile-*-stage.groovy`)

## Probar los Pipelines

Para probar los pipelines, consulta la guía completa en [TESTING.md](TESTING.md).

### Prueba Rápida

Usa el script de prueba incluido:

```bash
# Probar user-service
./pipelines/test-pipeline.sh user-service 1.0

# Probar product-service
./pipelines/test-pipeline.sh product-service 1.0

# Probar payment-service
./pipelines/test-pipeline.sh payment-service 1.0

# Probar order-service
./pipelines/test-pipeline.sh order-service 1.0

# Probar shipping-service
./pipelines/test-pipeline.sh shipping-service 1.0

# Probar proxy-client
./pipelines/test-pipeline.sh proxy-client 1.0
```

## Contacto

Para preguntas o problemas con los pipelines, contactar al equipo de DevOps.

