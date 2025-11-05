#!/bin/bash

NAMESPACE="ecommerce-dev"
SERVICES=("zipkin" "service-discovery" "cloud-config" "user-service" "product-service" "order-service" "payment-service" "shipping-service" "proxy-client")

echo "════════════════════════════════════════"
echo "🔍 VERIFICACIÓN COMPLETA DEL CLUSTER"
echo "════════════════════════════════════════"
echo ""

for service in "${SERVICES[@]}"; do
    echo "📦 Verificando $service..."
    
    PODS=$(kubectl get pods -n $NAMESPACE -l app=$service -o jsonpath='{.items[*].metadata.name}' 2>/dev/null)
    
    if [ -z "$PODS" ]; then
        echo "  ❌ No hay pods para $service"
        continue
    fi
    
    for pod in $PODS; do
        STATUS=$(kubectl get pod $pod -n $NAMESPACE -o jsonpath='{.status.phase}')
        READY=$(kubectl get pod $pod -n $NAMESPACE -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}')
        RESTARTS=$(kubectl get pod $pod -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}')
        
        if [ "$READY" == "True" ]; then
            echo "  ✅ $pod - Ready (Restarts: $RESTARTS)"
        else
            echo "  ⚠️  $pod - Not Ready (Status: $STATUS, Restarts: $RESTARTS)"
            
            # Mostrar últimas 5 líneas de log
            echo "     Últimos logs:"
            kubectl logs $pod -n $NAMESPACE --tail=5 2>/dev/null | sed 's/^/       /'
            echo ""
        fi
    done
    echo ""
done

echo "════════════════════════════════════════"
echo "📊 RESUMEN"
echo "════════════════════════════════════════"
kubectl get pods -n $NAMESPACE -o wide
