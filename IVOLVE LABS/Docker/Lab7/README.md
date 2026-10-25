Lab 7: Docker Microservices, Networks, Volumes, and Bind Mounts

These steps use the existing `MicroserviceApp` source and keep the lab under `IVOLVE LABS/Docker/Lab7`.

## 1. Copy the application into the repository

From the repository root on the Ubuntu VM:

```bash
cd ~/ivolve-nti
mkdir -p "IVOLVE LABS/Docker/Lab7"
git clone https://github.com/Ibrahim-Adel15/MicroserviceApp.git /tmp/MicroserviceApp

LAB="$HOME/ivolve-nti/IVOLVE LABS/Docker/Lab7"
cp -r /tmp/MicroserviceApp/frontend \
      /tmp/MicroserviceApp/backend \
      /tmp/MicroserviceApp/notification \
      "$LAB/"
cd "$LAB"
find . -maxdepth 2 -type f -print
```

The three services are `frontend/`, `backend/`, and `notification/`.

## 2. Build the three service images

Run from the `Lab7` directory:

```bash
docker build -t lab7-frontend ./frontend
docker build -t lab7-backend ./backend
docker build -t lab7-notification ./notification
docker images | grep -E 'REPOSITORY|lab7-frontend|lab7-backend|lab7-notification'
```

## 3. Create the networks, volumes, and host data directory

```bash
docker network create --driver bridge --subnet 172.28.0.0/16 frontend-backend
docker network create --driver bridge --subnet 172.29.0.0/16 backend-notification

docker volume create backend-logs
docker volume create notification-logs
mkdir -p backend-data
```

If Docker reports a subnet overlap, inspect existing networks and choose unused private subnets.

## 4. Start the notification service

```bash
docker run -d --name notification \
  --network backend-notification \
  -v notification-logs:/app/logs \
  lab7-notification
```

## 5. Start the backend and attach it to both networks

The backend uses a named volume for logs and a bind mount for message data:

```bash
docker run -d --name backend \
  --network backend-notification \
  -e NOTIFICATION_URL=http://notification:5001/notify \
  -v backend-logs:/app/logs \
  --mount type=bind,source="$(pwd)/backend-data",target=/app/data \
  lab7-backend

docker network connect frontend-backend backend
```

## 6. Start the frontend

```bash
docker run -d --name frontend \
  --network frontend-backend \
  -e BACKEND_URL=http://backend:5000 \
  -p 3000:3000 \
  lab7-frontend

docker ps
```

Open `http://localhost:3000` in the VM browser. Create a message and confirm it appears in the frontend.

## 7. Verify data, logs, networks, and mounts

```bash
cat backend-data/messages.json
docker exec backend tail -n 20 /app/logs/backend.log
docker exec notification tail -n 20 /app/logs/notification.log

docker inspect backend \
  --format '{{range .Mounts}}{{println .Type .Name .Source "->" .Destination}}{{end}}'

docker network inspect frontend-backend \
  --format '{{range .Containers}}{{println .Name}}{{end}}'
docker network inspect backend-notification \
  --format '{{range .Containers}}{{println .Name}}{{end}}'

docker volume ls | grep -E 'backend-logs|notification-logs'
```

Expected network membership:

- `frontend-backend`: `frontend` and `backend`
- `backend-notification`: `backend` and `notification`

Expected mounts on `backend`:

- named volume `backend-logs` mounted at `/app/logs`
- host directory `backend-data` mounted at `/app/data`

## 8. Stop the containers after verification

```bash
docker stop frontend backend notification
docker rm frontend backend notification
```

The named volumes and host `backend-data/` directory remain available. Remove them only when you no longer need the saved logs or messages.

## 9. Push only the service source folders to GitHub

From the repository root, stage the three service folders (not local `backend-data/`):

```bash
cd ~/ivolve-nti
git status --short
git add -- \
  "IVOLVE LABS/Docker/Lab7/frontend" \
  "IVOLVE LABS/Docker/Lab7/backend" \
  "IVOLVE LABS/Docker/Lab7/notification"
git status --short
git commit -m "Add Docker Lab 7 microservices"
git pull --rebase origin main
git push origin main
```
<img width="1039" height="923" alt="Screenshot 2026-10-25 194240" src="https://github.com/user-attachments/assets/daa2579b-607d-4ff0-8a53-7418b8c2ead3" />


<img width="1093" height="982" alt="Screenshot 2026-10-25 193859" src="https://github.com/user-attachments/assets/c24b14b2-558e-41c2-a723-7972e165af60" />
