# Lab 4 Part 2 — Docker Image Optimization and Export/Load

This lab builds the sample Spring Boot app as an original Docker image and as an optimized multi-stage image. It then checks the app, confirms it runs as a non-root user, and practices saving and restoring the optimized image.

> Keep the existing `IVOLVE LABS/Docker/Lab4` folder unchanged. This lab uses the separate folder `IVOLVE LABS/Docker/lab 4 part 2`.

## 1. Prepare the project on the Ubuntu VM

From the repository root:

```bash
cd ~/ivolve-nti
git status
mkdir -p "IVOLVE LABS/Docker/lab 4 part 2"
```

Clone the sample application to a temporary directory and copy its source into the new lab folder:

```bash
git clone https://github.com/Ibrahim-Adel15/ImageOptimization-.git /tmp/ImageOptimization
LAB="$HOME/ivolve-nti/IVOLVE LABS/Docker/lab 4 part 2"
cp /tmp/ImageOptimization/Dockerfile "$LAB/Dockerfile.original"
cp /tmp/ImageOptimization/pom.xml "$LAB/"
cp -r /tmp/ImageOptimization/src "$LAB/"
cd "$LAB"
```

The Maven build creates `target/demo-0.0.1-SNAPSHOT.jar`.

## 2. Build the original image

The original Dockerfile uses Maven and Java 17 to build the application:

```bash
docker build -f Dockerfile.original -t image1 .
```

## 3. Create the optimized multi-stage Dockerfile

Create `Dockerfile.optimized`:

```bash
cat > Dockerfile.optimized <<'EOF'
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN useradd --create-home --shell /bin/bash appuser
COPY --from=builder --chown=appuser:appuser \
  /app/target/demo-0.0.1-SNAPSHOT.jar /app/app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EOF
```

The builder stage compiles the app. The runtime stage contains the Java runtime and packaged JAR, and runs as `appuser` rather than root.

## 4. Build and compare image sizes

```bash
docker build -f Dockerfile.optimized -t image2 .
docker images | grep -E 'REPOSITORY|image1|image2'
```

Record the size output from your VM. Image sizes vary with architecture, image versions, and Docker's displayed size metrics.

## 5. Run the optimized image and verify it

```bash
docker run -d --name container2 -p 8080:8080 image2
docker port container2
curl -s http://localhost:8080/; echo
docker exec container2 id
```

The HTTP response should be `Hello from Dockerized Spring Boot!`. The `id` command should show `appuser` (UID 1001 in this run), not root.

Stop and remove the container before deleting its image:

```bash
docker stop container2
docker rm container2
```

## 6. Export and restore the optimized image

```bash
docker save -o lab-image.tar image2:latest
docker image rm image2:latest
docker load -i lab-image.tar
docker images image2
```

Confirm Docker reports `Loaded image: image2:latest` and that `image2` appears in the image list. Remove the archive after verifying the restore so it is not committed:

```bash
rm lab-image.tar
```

## 7. Commit and push only the new lab folder

Return to the repository root and review the staged changes before committing:

```bash
cd ~/ivolve-nti
git status --short
git add -- "IVOLVE LABS/Docker/lab 4 part 2"
git status --short
git commit -m "Add Lab 4 Part 2"
git push origin main
```

The changes should be limited to the new `lab 4 part 2` folder. Do not stage or change the existing `Lab4` folder, and do not commit `lab-image.tar`.

## Evidence to keep

- Original and optimized image size output.
- Successful HTTP response from `curl`.
- `docker exec container2 id` output showing the non-root user.
- Successful `docker load` output and the restored `image2` entry.
<img width="862" height="158" alt="Screenshot 2026-10-24 211914" src="https://github.com/user-attachments/assets/c792679a-f672-4153-acc0-c6aab06fa31b" />
