# Star Rain Notes V1 — Production Deployment Guide

> 对应冻结规范 `06-testing-deployment.md`。V1 不使用 Docker / Kubernetes：
> Linux + Nginx + HTTPS + Spring Boot JAR (systemd) + MySQL 8.0.26 + mysqldump。

## 1. 架构

```text
Internet
   ↓ HTTPS
Nginx (deploy/nginx/star-rain-notes.conf)
   ├── /            → Vue dist (SPA, History Router fallback)
   ├── /assets/     → 构建产物（缺失 → 真实 404）
   ├── /api/*       → 反代 Spring Boot :24680
   ├── /uploads/*   → 媒体目录静态服务（目录不可执行）
   └── /actuator/health → 健康检查（仅 {"status":"UP"}）

Spring Boot (systemd, /opt/star-rain-notes/app.jar)
   ↓
MySQL 8.0.26 (star_rain_notes)
```

目标目录（06 §7）：

```text
/opt/star-rain-notes/            app.jar
/srv/star-rain-notes/web/        Vue dist 内容
/srv/star-rain-notes/uploads/    媒体目录（不可执行）
/var/log/star-rain-notes/        应用日志
/etc/star-rain-notes/            star-rain-notes.env（密钥，chmod 600）
/var/backups/star-rain-notes/    备份
```

## 2. 前置

- Linux（Ubuntu 22.04/Debian 12 或等效）、JDK 21、Nginx、MySQL 8.0.x
  （生产目标 8.0.26；V1 已在 8.0.34 高度一致环境全量验证）。
- 域名 + DNS 指向服务器；开放 80/443。

## 3. 构建（本地或 CI，产物与代码同源）

```bash
# 后端生产 JAR
cd backend && mvn -B -ntp clean package -DskipTests

# 前端生产 dist
cd frontend && npm ci && npm run type-check && npm run build
```

## 4. 服务器安装步骤

```bash
# 1) 用户与目录
sudo useradd -r -m -s /usr/sbin/nologin starrain
sudo mkdir -p /opt/star-rain-notes \
             /srv/star-rain-notes/web \
             /srv/star-rain-notes/uploads \
             /var/log/star-rain-notes \
             /etc/star-rain-notes \
             /var/backups/star-rain-notes
sudo chown -R starrain:starrain /opt/star-rain-notes /srv/star-rain-notes \
                                /var/log/star-rain-notes /var/backups/star-rain-notes

# 2) 上传产物
#    app.jar → /opt/star-rain-notes/app.jar
#    frontend/dist/* → /srv/star-rain-notes/web/

# 3) 媒体目录不可执行（06 §8：Public Media 目录必须不可执行）
sudo chmod -R a-x /srv/star-rain-notes/uploads
# 更严格做法：将 /srv/star-rain-notes/uploads 挂载为 noexec
```

### MySQL

```sql
CREATE DATABASE star_rain_notes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'star_rain'@'127.0.0.1' IDENTIFIED BY '<随机密码>';
GRANT ALL PRIVILEGES ON star_rain_notes.* TO 'star_rain'@'127.0.0.1';
FLUSH PRIVILEGES;
```

Flyway 会在应用首次启动时自动执行 `V1__init_schema.sql` / `V2__seed_system_singletons.sql`。

### 环境变量（密钥不外泄）

```bash
sudo mkdir -p /etc/star-rain-notes
sudo cp deploy/env/star-rain-notes.env.example /etc/star-rain-notes/star-rain-notes.env
sudo chmod 600 /etc/star-rain-notes/star-rain-notes.env
sudo nano /etc/star-rain-notes/star-rain-notes.env   # 填入 MYSQL_PASSWORD、APP_SETUP_TOKEN
```

### systemd

```bash
sudo cp deploy/systemd/star-rain-notes.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now star-rain-notes
systemctl status star-rain-notes
tail -f /var/log/star-rain-notes/star-rain-notes.log
```

### Nginx + HTTPS

```bash
sudo cp deploy/nginx/star-rain-notes.conf /etc/nginx/sites-available/star-rain-notes.conf
sudo sed -i 's/example.com/你的域名/g' /etc/nginx/sites-available/star-rain-notes.conf
sudo ln -s /etc/nginx/sites-available/star-rain-notes.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 证书（certbot 示例；私钥绝不提交）
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d 你的域名
```

## 5. 首次初始化（06 §10）

1. 在 `star-rain-notes.env` 设置随机 `APP_SETUP_TOKEN`（`openssl rand -hex 32`）。
2. 启动应用，访问 `https://你的域名/admin/setup`。
3. 使用 Setup Token 创建唯一管理员（用户名 ≥3 字符，密码 ≥8 字符）。
4. 确认 `admin_user` 表中存在记录。
5. **从生产环境移除 `APP_SETUP_TOKEN` 并重启**——此后 setup 被禁用。

