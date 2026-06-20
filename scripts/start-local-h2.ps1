$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend\rule-engine-service"
$frontend = Join-Path $root "frontend\rule-engine-ui"

$javaVersionOutput = cmd /c "java -version 2>&1"
$javaVersion = ($javaVersionOutput | Select-String "version" | Select-Object -First 1).ToString()
Write-Host "Using Java: $javaVersion"

Write-Host "Starting backend with H2 profile..."
Push-Location $backend
& mvn.cmd @("-q", "-DskipTests", "-Djacoco.skip=true", "install")
Pop-Location

Start-Process -FilePath "mvn.cmd" `
  -ArgumentList @("-f","rule-engine-start\pom.xml","spring-boot:run","-Dspring-boot.run.profiles=h2") `
  -WorkingDirectory $backend `
  -WindowStyle Hidden

Write-Host "Starting frontend..."
Start-Process -FilePath "npm.cmd" `
  -ArgumentList "run","dev","--","--port","5173" `
  -WorkingDirectory $frontend `
  -WindowStyle Hidden

Write-Host "Waiting for backend..."
for ($i = 0; $i -lt 60; $i++) {
  try {
    $status = (Invoke-WebRequest -UseBasicParsing "http://localhost:8080/rule-engine/actuator/health" -TimeoutSec 2).StatusCode
    if ($status -eq 200) { break }
  } catch {
    Start-Sleep -Seconds 2
  }
}

Write-Host "Backend:  http://localhost:8080/rule-engine"
Write-Host "Swagger:  http://localhost:8080/rule-engine/swagger-ui.html"
Write-Host "H2:       http://localhost:8080/rule-engine/h2-console"
Write-Host "H2 JDBC:  jdbc:h2:file:./data/rule_engine"
Write-Host "H2 User:  sa"
Write-Host "H2 Pass:  <empty>"
Write-Host "Frontend: http://localhost:5173"
