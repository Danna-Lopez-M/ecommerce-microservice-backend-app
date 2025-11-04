#!/bin/bash
# Script para configurar Prometheus, Grafana y SonarQube en minikube

set -e

NAMESPACE=${MINIKUBE_NAMESPACE:-ecommerce-dev}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================="
echo "Setting up monitoring stack in minikube"
echo "=========================================="

# 1. Crear namespace si no existe
echo ""
echo "1. Creating namespace ${NAMESPACE}..."
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# 2. Configurar Prometheus
echo ""
echo "2. Setting up Prometheus..."
kubectl apply -f ${SCRIPT_DIR}/prometheus-config.yaml
kubectl apply -f ${SCRIPT_DIR}/prometheus-deployment.yaml

echo "   Waiting for Prometheus to be ready..."
kubectl wait --for=condition=available --timeout=120s deployment/prometheus -n ${NAMESPACE} || true

# 3. Configurar Grafana
echo ""
echo "3. Setting up Grafana..."
kubectl apply -f ${SCRIPT_DIR}/grafana-deployment.yaml
kubectl apply -f ${SCRIPT_DIR}/grafana-datasource.yaml

echo "   Waiting for Grafana to be ready..."
kubectl wait --for=condition=available --timeout=120s deployment/grafana -n ${NAMESPACE} || true

# 4. Configurar SonarQube
echo ""
echo "4. Setting up SonarQube..."
kubectl apply -f ${SCRIPT_DIR}/sonarqube-deployment.yaml

echo "   Waiting for SonarQube to be ready..."
kubectl wait --for=condition=available --timeout=180s deployment/sonarqube -n ${NAMESPACE} || true

# 5. Obtener URLs
echo ""
echo "=========================================="
echo "Monitoring stack setup completed!"
echo "=========================================="
echo ""
echo "Access URLs (using minikube ip):"
echo ""

MINIKUBE_IP=$(minikube ip)

echo "Prometheus:"
PROMETHEUS_PORT=$(kubectl get service prometheus -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
echo "  http://${MINIKUBE_IP}:${PROMETHEUS_PORT}"
echo "  Or use: minikube service prometheus -n ${NAMESPACE} --url"

echo ""
echo "Grafana:"
GRAFANA_PORT=$(kubectl get service grafana -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
echo "  http://${MINIKUBE_IP}:${GRAFANA_PORT}"
echo "  Username: admin"
echo "  Password: admin"
echo "  Or use: minikube service grafana -n ${NAMESPACE} --url"

echo ""
echo "SonarQube:"
SONARQUBE_PORT=$(kubectl get service sonarqube -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
echo "  http://${MINIKUBE_IP}:${SONARQUBE_PORT}"
echo "  Default credentials: admin/admin"
echo "  Or use: minikube service sonarqube -n ${NAMESPACE} --url"

echo ""
echo "To check status:"
echo "  kubectl get pods -n ${NAMESPACE} | grep -E 'prometheus|grafana|sonarqube'"
echo ""

