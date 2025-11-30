# Solución para Migraciones de Flyway Fallidas

## Problema

Los servicios están fallando con `CrashLoopBackOff` debido a migraciones de Flyway que fallan durante la ejecución y se marcan como fallidas en el historial de la base de datos.

## Soluciones Implementadas

### 1. Configuración en los Deployments

Se agregaron las siguientes variables de entorno a todos los servicios:

- `SPRING_FLYWAY_REPAIR_ON_MIGRATE=true` - Intenta reparar automáticamente
- `SPRING_FLYWAY_IGNORE_MISSING_MIGRATIONS=true` - Ignora migraciones faltantes
- `SPRING_FLYWAY_IGNORE_IGNORED_MIGRATIONS=true` - Ignora migraciones ignoradas

### 2. Scripts de Reparación

#### `repair-flyway.sh`
Script manual que repara todas las migraciones fallidas y reinicia los servicios.

**Uso:**
```bash
cd k8s
./repair-flyway.sh
```

#### `auto-repair-flyway.sh`
Script que se ejecuta en un loop continuo, reparando automáticamente las migraciones fallidas cada 30 segundos (configurable).

**Uso:**
```bash
cd k8s
# Ejecutar en segundo plano
nohup ./auto-repair-flyway.sh ecommerce-stage 30 > flyway-repair.log 2>&1 &

# O en una terminal separada para monitorear
./auto-repair-flyway.sh ecommerce-stage 30
```

**Parámetros:**
- Primer argumento: namespace (default: `ecommerce-stage`)
- Segundo argumento: intervalo en segundos (default: `30`)

**Detener el script:**
```bash
# Si está en segundo plano, encontrar el proceso y matarlo
ps aux | grep auto-repair-flyway
kill <PID>

# O si está en una terminal, presionar Ctrl+C
```

## Uso Recomendado

### Durante el Despliegue Inicial

1. Ejecuta el script de reparación automática en segundo plano:
   ```bash
   cd k8s
   nohup ./auto-repair-flyway.sh > flyway-repair.log 2>&1 &
   ```

2. Despliega los servicios:
   ```bash
   ./deploy-stage.sh
   ```

3. Monitorea los pods:
   ```bash
   kubectl get pods -n ecommerce-stage --watch
   ```

4. Una vez que todos los servicios estén funcionando, detén el script de reparación automática:
   ```bash
   pkill -f auto-repair-flyway
   ```

### Si un Servicio Falla Después del Despliegue

Ejecuta el script de reparación manual:
```bash
cd k8s
./repair-flyway.sh
```

## Verificación

Para verificar si hay migraciones fallidas:
```bash
kubectl exec -n ecommerce-stage mysql-0 -- mysql -uecommerce -p$(kubectl get secret mysql-secret -n ecommerce-stage -o jsonpath='{.data.mysql-password}' | base64 -d) ecommerce_stage_db -e "SELECT 'flyway_user_schema_history' as table_name, version, description, success FROM flyway_user_schema_history WHERE success = 0 UNION ALL SELECT 'flyway_product_schema_history', version, description, success FROM flyway_product_schema_history WHERE success = 0 UNION ALL SELECT 'flyway_order_schema_history', version, description, success FROM flyway_order_schema_history WHERE success = 0 UNION ALL SELECT 'flyway_payment_schema_history', version, description, success FROM flyway_payment_schema_history WHERE success = 0 UNION ALL SELECT 'flyway_shipping_schema_history', version, description, success FROM flyway_shipping_schema_history WHERE success = 0 UNION ALL SELECT 'flyway_favourite_schema_history', version, description, success FROM flyway_favourite_schema_history WHERE success = 0;"
```

## Notas Importantes

1. **Las migraciones pueden fallar durante la ejecución**: Esto es normal cuando hay problemas de conectividad, recursos insuficientes, o errores en las migraciones SQL.

2. **El script de reparación automática es temporal**: Una vez que todas las migraciones se completen correctamente, el problema debería desaparecer.

3. **Monitoreo continuo**: Si las migraciones siguen fallando, verifica los logs de los pods para identificar la causa raíz:
   ```bash
   kubectl logs -n ecommerce-stage <pod-name> --tail=100
   ```

4. **Recursos del cluster**: Asegúrate de que el cluster tenga suficientes recursos (CPU/memoria) para ejecutar todas las migraciones.

## Troubleshooting

### Si los pods siguen en CrashLoopBackOff después de reparar:

1. Verifica los logs del pod:
   ```bash
   kubectl logs -n ecommerce-stage <pod-name> --tail=100
   ```

2. Verifica si hay nuevas migraciones fallidas:
   ```bash
   ./repair-flyway.sh
   ```

3. Verifica los recursos del cluster:
   ```bash
   kubectl top nodes
   kubectl top pods -n ecommerce-stage
   ```

4. Considera aumentar los recursos de los pods si es necesario.

