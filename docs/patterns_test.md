# Test Suite - Patrones de Diseño

## Resumen de Tests Creados

Se han creado **11 archivos de tests** cubriendo los tres patrones de diseño implementados:

### Tests Unitarios

1. **BulkheadPatternTest.java** - `user-service`
   - 10 tests para Bulkhead Pattern
   - Cobertura: Métodos normales y fallback
   - Verifica: Aislamiento de recursos y degradación controlada

2. **FeatureToggleServiceTest.java** - `feature-toggle-service`
   - 11 tests para Feature Toggle Service
   - Cobertura: CRUD, enable/disable, caché
   - Verifica: Lógica de negocio del servicio

3. **FeatureToggleAspectTest.java** - `feature-toggle-service`
   - 3 tests para AOP Aspect
   - Cobertura: Interceptación de métodos, fallback
   - Verifica: Comportamiento del aspecto

4. **CorrelationIdFilterTest.java** - `api-gateway`
   - 5 tests para Correlation ID Filter
   - Cobertura: Generación, extracción, MDC cleanup
   - Verifica: Manejo de headers y MDC

5. **FeignCorrelationIdInterceptorTest.java** - `proxy-client`
   - 4 tests para Feign Interceptor
   - Cobertura: Propagación de Correlation ID
   - Verifica: Headers en llamadas Feign

### Tests de Integración

6. **BulkheadIntegrationTest.java** - `user-service`
   - 5 tests de integración
   - Cobertura: Concurrencia, límites, aislamiento
   - Verifica: Comportamiento bajo carga

7. **FeatureToggleIntegrationTest.java** - `feature-toggle-service`
   - 10 tests de integración REST API
   - Cobertura: Todos los endpoints HTTP
   - Verifica: API completa con MockMvc

### Tests E2E

8. **CorrelationIdE2ETest.java** - `user-service`
   - 5 tests end-to-end
   - Cobertura: Generación, preservación, propagación
   - Verifica: Flujo completo de Correlation ID

9. **FeatureToggleE2ETest.java** - `feature-toggle-service`
   - 3 tests end-to-end
   - Cobertura: Workflow completo, múltiples entornos, caché
   - Verifica: Ciclo de vida completo de features

---

## Cobertura por Patrón

### 1. Bulkhead Pattern

**Tests Unitarios**:
- `testFindAll_Success()` - Operación normal
- `testFindAll_Fallback_ReturnsEmptyList()` - Fallback retorna lista vacía
- `testFindById_Success()` - Búsqueda exitosa
- `testFindById_NotFound_ThrowsException()` - Usuario no encontrado
- `testFindById_Fallback_ThrowsException()` - Fallback con excepción
- `testSave_Success()` - Guardado exitoso
- `testSave_Fallback_ThrowsException()` - Fallback en save
- `testDeleteById_Success()` - Eliminación exitosa
- `testDeleteById_Fallback_ThrowsException()` - Fallback en delete
- `testFindByUsername_Fallback_ThrowsException()` - Fallback en búsqueda

**Tests de Integración**:
- `testBulkhead_CriticalOperations_UnderLimit()` - 5 llamadas concurrentes
- `testBulkhead_NonCriticalOperations_UnderLimit()` - 3 llamadas concurrentes
- `testBulkhead_FallbackMethod_IsInvoked()` - Invocación de fallback
- `testBulkhead_DifferentInstances_AreIsolated()` - Aislamiento entre bulkheads
- `testBulkhead_Metrics_AreExposed()` - Métricas disponibles

**Cobertura Total**: 15 tests

---

### 2. Feature Toggle Pattern

**Tests Unitarios - Service**:
- `testCreateFeatureToggle()` - Creación de feature
- `testIsFeatureEnabled_WhenEnabled()` - Feature habilitado
- `testIsFeatureEnabled_WhenDisabled()` - Feature deshabilitado
- `testIsFeatureEnabled_WhenNotExists()` - Feature no existe
- `testEnableFeature()` - Habilitar feature
- `testDisableFeature()` - Deshabilitar feature
- `testFindAll()` - Listar todos
- `testFindByEnvironment()` - Filtrar por entorno
- `testUpdateFeature()` - Actualizar feature
- `testDeleteFeature()` - Eliminar feature
- `testCaching_IsFeatureEnabled()` - Verificar caché

**Tests Unitarios - Aspect**:
- `testCheckFeatureToggle_WhenEnabled_ShouldProceed()` - Feature habilitado procede
- `testCheckFeatureToggle_WhenDisabled_ShouldThrowException()` - Feature deshabilitado lanza excepción
- `testCheckFeatureToggle_WhenDisabledWithFallback_ShouldCallFallback()` - Invoca fallback

**Tests de Integración**:
- `testCreateFeature_Success()` - POST /api/features
- `testGetAllFeatures_Success()` - GET /api/features
- `testCheckFeature_Enabled()` - GET /api/features/check/{name}
- `testCheckFeature_Disabled()` - Feature deshabilitado
- `testCheckFeature_NotExists()` - Feature no existe
- `testEnableFeature_Success()` - PUT /api/features/{name}/enable
- `testDisableFeature_Success()` - PUT /api/features/{name}/disable
- `testUpdateFeature_Success()` - PUT /api/features/{id}
- `testDeleteFeature_Success()` - DELETE /api/features/{id}
- `testGetFeaturesByEnvironment_Success()` - GET /api/features/environment/{env}

