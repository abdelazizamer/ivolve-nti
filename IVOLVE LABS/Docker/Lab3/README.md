# Lab 3: Docker Part 1 - Build Inside Docker

## Steps to Run

1. Copy the Maven project files into the Lab3 directory:
   ```bash
   LAB3="$HOME/NTI-IVOLVE/IVOLVE LABS/Docker/Lab3"
   mkdir -p "$LAB3"
   cp "$HOME/Ivolve-lab1/CalculatorMavenApp/pom.xml" "$LAB3/"
   cp -r "$HOME/Ivolve-lab1/CalculatorMavenApp/src" "$LAB3/"
   cd "$LAB3"
   ```

2. Build the Docker image using the Dockerfile:
   ```bash
   docker build -t app1 .
   ```

3. Run the container:
   ```bash
   docker run --name container1 -it -p 8080:8080 app1
   ```

4. Clean up the container after testing:
   ```bash
   docker rm -f container1
   ```
<img width="1224" height="273" alt="Screenshot 2026-10-21 041701" src="https://github.com/user-attachments/assets/d9d84795-7428-4f15-90cb-039ecdb821f6" />
