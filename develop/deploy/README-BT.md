# 星雨笔录：阿里云 ECS + 宝塔面板逐操作部署指南

本文给出一条确定的生产路径：阿里云中国内地 ECS（Ubuntu 22.04/24.04，2 核 2 GB）+ 宝塔 Nginx/MySQL + systemd 托管 Spring Boot。域名按当前项目填写为 `yulanlin.cn`。前端、上传目录、JAR、密钥彼此隔离，不使用 Docker、PHP、Redis。

最终链路：

```text
浏览器 -> 80/443 -> 宝塔 Nginx
                     |- /、/assets -> /srv/star-rain-notes/web
                     |- /uploads   -> /srv/star-rain-notes/uploads
                     `- /api       -> 127.0.0.1:24680 Spring Boot
                                            `-> 127.0.0.1:3306 MySQL 8
```

## 0. 部署前必须准备好

1. 阿里云 ECS 公网 IP。
2. 已备案域名 `yulanlin.cn`。中国内地服务器必须先完成 ICP 备案；网站开通后 30 日内办理公安联网备案。
3. 一个真实的超级管理员邮箱。
4. 一个已开启 SMTP 的 163 邮箱及“客户端授权码”；授权码不是邮箱登录密码。
5. 本机已安装 JDK 21、Maven、Node.js/npm。

全文中的数据库密码、邮箱授权码只填写到服务器 `/etc/star-rain-notes/star-rain-notes.env`，不要粘贴到 Git、聊天、截图或前端目录。

## 1. 本机完成发布验收和构建

在 Windows PowerShell 进入项目根目录 `E:\star-rain`，依次执行：

```powershell
cd E:\star-rain\backend
mvn -B -ntp clean test
if ($LASTEXITCODE -ne 0) { throw '后端测试失败，停止发布' }
mvn -B -ntp package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'JAR 构建失败，停止发布' }

cd E:\star-rain\frontend
npm ci
npm run type-check
if ($LASTEXITCODE -ne 0) { throw '前端类型检查失败，停止发布' }
npm run build
if ($LASTEXITCODE -ne 0) { throw '前端构建失败，停止发布' }
```

准备上传包：

```powershell
cd E:\star-rain
New-Item -ItemType Directory -Force release | Out-Null
Copy-Item backend\target\star-rain-notes-backend-0.1.0-SNAPSHOT.jar release\app.jar -Force
Compress-Archive -Path frontend\dist\* -DestinationPath release\frontend-dist.zip -Force
Compress-Archive -Path deploy\* -DestinationPath release\deploy.zip -Force
Get-FileHash release\app.jar,release\frontend-dist.zip,release\deploy.zip -Algorithm SHA256
```

记录三个 SHA256。服务器上传完成后还要再次计算并比对，避免传输损坏。

## 2. 阿里云控制台配置

### 2.1 配置安全组

1. 登录阿里云控制台。
2. 打开“云服务器 ECS”→“实例与镜像”→“实例”。
3. 单击目标实例 ID →“安全组”→目标安全组 →“配置规则”→“入方向”→“手动添加”。
4. 添加以下规则：

| 协议 | 端口 | 来源 | 用途 |
|---|---:|---|---|
| TCP | 22 | 你的固定公网 IP `/32` | SSH；不要长期设为 `0.0.0.0/0` |
| TCP | 80 | `0.0.0.0/0` | HTTP 与证书校验 |
| TCP | 443 | `0.0.0.0/0` | HTTPS |
| TCP | 宝塔安装后显示的面板端口 | 你的固定公网 IP `/32` | 只允许自己管理面板 |

不要放行 `24680`、`3306`。如果已有 3306 或 24680 的公网放行规则，删除它们。

### 2.2 配置 DNS

1. 打开“云解析 DNS”→“公网权威解析”→单击 `yulanlin.cn` →“解析设置”。
2. 单击“添加记录”，添加根域名：记录类型 `A`，主机记录 `@`，记录值填 ECS 公网 IP，TTL 保持默认。
3. 再添加 `www`：记录类型 `A`，主机记录 `www`，记录值同一公网 IP。
4. 本机执行：

```powershell
Resolve-DnsName yulanlin.cn
Resolve-DnsName www.yulanlin.cn
```

两个结果都应出现 ECS 公网 IP。修改已有记录时，等待时间至少覆盖原 TTL。

