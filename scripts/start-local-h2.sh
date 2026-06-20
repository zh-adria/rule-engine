#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT/backend/rule-engine-service"
mvn -q -DskipTests -Djacoco.skip=true install
mvn -f rule-engine-start/pom.xml spring-boot:run -Dspring-boot.run.profiles=h2 > "$ROOT/backend-h2.log" 2>&1 &

cd "$ROOT/frontend/rule-engine-ui"
npm run dev -- --port 5173 > "$ROOT/frontend.log" 2>&1 &

echo "Waiting for backend..."
for i in {1..60}; do
  if curl -fsS http://localhost:8080/rule-engine/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Backend:  http://localhost:8080/rule-engine"
echo "Swagger:  http://localhost:8080/rule-engine/swagger-ui.html"
echo "H2:       http://localhost:8080/rule-engine/h2-console"
echo "H2 JDBC:  jdbc:h2:file:./data/rule_engine"
echo "H2 User:  sa"
echo "H2 Pass:  <empty>"
echo "Frontend: http://localhost:5173"
