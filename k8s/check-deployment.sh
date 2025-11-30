#!/bin/bash

# Script para verificar el estado del despliegue en Kubernetes
# Uso: ./check-deployment.sh [namespace]

set -e

NAMESPACE="${1:-ecommerce-stage}"

echo "=========================================="
echo "Verificando estado del despliegue"
echo "Namespace: $NAMESPACE"
echo "=========================================="
echo ""

# Verificar namespace
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo "❌ Namespace '$NAMESPACE' no existe"
    exit 1
fi

echo "📦 Estado de los Pods:"
echo "----------------------"
kubectl get pods -n "$NAMESPACE" -o wide
echo ""

echo "🔌 Estado de los Servicios:"
echo "----------------------------"
kubectl get svc -n "$NAMESPACE"
echo ""

echo "📊 Estado de los Deployments:"
echo "------------------------------"
kubectl get deployments -n "$NAMESPACE"
echo ""

# Verificar pods en estado de error
echo "⚠️  Pods con problemas:"
echo "----------------------"
ERROR_PODS=$(kubectl get pods -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\n"}{end}' | grep -v -E "(Running|Succeeded)" | cut -f1)
if [ -z "$ERROR_PODS" ]; then
    echo "✅ Todos los pods están corriendo correctamente"
else
    echo "$ERROR_PODS" | while read pod; do
        if [ ! -z "$pod" ]; then
            PHASE=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.phase}')
            REASON=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.containerStatuses[0].state.waiting.reason}' 2>/dev/null || echo "N/A")
            MESSAGE=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.conditions[?(@.type=="PodScheduled")].message}' 2>/dev/null || echo "")
            echo "❌ $pod: $PHASE"
            if [ ! -z "$REASON" ] && [ "$REASON" != "N/A" ]; then
                echo "   Razón: $REASON"
            fi
            if [ ! -z "$MESSAGE" ]; then
                echo "   Mensaje: $MESSAGE"
            fi
        fi
    done
fi
echo ""

# Verificar servicios de infraestructura
echo "🏗️  Servicios de Infraestructura:"
echo "----------------------------------"
INFRA_SERVICES=("mysql" "zipkin" "service-discovery" "cloud-config")
for service in "${INFRA_SERVICES[@]}"; do
    if kubectl get deployment "$service" -n "$NAMESPACE" &> /dev/null; then
        READY=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
        DESIRED=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
        if [ "$READY" == "$DESIRED" ]; then
            echo "✅ $service: $READY/$DESIRED replicas listas"
        else
            echo "⏳ $service: $READY/$DESIRED replicas listas"
        fi
    elif kubectl get statefulset "$service" -n "$NAMESPACE" &> /dev/null; then
        READY=$(kubectl get statefulset "$service" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
        DESIRED=$(kubectl get statefulset "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
        if [ "$READY" == "$DESIRED" ]; then
            echo "✅ $service: $READY/$DESIRED replicas listas"
        else
            echo "⏳ $service: $READY/$DESIRED replicas listas"
        fi
    else
        echo "❌ $service: No encontrado"
    fi
done
echo ""

# Verificar microservicios
echo "🚀 Microservicios:"
echo "------------------"
MICROSERVICES=("user-service" "product-service" "order-service" "payment-service" "shipping-service" "favourite-service" "proxy-client" "api-gateway")
for service in "${MICROSERVICES[@]}"; do
    if kubectl get deployment "$service" -n "$NAMESPACE" &> /dev/null; then
        READY=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
        DESIRED=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
        if [ "$READY" == "$DESIRED" ]; then
            echo "✅ $service: $READY/$DESIRED replicas listas"
        else
            echo "⏳ $service: $READY/$DESIRED replicas listas"
        fi
    else
        echo "⚠️  $service: No desplegado"
    fi
done
echo ""

# Obtener IPs externas
echo "🌐 Endpoints Externos:"
echo "----------------------"
EXTERNAL_SVCS=$(kubectl get svc -n "$NAMESPACE" -o jsonpath='{.items[?(@.spec.type=="LoadBalancer")].metadata.name}')
if [ -z "$EXTERNAL_SVCS" ]; then
    echo "ℹ️  No hay servicios LoadBalancer configurados"
else
    for svc in $EXTERNAL_SVCS; do
        EXTERNAL_IP=$(kubectl get svc "$svc" -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
        PORT=$(kubectl get svc "$svc" -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].port}')
        if [ -z "$EXTERNAL_IP" ]; then
            echo "⏳ $svc: Esperando IP externa (puerto $PORT)"
        else
            echo "✅ $svc: http://$EXTERNAL_IP:$PORT"
        fi
    done
fi
echo ""

echo "=========================================="
echo "Para ver logs de un servicio:"
echo "  kubectl logs -f deployment/<service-name> -n $NAMESPACE"
echo ""
echo "Para port-forward a un servicio:"
echo "  kubectl port-forward svc/<service-name> <local-port>:<service-port> -n $NAMESPACE"
echo "=========================================="

