pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'product-service'
        SERVICE_DIR = 'product-service'
        SERVICE_PORT = '8500'
        VERSION = "${env.BUILD_NUMBER}"
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
        MINIKUBE_NAMESPACE = 'ecommerce-dev'
        PERFORMANCE_RESULTS_DIR = 'performance-results'
        SONAR_HOST_URL = "${SONAR_HOST_URL ?: 'http://localhost:9000'}"
        SONAR_TOKEN = credentials('sonar-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app.git'
            }
        }
        
        stage('Build Application') {
            steps {
                dir(SERVICE_DIR) {
                    sh '''
                        mvn clean package -DskipTests
                    '''
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir(SERVICE_DIR) {
                    sh '''
                        mvn test -Dtest=**/*UnitTest
                    '''
                }
            }
            post {
                always {
                    junit "${SERVICE_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                dir(SERVICE_DIR) {
                    sh '''
                        mvn test -Dtest=**/*IntegrationTest
                    '''
                }
            }
            post {
                always {
                    junit "${SERVICE_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        stage('E2E Tests') {
            steps {
                dir(SERVICE_DIR) {
                    sh '''
                        mvn test -Dtest=**/*E2ETest
                    '''
                }
            }
            post {
                always {
                    junit "${SERVICE_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        stage('Code Quality Analysis - SonarQube') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=${SERVICE_NAME} \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.login=${SONAR_TOKEN} \
                                    -Dsonar.sources=src/main/java \
                                    -Dsonar.tests=src/test/java \
                                    -Dsonar.java.binaries=target/classes \
                                    -Dsonar.junit.reportPaths=target/surefire-reports
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        sh '''
                            mvn org.owasp:dependency-check-maven:check \
                                -DfailOnError=false
                        '''
                        
                        sh '''
                            docker build -t ${SERVICE_NAME}:${VERSION}-scan .
                            
                            docker run --rm \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                -v ${WORKSPACE}/${SERVICE_DIR}/trivy-results:/tmp/trivy-results \
                                aquasec/trivy image \
                                --severity HIGH,CRITICAL \
                                --exit-code 0 \
                                --format json \
                                --output /tmp/trivy-results/trivy-report.json \
                                ${SERVICE_NAME}:${VERSION}-scan
                            
                            docker run --rm \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy image \
                                --severity HIGH,CRITICAL \
                                --exit-code 0 \
                                ${SERVICE_NAME}:${VERSION}-scan
                        '''
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${SERVICE_DIR}/target/dependency-check-report.html", allowEmptyArchive: true
                    archiveArtifacts artifacts: "${SERVICE_DIR}/trivy-results/**/*", allowEmptyArchive: true
                }
            }
        }
        
        stage('Setup Minikube') {
            steps {
                script {
                    sh '''
                        # Verificar si minikube está corriendo
                        if ! minikube status > /dev/null 2>&1; then
                            echo "Starting minikube..."
                            minikube start
                        else
                            echo "Minikube is already running"
                        fi
                        
                        # Configurar kubectl para usar minikube
                        minikube update-context
                        
                        # Crear namespace si no existe
                        kubectl create namespace ecommerce-dev --dry-run=client -o yaml | kubectl apply -f -
                    '''
                }
            }
        }
        
        stage('Build and Load Docker Image to Minikube') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        sh '''
                            # Construir imagen Docker
                            docker build -t ${SERVICE_NAME}:${VERSION} .
                            
                            # Cargar imagen en minikube
                            minikube image load ${SERVICE_NAME}:${VERSION}
                            
                            # También cargar como latest
                            docker tag ${SERVICE_NAME}:${VERSION} ${SERVICE_NAME}:dev-latest
                            minikube image load ${SERVICE_NAME}:dev-latest
                            
                            echo "Image ${SERVICE_NAME}:${VERSION} loaded to minikube"
                        '''
                    }
                }
            }
        }
        
        stage('Deploy to Minikube') {
            steps {
                script {
                    sh '''
                        # Configurar kubectl para usar minikube
                        minikube update-context
                        
                        # Verificar si el deployment existe
                        if kubectl get deployment ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} > /dev/null 2>&1; then
                            # Actualizar deployment existente
                            kubectl set image deployment/${SERVICE_NAME} \
                                ${SERVICE_NAME}=${SERVICE_NAME}:${VERSION} \
                                -n ${MINIKUBE_NAMESPACE}
                            kubectl rollout status deployment/${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE}
                        else
                            echo "Deployment ${SERVICE_NAME} does not exist. Please create it first."
                            exit 1
                        fi
                    '''
                }
            }
        }
        
        stage('Smoke Tests') {
            steps {
                script {
                    sh '''
                        # Obtener URL del servicio usando minikube
                        export SERVICE_URL=$(minikube service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} --url 2>/dev/null || echo "")
                        
                        if [ -z "$SERVICE_URL" ]; then
                            # Si minikube service no funciona, usar NodePort
                            export NODE_PORT=$(kubectl get service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
                            export MINIKUBE_IP=$(minikube ip)
                            export SERVICE_URL=http://${MINIKUBE_IP}:${NODE_PORT}
                        fi
                        
                        echo "Service URL: ${SERVICE_URL}"
                        
                        # Health check
                        curl -f ${SERVICE_URL}/product-service/actuator/health || exit 1
                        
                        echo "Service running on port: ${SERVICE_PORT}"
                        
                        echo "Smoke tests passed!"
                    '''
                }
            }
        }
        
        stage('Performance Tests') {
            steps {
                script {
                    sh '''
                        export SERVICE_URL=$(minikube service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} --url 2>/dev/null || echo "")
                        
                        if [ -z "$SERVICE_URL" ]; then
                            export NODE_PORT=$(kubectl get service ${SERVICE_NAME} -n ${MINIKUBE_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
                            export MINIKUBE_IP=$(minikube ip)
                            export SERVICE_URL=http://${MINIKUBE_IP}:${NODE_PORT}
                        fi
                        
                        echo "Running performance tests against: ${SERVICE_URL}"
                        
                        cd tests/performance
                        python3 -m locust \
                            --headless \
                            --users 10 \
                            --spawn-rate 2 \
                            --run-time 60s \
                            --host ${SERVICE_URL} \
                            --html ${WORKSPACE}/${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-performance-report.html \
                            --csv ${WORKSPACE}/${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-performance || true
                        
                        cd ${WORKSPACE}
                        
                        if [ -f analyze-performance.py ]; then
                            python3 analyze-performance.py \
                                --stats-file ${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-performance_stats_stats.csv \
                                --output ${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-performance-analysis.json || true
                        fi
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${PERFORMANCE_RESULTS_DIR}/**/*", allowEmptyArchive: true
                    publishHTML([
                        reportDir: "${PERFORMANCE_RESULTS_DIR}",
                        reportFiles: "${SERVICE_NAME}-performance-report.html",
                        reportName: 'Performance Test Report'
                    ])
                }
            }
        }
    }
    
    post {
        success {
            echo "Dev deployment ${VERSION} completed successfully for ${SERVICE_NAME}!"
        }
        failure {
            echo "Dev deployment ${VERSION} failed for ${SERVICE_NAME}!"
        }
        always {
            cleanWs()
        }
    }
}

