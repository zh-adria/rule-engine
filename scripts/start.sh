#!/usr/bin/env bash
set -euo pipefail

echo "Starting insurance rule engine platform..."
docker compose -f deploy/docker-compose.yml up --build -d

echo "Backend: http://localhost:8080/rule-engine/actuator/health"
echo "Swagger: http://localhost:8080/rule-engine/swagger-ui.html"
echo "Frontend: http://localhost:5173"

