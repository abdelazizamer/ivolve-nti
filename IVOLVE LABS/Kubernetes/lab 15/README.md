# Lab 15: Node.js Application Deployment with ClusterIP Service

## Objective

Deploy the Lab 8 Node.js application in the `ivolve` Kubernetes namespace with two replicas, connect it to MySQL, mount the Lab 13 application log PVC, and expose it internally through a ClusterIP Service.

## Prerequisites

The following resources were created in earlier labs:

- Namespace: `ivolve`
- ConfigMap: `ivolvecm` (`DB_HOST=mysql`, `DB_USER=ivolve_user`)
- Secret: `ivolvesecret` (`MYSQL_ROOT_PASSWORD`, `DB_PASSWORD`)
- MySQL Service and Pod: `mysql` / `mysql-0`
- Lab 13 PVC: `ivolvepvc`
- Worker node label: `project=ivolve`
- Worker node taint: `workload=ivolveApp:NoSchedule`
- ResourceQuota: maximum of 3 Pods in `ivolve`

The quota allows one MySQL Pod and two application Pods. The existing `ivolvedb` StatefulSet was scaled to zero to leave room for the application replicas.

## 1. Build and push the Lab 8 application image

On the VMware Ubuntu VM, go to the Lab 8 application directory:

```bash
cd ~/ivolve-nti/"IVOLVE LABS"/Docker/Lab8
```

Build the custom image and push it to Docker Hub:

```bash
docker build -t abdelazizamer/new-app:lab8 .
docker login -u abdelazizamer
docker push abdelazizamer/new-app:lab8
```

Enter the Docker Hub Personal Access Token when `docker login` prompts for a password. The Lab 8 application listens on port `3000`.

## 2. Configure the Kubernetes manifest

On the AWS control-plane instance, the manifest is `~/lab15-app.yaml`. It configures:

- Deployment `ivolveapp` with 2 replicas.
- The custom image `abdelazizamer/new-app:lab8`.
- Environment variables from `ivolvecm` and `ivolvesecret`.
- Node selector `project=ivolve` and a toleration for `workload=ivolveApp:NoSchedule`.
- Init container `setup-database`, which waits for MySQL, creates database `ivolve`, creates `ivolve_user`, and grants privileges on `ivolve.*`.
- PVC `ivolvepvc` mounted at `/app/logs`.
- ClusterIP Service `ivolveappsvc` on port `3000`.

Because the namespace quota is 3 Pods, scale the application Deployment to zero before applying the updated manifest:

```bash
kubectl scale deployment ivolveapp --replicas=0 -n ivolve
```

Update the image and application/Service ports in the existing manifest:

```bash
sed -i \
  -e 's|abdelazizamer/node-hello-nti:6|abdelazizamer/new-app:lab8|' \
  -e 's/containerPort: 5000/containerPort: 3000/' \
  -e 's/targetPort: 5000/targetPort: 3000/' \
  -e 's/port: 5000/port: 3000/' \
  ~/lab15-app.yaml
```

Confirm the image and ports before applying:

```bash
grep -E 'image:|containerPort:|targetPort:|port:' ~/lab15-app.yaml
```

## 3. Deploy the application

```bash
kubectl apply -f ~/lab15-app.yaml
kubectl rollout status deployment/ivolveapp -n ivolve --timeout=180s
kubectl get deployment,pods,svc -n ivolve -o wide
```

Confirm the Deployment has 2 ready replicas, both Pods are running on the worker, and the Service is `ClusterIP` with port `3000`.

## 4. Verify database setup and application connection

Inspect the init-container and application logs for both replicas:

```bash
for p in $(kubectl get pods -n ivolve -l app=ivolveapp -o name); do
  echo "=== $p: database setup ==="
  kubectl logs -n ivolve "$p" -c setup-database
  echo "=== $p: application ==="
  kubectl logs -n ivolve "$p" -c app
done
```

The application logs should include a successful MySQL connection and show that the server listens on `0.0.0.0:3000`.

Verify that the Service routes to both Pods and that the health endpoint responds successfully:

```bash
kubectl get endpoints ivolveappsvc -n ivolve
kubectl exec -n ivolve deployment/ivolveapp -c app -- node -e 'fetch("http://ivolveappsvc:3000/health").then(async r => { console.log("HTTP", r.status); console.log(await r.text()); process.exit(r.ok ? 0 : 1); }).catch(e => { console.error(e); process.exit(1); })'
```

Expected HTTP status: `200`.

Connect to MySQL using the application database user to verify the database and privileges:

```bash
kubectl exec -n ivolve mysql-0 -- sh -c \
'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT DATABASE(), CURRENT_USER(); SHOW GRANTS;"'
```

Confirm the output identifies database `ivolve`, user `ivolve_user`, and a grant on `ivolve.*`.

<img width="971" height="1079" alt="image" src="https://github.com/user-attachments/assets/bdfbe65b-69eb-49b7-bec1-63acc8649b25" />

