# 星雨笔录 · Star Rain Notes

星雨笔录是一套前后端分离的个人知识与学习管理平台，用于系统整理教程、博客、项目作品和英语学习内容。项目同时提供公开阅读站点与管理后台，适合作为个人知识库、学习档案或全栈实践项目。

当前开源版本：`v1.0.0`

## 主要功能

- 教程：知识体系、课程、目录分组、Markdown 章节和发布管理。
- 博客：文章、标签、封面、内容审核与公开阅读。
- 作品：项目案例、技术栈、状态、精选展示与详情页。
- 英语学习：8,505 条主题词汇、系统语法、阅读、听力、语音规则、写作、词族、学习组合和进度分析。
- 账号协作：超级管理员激活、管理员邀请、内容审核、审计日志和学习账号。
- 内容体验：Markdown 编辑、媒体库、站内搜索、亮暗主题、响应式布局和 SEO HTML 网关。

## 技术栈

### 后端

- JDK 21
- Spring Boot 3.5.16
- Spring Web MVC、Spring Security、Spring Validation、Spring Mail
- MyBatis-Plus 3.5.17
- MySQL 8、Flyway
- Spring Boot Actuator、springdoc-openapi
- Maven

### 前端

- Vue 3.5、TypeScript 5.9
- Vite 7、Vue Router、Pinia
- Axios、Element Plus
- Vditor、markdown-it、highlight.js、DOMPurify
- Vitest、Vue Test Utils

## 仓库结构

```text
backend/                  Spring Boot 后端源码与测试
frontend/                 Vue 前台、管理后台与前端测试
database/V1__baseline.sql MySQL 8 / Flyway 单一初始化基线
README.md                 本地安装、运行与验证说明
LICENSE                   MIT License
```

## 本地运行要求

请先安装：

- JDK 21
- Maven 3.9+
- Node.js 20.19+（或 22.12+）与 npm
- MySQL 8.0+

默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:24680`
- 数据库：`star_rain_notes`

## 1. 创建空数据库

使用 MySQL 客户端创建一个全新的空数据库：

```sql
CREATE DATABASE star_rain_notes
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

后端第一次启动时，Flyway 会自动执行 `database/V1__baseline.sql`，创建 53 张业务表并写入公共学习基础数据，无需手工导入 SQL。

> 此基线只适用于全新的空数据库，不能覆盖或升级已经执行过其他 Flyway 迁移链的数据库。

## 2. 配置本机私密信息

复制配置模板：

```text
backend/application-local-secret.example.yml
→ backend/application-local-secret.yml
```

在复制出的文件中填写本机 MySQL 密码、首次激活令牌、超级管理员邮箱和 SMTP 参数。真实文件已被 `.gitignore` 排除，不要提交、截图或发送给他人。

非敏感数据库参数也可以通过环境变量覆盖：

```text
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=star_rain_notes
MYSQL_USER=root
MYSQL_PASSWORD=本机数据库密码
```

如果暂时不配置 SMTP，应用可以启动，但邮箱验证码、管理员邀请与首次账号激活无法完成。

## 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动成功后可访问：

- 健康检查：`http://localhost:24680/actuator/health`
- OpenAPI：`http://localhost:24680/swagger-ui.html`

媒体文件默认保存在 `backend/uploads/`，该目录不会进入 Git。

## 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

Vite 会把 `/api`、`/actuator` 和 `/uploads` 转发到本地后端。需要修改地址时，将 `frontend/.env.example` 复制为 `.env.local` 后调整：

```text
VITE_API_TARGET=http://localhost:24680
VITE_SITE_ORIGIN=http://localhost:5173
```

## 5. 首次激活管理员

前后端启动后访问：

```text
http://localhost:5173/admin/activate
```

使用 `application-local-secret.yml` 中配置的超级管理员邮箱、激活令牌和收到的邮箱验证码完成初始化。系统不会在数据库脚本中创建默认管理员或默认密码。

## 测试与构建

后端集成测试需要独立的空 MySQL 测试库，推荐创建 `star_rain_notes_test`，并通过 `TEST_MYSQL_*` 环境变量提供连接信息：

```bash
cd backend
mvn test
mvn clean package
```

前端验证：

```bash
cd frontend
npm run type-check
npm test
npm run build
```

构建产物分别位于 `backend/target/` 和 `frontend/dist/`，两者均不会提交到仓库。

## 数据与安全说明

- 仓库不包含任何管理员、用户、验证码、邀请、媒体、博客、作品或个人学习记录。
- 初始化数据仅包含站点空单例、词汇、语法、英语标签和 CEFR 标准。
- 所有密码、SMTP 授权码、令牌、生产域名和机器路径必须由本地环境提供。
- 禁止把 `application-local-secret.yml`、`.env.local`、数据库导出或上传目录提交到 Git。

## License

[MIT License](LICENSE) © 2026 LingXI5499
