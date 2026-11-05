# Lab 21 — Jenkins CI/CD Pipeline for the Node.js + MySQL App

This guide records the Lab 21 setup and the fixes needed in the current environment. Run cluster commands from the Kubernetes control-plane SSH session, and run Docker Desktop / SSH tunnel commands from Windows PowerShell.

## 1. Lab flow

The Jenkins Pipeline checks out the GitHub repository, installs Node.js dependencies, builds a Docker image, scans it with Trivy, pushes it to Docker Hub, updates the Kubernetes Deployment image, applies the manifests, and waits for the rollout.

The build is started manually with **Build Now**. No GitHub webhook is configured.

## 2. Repositories and credentials

- GitHub repository: `https://github.com/abdelazizamer/JENKINS-_LAB21.git`
- Docker Hub repository: `abdelazizamer/ivolve-app` (Public)
- Docker Hub Jenkins credential ID used by the Jenkinsfile: `docker`
- Kubernetes kubeconfig Jenkins credential ID: `k8s-kubeconfig`
- The GitHub repository is checked out without credentials when it is public. If it becomes private, configure a Jenkins GitHub PAT credential and select it in the job's SCM settings.

Never place tokens, passwords, or kubeconfig contents in the repository, Jenkinsfile, or screenshots.

## 3. Verify the Kubernetes prerequisites

From the control-plane SSH session:

```bash
kubectl get nodes -o wide
kubectl get namespace ivolve
kubectl get configmap ivolvecm -n ivolve
kubectl get secret ivolvesecret -n ivolve
kubectl get pvc -n ivolve
kubectl get statefulset,service -n ivolve
```

Do not display Secret data. The database Pod should be `mysql-0` and the app Deployment is `ivolveapp` in namespace `ivolve`.

## 4. Verify Windows Docker and Jenkins tools

Start Docker Desktop. In Windows PowerShell, verify the Jenkins container and tools:

```powershell
docker exec jenkins docker info --format "Docker Engine: {{.ServerVersion}}"
docker exec jenkins /var/jenkins_home/bin/trivy --version
docker exec jenkins /var/jenkins_home/bin/kubectl version --client
```

The Jenkins container must have Docker CLI, Trivy, and kubectl, and must be able to access the host Docker Engine.

## 5. Configure Jenkins credentials

Open **Manage Jenkins → Credentials → System → Global credentials** and verify:

1. `docker` — Username with password; Docker Hub username and access token.
2. `k8s-kubeconfig` — Secret file containing the cluster kubeconfig.

The Jenkinsfile references these exact IDs. Do not create duplicate credentials if these already exist.

## 6. Configure the Pipeline job

The job `lab21-mysql-app` already exists. Open **Configure → Pipeline** and set:

1. **Definition:** `Pipeline script from SCM`
2. **SCM:** `Git`
3. **Repository URL:** `https://github.com/abdelazizamer/JENKINS-_LAB21.git`
4. **Credentials:** `- none -` while the repository is public.
5. **Branch Specifier:** `*/main`
6. **Script Path:** `Jenkinsfile`
7. Leave build triggers unchecked for manual runs.

Save the configuration. The Pipeline stages are:

1. Checkout
2. Build App
3. Build Docker Image
4. Scan Docker Image
5. Push Image to Docker Hub
6. Delete Local Image
7. Deploy to Kubernetes

## 7. Keep the Kubernetes API SSH tunnel open

Jenkins runs in Docker Desktop, while the Kubernetes API is on the EC2 control-plane. The kubeconfig in Jenkins uses `host.docker.internal:16443`, so open a separate Windows PowerShell window and run:

```powershell
ssh -i "$env:USERPROFILE\Documents\Downloads\k8s-lab-key.pem" -o ExitOnForwardFailure=yes -N -L 16443:127.0.0.1:6443 ubuntu@54.91.107.214
```

Leave this PowerShell window running during the Jenkins build. With `-N`, it normally remains open without showing a shell prompt. If Jenkins reports `host.docker.internal:16443: connect: connection refused`, the tunnel is not listening; reopen it before the next build.

## 8. Internal EC2 security-group rules

Both EC2 nodes use the `k8s-lab-sg` security group. In **EC2 → Security Groups → k8s-lab-sg → Inbound rules**, keep the existing SSH rules and allow internal node traffic with the security group itself as source:

| Type | Port | Source |
|---|---:|---|
| Custom TCP | 6443 | `k8s-lab-sg` |
| Custom TCP | 10250 | `k8s-lab-sg` |

Do not expose these ports to `0.0.0.0/0`. Port 6443 is the Kubernetes API; 10250 is the kubelet port used by the control-plane to communicate with the worker.

## 9. Fix the MySQL Service selector if the init container waits forever

In this environment, the MySQL Pod has label `app=mysql`, but the `ivolvedbsvc` Service initially selected `app=ivolvedb`, leaving it with no endpoints. The application init container uses `ivolvedbsvc` as its DB host.

Verify the state:

```bash
kubectl get pod mysql-0 -n ivolve --show-labels
kubectl get endpoints ivolvedbsvc -n ivolve
```

If the MySQL Pod label is `app=mysql` and `ivolvedbsvc` has no endpoints, correct the selector:

```bash
kubectl patch service ivolvedbsvc -n ivolve --type=merge -p '{"spec":{"selector":{"app":"mysql"}}}'
kubectl get endpoints ivolvedbsvc -n ivolve
```

The endpoint should include the MySQL Pod IP on port `3306`. Do not print Secret contents while debugging database access.

## 10. Run and verify the pipeline

1. Make sure Docker Desktop is running and the SSH tunnel is still open.
2. In Jenkins, open `lab21-mysql-app` and select **Build Now**.
3. Watch **Stage View** and open **Console Output** if a stage fails.
4. A successful run ends with `Finished: SUCCESS`.
5. Verify Kubernetes from the control-plane:

```bash
kubectl rollout status deployment/ivolveapp -n ivolve --timeout=300s
kubectl get deployment,pods,service -n ivolve -o wide
kubectl get deployment ivolveapp -n ivolve -o jsonpath='{.spec.template.spec.containers[?(@.name=="ivolve-app")].image}{"\n"}'
```

Confirm the two application replicas and MySQL are Ready/Running, and the deployed image tag matches the successful Jenkins build number. Confirm the same tag appears in Docker Hub under `abdelazizamer/ivolve-app`.

## 11. Trivy database timeout recovery

If the Scan stage fails downloading the Trivy vulnerability database with `context deadline exceeded`, run these from Windows PowerShell:

```powershell
docker exec -u 1000:1000 jenkins /var/jenkins_home/bin/trivy clean --vuln-db
docker exec -u 1000:1000 jenkins /var/jenkins_home/bin/trivy image --db-repository ghcr.io/aquasecurity/trivy-db:2 --download-db-only
```

Wait for `Artifact successfully downloaded`, then start another build. The pipeline's `--exit-code 0` allows reported vulnerabilities without failing the scan stage; it does not mean the image has no vulnerabilities.

## 12. Completion status

Lab 21 completed successfully in Jenkins Build **#5**. All pipeline stages passed, including Trivy scan, Docker Hub push, and deployment to Kubernetes. The pushed image and deployed Deployment use tag `5` (`abdelazizamer/ivolve-app:5`). Kubernetes reported the `ivolveapp` Deployment rollout successful with two application Pods Ready and the MySQL Pod Running



<img width="1919" height="683" alt="Screenshot 2026-11-05 155829" src="https://github.com/user-attachments/assets/83608fae-cedc-404e-bb5a-1d41abed868a" />
