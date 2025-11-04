# Pipelines de CI/CD para Production Environment

Este directorio contiene los pipelines de Jenkins para la construcción, validación y despliegue de los microservicios en el entorno de **producción**.

## Microservicios con Pipelines de Production

Los siguientes microservicios tienen pipelines de CI/CD configurados para producción:

1. **user-service** - `Jenkinsfile-user-service-production.groovy`
2. **product-service** - `Jenkinsfile-product-service-production.groovy`
3. **payment-service** - `Jenkinsfile-payment-service-production.groovy`
4. **order-service** - `Jenkinsfile-order-service-production.groovy`
5. **shipping-service** - `Jenkinsfile-shipping-service-production.groovy`
6. **proxy-client** - `Jenkinsfile-proxy-client-production.groovy`

## Características Principales

### 1. Construcción y Tests
- ✅ Construcción de aplicación
- ✅ **Tests unitarios obligatorios** (no se puede omitir)
- ✅ **Tests de integración obligatorios** (no se puede omitir)
- ✅ Code Quality Analysis (SonarQube)
- ✅ Security Scan (OWASP + Trivy)

### 2. Generación Automática de Release Notes
- ✅ **Generación automática** desde commits de Git
- ✅ Categorización automática (features, fixes, breaking changes)
- ✅ Información de despliegue incluida
- ✅ Formato profesional siguiendo buenas prácticas de Change Management

### 3. Change Management
- ✅ **Aprobación requerida** antes del despliegue
- ✅ Documentación de aprobador y desplegador
- ✅ Notas de despliegue opcionales

### 4. Despliegue en Kubernetes
- ✅ Backup automático del deployment actual
- ✅ **Estrategias de despliegue** (Rolling, Blue-Green, Canary)
- ✅ Despliegue en Kubernetes producción
- ✅ Espera a que el deployment esté listo

### 5. Validación de Pruebas de Sistema
- ✅ **System Tests**: Validación de endpoints del sistema
- ✅ **Smoke Tests**: Pruebas básicas contra app desplegada
- ✅ **E2E Tests**: Tests end-to-end contra app desplegada
- ✅ **Performance Tests**: Tests intensivos (30 usuarios, 180s)
- ✅ **Validación estricta de métricas**: Thresholds más estrictos que stage

### 6. Rollback Automático
- ✅ **Rollback automático** en caso de fallo (opcional)
- ✅ Restauración a versión anterior

## Estructura del Pipeline

### 1. Checkout
- Clona el repositorio desde la rama `master`
- Obtiene información de Git para Release Notes

### 2. Version Management
- Versionado semántico automático
- Basado en tags de Git o commit messages
- Formatos: `[major]`, `[minor]`, `[patch]`

### 3. Build Application
- Compila la aplicación con Maven

### 4. Unit Tests (OBLIGATORIO)
- Tests unitarios **obligatorios**
- Falla el pipeline si los tests fallan
- Publica resultados con JUnit

### 5. Integration Tests (OBLIGATORIO)
- Tests de integración **obligatorios**
- Falla el pipeline si los tests fallan
- Publica resultados con JUnit

### 6. Code Quality Analysis - SonarQube
- Análisis de calidad de código
- Project key: `${SERVICE_NAME}-production`

### 7. Security Scan
- OWASP Dependency Check
- Trivy para imágenes Docker

### 8. Build and Push Docker Image
- Construye imagen Docker
- Publica en Docker Registry con tags:
  - `${RELEASE_VERSION}` (ej: v1.2.3)
  - `latest`
  - `production-latest`

### 9. Generate Release Notes (AUTOMÁTICO)
- **Generación automática** desde commits de Git
- Categoriza cambios:
  - ⚠️ Breaking Changes
  - ✨ New Features
  - 🔧 Improvements
  - 🐛 Bug Fixes
  - 📝 Other Changes
- Incluye información de despliegue
- Sigue buenas prácticas de Change Management

### 10. Change Management Approval
- **Aprobación requerida** antes del despliegue
- Timeout: 30 minutos
- Captura aprobador y notas de despliegue

### 11. Backup Current Deployment
- Backup automático del deployment actual
- Backup del service
- Archivos YAML guardados como artefactos

