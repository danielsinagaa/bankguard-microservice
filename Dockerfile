FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE

WORKDIR /app
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
