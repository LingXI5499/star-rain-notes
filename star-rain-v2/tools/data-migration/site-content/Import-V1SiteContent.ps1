param(
    [string]$SourceDatabase = 'star_rain_notes',
    [Parameter(Mandatory = $true)] [string]$TargetDatabase,
    [string]$MysqlPath = 'D:\DevelopTool\Language\MySQL\mysql-8.0.34-winx64\bin\mysql.exe'
)

$ErrorActionPreference = 'Stop'
if ($SourceDatabase -notmatch '^[A-Za-z0-9_]+$' -or $TargetDatabase -notmatch '^[A-Za-z0-9_]+$') {
    throw 'Database names may contain only letters, numbers and underscores.'
}
if ($SourceDatabase -eq $TargetDatabase) { throw 'Source and target databases must differ.' }

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\backend')).Path
$secretPath = Join-Path $backendRoot 'application-local-secret.yml'
if (-not (Test-Path -LiteralPath $secretPath)) { throw 'V2 local database configuration not found.' }
$lines = Get-Content -LiteralPath $secretPath
$jdbcLine = $lines | Where-Object { $_ -match '^STAR_RAIN_DB_URL:' } | Select-Object -First 1
$userLine = $lines | Where-Object { $_ -match '^STAR_RAIN_DB_USERNAME:' } | Select-Object -First 1
$passwordLine = $lines | Where-Object { $_ -match '^STAR_RAIN_DB_PASSWORD:' } | Select-Object -First 1
$jdbcUrl = ($jdbcLine -split ':\s*', 2)[1].Trim().Trim('"').Trim("'")
$dbUser = ($userLine -split ':\s*', 2)[1].Trim().Trim('"').Trim("'")
$dbPassword = ($passwordLine -split ':\s*', 2)[1].Trim().Trim('"').Trim("'")
if ($jdbcUrl -notmatch '^jdbc:mysql://(?<dbHost>[^:/?]+):(?<dbPort>\d+)/[^?]+') {
    throw 'Unsupported database URL.'
}
$dbHost = $Matches.dbHost
$dbPort = $Matches.dbPort
$sqlTemplate = [System.IO.File]::ReadAllText(
    (Join-Path $PSScriptRoot 'import-v1-site-content.sql'), [System.Text.Encoding]::UTF8)
$sql = $sqlTemplate.Replace('{{SOURCE}}', "``$SourceDatabase``").Replace('{{TARGET}}', "``$TargetDatabase``")
$sqlFile = Join-Path ([System.IO.Path]::GetTempPath()) ("star-rain-v1-site-" + [guid]::NewGuid().ToString('N') + ".sql")
[System.IO.File]::WriteAllText($sqlFile, $sql, (New-Object System.Text.UTF8Encoding($false)))
$env:MYSQL_PWD = $dbPassword
try {
    $sourcePath = $sqlFile.Replace('\', '/')
    & $MysqlPath -h $dbHost -P $dbPort -u $dbUser --default-character-set=utf8mb4 $TargetDatabase -e "source $sourcePath"
    if ($LASTEXITCODE -ne 0) { throw "MySQL import failed with exit code $LASTEXITCODE." }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $sqlFile -ErrorAction SilentlyContinue
}
