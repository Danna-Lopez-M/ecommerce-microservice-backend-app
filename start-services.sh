#!/bin/bash

# Startup script for all microservices
set -e

echo "🚀 Starting all microservices..."

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

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    print_status "Waiting for $service_name to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            print_success "$service_name is ready!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts - $service_name not ready yet, waiting 5 seconds..."
        sleep 5
        ((attempt++))
    done
    
    print_error "$service_name failed to start within expected time"
    return 1
}

# Function to start a service
start_service() {
    local service=$1
    local port=$2
    local profile=${3:-dev}
    
    print_status "Starting $service on port $port..."
    
    cd "$service"
    
    # Start service in background
    nohup ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=$port --spring.profiles.active=$profile" > "../logs/${service}.log" 2>&1 &
    
    # Store PID
    echo $! > "../logs/${service}.pid"
    
    cd ..
    
    # Wait for service to be ready
    wait_for_service "$service" "$port"
}

# Function to cleanup on exit
cleanup() {
    print_status "Shutting down all services..."
    
    # Kill all services
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            if kill -0 "$pid" 2>/dev/null; then
                print_status "Stopping service with PID $pid"
                kill "$pid"
            fi
            rm "$pidfile"
        fi
    done
    
    print_success "All services stopped"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Create logs directory
mkdir -p logs

# Start services in order
print_status "Starting infrastructure services..."

# 1. Service Discovery (Eureka)
start_service "service-discovery" "8761"

# 2. Cloud Config
start_service "cloud-config" "9296"

# Wait a bit for config server to be ready
sleep 10

print_status "Starting business services..."

# 3. API Gateway
start_service "api-gateway" "8080"

# 4. Microservices
start_service "user-service" "8700"
start_service "product-service" "8500"
start_service "order-service" "8300"
start_service "payment-service" "8400"
start_service "shipping-service" "8600"
start_service "favourite-service" "8800"

# 5. Proxy Client
start_service "proxy-client" "8900"

print_success "All services started successfully! 🎉"

print_status "Service URLs:"
echo "  - Service Discovery (Eureka): http://localhost:8761"
echo "  - Cloud Config: http://localhost:9296"
echo "  - API Gateway: http://localhost:8080"
echo "  - User Service: http://localhost:8700"
echo "  - Product Service: http://localhost:8500"
echo "  - Order Service: http://localhost:8300"
echo "  - Payment Service: http://localhost:8400"
echo "  - Shipping Service: http://localhost:8600"
echo "  - Favourite Service: http://localhost:8800"
echo "  - Proxy Client: http://localhost:8900"

print_status "Press Ctrl+C to stop all services"

# Keep script running
while true; do
    sleep 10
done
