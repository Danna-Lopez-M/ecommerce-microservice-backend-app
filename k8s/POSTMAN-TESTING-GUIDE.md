# Guía de Pruebas con Postman

## Información de Acceso

### API Gateway (Recomendado - Punto de Entrada Principal)
- **URL Base**: `http://45.55.107.21:8080`
- **Tipo**: LoadBalancer (accesible desde internet)

### Servicios Individuales (NodePort - Acceso Directo)
Si necesitas acceder directamente a un servicio sin pasar por el API Gateway:

- **User Service**: `http://<NODE_IP>:30220/user-service`
- **Product Service**: `http://<NODE_IP>:31402/product-service`
- **Order Service**: `http://<NODE_IP>:30673/order-service`
- **Payment Service**: `http://<NODE_IP>:31631/payment-service`
- **Shipping Service**: `http://<NODE_IP>:30833/shipping-service`
- **Favourite Service**: `http://<NODE_IP>:31522/favourite-service`

**IPs de los Nodos**:
- `137.184.96.188`
- `192.34.63.125`
- `178.128.153.199`

### Service Discovery (Eureka)
- **URL**: `http://<NODE_IP>:30406`
- **Dashboard**: `http://<NODE_IP>:30406` (ver servicios registrados)

### Proxy Client
- **URL Base**: `http://146.190.196.21:8900/app`

---

## Configuración en Postman

### 1. Crear un Environment

Crea un nuevo environment en Postman con las siguientes variables:

```
BASE_URL: http://45.55.107.21:8080
PROXY_CLIENT_URL: http://146.190.196.21:8900
```

### 2. Headers Comunes

Para todas las peticiones, agrega estos headers:

```
Content-Type: application/json
Accept: application/json
```

---

## Endpoints por Microservicio

### User Service

**Base Path**: `/user-service`

#### Health Check
```
GET {{BASE_URL}}/user-service/actuator/health
```

#### Crear Usuario
```
POST {{BASE_URL}}/user-service/api/users
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "firstName": "Test",
  "lastName": "User"
}
```

#### Obtener Usuario por ID
```
GET {{BASE_URL}}/user-service/api/users/{id}
```

#### Obtener Todos los Usuarios
```
GET {{BASE_URL}}/user-service/api/users
```

#### Actualizar Usuario
```
PUT {{BASE_URL}}/user-service/api/users/{id}
Content-Type: application/json

{
  "username": "updateduser",
  "email": "updated@example.com",
  "firstName": "Updated",
  "lastName": "User"
}
```

#### Eliminar Usuario
```
DELETE {{BASE_URL}}/user-service/api/users/{id}
```

---

### Product Service

**Base Path**: `/product-service`

#### Health Check
```
GET {{BASE_URL}}/product-service/actuator/health
```

#### Crear Producto
```
POST {{BASE_URL}}/product-service/api/products
Content-Type: application/json

{
  "name": "Producto Test",
  "description": "Descripción del producto",
  "price": 99.99,
  "stock": 100,
  "categoryId": 1
}
```

#### Obtener Producto por ID
```
GET {{BASE_URL}}/product-service/api/products/{id}
```

#### Obtener Todos los Productos
```
GET {{BASE_URL}}/product-service/api/products
```

#### Actualizar Producto
```
PUT {{BASE_URL}}/product-service/api/products/{id}
Content-Type: application/json

{
  "name": "Producto Actualizado",
  "price": 149.99,
  "stock": 50
}
```

#### Eliminar Producto
```
DELETE {{BASE_URL}}/product-service/api/products/{id}
```

#### Obtener Productos por Categoría
```
GET {{BASE_URL}}/product-service/api/products/category/{categoryId}
```

---

### Order Service

**Base Path**: `/order-service`

#### Health Check
```
GET {{BASE_URL}}/order-service/actuator/health
```

#### Crear Orden
```
POST {{BASE_URL}}/order-service/api/orders
Content-Type: application/json

{
  "userId": 1,
  "orderItems": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 99.99
    }
  ],
  "totalAmount": 199.98
}
```

#### Obtener Orden por ID
```
GET {{BASE_URL}}/order-service/api/orders/{id}
```

#### Obtener Órdenes por Usuario
```
GET {{BASE_URL}}/order-service/api/orders/user/{userId}
```

#### Obtener Todas las Órdenes
```
GET {{BASE_URL}}/order-service/api/orders
```

#### Actualizar Estado de Orden
```
PUT {{BASE_URL}}/order-service/api/orders/{id}/status
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

---

### Payment Service

**Base Path**: `/payment-service`

#### Health Check
```
GET {{BASE_URL}}/payment-service/actuator/health
```

#### Procesar Pago
```
POST {{BASE_URL}}/payment-service/api/payments
Content-Type: application/json

