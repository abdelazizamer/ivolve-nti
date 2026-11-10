# Lab 24: Initial Ansible Configuration and Ad-Hoc Execution

## Objective

Configure Ansible on an EC2 instance acting as the **Control Node**, connect it to a second EC2 instance acting as the **Managed Node** over SSH, and install Docker on the managed host using an Ansible ad-hoc command.

## EC2 instances

| Role | Instance | Connection address |
|---|---|---|
| Control Node | `k8s-control-pl...` | Public IP: `54.91.107.214` |
| Managed Node | `k8s-worker` | Private IP: `172.31.26.136` |

Both instances run Ubuntu and use the `ubuntu` account. The AWS key pair file `k8s-lab-key.pem` was used for the initial connection to the Control Node.

## Steps

### 1. Connect to the Control Node from Windows PowerShell

```powershell
ssh -i "$env:USERPROFILE\Documents\Downloads\k8s-lab-key.pem" ubuntu@54.91.107.214
```

### 2. Install Ansible on the Control Node

Run these commands in the Control Node's Ubuntu shell:

```bash
sudo apt update
sudo apt install ansible openssh-client -y
ansible --version
```

### 3. Generate an SSH key on the Control Node

```bash
ssh-keygen -t ed25519
```

Accept the default path `/home/ubuntu/.ssh/id_ed25519` and leave the passphrase empty for this lab.

### 4. Configure SSH access from the Control Node to the Managed Node

EC2 uses the AWS key pair for initial SSH access. The Windows OpenSSH agent was enabled and forwarded to the Control Node session.

If needed, run these commands in PowerShell as Administrator to enable the agent service:

```powershell
Set-Service ssh-agent -StartupType Automatic
Start-Service ssh-agent
```

Then run this command in regular PowerShell:

```powershell
ssh-add "$env:USERPROFILE\Documents\Downloads\k8s-lab-key.pem"
```

Reconnect to the Control Node with agent forwarding enabled:

```powershell
ssh -A -i "$env:USERPROFILE\Documents\Downloads\k8s-lab-key.pem" ubuntu@54.91.107.214
```

An inbound SSH rule was added to the EC2 security group to allow port 22 traffic from the same security group. Then, on the Control Node, install its public key on the Managed Node:

```bash
ssh-copy-id -i ~/.ssh/id_ed25519.pub ubuntu@172.31.26.136
```

The command completed with `Number of key(s) added: 1`.

### 5. Create the inventory and verify connectivity

On the Control Node, create `inventory.ini`:

```bash
cat > inventory.ini <<'EOF'
[managed]
172.31.26.136 ansible_user=ubuntu
EOF
```

Test Ansible connectivity:

```bash
ansible managed -i inventory.ini -m ping
```

The expected result includes `SUCCESS` and `"ping": "pong"`.

### 6. Install Docker with an ad-hoc command

```bash
ansible managed -i inventory.ini -b -m apt -a "name=docker.io state=present update_cache=yes"
```

Options used:

- `managed`: the target inventory group.
- `-i inventory.ini`: the inventory file.
- `-b`: use sudo privileges on the managed host.
- `-m apt`: use Ansible's Ubuntu package-management module.
- `state=present`: ensure Docker is installed.

### 7. Verify the Docker installation

```bash
ansible managed -i inventory.ini -b -m command -a "docker --version"
```

The lab returned:

```text
172.31.26.136 | CHANGED | rc=0 >>
Docker version 29.1.3, build 29.1.3-0ubuntu4.1
```

## Screenshot evidence

Run these two commands on the Control Node, then take one screenshot showing both commands and their output:

```bash
ansible managed -i inventory.ini -m ping
ansible managed -i inventory.ini -b -m command -a "docker --version"
```

The screenshot should show `SUCCESS` and `"ping": "pong"`, followed by `rc=0` and the Docker version. This demonstrates that Ansible can reach the Managed Node and that Docker is installed.

