# Lab 4: Docker Part 2 - Build Locally (Size Comparison)

## Steps Executed

1. **Build the JAR locally using Maven and copy to Lab4:**
   ```bash
   cd ~/Ivolve-lab1/CalculatorMavenApp
   mvn clean package

   LAB4="$HOME/NTI-IVOLVE/IVOLVE LABS/Docker/Lab4"
   mkdir -p "$LAB4/target"
   cp target/calculator-1.0-SNAPSHOT.jar "$LAB4/target/calculator.jar"
   cd "$LAB4"
   ```

2. **Create Dockerfile:**
   ```dockerfile
   FROM eclipse-temurin:17-jre-noble
   WORKDIR /app
   RUN useradd --create-home --shell /bin/bash appuser \
       && chown appuser:appuser /app
   COPY --chown=appuser:appuser target/calculator.jar /app/calculator.jar
   USER appuser
   EXPOSE 8080
   CMD ["java", "-jar", "/app/calculator.jar"]
   ```

3. **Build local Docker image (`app2`):**
   ```bash
   docker build -t app2 .
   ```

4. **Compare Image Sizes (`app1` vs `app2`):**
   ```bash
   docker images | grep -E "REPOSITORY|app1|app2"
   ```

5. **Run Container:**
   ```bash
   docker run --name container2 -it -p 8080:8080 app2
   ```

6. **Cleanup:**
   ```bash
   docker rm -f container2
   ```

