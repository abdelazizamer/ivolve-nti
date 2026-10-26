Lab 9: Deploy a Kubernetes Cluster Using kubeadm on AWS

## Objective

Deploy a two-node Kubernetes cluster on AWS EC2 using containerd, kubeadm, kubelet, kubectl, and the Calico CNI. Initialize one control-plane node, join one worker node, and verify that both nodes and the cluster components are healthy.

## Lab topology

| Role | Example hostname | Example private IP |
|---|---|---|
| Control plane | `k8s-control-plane` | `172.31.19.122` |
| Worker | `k8s-worker` | `172.31.26.136` |

Use the private IPs shown on your own instances if they differ. Public IPs can change; use the current public IP only for SSH from your computer.

## 1. Create the EC2 instances

Create two Ubuntu Server EC2 instances in the same AWS Region and VPC. This lab used Ubuntu 26.04 LTS, `t3.small` instances (2 vCPUs, 2 GiB RAM), and 15 GiB `gp3` root volumes. Use the same key pair and enable public IPv4 addresses for SSH.

Configure a security group with:

- Inbound SSH (TCP 22) restricted to **My IP**.
- Cluster traffic allowed between instances using the same security group. For a tighter policy, allow only the required Kubernetes and CNI ports between nodes.
- No public inbound Kubernetes API rule is required for this setup.

Disable **source/destination checks** on both EC2 instances for Calico pod networking.

From Windows PowerShell, connect to each instance using its current public IP:

```powershell
ssh -i "$env:USERPROFILE\Documents\Downloads\k8s-lab-key.pem" ubuntu@<CURRENT_PUBLIC_IP>
```

Run the remaining Linux commands inside the Ubuntu SSH session, not at the PowerShell prompt.

## 2. Prepare both nodes

Run the following on both instances. Set unique hostnames:

Control plane:

```bash
sudo hostnamectl set-hostname k8s-control-plane
```

Worker:

```bash
sudo hostnamectl set-hostname k8s-worker
```

Disable swap and persist the setting:

```bash
sudo swapoff -a
sudo sed -ri '/\sswap\s/s/^#?/#/' /etc/fstab
swapon --show
```

`swapon --show` should return no active swap devices.

Load required kernel modules and configure networking on both nodes:

```bash
sudo modprobe overlay
sudo modprobe br_netfilter
printf "overlay\nbr_netfilter\n" | sudo tee /etc/modules-load.d/k8s.conf
printf "net.bridge.bridge-nf-call-iptables = 1\nnet.bridge.bridge-nf-call-ip6tables = 1\nnet.ipv4.ip_forward = 1\n" | sudo tee /etc/sysctl.d/k8s.conf
sudo sysctl --system
```

## 3. Install and configure containerd on both nodes

```bash
sudo apt-get update
sudo apt-get install -y containerd ca-certificates curl gpg
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl restart containerd
sudo systemctl enable containerd
sudo systemctl is-active containerd
```

The last command should print `active`.

## 4. Install Kubernetes packages on both nodes

Add the Kubernetes v1.37 package repository and install the tools on both nodes:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key | sudo gpg --dearmor --yes -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
sudo chmod 644 /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
sudo systemctl enable kubelet
```

Verify on both nodes:

```bash
containerd --version
kubeadm version -o short
kubelet --version
kubectl version --client
sudo systemctl is-active containerd
swapon --show
```

The Kubernetes versions must match on both nodes. This lab used Kubernetes `v1.37.1` and containerd `2.2.2`.

## 5. Initialize the control plane

On the control-plane node, use its **private** IP address:

```bash
sudo kubeadm init \
  --apiserver-advertise-address=172.31.19.122 \
  --pod-network-cidr=192.168.0.0/16 \
  --cri-socket=unix:///run/containerd/containerd.sock
```

Wait for `Your Kubernetes control-plane has initialized successfully!`. Save the generated `kubeadm join` command for the worker. Treat its token as a credential and do not publish it.

Configure kubectl for the Ubuntu user on the control plane:

```bash
mkdir -p "$HOME/.kube"
sudo cp -i /etc/kubernetes/admin.conf "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"
```

## 6. Install the Calico pod network

On the control-plane node, install Calico. Its default pod CIDR matches the `192.168.0.0/16` value used above:

```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.33.0/manifests/calico.yaml
```

Watch the system pods:

```bash
kubectl get pods -A -w
```

Wait for Calico and CoreDNS pods to become `Running`, then press `Ctrl+C` to stop watching.

## 7. Join the worker

On the worker, run the complete `kubeadm join` command saved from `kubeadm init`, with `sudo`:

```bash
sudo kubeadm join <CONTROL_PLANE_PRIVATE_IP>:6443 --token <TOKEN> \
  --discovery-token-ca-cert-hash sha256:<CA_CERT_HASH>
```

Replace the placeholders with the exact values from the generated command. If the token expires, create a fresh command on the control plane:

```bash
sudo kubeadm token create --ttl 30m --print-join-command
```

Run the generated command with `sudo` on the worker.

## 8. Verify the cluster

On the control-plane node:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl cluster-info
```

Success criteria:

- Both nodes show `Ready`.
- Calico, CoreDNS, etcd, API server, controller manager, scheduler, and kube-proxy pods show `Running` (or `Completed` where appropriate).
- `kubectl cluster-info` reports the Kubernetes control-plane endpoint.

## 9. Capture evidence and clean up

Capture required screenshots before shutting down the lab. Stop or terminate the EC2 instances when finished. Stopping instances can leave EBS volumes incurring storage charges; terminating instances may delete their root volumes. Review the AWS account's current Free Tier or Free Plan terms and billing page.

<img width="1910" height="1079" alt="Screenshot 2026-10-26 144110" src="https://github.com/user-attachments/assets/91a7ade4-7a74-4cdc-b8dd-b1173edcd6b6" />

