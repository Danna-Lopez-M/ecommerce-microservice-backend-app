# Pruebas de seguridad (OWASP ZAP)

## Comandos ejecutados

Baseline (pasivo):
```powershell
docker run --rm -v "${PWD}:/zap/wrk" zaproxy/zap-stable ^
  zap-baseline.py -t http://host.docker.internal:8080 ^
  -r zap-baseline-report.html
```

Full scan (activo/intrusivo):
```powershell
docker run --rm -v "${PWD}:/zap/wrk" zaproxy/zap-stable ^
  zap-full-scan.py -t http://host.docker.internal:8080 ^
  -r zap-full-report.html
```

> Nota: si `host.docker.internal` no resuelve, usar la IP del host.

Full scan usando el gateway de Compose (misma red):
```powershell
docker run --rm --network ecommerce-microservice-backend-app_microservices_network `
  -v "${PWD}:/zap/wrk" `
  zaproxy/zap-stable `
  zap-full-scan.py -t http://api-gateway-container:8080 -r zap-full-report.html
```

## Evidencia
- `zap-baseline-report.html`
- `zap-full-report.html` (scan activo, sin alertas en `http://api-gateway-container:8080`)

## Hallazgos del full scan (resumen)
- **HTTP Only Site [10106]**: solo HTTP. Mitigar en entornos públicos habilitando HTTPS/redirección 301.
- **Private IP Disclosure [2]**: respuestas 404 exponían IP. Mitigado sirviendo `robots.txt` y `sitemap.xml` básicos y deshabilitando cache.
- **Spring Actuator Information Leak [40042]**: `/actuator/health` con detalles. Mitigado limitando exposición a `health,info` y `show-details=never` (gateway).

## Mitigaciones aplicadas
- Gateway: restringir endpoints de actuator a `health,info` y `show-details=never`.
- Gateway: deshabilitar cache en recursos y servir `robots.txt` / `sitemap.xml` mínimos para evitar 404 informativos.

## Cómo volver a ejecutar
- Baseline: ver comando anterior (reporta en `zap-baseline-report.html`).
- Full: ver comando anterior (reporta en `zap-full-report.html`).

Ejemplo con JWT (si se requiere auth):
```powershell
docker run --rm -v "${PWD}:/zap/wrk" zaproxy/zap-stable `
  zap-full-scan.py -t http://host.docker.internal:8080 `
  -r zap-full-report.html `
  -z "replacer.full_list(0).description=auth \
      replacer.full_list(0).enabled=true \
      replacer.full_list(0).matchtype=REQ_HEADER \
      replacer.full_list(0).matchstr=Authorization \
      replacer.full_list(0).regex=false \
      replacer.full_list(0).replacement=Bearer <TOKEN>"
```
