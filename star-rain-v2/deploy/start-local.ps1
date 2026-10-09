param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtimeRoot = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
$pidPath = Join-Path $runtimeRoot 'processes.json'
if (Test-Path $pidPath) {
    $existing = Get-Content -Raw $pidPath | ConvertFrom-Json
    foreach ($taskKind in @('backend', 'frontend')) {
        $existingProcess = Get-Process -Id $existing."${taskKind}Pid" -ErrorAction SilentlyContinue
        if ($existingProcess -and $existingProcess.StartTime.Ticks -eq $existing."${taskKind}Started") {
            throw 'V2 is already running. Use stop-local.ps1 before starting again.'
        }
    }
}
if (!(Test-Path (Join-Path $projectRoot 'backend/application-local-secret.yml'))) {
    throw 'Create the ignored backend/application-local-secret.yml from application-local-secret.example.yml first.'
}
if ($Build) {
    Push-Location (Join-Path $projectRoot 'backend')
    try { & mvn.cmd -B -pl star-rain-boot -am -DskipTests clean package; if ($LASTEXITCODE) { throw 'Backend build failed' } } finally { Pop-Location }
    Push-Location (Join-Path $projectRoot 'frontend')
    try { & npm.cmd ci --ignore-scripts --cache (Join-Path $runtimeRoot 'npm-cache'); if ($LASTEXITCODE) { throw 'Dependency install failed' }; & npm.cmd run build; if ($LASTEXITCODE) { throw 'Frontend build failed' } } finally { Pop-Location }
}
$jarPath = Join-Path $projectRoot 'backend/star-rain-boot/target/star-rain-boot-2.0.0.jar'
if (!(Test-Path $jarPath)) { throw 'Build the backend first with -Build.' }
$backendProcess = Start-Process -FilePath (Get-Command java.exe).Source -ArgumentList @('-jar', $jarPath) -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot 'backend.log') -RedirectStandardError (Join-Path $runtimeRoot 'backend-error.log')
$frontendProcess = Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList @((Join-Path $projectRoot 'frontend/node_modules/vite/bin/vite.js'), '--host', '127.0.0.1') -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot 'frontend.log') -RedirectStandardError (Join-Path $runtimeRoot 'frontend-error.log')
@{ backendPid = $backendProcess.Id; backendStarted = $backendProcess.StartTime.Ticks; frontendPid = $frontendProcess.Id; frontendStarted = $frontendProcess.StartTime.Ticks } | ConvertTo-Json | Set-Content $pidPath
Write-Host 'V2 starting: frontend http://127.0.0.1:5174 | backend http://127.0.0.1:8088'
