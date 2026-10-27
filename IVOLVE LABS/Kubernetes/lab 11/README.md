Lab 11: Namespace Management and Resource Quota

## 1. Create the namespace and quota

Run on the control-plane node:

```bash
kubectl create namespace ivolve
kubectl create quota ivolvequota --hard=pods=3 -n ivolve
```

Kubernetes resource names must be lowercase, so use `ivolvequota` instead of `ivolveQuota`.

## 2. Verify the quota

```bash
kubectl get namespace ivolve
kubectl describe resourcequota ivolvequota -n ivolve
```

Confirm that `Hard` shows `pods: 3`.

## 3. Create three test Pods

Run each command separately:

```bash
kubectl run quota-test-1 -n ivolve --image=busybox:1.36 --restart=Never --command -- sleep 3600
kubectl run quota-test-2 -n ivolve --image=busybox:1.36 --restart=Never --command -- sleep 3600
kubectl run quota-test-3 -n ivolve --image=busybox:1.36 --restart=Never --command -- sleep 3600
```

Verify the Pods and quota usage:

```bash
kubectl get pods -n ivolve
kubectl describe resourcequota ivolvequota -n ivolve
```

The quota should show `Used: pods: 3`.

## 4. Confirm the quota blocks an additional Pod

```bash
kubectl run quota-test-4 -n ivolve --image=busybox:1.36 --restart=Never --command -- sleep 3600
```

The command should fail with an `exceeded quota` error. Pods may remain `Pending` because of the Lab 10 node taints; they still count toward the Pod quota.

## 5. Remove the test Pods

After recording the verification and rejection output:

```bash
kubectl delete pods quota-test-1 quota-test-2 quota-test-3 -n ivolve
```
<img width="946" height="739" alt="Screenshot 2026-10-27 163439" src="https://github.com/user-attachments/assets/43e7a5e1-4a31-49ad-8bc6-d909b93ab2ec" />
