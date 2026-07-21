# ============================================================
# rule-engine 本地开发一键启动脚本 (MySQL)
#
# 数据源配置使用 ENC(...)，启动时输入配置解密密钥。
#
# 启动顺序: approval(8082) → rule-engine(8080) → ui(5173)
# 可选:     gateway(9000, validates Sa-Token JWT)
#
# 用法:
#   .\scripts\start-dev.ps1            # 启动全部
#   .\scripts\start-dev.ps1 -SkipUI    # 仅启动后端
#   .\scripts\start-dev.ps1 -Stop      # 停止全部
# ============================================================

param(
    [switch]$SkipUI,
    [switch]$Stop,
    [switch]$Gateway
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Split-Path -Parent $ScriptDir
$Backend = Join-Path $Root 'backend'
$Frontend = Join-Path $Root 'frontend\rule-engine-ui'
. (Join-Path $ScriptDir 'db-check.ps1')

# ---- Colors ----
$GREEN  = "`e[32m"
$YELLOW = "`e[33m"
$RED    = "`e[31m"
$CYAN   = "`e[36m"
$RESET  = "`e[0m"

function Info($msg) { Write-Host "${CYAN}[INFO]${RESET} $msg" }
function Ok($msg)   { Write-Host "${GREEN}[OK]${RESET}   $msg" }
function Warn($msg) { Write-Host "${YELLOW}[WARN]${RESET} $msg" }
function Fail($msg) { Write-Host "${RED}[FAIL]${RESET} $msg" }

function Wait-Http {
    param(
        [string]$Name,
        [string]$Url,
        [int]$MaxTry = 60
    )

    Info "Waiting for $Name..."
    for ($i = 0; $i -lt $MaxTry; $i++) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) {
                Ok "$Name health check passed"
                return
            }
        } catch {}
    }
    throw "$Name health check timed out"
}

# ---- PID tracking ----
$PidFile = Join-Path $Root '.dev-pids.json'

