# Jenkins Lab 22 — CI/CD with Shared Library and Jenkins Agent

This lab builds, tests, scans, publishes, and deploys the iVolve Node.js application using Jenkins.

## Project Layout

- `agent/Dockerfile`: Jenkins agent image with the required tools.
- `vars/BuildApp.groovy`: installs dependencies, runs tests, and checks syntax.
- `vars/BuildImage.groovy`: builds, scans, pushes, and cleans the Docker image.
- `vars/DeployOnK8s.groovy`: deploys the application to Kubernetes.
- `k8s/`: Kubernetes manifests.
- `test/`: application tests.

## Pipeline Flow

Build App → Build Image, Scan and Push → Deploy to Kubernetes

## Jenkins Requirements

- Global shared library name: `ivolve-lab22`
- Default branch: `main`
- Library path: `Lab22`
- Agent label: `lab22-agent`
- Docker credential ID: `docker`
- Kubernetes kubeconfig credential ID: `k8s-kubeconfig`
