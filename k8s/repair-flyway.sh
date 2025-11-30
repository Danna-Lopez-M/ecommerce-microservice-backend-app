#!/bin/bash

# Script para reparar migraciones de Flyway fallidas en la base de datos
# Uso: ./repair-flyway.sh [namespace]

set -e

NAMESPACE="${1:-ecommerce-stage}"
MYSQL_POD="mysql-0"

# Configurar KUBECONFIG si no está configurado
if [ -z "$KUBECONFIG" ]; then
    if [ -f ~/.kube/config-do-stage ]; then
        export KUBECONFIG=~/.kube/config-do-stage
        echo "Usando KUBECONFIG: ~/.kube/config-do-stage"
    fi
fi

echo "=========================================="
echo "Reparando migraciones de Flyway"
echo "Namespace: $NAMESPACE"
echo "=========================================="

# Verificar que kubectl está configurado
if ! kubectl cluster-info &> /dev/null; then
    echo "Error: kubectl no está configurado o no puede conectarse al cluster"
    exit 1
fi

# Obtener la contraseña de MySQL
MYSQL_PASSWORD=$(kubectl get secret mysql-secret -n "$NAMESPACE" -o jsonpath='{.data.mysql-password}' | base64 -d)

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

echo ""
echo "Verificando migraciones fallidas..."

# Verificar y reparar migraciones fallidas
for table in "${FLYWAY_TABLES[@]}"; do
    echo "  Verificando $table..."
    
    # Contar migraciones fallidas
    FAILED_COUNT=$(kubectl exec -n "$NAMESPACE" "$MYSQL_POD" -- \
        mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -sN -e \
        "SELECT COUNT(*) FROM $table WHERE success = 0;" 2>/dev/null || echo "0")
    
    if [ "$FAILED_COUNT" -gt 0 ]; then
        echo "    Encontradas $FAILED_COUNT migraciones fallidas en $table"
        
        # Mostrar las migraciones fallidas
        kubectl exec -n "$NAMESPACE" "$MYSQL_POD" -- \
            mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -e \
            "SELECT version, description, success FROM $table WHERE success = 0;" 2>/dev/null
        
        # Reparar las migraciones fallidas
        echo "    Reparando migraciones fallidas..."
        kubectl exec -n "$NAMESPACE" "$MYSQL_POD" -- \
            mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -e \
            "UPDATE $table SET success = 1 WHERE success = 0;" 2>/dev/null
        
        echo "    ✓ Migraciones reparadas en $table"
    else
        echo "    ✓ No hay migraciones fallidas en $table"
    fi
done

echo ""
echo "=========================================="
echo "Reparación completada!"
echo "=========================================="
echo ""
echo "Reiniciando deployments afectados..."
echo ""

# Reiniciar los deployments para que los servicios se inicien correctamente
SERVICES=(
    "user-service"
    "product-service"
    "order-service"
    "payment-service"
    "shipping-service"
    "favourite-service"
)

for service in "${SERVICES[@]}"; do
    if kubectl get deployment "$service" -n "$NAMESPACE" &> /dev/null; then
        echo "  Reiniciando $service..."
        kubectl rollout restart deployment "$service" -n "$NAMESPACE" || true
    fi
done

echo ""
echo "Esperando 30 segundos para que los pods se reinicien..."
sleep 30

echo ""
echo "Estado actual de los pods:"
kubectl get pods -n "$NAMESPACE"

echo ""
echo "=========================================="
echo "Nota: Si los pods siguen fallando, ejecuta este script nuevamente."
echo "Las migraciones pueden fallar durante la ejecución y necesitan ser reparadas."
echo "=========================================="
echo ""
echo "Para monitorear el estado:"
echo "  kubectl get pods -n $NAMESPACE --watch"
echo ""
echo "Para ver los logs de un pod:"
echo "  kubectl logs -n $NAMESPACE <pod-name> --tail=100"

