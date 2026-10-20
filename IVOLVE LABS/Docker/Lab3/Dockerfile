FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

RUN useradd --create-home --shell /bin/bash appuser \
    && chown appuser:appuser /app

COPY --chown=appuser:appuser pom.xml .
COPY --chown=appuser:appuser src ./src

USER appuser

RUN mvn -B package

EXPOSE 8080

CMD ["java", "-jar", "target/calculator.jar"]
