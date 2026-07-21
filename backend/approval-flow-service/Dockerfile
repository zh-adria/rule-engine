FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/approval-flow-service ./backend/approval-flow-service
WORKDIR /workspace/backend/approval-flow-service
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/backend/approval-flow-service/approval-start/target/approval-start-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
