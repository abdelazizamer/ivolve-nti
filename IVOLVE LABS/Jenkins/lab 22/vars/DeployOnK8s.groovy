def call(Map config = [:]) {
    def imageRepo = config.imageRepo ?: 'abdelazizamer/ivolve-app'
    def kubeconfigCredentialsId = config.kubeconfigCredentialsId ?: 'k8s-kubeconfig'
    def namespace = config.namespace ?: 'ivolve'

    withEnv([
        "IMAGE_REPO=${imageRepo}",
        "IMAGE_TAG=${env.BUILD_NUMBER}",
        "K8S_NAMESPACE=${namespace}"
    ]) {
        withCredentials([file(
            credentialsId: kubeconfigCredentialsId,
            variable: 'KUBECONFIG_FILE'
        )]) {
            sh '''
                set -e

                # Update only the application image; preserve the MySQL init container image.
                sed -i "s|image: abdelazizamer/ivolve-app:.*|image: ${IMAGE_REPO}:${IMAGE_TAG}|" k8s/deployment.yaml

                export KUBECONFIG="$KUBECONFIG_FILE"
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml

                kubectl rollout status deployment/ivolveapp \
                    -n "$K8S_NAMESPACE" --timeout=300s

                kubectl get deployment,pods,service \
                    -n "$K8S_NAMESPACE" -o wide
            '''
        }
    }
}
