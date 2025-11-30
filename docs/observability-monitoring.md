# Observabilidad y Monitoreo

Stack listo para métricas, trazas y logs:

- **Prometheus** (`core.yml`): scrapea `/actuator/prometheus` de todos los microservicios.
- **Grafana** (`core.yml` + `observability/grafana/**`): dashboards técnicos, por servicio, negocio (orders) y logs.
- **Zipkin** (`core.yml`): tracing distribuido (Sleuth ya integrado).
- **ELK** (`core.yml` + `observability/logging/*`): Elasticsearch + Logstash + Kibana + Filebeat (lee `./logs/*.log`).

## Endpoints rápidos
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- Logstash API: http://localhost:9600
- Actuator Prometheus (ejemplos):
  - API Gateway: http://localhost:8080/actuator/prometheus
  - Order Service: http://localhost:8300/order-service/actuator/prometheus
  - Product Service: http://localhost:8500/product-service/actuator/prometheus
  - Payment Service: http://localhost:8400/payment-service/actuator/prometheus
  - Shipping Service: http://localhost:8600/shipping-service/actuator/prometheus
  - User Service: http://localhost:8700/user-service/actuator/prometheus
  - Proxy Client: http://localhost:8900/app/actuator/prometheus
  - Service Discovery: http://localhost:8761/actuator/prometheus
  - Cloud Config: http://localhost:9296/actuator/prometheus

## Dashboards en Grafana (carpeta Ecommerce)
- **Microservices - Technical Overview**: RPS, error rate, latencia p95, heap, circuit breaker, up/down.
- **Service - Technical Detail**: filtros por servicio, mismo set técnico enfocado.
- **Orders - Business Metrics**: órdenes creadas/actualizadas/eliminadas, p95 y promedio de `orderFee`.
- **Logs - Overview**: stream de logs y recuento por nivel/servicio (datasource ElasticsearchLogs).

## Alertas (Prometheus)
En `observability/prometheus/alert.rules.yml`:
- ServiceDown
- HighErrorRate (5xx > 5%)
- HighLatencyP95 (> 1s)
- PaymentQueueBacklog (fallos en payment)

## Logs centralizados
- Filebeat lee `./logs/*.log` → Logstash (5044) → Elasticsearch (`ecommerce-logs-*`) → Kibana.
- En Kibana (Stack Management > Index Patterns/Data Views), usar `ecommerce-logs-*` con `@timestamp`. Discover muestra los logs recientes.

## Trazas
- Sleuth + Zipkin integrados; UI en http://localhost:9411.

## Salud / readiness
- Probes configuradas en `k8s/*.yaml` hacia `/actuator/health` (o `/<service>/actuator/health` según context-path).

## Métricas de negocio (order-service)
- Counters: `orders_created_total`, `orders_updated_total`, `orders_deleted_total`.
- Histograma/summary: `order_value_amount_USD_*`.
- Dashboard “Orders - Business Metrics” ya las consume.

## Cómo levantar todo
```bash
# Core (Prometheus, Grafana, ELK, Zipkin, etc.)
docker compose -f core.yml up -d
# Servicios de la app
docker compose -f compose.yml up -d
```

## Generar tráfico / métricas
- Hacer peticiones a los servicios (ej. order-service) o escribir líneas en `./logs/*.log` para verlas en Kibana.
- Ejemplo rápido (PowerShell):
```powershell
echo "$(Get-Date) TEST observability ok" >> logs/order-service.log
echo "$(Get-Date) TEST observability ok" >> logs/api-gateway.log
```

