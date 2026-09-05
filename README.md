# 星雨笔录 · Star Rain Notes

> **当前版本：V1.1**（前端 Vue 3 + 后端 Spring Boot 3.5，前后端分离）

个人知识系统：教程、博客、作品、英语词汇与个人主页一体化管理。模块化单体架构，前后端分离。

## 技术栈

- **后端**：JDK 21 · Spring Boot 3.5 · Spring Web MVC · Spring Security · MyBatis-Plus · MySQL 8.0 · Flyway · Maven
- **前端**：Vue 3 · TypeScript · Vite · Vue Router · Pinia · Axios · Element Plus
- **部署**：Linux · Nginx · HTTPS · Spring Boot JAR (systemd) · mysqldump

## 目录结构

```
backend/   Spring Boot 后端（业务模块：auth/blog/english/media/portfolio/profile/search/site/tutorial/vocabulary）
frontend/  Vue 3 前端（public 前台 + admin 后台）
develop/   部署配置、开发文档、内容资料、发布产物与辅助脚本
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

首次访问 `http://localhost:5173/admin/activate`，通过配置的超级管理员邮箱验证码完成激活。

### 验证

```bash
cd frontend && npm run type-check && npm run build
cd backend  && mvn test
```

## 文档索引

- `develop/deploy/README-BT.md`：宝塔面板部署指南
- `develop/deploy/README.md`：手工 Nginx + systemd 部署指南

> 根目录保留当前前后端源码与主说明文档，其余开发、部署和发布资料统一收纳在 `develop/`。
