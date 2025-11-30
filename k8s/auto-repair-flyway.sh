#!/bin/bash

# Script para reparar automáticamente las migraciones de Flyway en un loop
# Útil cuando las migraciones fallan continuamente durante el despliegue
# Uso: ./auto-repair-flyway.sh [namespace] [intervalo_en_segundos]

set -e

NAMESPACE="${1:-ecommerce-stage}"
INTERVAL="${2:-30}"
MYSQL_POD="mysql-0"

# Configurar KUBECONFIG si no está configurado
if [ -z "$KUBECONFIG" ]; then
    if [ -f ~/.kube/config-do-stage ]; then
        export KUBECONFIG=~/.kube/config-do-stage
    fi
fi

echo "=========================================="
echo "Reparación automática de migraciones Flyway"
echo "Namespace: $NAMESPACE"
echo "Intervalo: $INTERVAL segundos"
echo "Presiona Ctrl+C para detener"
echo "=========================================="

# Obtener la contraseña de MySQL
MYSQL_PASSWORD=$(kubectl get secret mysql-secret -n "$NAMESPACE" -o jsonpath='{.data.mysql-password}' | base64 -d 2>/dev/null || echo "")

if [ -z "$MYSQL_PASSWORD" ]; then
    echo "Error: No se pudo obtener la contraseña de MySQL"
    exit 1
fi

# Lista de tablas de historial de Flyway
FLYWAY_TABLES=(
    "flyway_user_schema_history"
    "flyway_product_schema_history"
    "flyway_order_schema_history"
    "flyway_payment_schema_history"
    "flyway_shipping_schema_history"
    "flyway_favourite_schema_history"
)

# Función para reparar migraciones
repair_migrations() {
    local repaired=0
    for table in "${FLYWAY_TABLES[@]}"; do
        FAILED_COUNT=$(kubectl exec -n "$NAMESPACE" "$MYSQL_POD" -- \
            mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -sN -e \
            "SELECT COUNT(*) FROM $table WHERE success = 0;" 2>/dev/null || echo "0")
        
        if [ "$FAILED_COUNT" -gt 0 ]; then
            echo "[$(date +'%H:%M:%S')] Reparando $FAILED_COUNT migraciones fallidas en $table..."
            kubectl exec -n "$NAMESPACE" "$MYSQL_POD" -- \
                mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -e \
                "UPDATE $table SET success = 1 WHERE success = 0;" 2>/dev/null || true
            repaired=$((repaired + FAILED_COUNT))
        fi
    done
    
    if [ $repaired -gt 0 ]; then
        echo "[$(date +'%H:%M:%S')] ✓ Reparadas $repaired migraciones"
        return 0
    else
        return 1
    fi
}

# Loop principal
ITERATION=0
while true; do
    ITERATION=$((ITERATION + 1))
    
    if repair_migrations; then
        echo "[$(date +'%H:%M:%S')] Esperando $INTERVAL segundos antes de la siguiente verificación..."
    else
        echo "[$(date +'%H:%M:%S')] No hay migraciones fallidas. Verificando nuevamente en $INTERVAL segundos..."
    fi
    
    sleep "$INTERVAL"
done