不允许默认口令：`admin/admin`、`admin/123456`。

## 6. 健康检查与运维

```bash
curl -fsS https://你的域名/actuator/health        # {"status":"UP"}
journalctl -u star-rain-notes -f                  # systemd 日志
sudo systemctl restart star-rain-notes            # 重启
```

生产只暴露 `/actuator/health`（`show-details: never`，不泄露 environment/db url/路径）。

## 7. 备份（06 §11）

```bash
sudo cp deploy/scripts/backup.sh /opt/star-rain-notes/backup.sh
sudo chmod +x /opt/star-rain-notes/backup.sh
sudo crontab -e -u starrain
# 每天 02:30 备份
30 2 * * * /opt/star-rain-notes/backup.sh >> /var/log/star-rain-notes/backup.log 2>&1
```

恢复演练（至少定期验证一次）：

```bash
zcat /var/backups/star-rain-notes/star_rain_notes-YYYYMMDD-HHMMSS.sql.gz \
  | mysql -h 127.0.0.1 -u star_rain -p star_rain_notes
tar -xzf /var/backups/star-rain-notes/media-YYYYMMDD-HHMMSS.tar.gz -C /srv/star-rain-notes/
# 恢复后记得重新 chmod -R a-x uploads
```

## 8. 上线 Smoke Test（06 §5）

见仓库根目录 `scripts/smoke-test.ps1`（本地系统级执行脚本）；服务器上按同一清单人工/脚本执行：

1. Open Home → `/api/v1/public/home` 200
2. Admin Login → `POST /api/v1/auth/login` 200
3. Create Blog Draft → `POST /api/v1/admin/blog/posts` 201
4. Save → `PUT /api/v1/admin/blog/posts/{id}` 200
5. Publish → `POST .../posts/{id}/publish` 2xx
6. Visit Public Blog → `GET /api/v1/public/blog/posts/{slug}` 200
7. Search Blog → `GET /api/v1/public/search?q=…` 命中
8. Edit Blog → `PUT` 200
9. Public Page Updated → 公共详情出现新内容
10. Withdraw → `POST .../withdraw` 2xx
11. Public Detail 404 → `GET` 公共详情 404
12. Search Result disappears → 搜索不再命中
13. Republish → `POST .../publish` 2xx
14. Public Detail returns → `GET` 200
15. Logout → `POST /api/v1/auth/logout` 204
16. Anonymous Admin API → `GET /api/v1/admin/dashboard` 401

同时验证：Tutorial（建分类→建教程→建分组/章节→发布章节→发布教程→公共详情/章节可读）、Portfolio（建项目→发布→公共可读）、About（保存→公共可读）、Media（上传→`GET /uploads/...` 匿名 200）。

## 9. SEO / SPA 404 策略（06 §13）

- `/assets/*` 缺失 → nginx 真实 404（`try_files $uri =404`）。
- `/api/*` 不存在的资源 → 后端 RFC 9457 Problem Details 404。
- `/uploads/*` 缺失 → 404。
- 其余未知前端路由 → 落到 `index.html`，由 Vue 404 页面渲染（`noindex`，已随路由 meta 注入）。
- `robots.txt` / `sitemap.xml` 在 `frontend/public/`，构建后进入 dist；**上线前把其中的占位域名替换为真实 HTTPS 域名**。

## 10. 本机（Windows 开发机）已完成的验证与真实服务器待办

本机已自动完成：

- 后端全量回归（204 + 3 新增媒体公开服务测试，全部通过）
- 前端 type-check + production build
- 后端 production package（`mvn package` JAR）
- 本地生产 profile（`prod`）+ 独立 smoke 数据库上的**系统级 Smoke Test**（16 步 + 四域关键验证，脚本 `scripts/smoke-test.ps1`）
- 部署产物齐全（本目录）

真实 Linux 服务器仍需执行（本机无法模拟）：

1. 第 4 节全部服务器安装步骤（用户/目录/MySQL/systemd/Nginx 安装）
2. certbot 签发 HTTPS 证书并配置
3. `/etc/star-rain-notes/star-rain-notes.env` 写入真实密钥（chmod 600）
4. 首次 Setup 创建管理员后移除 `APP_SETUP_TOKEN`
5. 配置 cron 定时备份并做一次恢复演练
6. 服务器上执行 §8 Smoke Test
7. 替换 `robots.txt` / `sitemap.xml` 占位域名

> 不得提交：DB 密码、APP_SETUP_TOKEN、API keys、TLS 私钥（`deploy/.gitignore` 与根 `.gitignore` 已排除 `.env`/`*.key`/`*.pem`）。
