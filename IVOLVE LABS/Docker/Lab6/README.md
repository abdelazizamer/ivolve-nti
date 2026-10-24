# Docker Lab 6: Environment Variables and Resource Management

## Objective

Build and run a small Flask application in Docker, configure it with environment variables, set container CPU and memory limits, and inspect resource usage.

## Application

The Flask app listens on `0.0.0.0:5000` and provides:

- `/` — returns the application name, environment, and a welcome message as JSON.
- `/health` — returns the health status as JSON.

The app reads `APP_NAME` and `APP_ENV` from the container environment. The project files are `app.py`, `requirements.txt`, and `Dockerfile`.

## 1. Build the initial image

Run from the `Lab6` directory:

```bash
docker build -t ivolve1 .
docker image ls ivolve1
```

## 2. Run with explicit environment variables and resource limits

```bash
docker run -d --name container1 \
  -p 5000:5000 --cpus=1 --memory=256m \
  -e APP_NAME=iVolveApp1 -e APP_ENV=stg \
  ivolve1
```

Verify the values, endpoints, and resource usage:

```bash
docker exec container1 printenv APP_NAME
docker exec container1 printenv APP_ENV
curl http://localhost:5000/
curl http://localhost:5000/health
docker stats container1
docker inspect -f 'NanoCpus={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}} bytes' container1
```

The observed values were `iVolveApp1` and `stg`; `/health` returned `healthy`. The limits were one CPU and 256 MiB of memory. One resource snapshot showed 21.41 MiB used out of 256 MiB.

Stop and remove the container before reusing host port 5000:

```bash
docker stop container1
docker rm container1
```

## 3. Run with an environment file

The harmless `env.txt` file contains:

```text
APP_NAME=iVolveApp2
APP_ENV=prod
```

Start the second container:

```bash
docker run -d --name container2 \
  -p 5000:5000 --cpus=1 --memory=256m \
  --env-file env.txt ivolve1
```

Verify the values and application response:

```bash
docker exec container2 printenv APP_NAME
docker exec container2 printenv APP_ENV
curl http://localhost:5000/
```

The observed values were `iVolveApp2` and `prod`.

```bash
docker stop container2
docker rm container2
```

## 4. Set default environment values in the Dockerfile

The Dockerfile defines these image defaults:

```dockerfile
ENV APP_NAME=iVolveApp3
ENV APP_ENV=dev
```

Build a separately tagged image and run it without passing environment variables:

```bash
docker build -t ivolve3 .
docker run -d --name container3 -p 5000:5000 ivolve3
```

Verify the defaults and endpoints:

```bash
docker exec container3 printenv APP_NAME
docker exec container3 printenv APP_ENV
curl http://localhost:5000/
curl http://localhost:5000/health
```

The observed defaults were `iVolveApp3` and `dev`; `/health` returned `healthy`.

Stop and remove the container after verification:

```bash
docker stop container3
docker rm container3
```

## 5. Commit and push the Lab6 folder

From the repository root, stage only this lab, review the changes, commit, synchronize with the remote branch, and push:

```bash
cd ~/ivolve-nti
git status --short
git add -- "IVOLVE LABS/Docker/Lab6"
git status --short
git commit -m "Add Docker Lab 6 environment configuration"
git pull --rebase origin main
git push origin main
```

Keep `env.txt` limited to harmless lab values; do not put real credentials in it.

<img width="893" height="109" alt="Screenshot 2026-10-24 224616" src="https://github.com/user-attachments/assets/f8dd8a38-adaf-4810-8386-718244157754" />


<img width="1068" height="98" alt="Screenshot 2026-10-24 224625" src="https://github.com/user-attachments/assets/0d9168a9-c5f9-439f-9b6d-e972ae9b74d7" />
