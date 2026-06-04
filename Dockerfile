FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

ARG MODULE

COPY pom.xml .
COPY common-library/pom.xml common-library/pom.xml
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY risk-engine-service/pom.xml risk-engine-service/pom.xml
COPY audit-search-service/pom.xml audit-search-service/pom.xml
COPY master-data-service/pom.xml master-data-service/pom.xml
COPY . .

RUN mvn -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG MODULE

WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
