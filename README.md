# 星雨笔录 · Star Rain Notes

个人知识系统：教程、博客、作品、英语词汇与个人主页一体化管理。模块化单体架构，前后端分离。

## 技术栈

- **后端**：JDK 21 · Spring Boot 3.5 · Spring Web MVC · Spring Security · MyBatis-Plus · MySQL 8.0 · Flyway · Maven
- **前端**：Vue 3 · TypeScript · Vite · Vue Router · Pinia · Axios · Element Plus
- **部署**：Linux · Nginx · HTTPS · Spring Boot JAR (systemd) · mysqldump

## 目录结构

```
backend/   Spring Boot 后端（业务模块：auth/blog/english/media/portfolio/profile/search/site/tutorial/vocabulary）
frontend/  Vue 3 前端（public 前台 + admin 后台）
deploy/    Nginx/systemd/环境变量/备份脚本（部署配置）
```

## 本地运行

### 后端

```bash
cd backend
# 需要本机 MySQL（默认库 star_rain_notes，可在 application-local.yml 配置）
mvn spring-boot:run          # 默认端口 24680
```

### 前端

```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173，代理 /api 到 24680
```

首次访问 `http://localhost:5173/admin/setup` 用 `APP_SETUP_TOKEN` 创建管理员。

### 验证

```bash
cd frontend && npm run type-check && npm run build
cd backend  && mvn test
```

## 文档索引

- `deploy/README-BT.md`：宝塔面板部署指南
- `deploy/README.md`：手工 Nginx + systemd 部署指南

> 本包为纯净源码导出：仅含源代码、构建配置与部署脚本，不含开发规格文档与内容源数据。
