#!/bin/bash

# Build script for all microservices
set -e

echo "🚀 Starting build process for all microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

MICROSERVICES=(  
    "api-gateway"
    "cloud-config"  
    "favourite-service"
    "user-service"
    "product-service"
    "order-service"    
    "payment-service"
    "shipping-service"    
    "proxy-client"
    "service-discovery"
)

# Function to build a microservice
build_microservice() {
    local service=$1
    print_status "Building $service..."
    
    cd "$service"
    
    # Clean and compile
    print_status "Cleaning and compiling $service..."
    ./mvnw clean compile -q
    
    # Package
    print_status "Packaging $service..."
    ./mvnw package -DskipTests -q
    
    # Build Docker image
    print_status "Building Docker image for $service..."
    docker build -t "selimhorri/${service}-ecommerce-boot:0.1.0" .
    
    print_success "$service built successfully!"
    cd ..
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_success "Docker is running"
}

# Function to check if Maven is available
check_maven() {
    if ! command -v ./mvnw &> /dev/null; then
        print_error "Maven wrapper not found. Please ensure mvnw files are present."
        exit 1
    fi
    print_success "Maven wrapper found"
}

# Main execution
main() {
    print_status "Starting build process..."
    
    # Check prerequisites
    check_docker
    check_maven
    
    # Build each microservice
    for service in "${MICROSERVICES[@]}"; do
        if [ -d "$service" ]; then
            build_microservice "$service"
        else
            print_warning "Directory $service not found, skipping..."
        fi
    done
    
    print_success "All microservices built successfully! 🎉"
    
    # List built images
    print_status "Built Docker images:"
    docker images | grep "selimhorri.*ecommerce-boot"
    
    print_status "Build process completed!"
}

# Run main function
main "$@"
