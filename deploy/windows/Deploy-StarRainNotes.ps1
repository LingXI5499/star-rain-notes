param(
    [switch]$ValidateOnly,
    [string]$HeadlessInstallPath
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

[System.Windows.Forms.Application]::EnableVisualStyles()

$script:PayloadRoot = Join-Path $PSScriptRoot 'payload'
$script:Version = '1.7.0'

function Quote-Yaml([string]$Value) {
    return "'" + ($Value -replace "'", "''") + "'"
}

function Add-Field($Form, [string]$Label, [int]$Top, [string]$Default = '', [bool]$Password = $false) {
    $caption = New-Object System.Windows.Forms.Label
    $caption.Text = $Label
    $caption.Left = 24
    $caption.Top = $Top + 4
    $caption.Width = 145
    $Form.Controls.Add($caption)

    $input = New-Object System.Windows.Forms.TextBox
    $input.Left = 175
    $input.Top = $Top
    $input.Width = 415
    $input.Text = $Default
    $input.UseSystemPasswordChar = $Password
    $Form.Controls.Add($input)
    return $input
}

function Write-DeploymentFiles($Values) {
    $target = [System.IO.Path]::GetFullPath($Values.InstallPath)
    if (-not (Test-Path (Join-Path $script:PayloadRoot 'backend\star-rain-notes-backend-1.7.0.jar'))) {
        throw '部署包不完整：未找到后端 JAR。请重新解压完整发布包。'
    }
    if (-not (Test-Path (Join-Path $script:PayloadRoot 'frontend\index.html'))) {
        throw '部署包不完整：未找到前端 index.html。请重新解压完整发布包。'
    }

    $directories = @(
        $target,
        (Join-Path $target 'app\backend'),
        (Join-Path $target 'app\frontend'),
        (Join-Path $target 'config'),
        (Join-Path $target 'data\uploads'),
        (Join-Path $target 'data\private-media'),
        (Join-Path $target 'data\portfolio-prototypes'),
        (Join-Path $target 'logs')
    )
    foreach ($directory in $directories) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    Copy-Item (Join-Path $script:PayloadRoot 'backend\star-rain-notes-backend-1.7.0.jar') `
        (Join-Path $target 'app\backend\star-rain-notes-backend-1.7.0.jar') -Force
    Copy-Item (Join-Path $script:PayloadRoot 'frontend\*') (Join-Path $target 'app\frontend') -Recurse -Force

    $normalizedTarget = $target.Replace('\', '/')
    $secureCookie = $Values.SecureCookie.ToString().ToLowerInvariant()
    $config = @"
server:
  port: $($Values.BackendPort)
  address: 127.0.0.1
  forward-headers-strategy: framework
  servlet:
    session:
      cookie:
        secure: $secureCookie

spring:
  datasource:
    url: $(Quote-Yaml "jdbc:mysql://$($Values.MySqlHost):$($Values.MySqlPort)/$($Values.Database)?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=DISABLED&allowPublicKeyRetrieval=true")
    username: $(Quote-Yaml $Values.MySqlUser)
    password: $(Quote-Yaml $Values.MySqlPassword)

app:
  seo:
    site-origin: $(Quote-Yaml $Values.SiteUrl)
    frontend-index-path: $(Quote-Yaml "$normalizedTarget/app/frontend/index.html")
  account:
    super-admin-email: $(Quote-Yaml $Values.AdminEmail)
    mail:
      host: $(Quote-Yaml $Values.MailHost)
      port: $($Values.MailPort)
      username: $(Quote-Yaml $Values.MailUser)
      auth-code: $(Quote-Yaml $Values.MailPassword)
      from: $(Quote-Yaml $Values.MailFrom)
      ssl-enabled: true
      base-url: $(Quote-Yaml $Values.SiteUrl)
  media:
    storage-dir: $(Quote-Yaml "$normalizedTarget/data/uploads")
    private-storage-dir: $(Quote-Yaml "$normalizedTarget/data/private-media")
    public-base: /uploads
  portfolio-prototype:
    private-root: $(Quote-Yaml "$normalizedTarget/data/portfolio-prototypes")
    public-root: $(Quote-Yaml "$normalizedTarget/data/uploads/prototypes")
"@
    Set-Content -LiteralPath (Join-Path $target 'config\application-deploy.yml') -Value $config -Encoding UTF8

    $nginxRoot = $normalizedTarget
    $nginx = @"
server {
    listen 80;
    server_name _;
    root $nginxRoot/app/frontend;
    index index.html;
    client_max_body_size 55m;

    location ^~ /uploads/prototypes/ {
        alias $nginxRoot/data/uploads/prototypes/;
        autoindex off;
        add_header Content-Security-Policy "default-src 'self' data:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'self'" always;
        add_header X-Content-Type-Options nosniff always;
        add_header Cache-Control "public, max-age=31536000, immutable" always;
    }

    location ^~ /uploads/ {
        alias $nginxRoot/data/uploads/;
        autoindex off;
        add_header X-Content-Type-Options nosniff always;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:$($Values.BackendPort);
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }

    location /actuator/ {
        proxy_pass http://127.0.0.1:$($Values.BackendPort);
        proxy_set_header Host `$host;
    }

    location / {
        try_files `$uri `$uri/ /index.html;
    }
}
"@
    Set-Content -LiteralPath (Join-Path $target 'config\nginx-star-rain-notes.conf') -Value $nginx -Encoding UTF8

    $startScript = @"
