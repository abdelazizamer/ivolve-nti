Kubernetes Lab 19: RBAC and ServiceAccount Steps

This guide creates a ServiceAccount for Jenkins in the `ivolve` namespace, grants it permission to create and update Deployments in that namespace, and verifies allowed and denied actions.

## 1. Connect and check the namespace

From Windows PowerShell, connect to the control-plane using your own key file and current public IP:

```powershell
ssh -i "<PATH_TO_KEY.pem>" ubuntu@<CONTROL_PLANE_PUBLIC_IP>
```

On the control-plane:

```bash
kubectl config current-context
kubectl get namespace ivolve
```

The namespace must exist before applying the Lab 19 resources.

## 2. Create the working directory

```bash
mkdir -p ~/Lab19
cd ~/Lab19
```

If the SSH connection drops and reconnects, return to this directory with `cd ~/Lab19` before using the manifest files.

## 3. Create the ServiceAccount

Create `serviceaccount.yaml`:

```bash
cat > serviceaccount.yaml <<'EOF'
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkinssa
  namespace: ivolve
EOF
```

Validate and apply it:

```bash
kubectl apply --dry-run=server -f serviceaccount.yaml
kubectl apply -f serviceaccount.yaml
```

## 4. Create the Role

This Role grants only `create` and `update` on Deployments in `ivolve`.

Create `role.yaml`:

```bash
cat > role.yaml <<'EOF'
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ivolvedeployrole
  namespace: ivolve
rules:
  - apiGroups:
      - apps
    resources:
      - deployments
    verbs:
      - create
      - update
EOF
```

Validate and apply it:

```bash
kubectl apply --dry-run=server -f role.yaml
kubectl apply -f role.yaml
```

## 5. Bind the Role to the ServiceAccount

Create `rolebinding.yaml`:

```bash
cat > rolebinding.yaml <<'EOF'
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ivolvedeployrolebinding
  namespace: ivolve
subjects:
  - kind: ServiceAccount
    name: jenkinssa
    namespace: ivolve
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: ivolvedeployrole
EOF
```

Validate and apply it:

```bash
kubectl apply --dry-run=server -f rolebinding.yaml
kubectl apply -f rolebinding.yaml
```

## 6. Verify the resources

```bash
kubectl get serviceaccount,role,rolebinding -n ivolve
kubectl describe role ivolvedeployrole -n ivolve
kubectl describe rolebinding ivolvedeployrolebinding -n ivolve
```

The Role should list `deployments.apps` with verbs `[create update]`, and the RoleBinding subject should be ServiceAccount `jenkinssa` in namespace `ivolve`.

## 7. Verify allowed and denied permissions

Run these checks as written:

```bash
kubectl auth can-i create deployments.apps --as=system:serviceaccount:ivolve:jenkinssa -n ivolve
kubectl auth can-i update deployments.apps --as=system:serviceaccount:ivolve:jenkinssa -n ivolve
kubectl auth can-i delete deployments.apps --as=system:serviceaccount:ivolve:jenkinssa -n ivolve
kubectl auth can-i create configmaps --as=system:serviceaccount:ivolve:jenkinssa -n ivolve
kubectl auth can-i create deployments.apps --as=system:serviceaccount:ivolve:jenkinssa -n kube-system
```
results, in order:

```text
yes
yes
no
no
no
```


If Kubernetes reports that impersonation is forbidden, the current `kubectl` identity needs permission to impersonate ServiceAccounts; use an administrator kubeconfig for this verification.



<img width="938" height="627" alt="Screenshot 2026-11-04 145925" src="https://github.com/user-attachments/assets/418ba06b-76d8-4d91-bea2-c92e3686622c" />
<img width="950" height="1078" alt="Screenshot 2026-11-04 145822" src="https://github.com/user-attachments/assets/0cfe563a-6b60-4c4d-9fc5-a7e43a19c6ac" />










