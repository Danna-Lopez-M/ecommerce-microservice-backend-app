#!/bin/bash

# Script para actualizar recursos de todos los microservicios
# Esto soluciona los problemas de CrashLoopBackOff por falta de recursos

set -e

export KUBECONFIG=~/.kube/config-do-stage

echo "=========================================="
echo "Actualizando recursos de microservicios"
echo "=========================================="
echo ""

# Lista de servicios a actualizar (excepto MySQL, Zipkin y Service Discovery que ya están bien)
SERVICES=(
    "cloud-config"
    "user-service"
    "product-service"
    "order-service"
    "payment-service"
    "shipping-service"
    "favourite-service"
    "proxy-client"
    "api-gateway"
)

for service in "${SERVICES[@]}"; do
    echo "Actualizando $service..."
    
    # Aumentar recursos CPU y memoria
    kubectl patch deployment $service -n ecommerce-stage --type='json' -p='[
        {
            "op": "replace",
            "path": "/spec/template/spec/containers/0/resources/requests/cpu",
            "value": "100m"
        },
        {
            "op": "replace",
            "path": "/spec/template/spec/containers/0/resources/requests/memory",
            "value": "256Mi"
        },
        {
            "op": "replace",
            "path": "/spec/template/spec/containers/0/resources/limits/cpu",
            "value": "500m"
        },
        {
            "op": "replace",
            "path": "/spec/template/spec/containers/0/resources/limits/memory",
            "value": "512Mi"
        }
    ]' 2>/dev/null || echo "  ⚠ No se pudo actualizar $service (puede que no exista o no tenga recursos definidos)"
    
    echo "  ✓ $service actualizado"
done

echo ""
echo "=========================================="
echo "✓ Recursos actualizados!"
echo "=========================================="
echo ""
echo "Esperando 30 segundos para que los pods se reinicien..."
sleep 30

echo ""
echo "Estado actual de los pods:"
kubectl get pods -n ecommerce-stage

echo ""
echo "Monitorea el estado con:"
echo "  kubectl get pods -n ecommerce-stage --watch"
