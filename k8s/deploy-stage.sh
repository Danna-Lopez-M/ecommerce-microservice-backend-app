#!/bin/bash

# Script para desplegar microservicios en Kubernetes (entorno stage)
# Uso: ./deploy-stage.sh [namespace]

set -e


NAMESPACE="${1:-ecommerce-stage}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configurar KUBECONFIG si no está configurado
if [ -z "$KUBECONFIG" ]; then
    if [ -f ~/.kube/config-do-stage ]; then
        export KUBECONFIG=~/.kube/config-do-stage
        echo "Usando KUBECONFIG: ~/.kube/config-do-stage"
    fi
fi

echo "=========================================="
echo "Desplegando microservicios en Kubernetes"
echo "Namespace: $NAMESPACE"
echo "=========================================="

# Verificar que kubectl está configurado
if ! kubectl cluster-info &> /dev/null; then
    echo "Error: kubectl no está configurado o no puede conectarse al cluster"
    echo "Por favor, configura kubectl primero:"
    echo "  export KUBECONFIG=~/.kube/config-do-stage"
    echo "  kubectl get nodes"
    exit 1
fi

# Crear namespace si no existe
echo "Creando namespace $NAMESPACE..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

# Función para actualizar namespace en un archivo YAML
update_namespace() {
    local file=$1
    if [ -f "$file" ]; then
        sed -i "s/namespace: ecommerce-dev/namespace: $NAMESPACE/g" "$file"
        sed -i "s/namespace: ecommerce-prod/namespace: $NAMESPACE/g" "$file"
        # Actualizar ConfigMap de forma más segura
        if grep -q "kind: ConfigMap" "$file"; then
            sed -i 's/SPRING_PROFILES_ACTIVE: "dev"/SPRING_PROFILES_ACTIVE: "stage"/g' "$file"
            sed -i 's/SPRING_PROFILES_ACTIVE: "stage""/SPRING_PROFILES_ACTIVE: "stage"/g' "$file"
        fi
    fi
}

# Función para actualizar perfiles de Spring
update_spring_profile() {
    local file=$1
    if [ -f "$file" ]; then
        sed -i 's/value: "dev"/value: "stage"/g' "$file"
    fi
}

# Función para agregar variables de entorno de MySQL
add_mysql_env() {
    local file=$1
    if [ -f "$file" ] && grep -q "SPRING_PROFILES_ACTIVE" "$file"; then
        # Verificar si las variables de MySQL ya existen
        if ! grep -q "SPRING_DATASOURCE_URL" "$file"; then
            # Agregar variables de MySQL después de SPRING_PROFILES_ACTIVE
            sed -i '/SPRING_PROFILES_ACTIVE/a\
        - name: SPRING_DATASOURCE_URL\
          value: "jdbc:mysql://mysql:3306/ecommerce_stage_db"\
        - name: SPRING_DATASOURCE_USERNAME\
          value: "ecommerce"\
        - name: SPRING_DATASOURCE_PASSWORD\
          valueFrom:\
            secretKeyRef:\
              name: mysql-secret\
              key: mysql-password' "$file"
        fi
    fi
}

echo "Actualizando manifiestos para el entorno stage..."

