$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidPath = Join-Path $projectRoot '.local/processes.json'
if (!(Test-Path $pidPath)) { return }
$processIds = Get-Content -Raw $pidPath | ConvertFrom-Json
foreach ($taskKind in @('backend', 'frontend')) {
    $taskPid = $processIds."${taskKind}Pid"
    $taskStarted = $processIds."${taskKind}Started"
    $taskProcess = Get-Process -Id $taskPid -ErrorAction SilentlyContinue
    if ($taskProcess -and $taskStarted -and $taskProcess.StartTime.Ticks -eq $taskStarted) {
        Stop-Process -Id $taskPid -ErrorAction Stop
    }
}
