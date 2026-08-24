# 星雨笔录 · 宝塔面板部署指南（BaoTa Panel）

> 适用于：阿里云 ECS 2核2G / 40G 磁盘，域名 `yulanlin.cn`（ICP 备案已完成），系统 Ubuntu 22.04 / Debian 12 / Alibaba Cloud Linux 3 / CentOS 7.9。
> 与 `README.md`（纯手工 systemd 流程）等价，本文件为**宝塔面板操作版**。V1 不使用 Docker/K8s。

---

## 0. 前置条件（先做，缺一不可）

1. **ICP 备案已通过**（晋ICP备2026008281号-1）——大陆 ECS 未备案不能开 80/443。
2. **DNS**：把 `yulanlin.cn`（如需要 `www` 也加）解析到服务器公网 IP（A 记录），并等待生效。
3. **阿里云安全组**放行：`22`（SSH）、`80`、`443`。`3306` 不要对公网开放（数据库只给本机用）。
4. 记住：公安备案（晋公网安备14050002001992号）需在网站开通后 **30 天内** 提交。

---

## 1. 安装宝塔

SSH 登录服务器，按系统执行（宝塔官方最新命令以官网为准）：

```bash
# Ubuntu / Debian
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && bash install.sh
# CentOS / Alibaba Cloud Linux
# wget -O install.sh https://download.bt.cn/install/install_6.0.sh && bash install.sh
```

安装完成会打印面板地址 / 用户名 / 密码。浏览器登录面板，**立即修改面板密码与安全入口**。

---

## 2. 安装运行环境（宝塔「软件商店」）

| 软件 | 版本 | 说明 |
|---|---|---|
| Nginx | 1.24 / 1.26 | 网站与反向代理 |
| MySQL | **8.0.x** | 目标 8.0.26，8.0.3x 已验证兼容（utf8mb4_0900_ai_ci） |
| Java 项目管理器 | 最新 | 托管 Spring Boot JAR，需选择 **JDK 21**（本项目用 JDK 21；宝塔内置 JDK 无 21 时手动安装） |
| PHP | 不装 | 纯静态前端 + 后端 API，不需要 PHP |

**JDK 21**（宝塔 Java 项目管理器支持自定义 JDK 路径时）：
```bash
# Ubuntu/Debian
apt install -y openjdk-21-jdk-headless
# CentOS/Alibaba Cloud Linux
# yum install -y java-21-openjdk-headless
java -version   # 确认 21.x
```

> 2G 内存小贴士：装完环境后关掉宝塔里用不到的进程（如自带的 MySQL 版本就装一个；不要同时装 PHP/Redis 等未用服务）。

---

## 3. 创建数据库（宝塔「数据库」→ 添加数据库）

```
数据库名：  star_rain_notes
用户名：    star_rain
密码：      <生成一个随机强密码，记下来>
访问权限：  本地服务器（127.0.0.1）
字符集：    utf8mb4
```

**不需要手动导入任何 SQL** —— 后端首次启动时 Flyway 会自动执行 `V1~V4` 迁移（13 张表 + 词汇 8505 词 + 教程体系 5 分类/55 教程/828 分组/163 章节）。表级 collation 由迁移脚本显式指定为 `utf8mb4_0900_ai_ci`，与库默认值无关。

---

## 4. 构建产物（建议本地构建，2G 服务器不适合跑 Maven+Vite）

```bash
# 本地（你的电脑）
# 后端
cd backend && mvn -B -ntp clean package -DskipTests
# 产物: backend/target/star-rain-notes-backend-0.1.0-SNAPSHOT.jar

# 前端
cd frontend && npm ci && npm run type-check && npm run build
# 产物: frontend/dist/（整个目录内容）
```

用宝塔「文件」上传到服务器：

```
/www/wwwroot/yulanlin.cn/app.jar          ← 后端 JAR
/www/wwwroot/yulanlin.cn/                 ← 前端 dist 的全部内容（index.html、assets/ 等）
/www/wwwroot/yulanlin.cn/uploads/         ← 媒体上传目录（可先创建，应用会自己建）
```

