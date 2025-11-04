# Comparación de Pipelines: Dev vs Stage vs Production

Este documento explica las diferencias entre los tres tipos de pipelines implementados para los microservicios.

## Resumen de Diferencias

| Aspecto | Dev Environment | Stage Environment | Production Environment |
|---------|----------------|-------------------|----------------------|
| **Objetivo** | Desarrollo local | Testing/Staging | Producción |
| **Kubernetes** | Minikube local | Cluster Kubernetes real | Cluster Kubernetes producción |
| **Docker Registry** | No requerido | Requerido | Requerido |
| **Imágenes** | Carga local en minikube | Push a registry | Push a registry con tags de release |
| **Tests contra app** | Local (mismo host) | Desplegada en K8s | Desplegada en K8s |
| **Performance Tests** | 10 usuarios, 60s | 20 usuarios, 120s | 30 usuarios, 180s |
| **Validación métricas** | Opcional | Obligatoria | Obligatoria (thresholds más estrictos) |
| **Release Notes** | No | No | **Sí (automático)** |
| **Change Management** | No | No | **Sí (aprobaciones)** |
| **Estrategias despliegue** | Rolling update | Rolling update | Rolling/Blue-Green/Canary |
| **Rollback automático** | No | No | **Sí (en caso de fallo)** |

## 1. Dev Environment

### Objetivo
Permite a los desarrolladores construir y probar localmente sus cambios en minikube con servicios de monitoreo.

### Características
- ✅ Construcción de imágenes Docker localmente
- ✅ Carga de imágenes directamente en minikube (sin registry)
- ✅ Despliegue en minikube local
- ✅ Stack de monitoreo local (Prometheus, Grafana, SonarQube)
- ✅ Tests unitarios, integración y E2E
- ✅ Tests de performance básicos (10 usuarios, 60s)
- ✅ Análisis de calidad (SonarQube)
- ✅ Security scan (OWASP + Trivy)

### Flujo
```
1. Checkout código
2. Build aplicación
3. Unit Tests
4. Integration Tests
5. E2E Tests
6. Code Quality (SonarQube)
7. Security Scan
8. Setup Minikube
9. Build Docker Image (local)
10. Load Image to Minikube
11. Deploy to Minikube
12. Smoke Tests
13. Performance Tests
```

### Ubicación
- `pipelines/dev/Jenkinsfile-{service}-dev.groovy`

### Cuándo usar
- Desarrollo local
- Testing rápido de cambios
- Validación antes de commit
- Desarrollo de nuevas features

---

## 2. Stage Environment

### Objetivo
Validar que la aplicación funciona correctamente en un entorno similar a producción antes de desplegar a producción.

### Características
- ✅ Construcción de imágenes Docker
- ✅ **Publicación a Docker Registry** (requerido)
- ✅ Despliegue en Kubernetes real (stage cluster)
- ✅ **Pruebas contra aplicación desplegada en Kubernetes**
- ✅ Tests E2E contra app desplegada
- ✅ Performance tests más intensos (20 usuarios, 120s)
- ✅ **Validación obligatoria de métricas** (thresholds)
- ✅ Análisis de calidad (SonarQube)
- ✅ Security scan (OWASP + Trivy)

### Flujo
```
1. Checkout código
2. Build aplicación
3. Unit Tests
4. Integration Tests
5. Code Quality (SonarQube)
6. Security Scan
7. Build Docker Image
8. Push to Docker Registry
9. Deploy to Kubernetes Stage
10. Wait for Deployment Ready
11. Get Service URL
12. Smoke Tests Against Deployed App
13. E2E Tests Against Deployed App
14. Performance Tests Against Deployed App
15. Validate Performance Metrics
```

### Ubicación
- `pipelines/stage/Jenkinsfile-{service}-stage.groovy`

### Cuándo usar
- Testing en entorno similar a producción
- Validación de integración con otros servicios
- Performance testing en condiciones reales
- Validación antes de producción

### Diferencias clave con Dev
- ❌ **No usa minikube** - usa cluster Kubernetes real
- ❌ **No carga imágenes localmente** - publica a Docker Registry
- ✅ **Pruebas contra app desplegada** - no contra app local
- ✅ **Validación obligatoria de métricas** - thresholds deben cumplirse

---

## 3. Production Environment (Master)

### Objetivo
Desplegar a producción con todas las validaciones, aprobaciones y documentación necesaria siguiendo buenas prácticas de Change Management.

