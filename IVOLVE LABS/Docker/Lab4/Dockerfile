FROM eclipse-temurin:17-jre-noble

WORKDIR /app

RUN useradd --create-home --shell /bin/bash appuser \
    && chown appuser:appuser /app

COPY --chown=appuser:appuser target/calculator.jar /app/calculator.jar

USER appuser

EXPOSE 8080

CMD ["java", "-jar", "/app/calculator.jar"]
