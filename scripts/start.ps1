<#
  rule-engine 一键启动器 (PowerShell)
  用法: .\scripts\start.ps1
#>

param([string]$Choice)

$ROOT = Split-Path -Parent $PSScriptRoot
$BACKEND = Join-Path $ROOT "backend"
$FRONTEND = Join-Path $ROOT "frontend\rule-engine-ui"
$APPROVAL_START = Join-Path $BACKEND "approval-flow-service\approval-start"
$RULEENGINE_START = Join-Path $BACKEND "rule-engine-service\rule-engine-start"
$GATEWAY_ROOT = Join-Path $BACKEND "gateway-service"
. (Join-Path $PSScriptRoot "db-check.ps1")

function Show-Menu {
    param([string]$Title = "规则引擎平台 - 启动器")
    Clear-Host
    Write-Host "==============================================================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "==============================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   [1] MySQL-Docker           - 全部容器化 (MySQL + rule-engine + UI)"
    Write-Host "   [2] MySQL-Local-Full       - 本地起所有后端 + 前端 (完整功能,推荐)"
    Write-Host "   [3] MySQL-Local-Quick      - 本地只起 rule-engine + 前端 (最快)"
    Write-Host "   [4] Docker-Compose-Full    - MySQL / Redis / Nacos + 全部服务"
    Write-Host ""
    Write-Host "   [5] 帮助 / [q] 退出"
    Write-Host ""
}

function Show-Help {
    Clear-Host
    Write-Host ""
    Write-Host "== 使用说明 =================================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "模式 1 | MySQL-Docker"
    Write-Host "  直接 docker compose 启动,无需本地 Java / Node。"
    Write-Host "  依赖: Docker Desktop 运行中；数据库连接配置加密。"
    Write-Host ""
    Write-Host "模式 2 | MySQL-Local-Full (推荐)"
    Write-Host "  本地顺序启动 approval-flow-service (8082) /"
    Write-Host "  rule-engine-service (8080) / gateway (9000) + frontend dev server (5173)。"
    Write-Host "  依赖: JDK 21 + Maven 3.9 + Node 18 + 本地/远程 MySQL。"
    Write-Host ""
    Write-Host "模式 3 | MySQL-Local-Quick"
    Write-Host "  只起 rule-engine + UI,启动最快。审批 / Gateway 不可用。"
    Write-Host "  依赖: JDK 21 + Maven 3.9 + Node 18 + MySQL。"
    Write-Host ""
    Write-Host "模式 4 | Docker-Compose-Full"
    Write-Host "  生产拓扑全栈: MySQL + Redis + Nacos + 三服务 + UI。"
    Write-Host ""
    Write-Host "退出任意模式: 按 Ctrl+C 终止后端:"
    Write-Host "  .\scripts\stop.ps1"
    Write-Host ""
    Write-Host "==============================================================================" -ForegroundColor Yellow
    Pause
}

function Wait-Backend {
    param(
        [string]$Url,
        [int]$MaxTry = 60
    )
    $count = 0
    while ($count -lt $MaxTry) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) {
                Write-Host "[OK] 后端健康检查通过 (${count}s)" -ForegroundColor Green
                return
            }
        } catch {}
        $count++
        Write-Host "  等待中... ($count/$MaxTry)" -NoNewline
    }
    Write-Host ""
    Write-Host "[警告] 后端等待超时 (${MaxTry}s)。Java / Node 进程快照:" -ForegroundColor Yellow
    Get-Process -Name java,node -ErrorAction SilentlyContinue |
        Format-Table Id, StartTime, @{N='CPU(s)';E={$_.CPU}}, Path -AutoSize
}

function Print-Urls {
    param([string]$Mode)
    Write-Host ""
    Write-Host "==============================================================================" -ForegroundColor Green
    Write-Host "  启动完成 ($(Get-Date -Format 'HH:mm:ss'))"
    Write-Host "==============================================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  前端.......... http://localhost:5173"
    Write-Host "  Swagger...... http://localhost:8080/rule-engine/swagger-ui.html"
    Write-Host "  Actuator..... http://localhost:8080/rule-engine/actuator/health"
    Write-Host "  MySQL........ encrypted datasource"
    if ($Mode -eq "full") {
        Write-Host "  Approval SW... http://localhost:8082/approval/swagger-ui/index.html"
        Write-Host "  Gateway...... http://localhost:9000"
    }
    Write-Host ""
    Write-Host "  关闭命令: .\scripts\stop.ps1"
    Write-Host "==============================================================================" -ForegroundColor Green
}

