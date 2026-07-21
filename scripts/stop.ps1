<#
  rule-engine 一键停止脚本
  停止 start-dev.ps1 记录的 PID、常用本地端口进程，并关闭 compose 服务。
#>

$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path $Root '.dev-pids.json'
$Ports = @(8080, 8082, 9000, 5173)

function Stop-ProcessId {
    param([int]$ProcessId, [string]$Reason)
    $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "Stopping PID $ProcessId ($Reason)"
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

if (Test-Path $PidFile) {
    $pids = Get-Content $PidFile | ConvertFrom-Json
    foreach ($name in @('approval', 'ruleEngine', 'gateway', 'ui')) {
        if ($pids.$name) {
            Stop-ProcessId -ProcessId ([int]$pids.$name) -Reason $name
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

foreach ($port in $Ports) {
    $owners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($owner in $owners) {
        Stop-ProcessId -ProcessId ([int]$owner) -Reason "port $port"
    }
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Push-Location $Root
    docker compose -f deploy\docker-compose.yml down
    Pop-Location
}

Write-Host "All rule-engine local services stopped."
