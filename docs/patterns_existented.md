# Patrones de Diseño Existentes en la Arquitectura

## 1. Patrones Arquitectónicos

### 1.1 Microservices Architecture Pattern
**Ubicación**: Toda la arquitectura  
**Descripción**: El sistema está dividido en múltiples servicios independientes:
- `user-service`: Gestión de usuarios y credenciales
- `product-service`: Gestión de productos y categorías
- `order-service`: Gestión de pedidos y carritos
- `payment-service`: Gestión de pagos
- `shipping-service`: Gestión de envíos
- `favourite-service`: Gestión de favoritos

**Beneficios**:
- Escalabilidad independiente de cada servicio
- Despliegue independiente
- Tecnologías específicas por servicio
- Aislamiento de fallos

### 1.2 API Gateway Pattern
**Ubicación**: [`api-gateway`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/api-gateway)  
**Descripción**: Punto de entrada único para todas las peticiones de clientes.

**Beneficios**:
- Simplifica la comunicación del cliente
- Centraliza autenticación y autorización
- Enrutamiento de peticiones
- Balanceo de carga

### 1.3 Service Discovery Pattern (Eureka)
**Ubicación**: [`service-discovery`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/service-discovery)  
**Descripción**: Registro y descubrimiento dinámico de servicios usando Netflix Eureka.

**Implementación**:
```yaml
# Configuración en cada servicio
spring:
  application:
    name: USER-SERVICE
```

**Beneficios**:
- Descubrimiento automático de servicios
- Balanceo de carga del lado del cliente
- Health checks automáticos

## 2. Patrones de Resiliencia

### 2.1 Circuit Breaker Pattern (Resilience4j)
**Ubicación**: Configurado en todos los microservicios  
**Archivo**: [`application.yml`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/proxy-client/src/main/resources/application.yml)

**Implementación**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      proxyService:
        register-health-indicator: true
        event-consumer-buffer-size: 10
        automatic-transition-from-open-to-half-open-enabled: true
        failure-rate-threshold: 50
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10
        wait-duration-in-open-state: 5s
        sliding-window-type: COUNT_BASED
```

**Beneficios**:
- Previene cascadas de fallos
- Recuperación automática
- Monitoreo de salud del servicio
- Fallback automático

**Estados del Circuit Breaker**:
- **CLOSED**: Funcionamiento normal
- **OPEN**: Demasiados fallos, rechaza peticiones
- **HALF_OPEN**: Prueba si el servicio se recuperó

## 3. Patrones de Configuración

### 3.1 External Configuration Pattern (Spring Cloud Config)
**Ubicación**: [`cloud-config`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/cloud-config)  
**Repositorio Git**: https://github.com/SelimHorri/cloud-config-server

**Implementación**:
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/SelimHorri/cloud-config-server
          clone-on-start: true
```

**Beneficios**:
- Configuración centralizada
- Cambios sin recompilar
- Versionamiento de configuración
- Diferentes perfiles (dev, stage, prod)

### 3.2 Profile-Based Configuration
**Ubicación**: Todos los servicios  
**Archivos**: `application-dev.yml`, `application-stage.yml`, `application-prod.yml`

**Beneficios**:
- Configuraciones específicas por entorno
- Fácil cambio entre entornos

## 4. Patrones de Comunicación

### 4.1 Feign Client Pattern
**Ubicación**: [`proxy-client/src/main/java/com/selimhorri/app/business`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/proxy-client/src/main/java/com/selimhorri/app/business)

**Ejemplo**:
```java
@FeignClient(name = "PRODUCT-SERVICE", 
             contextId = "productClientService", 
             path = "/product-service/api/products")
public interface ProductClientService {
    @GetMapping
    ResponseEntity<ProductProductServiceCollectionDtoResponse> findAll();
    
    @GetMapping("/{productId}")
    ResponseEntity<ProductDto> findById(@PathVariable("productId") String productId);
}
```

**Beneficios**:
- Comunicación declarativa entre servicios
- Integración con Service Discovery
- Balanceo de carga automático
- Manejo de errores integrado

## 5. Patrones de Diseño (GoF)

### 5.1 Repository Pattern
**Ubicación**: Todos los servicios de dominio  
**Ejemplo**: [`user-service/src/main/java/com/selimhorri/app/repository`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/user-service/src/main/java/com/selimhorri/app/repository)

**Beneficios**:
- Abstracción de la capa de datos
- Facilita testing con mocks
- Separación de responsabilidades

## 6. Patrones de Observabilidad

### 6.1 Distributed Tracing (Zipkin)
**Ubicación**: Configurado en todos los servicios

**Implementación**:
```yaml
spring:
  zipkin:
    base-url: http://localhost:9411/
```

**Beneficios**:
- Trazabilidad de peticiones distribuidas
- Análisis de latencia
- Debugging de sistemas distribuidos

### 6.2 Health Check Pattern
**Ubicación**: Spring Boot Actuator en todos los servicios

**Implementación**:
```yaml
management:
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

**Beneficios**:
- Monitoreo de salud de servicios
- Detección temprana de problemas
- Integración con orquestadores (Kubernetes)

## 7. Patrones de Seguridad

### 7.1 Proxy Pattern (proxy-client)
**Ubicación**: [`proxy-client`](file:///home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/proxy-client)

**Beneficios**:
- Centralización de autenticación
- Control de acceso
- Validación de peticiones

## Resumen de Patrones Identificados

| Categoría | Patrón | Estado | Ubicación |
|-----------|--------|--------|-----------|
| Arquitectónico | Microservices | Implementado | Toda la arquitectura |
| Arquitectónico | API Gateway | Implementado | api-gateway |
| Arquitectónico | Service Discovery | Implementado | service-discovery |
| Resiliencia | Circuit Breaker | Implementado | Todos los servicios |
| Configuración | External Configuration | Implementado | cloud-config |
| Configuración | Profile-Based Config | Implementado | Todos los servicios |
| Comunicación | Feign Client | Implementado | proxy-client |
| Observabilidad | Distributed Tracing | Implementado | Todos los servicios |
| Observabilidad | Health Check | Implementado | Todos los servicios |
| Observabilidad | Metrics | Implementado | Todos los servicios |
| Seguridad | Proxy | Implementado | proxy-client |
