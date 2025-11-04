pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'proxy-client'
        SERVICE_DIR = 'proxy-client'
        SERVICE_PORT = '8900'
        VERSION = "${env.BUILD_NUMBER}"
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'your-registry'}"
        KUBERNETES_NAMESPACE = 'ecommerce-stage'
        KUBERNETES_CONTEXT = "${env.KUBERNETES_CONTEXT ?: 'stage'}"
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
        
        stage('Code Quality Analysis - SonarQube') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=${SERVICE_NAME}-stage \
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
                        // OWASP Dependency Check
                        sh '''
                            mvn org.owasp:dependency-check-maven:check \
                                -DfailOnError=false
                        '''
                        
                        // Build Docker image for scanning
                        sh '''
                            docker build -t ${SERVICE_NAME}:${VERSION} .
                        '''
                        
                        // Trivy Security Scan
                        sh '''
                            docker run --rm \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                -v ${WORKSPACE}/${SERVICE_DIR}/trivy-results:/tmp/trivy-results \
                                aquasec/trivy image \
                                --severity HIGH,CRITICAL \
                                --exit-code 0 \
                                --format json \
                                --output /tmp/trivy-results/trivy-report.json \
                                ${SERVICE_NAME}:${VERSION}
                            
                            docker run --rm \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy image \
                                --severity HIGH,CRITICAL \
                                --exit-code 0 \
                                ${SERVICE_NAME}:${VERSION}
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
        
        stage('Build and Push Docker Image') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        dockerImage = docker.build("${DOCKER_REGISTRY}/${SERVICE_NAME}:${VERSION}")
                        docker.withRegistry('', 'docker-credentials') {
                            dockerImage.push("${VERSION}")
                            dockerImage.push('stage-latest')
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes Stage') {
            steps {
                script {
                    sh """
                        # Configurar contexto de Kubernetes
                        kubectl config use-context ${KUBERNETES_CONTEXT}
                        
                        # Verificar que el deployment existe
                        if ! kubectl get deployment ${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE} > /dev/null 2>&1; then
                            echo "ERROR: Deployment ${SERVICE_NAME} does not exist in namespace ${KUBERNETES_NAMESPACE}"
                            echo "Please create the deployment first"
                            exit 1
                        fi
                        
                        # Actualizar imagen del deployment
                        kubectl set image deployment/${SERVICE_NAME} \
                            ${SERVICE_NAME}=${DOCKER_REGISTRY}/${SERVICE_NAME}:${VERSION} \
                            -n ${KUBERNETES_NAMESPACE}
                        
                        # Esperar rollout
                        kubectl rollout status deployment/${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE} --timeout=5m
                    """
                }
            }
        }
        
        stage('Wait for Deployment Ready') {
            steps {
                script {
                    sh """
                        kubectl wait --for=condition=available --timeout=300s \
                            deployment/${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE}
                        
                        # Esperar adicional para que el servicio esté completamente listo
                        sleep 30
                    """
                }
            }
        }
        
        stage('Get Service URL') {
            steps {
                script {
                    sh """
                        # Obtener URL del servicio desplegado
                        export SERVICE_URL=\$(kubectl get service ${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
                        
                        if [ -z "\$SERVICE_URL" ]; then
                            # Intentar con IP
                            export SERVICE_URL=\$(kubectl get service ${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
                        fi
                        
                        if [ -z "\$SERVICE_URL" ]; then
                            # Usar NodePort o ClusterIP
                            export NODE_PORT=\$(kubectl get service ${SERVICE_NAME} -n ${KUBERNETES_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
                            if [ -n "\$NODE_PORT" ]; then
                                export CLUSTER_IP=\$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type==\"ExternalIP\")].address}' 2>/dev/null || \
                                                   kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type==\"InternalIP\")].address}' 2>/dev/null || echo "localhost")
                                export SERVICE_URL=http://\${CLUSTER_IP}:\${NODE_PORT}
                            else
                                # Usar port-forward como último recurso
                                export SERVICE_URL=http://localhost:${SERVICE_PORT}
                            fi
                        else
                            export SERVICE_URL=http://\${SERVICE_URL}:${SERVICE_PORT}
                        fi
                        
                        echo "Service URL: \${SERVICE_URL}"
                        echo "SERVICE_URL=\${SERVICE_URL}" > service-url.env
                    """
                }
            }
        }
        
        stage('Smoke Tests Against Deployed Application') {
            steps {
                script {
                    sh '''
                        # Cargar URL del servicio
                        source service-url.env
                        
                        echo "Running smoke tests against deployed service: ${SERVICE_URL}"
                        
                        # Health check
                        curl -f ${SERVICE_URL}/app/actuator/health || exit 1
                        
                        # Verificar que el servicio responde
                        curl -f ${SERVICE_URL}/app/actuator/info || echo "Info endpoint not available"
                        
                        echo "Smoke tests passed!"
                    '''
                }
            }
        }
        
        stage('E2E Tests Against Deployed Application') {
            steps {
                dir(SERVICE_DIR) {
                    script {
                        sh '''
                            # Cargar URL del servicio
                            source ${WORKSPACE}/service-url.env
                            
                            echo "Running E2E tests against deployed service: ${SERVICE_URL}"
                            
                            # Ejecutar E2E tests contra la aplicación desplegada
                            mvn test -Dtest=**/*E2ETest \
                                -Dservice.base.url=${SERVICE_URL} \
                                -Dservice.context.path=/app || true
                        '''
                    }
                }
            }
            post {
                always {
                    junit "${SERVICE_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        stage('Performance Tests Against Deployed Application') {
            steps {
                script {
                    sh '''
                        # Cargar URL del servicio
                        source service-url.env
                        
                        echo "Running performance tests against deployed service: ${SERVICE_URL}"
                        
                        # Ejecutar tests de performance con Locust
                        cd tests/performance
                        python3 -m locust \
                            --headless \
                            --users 20 \
                            --spawn-rate 4 \
                            --run-time 120s \
                            --host ${SERVICE_URL} \
                            --html ${WORKSPACE}/${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-stage-performance-report.html \
                            --csv ${WORKSPACE}/${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-stage-performance || true
                        
                        cd ${WORKSPACE}
                        
                        # Analizar resultados con el script de análisis
                        if [ -f analyze-performance.py ]; then
                            python3 analyze-performance.py \
                                --stats-file ${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-stage-performance_stats_stats.csv \
                                --output ${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-stage-performance-analysis.json || true
                        fi
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${PERFORMANCE_RESULTS_DIR}/**/*", allowEmptyArchive: true
                    publishHTML([
                        reportDir: "${PERFORMANCE_RESULTS_DIR}",
                        reportFiles: "${SERVICE_NAME}-stage-performance-report.html",
                        reportName: 'Stage Performance Test Report'
                    ])
                }
            }
        }
        
        stage('Validate Performance Metrics') {
            steps {
                script {
                    sh '''
                        # Validar métricas de performance
                        if [ -f ${PERFORMANCE_RESULTS_DIR}/${SERVICE_NAME}-stage-performance_stats_stats.csv ]; then
                            python3 - << EOF
import csv
import sys
import os

thresholds = {
    'max_avg_response_time': 2000,  # ms
    'max_95_percentile': 3000,      # ms
    'max_error_rate': 5.0,          # percentage
    'min_rps': 10                   # requests per second
}

stats_file = os.environ.get('PERFORMANCE_RESULTS_DIR', 'performance-results') + '/' + \
             os.environ.get('SERVICE_NAME', 'proxy-client') + '-stage-performance_stats_stats.csv'

try:
    with open(stats_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get('Type') == 'Aggregated':
                avg_response = float(row.get('Average Response Time', 0))
                failure_count = int(row.get('Failure Count', 0))
                total_requests = int(row.get('Request Count', 1))
                rps = float(row.get('Requests/s', 0))
                
                error_rate = (failure_count / total_requests * 100) if total_requests > 0 else 0
                
                print(f"Performance Metrics:")
                print(f"  Average Response Time: {avg_response}ms")
                print(f"  Error Rate: {error_rate:.2f}%")
                print(f"  Requests per Second: {rps}")
                
                # Validar thresholds
                if avg_response > thresholds['max_avg_response_time']:
                    print(f"❌ Average response time ({avg_response}ms) exceeds threshold ({thresholds['max_avg_response_time']}ms)")
                    sys.exit(1)
                if error_rate > thresholds['max_error_rate']:
                    print(f"❌ Error rate ({error_rate:.2f}%) exceeds threshold ({thresholds['max_error_rate']}%)")
                    sys.exit(1)
                if rps < thresholds['min_rps']:
                    print(f"❌ RPS ({rps}) below threshold ({thresholds['min_rps']})")
                    sys.exit(1)
                
                print("✅ Performance tests passed all thresholds!")
                break
except FileNotFoundError:
    print(f"Warning: Performance stats file not found: {stats_file}")
    sys.exit(0)
except Exception as e:
    print(f"Error analyzing performance results: {e}")
    sys.exit(1)
EOF
                        else
                            echo "Warning: Performance stats file not found, skipping validation"
                        fi
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo "Stage deployment ${VERSION} completed successfully for ${SERVICE_NAME}!"
        }
        failure {
            echo "Stage deployment ${VERSION} failed for ${SERVICE_NAME}!"
        }
        always {
            archiveArtifacts artifacts: "service-url.env", allowEmptyArchive: true
            cleanWs()
        }
    }
}