## 3. 首次登录服务器并做基础设置

PowerShell 执行：

```powershell
ssh root@你的ECS公网IP
```

在服务器终端执行：

```bash
apt update
apt upgrade -y
timedatectl set-timezone Asia/Shanghai
apt install -y curl wget unzip rsync openssl ca-certificates acl
hostnamectl
timedatectl
free -h
df -h /
```

2 GB 机器建议准备 2 GB swap。先执行 `swapon --show`；只有没有任何输出时才执行：

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
swapon --show
```

系统升级提示需要重启时执行 `reboot`，等待约一分钟后重新 SSH 登录。

## 4. 安装并加固宝塔面板

必须在未预装 Nginx/MySQL/PHP 的纯净系统上安装。执行宝塔当前官方正式版命令：

```bash
if [ -f /usr/bin/curl ]; then
  curl -sSO https://download.bt.cn/install/install_panel.sh
else
  wget -O install_panel.sh https://download.bt.cn/install/install_panel.sh
fi
bash install_panel.sh docscenter
```

安装脚本询问是否安装到 `/www` 时输入 `y`。结束后终端会显示面板 URL、用户名、密码和端口：

1. 立即在阿里云安全组新增“面板端口/你的 IP `/32`”；不要对全网开放面板端口。
2. 浏览器打开安装结果中的完整面板 URL。
3. 登录后打开“面板设置”：修改用户名和强密码；修改“安全入口”；开启面板 SSL；设置“授权 IP”为你的固定公网 IP。
4. 如果修改了面板端口，先在安全组放行新端口，再保存面板设置；确认新地址能登录后，删除旧端口规则。
5. 宝塔左侧“安全”中只放行 80、443 和实际面板端口。若系统 UFW 已启用，执行 `ufw status` 检查规则；不要同时开放 3306/24680。

## 5. 在宝塔安装运行环境

打开“软件商店”→“运行环境”：

1. 找到 Nginx，选择稳定版并“极速安装”。
2. 找到 MySQL，选择 **8.0.x** 并“极速安装”。不要选 5.7；迁移使用 `utf8mb4_0900_ai_ci`。
3. 不安装 PHP、Redis、Docker、phpMyAdmin（确有管理需求时再装 phpMyAdmin，且只从面板内访问）。

然后 SSH 执行：

```bash
apt install -y openjdk-21-jre-headless
java -version
/www/server/nginx/sbin/nginx -v
/www/server/mysql/bin/mysql --version
```

三项必须分别显示 Java 21、Nginx、MySQL 8.0。

## 6. 创建数据库

1. 宝塔左侧“数据库”→“MySQL”→“添加数据库”。
2. 数据库名填 `star_rain_notes`。
3. 用户名填 `star_rain`。
4. 单击密码框旁“随机”并保存密码；推荐使用十六进制强密码，避免环境文件转义问题。
5. 访问权限选“本地服务器”。
6. 字符集选 `utf8mb4`，单击“确定”。

不手工导入迁移 SQL。应用首次启动会由 Flyway 执行 JAR 内全部迁移。确认数据库没有公网开放：

```bash
ss -lntp | grep ':3306'
```

建议在 MySQL 配置中将 `bind-address` 设为 `127.0.0.1` 后重启 MySQL。2 GB 机器在“软件商店”→ MySQL →“设置”→“性能调整”中使用保守值：`innodb_buffer_pool_size=256M`、`max_connections=40`。保存后重启 MySQL，再确认状态正常。

## 7. 上传发布文件并安装目录

1. 宝塔左侧“文件”→进入 `/tmp`。
2. 新建目录 `star-rain-release`。
3. 进入该目录，上传本机 `E:\star-rain\release` 下的三个文件。
4. 宝塔“终端”或 SSH 执行校验，并与第 1 步记录的 SHA256 比对：

```bash
cd /tmp/star-rain-release
sha256sum app.jar frontend-dist.zip deploy.zip
```

5. 创建专用用户和目录。宝塔 Nginx worker 默认用户为 `www`，把它加入只读共享组：

```bash
id www
id starrain >/dev/null 2>&1 || useradd --system --home /opt/star-rain-notes --shell /usr/sbin/nologin starrain
usermod -aG starrain www
mkdir -p /opt/star-rain-notes /srv/star-rain-notes/web /srv/star-rain-notes/uploads
mkdir -p /var/log/star-rain-notes /var/backups/star-rain-notes /etc/star-rain-notes
chown -R starrain:starrain /opt/star-rain-notes /srv/star-rain-notes
chown -R starrain:starrain /var/log/star-rain-notes /var/backups/star-rain-notes
chmod 750 /opt/star-rain-notes /var/log/star-rain-notes /var/backups/star-rain-notes
chmod 2750 /srv/star-rain-notes/uploads
```

目录的 `x` 是“允许遍历”，不能对上传目录执行 `chmod -R a-x`。正确策略是目录 `2750`、普通文件无执行位、应用用户可写、Nginx 通过 `starrain` 组只读。

6. 安装文件：

```bash
cd /tmp/star-rain-release
install -o starrain -g starrain -m 0640 app.jar /opt/star-rain-notes/app.jar
rm -rf /tmp/star-rain-frontend /tmp/star-rain-deploy
mkdir /tmp/star-rain-frontend /tmp/star-rain-deploy
unzip -q frontend-dist.zip -d /tmp/star-rain-frontend
unzip -q deploy.zip -d /tmp/star-rain-deploy
rsync -a --delete /tmp/star-rain-frontend/ /srv/star-rain-notes/web/
chown -R starrain:starrain /srv/star-rain-notes/web
find /srv/star-rain-notes/web -type d -exec chmod 755 {} \;
find /srv/star-rain-notes/web -type f -exec chmod 644 {} \;
find /srv/star-rain-notes/uploads -type d -exec chmod 2750 {} \;
find /srv/star-rain-notes/uploads -type f -exec chmod 640 {} \;
```

JAR 和环境文件绝不能放到 `/srv/star-rain-notes/web`，否则错误的 Nginx 配置可能把后端程序或密钥直接下载给访客。

## 8. 填写生产环境变量

```bash
cp /tmp/star-rain-deploy/env/star-rain-notes.env.example /etc/star-rain-notes/star-rain-notes.env
chown root:starrain /etc/star-rain-notes/star-rain-notes.env
chmod 640 /etc/star-rain-notes/star-rain-notes.env
```

在宝塔“文件”中打开 `/etc/star-rain-notes/star-rain-notes.env`，至少把下面项目取消注释并填写真实值：

```dotenv
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=24680
SERVER_ADDRESS=127.0.0.1
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=star_rain_notes
MYSQL_USER=star_rain
MYSQL_PASSWORD=第6步保存的数据库密码
DB_POOL_MAX_SIZE=4
DB_POOL_MIN_IDLE=1
APP_SUPER_ADMIN_EMAIL=你的超级管理员邮箱
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=发件人163邮箱
MAIL_AUTH_CODE=163客户端授权码
MAIL_FROM=发件人163邮箱
MAIL_SSL_ENABLED=true
MAIL_BASE_URL=https://yulanlin.cn
MEDIA_STORAGE_DIR=/srv/star-rain-notes/uploads
SESSION_COOKIE_SECURE=true
TOMCAT_MAX_THREADS=32
TOMCAT_MIN_SPARE_THREADS=4
TOMCAT_MAX_CONNECTIONS=200
TOMCAT_ACCEPT_COUNT=50
TOMCAT_CONNECTION_TIMEOUT=10s
```

不要填写 `APP_SETUP_TOKEN`；生产环境已关闭旧 Setup 和用户名登录。保存后执行：

```bash
stat -c '%U %G %a %n' /etc/star-rain-notes/star-rain-notes.env
```

输出必须是 `root starrain 640`。

## 9. 安装并启动后端服务

```bash
cp /tmp/star-rain-deploy/systemd/star-rain-notes.service /etc/systemd/system/star-rain-notes.service
cp /tmp/star-rain-deploy/logrotate/star-rain-notes /etc/logrotate.d/star-rain-notes
systemctl daemon-reload
systemctl enable --now star-rain-notes
systemctl status star-rain-notes --no-pager
curl -fsS http://127.0.0.1:24680/actuator/health
ss -lntp | grep ':24680'
```

健康响应必须是 `{"status":"UP"}`，监听地址必须是 `127.0.0.1:24680`。若启动失败：

```bash
journalctl -u star-rain-notes -n 200 --no-pager
tail -n 200 /var/log/star-rain-notes/star-rain-notes.log
```

重点检查数据库密码、MySQL 8.0、环境文件权限和端口占用。首次成功启动后，在宝塔数据库中应能看到 `flyway_schema_history` 和业务表。

## 10. 在宝塔创建静态站点并申请 SSL

1. 宝塔左侧“网站”→“添加站点”。
2. 域名填写 `yulanlin.cn` 和 `www.yulanlin.cn`。
3. 根目录填写 `/srv/star-rain-notes/web`。
4. PHP 版本选“纯静态”，不创建 FTP，不在这里创建数据库。
5. 单击“确定”，先用 `http://yulanlin.cn` 验证能打开首页。
6. 单击站点名 →“SSL”→“Let's Encrypt”或“LiteSSL”→选择两个域名 →算法选 RSA2048 →文件验证 →“申请证书”。
7. 证书申请成功并确认 `https://yulanlin.cn` 可访问后，再开启“强制 HTTPS”。不要在证书申请前做强制跳转。

