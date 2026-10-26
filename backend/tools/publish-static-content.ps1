param([Parameter(Mandatory=$true)][string]$Plan, [Parameter(Mandatory=$true)][string]$Config)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$javaHome = $env:STAR_RAIN_JAVA_HOME
if (!$javaHome) { $javaHome = $env:JAVA_HOME }
if (!$javaHome -or !(Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
    throw 'Set STAR_RAIN_JAVA_HOME to the existing JDK 21 installation.'
}
$jar = Join-Path $projectRoot '.local\dev\backend-app.jar'
if (!(Test-Path -LiteralPath $jar)) {
    $jar = Join-Path $projectRoot 'backend\star-rain-boot\target\star-rain-boot-2.0.1.jar'
}
if (!(Test-Path -LiteralPath $jar)) { throw 'Build the current backend first.' }
$runtime = Join-Path $projectRoot '.local\dev\content-maintenance'
$classes = Join-Path $runtime 'classes'
$dependencies = Join-Path $runtime 'dependencies'
New-Item -ItemType Directory -Path $classes,$dependencies -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
try {
    foreach($entry in $archive.Entries) {
        if ($entry.FullName -match '^BOOT-INF/lib/[^/]+\.jar$') {
            $destination = Join-Path $dependencies ([IO.Path]::GetFileName($entry.FullName))
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$destination,$true)
        }
    }
} finally { $archive.Dispose() }
$classpath = "$classes;$dependencies\*"
# Project classes live inside the executable archive; expose them for javac and
# the standalone launcher without changing the built application.
$archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
try {
    foreach($entry in $archive.Entries) {
        if ($entry.FullName -like 'BOOT-INF/classes/*' -and $entry.Name) {
            $relative = $entry.FullName.Substring('BOOT-INF/classes/'.Length)
            $destination = Join-Path $classes $relative
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$destination,$true)
        }
    }
} finally { $archive.Dispose() }
& (Join-Path $javaHome 'bin\javac.exe') -encoding UTF-8 -cp $classpath -d $classes (Join-Path $PSScriptRoot 'PublishStaticContent.java')
if ($LASTEXITCODE -ne 0) { throw 'Maintenance compilation failed.' }
Set-Location -LiteralPath (Join-Path $projectRoot 'backend')
& (Join-Path $javaHome 'bin\java.exe') -Xmx768m -cp $classpath PublishStaticContent $Plan $Config
if ($LASTEXITCODE -ne 0) { throw 'Content publication failed. Retain the plan and database backup.' }
