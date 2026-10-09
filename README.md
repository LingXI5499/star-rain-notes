# 星雨笔录 · Star Rain Notes

个人知识与作品展示系统，集教程、博客、作品、英语学习与内容管理于一体。当前版本采用 Vue 3 前端与 Spring Boot 多模块后端。

## 当前版本

**v2.0.0 — V2 首个正式源码版本（2026-10-09）**

V2 的源码、数据库迁移与启动脚本位于 [`star-rain-v2/`](star-rain-v2/)。[`main`](https://github.com/LingXI5499/star-rain-notes/tree/main) 为 GitHub 默认发布分支，`mainV2` 为 V2 集成分支，版本标签为 `v2.0.0`。

详细说明见 [V2 README](star-rain-v2/README.md)，更新内容见 [版本记录](star-rain-v2/CHANGELOG.md)。根目录的 `backend/`、`frontend/` 保留 V1 源码；既有 V1 标签继续保留。

## 功能概览

| 模块 | V2 功能 |
| --- | --- |
| 教程与学习 | 目录、章节、问题、练习、学习记录、掌握度、复习与学习计划 |
| 博客 | 主题、标签、时间线、Markdown 阅读与公开主题 SEO |
| 作品 | 内容区块、展示模板、真实域名与隔离运行的多页面静态原型 |
| 英语 | 主题词库、发音、学习计划、语法、双语阅读、私有写作及管理员原创公开 |
| 账户与媒体 | 邮箱验证、管理员邀请、固定角色权限、账户治理与媒体库 |
| 站点 | 公开路径与账户路径、昼夜主题、首页、关于页、分页与筛选 |
| 搜索与 SEO | 全站及快捷搜索、英语筛选、内容变更索引同步、SEO HTML 与站点地图 |

英语搜索覆盖词汇主题、单词、语法课程、语法课时、阅读和公开原创写作。阅读沿用公开状态与版权核查条件，私有写作不进入全站索引；启动时会补建已有英语内容索引。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、JavaScript、Vite、Vue Router、Pinia、Axios |
| 内容编辑与呈现 | Vditor、Markdown-it、MathJax、Highlight.js、DOMPurify |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus |
| 数据库 | MySQL 8.0、Flyway，V2 迁移版本 `2.037` |
| 构建 | Maven、npm |

准确依赖版本以 `star-rain-v2/frontend/package-lock.json` 和 `star-rain-v2/backend/pom.xml` 为准。

## 仓库结构

```text
star-rain-v2/
  backend/                 V2 Maven 多模块后端
    star-rain-boot/         启动入口与数据库迁移
  frontend/                V2 前端与测试
  deploy/                  本地启停脚本
  maintenance/             内容维护脚本与说明
  README.md                V2 配置、功能与工程约定
  CHANGELOG.md             V2 版本记录
backend/                   保留的 V1 后端
frontend/                  保留的 V1 前端
README.md                  项目入口说明
```

## 本地运行

需要 Java 21、Maven、Node.js 22.12 或更新的兼容版本、npm 和 MySQL 8.0。

1. 创建独立的 `star_rain_v2` 数据库，使用 utf8mb4。V2 不能直接使用 V1 数据库。
2. 将 `star-rain-v2/backend/application-local-secret.example.yml` 复制为同目录的 `application-local-secret.yml`，填写数据库与 SMTP 配置。
3. 根据模板配置首次 bootstrap 超级管理员；初始化后关闭该开关。
4. 在 Windows PowerShell 执行：

```powershell
cd star-rain-v2
./deploy/start-local.ps1 -Build
```

前端地址为 `http://127.0.0.1:5174`，后端地址为 `http://127.0.0.1:8088`。公开页面从 `/` 访问，账户页面从 `/useradmin` 访问。停止服务使用 `./deploy/stop-local.ps1`。

## 测试与构建

```powershell
# 从仓库根目录执行
mvn -f star-rain-v2/backend/pom.xml test
mvn -f star-rain-v2/backend/pom.xml -pl star-rain-boot -am -DskipTests package
cd star-rain-v2/frontend
npm ci
npm test
npm run build
```

后端产物为 `star-rain-v2/backend/star-rain-boot/target/star-rain-boot-2.0.0.jar`，前端产物为 `star-rain-v2/frontend/dist/`。数据库不可达时，依赖 MySQL 的集成测试会跳过；验证结果应同时核对执行与跳过数量。

数据库内容、上传媒体、本地密钥、运行日志和内部交接材料独立于源码管理，不包含在版本发布中。部署与升级前备份数据库和媒体；生产环境使用 HTTPS，并设置 `STAR_RAIN_COOKIE_SECURE=true`。

## 历史版本

V1 最后版本为 [`v1.7.0`](https://github.com/LingXI5499/star-rain-notes/tree/v1.7.0)，历史功能与启动方式见该标签的 README。V2 使用独立的数据库、会话与构建目录。