## 11. 替换为项目 Nginx 配置

1. 站点设置 →“配置文件”，先把原配置完整复制到本地备份。
2. 记下宝塔生成的 `ssl_certificate` 和 `ssl_certificate_key` 两行真实路径。
3. 用 `/tmp/star-rain-deploy/nginx/star-rain-notes.conf` 为主体：把 `example.com` 改为 `yulanlin.cn www.yulanlin.cn`；把证书两行替换为第 2 步路径；`root` 与上传 alias 保持 `/srv/star-rain-notes/...`；保留 `/api/`、55 MB 上传限制和 SPA `try_files`。
4. 把修改后的完整内容粘贴回配置文件并保存。
5. 宝塔终端执行：

```bash
/www/server/nginx/sbin/nginx -t
systemctl restart nginx 2>/dev/null || /etc/init.d/nginx restart
curl -I http://yulanlin.cn
curl -I https://yulanlin.cn
curl -fsS https://yulanlin.cn/actuator/health
```

HTTP 应 301 到 HTTPS；HTTPS 应 200；健康接口应返回 UP。若新版 Nginx 只警告 `listen ... http2` 已过时，可改为 `listen 443 ssl;`、`listen [::]:443 ssl;`，并在 server 块增加 `http2 on;`。

重启 Nginx 后它才会获得新的附加组权限。执行：