function Check-Prereqs {
    foreach ($cmd in @("java", "mvn", "node")) {
        if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
            Write-Host "[错误] $cmd 命令未找到,请安装并加入 PATH" -ForegroundColor Red
            return $false
        }
    }
    $javaLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
    if ($javaLine -notmatch '"(?<major>\d+)(\.(?<minor>\d+))?') {
        Write-Host "[错误] 无法识别 Java 版本: $javaLine" -ForegroundColor Red
        return $false
    }
    $major = [int]$Matches.major
    if ($major -eq 1) { $major = [int]$Matches.minor }
    if ($major -lt 21) {
        Write-Host "[错误] 当前 Java 版本低于 21,请切换 JDK 21+ 后重试" -ForegroundColor Red
        return $false
    }
    return $true
}

function Get-JasyptPassword {
    return Get-ConfigDecryptKey
}

function Set-AuthDefaults {
    if (-not $env:SA_TOKEN_JWT_SECRET_KEY) { $env:SA_TOKEN_JWT_SECRET_KEY = "rule-engine-sa-token-jwt-secret-2026" }
    if (-not $env:RULE_ENGINE_ADMIN_USERNAME) { $env:RULE_ENGINE_ADMIN_USERNAME = "admin" }
    if (-not $env:RULE_ENGINE_ADMIN_PASSWORD) { $env:RULE_ENGINE_ADMIN_PASSWORD = "admin123" }
    if (-not $env:RULE_ENGINE_TESTER_USERNAME) { $env:RULE_ENGINE_TESTER_USERNAME = "underwriter" }
    if (-not $env:RULE_ENGINE_TESTER_PASSWORD) { $env:RULE_ENGINE_TESTER_PASSWORD = "underwriter123" }
}

# ============================================================================
# 模式 1: MySQL-Docker
# ============================================================================
function Start-MySQLDocker {
    Clear-Host
    Write-Host "[MySQL-Docker] 通过 docker compose 启动 rule-engine + UI 容器..." -ForegroundColor Cyan
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "[错误] 未检测到 docker 命令。请先安装 Docker Desktop 并启动。" -ForegroundColor Red
        Pause; return
    }
    [void](Get-JasyptPassword)
    try {
        Test-DatabaseConnection
        Reset-ProjectDatabase -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }
    Push-Location $ROOT
    docker compose -f "deploy\docker-compose.yml" up --build -d
    Write-Host ""
    Write-Host "访问地址:"
    Write-Host "  Swagger  http://localhost:8080/rule-engine/swagger-ui.html"
    Write-Host "  前端     http://localhost:5173"
    Write-Host "  停止: docker compose -f deploy\docker-compose.yml down"
    Pop-Location
    try {
        Test-DatabaseState -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
    }
    Pause
}

# ============================================================================
# 模式 4: Docker-Compose-Full
# ============================================================================
function Start-FullDocker {
    Clear-Host
    Write-Host "[Docker-Compose-Full] 生产拓扑全栈 (MySQL/Redis/Nacos)..." -ForegroundColor Cyan
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "[错误] 未检测到 docker 命令。请先安装 Docker Desktop 并启动。" -ForegroundColor Red
        Pause; return
    }
    [void](Get-JasyptPassword)
    try {
        Test-DatabaseConnection
        Reset-ProjectDatabase -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }
    Push-Location $ROOT
    docker compose -f deploy\docker-compose.yml up --build -d
    Write-Host ""
    Write-Host "访问地址:"
    Write-Host "  Swagger  http://localhost:8080/rule-engine/swagger-ui.html"
    Write-Host "  前端     http://localhost:5173"
    Write-Host "  停止: docker compose -f deploy\docker-compose.yml down"
    Pop-Location
    try {
        Test-DatabaseState -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
    }
    Pause
}

# ============================================================================
# 模式 3: MySQL-Local-Quick
# ============================================================================
function Start-MySQLLocalQuick {
    Clear-Host
    if (-not (Check-Prereqs)) { Pause; return }

    Write-Host "[MySQL-Local-Quick] 启动 rule-engine + 前端 ..." -ForegroundColor Cyan

    $jasyptPwd = Get-JasyptPassword
    Set-AuthDefaults
    try {
        Test-DatabaseConnection
        Reset-ProjectDatabase
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }

    Write-Host "1/3 构建 rule-engine-service ..."
    Push-Location $BACKEND\rule-engine-service
    $build = mvn -q -DskipTests "-Djacoco.skip=true" install 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 构建 rule-engine-service 失败" -ForegroundColor Red
        Write-Host $build
        Pop-Location; Pause; return
    }
    Pop-Location

    Write-Host "2/3 启动 rule-engine-service (MySQL) ..."
    $jvmArgs = "-DRULE_ENGINE_AES_KEY=0123456789abcdef0123456789abcdef -Djasypt.encryptor.password=$jasyptPwd"
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-Dspring-boot.run.jvmArguments=$jvmArgs" -WorkingDirectory $RULEENGINE_START -WindowStyle Minimized -PassThru | Out-Null

    Write-Host "3/3 启动前端 dev server ..."
    Start-Process -FilePath "npm" -ArgumentList "run","dev","--","--port","5173" -WorkingDirectory $FRONTEND -WindowStyle Minimized -PassThru | Out-Null

    Write-Host "等待 rule-engine 健康检查..."
    Wait-Backend -Url "http://localhost:8080/rule-engine/actuator/health" -MaxTry 90
    try {
        Test-DatabaseState
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }
    Print-Urls -Mode "quick"
    Pause
}

