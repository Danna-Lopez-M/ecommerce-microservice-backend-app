# GitHub Actions CI/CD Pipelines

Este repositorio contiene pipelines de CI/CD configurados con GitHub Actions para todos los microservicios del proyecto ecommerce.

## Características Implementadas

### ✅ Ambientes Separados (Stage y Prod)
- **Stage**: Se activa en push a la rama `stage`
- **Prod**: Se activa en push a la rama `master`
- Promoción controlada: Los cambios deben pasar por stage antes de llegar a prod

### ✅ Análisis Estático con SonarQube
- Análisis de código automático en cada PR y push
- Integración con SonarCloud o SonarQube self-hosted
- Reportes de calidad de código y cobertura

### ✅ Escaneo de Vulnerabilidades con Trivy
- Escaneo automático de imágenes Docker
- Detección de vulnerabilidades críticas y altas
- Integración con GitHub Security

### ✅ Versionado Semántico Automático
- Calcula versiones automáticamente basándose en:
  - Último tag del repositorio
  - Tipo de commit (feat, fix, etc.)
  - Versión en pom.xml como fallback
- Formato: `MAJOR.MINOR.PATCH`
- Para stage: `VERSION-stage-SHA`
- Para prod: `VERSION`

### ✅ Notificaciones Automáticas
- Envío de emails cuando un pipeline falla
- Configuración opcional mediante secrets de SMTP
- Incluye información detallada del error

## Estructura de Pipelines

### Workflows Reutilizables

1. **`reusable-service-pipeline.yml`**: Pipeline completo para builds y deployments
   - Calcula versión semántica
   - Ejecuta tests
   - Análisis SonarQube
   - Escaneo Trivy
   - Build y push de imagen Docker
   - Notificaciones en caso de fallo

2. **`reusable-pr-pipeline.yml`**: Pipeline simplificado para Pull Requests
   - Ejecuta tests
   - Análisis SonarQube
   - Escaneo Trivy
   - Notificaciones en caso de fallo

### Pipelines por Servicio

Cada servicio tiene 4 pipelines:
- `{service}-pipeline-stage-push.yml`: Deploy a stage en push
- `{service}-pipeline-prod-push.yml`: Deploy a prod en push
- `{service}-pipeline-stage-pr.yml`: Validación en PRs a stage
- `{service}-pipeline-prod-pr.yml`: Validación en PRs a prod

## Servicios Configurados

- `api-gateway`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`
- `shipping-service`
- `favourite-service`
- `proxy-client`
- `service-discovery`
- `cloud-config`

## Flujo de Trabajo

### Desarrollo Normal
1. Crear una rama feature
2. Hacer cambios y commit
3. Crear Pull Request a `stage` o `master`
4. El pipeline PR se ejecuta automáticamente:
   - Build y tests
   - Análisis SonarQube
   - Escaneo Trivy
5. Si todo pasa, mergear el PR
6. Al hacer push a `stage` o `master`, se ejecuta el pipeline de push:
   - Calcula nueva versión
   - Build y tests
   - Análisis SonarQube
   - Escaneo Trivy
   - Build y push de imagen Docker con la nueva versión

### Versionado Semántico

El sistema calcula automáticamente la versión basándose en:
- **Features** (`feat:`): Incrementa MINOR (1.2.3 → 1.3.0)
- **Breaking changes** (`feat!:` o `!:`): Incrementa MAJOR (1.2.3 → 2.0.0)
- **Fixes** (`fix:`): Incrementa PATCH (1.2.3 → 1.2.4)
- **Otros commits**: Incrementa PATCH por defecto

## Configuración Requerida

Ver [SECRETS_CONFIGURATION.md](./SECRETS_CONFIGURATION.md) para detalles sobre los secrets que deben configurarse.

### Secrets Mínimos Requeridos
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

### Secrets Opcionales (pero recomendados)
- `SONAR_TOKEN`
- `SONAR_HOST_URL`
- `SMTP_SERVER`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `NOTIFICATION_EMAIL`

## Solución de Problemas

### Error: "invalid tag"
**Problema**: El tag de Docker está vacío o mal formado.

**Solución**: Este error ya está resuelto. El sistema ahora calcula automáticamente las versiones. Si aún ves este error, verifica que:
1. El workflow esté usando el workflow reutilizable
2. Los secrets de Docker estén configurados correctamente

### SonarQube no funciona
**Problema**: El análisis de SonarQube falla.

**Solución**: 
1. Verifica que `SONAR_TOKEN` y `SONAR_HOST_URL` estén configurados
2. El análisis es opcional - el pipeline continuará aunque falle
3. Revisa los logs del job `sonarqube-analysis` para más detalles

### No recibo notificaciones por email
**Problema**: No se envían emails cuando falla el pipeline.

**Solución**:
1. Verifica que todos los secrets de SMTP estén configurados
2. Para Gmail, usa una "App Password", no tu contraseña normal
3. Revisa los logs del job `notify-on-failure` para ver errores de conexión SMTP

## Mejoras Futuras

- [ ] Integración con Kubernetes para deployment automático
- [ ] Rollback automático en caso de fallos
- [ ] Métricas de performance en los pipelines
- [ ] Integración con Slack/Teams además de email
- [ ] Caché de dependencias de Maven mejorado

## Contribuir

Al agregar un nuevo servicio:
1. Crea los 4 archivos de pipeline siguiendo el patrón existente
2. Asegúrate de actualizar el mapeo de servicios si es necesario
3. Verifica que el Dockerfile esté en la ubicación correcta

