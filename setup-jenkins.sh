#!/bin/bash

# Script para configurar y ejecutar Jenkins con el pipeline

set -e

echo "🚀 Configurando Jenkins para el pipeline de microservicios..."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Verificar prerrequisitos
print_status "Verificando prerrequisitos..."

# Verificar Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker no está instalado. Por favor instala Docker primero."
    exit 1
fi
print_success "Docker está instalado"

# Verificar Minikube
if ! command -v minikube &> /dev/null; then
    print_warning "Minikube no está instalado. Será necesario instalarlo."
    print_status "Para instalar Minikube:"
    print_status "  curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64"
    print_status "  sudo install minikube-linux-amd64 /usr/local/bin/minikube"
else
    print_success "Minikube está instalado"
fi

# Verificar kubectl
if ! command -v kubectl &> /dev/null; then
    print_warning "kubectl no está instalado. Será necesario instalarlo."
    print_status "Para instalar kubectl:"
    print_status "  curl -LO 'https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl'"
    print_status "  sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl"
else
    print_success "kubectl está instalado"
fi

# Verificar si Docker está corriendo
if ! docker info > /dev/null 2>&1; then
    print_error "Docker no está corriendo. Por favor inicia Docker."
    exit 1
fi
print_success "Docker está corriendo"

# Iniciar Jenkins
print_status "Iniciando Jenkins..."
if [ -f "jenkins-docker-compose.yml" ]; then
    docker-compose -f jenkins-docker-compose.yml up -d
    print_success "Jenkins iniciado"
else
    print_error "No se encontró jenkins-docker-compose.yml"
    exit 1
fi

# Esperar a que Jenkins esté listo
print_status "Esperando a que Jenkins esté listo..."
sleep 10

# Obtener contraseña inicial
print_status "Obteniendo contraseña inicial de Jenkins..."
JENKINS_PASSWORD=$(docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "")

if [ -z "$JENKINS_PASSWORD" ]; then
    print_warning "No se pudo obtener la contraseña automáticamente."
    print_status "Obtén la contraseña manualmente con:"
    print_status "  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
else
    print_success "Contraseña inicial obtenida"
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "  CONTRASEÑA INICIAL DE JENKINS:"
    echo "  $JENKINS_PASSWORD"
    echo "════════════════════════════════════════════════════════════"
    echo ""
fi

# Mostrar información
print_success "Jenkins está corriendo!"
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  INFORMACIÓN DE ACCESO:"
echo "  URL: http://localhost:8080"
echo "  Usuario inicial: admin"
echo "  Contraseña: $JENKINS_PASSWORD"
echo ""
echo "  PRÓXIMOS PASOS:"
echo "  1. Abre http://localhost:8080 en tu navegador"
echo "  2. Ingresa la contraseña inicial"
echo "  3. Instala los plugins recomendados"
echo "  4. Crea un usuario administrador"
echo "  5. Crea un nuevo Pipeline Job"
echo "  6. Configura el job para usar el Jenkinsfile"
echo ""
echo "  Para ver los logs de Jenkins:"
echo "    docker logs -f jenkins"
echo ""
echo "  Para detener Jenkins:"
echo "    docker-compose -f jenkins-docker-compose.yml down"
echo "════════════════════════════════════════════════════════════"
echo ""

