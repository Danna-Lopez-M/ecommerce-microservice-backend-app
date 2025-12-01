#!/bin/bash
# Script to update resources in YAML files

cd /home/danna/Desktop/2.Ingesoft/ecommerce-microservice-backend-app/k8s

# Files to update (excluding mysql and service-discovery which are already handled/custom)
FILES=(
    "01-configmap.yaml"
    "02-user-service.yaml"
    "03-product-service.yaml"
    "04-order-service.yaml"
    "05-payment-service.yaml"
    "06-shipping-service.yaml"
    "07-proxy-client.yaml"
    "09-cloud-config.yaml"
    "10-zipkin.yaml"
    "11-favourite-service.yaml"
    "12-api-gateway.yaml"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "Updating $file..."
        # Update requests
        sed -i 's/memory: "64Mi"/memory: "512Mi"/g' "$file"
        sed -i 's/memory: "256Mi"/memory: "512Mi"/g' "$file"
        sed -i 's/cpu: "25m"/cpu: "100m"/g' "$file"
        
        # Update limits
        sed -i 's/memory: "256Mi"/memory: "1Gi"/g' "$file"
        sed -i 's/memory: "512Mi"/memory: "1Gi"/g' "$file"
        sed -i 's/cpu: "250m"/cpu: "1000m"/g' "$file"
        sed -i 's/cpu: "500m"/cpu: "1000m"/g' "$file"
    fi
done
