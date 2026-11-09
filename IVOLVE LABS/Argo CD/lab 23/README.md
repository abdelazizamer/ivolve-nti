Lab 23 — GitOps with Jenkins and Argo CD

## Objective

Automate the application delivery workflow. Jenkins tests the Node.js application, builds and scans a Docker image, pushes the image to Docker Hub, and commits the new image tag to the Kubernetes manifest repository. Argo CD watches that Git repository and synchronizes the manifest to Kubernetes.

## Architecture

- **Jenkins** runs the pipeline on the `lab22-agent` agent.
- **Shared Library:** `ivolve-lab22` provides the `BuildApp()` and `BuildImage()` steps.
- **Git repository:** `https://github.com/abdelazizamer/lab-23.git` contains the application, `Jenkinsfile`, and Kubernetes manifests.
- **Docker Hub image:** `abdelazizamer/ivolve-app:<BUILD_NUMBER>`.
- **Argo CD application:** `ivolve`, tracking branch `main` and directory `k8s` in the `lab-23` repository.
- **Kubernetes destination:** the in-cluster cluster, namespace `ivolve`.

## Prerequisites

1. A Kubernetes cluster is available, with the `ivolve` namespace and the application's required database resources configured.
2. Jenkins is running and the `lab22-agent` agent is online with Node.js/npm, Docker, and Trivy available.
3. Jenkins credentials are configured:
   - `docker` for Docker Hub image publishing.
   - `github-write-creds` for pushing the updated manifest to GitHub.
4. Argo CD is installed and its application `ivolve` is configured to watch `https://github.com/abdelazizamer/lab-23.git`, branch `main`, path `k8s`, namespace `ivolve`.
5. Argo CD automated sync is enabled. Self-heal is enabled and pruning is disabled for this setup.

> Store credentials in Jenkins Credentials. Never add passwords, access tokens, kubeconfig contents, or agent secrets to this repository or to screenshots.

## Jenkins pipeline setup

Create or configure the Jenkins Pipeline job `lab23-gitops`:

1. Set the definition to **Pipeline script from SCM**.
2. Choose **Git** and set the repository URL to `https://github.com/abdelazizamer/lab-23.git`.
3. Set the branch specifier to `*/main` and the script path to `Jenkinsfile`.
4. Ensure the global shared library `ivolve-lab22` is configured and available to the job. Its SCM points to the Lab 22 repository and its library path points to the Lab 22 library directory containing `vars/`.
5. Save the job configuration.

## Pipeline stages

The `Jenkinsfile` runs these stages on `lab22-agent`:

1. **Checkout:** Check out the `lab-23` repository.
2. **Build App:** Call `BuildApp()` to install dependencies and run the application tests/checks.
3. **Build Image, Scan and Push:** Call `BuildImage()` to build `abdelazizamer/ivolve-app:<BUILD_NUMBER>`, scan it with Trivy, push it to Docker Hub, and remove the local image.
4. **Update Manifest for Argo CD:** Use the Jenkins GitHub credential to change the image in `k8s/deployment.yaml` to the current build tag, commit the manifest change, and push it to `main`. The token is injected through Jenkins Credentials and is not printed in the build log.

Jenkins updates Git instead of applying the application manifest directly with `kubectl`. Argo CD performs the deployment after it detects the new Git commit.

## Run and verify

1. In Jenkins, open `lab23-gitops` and click **Build Now**.
2. Check **Console Output**. Confirm that the application checks pass, Trivy completes, the Docker image push succeeds, and the manifest commit is pushed to GitHub.
3. In GitHub, verify that `k8s/deployment.yaml` references the new build tag.
4. In Argo CD, open **Applications → ivolve**. Confirm the application reports **Healthy** and **Synced**, with automated sync enabled.
5. On the Kubernetes control-plane, verify the application and image:

   ```bash
   kubectl get application ivolve -n argocd
   kubectl get deployment ivolveapp -n ivolve -o wide
   kubectl get pods -n ivolve
   kubectl get deployment ivolveapp -n ivolve -o=jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
   ```

## Verified result

- Jenkins build **#17** completed successfully.
- Docker image `abdelazizamer/ivolve-app:17` was pushed.
- Jenkins pushed manifest commit `605c1a9` to the `main` branch.
- Argo CD reported the `ivolve` application as **Healthy** and **Synced** to commit `605c1a9`.
- Kubernetes used image `abdelazizamer/ivolve-app:17`; the Deployment had **2/2** replicas available and the application Pods were Running.

## Submission evidence

1. Jenkins build stages and successful Console Output, with secrets hidden.
2. The GitHub commit that updates `k8s/deployment.yaml` to the build image tag.
3. Argo CD application details showing **Healthy**, **Synced**, the revision, and automated sync enabled.
4. Kubernetes Deployment and Pods showing the expected image and ready replicas.

<img width="1919" height="584" alt="image" src="https://github.com/user-attachments/assets/836b743e-41fd-4606-809e-ce789a5adcb2" />
<img width="1919" height="926" alt="Screenshot 2026-11-09 212611" src="https://github.com/user-attachments/assets/27f17e8e-4597-41fc-8f64-f28c4fae20d9" />
