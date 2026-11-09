@Library('ivolve-lab22') _

pipeline {
    agent { label 'lab22-agent' }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build App') {
            steps {
                BuildApp()
            }
        }

        stage('Build Image, Scan and Push') {
            steps {
                BuildImage()
            }
        }

        stage('Update Manifest for Argo CD') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-write-creds',
                    usernameVariable: 'GIT_USERNAME',
                    passwordVariable: 'GIT_TOKEN'
                )]) {
                    sh '''
                        set -eu
                        set +x

                        manifest='k8s/deployment.yaml'
                        image_repo='abdelazizamer/ivolve-app'
                        image_tag="${BUILD_NUMBER}"

                        if ! grep -Eq '^[[:space:]]*image:[[:space:]]*[^[:space:]]*ivolve-app:' "$manifest"; then
                            echo "Could not find the ivolve-app image in $manifest" >&2
                            exit 1
                        fi

                        sed -i -E "s#(^[[:space:]]*image:[[:space:]]*)[^[:space:]]*ivolve-app:[^[:space:]]+#\\1${image_repo}:${image_tag}#" "$manifest"

                        git diff --check -- "$manifest"
                        git add -- "$manifest"

                        if git diff --cached --quiet; then
                            echo "Manifest already uses ${image_repo}:${image_tag}; nothing to push."
                            exit 0
                        fi

                        git config user.name 'Jenkins'
                        git config user.email 'jenkins@users.noreply.github.com'
                        git commit -m "chore: deploy ${image_repo}:${image_tag}"

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

                        git push origin HEAD:main
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Lab 23 GitOps pipeline completed successfully.'
        }
        failure {
            echo 'Lab 23 GitOps pipeline failed. Check the Console Output.'
        }
    }
}
