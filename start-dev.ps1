[CmdletBinding()]
param(
    [switch]$Stop,
    [switch]$Restart,
    [switch]$NoBrowser,
    [ValidateRange(1024, 65535)][int]$BackendPort = 8088,
    [ValidateRange(1024, 65535)][int]$FrontendPort = 5174,
    [ValidateRange(1024, 65535)][int]$ManagementPort = 18089,
    [string]$ConfigPath = $env:STAR_RAIN_LOCAL_CONFIG,
    [switch]$FrontendWorker
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'backend'
$frontendRoot = Join-Path $projectRoot 'frontend'
$runtimeRoot = Join-Path $projectRoot '.local\dev'
$statePath = Join-Path $runtimeRoot 'session.json'
$siteUrl = "http://127.0.0.1:$FrontendPort/"
$apiUrl = "http://127.0.0.1:$BackendPort"

# A hidden worker keeps npm and its descendants attached to an identifiable owner.
if ($FrontendWorker) {
    $env:STAR_RAIN_API_TARGET = $apiUrl
    Set-Location -LiteralPath $frontendRoot
    $ErrorActionPreference = 'Continue'
    & (Get-Command npm.cmd -ErrorAction Stop).Source run dev -- --host 127.0.0.1 --port $FrontendPort --strictPort
    exit $LASTEXITCODE
}

function Get-Session {
    if (!(Test-Path -LiteralPath $statePath)) { return $null }
    $session = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    if ($session.Root -ne $projectRoot) { throw 'The startup state belongs to a different project directory.' }
    return $session
}

function Save-Session($Session) {
    $Session | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $statePath -Encoding UTF8
}

function Get-OwnedProcess($Record) {
    if (!$Record) { return $null }
    $process = Get-Process -Id $Record.Id -ErrorAction SilentlyContinue
    if (!$process) { return $null }
    try {
        if ($process.StartTime.ToUniversalTime().Ticks.ToString() -ne $Record.StartTicks) { return $null }
    } catch { return $null }
    return $process
}

function Stop-ProcessChildren([int]$ParentId) {
    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentId" -ErrorAction SilentlyContinue)
    foreach ($child in $children) {
        Stop-ProcessChildren -ParentId $child.ProcessId
        Stop-Process -Id $child.ProcessId -ErrorAction SilentlyContinue
    }
}

function Stop-Session($Session) {
    if (!$Session) { return }
    foreach ($record in @($Session.Frontend, $Session.Backend)) {
        $process = Get-OwnedProcess $record
        if ($process) {
            Stop-ProcessChildren -ParentId $process.Id
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
            $null = $process.WaitForExit(10000)
        }
    }
    if (Test-Path -LiteralPath $statePath) { Remove-Item -LiteralPath $statePath }
}

function Get-ProcessRecord($Process) {
    return @{ Id = $Process.Id; StartTicks = $Process.StartTime.ToUniversalTime().Ticks.ToString() }
}

function Test-Http([string]$Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch { return $false }
}

function Wait-Http([string]$Url, $Record, [string]$Name) {
    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (!(Get-OwnedProcess $Record)) {
            $hint = if ($Name -eq 'Backend') { ' Check that the configured MySQL instance is running and the private database configuration is valid.' } else { '' }
            throw "$Name exited.$hint Check the logs in $runtimeRoot"
        }
        if (Test-Http $Url) { return }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not become ready within 120 seconds. Check MySQL/configuration and the logs in $runtimeRoot"
}

function Assert-FreePort([int]$Port) {
    $listeners = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    if ($listeners | Where-Object { $_.Port -eq $Port }) {
        throw "Port $Port is already in use by a program outside this startup session. Close it or choose another port."
    }
}

function Test-Tcp([string]$HostName, [int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        if (!$connection.AsyncWaitHandle.WaitOne(500)) { return $false }
        $client.EndConnect($connection)
        return $true
    } catch { return $false } finally { $client.Dispose() }
}

function Ensure-LocalDatabase([string]$Configuration) {
    $databaseUrl = $env:STAR_RAIN_DB_URL
    if (!$databaseUrl -and $Configuration) {
        $text = Get-Content -LiteralPath $Configuration -Raw
        $line = [regex]::Match($text, '(?m)^\s*STAR_RAIN_DB_URL\s*:\s*(.+)$')
        if ($line.Success) { $databaseUrl = $line.Groups[1].Value.Trim().Trim([char[]]@('"', "'")) }
    }
    if (!$databaseUrl -or !$databaseUrl.StartsWith('jdbc:mysql://')) { return }
    $endpoint = [uri]$databaseUrl.Substring(5)
    if ($endpoint.Host -notin @('127.0.0.1', 'localhost', '[::1]', '::1')) { return }
    $databasePort = if ($endpoint.Port -gt 0) { $endpoint.Port } else { 3306 }
    if (Test-Tcp $endpoint.Host $databasePort) { return }
    if ($databasePort -ne 3306) { throw "The configured MySQL port $databasePort is unavailable. Start that database instance first." }
    $service = Get-CimInstance Win32_Service -Filter "Name='MySQL'" -ErrorAction SilentlyContinue
    if (!$service) { throw 'MySQL is not reachable. Start the configured local database first.' }
    Write-Host 'Starting the existing local MySQL installation...'
    try {
        Start-Service -Name $service.Name -ErrorAction Stop
    } catch {
        # Starting a service needs administrator rights. The existing server can also run
        # as the current user, with the same data and no database initialization or reset.
        $pathMatch = [regex]::Match($service.PathName, '^\s*(?:"(?<exe>[^"]+)"|(?<exe>.+?mysqld(?:\.exe)?))(?=\s|$)')
        if (!$pathMatch.Success) { throw 'Cannot locate the existing MySQL server. Start MySQL manually.' }
        $server = $pathMatch.Groups['exe'].Value
        if (!(Test-Path -LiteralPath $server) -and (Test-Path -LiteralPath ($server + '.exe'))) { $server += '.exe' }
        if (!(Test-Path -LiteralPath $server)) { throw 'The registered MySQL executable is missing.' }
        $arguments = @()
        $defaults = [regex]::Match($service.PathName, '--defaults-file=(?:"(?<file>[^"]+)"|(?<file>\S+))')
        if ($defaults.Success) { $arguments += ('"--defaults-file={0}"' -f $defaults.Groups['file'].Value) }
        $arguments += @('--standalone', '--port=3306', '--bind-address=127.0.0.1', '--mysqlx-bind-address=127.0.0.1',
            ('"--log-error={0}"' -f (Join-Path $runtimeRoot 'mysql.log')),
            ('"--pid-file={0}"' -f (Join-Path $runtimeRoot 'mysql.pid')),
            ('"--tmpdir={0}"' -f (Join-Path $runtimeRoot 'tmp')))
        $database = Start-Process -FilePath $server -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $runtimeRoot 'mysql-output.log') -RedirectStandardError (Join-Path $runtimeRoot 'mysql-error.log')
        @{ Root = $projectRoot; Process = (Get-ProcessRecord $database) } | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $runtimeRoot 'mysql-session.json') -Encoding UTF8
    }
    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-Tcp $endpoint.Host $databasePort) { return }
        Start-Sleep -Milliseconds 500
    }
    throw "MySQL did not become ready. Check the database logs in $runtimeRoot"
}

