# Lab 25: Automated Web Server Configuration Using Ansible Playbooks

## Objective

Use an Ansible playbook on the Control Node to install Docker, `kubectl`, and Jenkins on the Managed Node, start Docker and Jenkins, and verify the configuration remotely.

## Environment

| Role | EC2 instance | Address |
|---|---|---|
| Control Node | `k8s-control-pl...` | `54.91.107.214` (public IP) |
| Managed Node | `k8s-worker` | `172.31.26.136` (private IP) |

Both instances run Ubuntu and use the `ubuntu` account. The inventory created in Lab 24 is reused:

```ini
[managed]
172.31.26.136 ansible_user=ubuntu
```

## 1. Create the playbook

On the Control Node, create `lab25.yml`:

```bash
cat > lab25.yml <<'EOF'
---
- name: Configure Docker, kubectl, and Jenkins
  hosts: managed
  become: true

  vars:
    kubernetes_repo_version: "v1.37"

  tasks:
    - name: Install repository prerequisites and Java 21
      ansible.builtin.apt:
        name:
          - ca-certificates
          - curl
          - gnupg
          - python3-apt
          - fontconfig
          - openjdk-21-jre
        state: present
        update_cache: true

    - name: Ensure apt keyrings directory exists
      ansible.builtin.file:
        path: /etc/apt/keyrings
        state: directory
        mode: "0755"

    - name: Add Jenkins LTS signing key
      ansible.builtin.get_url:
        url: https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key
        dest: /etc/apt/keyrings/jenkins.asc
        mode: "0644"

    - name: Add Jenkins LTS apt repository
      ansible.builtin.apt_repository:
        repo: "deb [signed-by=/etc/apt/keyrings/jenkins.asc] https://pkg.jenkins.io/debian-stable binary/"
        filename: jenkins
        state: present
        update_cache: true

    - name: Add Kubernetes signing key
      ansible.builtin.get_url:
        url: "https://pkgs.k8s.io/core:/stable:/{{ kubernetes_repo_version }}/deb/Release.key"
        dest: /etc/apt/keyrings/kubernetes-apt-keyring.asc
        mode: "0644"

    - name: Add Kubernetes apt repository
      ansible.builtin.apt_repository:
        repo: "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.asc] https://pkgs.k8s.io/core:/stable:/{{ kubernetes_repo_version }}/deb/ /"
        filename: kubernetes
        state: present
        update_cache: true

    - name: Install Docker, kubectl, and Jenkins
      ansible.builtin.apt:
        name:
          - docker.io
          - kubectl
          - jenkins
        state: present
        update_cache: true

    - name: Enable and start Docker
      ansible.builtin.systemd_service:
        name: docker
        enabled: true
        state: started
        daemon_reload: true

    - name: Enable and start Jenkins
      ansible.builtin.systemd_service:
        name: jenkins
        enabled: true
        state: started
        daemon_reload: true
EOF
```

Jenkins requires Java 21 or later; the playbook installs Java before Jenkins. The Kubernetes repository uses the stable `v1.37` package channel.

## 2. Check syntax and run the playbook

```bash
ansible-playbook -i inventory.ini lab25.yml --syntax-check
ansible-playbook -i inventory.ini lab25.yml
```

The successful run ended with a recap similar to:

```text
172.31.26.136 : ok=10 changed=1 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

## 3. Resolve the existing Kubernetes repository conflict (if it occurs)

The worker already had a Kubernetes apt source using a `.gpg` keyring. The playbook added the same repository using an `.asc` keyring, and apt reported conflicting `Signed-By` values. Remove the old `.gpg` source line with Ansible, then rerun the playbook:

```bash
ansible managed -i inventory.ini -b -m lineinfile -a "path=/etc/apt/sources.list.d/kubernetes.list regexp='signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg' state=absent"
ansible-playbook -i inventory.ini lab25.yml
```

## 4. Verify the Managed Node

Run this from the Control Node:

```bash
ansible managed -i inventory.ini -b -m shell -a 'docker --version && kubectl version --client && java -version 2>&1 && systemctl is-active docker jenkins'
```

The successful output showed Docker `29.1.3`, kubectl client `v1.37.1`, Java `21.0.13`, and `active` for both Docker and Jenkins.
<img width="946" height="386" alt="Screenshot 2026-11-10 171107" src="https://github.com/user-attachments/assets/0365c02c-5c8f-41da-956d-0612ee4b7cf0" />


