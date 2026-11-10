Lab 26: Structured Configuration Management with Ansible Roles

## Objective

Organize the Lab 25 configuration into three reusable Ansible roles—Docker, kubectl, and Jenkins—and run them from one playbook against the Managed Node.

## Environment

| Role | EC2 instance | Address |
|---|---|---|
| Control Node | `k8s-control-pl...` | `54.91.107.214` (public IP) |
| Managed Node | `k8s-worker` | `172.31.26.136` (private IP) |

Both instances run Ubuntu and use the `ubuntu` account. The inventory targets the worker's private IP.

## Project structure

```text
lab26-ansible-roles/
├── inventory.ini
├── site.yml
└── roles/
    ├── docker/tasks/main.yml
    ├── kubectl/defaults/main.yml
    ├── kubectl/tasks/main.yml
    └── jenkins/tasks/main.yml
```

## Setup and execution

Place this project directory on the Control Node and run:

```bash
cd ~/lab26-ansible-roles
ansible-playbook -i inventory.ini site.yml --syntax-check
ansible-playbook -i inventory.ini site.yml
```

The successful run completed all Docker, kubectl, and Jenkins role tasks with `failed=0` and `unreachable=0`. It reported `changed=0` because Lab 25 had already installed and started the software and services.

## Verify the Managed Node

From the project directory on the Control Node, run:

```bash
ansible managed -i inventory.ini -b -m shell -a 'docker --version && kubectl version --client && java -version 2>&1 && systemctl is-active docker jenkins'
```

The expected output includes Docker `29.1.3`, kubectl client `v1.37.1`, Java `21.0.13`, and `active` for Docker and Jenkins.

## Screenshot evidence

For proof that the roles ran, take a screenshot of:

```bash
ansible-playbook -i inventory.ini site.yml
```

<img width="950" height="1079" alt="Screenshot 2026-11-10 185918" src="https://github.com/user-attachments/assets/c8a834b6-6d44-43f0-be06-57659ca98677" />