`$root = Split-Path -Parent `$MyInvocation.MyCommand.Path
`$jar = Join-Path `$root 'app\backend\star-rain-notes-backend-1.7.0.jar'
`$config = (Join-Path `$root 'config\application-deploy.yml').Replace('\', '/')
`$stdout = Join-Path `$root 'logs\backend.out.log'
`$stderr = Join-Path `$root 'logs\backend.err.log'
`$process = Start-Process -FilePath 'java.exe' -ArgumentList @('-jar', `$jar, '--spring.profiles.active=prod', "--spring.config.additional-location=file:`$config") -WorkingDirectory `$root -WindowStyle Hidden -RedirectStandardOutput `$stdout -RedirectStandardError `$stderr -PassThru
Set-Content -LiteralPath (Join-Path `$root 'logs\backend.pid') -Value `$process.Id -Encoding ASCII
Write-Host "后端已启动，PID: `$(`$process.Id)"
Write-Host '请将 config\nginx-star-rain-notes.conf 加入 Nginx 配置并重载 Nginx。'
"@
    Set-Content -LiteralPath (Join-Path $target 'start-service.ps1') -Value $startScript -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $target '启动服务.cmd') -Value "@echo off`r`npowershell.exe -NoProfile -ExecutionPolicy Bypass -File `"%~dp0start-service.ps1`"`r`npause`r`n" -Encoding ASCII

    $stopScript = @"
`$root = Split-Path -Parent `$MyInvocation.MyCommand.Path
`$pidFile = Join-Path `$root 'logs\backend.pid'
if (-not (Test-Path `$pidFile)) { Write-Host '未找到运行中的后端 PID。'; exit 0 }
`$backendPid = [int](Get-Content -LiteralPath `$pidFile -Raw)
`$process = Get-CimInstance Win32_Process -Filter "ProcessId=`$backendPid" -ErrorAction SilentlyContinue
if (`$process -and `$process.CommandLine -like '*star-rain-notes-backend-1.7.0.jar*') {
    Stop-Process -Id `$backendPid
    Write-Host "后端已停止，PID: `$backendPid"
} else {
    Write-Host 'PID 已失效或不属于星雨笔录，未执行停止操作。'
}
Remove-Item -LiteralPath `$pidFile -Force -ErrorAction SilentlyContinue
"@
    Set-Content -LiteralPath (Join-Path $target 'stop-service.ps1') -Value $stopScript -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $target '停止服务.cmd') -Value "@echo off`r`npowershell.exe -NoProfile -ExecutionPolicy Bypass -File `"%~dp0stop-service.ps1`"`r`npause`r`n" -Encoding ASCII

    $notes = @"
星雨笔录 v1.7.0 已部署到：$target

1. 确保 Java 21、MySQL 8.0 与 Nginx 已安装。
2. 数据库必须已创建；后端首次启动会自动执行 Flyway 迁移。
3. 将 config\nginx-star-rain-notes.conf 引入 Nginx，并按域名与 HTTPS 情况调整 listen/server_name。
4. 双击“启动服务.cmd”启动后端，然后重载 Nginx。
5. 首次使用访问 /admin/activate，通过超级管理员邮箱激活。
6. 升级时必须保留 data、config 和数据库；不要用空目录覆盖它们。

敏感凭据保存在 config\application-deploy.yml，请限制该文件的读取权限。
"@
    Set-Content -LiteralPath (Join-Path $target '部署后请阅读.txt') -Value $notes -Encoding UTF8

    if ($Values.StartAfterDeploy) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $target 'start-service.ps1')
    }
    return $target
}

if ($ValidateOnly) {
    $jar = Join-Path $script:PayloadRoot 'backend\star-rain-notes-backend-1.7.0.jar'
    $index = Join-Path $script:PayloadRoot 'frontend\index.html'
    if (-not (Test-Path $jar) -or -not (Test-Path $index)) {
        throw '部署包校验失败：缺少后端 JAR 或前端 index.html。'
    }
    Write-Output 'Deployment package validation passed.'
    exit 0
}

if (-not [string]::IsNullOrWhiteSpace($HeadlessInstallPath)) {
    $testValues = [pscustomobject]@{
        InstallPath = $HeadlessInstallPath
        MySqlHost = '127.0.0.1'
        MySqlPort = 3306
        Database = 'star_rain_notes'
        MySqlUser = 'root'
        MySqlPassword = ''
        BackendPort = 24680
        SiteUrl = 'http://localhost'
        AdminEmail = ''
        MailHost = 'smtp.example.com'
        MailPort = 465
        MailUser = ''
        MailPassword = ''
        MailFrom = ''
        SecureCookie = $false
        StartAfterDeploy = $false
    }
    $result = Write-DeploymentFiles $testValues
    Write-Output "Headless deployment test completed: $result"
    exit 0
}

$form = New-Object System.Windows.Forms.Form
$form.Text = '星雨笔录 v1.7.0 · 图形化部署向导'
$form.StartPosition = 'CenterScreen'
$form.Size = New-Object System.Drawing.Size(650, 690)
$form.MinimumSize = $form.Size
$form.MaximizeBox = $false
$form.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 9)