function Get-JavaHome {
    $candidates = @($env:JAVA_HOME)
    foreach ($name in @('javac.exe', 'java.exe')) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) { $candidates += Split-Path (Split-Path $command.Source -Parent) -Parent }
    }
    foreach ($registryRoot in @('HKLM:\SOFTWARE\JavaSoft\JDK', 'HKLM:\SOFTWARE\JavaSoft\Java Development Kit')) {
        foreach ($key in @(Get-ChildItem $registryRoot -ErrorAction SilentlyContinue)) {
            $candidates += (Get-ItemProperty $key.PSPath -ErrorAction SilentlyContinue).JavaHome
        }
    }
    foreach ($candidate in @($candidates | Where-Object { $_ } | Select-Object -Unique)) {
        $compiler = Join-Path $candidate 'bin\javac.exe'
        if (!(Test-Path -LiteralPath $compiler)) { continue }
        $ErrorActionPreference = 'Continue'
        $version = (& $compiler -version 2>&1 | Out-String)
        $ErrorActionPreference = 'Stop'
        if ($version -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 21) { return $candidate }
    }
    throw 'JDK 21 or newer was not found. Install JDK 21 and add its bin directory to PATH or set JAVA_HOME.'
}

function Get-Chrome {
    $candidates = @()
    $command = Get-Command chrome.exe -ErrorAction SilentlyContinue
    if ($command) { $candidates += $command.Source }
    foreach ($key in @('HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe',
            'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe')) {
        if (Test-Path $key) { $candidates += (Get-Item $key).GetValue('') }
    }
    foreach ($base in @($env:ProgramFiles, ${env:ProgramFiles(x86)}, $env:LOCALAPPDATA)) {
        if ($base) { $candidates += Join-Path $base 'Google\Chrome\Application\chrome.exe' }
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    throw 'Google Chrome was not found. Install Chrome, or use -NoBrowser and open the displayed URL manually.'
}

function Open-Site([string]$ChromePath) {
    Write-Host "Website ready: $siteUrl" -ForegroundColor Green
    Write-Host "Logs: $runtimeRoot"
    Write-Host 'Run this launcher with -Stop to stop both services, or -Restart to rebuild/restart.'
    if (!$NoBrowser) { Start-Process -FilePath $ChromePath -ArgumentList @('--new-tab', $siteUrl) }
}

function Resolve-Config {
    if ($ConfigPath) {
        if (!(Test-Path -LiteralPath $ConfigPath -PathType Leaf)) { throw "Configuration file not found: $ConfigPath" }
        $resolvedConfig = (Resolve-Path -LiteralPath $ConfigPath).Path
        if (!$resolvedConfig.StartsWith($projectRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Keep this project configuration inside the project root directory.'
        }
        return $resolvedConfig
    }
    $localConfig = Join-Path $backendRoot 'application-local-secret.yml'
    if (Test-Path -LiteralPath $localConfig) { return $localConfig }
    if ($env:STAR_RAIN_DB_URL -and $env:STAR_RAIN_DB_USERNAME -and $null -ne $env:STAR_RAIN_DB_PASSWORD) { return $null }
    throw 'Configure backend/application-local-secret.yml from its example, or provide STAR_RAIN_DB_URL/USERNAME/PASSWORD. Start the configured MySQL instance first.'
}

function Invoke-Build([string]$Program, [string[]]$Arguments, [string]$Directory, [string]$Log) {
    Push-Location -LiteralPath $Directory
    try {
        $ErrorActionPreference = 'Continue'
        & $Program @Arguments *> $Log
        $code = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($code -ne 0) { throw "Build/install failed (exit $code). See $Log" }
    } finally { Pop-Location }
}

$mutex = $null
$locked = $false
$started = $null
try {
    New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
    $temporaryRoot = Join-Path $runtimeRoot 'tmp'
    New-Item -ItemType Directory -Path $temporaryRoot -Force | Out-Null
    $env:TEMP = $temporaryRoot
    $env:TMP = $temporaryRoot
    $env:npm_config_cache = Join-Path $projectRoot '.local\npm-cache'
    $mavenRepository = Join-Path $projectRoot '.local\maven-repository'
    $hasher = [System.Security.Cryptography.SHA256]::Create()
    try { $key = ([BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot.ToLowerInvariant())))).Replace('-', '').Substring(0, 16) }
    finally { $hasher.Dispose() }
    $mutex = New-Object System.Threading.Mutex($false, "Local\StarRainDev_$key")
    $locked = $mutex.WaitOne(0)
    if (!$locked) { throw 'This project is already starting. Wait for the other startup window to finish.' }
    $session = Get-Session
    if ($Stop) {
        Stop-Session $session
        Write-Host 'The services started by this launcher have been stopped.' -ForegroundColor Green
        exit 0
    }
    if (@($BackendPort, $FrontendPort, $ManagementPort | Select-Object -Unique).Count -ne 3) {
        throw 'Backend, frontend and management ports must be different.'
    }
    $chrome = if (!$NoBrowser) { Get-Chrome } else { $null }
    if (!$Restart -and $session -and $session.BackendPort -eq $BackendPort -and $session.FrontendPort -eq $FrontendPort -and $session.ManagementPort -eq $ManagementPort -and
            (Get-OwnedProcess $session.Backend) -and (Get-OwnedProcess $session.Frontend)) {
        Wait-Http "$apiUrl/api/public/site/config" $session.Backend 'Backend'
        Wait-Http $siteUrl $session.Frontend 'Frontend'
        Open-Site $chrome
        exit 0
    }
    Stop-Session $session
    foreach ($port in @($BackendPort, $FrontendPort, $ManagementPort)) { Assert-FreePort $port }
    $configuration = Resolve-Config
    Ensure-LocalDatabase $configuration
    $javaHome = Get-JavaHome
    $env:JAVA_HOME = $javaHome
    $env:PATH = (Join-Path $javaHome 'bin') + ';' + $env:PATH
    $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $nodeVersion = (& $node --version).TrimStart('v')
    if ([version]$nodeVersion -lt [version]'22.12.0') { throw 'Node.js 22.12 or newer is required.' }
    Write-Host "JDK: $javaHome"
    Write-Host "Configuration: $(if ($configuration) { $configuration } else { 'process environment' })"
    Write-Host "Backend: $apiUrl | Frontend: $siteUrl"

    [xml]$pom = Get-Content -LiteralPath (Join-Path $backendRoot 'pom.xml') -Raw
    $jar = Join-Path $backendRoot "star-rain-boot\target\star-rain-boot-$($pom.project.version).jar"
    $rebuild = !(Test-Path -LiteralPath $jar)
    if (!$rebuild) {
        $jarTime = (Get-Item -LiteralPath $jar).LastWriteTimeUtc
        $inputs = @(Get-Item -LiteralPath (Join-Path $backendRoot 'pom.xml'))
        foreach ($module in @(Get-ChildItem -LiteralPath $backendRoot -Directory -Filter 'star-rain-*')) {
            $inputs += Get-Item -LiteralPath (Join-Path $module.FullName 'pom.xml')
            $source = Join-Path $module.FullName 'src\main'
            if (Test-Path -LiteralPath $source) { $inputs += Get-ChildItem -LiteralPath $source -Recurse -File }
        }
        $rebuild = [bool]($inputs | Where-Object { $_.LastWriteTimeUtc -gt $jarTime } | Select-Object -First 1)
    }
    if ($rebuild) {
        Write-Host 'Building backend (first startup may take several minutes)...'
        Invoke-Build $maven @('-B', '-DskipTests', "-Dmaven.repo.local=$mavenRepository", 'package') $backendRoot (Join-Path $runtimeRoot 'backend-build.log')
    }
    $lockHash = (Get-FileHash -LiteralPath (Join-Path $frontendRoot 'package-lock.json') -Algorithm SHA256).Hash
    $dependencyStamp = Join-Path $runtimeRoot 'dependencies.sha256'
    $install = !(Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules\.bin\vite.cmd'))
    if (Test-Path -LiteralPath $dependencyStamp) { $install = $install -or ((Get-Content -LiteralPath $dependencyStamp -Raw).Trim() -ne $lockHash) }
    if ($install) {
        Write-Host 'Installing locked frontend dependencies...'
        Invoke-Build $npm @('ci', '--no-audit', '--no-fund') $frontendRoot (Join-Path $runtimeRoot 'frontend-install.log')
    }
    $lockHash | Set-Content -LiteralPath $dependencyStamp -Encoding ASCII

    # The running JVM uses its own copy so a later Maven build cannot hit a locked target JAR.
    $runtimeJar = Join-Path $runtimeRoot 'backend-app.jar'
    Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force
    $javaArguments = @('-Xmx512m', ('"-Djava.io.tmpdir={0}"' -f $temporaryRoot), '-jar', ('"{0}"' -f $runtimeJar), "--server.port=$BackendPort", '--server.address=127.0.0.1',
        "--management.server.port=$ManagementPort", "--star-rain.account.frontend-origin=$($siteUrl.TrimEnd('/'))")
    if ($configuration) { $javaArguments += "--spring.config.import=$(([uri]$configuration).AbsoluteUri)" }
    $backend = Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList $javaArguments -WorkingDirectory $backendRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runtimeRoot 'backend.log') -RedirectStandardError (Join-Path $runtimeRoot 'backend-error.log')
    $started = @{ Root = $projectRoot; BackendPort = $BackendPort; FrontendPort = $FrontendPort; ManagementPort = $ManagementPort;
        Backend = (Get-ProcessRecord $backend); Frontend = $null }
    Save-Session $started
    Write-Host 'Waiting for backend and database...'
    Wait-Http "$apiUrl/api/public/site/config" $started.Backend 'Backend'
    $workerArguments = @('-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $PSCommandPath),
        '-FrontendWorker', '-BackendPort', $BackendPort, '-FrontendPort', $FrontendPort)
    $frontend = Start-Process -FilePath (Join-Path $PSHOME 'powershell.exe') -ArgumentList $workerArguments -WorkingDirectory $frontendRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runtimeRoot 'frontend.log') -RedirectStandardError (Join-Path $runtimeRoot 'frontend-error.log')
    $started.Frontend = Get-ProcessRecord $frontend
    Save-Session $started
    Write-Host 'Waiting for frontend...'
    Wait-Http $siteUrl $started.Frontend 'Frontend'
    if (!(Test-Http "$($siteUrl)api/public/site/config")) { throw 'The frontend API proxy cannot reach the backend. Check the frontend log.' }
    Open-Site $chrome
} catch {
    if ($started) { Stop-Session $started }
    Write-Host "Startup failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Logs: $runtimeRoot"
    exit 1
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    if ($mutex) { $mutex.Dispose() }
}
