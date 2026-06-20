#!/usr/bin/env bash
set -euo pipefail

echo "Starting H2 local stack with Docker Java 11 runtime..."
docker compose -f deploy/docker-compose-h2.yml up --build -d

echo "Backend:  http://localhost:8080/rule-engine"
echo "Swagger:  http://localhost:8080/rule-engine/swagger-ui.html"
echo "H2:       http://localhost:8080/rule-engine/h2-console"
echo "H2 JDBC:  jdbc:h2:file:./data/rule_engine"
echo "H2 User:  sa"
echo "H2 Pass:  <empty>"
echo "Frontend: http://localhost:5173"

