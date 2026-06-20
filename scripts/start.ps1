$ErrorActionPreference = "Stop"

Write-Host "Starting insurance rule engine platform..."
docker compose -f deploy/docker-compose.yml up --build -d

Write-Host "Backend: http://localhost:8080/rule-engine/actuator/health"
Write-Host "Swagger: http://localhost:8080/rule-engine/swagger-ui.html"
Write-Host "Frontend: http://localhost:5173"

