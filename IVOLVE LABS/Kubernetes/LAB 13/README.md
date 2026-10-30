# Lab 13: Persistent Storage Setup for Application Logging

## Objective

Create a 1 GiB PersistentVolume (PV) using `hostPath` at `/mnt/app-logs`, and a matching PersistentVolumeClaim (PVC) in the `ivolve` namespace. Both use `ReadWriteMany`; the PV uses the `Retain` reclaim policy. No StorageClass is used.

## 1. Confirm the cluster

Run on the control-plane node:

```bash
kubectl get namespace ivolve
kubectl get nodes -o wide
```

The namespace exists and the worker node is `k8s-worker`.

## 2. Create the PV manifest

```bash
cat > lab13-pv.yaml <<'EOF'
apiVersion: v1
kind: PersistentVolume
metadata:
  name: ivolvepv
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ""
  hostPath:
    path: /mnt/app-logs
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

Apply the PV and confirm it is available:

```bash
kubectl apply -f lab13-pv.yaml
kubectl get pv ivolvepv
```

The PV was created with `1Gi`, `RWX`, and `Retain`; its initial status was `Available`.

## 3. Create the PVC manifest

```bash
cat > lab13-pvc.yaml <<'EOF'
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ivolvepvc
  namespace: ivolve
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: ""
  resources:
    requests:
      storage: 1Gi
  volumeName: ivolvepv
EOF
```

Apply the PVC and check that it binds to the PV:

```bash
kubectl apply -f lab13-pvc.yaml
kubectl get pv ivolvepv
kubectl get pvc ivolvepvc -n ivolve
```

The PV and PVC both reached `Bound`; the claim was `ivolve/ivolvepvc`.

## 4. Verify the final configuration

```bash
kubectl describe pv ivolvepv
kubectl describe pvc ivolvepvc -n ivolve
```

Verified values:

- PV: `ivolvepv`, capacity `1Gi`, access mode `RWX`, reclaim policy `Retain`.
- PV source: `HostPath`, path `/mnt/app-logs`, type `DirectoryOrCreate`.
- PV node affinity: hostname `k8s-worker`.
- PV and PVC StorageClass: empty; no StorageClass is used.
- PVC: `ivolvepvc` in namespace `ivolve`, requests `1Gi`, status `Bound` to `ivolvepv`.


<img width="956" height="1079" alt="image" src="https://github.com/user-attachments/assets/4b884d6b-dd64-47c1-81e5-775ba057b83a" />

