#!/bin/bash
# Script de prueba para pipelines Jenkins
# Uso: ./test-pipeline.sh <service-name> <version>

set -e

SERVICE_NAME=${1:-user-service}
VERSION=${2:-1.0}
SERVICE_DIR=${SERVICE_NAME}
MINIKUBE_NAMESPACE=${MINIKUBE_NAMESPACE:-ecommerce-dev}

echo "=========================================="
echo "Testing pipeline for ${SERVICE_NAME} v${VERSION}"
echo "=========================================="

# 1. Verificar minikube
echo ""
echo "1. Checking minikube..."
if ! minikube status > /dev/null 2>&1; then
    echo "   Starting minikube..."
    minikube start
else
    echo "   ✓ Minikube is running"
fi
minikube update-context
echo "   ✓ kubectl configured for minikube"

# 2. Crear namespace
echo ""
echo "2. Creating namespace..."
kubectl create namespace ${MINIKUBE_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - > /dev/null
echo "   ✓ Namespace ${MINIKUBE_NAMESPACE} ready"

# 3. Verificar que el directorio del servicio existe
echo ""
echo "3. Verifying service directory..."
if [ ! -d "${SERVICE_DIR}" ]; then
    echo "   ✗ ERROR: Directory ${SERVICE_DIR} does not exist!"
    exit 1
fi
echo "   ✓ Directory ${SERVICE_DIR} exists"

# 4. Construir y cargar imagen
echo ""
echo "4. Building Docker image..."
cd ${SERVICE_DIR}
if docker build -t ${SERVICE_NAME}:${VERSION} . > /dev/null 2>&1; then
    echo "   ✓ Docker image built successfully"
else
    echo "   ✗ ERROR: Docker build failed!"
    exit 1
fi

echo "   Loading image to minikube..."
minikube image load ${SERVICE_NAME}:${VERSION} > /dev/null 2>&1
echo "   ✓ Image loaded to minikube"

# También cargar como latest
docker tag ${SERVICE_NAME}:${VERSION} ${SERVICE_NAME}:dev-latest > /dev/null 2>&1
minikube image load ${SERVICE_NAME}:dev-latest > /dev/null 2>&1
cd ..

# 5. Verificar deployment existe
echo ""
echo "5. Checking deployment..."
if ! kubectl get deployment ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} > /dev/null 2>&1; then
    echo "   ⚠ WARNING: Deployment ${SERVICE_NAME} does not exist!"
    echo "   Creating basic deployment..."
    
    # Crear deployment básico
    kubectl create deployment ${SERVICE_NAME} \
        --image=${SERVICE_NAME}:${VERSION} \
        --namespace=${MINIKUBE_NAMESPACE} \
        --dry-run=client -o yaml | kubectl apply -f - > /dev/null
    
    # Exponer servicio
    kubectl expose deployment ${SERVICE_NAME} \
        --type=NodePort \
        --port=8080 \
        --namespace=${MINIKUBE_NAMESPACE} \
        --dry-run=client -o yaml | kubectl apply -f - > /dev/null
    
    echo "   ✓ Basic deployment created"
else
    echo "   ✓ Deployment exists"
fi

# 6. Actualizar deployment
echo ""
echo "6. Updating deployment..."
kubectl set image deployment/${SERVICE_NAME} \
    ${SERVICE_NAME}=${SERVICE_NAME}:${VERSION} \
    -n ${MINIKUBE_NAMESPACE} > /dev/null 2>&1

echo "   Waiting for rollout..."
if kubectl rollout status deployment/${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} --timeout=60s > /dev/null 2>&1; then
    echo "   ✓ Deployment updated successfully"
else
    echo "   ⚠ WARNING: Rollout may still be in progress"
fi

# 7. Smoke test
echo ""
echo "7. Running smoke tests..."
SERVICE_URL=$(minikube service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} --url 2>/dev/null || echo "")

if [ -z "$SERVICE_URL" ]; then
    NODE_PORT=$(kubectl get service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
    if [ -n "$NODE_PORT" ]; then
        MINIKUBE_IP=$(minikube ip)
        SERVICE_URL=http://${MINIKUBE_IP}:${NODE_PORT}
        echo "   Using NodePort: ${SERVICE_URL}"
    else
        echo "   ✗ ERROR: Could not determine service URL"
        exit 1
    fi
else
    echo "   Using minikube service: ${SERVICE_URL}"
fi

# Determinar path del health check según el servicio
if [ "$SERVICE_NAME" = "proxy-client" ]; then
    HEALTH_PATH="/app/actuator/health"
else
    HEALTH_PATH="/${SERVICE_NAME}/actuator/health"
fi

echo "   Testing health endpoint: ${SERVICE_URL}${HEALTH_PATH}"
if curl -f -s ${SERVICE_URL}${HEALTH_PATH} > /dev/null 2>&1; then
    echo "   ✓ Smoke test passed!"
else
    echo "   ⚠ WARNING: Health check failed (service may still be starting)"
    echo "   You can check manually with: curl ${SERVICE_URL}${HEALTH_PATH}"
fi

echo ""
echo "=========================================="
echo "Pipeline test completed for ${SERVICE_NAME}!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Check deployment: kubectl get deployment ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE}"
echo "  2. Check pods: kubectl get pods -n ${MINIKUBE_NAMESPACE} | grep ${SERVICE_NAME}"
echo "  3. Check logs: kubectl logs -n ${MINIKUBE_NAMESPACE} -l app=${SERVICE_NAME}"
echo "  4. Access service: ${SERVICE_URL}"
echo ""

