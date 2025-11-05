pipeline {
    agent any
    
    environment {
        PROJECT_VERSION = '0.1.0'
        DOCKER_REGISTRY = 'localhost:5000'
        KUBERNETES_NAMESPACE = 'ecommerce-dev'
        IMAGE_PREFIX = 'selimhorri'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    echo '🚀 Configuring environment...'
                    def hasMinikube = sh(
                        script: 'command -v minikube > /dev/null 2>&1',
                        returnStatus: true
                    ) == 0
                    
                    if (hasMinikube) {
                        echo '✅ Minikube detectado, usando Minikube...'
                        sh '''
                            # Verificar si Minikube está corriendo
                            if ! minikube status > /dev/null 2>&1; then
                                echo "Iniciando Minikube..."
                                minikube start
                            fi
                            
                            # Configurar Docker para usar el Docker de Minikube
                            eval $(minikube docker-env)
                            
                            # Verificar que Minikube está corriendo
                            minikube status
                        '''
                        env.USE_MINIKUBE = 'true'
                    } else {
                        echo '⚠️  Minikube no detectado, usando Docker directamente...'
                        env.USE_MINIKUBE = 'false'
                    }
                    
                    // Iniciar registry local de Docker (siempre necesario)
                    sh '''
                        # Iniciar el registry local de Docker si no está corriendo
                        if ! docker ps | grep -q "registry:2"; then
                            echo "Iniciando registry local de Docker..."
                            # Verificar si el contenedor existe pero está detenido
                            if docker ps -a | grep -q "registry"; then
                                docker start registry || true
                            else
                                docker run -d -p 5000:5000 --restart=always --name registry registry:2 || true
                            fi
                        fi
                        
                        # Esperar a que el registry esté listo
                        sleep 3
                        echo "Registry local disponible en localhost:5000"
                    '''
                }
            }
        }
        
        stage('Build Maven Project') {
            steps {
                script {
                    echo '🔨 Building Maven project...'
                    sh '''
                        # Construir todos los microservicios
                        ./mvnw clean package -DskipTests
                    '''
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    echo '🐳 Building Docker images...'
                    
                    // Servicios que se construyen desde la raíz del proyecto
                    def rootBuildServices = ['api-gateway', 'cloud-config', 'favourite-service']
                    
                    // Todos los servicios
                    def allServices = [
                        'service-discovery': 8761,
                        'cloud-config': 9296,
                        'api-gateway': 8080,
                        'user-service': 8700,
                        'product-service': 8500,
                        'order-service': 8300,
                        'payment-service': 8400,
                        'shipping-service': 8600,
                        'favourite-service': 8800,
                        'proxy-client': 8900
                    ]
                    
                    allServices.each { serviceName, port ->
                        echo "Building Docker image for ${serviceName}..."
                        if (rootBuildServices.contains(serviceName)) {
                            // Construir desde la raíz del proyecto
                            sh """
                                # Configurar Docker para usar Minikube si está disponible
                                if [ "${USE_MINIKUBE}" == "true" ]; then
                                    eval \$(minikube docker-env)
                                fi
                                
                                # Construir la imagen Docker desde la raíz
                                docker build -f ${serviceName}/Dockerfile -t ${IMAGE_PREFIX}/${serviceName}-ecommerce-boot:${PROJECT_VERSION} .
                                
                                # Etiquetar para el registry local
                                docker tag ${IMAGE_PREFIX}/${serviceName}-ecommerce-boot:${PROJECT_VERSION} ${DOCKER_REGISTRY}/${serviceName}:${PROJECT_VERSION}
                                
                                # Subir la imagen al registry local
                                docker push ${DOCKER_REGISTRY}/${serviceName}:${PROJECT_VERSION}
                            """
                        } else {
                            // Construir desde el directorio del servicio
                            sh """
                                # Configurar Docker para usar Minikube si está disponible
                                if [ "${USE_MINIKUBE}" == "true" ]; then
                                    eval \$(minikube docker-env)
                                fi
                                
                                # Construir la imagen Docker desde el directorio del servicio
                                docker build -f ${serviceName}/Dockerfile -t ${IMAGE_PREFIX}/${serviceName}-ecommerce-boot:${PROJECT_VERSION} ${serviceName}/
                                
                                # Etiquetar para el registry local
                                docker tag ${IMAGE_PREFIX}/${serviceName}-ecommerce-boot:${PROJECT_VERSION} ${DOCKER_REGISTRY}/${serviceName}:${PROJECT_VERSION}
                                
                                # Subir la imagen al registry local
                                docker push ${DOCKER_REGISTRY}/${serviceName}:${PROJECT_VERSION}
                            """
                        }
                    }
                }
            }
        }
        
        stage('Create Kubernetes Namespace') {
            steps {
                script {
                    echo 'Creating Kubernetes namespace...'
                    def hasKubectl = sh(
                        script: 'command -v kubectl > /dev/null 2>&1',
                        returnStatus: true
                    ) == 0
                    
                    if (hasKubectl) {
                        sh """
                            # Configurar kubectl para usar Minikube si está disponible
                            if [ "${USE_MINIKUBE}" == "true" ]; then
                                eval \$(minikube docker-env) || true
                                minikube update-context || true
                            fi
                            
                            # Crear namespace si no existe
                            kubectl create namespace ${KUBERNETES_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        """
                    } else {
                        echo '⚠️  kubectl no está disponible. El despliegue se saltará.'
                        env.SKIP_DEPLOYMENT = 'true'
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            when {
                expression { env.SKIP_DEPLOYMENT != 'true' }
            }
            steps {
                script {
                    echo '🚀 Deploying to Kubernetes...'
                    
                    def hasKubectl = sh(
                        script: 'command -v kubectl > /dev/null 2>&1',
                        returnStatus: true
                    ) == 0
                    
                    if (!hasKubectl) {
                        echo '⚠️  kubectl no está disponible. Saltando despliegue.'
                        return
                    }
                    
                    // Configurar kubectl para usar Minikube si está disponible
                    if (env.USE_MINIKUBE == 'true') {
                        sh '''
                            eval $(minikube docker-env) || true
                            minikube update-context || true
                        '''
                    }
                    
                    // Orden de despliegue: primero servicios core, luego servicios de negocio
                    def deploymentOrder = [
                        'zipkin',                    // Servicio de tracing
                        'service-discovery',         // Service discovery debe ir primero
                        'cloud-config',              // Config server debe ir segundo
                        'api-gateway',               // API Gateway
                        'user-service',
                        'product-service',
                        'order-service',
                        'payment-service',
                        'shipping-service',
                        'favourite-service',
                        'proxy-client'
                    ]
                    
                    deploymentOrder.each { serviceName ->
                        echo "Desplegando ${serviceName}..."
                        sh """
                            # Aplicar el deployment del servicio
                            if [ -f "kubernetes/${serviceName}-deployment.yaml" ]; then
                                kubectl apply -f kubernetes/${serviceName}-deployment.yaml -n ${KUBERNETES_NAMESPACE}
                            else
                                echo "⚠️  No se encontró el deployment para ${serviceName}"
                            fi
                            
                            # Esperar un poco antes del siguiente servicio
                            sleep 2
                        """
                    }
                    
                    // Aplicar cualquier otro archivo de Kubernetes que pueda existir
                    sh """
                        # Aplicar archivos que no estén en el orden específico
                        if [ -d "kubernetes" ]; then
                            kubectl apply -f kubernetes/ -n ${KUBERNETES_NAMESPACE} || true
                        fi
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            when {
                expression { env.SKIP_DEPLOYMENT != 'true' }
            }
            steps {
                script {
                    echo '✅ Verifying deployment...'
                    def hasKubectl = sh(
                        script: 'command -v kubectl > /dev/null 2>&1',
                        returnStatus: true
                    ) == 0
                    
                    if (!hasKubectl) {
                        echo '⚠️  kubectl no está disponible. Saltando verificación.'
                        return
                    }
                    
                    // Configurar kubectl para usar Minikube si está disponible
                    if (env.USE_MINIKUBE == 'true') {
                        sh '''
                            eval $(minikube docker-env) || true
                            minikube update-context || true
                        '''
                    }
                    
                    sh """
                        # Esperar a que los pods estén listos (con reintentos)
                        echo "Esperando a que los pods estén listos..."
                        kubectl wait --for=condition=ready pod --all -n ${KUBERNETES_NAMESPACE} --timeout=600s || true
                        
                        # Mostrar el estado de los pods
                        echo ""
                        echo "════════════════════════════════════════"
                        echo "Estado de los pods:"
                        echo "════════════════════════════════════════"
                        kubectl get pods -n ${KUBERNETES_NAMESPACE}
                        
                        # Mostrar los servicios
                        echo ""
                        echo "════════════════════════════════════════"
                        echo "Servicios desplegados:"
                        echo "════════════════════════════════════════"
                        kubectl get services -n ${KUBERNETES_NAMESPACE}
                        
                        # Verificar pods con problemas
                        echo ""
                        echo "════════════════════════════════════════"
                        echo "Pods con problemas:"
                        echo "════════════════════════════════════════"
                        kubectl get pods -n ${KUBERNETES_NAMESPACE} --field-selector=status.phase!=Running,status.phase!=Succeeded || true
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline completado exitosamente!'
            sh """
                echo "Microservicios desplegados en Minikube:"
                kubectl get pods -n ${KUBERNETES_NAMESPACE}
            """
        }
        failure {
            echo '❌ Pipeline falló!'
            sh """
                echo "Logs de error:"
                kubectl get pods -n ${KUBERNETES_NAMESPACE}
            """
        }
        always {
            echo '🧹 Limpiando...'
        }
    }
}

