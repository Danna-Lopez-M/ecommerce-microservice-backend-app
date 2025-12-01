#!/bin/bash

# Script para configurar KUBECONFIG permanentemente
# Esto es necesario para que ./deploy-stage.sh funcione

set -e

echo "=========================================="
echo "Configurando kubectl para DigitalOcean"
echo "=========================================="
echo ""

# Agregar a .bashrc si no existe
if ! grep -q "KUBECONFIG=~/.kube/config-do-stage" ~/.bashrc 2>/dev/null; then
    echo "Agregando KUBECONFIG a ~/.bashrc..."
    echo "" >> ~/.bashrc
    echo "# DigitalOcean Kubernetes config" >> ~/.bashrc
    echo "export KUBECONFIG=~/.kube/config-do-stage" >> ~/.bashrc
    echo "✓ Agregado a ~/.bashrc"
else
    echo "✓ KUBECONFIG ya está en ~/.bashrc"
fi

# Exportar para la sesión actual
export KUBECONFIG=~/.kube/config-do-stage

echo ""
echo "Testing connection..."
kubectl get nodes

echo ""
echo "=========================================="
echo "✓ kubectl configured successfully!"
echo "=========================================="
echo ""
echo "KUBECONFIG is now set to: ~/.kube/config-do-stage"
echo ""
echo "To apply in current terminal:"
echo "  source ~/.bashrc"
echo ""
echo "Or export manually:"
echo "  export KUBECONFIG=~/.kube/config-do-stage"