---

## 5. 配置后端（宝塔「网站」→「Java项目」→ 添加项目）

| 项 | 值 |
|---|---|
| 项目名称 | star-rain-notes |
| 项目类型 | Spring Boot |
| 项目路径 | `/www/wwwroot/yulanlin.cn/app.jar` |
| 启动方式 | jar |
| 端口 | `24680` |
| JDK | 21 |
| 启动参数 | `-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 --server.port=24680` |
| 环境变量 | 见下方表格 |

环境变量（宝塔 Java 项目支持自定义环境变量，等价于手工版的 `/etc/star-rain-notes/star-rain-notes.env`）：

```text
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=star_rain_notes
MYSQL_USER=star_rain
MYSQL_PASSWORD=<第3步的密码>
DB_POOL_MAX_SIZE=6
DB_POOL_MIN_IDLE=1
APP_SETUP_TOKEN=<openssl rand -hex 32 生成的随机值>   # 首次初始化用，之后必须删除
MEDIA_STORAGE_DIR=/www/wwwroot/yulanlin.cn/uploads
SESSION_COOKIE_SECURE=true
```

> 若宝塔 Java 项目表单没有「环境变量」输入框，可在启动参数里加 `--spring.config.additional-location=` 或改用下方 systemd 方案：把 `deploy/env/star-rain-notes.env.example` 放 `/etc/star-rain-notes/star-rain-notes.env` 并用 `deploy/systemd/star-rain-notes.service` 托管（宝塔「计划任务」旁的手工 systemd 同样可用）。

添加后项目自动启动。验证：

```bash
curl -s http://127.0.0.1:24680/actuator/health   # {"status":"UP"}
```

---

## 6. 配置网站与 Nginx（宝塔「网站」→ 添加站点）