```bash
namei -l /srv/star-rain-notes/uploads
id www
```

## 12. 首次激活和业务链路验收

1. 访问 `https://yulanlin.cn/admin/activate`，发送验证码并完成超级管理员激活。
2. 打开 `/admin/login`，用邮箱和新密码登录。
3. 验证：首页、教程、博客、作品、搜索、英语词汇/语法/阅读/听力/写作、后台编辑与媒体上传。
4. 邀请一个测试管理员，检查邮件链接必须以 `https://yulanlin.cn` 开头；完成注册并登录。
5. 禁用该测试管理员，它的旧会话下一次请求必须立即得到 401。
6. 上传图片、PDF 和小于 50 MB 的音频，刷新公开页确认 `/uploads/...` 可读取。

安装自动验收脚本：

```bash
install -o root -g root -m 0755 /tmp/star-rain-deploy/scripts/health-check.sh /opt/star-rain-notes/health-check.sh
/opt/star-rain-notes/health-check.sh https://yulanlin.cn
```

脚本验证数据库健康、公开 API、Vue history、CSRF、未登录 401、旧 Setup 410、哈希资源、HSTS、nosniff 和缓存头。全部显示 `OK` 才算发布通过。

## 13. 配置备份并实际恢复一次

```bash
install -o starrain -g starrain -m 0750 /tmp/star-rain-deploy/scripts/backup.sh /opt/star-rain-notes/backup.sh
sudo -u starrain /opt/star-rain-notes/backup.sh
ls -lh /var/backups/star-rain-notes
```

宝塔“计划任务”→“添加任务”：任务类型选 Shell 脚本；名称 `star-rain-notes-nightly-backup`；每天 `02:30`；脚本内容：

```bash
sudo -u starrain /opt/star-rain-notes/backup.sh >> /var/log/star-rain-notes/backup.log 2>&1
```

首次上线必须恢复演练：