# ============================================================================
# 模式 2: MySQL-Local-Full
# ============================================================================
function Start-MySQLLocalFull {
    Clear-Host
    if (-not (Check-Prereqs)) { Pause; return }

    Write-Host "[MySQL-Local-Full] 启动 approval-flow + rule-engine + gateway + 前端 ..." -ForegroundColor Cyan

    # 构建 rule-engine-service (多模块)
    Write-Host "1/4 构建 rule-engine-service ..."
    Push-Location $BACKEND\rule-engine-service
    $build = mvn -q -DskipTests "-Djacoco.skip=true" install 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 构建 rule-engine-service 失败" -ForegroundColor Red
        Write-Host $build
        Pop-Location; Pause; return
    }
    Pop-Location

    # 构建 approval-flow-service (多模块)
    Write-Host "2/4 构建 approval-flow-service ..."
    Push-Location $BACKEND\approval-flow-service
    $build = mvn -q -DskipTests "-Djacoco.skip=true" install 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 构建 approval-flow-service 失败" -ForegroundColor Red
        Write-Host $build
        Pop-Location; Pause; return
    }
    Pop-Location

    # 构建 gateway-service (单模块)
    Write-Host "3/4 构建 gateway-service ..."
    Push-Location $BACKEND\gateway-service
    $build = mvn -q -DskipTests "-Djacoco.skip=true" install 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 构建 gateway-service 失败" -ForegroundColor Red
        Write-Host $build
        Pop-Location; Pause; return
    }
    Pop-Location

    $jasyptPwd = Get-JasyptPassword
    Set-AuthDefaults
    try {
        Test-DatabaseConnection
        Reset-ProjectDatabase -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }
    $AES_KEY = "0123456789abcdef0123456789abcdef"

    Write-Host "4/4 启动服务 ..."

    # approval-flow-service
    $jvmApproval = "-Djasypt.encryptor.password=$jasyptPwd -DAPPROVAL_CALLBACK_SECRET=rule-engine-local-callback-secret"
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-Dspring-boot.run.jvmArguments=$jvmApproval" -WorkingDirectory $APPROVAL_START -WindowStyle Minimized -PassThru | Out-Null

    # rule-engine-service
    $jvmArgs = "-DRULE_ENGINE_AES_KEY=$AES_KEY -DAPPROVAL_CALLBACK_SECRET=rule-engine-local-callback-secret -Djasypt.encryptor.password=$jasyptPwd"
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-Dspring-boot.run.jvmArguments=$jvmArgs" -WorkingDirectory $RULEENGINE_START -WindowStyle Minimized -PassThru | Out-Null

    # gateway-service
    $jvmGateway = "-Djasypt.encryptor.password=$jasyptPwd -DNACOS_ENABLED=false"
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-Dspring-boot.run.jvmArguments=$jvmGateway" -WorkingDirectory $GATEWAY_ROOT -WindowStyle Minimized -PassThru | Out-Null

    # 前端
    Write-Host "启动前端 dev server ..."
    Start-Process -FilePath "npm" -ArgumentList "run","dev","--","--port","5173" -WorkingDirectory $FRONTEND -WindowStyle Minimized -PassThru | Out-Null

    Write-Host "等待 rule-engine 健康检查通过..."
    Wait-Backend -Url "http://localhost:8080/rule-engine/actuator/health" -MaxTry 90
    try {
        Test-DatabaseState -IncludeApproval
    } catch {
        Write-Host "[错误] $($_.Exception.Message)" -ForegroundColor Red
        Pause; return
    }
    Print-Urls -Mode "full"
    Write-Host "注意: gateway 启动需额外 10-20 秒,首次访问 http://localhost:9000 时触发路由发现。" -ForegroundColor Yellow
    Pause
}

# ============================================================================
# Main loop
# ============================================================================
function Invoke-Choice {
    param([string]$Selected)
    switch ($Selected) {
        "1" { Start-MySQLDocker; return $true }
        "2" { Start-MySQLLocalFull; return $true }
        "3" { Start-MySQLLocalQuick; return $true }
        "4" { Start-FullDocker; return $true }
        "5" { Show-Help; return $true }
        "q" { exit 0 }
        default { return $false }
    }
}

if ($Choice) {
    if (-not (Invoke-Choice $Choice)) {
        Write-Host "Invalid choice: $Choice" -ForegroundColor Yellow
        exit 1
    }
    exit 0
}

while ($true) {
    Show-Menu
    $CHOICE = Read-Host "请选择启动模式 [1/2/3/4/5/q]"

    if (-not (Invoke-Choice $CHOICE)) {
        Write-Host "无效输入,请重试" -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}
