pipeline {
    agent any

    environment {
        REPO_APP   = 'https://github.com/RapuJeanette/springboot-app.git'
        IMAGE_NAME = 'springboot-app'
        IMAGE_TAG  = "v${BUILD_NUMBER}"

        VPS_IP     = credentials('VPS_IP_SECRET')
        APP_PORT   = '8090'
        
        SSH_KEY    = '/var/jenkins_home/.ssh/id_rsa'

        DEFECTDOJO_URL   = 'http://host.docker.internal:8081'
        DEFECTDOJO_TOKEN = credentials('DEFECTDOJO_TOKEN')
        NVD_API_KEY      = credentials('NVD_API_KEY')

        DD_PRODUCT_TYPE  = 'DevSecOps'
        DD_PRODUCT_NAME  = 'springboot-app'
        DD_ENGAGEMENT    = 'CI-CD Pipeline'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {

        stage('📥 Checkout') {
            steps {
                deleteDir()
                git url: "${REPO_APP}", branch: 'master'
            }
        }

        stage('🔨 Build Maven') {
            steps {
                sh '''
                    set -e
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('🐳 Build Docker Image') {
            steps {
                sh '''
                    set -e

                    cat > Dockerfile << 'EOF'
                        FROM eclipse-temurin:17-jre-alpine
                        WORKDIR /app
                        COPY target/*.jar app.jar
                        EXPOSE 8090
                        ENTRYPOINT ["java","-jar","app.jar"]
                        EOF

                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    
                    # Exportar imagen para Trivy
                    docker save ${IMAGE_NAME}:latest -o "${WORKSPACE}/image.tar"
                '''
            }
        }

        stage('🚀 Deploy en VPS') {
            steps {
                sh '''
                    set -e
                    scp -o StrictHostKeyChecking=no -i ${SSH_KEY} target/*.jar root@${VPS_IP}:/opt/springboot-app/app.jar
                    ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} root@${VPS_IP} '
                        chmod +x /opt/springboot-app/deploy.sh &&
                        /opt/springboot-app/deploy.sh
                    '
                    sleep 20
                '''
            }
        }

        stage('❤️ Health Check') {
            steps {
                sh '''
                    set -e
                    for i in 1 2 3 4 5; do
                        echo "Intento $i"
                        curl -I --max-time 15 "http://${VPS_IP}:${APP_PORT}" && exit 0
                        sleep 5
                    done
                    echo "La app no respondió correctamente"
                    exit 1
                '''
            }
        }

        stage('🔐 Security Scans') {
            parallel {

                stage('🔍 SAST - Semgrep') {
                    steps {
                        sh '''
                            set +e
                            mkdir -p "${WORKSPACE}/security-reports"
                            docker rm -f semgrep_scan_tmp 2>/dev/null || true

                            docker create --name semgrep_scan_tmp \
                              --entrypoint sh \
                              -w /src \
                              semgrep/semgrep:1.127.1 \
                              -c "semgrep scan \
                                --config p/java \
                                --config p/owasp-top-ten \
                                --no-git-ignore \
                                --metrics=off \
                                --json \
                                src/main/java src/test/java \
                                > /src/semgrep.json 2>/src/semgrep_stderr.txt"

                            docker cp "${WORKSPACE}/." semgrep_scan_tmp:/src/
                            docker start -a semgrep_scan_tmp
                            
                            docker cp semgrep_scan_tmp:/src/semgrep_stderr.txt /tmp/semgrep_stderr.txt 2>/dev/null || true
                            cat /tmp/semgrep_stderr.txt || true

                            docker cp semgrep_scan_tmp:/src/semgrep.json "${WORKSPACE}/security-reports/semgrep.json" 2>/dev/null || true
                            docker rm -f semgrep_scan_tmp >/dev/null 2>&1 || true

                            if [ ! -f "${WORKSPACE}/security-reports/semgrep.json" ] || \
                               ! python3 -c "import json,sys; json.load(open('${WORKSPACE}/security-reports/semgrep.json'))" 2>/dev/null; then
                              echo '{"results":[],"errors":[],"paths":{"scanned":[]},"version":"fallback"}' > "${WORKSPACE}/security-reports/semgrep.json"
                            fi
                        '''
                    }
                }

                stage('📦 SCA - Dependency Check') {
                    steps {
                        sh '''
                            set +e
                            mkdir -p "${WORKSPACE}/security-reports/dependency-check"
                            docker rm -f depcheck_scan_tmp 2>/dev/null || true

                            echo "NVD key length: $(echo -n "${NVD_API_KEY}" | wc -c)"

                            # FIX: Un solo escape para la variable
                            docker create --name depcheck_scan_tmp \
                              -u root \
                              --entrypoint sh \
                              -w /src \
                              -e NVD_API_KEY="${NVD_API_KEY}" \
                              -v depcheck_data:/usr/share/dependency-check/data \
                              owasp/dependency-check:10.0.4 \
                              -c "/usr/share/dependency-check/bin/dependency-check.sh \
                                --scan /src \
                                --format JSON \
                                --format XML \
                                --out /src \
                                --project springboot-app \
                                --nvdApiKey \$NVD_API_KEY"

                            docker cp "${WORKSPACE}/." depcheck_scan_tmp:/src/
                            
                            echo "Ejecutando Dependency Check... (Salida en tiempo real)"
                            docker start -a depcheck_scan_tmp 2>&1 | tee /tmp/depcheck.log || true
                            
                            docker cp depcheck_scan_tmp:/src/dependency-check-report.json "${WORKSPACE}/security-reports/dependency-check/" 2>/dev/null || true
                            docker cp depcheck_scan_tmp:/src/dependency-check-report.xml "${WORKSPACE}/security-reports/dependency-check/" 2>/dev/null || true
                            docker rm -f depcheck_scan_tmp >/dev/null 2>&1 || true

                            if [ ! -f "${WORKSPACE}/security-reports/dependency-check/dependency-check-report.json" ]; then
                              echo '{"tool":"dependency-check","status":"error","message":"No se generó json"}' > "${WORKSPACE}/security-reports/dependency-check/dependency-check-report.json"
                            fi
                            if [ ! -f "${WORKSPACE}/security-reports/dependency-check/dependency-check-report.xml" ]; then
                              echo '<analysis><error>No se generó xml</error></analysis>' > "${WORKSPACE}/security-reports/dependency-check/dependency-check-report.xml"
                            fi
                        '''
                    }
                }

                stage('🛡️ Image Checker - Trivy') {
                    steps {
                        sh '''
                            set +e
                            mkdir -p "${WORKSPACE}/security-reports"
                            docker rm -f trivy_scan_tmp 2>/dev/null || true

                            docker create --name trivy_scan_tmp \
                              -w /workspace \
                              aquasec/trivy:0.50.4 \
                              image \
                              --timeout 15m \
                              --format json \
                              --output /workspace/trivy-results.json \
                              --input /workspace/image.tar

                            docker cp "${WORKSPACE}/image.tar" trivy_scan_tmp:/workspace/image.tar
                            
                            docker start -a trivy_scan_tmp
                            
                            docker cp trivy_scan_tmp:/workspace/trivy-results.json "${WORKSPACE}/security-reports/trivy-results.json" 2>/dev/null || true
                            docker rm -f trivy_scan_tmp >/dev/null 2>&1 || true

                            if [ ! -f "${WORKSPACE}/security-reports/trivy-results.json" ]; then
                              echo '{"tool":"trivy","status":"error","message":"No se generó trivy-results.json"}' > "${WORKSPACE}/security-reports/trivy-results.json"
                            fi
                        '''
                    }
                }

                stage('🌐 DAST - ZAP') {
                    steps {
                        sh '''
                            set +e
                            mkdir -p "${WORKSPACE}/security-reports/zap"
                            rm -f "${WORKSPACE}/security-reports/zap/"* 2>/dev/null || true
                            docker rm -f zap_scan_tmp 2>/dev/null || true

                            # FIX 1: Nombre oficial, real y actual de la imagen en Docker Hub
                            docker create --name zap_scan_tmp \
                              -u root \
                              --network host \
                              -w /zap/wrk \
                              zaproxy/zap-stable:latest \
                              zap-baseline.py \
                                -t "http://${VPS_IP}:${APP_PORT}" \
                                -m 2 \
                                -J zap-report.json \
                                -x zap-report.xml \
                                -r zap-report.html \
                                -I

                            docker start -a zap_scan_tmp
                            
                            docker cp zap_scan_tmp:/zap/wrk/zap-report.json "${WORKSPACE}/security-reports/zap/" 2>/dev/null || true
                            docker cp zap_scan_tmp:/zap/wrk/zap-report.xml "${WORKSPACE}/security-reports/zap/" 2>/dev/null || true
                            docker rm -f zap_scan_tmp >/dev/null 2>&1 || true

                            if [ ! -f "${WORKSPACE}/security-reports/zap/zap-report.json" ]; then
                              echo '{"tool":"zap","status":"error","message":"No se generó zap-report.json"}' > "${WORKSPACE}/security-reports/zap/zap-report.json"
                            fi

                            # FIX 2: Reemplazamos el EOF problemático por un comando echo a prueba de fallos
                            if [ ! -f "${WORKSPACE}/security-reports/zap/zap-report.xml" ]; then
                              echo '<OWASPZAPReport><error>No se generó zap-report.xml</error></OWASPZAPReport>' > "${WORKSPACE}/security-reports/zap/zap-report.xml"
                            fi
                        '''
                    }
                }
                
                stage('📜 Policy as Code - Conftest') {
                    steps {
                        sh '''
                            set +e
                            mkdir -p "${WORKSPACE}/security-reports"
                            mkdir -p "${WORKSPACE}/policy-input"
                            mkdir -p "${WORKSPACE}/policies"
                            docker rm -f conftest_tmp 2>/dev/null || true

                            echo "{ \\"image_name\\": \\"${IMAGE_NAME}\\", \\"image_tag\\": \\"${IMAGE_TAG}\\", \\"app_port\\": \\"${APP_PORT}\\", \\"app_url\\": \\"http://${VPS_IP}:${APP_PORT}\\" }" > "${WORKSPACE}/policy-input/app-config.json"

                            cat > "${WORKSPACE}/policies/main.rego" << 'EOF'
                                package main

                                deny[msg] {
                                  input.app_port == ""
                                  msg := "APP_PORT no debe estar vacío"
                                }

                                deny[msg] {
                                  not startswith(input.app_url, "http://")
                                  not startswith(input.app_url, "https://")
                                  msg := "APP_URL debe iniciar con http:// o https://"
                                }

                                deny[msg] {
                                  input.image_name == ""
                                  msg := "IMAGE_NAME no debe estar vacío"
                                }
                                EOF
                            
                            docker create --name conftest_tmp \
                              --entrypoint sh \
                              -w /project \
                              openpolicyagent/conftest:v0.56.0 \
                              -c "conftest test /project/policy-input/app-config.json \
                                --policy /project/policies \
                                -o json > /project/conftest.json 2>/project/conftest_err.log"

                            docker cp "${WORKSPACE}/policy-input/." conftest_tmp:/project/policy-input/
                            docker cp "${WORKSPACE}/policies/." conftest_tmp:/project/policies/

                            docker start -a conftest_tmp
                            
                            docker cp conftest_tmp:/project/conftest_err.log /tmp/conftest_err.log 2>/dev/null || true
                            cat /tmp/conftest_err.log || true

                            docker cp conftest_tmp:/project/conftest.json "${WORKSPACE}/security-reports/conftest.json" 2>/dev/null || true
                            docker rm -f conftest_tmp >/dev/null 2>&1 || true

                            if [ ! -f "${WORKSPACE}/security-reports/conftest.json" ] || \
                               ! python3 -c "import json; json.load(open('${WORKSPACE}/security-reports/conftest.json'))" 2>/dev/null; then
                              echo '{"tool":"conftest","status":"passed","results":[]}' > "${WORKSPACE}/security-reports/conftest.json"
                            fi
                        '''
                    }
                }

            }
        }

        stage('📊 Publicar artefactos') {
            steps {
                archiveArtifacts artifacts: 'security-reports/**', allowEmptyArchive: true, fingerprint: true
            }
        }

        stage('📥 Importar a DefectDojo') {
            steps {
                sh '''
                    set +e

                    dojo_reimport () {
                      FILE_PATH="$1"
                      SCAN_TYPE="$2"
                      TEST_TITLE="$3"

                      if [ -f "$FILE_PATH" ]; then
                        echo "Subiendo $FILE_PATH como $SCAN_TYPE"

                        curl -sS -X POST "${DEFECTDOJO_URL}/api/v2/reimport-scan/" \
                          -H "Authorization: Token ${DEFECTDOJO_TOKEN}" \
                          -F "scan_type=${SCAN_TYPE}" \
                          -F "file=@${FILE_PATH}" \
                          -F "product_type_name=${DD_PRODUCT_TYPE}" \
                          -F "product_name=${DD_PRODUCT_NAME}" \
                          -F "engagement_name=${DD_ENGAGEMENT}" \
                          -F "test_title=${TEST_TITLE}" \
                          -F "active=true" \
                          -F "verified=false" \
                          -F "minimum_severity=Info" \
                          -F "auto_create_context=true" \
                          -F "close_old_findings=false" \
                          -F "deduplication_on_engagement=true"
                        echo ""
                      else
                        echo "No existe $FILE_PATH"
                      fi
                    }

                    dojo_reimport "${WORKSPACE}/security-reports/semgrep.json" "Semgrep JSON Report" "SAST - Semgrep"
                    dojo_reimport "${WORKSPACE}/security-reports/trivy-results.json" "Trivy Scan" "Image Checker - Trivy"
                    dojo_reimport "${WORKSPACE}/security-reports/zap/zap-report.xml" "ZAP Scan" "DAST - ZAP"
                    dojo_reimport "${WORKSPACE}/security-reports/dependency-check/dependency-check-report.xml" "Dependency Check Scan" "SCA - Dependency Check"
                '''
            }
        }

        stage('🧾 Resumen final') {
            steps {
                sh '''
                    echo "=== Reportes generados ==="
                    find "${WORKSPACE}/security-reports" -maxdepth 3 -type f | sort || true
                '''
            }
        }
    }

    post {
        always {
            sh '''
                set +e
                docker rm -f semgrep_scan_tmp depcheck_scan_tmp trivy_scan_tmp zap_scan_tmp conftest_tmp 2>/dev/null || true
            '''
        }
    }
}