### Características
- ✅ Construcción de imágenes Docker
- ✅ Publicación a Docker Registry con tags de release
- ✅ **Generación automática de Release Notes**
- ✅ **Change Management** (aprobaciones, validaciones)
- ✅ Despliegue en Kubernetes producción
- ✅ **Estrategias de despliegue** (Rolling/Blue-Green/Canary)
- ✅ Pruebas contra aplicación desplegada
- ✅ Performance tests intensivos (30 usuarios, 180s)
- ✅ **Validación estricta de métricas** (thresholds más altos)
- ✅ **Rollback automático** en caso de fallo
- ✅ Análisis de calidad (SonarQube)
- ✅ Security scan (OWASP + Trivy)
- ✅ **Validación de pruebas de sistema** (sanity checks)

### Flujo
```
1. Checkout código
2. Build aplicación
3. Unit Tests (OBLIGATORIO)
4. Integration Tests (OBLIGATORIO)
5. Code Quality (SonarQube)
6. Security Scan
7. Build Docker Image
8. Push to Docker Registry (con tag de release)
9. Generate Release Notes (automático)
10. Change Management Approval
11. Deploy to Kubernetes Production
12. Wait for Deployment Ready
13. Get Service URL
14. System Tests (Validación de sistema)
15. Smoke Tests Against Deployed App
16. E2E Tests Against Deployed App
17. Performance Tests Against Deployed App
18. Validate Performance Metrics (thresholds estrictos)
19. Health Check Validation
20. Rollback (si falla)
```

### Ubicación
- `pipelines/production/Jenkinsfile-{service}-production.groovy`

### Cuándo usar
- Despliegue a producción
- Releases oficiales
- Cuando se requiere documentación completa (Release Notes)
- Cuando se requiere aprobación (Change Management)

### Diferencias clave con Stage
- ✅ **Release Notes automáticos** - documentación de cambios
- ✅ **Change Management** - aprobaciones requeridas
- ✅ **System Tests** - validación adicional de sistema
- ✅ **Estrategias de despliegue** - blue-green, canary
- ✅ **Rollback automático** - en caso de fallo
- ✅ **Thresholds más estrictos** - métricas de producción
- ✅ **Tags de release** - versionado semántico

---

## Release Notes en Production

### ¿Qué son?
Documentos que describen los cambios, mejoras, correcciones y nuevas características incluidas en una versión.

### ¿Por qué son importantes?
- **Transparencia**: Los usuarios/stakeholders saben qué cambió
- **Change Management**: Documenta cambios para auditoría
- **Comunicación**: Facilita comunicación con equipos
- **Historial**: Mantiene historial de cambios

### Generación Automática
Los pipelines de producción generan Release Notes automáticamente desde:
- **Commits**: Mensajes de commits con formato convencional
- **Pull Requests**: Descripciones de PRs mergeados
- **Tags**: Tags de Git con versiones
- **Changelog**: Archivo CHANGELOG.md si existe

### Formato de Release Notes
```
# Release Notes - {SERVICE_NAME} v{VERSION}

## Fecha
{date}

## Cambios
### Nuevas Características
- Feature 1
- Feature 2

### Mejoras
- Improvement 1
- Improvement 2

### Correcciones
- Bug fix 1
- Bug fix 2

### Cambios Técnicos
- Technical change 1
- Technical change 2

## Despliegue
- **Cluster**: {cluster_name}
- **Namespace**: {namespace}
- **Imagen**: {docker_image}:{version}
- **Desplegado por**: {user}
- **Aprobado por**: {approver}
```

---

## Resumen Visual

### Dev Environment
```
[Code] → [Build] → [Tests] → [Docker Image] → [Minikube] → [Smoke Tests]
         Local      Local     Local            Local        Local
```

### Stage Environment
```
[Code] → [Build] → [Tests] → [Docker Image] → [Registry] → [K8s Stage] → [Tests vs Deployed]
         Local      Local     Local            External     Real Cluster   Real Environment
```

### Production Environment
```
[Code] → [Build] → [Tests] → [Docker Image] → [Registry] → [Release Notes] → [Approval] → [K8s Prod] → [System Tests] → [Deployed Tests]
         Local      Required  Local            External     Auto Generated   Required      Prod Cluster  Required       Real Environment
```

---

## Recomendaciones de Uso

### Para Desarrollo
- ✅ Usa **Dev Environment** para desarrollo local
- ✅ Valida cambios rápidamente en minikube
- ✅ Prueba nuevas features antes de commit

### Para Testing
- ✅ Usa **Stage Environment** para testing en condiciones reales
- ✅ Valida integración con otros servicios
- ✅ Performance testing antes de producción

### Para Producción
- ✅ Usa **Production Environment** para releases oficiales
- ✅ Requiere aprobación (Change Management)
- ✅ Genera Release Notes automáticamente
- ✅ Monitorea métricas estrictamente

---

## Próximos Pasos

1. **Crear pipelines de Production** con Release Notes automáticos
2. **Configurar Change Management** (aprobaciones)
3. **Configurar estrategias de despliegue** (blue-green, canary)
4. **Implementar rollback automático**