1. 添加站点：域名 `yulanlin.cn`（勾选 www 则一起），根目录 `/www/wwwroot/yulanlin.cn`，**纯静态**。
2. 「设置 → 配置文件」，把宝塔生成的 server 块内容替换为下面的配置（**保留宝塔生成的 `ssl_certificate` / `ssl_certificate_key` 路径行**；宝塔会在申请 SSL 后自动写入证书路径，所以可以先申请 SSL 再贴）：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name yulanlin.cn www.yulanlin.cn;
    return 301 https://$host$request_uri;   # 强制 HTTPS
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name yulanlin.cn www.yulanlin.cn;

    # 宝塔申请 Let's Encrypt 后自动生成/保留下面两行（勿删）
    # ssl_certificate     /www/server/panel/vhost/cert/yulanlin.cn/fullchain.pem;
    # ssl_certificate_key /www/server/panel/vhost/cert/yulanlin.cn/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    sendfile on;
    tcp_nopush on;
    etag on;
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 5;
    gzip_types application/javascript application/json application/manifest+json
               application/problem+json application/xml image/svg+xml
               text/css text/plain text/xml;

    client_max_body_size 25m;   # 媒体上传上限（图片10MB/PDF20MB/总25MB）

    # 安全头（06 §8: nosniff 必带）
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;

    # ---------- Vue SPA ----------
    root /www/wwwroot/yulanlin.cn;
    index index.html;
    charset utf-8;

    # 带哈希的构建资源：immutable + 真正 404
    location /assets/ {
        try_files $uri =404;
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    # ---------- 后端 API 反向代理（Spring Boot :24680）----------
    location /api/ {
        proxy_pass http://127.0.0.1:24680;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header Connection "";
        proxy_connect_timeout 3s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # 健康检查（只暴露 {"status":"UP"}）
    location /actuator/health {
        proxy_pass http://127.0.0.1:24680;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # ---------- 公开媒体（不可执行目录）----------
    location /uploads/ {
        alias /www/wwwroot/yulanlin.cn/uploads/;
        try_files $uri =404;
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    location = /index.html {
        try_files $uri =404;
        add_header Cache-Control "no-cache";
    }

    # ---------- SPA history 路由回退 ----------
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 禁止访问隐藏文件（如误传的 .env）
    location ~ /\. {
        deny all;
    }
}
```

3. 「设置 → SSL」申请 **Let's Encrypt** 证书（需域名解析已生效），勾选「强制 HTTPS」。
4. `chmod -R a-x /www/wwwroot/yulanlin.cn/uploads`（媒体目录不可执行，安全要求）。

---

## 7. 首次初始化（一次性）

1. 确认第 5 步的 `APP_SETUP_TOKEN` 已设置，且项目已重启（Flyway 迁移完成）。
2. 浏览器访问 `https://yulanlin.cn/admin/setup`，用 Setup Token 创建唯一管理员（用户名 ≥3 字符、密码 ≥8 字符）。
3. 确认可登录 `https://yulanlin.cn/admin/login`。
4. **立即删除 `APP_SETUP_TOKEN` 环境变量并重启项目** —— 之后 setup 页被禁用，不允许默认口令。

---

## 8. 健康检查与日志

```bash
curl -fsS https://yulanlin.cn/actuator/health    # {"status":"UP"}
```

- 后端日志：宝塔「Java项目」→ 项目日志；或 `tail -f /www/wwwroot/yulanlin.cn/logs/*.log`（宝塔默认日志路径）。
- 前端/代理错误：宝塔「网站 → 日志」。

---

## 9. 备份（宝塔「计划任务」）

1. **数据库备份**：宝塔计划任务 → 备份数据库 → 选择 `star_rain_notes`，每天 02:30，保留最近 7 份。
2. **媒体/上传目录备份**：计划任务 → 备份目录 → `/www/wwwroot/yulanlin.cn/uploads`。
3. （可选）整个站点目录含 JAR 每周备份一次。

> 手工版 `deploy/scripts/backup.sh` 在宝塔上同样可用（把 mysqldump 路径换成宝塔的 `/www/server/mysql/bin/mysqldump`）。

---

## 10. 升级流程（发新版时）

```bash
# 本地重新构建 → 上传覆盖
# 后端: app.jar 覆盖 → 宝塔 Java项目 → 重启
# 前端: dist 内容覆盖到 /www/wwwroot/yulanlin.cn → 无需重启 nginx（或 reload）
```

数据库结构变更由新版本内的 Flyway 迁移在启动时自动执行；**先备份再升级**。

---

## 11. 2G 内存优化清单（重要）

- JVM：`-Xms128m -Xmx512m`（第 5 步已含，降低常驻内存同时保留峰值空间）。
- MySQL：宝塔「数据库 → MySQL → 设置 → 性能调整」：`innodb_buffer_pool_size=256M`、`max_connections=100`，保存并重启 MySQL。
- 关闭不需要的进程：卸载 PHP、Redis 等未用软件；宝塔面板本身占用可接受。
- 若内存仍紧张：`free -h` 观察，必要时把 JVM 降到 `-Xmx384m`。

---

## 12. 常见问题

| 现象 | 处理 |
|---|---|
| `/api` 404 / 502 | 后端未启动：检查 Java 项目状态与 24680 端口；`curl http://127.0.0.1:24680/actuator/health` |
| 前端刷新子路由 404 | nginx `location / { try_files $uri $uri/ /index.html; }` 未生效（确认 root 与配置已保存） |
| 上传 413 | 缺 `client_max_body_size 25m;` 或阿里云安全组/宝塔防火墙拦截 |
| 上传 500（旧代码） | 确认 JAR 是最新构建（multipart 25MB 配置在 application.yml 内） |
| 首次启动报 Flyway/DB 连接失败 | 检查 `MYSQL_*` 环境变量与 MySQL 是否已启动；MySQL 8 用 `caching_sha2_password` 时 JDBC 参数已兼容 |
| 登录后 CSRF 403 | `SESSION_COOKIE_SECURE=true` + HTTPS 已开启即正常；若用 http 访问需改为 false |
| 域名无法访问 | 备案/安全组/DNS 未生效；`curl -I http://yulanlin.cn` 排查 |