function Save-Pids {
    $pids = @{}
    if (Test-Path $PidFile) { $pids = Get-Content $PidFile | ConvertFrom-Json }
    $pids | Add-Member -NotePropertyName 'timestamp' -NotePropertyValue (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') -Force
    $pids | ConvertTo-Json -Depth 5 | Set-Content $PidFile
}

function Load-Pids {
    if (Test-Path $PidFile) {
        return Get-Content $PidFile | ConvertFrom-Json
    }
    return $null
}

# ---- Stop all ----
function Stop-All {
    Info "Stopping all services..."
    $pids = Load-Pids
    if (-not $pids) { Warn "No PID file found."; return }

    $services = @('approval', 'ruleEngine', 'gateway', 'ui')
    foreach ($svc in $services) {
        if ($pids.$svc) {
            $pid = $pids.$svc
            if (Get-Process -Id $pid -ErrorAction SilentlyContinue) {
                Info "Stopping $svc (PID $pid)..."
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                Ok "$svc stopped"
            } else {
                Warn "$svc (PID $pid) already stopped"
            }
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Ok "All services stopped"
    exit 0
}

if ($Stop) { Stop-All }

# ---- Check tools ----
Info "Checking prerequisites..."

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) { Fail "Maven not found. Please install Maven 3.8+"; exit 1 }
Ok "Maven: $($mvn.Source)"

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { Fail "Java not found. Please install JDK 21+"; exit 1 }
$javaLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($javaLine -notmatch '"(?<major>\d+)(\.(?<minor>\d+))?') {
    Fail "Cannot detect Java version: $javaLine"
    exit 1
}
$javaMajor = [int]$Matches.major
if ($javaMajor -eq 1) { $javaMajor = [int]$Matches.minor }
if ($javaMajor -lt 21) {
    Fail "JDK 21+ is required. Current Java is: $javaLine"
    exit 1
}
Ok "Java: $javaLine"

$node = Get-Command node -ErrorAction SilentlyContinue
$npm = Get-Command npm -ErrorAction SilentlyContinue
if (-not $SkipUI -and (-not $node -or -not $npm)) {
    Warn "Node.js not found, skipping frontend. Install Node.js 18+ for UI."
    $SkipUI = $true
}
if ($node) { Ok "Node.js: $(node --version)" }
if ($npm)  { Ok "npm: $(npm --version)" }

if (-not $env:SA_TOKEN_JWT_SECRET_KEY) { $env:SA_TOKEN_JWT_SECRET_KEY = "rule-engine-sa-token-jwt-secret-2026" }
if (-not $env:RULE_ENGINE_ADMIN_USERNAME) { $env:RULE_ENGINE_ADMIN_USERNAME = "admin" }
if (-not $env:RULE_ENGINE_ADMIN_PASSWORD) { $env:RULE_ENGINE_ADMIN_PASSWORD = "admin123" }
if (-not $env:RULE_ENGINE_TESTER_USERNAME) { $env:RULE_ENGINE_TESTER_USERNAME = "underwriter" }
if (-not $env:RULE_ENGINE_TESTER_PASSWORD) { $env:RULE_ENGINE_TESTER_PASSWORD = "underwriter123" }
if (-not $env:RULE_ENGINE_AES_KEY) { $env:RULE_ENGINE_AES_KEY = "0123456789abcdef0123456789abcdef" }
$jasyptPwd = Get-ConfigDecryptKey

if (-not [Environment]::GetEnvironmentVariable('APPROVAL_CALLBACK_SECRET')) {
    $env:APPROVAL_CALLBACK_SECRET = "rule-engine-local-callback-secret"
    Warn "APPROVAL_CALLBACK_SECRET not set; using local development default."
}

try {
    Test-DatabaseConnection
    Reset-ProjectDatabase -IncludeApproval
} catch {
    Fail $_.Exception.Message
    exit 1
}

# ---- Clean old PIDs ----
$pids = Load-Pids
if ($pids) {
    $services = @('approval', 'ruleEngine', 'gateway', 'ui')
    foreach ($svc in $services) {
        if ($pids.$svc) {
            $pid = $pids.$svc
            if (-not (Get-Process -Id $pid -ErrorAction SilentlyContinue)) {
                $pids.PSObject.Properties.Remove($svc)
            }
        }
    }
    $pids | ConvertTo-Json -Depth 5 | Set-Content $PidFile
}

# ---- Helper: start service ----
function Start-Service {
    param(
        [string]$Name,
        [string]$WorkingDir,
        [string]$Command,
        [string]$LogFile
    )

    $logDir = Split-Path -Parent $LogFile
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

    Info "Starting $Name..."
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    $psi.Arguments = "/c $Command"
    $psi.WorkingDirectory = $WorkingDir
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    $proc.Start() | Out-Null

    # Capture output
    $stdOut = $proc.StandardOutput.ReadToEndAsync()
    $stdErr = $proc.StandardError.ReadToEndAsync()

    $pid = $proc.Id
    Ok "$Name started (PID $pid) → $LogFile"

    # Async logging
    Start-Job -ScriptBlock {
        param($proc, $logFile)
        $output = $proc.StandardOutput.ReadToEnd()
        Add-Content -Path $logFile -Value $output -Encoding UTF8
    } -ArgumentList $proc, $LogFile | Out-Null

    Start-Job -ScriptBlock {
        param($proc, $logFile)
        $err = $proc.StandardError.ReadToEnd()
        if ($err) {
            Add-Content -Path $logFile -Value "`n[STDERR]`n$err" -Encoding UTF8
        }
    } -ArgumentList $proc, $LogFile | Out-Null

    return $pid
}

# ---- Start services ----
$allPids = @{}

# Approval flow service
$approvalPid = Start-Service `
    -Name 'approval-flow-service' `
    -WorkingDir (Join-Path $Backend 'approval-flow-service\approval-start') `
    -Command 'mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"' `
    -LogFile (Join-Path $Root 'logs\approval.log')
$allPids.approval = $approvalPid

# Rule engine service
$rePid = Start-Service `
    -Name 'rule-engine-service' `
    -WorkingDir (Join-Path $Backend 'rule-engine-service\rule-engine-start') `
    -Command 'mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"' `
    -LogFile (Join-Path $Root 'logs\rule-engine.log')
$allPids.ruleEngine = $rePid

# Gateway (optional)
if ($Gateway) {
    $gwPid = Start-Service `
        -Name 'gateway-service' `
        -WorkingDir (Join-Path $Backend 'gateway-service') `
        -Command 'mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9000"' `
        -LogFile (Join-Path $Root 'logs\gateway.log')
    $allPids.gateway = $gwPid
}

# Frontend UI
if (-not $SkipUI) {
    Start-Sleep -Seconds 2
    $uiLog = Join-Path $Root 'logs\ui.log'
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    $psi.Arguments = '/c npm run dev'
    $psi.WorkingDirectory = $Frontend
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $uiProc = New-Object System.Diagnostics.Process
    $uiProc.StartInfo = $psi
    $uiProc.Start() | Out-Null

    # Async logging for UI
    Start-Job -ScriptBlock {
        param($proc, $logFile)
        $output = $proc.StandardOutput.ReadToEnd()
        Add-Content -Path $logFile -Value $output -Encoding UTF8
    } -ArgumentList $uiProc, $uiLog | Out-Null

    Start-Job -ScriptBlock {
        param($proc, $logFile)
        $err = $proc.StandardError.ReadToEnd()
        if ($err) {
            Add-Content -Path $logFile -Value "`n[STDERR]`n$err" -Encoding UTF8
        }
    } -ArgumentList $uiProc, $uiLog | Out-Null

    $allPids.ui = $uiProc.Id
    Ok "Frontend UI started (PID $($uiProc.Id)) → $uiLog"
}

# ---- Save PIDs ----
$allPids | Add-Member -NotePropertyName 'timestamp' -NotePropertyValue (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') -Force
$allPids | ConvertTo-Json -Depth 5 | Set-Content $PidFile

try {
    Wait-Http -Name 'approval-flow-service' -Url 'http://localhost:8082/approval/actuator/health' -MaxTry 60
    Wait-Http -Name 'rule-engine-service' -Url 'http://localhost:8080/rule-engine/actuator/health' -MaxTry 60
    Test-DatabaseState -IncludeApproval
} catch {
    Fail $_.Exception.Message
    Write-Host "  ${YELLOW}Stop:${RESET} .\scripts\start-dev.ps1 -Stop"
    exit 1
}

# ---- Summary ----
Write-Host ""
Write-Host "${GREEN}========================================${RESET}"
Write-Host "${GREEN}  All services started successfully!${RESET}"
Write-Host "${GREEN}========================================${RESET}"
Write-Host ""
Write-Host "  ${CYAN}Service          Port   PID${RESET}"
Write-Host "  ${CYAN}-------          ----   ---${RESET}"
Write-Host "  approval-service 8082   $approvalPid"
Write-Host "  rule-engine      8080   $rePid"
if ($Gateway) { Write-Host "  gateway          9000   $($allPids.gateway)" }
if (-not $SkipUI) { Write-Host "  frontend         5173   $($allPids.ui)" }
Write-Host ""
Write-Host "  ${CYAN}API Docs:${RESET}"
Write-Host "    approval:     http://localhost:8082/approval/swagger-ui.html"
Write-Host "    rule-engine:  http://localhost:8080/rule-engine/swagger-ui.html"
if ($Gateway) { Write-Host "    gateway:      http://localhost:9000" }
if (-not $SkipUI) { Write-Host "    frontend:     http://localhost:5173" }
Write-Host ""
Write-Host "  ${CYAN}Logs:${RESET}"
Write-Host "    $(Join-Path $Root 'logs')"
Write-Host ""
Write-Host "  ${YELLOW}Stop:${RESET} .\scripts\start-dev.ps1 -Stop"
Write-Host ""
