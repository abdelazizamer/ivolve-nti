# Lab 12: Managing Configuration and Sensitive Data with ConfigMaps and Secrets

## Objective

Store MySQL connection settings in a ConfigMap and credentials in a Secret in the `ivolve` namespace. Use them to run MySQL behind a headless Service and verify a database connection.

## 1. Confirm the namespace

The `ivolve` namespace was created in Lab 11. Verify it exists:

```bash
kubectl get namespace ivolve
```

## 2. Prepare Base64 values for the Secret

Generate the Base64 values for the MySQL root password and application database password:

```bash
printf '%s' '<MYSQL_ROOT_PASSWORD>' | base64 -w0; echo
printf '%s' '<DB_PASSWORD>' | base64 -w0; echo
```

Replace the placeholders locally. Base64 is encoding, not encryption; do not commit real credentials to a public repository.

## 3. Create the ConfigMap and Secret

Kubernetes resource names must be lowercase. Use `ivolvecm` and `ivolvesecret`; the mixed-case names in the lab sheet are not valid Kubernetes object names.

Create `lab12-config.yaml`, replacing the Base64 placeholders with the generated values:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ivolvecm
  namespace: ivolve
data:
  DB_HOST: mysql
  DB_USER: ivolve_user
---
apiVersion: v1
kind: Secret
metadata:
  name: ivolvesecret
  namespace: ivolve
type: Opaque
data:
  MYSQL_ROOT_PASSWORD: <BASE64_ROOT_PASSWORD>
  DB_PASSWORD: <BASE64_DB_PASSWORD>
```

Apply and verify:

```bash
kubectl apply -f lab12-config.yaml
kubectl describe configmap ivolvecm -n ivolve
kubectl describe secret ivolvesecret -n ivolve
```

The ConfigMap should show `DB_HOST=mysql` and `DB_USER=ivolve_user`. The Secret description should list both keys without displaying their values.

## 4. Create the MySQL headless Service and StatefulSet

No MySQL Service or StatefulSet existed in the namespace. Create `lab12-mysql.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: ivolve
spec:
  clusterIP: None
  selector:
    app: mysql
  ports:
    - name: mysql
      port: 3306
      targetPort: 3306
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
  namespace: ivolve
spec:
  serviceName: mysql
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
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
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: 500m
              memory: 768Mi
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
          readinessProbe:
            exec:
              command:
                - sh
                - -c
                - 'mysqladmin ping -h 127.0.0.1 -u root -p"$MYSQL_ROOT_PASSWORD"'
            initialDelaySeconds: 20
            periodSeconds: 5
            failureThreshold: 20
      volumes:
        - name: mysql-data
          emptyDir: {}
```

The worker selector and toleration allow this Pod to run on the Lab 10 worker. `emptyDir` is temporary storage; database files are lost if the Pod is recreated.

Apply and wait for the StatefulSet:

```bash
kubectl apply -f lab12-mysql.yaml
kubectl rollout status statefulset/mysql -n ivolve --timeout=180s
kubectl get svc,statefulset,pods -n ivolve -o wide
```

Expected state: Service `mysql` has `CLUSTER-IP` `None`, StatefulSet is `1/1`, and Pod `mysql-0` is `Running` on `k8s-worker`.

## 5. Verify the application database credentials

```bash
kubectl exec -n ivolve mysql-0 -- sh -c 'mysql -h mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT 1;"'
```

A successful result includes `1`. MySQL may warn that using a password on the command line is insecure; the warning does not mean the query failed.

## Final verification

```bash
kubectl get configmap,secret,svc,statefulset,pods -n ivolve
```



<img width="960" height="1079" alt="image" src="https://github.com/user-attachments/assets/f216bf05-4c2f-4690-8e69-ad2bf50ba151" />
