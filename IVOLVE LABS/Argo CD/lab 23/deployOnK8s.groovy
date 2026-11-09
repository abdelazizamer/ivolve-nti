def call(Map config = [:]) {
    def imageRepo = config.imageRepo ?: 'abdelazizamer/ivolve-app'
    def githubCredentialsId = config.githubCredentialsId ?: 'github-write-creds'
    def targetBranch = config.branch ?: 'main'

    withEnv([
        "IMAGE_REPO=${imageRepo}",
        "IMAGE_TAG=${env.BUILD_NUMBER}",
        "TARGET_BRANCH=${targetBranch}"
    ]) {
        withCredentials([usernamePassword(
            credentialsId: githubCredentialsId,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_TOKEN'
        )]) {
            sh '''
                set -eu
                set +x

                manifest='k8s/deployment.yaml'
                if ! grep -Eq '^[[:space:]]*image:[[:space:]]*[^[:space:]]*ivolve-app:' "$manifest"; then
                    echo "Could not find the ivolve-app image in $manifest" >&2
                    exit 1
                fi

                sed -i -E "s#(^[[:space:]]*image:[[:space:]]*)[^[:space:]]*ivolve-app:[^[:space:]]+#\\1${IMAGE_REPO}:${IMAGE_TAG}#" "$manifest"
                git diff --check -- "$manifest"
                git add -- "$manifest"

                if git diff --cached --quiet; then
                    echo "Manifest already references ${IMAGE_REPO}:${IMAGE_TAG}; nothing to push."
                    exit 0
                fi

                git config user.name 'Jenkins'
                git config user.email 'jenkins@users.noreply.github.com'
                git commit -m "chore: deploy ${IMAGE_REPO}:${IMAGE_TAG}"

                askpass="$(mktemp)"
                trap 'rm -f "$askpass"' EXIT
                cat > "$askpass" <<'ASKPASS_EOF'
#!/bin/sh
case "$1" in
  *Username*) printf '%s\\n' "$GIT_USERNAME" ;;
  *) printf '%s\\n' "$GIT_TOKEN" ;;
esac
ASKPASS_EOF
                chmod 700 "$askpass"
                export GIT_ASKPASS="$askpass"
                export GIT_TERMINAL_PROMPT=0
                git push origin "HEAD:${TARGET_BRANCH}"
            '''
        }
    }
}
