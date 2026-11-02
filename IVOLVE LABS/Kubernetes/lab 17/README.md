# Lab 17 — Control Pod-to-Pod Traffic via Network Policy

Restrict access to the MySQL pod so only the application pod can reach TCP port `3306`.

| Item | Value |
|---|---|
| NetworkPolicy | `ivolvenp` in namespace `ivolve` |
| Applies to | Pods labelled `app=mysql` (`mysql-0`) |
| Allowed ingress | Pods labelled `app=ivolveapp`, TCP `3306` |
| Policy types | `Ingress` only |
| Egress | Not restricted |
| CNI | Calico (enforces NetworkPolicy) |

The resource name is lowercase because Kubernetes resource names must be lowercase. The steps below use the names and labels from this cluster.

## Step 1 — Check the pod labels

Run on the Kubernetes control-plane node:

```bash
kubectl get pods -n ivolve --show-labels -o wide
```

Confirm that `mysql-0` has label `app=mysql` and the application pod has label `app=ivolveapp`.

## Step 2 — Create the policy manifest

```bash
cat > lab17-network-policy.yaml <<'EOF'
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ivolvenp
  namespace: ivolve
spec:
  podSelector:
    matchLabels:
      app: mysql
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: ivolveapp
      ports:
        - protocol: TCP
          port: 3306
EOF
```

## Step 3 — Apply and verify

```bash
kubectl apply -f lab17-network-policy.yaml
kubectl get networkpolicy -n ivolve
kubectl describe -n ivolve networkpolicies.networking.k8s.io ivolvenp
```

The `describe` output should show `PodSelector: app=mysql`, ingress from `app=ivolveapp` on `3306/TCP`, and `Not affecting egress traffic`.

## Step 4 — Verify allowed traffic from the application pod

```bash
kubectl exec -n ivolve deployment/ivolveapp -c app -- node -e 'const net=require("net"); const s=net.createConnection({host:"mysql",port:3306}); s.setTimeout(5000); s.on("connect",()=>{console.log("PASS: app can reach MySQL on TCP/3306");s.end();process.exit(0)}); s.on("timeout",()=>{console.error("FAIL: connection timed out");s.destroy();process.exit(1)}); s.on("error",e=>{console.error("FAIL:",e.message);process.exit(1)});'
```

Expected result:

```text
PASS: app can reach MySQL on TCP/3306
```

## Step 5 — Verify traffic from an unselected pod is denied

This temporary pod does not have the allowed `app=ivolveapp` label. The node selector and toleration let it run on the tainted worker.

```bash
kubectl run netpol-deny-test -n ivolve --rm -i --restart=Never \
  --image=node:20-alpine \
  --labels=netpol-test=untrusted \
  --overrides='{"spec":{"nodeSelector":{"project":"ivolve"},"tolerations":[{"key":"workload","operator":"Equal","value":"ivolveApp","effect":"NoSchedule"}]}}' \
  --command -- node -e 'const net=require("net"); const s=net.createConnection({host:"mysql",port:3306}); s.setTimeout(5000); s.on("connect",()=>{console.error("FAIL: unauthorized pod connected to MySQL");s.end();process.exit(1)}); s.on("timeout",()=>{console.log("PASS: NetworkPolicy blocked unauthorized pod");s.destroy();process.exit(0)}); s.on("error",e=>{console.error("FAIL:",e.message);process.exit(1)});'
```

Expected result:

```text
PASS: NetworkPolicy blocked unauthorized pod
pod "netpol-deny-test" deleted from ivolve namespace
```

## Result

The policy selects `mysql-0` and allows only the application pods to connect to it on TCP `3306`. The application connection succeeded, while the unselected temporary pod was blocked. Calico enforces the policy in this cluster.

<img width="946" height="413" alt="image" src="https://github.com/user-attachments/assets/a8093168-b600-465b-a6cd-672592d1d623" />
