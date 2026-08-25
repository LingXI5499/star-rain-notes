# 超级管理员激活与本地验证

本后端已上线「真实邮箱 + 邀请注册」账号体系（阶段一）。超级管理员**不内置默认密码**、**不使用 setup token**，必须通过 **163 邮箱验证码**完成首次激活。

## 一、两种端口（别搞混）
- **后端 (Spring Boot)**：`24680`，只提供 `/api/**`（以及 `/uploads` 静态）。
- **前端 (Vite dev)**：`5173`，浏览器打开的是它，并把 `/api` 代理到 `24680`。

> 激活页 **`/admin/activate`** 是前端路由，必须访问 `http://localhost:5173/admin/activate`，不要访问后端端口 `24680/admin/activate`（那会返回 401 JSON）。

## 二、本地开发环境激活（全新库）
1. **准备一个全新数据库**（确保没有已激活的超级管理员）：
   - 可用独立库名，例如：`CREATE DATABASE star_rain_notes_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`
   - 并把 `backend/src/main/resources/application-local.yml` 的 `spring.datasource.url` 指向它（开发用）。
2. **配置真实 SMTP**：写入 gitignored 的 `backend/application-local-secret.yml`（后端通过 `spring.config.import: optional:file:./application-local-secret.yml` 读取）：
   ```yaml
   app:
     account:
       mail:
         host: smtp.163.com
         port: 465
         username: 19735023257@163.com   # 发码邮箱
         auth-code: 你的163授权码         # 不是登录密码；只写这里/环境变量，不进 Git/日志
         from: 19735023257@163.com
         ssl-enabled: true
         base-url: http://localhost:5173
   ```
   固定超管邮箱已在 `application-local.yml` 设为 `14717895499@163.com`。
3. **启动后端**：`cd backend && mvn spring-boot:run`（24680；首次启动 Flyway 会把空库迁移到最新版本）。
4. **启动前端**：`cd frontend && npm run dev`（5173）。
5. **浏览器打开** `http://localhost:5173/admin/activate`。
   - 「发送验证码」→ 163 会发 6 位验证码到 `14717895499@163.com`（10 分钟有效、每邮箱每 60 秒一次、每小时最多 5 次）。
   - 输入验证码 + 设置首次密码（≥10 位）+ 确认 → 激活成功 → 跳转登录。
6. **登录**：`http://localhost:5173/admin/login` 用 `14717895499@163.com` + 该密码。

## 三、重新完整测试激活
如果某个库**已经激活**了超管，激活页会显示"已激活"，后续激活接口返回 `410 SUPER_ADMIN_ALREADY_ACTIVATED`（符合设计）。要重测，请对**全新库**操作，或先清空：
```sql
USE star_rain_notes_dev;
DELETE FROM email_verification_challenge;
DELETE FROM admin_invitation;
DELETE FROM admin_audit_log;
DELETE FROM user_account;
```

## 四、生产环境（部署到服务器）
- 不使用 local 文件；用**环境变量**注入，例如：
  ```
  APP_SUPER_ADMIN_EMAIL=14717895499@163.com
  MAIL_HOST=smtp.163.com
  MAIL_PORT=465
  MAIL_USERNAME=19735023257@163.com
  MAIL_AUTH_CODE=你的163授权码
  MAIL_FROM=19735023257@163.com
  MAIL_SSL_ENABLED=true
  MAIL_BASE_URL=https://你的域名
  ```
- 首次访问 `https://你的域名/admin/activate` 完成激活；之后初始化入口永久关闭。
- 授权码/密码/验证码永不出现在日志、API 响应或 Git。

## 五、测试与 CI（无需真实 SMTP）
集成测试用 `@MockBean MailGateway` 捕获验证码，**不连真实 SMTP**；因此 `mvn test`、CI 不依赖授权码。