#!/bin/bash

NAMESPACE="ecommerce-dev"

echo "🧹 Limpiando deployments anteriores..."
kubectl delete deployment --all -n $NAMESPACE 2>/dev/null
sleep 5

echo ""
echo "1️⃣ Desplegando Zipkin..."
kubectl apply -f k8s/10-zipkin.yaml
sleep 10

echo "2️⃣ Desplegando Service Discovery (Eureka)..."
kubectl apply -f k8s/08-service-discovery.yaml
echo "⏳ Esperando 60s para que Eureka inicie..."
sleep 60

echo "3️⃣ Desplegando Cloud Config..."
kubectl apply -f k8s/09-cloud-config.yaml
echo "⏳ Esperando 60s para que Cloud Config inicie..."
sleep 60

echo ""
echo "4️⃣ Desplegando microservicios..."
kubectl apply -f k8s/02-user-service.yaml
kubectl apply -f k8s/03-product-service.yaml
kubectl apply -f k8s/04-order-service.yaml
kubectl apply -f k8s/05-payment-service.yaml
kubectl apply -f k8s/06-shipping-service.yaml

echo "⏳ Esperando 30s..."
sleep 30

kubectl apply -f k8s/07-proxy-client.yaml

echo ""
echo "⏳ Esperando 3 minutos para que todos los servicios inicien..."
sleep 180

echo ""
echo "════════════════════════════════════════"
echo "📊 ESTADO DE LOS PODS"
echo "════════════════════════════════════════"
kubectl get pods -n $NAMESPACE

echo ""
echo "════════════════════════════════════════"
echo "📊 ESTADO DE LOS SERVICIOS"
echo "════════════════════════════════════════"
kubectl get services -n $NAMESPACE

echo ""
echo "Para ver logs de un servicio:"
echo "kubectl logs -l app=user-service -n $NAMESPACE --tail=50"
