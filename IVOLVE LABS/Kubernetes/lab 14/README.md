# Lab 14: StatefulSet with Headless Service

## Objective

Deploy a single-replica MySQL StatefulSet in the `ivolve` namespace. Configure it with the existing ConfigMap and Secret, schedule it on the labeled worker, persist its database files using a PVC template, and expose it through a headless Service.

> Kubernetes resource names must be lowercase. The task names `ivolveDB` and `ivolveDBSvc` are therefore written as `ivolvedb` and `ivolvedbsvc`.

## Prerequisites checked

- Namespace `ivolve` exists.
- ConfigMap `ivolvecm` contains `DB_HOST` and `DB_USER`.
- Secret `ivolvesecret` contains `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD`.
- Worker node `k8s-worker` has label `project=ivolve` and taint `workload=ivolveApp:NoSchedule`.
- The namespace quota allows the new pod.

Check the cluster resources:

```bash
kubectl get pods,svc,statefulset,pvc -n ivolve
kubectl get configmap,secret -n ivolve
kubectl describe quota ivolvequota -n ivolve
```

## 1. Create storage for the MySQL data

The existing Lab 13 claim is for application logs, so create a separate PV for MySQL. The hostPath is local to `k8s-worker`; node affinity keeps the database Pod on that node.

Create `lab14-pv.yaml`:

```bash
cat > lab14-pv.yaml <<'EOF'
apiVersion: v1
kind: PersistentVolume
metadata:
  name: ivolvedb-pv
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ""
  hostPath:
    path: /mnt/mysql-data
    type: DirectoryOrCreate
  nodeAffinity:
    required:
      nodeSelectorTerms:
        - matchExpressions:
            - key: kubernetes.io/hostname
              operator: In
              values:
                - k8s-worker
EOF
```

Apply and confirm the PV is available:

```bash
kubectl apply -f lab14-pv.yaml
kubectl get pv ivolvedb-pv
```

## 2. Create the Headless Service and StatefulSet

Create `lab14-mysql.yaml`:

```bash
cat > lab14-mysql.yaml <<'EOF'
apiVersion: v1
kind: Service
metadata:
  name: ivolvedbsvc
  namespace: ivolve
spec:
  clusterIP: None
  selector:
    app: ivolvedb
  ports:
    - name: mysql
      port: 3306
      targetPort: 3306
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: ivolvedb
  namespace: ivolve
spec:
  serviceName: ivolvedbsvc
  replicas: 1
  selector:
    matchLabels:
      app: ivolvedb
  template:
    metadata:
      labels:
        app: ivolvedb
    spec:
      nodeSelector:
        project: ivolve
      tolerations:
        - key: workload
          operator: Equal
          value: ivolveApp
          effect: NoSchedule
      containers:
        - name: mysql
          image: mysql:8.0
          ports:
            - name: mysql
              containerPort: 3306
          env:
            - name: MYSQL_DATABASE
              value: ivolve
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_HOST
            - name: MYSQL_USER
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_USER
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ivolvesecret
                  key: DB_PASSWORD
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ivolvesecret
                  key: MYSQL_ROOT_PASSWORD
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: mysql-data
      spec:
        accessModes:
          - ReadWriteOnce
        storageClassName: ""
        resources:
          requests:
            storage: 1Gi
EOF
```

Apply the Service and StatefulSet, then wait for rollout:

```bash
kubectl apply -f lab14-mysql.yaml
kubectl rollout status statefulset/ivolvedb -n ivolve --timeout=180s
kubectl get svc,statefulset,pods,pvc -n ivolve -o wide
```

Expected results: `ivolvedb` is ready with one replica, Pod `ivolvedb-0` is `Running`, and PVC `mysql-data-ivolvedb-0` is `Bound` to `ivolvedb-pv`.

## 3. Verify MySQL connectivity

Connect to MySQL through the headless Service DNS name and run a simple query:

```bash
kubectl exec -n ivolve ivolvedb-0 -- sh -c 'mysql -h ivolvedbsvc.ivolve.svc.cluster.local -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT 1;"'
```

The successful query returns `1`. MySQL may print a warning that using a password on the command line can be insecure; this warning does not mean the query failed.

## 4. Verify Service DNS resolution

```bash
kubectl exec -n ivolve ivolvedb-0 -- getent hosts ivolvedbsvc.ivolve.svc.cluster.local
```

The command returns the Pod IP for `ivolvedb-0` because the Service is headless.

## Completed verification

- `ivolvedb` StatefulSet: one ready MySQL replica.
- `ivolvedbsvc`: headless Service on port `3306`.
- ConfigMap and Secret values are consumed as container environment variables.
- The Pod uses `project=ivolve` node selection and tolerates `workload=ivolveApp:NoSchedule`.
- PVC template mounts persistent storage at `/var/lib/mysql`; its generated claim is bound.
- MySQL query and in-cluster Service DNS lookup both succeed.
- <img width="958" height="1074" alt="image" src="https://github.com/user-attachments/assets/9f10c804-411e-46c8-b282-03dcd26e409f" />