### 12. Deploy to Kubernetes Production
- Despliegue según estrategia seleccionada:
  - **Rolling Update**: Actualización gradual (por defecto)
  - **Blue-Green**: Despliegue en paralelo
  - **Canary**: Despliegue parcial para validación
- Namespace: `ecommerce-prod`
- Timeout: 10 minutos

### 13. Wait for Deployment Ready
- Espera a que el deployment esté disponible
- Timeout: 300 segundos
- Espera adicional de 30 segundos

### 14. Get Service URL
- Obtiene URL del servicio desplegado
- Soporta LoadBalancer, NodePort y ClusterIP

### 15. System Tests - Validation
- **Validación de pruebas de sistema**:
  - Health check
  - Info endpoint
  - Metrics endpoint
  - Prometheus endpoint
- Valida que todos los endpoints estén disponibles

### 16. Smoke Tests Against Deployed Application
- Smoke tests contra la aplicación desplegada
- Health check e info endpoint

### 17. E2E Tests Against Deployed Application
- E2E tests contra la aplicación desplegada
- Publica resultados con JUnit

### 18. Performance Tests Against Deployed Application
- Performance tests intensivos:
  - Usuarios: 30
  - Spawn rate: 6 usuarios/segundo
  - Duración: 180 segundos
- Genera reportes HTML y CSV

### 19. Validate Performance Metrics
- **Validación estricta de métricas**:
  - Average Response Time: máximo 1500ms (más estricto que stage)
  - Error Rate: máximo 1% (más estricto que stage)
  - Requests per Second: mínimo 15 (más alto que stage)
- Falla el pipeline si los thresholds no se cumplen

### 20. Health Check Validation
- Health check múltiple (5 intentos)
- Valida estabilidad del servicio

## Generación Automática de Release Notes

### Formato de Release Notes

```markdown
# Release Notes - {SERVICE_NAME} {RELEASE_VERSION}

**Release Date:** {date}
**Build Number:** {build_number}
**Commit:** {commit_short}
**Branch:** {branch}
**Author:** {author}

## Overview
This release includes changes from the master branch...

## ⚠️ Breaking Changes
- Change 1

## ✨ New Features
- Feature 1

## 🔧 Improvements
- Improvement 1

## 🐛 Bug Fixes
- Fix 1

## 🚀 Deployment Information
- Docker Image
- Kubernetes Deployment
- Test Results
- Performance Metrics

## 📋 Change Management
- Approvals
- Rollback Plan
```

### Categorización Automática

Los commits se categorizan automáticamente según su mensaje:

- **Breaking Changes**: `[breaking]`, `BREAKING`
- **Features**: `[feature]`, `feat:`
- **Fixes**: `[fix]`, `fix:`
- **Improvements**: `[improvement]`, `improve:`
- **Other**: Resto de commits

### Información Incluida

- Cambios desde el último tag (o últimos 50 commits)
- Información de despliegue (Docker image, Kubernetes namespace)
- Resultados de tests
- Métricas de performance
- Plan de rollback
- Links a repositorio, imagen Docker y build

## Configuración Requerida

### Variables de Entorno

1. **DOCKER_REGISTRY**: URL del Docker Registry
2. **KUBERNETES_CONTEXT**: Contexto de Kubernetes (por defecto: `production`)
3. **KUBERNETES_NAMESPACE**: Namespace de Kubernetes (por defecto: `ecommerce-prod`)
4. **SONAR_HOST_URL**: URL de SonarQube
5. **GIT_REPO**: URL del repositorio Git

### Credenciales en Jenkins

1. **Docker Registry Credentials** (`docker-credentials`)
2. **SonarQube Token** (`sonar-token`)
3. **Kubernetes Config** (`kubeconfig-prod`) - Opcional si se usa contexto configurado

### Parámetros del Pipeline

- **SKIP_TESTS**: Saltar tests (NO RECOMENDADO)
- **ROLLBACK_ON_FAILURE**: Rollback automático en caso de fallo (por defecto: true)
- **PERFORMANCE_TEST**: Ejecutar tests de performance (por defecto: true)
- **DEPLOYMENT_STRATEGY**: Estrategia de despliegue (rolling, blue-green, canary)

## Prerequisitos

- **Jenkins con plugins**:
  - Docker Pipeline
  - Kubernetes CLI
  - JUnit
  - SonarQube Scanner
  - HTML Publisher
  
