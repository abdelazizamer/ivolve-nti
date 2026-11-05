pipeline {
    agent any

    environment {
        IMAGE_REPO = 'abdelazizamer/ivolve-app'
        KUBECTL = '/var/jenkins_home/bin/kubectl'
        TRIVY = '/var/jenkins_home/bin/trivy'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build App') {
            steps {
                sh '''
                    docker run --rm \
                      -v jenkins_home:/var/jenkins_home \
                      -w "$WORKSPACE" \
                      node:18-alpine \
                      sh -c 'npm install && chown -R 1000:1000 node_modules'
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build --pull -t "$IMAGE_REPO:$BUILD_NUMBER" .'
            }
        }

        stage('Scan Docker Image') {
            steps {
                sh '''
                    "$TRIVY" image \
                      --db-repository ghcr.io/aquasecurity/trivy-db:2 \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --exit-code 0 \
                      "$IMAGE_REPO:$BUILD_NUMBER"
                '''
            }
        }

        stage('Push Image to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        set +x
                        echo "$DOCKER_PASSWORD" | docker login \
                          --username "$DOCKER_USERNAME" \
                          --password-stdin
                        docker push "$IMAGE_REPO:$BUILD_NUMBER"
                    '''
                }
            }
        }

        stage('Delete Local Image') {
            steps {
                sh 'docker rmi "$IMAGE_REPO:$BUILD_NUMBER"'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(
                    credentialsId: 'k8s-kubeconfig',
                    variable: 'KUBECONFIG_FILE'
                )]) {
                    sh '''
                        sed -i "s|image: abdelazizamer/ivolve-app:.*|image: $IMAGE_REPO:$BUILD_NUMBER|" k8s/deployment.yaml

                        export KUBECONFIG="$KUBECONFIG_FILE"
                        "$KUBECTL" apply -f k8s/deployment.yaml
                        "$KUBECTL" apply -f k8s/service.yaml
                        "$KUBECTL" rollout status deployment/ivolveapp \
                          -n ivolve --timeout=300s
                        "$KUBECTL" get deployment,pods,service -n ivolve -o wide
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            echo 'Lab 21 pipeline completed successfully.'
        }
        failure {
            echo 'Lab 21 pipeline failed. Check the Console Output.'
        }
    }
}