{
  "orderId": 1,
  "amount": 199.98,
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "4111111111111111",
  "expiryDate": "12/25",
  "cvv": "123"
}
```

#### Obtener Pago por ID
```
GET {{BASE_URL}}/payment-service/api/payments/{id}
```

#### Obtener Pagos por Orden
```
GET {{BASE_URL}}/payment-service/api/payments/order/{orderId}
```

---

### Shipping Service

**Base Path**: `/shipping-service`

#### Health Check
```
GET {{BASE_URL}}/shipping-service/actuator/health
```

#### Crear Envío
```
POST {{BASE_URL}}/shipping-service/api/shippings
Content-Type: application/json

{
  "orderId": 1,
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA"
}
```

#### Obtener Envío por ID
```
GET {{BASE_URL}}/shipping-service/api/shippings/{id}
```

#### Obtener Envíos por Orden
```
GET {{BASE_URL}}/shipping-service/api/shippings/order/{orderId}
```

#### Actualizar Estado de Envío
```
PUT {{BASE_URL}}/shipping-service/api/shippings/{id}/status
Content-Type: application/json

{
  "status": "IN_TRANSIT"
}
```

---

### Favourite Service

**Base Path**: `/favourite-service`

#### Health Check
```
GET {{BASE_URL}}/favourite-service/actuator/health
```

#### Agregar a Favoritos
```
POST {{BASE_URL}}/favourite-service/api/favourites
Content-Type: application/json

{
  "userId": 1,
  "productId": 1
}
```

#### Obtener Favoritos por Usuario
```
GET {{BASE_URL}}/favourite-service/api/favourites/user/{userId}
```

#### Eliminar de Favoritos
```
DELETE {{BASE_URL}}/favourite-service/api/favourites/{id}
```

---

## Flujo de Prueba Completo

### 1. Verificar que todos los servicios estén funcionando

```bash
# Health checks de todos los servicios
GET {{BASE_URL}}/user-service/actuator/health
GET {{BASE_URL}}/product-service/actuator/health
GET {{BASE_URL}}/order-service/actuator/health
GET {{BASE_URL}}/payment-service/actuator/health
GET {{BASE_URL}}/shipping-service/actuator/health
GET {{BASE_URL}}/favourite-service/actuator/health
```

### 2. Flujo E-commerce Completo

1. **Crear un usuario**
   ```
   POST {{BASE_URL}}/user-service/api/users
   ```

2. **Crear productos**
   ```
   POST {{BASE_URL}}/product-service/api/products
   ```

3. **Agregar productos a favoritos**
   ```
   POST {{BASE_URL}}/favourite-service/api/favourites
   ```

4. **Crear una orden**
   ```
   POST {{BASE_URL}}/order-service/api/orders
   ```

5. **Procesar el pago**
   ```
   POST {{BASE_URL}}/payment-service/api/payments
   ```

6. **Crear el envío**
   ```
   POST {{BASE_URL}}/shipping-service/api/shippings
   ```

---

## Verificación de Service Discovery

Para verificar que todos los servicios están registrados en Eureka:

```
GET http://<NODE_IP>:30406/eureka/apps
```

O accede al dashboard en el navegador:
```
http://<NODE_IP>:30406
```

---

## Troubleshooting

### Si un endpoint no responde:

1. **Verifica el health check del servicio**:
   ```
   GET {{BASE_URL}}/<service-name>/actuator/health
   ```

2. **Verifica que el servicio esté registrado en Eureka**:
   ```
   GET http://<NODE_IP>:30406/eureka/apps/<SERVICE-NAME>
   ```

3. **Revisa los logs del pod**:
   ```bash
   kubectl logs -n ecommerce-stage <pod-name> --tail=100
   ```

4. **Verifica el estado del pod**:
   ```bash
   kubectl get pods -n ecommerce-stage
   ```

### Errores Comunes

- **404 Not Found**: Verifica que la ruta sea correcta y que el servicio esté registrado en Eureka
- **503 Service Unavailable**: El servicio no está disponible o no está registrado en Eureka
- **500 Internal Server Error**: Revisa los logs del servicio para más detalles

---

## Notas Importantes

1. **El API Gateway es el punto de entrada recomendado** - Enruta automáticamente las peticiones a los servicios correctos usando Service Discovery (Eureka)

2. **Los servicios tienen context-path** - Todos los endpoints deben incluir el context-path del servicio (ej: `/user-service/api/users`)

3. **CORS está configurado** - El API Gateway tiene CORS habilitado para permitir peticiones desde el frontend

4. **Trazabilidad** - Todas las peticiones pasan por Zipkin para trazabilidad distribuida

---

## Colección de Postman

Puedes crear una colección en Postman con todas estas peticiones organizadas por carpetas:

```
📁 E-commerce Microservices
  📁 Health Checks
  📁 User Service
  📁 Product Service
  📁 Order Service
  📁 Payment Service
  📁 Shipping Service
  📁 Favourite Service
```