**Tests E2E**:
- `testCompleteFeatureToggleWorkflow()` - Workflow completo (crear, verificar, enable, disable, delete)
- `testMultipleEnvironments()` - Features en múltiples entornos
- `testCachingBehavior()` - Comportamiento de caché

**Cobertura Total**: 27 tests

---

### 3. Correlation ID Pattern

**Tests Unitarios - Filter**:
- `testDoFilterInternal_WithExistingCorrelationId()` - ID existente preservado
- `testDoFilterInternal_WithoutCorrelationId_ShouldGenerate()` - Generación de ID
- `testDoFilterInternal_WithEmptyCorrelationId_ShouldGenerate()` - ID vacío genera nuevo
- `testDoFilterInternal_MDC_IsClearedAfterExecution()` - MDC limpiado
- `testDoFilterInternal_MDC_IsClearedEvenOnException()` - MDC limpiado en excepción

**Tests Unitarios - Feign Interceptor**:
- `testApply_WithCorrelationIdInMDC_ShouldAddHeader()` - Agrega header desde MDC
- `testApply_WithoutCorrelationIdInMDC_ShouldNotAddHeader()` - Sin MDC no agrega
- `testApply_WithNullCorrelationIdInMDC_ShouldNotAddHeader()` - MDC null no agrega
- `testApply_MultipleCalls_ShouldPropagateDifferentIds()` - Propaga IDs diferentes

**Tests E2E**:
- `testCorrelationId_GeneratedWhenNotProvided()` - Generación automática
- `testCorrelationId_PreservedWhenProvided()` - Preservación de ID custom
- `testCorrelationId_PropagatedAcrossMultipleRequests()` - Propagación en múltiples requests
- `testCorrelationId_DifferentForDifferentRequests()` - IDs únicos por request
- `testCorrelationId_ValidUUIDFormat()` - Formato UUID válido

**Cobertura Total**: 14 tests

---

## Resumen General

| Tipo de Test | Cantidad | Patrones Cubiertos |
|--------------|----------|-------------------|
| **Unitarios** | 33 tests | Bulkhead, Feature Toggle, Correlation ID |
| **Integración** | 15 tests | Bulkhead, Feature Toggle |
| **E2E** | 8 tests | Feature Toggle, Correlation ID |
| **TOTAL** | **56 tests** | 3 patrones completos |

---

## Ejecutar los Tests

### Tests Unitarios

```bash
# User Service (Bulkhead)
cd user-service
./mvnw test -Dtest=BulkheadPatternTest

# Feature Toggle Service
cd feature-toggle-service
./mvnw test -Dtest=FeatureToggleServiceTest
./mvnw test -Dtest=FeatureToggleAspectTest

# API Gateway (Correlation ID)
cd api-gateway
./mvnw test -Dtest=CorrelationIdFilterTest

# Proxy Client (Feign Interceptor)
cd proxy-client
./mvnw test -Dtest=FeignCorrelationIdInterceptorTest
```

### Tests de Integración

```bash
# Bulkhead Integration
cd user-service
./mvnw verify -Dit.test=BulkheadIntegrationTest

# Feature Toggle Integration
cd feature-toggle-service
./mvnw verify -Dit.test=FeatureToggleIntegrationTest
```

### Tests E2E

```bash
# Correlation ID E2E
cd user-service
./mvnw verify -Dit.test=CorrelationIdE2ETest

# Feature Toggle E2E
cd feature-toggle-service
./mvnw verify -Dit.test=FeatureToggleE2ETest
```

### Ejecutar Todos los Tests

```bash
# Desde el directorio raíz
./mvnw clean verify
```

---

## Verificación de Cobertura

### Herramientas Recomendadas

1. **JaCoCo** - Cobertura de código
2. **SonarQube** - Análisis de calidad (ya configurado en el proyecto)
3. **Maven Surefire Report** - Reportes de tests

### Generar Reporte de Cobertura

```bash
# Agregar JaCoCo al pom.xml y ejecutar
./mvnw clean test jacoco:report
```

---

## Métricas de Calidad

### Cobertura Esperada

- **Líneas de Código**: > 80%
- **Métodos**: > 85%

### Tests por Componente

| Componente | Unit | Integration | E2E | Total |
|------------|------|-------------|-----|-------|
| Bulkhead Pattern | 10 | 5 | 0 | 15 |
| Feature Toggle | 14 | 10 | 3 | 27 |
| Correlation ID | 9 | 0 | 5 | 14 |
| **TOTAL** | **33** | **15** | **8** | **56** |

---

## Notas Importantes

- Todos los tests usan **JUnit 5** y **Mockito**
- Tests de integración usan **@SpringBootTest**
- Tests E2E usan **TestRestTemplate** para requests HTTP reales