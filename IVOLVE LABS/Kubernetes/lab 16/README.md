Lab 16: Pod Resource Management with CPU and Memory Requests and Limits

## Objective

Configure CPU and memory requests and limits for the existing `ivolveapp` Deployment, inspect the Pod specification, and view live resource usage with `kubectl top`.

These steps follow the same sequence and command style as the supplied Lab 16 README. The README uses different resource names (`ivolve-app`, `application-ivolve`) and describes lowering requests and raising the quota to keep two replicas. This cluster uses `ivolveapp`, container `app`, and the original requested values, so the commands below use the names and configuration actually applied here.

## Cluster notes

- Namespace: `ivolve`
- Deployment: `ivolveapp`
- Application container: `app`
- Worker node: `k8s-worker` (`t3.small`)
- The worker had 2 CPUs and `1848468Ki` of allocatable memory (about `1.76Gi`).
- The application Deployment was set to one replica for this lab. Two replicas requesting `1Gi` each would require `2Gi`, which is more than the worker's allocatable memory.

## 1. Check worker capacity and current Pods

Run on the AWS control-plane instance:

```bash
kubectl describe node k8s-worker | sed -n '/Allocatable:/,/System Info:/p'
kubectl get pods -n ivolve -o wide
```

## 2. Set the application resources

Set one application replica and add the requested resources to the `app` container:

```bash
kubectl patch deployment ivolveapp -n ivolve --type=strategic -p '{"spec":{"replicas":1,"template":{"spec":{"containers":[{"name":"app","resources":{"requests":{"cpu":"1","memory":"1Gi"},"limits":{"cpu":"2","memory":"2Gi"}}}]}}}}'
```

Wait for the application rollout and find its Pod:

```bash
kubectl rollout status deployment/ivolveapp -n ivolve
kubectl get pods -n ivolve -l app=ivolveapp
POD=$(kubectl get pods -n ivolve -l app=ivolveapp -o jsonpath='{.items[0].metadata.name}')
```

Inspect the resource settings:

```bash
kubectl describe pod "$POD" -n ivolve | sed -n '/Containers:/,/Conditions:/p'
```

Expected values for container `app`:

```text
Requests:
  cpu:     1
  memory:  1Gi
Limits:
  cpu:     2
  memory:  2Gi
```

## 3. Install Metrics Server

`kubectl top` requires Metrics Server. Install the official release manifest:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

## 4. Allow Metrics Server to run on the tainted worker

The worker has the taint `workload=ivolveApp:NoSchedule`. Add a matching toleration to Metrics Server:

```bash
kubectl patch deployment metrics-server -n kube-system --type=merge -p '{"spec":{"template":{"spec":{"tolerations":[{"key":"workload","operator":"Equal","value":"ivolveApp","effect":"NoSchedule"}]}}}}'
```

## 5. Configure Metrics Server for the kubelet serving certificates

The kubelet certificates in this lab did not contain IP Subject Alternative Names (SANs). Metrics Server logs showed certificate verification errors when connecting to kubelets. For this lab cluster, add the testing flag that skips kubelet certificate validation:

```bash
kubectl patch deployment metrics-server -n kube-system --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

This flag is for testing environments; do not use it as a production TLS configuration.

Wait for Metrics Server and inspect live usage:

```bash
kubectl rollout status deployment/metrics-server -n kube-system --timeout=180s
kubectl top -n ivolve pod "$POD"
```

The completed check in this lab showed the application Pod using approximately `1m` CPU and `20Mi` memory. Metrics values change over time.


<img width="957" height="988" alt="image" src="https://github.com/user-attachments/assets/aa50ee86-7ea1-43ed-86ba-19fdf9a5a531" />
<img width="954" height="113" alt="image" src="https://github.com/user-attachments/assets/b31cb5dc-f434-40db-a563-737e8beeabc5" />
