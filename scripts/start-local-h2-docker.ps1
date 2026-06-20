$ErrorActionPreference = "Stop"

Write-Host "Starting H2 local stack with Docker Java 11 runtime..."
docker compose -f deploy/docker-compose-h2.yml up --build -d

Write-Host "Backend:  http://localhost:8080/rule-engine"
Write-Host "Swagger:  http://localhost:8080/rule-engine/swagger-ui.html"
Write-Host "H2:       http://localhost:8080/rule-engine/h2-console"
Write-Host "H2 JDBC:  jdbc:h2:file:./data/rule_engine"
Write-Host "H2 User:  sa"
Write-Host "H2 Pass:  <empty>"
Write-Host "Frontend: http://localhost:5173"

