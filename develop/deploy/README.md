# 星雨笔录生产部署指南（2 核 2 GB ECS）

本项目采用 Nginx + Vue 静态文件 + Spring Boot JAR + MySQL 8 的单体部署，不需要 Docker、Redis、Elasticsearch 或消息队列。

## 1. 上线前准备

- Ubuntu 22.04/24.04 或同类 Linux ECS，系统盘至少保留 10 GB 可用空间。
- 域名完成备案（中国大陆公网服务需要）并将 A 记录解析到 ECS 公网 IP。
- 阿里云安全组只开放 SSH（建议限制来源 IP）、TCP 80、TCP 443；不要开放 24680 和 3306。
- 163 邮箱已开启 SMTP 服务并取得客户端授权码（不是邮箱登录密码）。
- 发布提交已经通过后端全量测试、前端类型检查和生产构建。

```text
Internet -> HTTPS :443 -> Nginx
                         |- /assets 与 SPA -> /srv/star-rain-notes/web
                         |- /uploads        -> /srv/star-rain-notes/uploads
                         `- /api            -> 127.0.0.1:24680 Spring Boot
                                                    `-> 127.0.0.1:3306 MySQL
```

## 2. 本机构建

```bash
cd backend
mvn -B -ntp clean test
mvn -B -ntp package -DskipTests

cd ../frontend
npm ci
npm run type-check
npm run build
```

上传后端 JAR、`frontend/dist/` 全部文件和 `deploy/` 目录。

## 3. ECS 安装与目录

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless mysql-server nginx certbot python3-certbot-nginx curl rsync
sudo useradd --system --home /opt/star-rain-notes --shell /usr/sbin/nologin starrain || true
# Ubuntu/Debian 官方 Nginx worker 使用 www-data；让它只通过共享组读取上传文件。
sudo usermod -aG starrain www-data
sudo mkdir -p /opt/star-rain-notes /srv/star-rain-notes/web \
  /srv/star-rain-notes/uploads /var/log/star-rain-notes \
  /var/backups/star-rain-notes /etc/star-rain-notes /var/www/certbot
sudo chown -R starrain:starrain /opt/star-rain-notes /srv/star-rain-notes \
  /var/log/star-rain-notes /var/backups/star-rain-notes
sudo chmod 750 /opt/star-rain-notes /var/log/star-rain-notes
sudo chmod 2750 /srv/star-rain-notes/uploads

