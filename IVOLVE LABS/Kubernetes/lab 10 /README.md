# Kubernetes Lab 10: Node Isolation Using Taints and Labels

## Objective

Apply a taint and a label to the worker node, then verify that both are configured correctly.

## Concepts

- **Taint:** Marks a node so that Pods without a matching toleration are not scheduled on it.
- **Toleration:** Allows a Pod to be scheduled on a node with a matching taint. A toleration does not select the node by itself.
- **`NoSchedule`:** Prevents new Pods without a matching toleration from being scheduled on the node. Existing Pods are not evicted.
- **Label:** A key-value tag used to identify and select nodes.
- **Node selector and node affinity:** Pod scheduling rules that can select nodes based on labels. This lab applies and verifies a label; it does not require creating or scheduling a Pod.

## Worker Node

The worker node used for this lab is:

```text
k8s-worker
```

Its private IP address is `172.31.26.136`. To check the current node names and addresses:

```bash
kubectl get nodes -o wide
```

Run the following steps from the control-plane terminal where `kubectl` is configured.

## Steps

### 1. Apply the taint

```bash
kubectl taint nodes k8s-worker workload=ivolveApp:NoSchedule --overwrite
```

This taint prevents new Pods without a matching toleration from being scheduled on the worker.

### 2. Apply the label

```bash
kubectl label nodes k8s-worker project=ivolve --overwrite
```

The label identifies the worker as part of the `ivolve` project. It can be used by Pod scheduling rules such as `nodeSelector` or node affinity.

### 3. Verify the taint and label

Check the taint and node details:

```bash
kubectl describe node k8s-worker
```

Confirm the output includes:

```text
Taints: workload=ivolveApp:NoSchedule
```

Check the project label:

```bash
kubectl get nodes -L project
```

The `k8s-worker` row should show `ivolve` in the `PROJECT` column. To display all node labels, run:

```bash
kubectl get nodes --show-labels
```
<img width="960" height="1079" alt="Screenshot 2026-10-27 161037" src="https://github.com/user-attachments/assets/ba99d1d6-31d1-4707-9b1f-6df8dc6a5366" />

