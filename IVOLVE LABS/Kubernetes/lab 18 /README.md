
# Kubernetes Lab 18: Deployment Steps

This file summarizes the steps used to deploy Node Exporter, Prometheus, Alertmanager, and Grafana to a Kubernetes cluster.

## 1. Connect to the server and inspect the cluster

From Windows PowerShell, connect using your own key file and current control-plane address:

```powershell
ssh -i "<PATH_TO_KEY.pem>" ubuntu@<CONTROL_PLANE_PUBLIC_IP>
```

On the control-plane, inspect the context, node health, capacity, storage classes, taints, labels, and existing Pods:

```bash
kubectl config current-context
kubectl get nodes -o wide
kubectl top nodes
kubectl get storageclass
kubectl describe nodes <CONTROL_PLANE_NODE> <WORKER_NODE> | grep -i taints
kubectl get nodes --show-labels
kubectl get pods -A -o wide
```

Check node names, health, available resources, and taints before deploying. This cluster had no StorageClass, so temporary `emptyDir` volumes were used.

## 2. Create the working directory and namespace

```bash
mkdir -p ~/Lab18
cd ~/Lab18
```

Create `namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
```

Validate and apply it:

```bash
kubectl apply --dry-run=server -f namespace.yaml
kubectl apply -f namespace.yaml
```

## 3. Deploy Node Exporter on every node

Create `node-exporter-daemonset.yaml` as a DaemonSet in the `monitoring` namespace. Use image `quay.io/prometheus/node-exporter:v1.8.2`, port `9100`, and a `NoSchedule` toleration so it can run on both tainted nodes. Use `hostNetwork: true`, mount the host root read-only, and set `--path.rootfs=/host`.

Validate, apply, and verify:

```bash
kubectl apply --dry-run=server -f node-exporter-daemonset.yaml
kubectl apply -f node-exporter-daemonset.yaml
kubectl rollout status daemonset/node-exporter -n monitoring --timeout=120s
kubectl get daemonset,pods -n monitoring -o wide
```

Confirm the number of ready Node Exporter Pods equals the number of cluster nodes.

## 4. Configure and deploy Prometheus

Create `prometheus-config.yaml` as a ConfigMap named `prometheus-config` in the `monitoring` namespace. Set the scrape interval to `15s`, add the current internal node addresses on port `9100` as Node Exporter targets, and add `localhost:9090` as the Prometheus target. Configure Alertmanager at:

```text
alertmanager.monitoring.svc.cluster.local:9093
```

Validate and apply the ConfigMap:

```bash
kubectl apply --dry-run=server -f prometheus-config.yaml
kubectl apply -f prometheus-config.yaml
```

Create `prometheus.yaml` as a single-replica Deployment in the `monitoring` namespace. Use image `prom/prometheus:v2.55.1`, mount `prometheus-config` at `/etc/prometheus`, set seven-day retention, and use an `emptyDir` for `/prometheus`. Add a `NoSchedule` toleration.

Create `prometheus-service.yaml` as a ClusterIP Service on port `9090`.

```bash
kubectl apply --dry-run=server -f prometheus.yaml
kubectl apply -f prometheus.yaml
kubectl apply --dry-run=server -f prometheus-service.yaml
kubectl apply -f prometheus-service.yaml
kubectl rollout status deployment/prometheus -n monitoring --timeout=180s
```

## 5. Configure and deploy Alertmanager

Create `alertmanager-config.yaml` as a ConfigMap named `alertmanager-config`. Configure a default route to a receiver named `default`.

Create `alertmanager.yaml` as a single-replica Deployment using image `quay.io/prometheus/alertmanager:v0.27.0`. Mount the configuration file and use `emptyDir` for its data.

Create `alertmanager-service.yaml` as a ClusterIP Service on port `9093`.

