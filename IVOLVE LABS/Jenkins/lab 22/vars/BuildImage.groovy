def call(Map config = [:]) {
    def imageRepo = config.imageRepo ?: 'abdelazizamer/ivolve-app'
    def dockerCredentialsId = config.dockerCredentialsId ?: 'docker'
    def imageTag = env.BUILD_NUMBER

    withEnv(["IMAGE_REPO=${imageRepo}", "IMAGE_TAG=${imageTag}"]) {
        try {
            sh '''
                docker build --pull -t "$IMAGE_REPO:$IMAGE_TAG" .
                trivy image \
                    --timeout 15m \
                    --db-repository ghcr.io/aquasecurity/trivy-db:2 \
                    --scanners vuln \
                    --severity HIGH,CRITICAL \
                    --exit-code 0 \
                    "$IMAGE_REPO:$IMAGE_TAG"
            '''

            withCredentials([usernamePassword(
                credentialsId: dockerCredentialsId,
                usernameVariable: 'DOCKER_USERNAME',
                passwordVariable: 'DOCKER_PASSWORD'
            )]) {
                sh '''
                    set +x
                    echo "$DOCKER_PASSWORD" | docker login \
                        --username "$DOCKER_USERNAME" --password-stdin
                    docker push "$IMAGE_REPO:$IMAGE_TAG"
                '''
            }
        } finally {
            sh 'docker logout || true'
            sh 'docker rmi "$IMAGE_REPO:$IMAGE_TAG" || true'
        }
    }
}