- **Herramientas instaladas**:
  - Docker
  - kubectl (configurado con acceso al cluster de producción)
  - Maven
  - Python 3 y Locust
  
- **Infraestructura**:
  - Cluster de Kubernetes (producción)
  - Docker Registry accesible
  - SonarQube accesible
  - Deployments creados previamente en el namespace `ecommerce-prod`

## Uso del Pipeline

### Crear un Job en Jenkins

1. Crear un nuevo Pipeline Job
2. En "Pipeline Definition", seleccionar "Pipeline script from SCM"
3. Configurar:
   - SCM: Git
   - Repository URL: `https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git`
   - Branch: `master`
   - Script Path: `pipelines/production/Jenkinsfile-{service-name}-production.groovy`

### Ejecutar Manualmente

1. Configurar parámetros del pipeline
2. Ejecutar manualmente desde Jenkins
3. **Aprobar despliegue** cuando se solicite
4. Monitorear el progreso

### Versionado Semántico

El pipeline calcula automáticamente la versión basándose en:

- **Último tag**: Busca el último tag de Git
- **Commit message**: Identifica `[major]`, `[minor]` o `[patch]`
- **Incremento automático**: Incrementa según el tipo detectado

Ejemplos:
- `[major]` → v1.0.0 → v2.0.0
- `[minor]` → v1.0.0 → v1.1.0
- `[patch]` → v1.0.0 → v1.0.1

## Change Management

### Aprobación Requerida

El pipeline requiere aprobación manual antes del despliegue:

- **Timeout**: 30 minutos
- **Aprobador**: Se captura automáticamente
- **Notas de despliegue**: Opcionales

### Rollback Automático

Si el parámetro `ROLLBACK_ON_FAILURE` está habilitado:

- Se ejecuta rollback automático en caso de fallo
- Restaura a la versión anterior del deployment
- Usa `kubectl rollout undo`

## Troubleshooting

### Error: Unit/Integration tests failed
- Los tests son obligatorios y no se pueden omitir
- Revisar logs de tests
- Corregir errores antes de continuar

### Error: Change Management approval timeout
- El timeout es de 30 minutos
- Aprobar el despliegue antes del timeout

### Error: Performance metrics validation failed
- Los thresholds de producción son más estrictos
- Revisar métricas en el reporte de performance
- Ajustar thresholds si es necesario (no recomendado)

### Error: Deployment failed - Rollback initiated
- El rollback se ejecuta automáticamente si está habilitado
- Revisar logs del deployment
- Verificar que la versión anterior esté disponible

## Notas Importantes

- ⚠️ **Los tests unitarios e integración son OBLIGATORIOS** - no se pueden omitir
- ⚠️ **La aprobación de Change Management es REQUERIDA** - el pipeline espera aprobación
- ⚠️ **Los thresholds de performance son más estrictos** que en stage
- ✅ **Release Notes se generan automáticamente** desde commits de Git
- ✅ **Rollback automático** está habilitado por defecto
- ✅ **Backup automático** del deployment actual antes del despliegue

## Diferencias con Stage Environment

| Aspecto | Stage | Production |
|---------|-------|------------|
| **Tests obligatorios** | No (pueden omitirse) | **Sí (obligatorios)** |
| **Change Management** | No | **Sí (aprobación requerida)** |
| **Release Notes** | No | **Sí (automático)** |
| **System Tests** | No | **Sí (validación de sistema)** |
| **Performance Users** | 20 usuarios | **30 usuarios** |
| **Performance Duration** | 120s | **180s** |
| **Thresholds** | Menos estrictos | **Más estrictos** |
| **Rollback** | No | **Sí (automático)** |
| **Backup** | No | **Sí (automático)** |
| **Estrategias despliegue** | Rolling | **Rolling/Blue-Green/Canary** |

## Próximos Pasos

1. **Crear Deployments**: Crear deployments en namespace `ecommerce-prod`
2. **Configurar Jenkins**: Configurar jobs con credenciales
3. **Configurar Change Management**: Definir aprobadores
4. **Probar Pipeline**: Ejecutar pipeline manualmente para validar
5. **Configurar Notificaciones**: Configurar notificaciones de despliegue