```bash
kubectl apply --dry-run=server -f alertmanager-config.yaml
kubectl apply -f alertmanager-config.yaml
kubectl apply --dry-run=server -f alertmanager.yaml
kubectl apply -f alertmanager.yaml
kubectl apply --dry-run=server -f alertmanager-service.yaml
kubectl apply -f alertmanager-service.yaml
kubectl rollout status deployment/alertmanager -n monitoring --timeout=180s
```

The current configuration receives and routes alerts, but does not send them to email or Slack because no external receiver is configured.

## 6. Deploy Grafana

Create `grafana.yaml` as a single-replica Deployment using image `grafana/grafana:11.4.0`. Use an `emptyDir` for `/var/lib/grafana` and add a `NoSchedule` toleration.

Create `grafana-service.yaml` as a ClusterIP Service on port `3000`.

```bash
kubectl apply --dry-run=server -f grafana.yaml
kubectl apply -f grafana.yaml
kubectl apply --dry-run=server -f grafana-service.yaml
kubectl apply -f grafana-service.yaml
kubectl rollout status deployment/grafana -n monitoring --timeout=180s
```

## 7. Verify the services

```bash
kubectl get pods,services -n monitoring -o wide
kubectl get daemonset node-exporter -n monitoring
kubectl get endpointslice -n monitoring
```

Confirm Prometheus, Alertmanager, and Grafana are ready, and that the DaemonSet has one ready Pod on each node.

## 8. Open Prometheus through an SSH tunnel

On the control-plane:

```bash
kubectl port-forward -n monitoring service/prometheus 9090:9090
```

In a separate Windows PowerShell window:

```powershell
ssh -i "<PATH_TO_KEY.pem>" -L 9090:127.0.0.1:9090 -N ubuntu@<CONTROL_PLANE_PUBLIC_IP>
```

Open `http://localhost:9090/targets` and confirm both Node Exporter targets show `UP`.

## 9. Open Grafana and configure the dashboard

Stop the Prometheus tunnel and port-forward with `Ctrl+C`. On the control-plane, run:

```bash
kubectl port-forward -n monitoring service/grafana 3000:3000
```

In another PowerShell window:

```powershell
ssh -i "<PATH_TO_KEY.pem>" -L 3000:127.0.0.1:3000 -N ubuntu@<CONTROL_PLANE_PUBLIC_IP>
```

Open `http://localhost:3000` and sign in. Add a Prometheus data source with this URL:

```text
http://prometheus.monitoring.svc.cluster.local:9090
```

Click **Save & test**. Then go to **Dashboards → New → Import**, import Dashboard ID `1860`, and select `prometheus` as the data source if prompted. Change the **Nodename** selector to confirm metrics appear for each node.

## 10. Lab 18 manifest files

The following YAML files are in `~/Lab18` on the server:

```text
namespace.yaml
node-exporter-daemonset.yaml
prometheus-config.yaml
prometheus.yaml
prometheus-service.yaml
alertmanager-config.yaml
alertmanager.yaml
alertmanager-service.yaml
grafana.yaml
grafana-service.yaml
```
<img width="952" height="716" alt="Screenshot 2026-11-04 022215" src="https://github.com/user-attachments/assets/efc081a3-51f9-4870-8925-558aef7ac0f0" />
<img width="1919" height="794" alt="Screenshot 2026-11-04 020313" src="https://github.com/user-attachments/assets/e111c593-633d-4374-a009-730ca68522ab" />
<img width="1915" height="925" alt="Screenshot 2026-11-04 020403" src="https://github.com/user-attachments/assets/744299f7-fa9e-4cb4-88a8-87db2e7b4f9c" />
<img width="1919" height="928" alt="Screenshot 2026-11-04 021728" src="https://github.com/user-attachments/assets/40a7dd8f-de02-4950-9ec5-9fb6dcc0d451" />
<img width="1919" height="929" alt="Screenshot 2026-11-04 015058" src="https://github.com/user-attachments/assets/10bf0269-42ca-4e84-9f92-b75eebe54128" />

