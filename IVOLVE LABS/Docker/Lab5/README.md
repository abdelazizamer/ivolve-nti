# Docker Lab 5: Image Vulnerability Scanning with Trivy

This lab builds the supplied Node.js application as a Docker image, scans it with Trivy, filters HIGH and CRITICAL findings, and saves an HTML report.

## Application

- Source: [TrivyImage](https://github.com/Ibrahim-Adel15/TrivyImage)
- Docker image tag: `trivy-lab:v1`
- Trivy version used: `0.76.1`
- Scan date: `2026-10-24`

## Build the image

Run these commands from the `Lab5` directory:

```bash
docker build -t trivy-lab:v1 .
docker image ls trivy-lab
```

## Install Trivy on Ubuntu

The official APT repository installation commands are:

```bash
sudo apt-get update
sudo apt-get install -y wget gnupg

wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key \
  | gpg --dearmor \
  | sudo tee /usr/share/keyrings/trivy.gpg >/dev/null

echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" \
  | sudo tee /etc/apt/sources.list.d/trivy.list

sudo apt-get update
sudo apt-get install -y trivy
trivy --version
```

## Scan and save results

Run from the `Lab5` directory:

```bash
mkdir -p results
trivy image trivy-lab:v1 | tee results/trivy-full-scan.txt
trivy image --severity HIGH,CRITICAL trivy-lab:v1 \
  | tee results/trivy-high-critical.txt
```

The HTML template is kept at `contrib/html.tpl`. Generate the report with:

```bash
trivy image --severity HIGH,CRITICAL \
  --format template \
  --template "@contrib/html.tpl" \
  --output results/trivy-report.html \
  trivy-lab:v1
```

The template was downloaded from the official Trivy repository at the matching release tag:

```bash
mkdir -p contrib
wget -qO contrib/html.tpl \
  https://raw.githubusercontent.com/aquasecurity/trivy/v0.76.1/contrib/html.tpl
```

## Findings and notes

The scan reported vulnerabilities in the image. Trivy also warned that the detected Debian 11.11 release is no longer supported, so vulnerability detection may be incomplete. Preserve scanner output as generated; do not edit reports to remove findings. No remediation is documented here.

## Evidence

- `results/trivy-full-scan.txt` — full scan output
- `results/trivy-high-critical.txt` — HIGH and CRITICAL findings
- `results/trivy-report.html` — HTML report
- `results/trivy-version.txt` — Trivy version output (save with `trivy --version | tee results/trivy-version.txt`)
- `contrib/html.tpl` — HTML report template

Review the output files before committing. Add only the Lab5 folder to Git, and keep unrelated files and credentials out of the commit.

