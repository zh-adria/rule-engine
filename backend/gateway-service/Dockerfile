FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/gateway-service ./backend/gateway-service
WORKDIR /workspace/backend/gateway-service
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/backend/gateway-service/target/gateway-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