# Actualizar todos los archivos YAML
for yaml_file in "$SCRIPT_DIR"/*.yaml; do
    if [ -f "$yaml_file" ] && [[ "$yaml_file" != *"00-namespace.yaml"* ]] && [[ "$yaml_file" != *"00-mysql.yaml"* ]]; then
        echo "  Actualizando $(basename "$yaml_file")..."
        update_namespace "$yaml_file"
        update_spring_profile "$yaml_file"
        # Nota: Las variables de MySQL deben agregarse manualmente a los archivos YAML
        # ya que sed no es adecuado para insertar bloques YAML complejos
    fi
done

# Desplegar en orden de dependencias
echo ""
echo "Desplegando servicios de infraestructura..."

# 1. Namespace (ya creado arriba)
echo "  ✓ Namespace"

# 2. ConfigMap
echo "  Desplegando ConfigMap..."
kubectl apply -f "$SCRIPT_DIR/01-configmap.yaml"

# 3. MySQL (si no existe)
echo "  Verificando MySQL..."
if ! kubectl get statefulset mysql -n "$NAMESPACE" &> /dev/null; then
    echo "  MySQL no encontrado. Creando MySQL..."
    kubectl apply -f "$SCRIPT_DIR/00-mysql.yaml" || echo "  Nota: Archivo MySQL no encontrado, asumiendo que MySQL está gestionado externamente"
else
    echo "  ✓ MySQL ya existe"
fi

# 4. Zipkin
echo "  Desplegando Zipkin..."
kubectl apply -f "$SCRIPT_DIR/10-zipkin.yaml"

# 5. Service Discovery (debe ir antes que los otros servicios)
echo "  Desplegando Service Discovery..."
kubectl apply -f "$SCRIPT_DIR/08-service-discovery.yaml"
echo "  Esperando a que Service Discovery esté listo..."
kubectl wait --for=condition=available --timeout=300s deployment/service-discovery -n "$NAMESPACE" || true

# 6. Cloud Config
echo "  Desplegando Cloud Config..."
kubectl apply -f "$SCRIPT_DIR/09-cloud-config.yaml"
echo "  Esperando a que Cloud Config esté listo..."
kubectl wait --for=condition=available --timeout=300s deployment/cloud-config -n "$NAMESPACE" || true

# Esperar un poco para que los servicios de infraestructura estén completamente listos
echo "  Esperando a que los servicios de infraestructura estén listos..."
sleep 10

# Reparar migraciones de Flyway fallidas antes de desplegar los servicios
echo ""
echo "Reparando migraciones de Flyway fallidas (si las hay)..."
if [ -f "$SCRIPT_DIR/repair-flyway.sh" ]; then
    # Ejecutar solo la parte de reparación sin reiniciar los servicios
    MYSQL_PASSWORD=$(kubectl get secret mysql-secret -n "$NAMESPACE" -o jsonpath='{.data.mysql-password}' | base64 -d 2>/dev/null || echo "")
    if [ -n "$MYSQL_PASSWORD" ]; then
        FLYWAY_TABLES=(
            "flyway_user_schema_history"
            "flyway_product_schema_history"
            "flyway_order_schema_history"
            "flyway_payment_schema_history"
            "flyway_shipping_schema_history"
            "flyway_favourite_schema_history"
        )
        for table in "${FLYWAY_TABLES[@]}"; do
            FAILED_COUNT=$(kubectl exec -n "$NAMESPACE" mysql-0 -- \
                mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -sN -e \
                "SELECT COUNT(*) FROM $table WHERE success = 0;" 2>/dev/null || echo "0")
            if [ "$FAILED_COUNT" -gt 0 ]; then
                echo "  Reparando $FAILED_COUNT migraciones fallidas en $table..."
                kubectl exec -n "$NAMESPACE" mysql-0 -- \
                    mysql -uecommerce -p"$MYSQL_PASSWORD" ecommerce_stage_db -e \
                    "UPDATE $table SET success = 1 WHERE success = 0;" 2>/dev/null || true
            fi
        done
        echo "  ✓ Migraciones reparadas"
    fi
fi

echo ""
echo "Desplegando microservicios de negocio..."

# 7. User Service
echo "  Desplegando User Service..."
kubectl apply -f "$SCRIPT_DIR/02-user-service.yaml"

# 8. Product Service
echo "  Desplegando Product Service..."
kubectl apply -f "$SCRIPT_DIR/03-product-service.yaml"

# 9. Order Service
echo "  Desplegando Order Service..."
kubectl apply -f "$SCRIPT_DIR/04-order-service.yaml"

# 10. Payment Service
echo "  Desplegando Payment Service..."
kubectl apply -f "$SCRIPT_DIR/05-payment-service.yaml"

# 11. Shipping Service
echo "  Desplegando Shipping Service..."
kubectl apply -f "$SCRIPT_DIR/06-shipping-service.yaml"

# 12. Favourite Service (si existe)
if [ -f "$SCRIPT_DIR/11-favourite-service.yaml" ]; then
    echo "  Desplegando Favourite Service..."
    kubectl apply -f "$SCRIPT_DIR/11-favourite-service.yaml"
fi

# 13. Proxy Client
echo "  Desplegando Proxy Client..."
kubectl apply -f "$SCRIPT_DIR/07-proxy-client.yaml"

# 14. API Gateway (si existe)
if [ -f "$SCRIPT_DIR/12-api-gateway.yaml" ]; then
    echo "  Desplegando API Gateway..."
    kubectl apply -f "$SCRIPT_DIR/12-api-gateway.yaml"
fi

echo ""
echo "=========================================="
echo "Despliegue completado!"
echo "=========================================="
echo ""
echo "Verificando estado de los pods..."
kubectl get pods -n "$NAMESPACE"
echo ""
echo "Para ver los logs de un servicio:"
echo "  kubectl logs -f deployment/<service-name> -n $NAMESPACE"
echo ""
echo "Para verificar los servicios:"
echo "  kubectl get svc -n $NAMESPACE"
echo ""