$title = New-Object System.Windows.Forms.Label
$title.Text = '星雨笔录 · Windows 图形化部署'
$title.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 16, [System.Drawing.FontStyle]::Bold)
$title.Left = 24
$title.Top = 18
$title.Width = 500
$form.Controls.Add($title)

$subtitle = New-Object System.Windows.Forms.Label
$subtitle.Text = '部署前请准备 Java 21、MySQL 8.0 和 Nginx。已有站点升级请先备份数据库与 data 目录。'
$subtitle.Left = 26
$subtitle.Top = 56
$subtitle.Width = 585
$subtitle.Height = 38
$form.Controls.Add($subtitle)

$install = Add-Field $form '安装目录' 100 'C:\StarRainNotes'
$browse = New-Object System.Windows.Forms.Button
$browse.Text = '浏览…'
$browse.Left = 505
$browse.Top = 99
$browse.Width = 85
$form.Controls.Add($browse)
$install.Width = 325

$mysqlHost = Add-Field $form 'MySQL 主机' 138 '127.0.0.1'
$mysqlPort = Add-Field $form 'MySQL 端口' 174 '3306'
$database = Add-Field $form '数据库名' 210 'star_rain_notes'
$mysqlUser = Add-Field $form '数据库用户' 246 'root'
$mysqlPassword = Add-Field $form '数据库密码' 282 '' $true
$backendPort = Add-Field $form '后端端口' 318 '24680'
$siteUrl = Add-Field $form '站点地址' 354 'http://localhost'
$adminEmail = Add-Field $form '超级管理员邮箱' 390 ''
$mailHost = Add-Field $form 'SMTP 主机' 426 'smtp.163.com'
$mailPort = Add-Field $form 'SMTP 端口' 462 '465'
$mailUser = Add-Field $form 'SMTP 用户名' 498 ''
$mailPassword = Add-Field $form 'SMTP 授权码' 534 '' $true
$mailFrom = Add-Field $form '发件地址' 570 ''

$secure = New-Object System.Windows.Forms.CheckBox
$secure.Text = '站点使用 HTTPS（启用 Secure Cookie）'
$secure.Left = 175
$secure.Top = 607
$secure.Width = 270
$form.Controls.Add($secure)

$startAfter = New-Object System.Windows.Forms.CheckBox
$startAfter.Text = '部署完成后启动后端'
$startAfter.Left = 445
$startAfter.Top = 607
$startAfter.Width = 165
$form.Controls.Add($startAfter)

$deploy = New-Object System.Windows.Forms.Button
$deploy.Text = '开始部署'
$deploy.Left = 475
$deploy.Top = 630
$deploy.Width = 115
$deploy.Height = 32
$form.Controls.Add($deploy)

$browse.Add_Click({
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = '选择星雨笔录安装目录'
    $dialog.SelectedPath = $install.Text
    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        $install.Text = $dialog.SelectedPath
    }
})

$deploy.Add_Click({
    try {
        if ([string]::IsNullOrWhiteSpace($install.Text) -or [string]::IsNullOrWhiteSpace($database.Text) -or
            [string]::IsNullOrWhiteSpace($mysqlUser.Text) -or [string]::IsNullOrWhiteSpace($siteUrl.Text)) {
            throw '安装目录、数据库名、数据库用户和站点地址不能为空。'
        }
        $deploy.Enabled = $false
        $form.Cursor = [System.Windows.Forms.Cursors]::WaitCursor
        $values = [pscustomobject]@{
            InstallPath = $install.Text
            MySqlHost = $mysqlHost.Text
            MySqlPort = [int]$mysqlPort.Text
            Database = $database.Text
            MySqlUser = $mysqlUser.Text
            MySqlPassword = $mysqlPassword.Text
            BackendPort = [int]$backendPort.Text
            SiteUrl = $siteUrl.Text.TrimEnd('/')
            AdminEmail = $adminEmail.Text
            MailHost = $mailHost.Text
            MailPort = [int]$mailPort.Text
            MailUser = $mailUser.Text
            MailPassword = $mailPassword.Text
            MailFrom = $mailFrom.Text
            SecureCookie = $secure.Checked
            StartAfterDeploy = $startAfter.Checked
        }
        $target = Write-DeploymentFiles $values
        [System.Windows.Forms.MessageBox]::Show("部署文件已生成：`n$target`n`n请阅读 [部署后请阅读.txt] 并配置 Nginx。", '部署完成', 'OK', 'Information') | Out-Null
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, '部署失败', 'OK', 'Error') | Out-Null
    } finally {
        $form.Cursor = [System.Windows.Forms.Cursors]::Default
        $deploy.Enabled = $true
    }
})

[void]$form.ShowDialog()