```bash
latest_dump="$(ls -1t /var/backups/star-rain-notes/star_rain_notes-*.sql.gz | head -n 1)"
/www/server/mysql/bin/mysql -uroot -p -e "CREATE DATABASE star_rain_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'star_rain_restore'@'127.0.0.1' IDENTIFIED BY '临时强密码'; GRANT ALL ON star_rain_restore.* TO 'star_rain_restore'@'127.0.0.1';"
zcat "$latest_dump" | /www/server/mysql/bin/mysql -h127.0.0.1 -ustar_rain_restore -p star_rain_restore
/www/server/mysql/bin/mysql -uroot -p -e "USE star_rain_restore; SHOW TABLES;"
/www/server/mysql/bin/mysql -uroot -p -e "DROP DATABASE star_rain_restore; DROP USER 'star_rain_restore'@'127.0.0.1';"
```

每条 `-p` 都会交互提示密码，不要把密码写进命令历史。看到恢复库包含迁移表和业务表后，才执行最后的清理命令。

## 14. 日常发布、回滚和监控

每次更新先重复第 1 步，然后在服务器执行：

```bash
sudo -u starrain /opt/star-rain-notes/backup.sh
stamp="$(date +%Y%m%d-%H%M%S)"
cp /opt/star-rain-notes/app.jar "/opt/star-rain-notes/app.jar.$stamp"
cp -a /srv/star-rain-notes/web "/srv/star-rain-notes/web.$stamp"
systemctl stop star-rain-notes
install -o starrain -g starrain -m 0640 /tmp/star-rain-release/app.jar /opt/star-rain-notes/app.jar
rm -rf /tmp/star-rain-frontend
mkdir /tmp/star-rain-frontend
unzip -q /tmp/star-rain-release/frontend-dist.zip -d /tmp/star-rain-frontend
rsync -a --delete /tmp/star-rain-frontend/ /srv/star-rain-notes/web/
chown -R starrain:starrain /srv/star-rain-notes/web
systemctl start star-rain-notes
/opt/star-rain-notes/health-check.sh https://yulanlin.cn
```

失败时恢复同一时间戳的 JAR 和前端。若新版本执行了 Flyway 迁移，不能只换回旧 JAR；必须停站并恢复升级前数据库备份，确保代码与数据库版本匹配。

上线后至少观察 30 分钟：

```bash
watch -n 5 'free -h; systemctl --no-pager --full status star-rain-notes | head -n 12'
tail -f /var/log/star-rain-notes/star-rain-notes.log
```

正常标准：Java RSS 不持续增长、无连接池超时、无重复 5xx、磁盘剩余大于 10 GB、证书自动续签启用。

## 15. 常见故障排查顺序

| 现象 | 依次操作 |
|---|---|
| 502 | `systemctl status star-rain-notes` → 本机 curl 健康接口 → 查 Java 日志 |
| API 404 | 检查 Nginx `location /api/`、`proxy_pass`，再执行 `nginx -t` |
| 刷新子路由 404 | 检查 `try_files $uri $uri/ /index.html` |
| 登录后 CSRF 403 | 必须 HTTPS；确认安全 Cookie；清理旧 Cookie 后重登 |
| 上传 413 | 检查 Nginx 55 MB；图片/PDF/音频业务上限分别为 10/20/50 MB |
| 媒体 403 | `id www` 应含 `starrain`；目录 2750、文件 640；重启 Nginx |
| 邀请链接是 localhost | 修正 `MAIL_BASE_URL` 后重启后端 |
| Flyway 报错 | 停止发布，核对 MySQL 8.0 和数据库配置；不要手改迁移历史 |
| 内存不足 | 保留 swap、JVM Xmx512m、MySQL buffer pool 256 MB，卸载未用软件 |

## 官方操作参考

- 宝塔安装：<https://docs.bt.cn/getting-started/quick-installation-of-bt-panel>
- 宝塔添加 MySQL 数据库：<https://docs.bt.cn/user-guide/database/mysql/add>
- 宝塔 SSL：<https://docs.bt.cn/user-guide/site/php/site-config/ssl>
- 阿里云安全组：<https://help.aliyun.com/zh/ecs/user-guide/start-using-security-groups>
- 阿里云 DNS A 记录：<https://help.aliyun.com/zh/dns/beginner-s-guide>
- 阿里云个人网站备案：<https://help.aliyun.com/zh/icp-filing/basic-icp-service/getting-started/quick-start-for-icp-filing-for-personal-websites>