sudo install -o starrain -g starrain -m 0640 app.jar /opt/star-rain-notes/app.jar
sudo rsync -a --delete frontend-dist/ /srv/star-rain-notes/web/
sudo chown -R starrain:starrain /srv/star-rain-notes/web
sudo find /srv/star-rain-notes/web -type d -exec chmod 755 {} \;
sudo find /srv/star-rain-notes/web -type f -exec chmod 644 {} \;
sudo systemctl restart nginx
```

## 4. MySQL 初始化

进入 `sudo mysql` 后执行：

```sql
CREATE DATABASE star_rain_notes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'star_rain'@'127.0.0.1' IDENTIFIED BY '替换为随机强密码';
GRANT ALL PRIVILEGES ON star_rain_notes.* TO 'star_rain'@'127.0.0.1';
FLUSH PRIVILEGES;
```

不要向公网开放 3306。应用首次启动时 Flyway 自动迁移空库；若迁移校验失败，应停止上线并核查数据库，禁止手改迁移历史。

2 GB 机器可参考以下保守配置（通常在 `/etc/mysql/mysql.conf.d/mysqld.cnf`）。先备份原文件，只在确认现状后调整：

```ini
[mysqld]
bind-address=127.0.0.1
max_connections=40
innodb_buffer_pool_size=384M
innodb_buffer_pool_instances=1
tmp_table_size=32M
max_heap_table_size=32M
```

## 5. 真实 163 邮件与生产配置

```bash
sudo cp deploy/env/star-rain-notes.env.example /etc/star-rain-notes/star-rain-notes.env
sudo chown root:starrain /etc/star-rain-notes/star-rain-notes.env
sudo chmod 640 /etc/star-rain-notes/star-rain-notes.env
sudo nano /etc/star-rain-notes/star-rain-notes.env
```

至少填写：

```dotenv
MYSQL_PASSWORD=数据库强密码
APP_SUPER_ADMIN_EMAIL=你的真实邮箱
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=发件163邮箱
MAIL_AUTH_CODE=163客户端授权码
MAIL_FROM=发件163邮箱
MAIL_SSL_ENABLED=true
MAIL_BASE_URL=https://你的真实域名
SESSION_COOKIE_SECURE=true
```

`MAIL_BASE_URL` 决定邀请邮件中的注册链接，必须为外网可访问的 HTTPS 根地址，不能保留 localhost。环境文件不进入 Git，不要在聊天、截图或日志中展示授权码。

## 6. 启动后端

```bash
sudo cp deploy/systemd/star-rain-notes.service /etc/systemd/system/
sudo cp deploy/logrotate/star-rain-notes /etc/logrotate.d/star-rain-notes
sudo systemctl daemon-reload
sudo systemctl enable --now star-rain-notes
sudo systemctl status star-rain-notes --no-pager
sudo tail -n 200 /var/log/star-rain-notes/star-rain-notes.log
curl -fsS http://127.0.0.1:24680/actuator/health
```

后端默认只绑定 `127.0.0.1`；JDBC 池最大 4、Tomcat 工作线程最大 32，systemd 限制 Java 进程为 768 MB、堆上限为 512 MB。

## 7. Nginx、域名与 HTTPS

仓库配置引用证书路径，因此先停止 Nginx，使用 standalone 模式取得证书，再安装完整配置：

```bash
sudo systemctl stop nginx
sudo certbot certonly --standalone -d 你的域名
sudo cp deploy/nginx/star-rain-notes.conf /etc/nginx/sites-available/star-rain-notes.conf
sudo sed -i 's/example.com/你的域名/g' /etc/nginx/sites-available/star-rain-notes.conf
sudo ln -sfn /etc/nginx/sites-available/star-rain-notes.conf /etc/nginx/sites-enabled/star-rain-notes.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl start nginx
sudo certbot renew --dry-run
```

Nginx multipart 上限为 55 MB；后端业务限制仍为图片 10 MB、PDF 20 MB、音频 50 MB。

## 8. 首次激活超级管理员

本版本不使用旧 `/admin/setup` 或 `APP_SETUP_TOKEN`：

1. 访问 `https://你的域名/admin/activate`。
2. 发送验证码到 `APP_SUPER_ADMIN_EMAIL`，输入验证码并设置强密码。
3. 登录 `/admin/login`，确认用户、邀请、审核和审计入口可用。
4. 邀请测试邮箱，确认链接域名正确、手机可打开且验证码注册成功。

超级管理员激活成功后，激活接口返回 410，不能创建第二个超级管理员。

## 9. 备份与恢复演练

```bash
sudo install -o starrain -g starrain -m 0750 deploy/scripts/backup.sh /opt/star-rain-notes/backup.sh
sudo -u starrain /opt/star-rain-notes/backup.sh
sudo crontab -u starrain -e
# 每天 02:30
30 2 * * * /opt/star-rain-notes/backup.sh >> /var/log/star-rain-notes/backup.log 2>&1
```

脚本提供互斥锁、临时文件、原子改名和 gzip/tar 校验，默认保留 14 天，磁盘可用空间低于 1 GB 时拒绝执行。上线前恢复一次到临时数据库：

```bash
sudo mysql -e "CREATE DATABASE star_rain_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
zcat /var/backups/star-rain-notes/star_rain_notes-时间.sql.gz \
  | mysql -h127.0.0.1 -ustar_rain -p star_rain_restore
sudo mysql -e "DROP DATABASE star_rain_restore"
```

## 10. 健康检查、验收与回滚

```bash
sudo install -m 0755 deploy/scripts/health-check.sh /opt/star-rain-notes/health-check.sh
/opt/star-rain-notes/health-check.sh https://你的域名
```

人工验证：亮/暗主题、390/1024/1440 宽度、邮件激活和邀请、普通管理员权限与审核、教程/博客/英语阅读、词汇分页，以及 50 MB 内音频上传和播放。

升级前先备份，并保留上一版 JAR 与前端目录。健康检查失败时恢复匹配的上一版前后端；包含数据库迁移的版本不得只回滚 JAR，必须按上线前备份处理数据库。

## 11. 发布门槛

- 后端全量测试、前端 type-check/build 全部通过。
- 24680/3306 公网不可达，80 跳转 443，证书续期演练成功。
- 163 邮件激活、邀请、忘记密码均真实收信通过。
- 禁用管理员后，其现有 Session 下一次请求立即返回 401。
- 完成数据库备份校验和临时库恢复。
- 观察至少 30 分钟：Java RSS 不持续增长、无连接池耗尽、日志无重复异常。
