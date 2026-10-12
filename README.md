# 星雨笔录 · Star Rain Notes

基于 Vue 3 与 Spring Boot 的学习与内容管理网站。公开页面用于阅读，`/useradmin` 提供个人学习空间和管理功能；前后端采用同一个域名，通过 Session 与 CSRF 保护账号及写操作。

仓库只维护当前 V2 主线，前端在 `frontend/`，后端在 `backend/`，自建技能合集在 `skills/`。旧版源码、数据库导出、部署产物和私人配置不在当前源码树中。

## 功能

| 模块 | 主要能力 |
| --- | --- |
| 教程与学习 | 知识体系、教程、分组与章节，Markdown 正文、知识卡片、问题练习，学习记录、复习与计划；草稿和已发布版本隔离，管理员变更可提交审核 |
| 博客 | 文章、专题、标签、归档与分页检索，Markdown、代码和公式阅读；发布、撤回与删除管理 |
| 作品 | 内容区块、展示模板与隔离的静态原型展示 |
| 英语 | 主题词汇、发音、学习计划、语法、双语阅读和写作；私有内容按身份隔离 |
| 账号与审核 | 邮箱验证、管理员邀请、固定角色权限、账户治理、审核轨迹及教程变更失败恢复 |
| 站点 | 作者资料、留言、媒体管理、全站搜索、双主题与移动导航 |
| SEO | 公开内容的语义 HTML 快照、Canonical、JSON-LD、Sitemap、robots，以及可选 IndexNow |

首页保留导航与加载反馈，后台页面按需加载。教程管理在数据库中分页和筛选，排序单独读取轻量目录；博客列表查询不读取文章正文。

## 目录与技术

```text
frontend/       Vue 3、Vite、Vue Router、Pinia、Axios
backend/        Java 21、Spring Boot 3.5、MyBatis、MySQL 8、Flyway
skills/         算法博客、通用教程、Java 开发规范、工程报告技能合集
.github/        仅测试和构建的 GitHub Actions
LICENSE         MIT 开源协议
```

进一步说明见 [前端 README](frontend/README.md) 与 [后端 README](backend/README.md)。

## 自建 Skills

[Skills 合集](skills/README.md) 将星雨笔录创建的写作、开发与工程分析方法整理为可复用规范，包含完整技能入口、参考资料、模板及辅助脚本。

| 技能 | 主要意义 |
| --- | --- |
| [算法博客](skills/star-rain-algorithm-blog/SKILL.md) | 用题意、测试、逐解法推导、多语言实现和复杂度讲清任意来源算法题，不限于 LeetCode |
| [通用教程写作](skills/star-rain-tutorial-authoring/SKILL.md) | 面向任意学科与工具，控制章节边界、知识价值、编号、示例及练习；JavaSE 为可选领域上下文 |
| [Java 开发规范](skills/java-songshan-guidelines/SKILL.md) | 将嵩山版手册结合项目约定用于服务端编码与审查 |
| [工程报告](skills/project-engineering-report/SKILL.md) | 从源码、数据库和测试证据生成工程现状及设计文档 |

技能由用户请求或支持技能加载的工具调用，不属于网站运行时依赖，部署仍只需要前端 `dist/` 和后端 JAR。合集及本仓库维护的文件使用 MIT 协议，引用资料保留其来源的权利与条件。

## 本地运行

准备 JDK 21、Maven 3.9+、Node.js 22.12+（或 24 LTS）、MySQL 8。先创建独立数据库：

```sql
CREATE DATABASE star_rain_v2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

复制 `backend/application-local-secret.example.yml` 为同目录的 `application-local-secret.yml`，填入自己的数据库配置。SMTP 可先关闭；注册验证码和邮件通知需要配置可用 SMTP。真实配置已经被 Git 忽略。

首次建立超级管理员时，填写 `STAR_RAIN_BOOTSTRAP_ADMIN_*` 并临时开启 `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED`，启动成功后关闭它。管理员身份邮箱与 SMTP 发信邮箱分别配置。不要把密码或邮箱写回示例文件。

在两个终端启动：

```bash
cd backend
mvn -DskipTests package
java -jar star-rain-boot/target/star-rain-boot-2.0.1.jar
```

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://127.0.0.1:5174`；后端端口为 `8088`，Vite 代理 `/api`。数据库迁移由后端启动时执行，当前迁移版本为 `2.040`。V2 使用独立数据库，不能直接连接 V1 数据库。

## 构建与测试

```bash
cd frontend
npm ci
npm test
npm run build
```

```bash
cd backend
mvn verify
```

部分后端集成测试需要 MySQL，连接参数使用 `STAR_RAIN_DB_URL`、`STAR_RAIN_DB_USERNAME`、`STAR_RAIN_DB_PASSWORD`。使用已执行全部迁移的**独立测试库**，测试账号需能创建和删除临时测试 schema。没有可用数据库时相关测试会跳过，应检查报告中的 skipped 数量。GitHub Actions 提供独立 MySQL，先迁移再运行测试，不执行部署。

## 部署

部署产物是后端 JAR 与前端 `dist/`。部署者提供 Java 21、MySQL 8、HTTPS 反向代理、独立的配置和媒体目录；本仓库不包含生产凭据或自动部署任务。

1. 构建并上传 JAR 和 `frontend/dist/`，保留数据库及媒体备份。
2. 后端从固定工作目录启动，外部配置填写数据库、邮件及初始化参数。设置 `STAR_RAIN_MEDIA_ROOT` 为持久化媒体目录。
3. 设置 `STAR_RAIN_FRONTEND_ORIGIN` 和 `STAR_RAIN_PUBLIC_BASE_URL` 为实际 HTTPS 公网来源，开启 `STAR_RAIN_COOKIE_SECURE=true`。
4. 设置 `STAR_RAIN_FRONTEND_INDEX_PATH` 为已上传 `dist/index.html` 的绝对路径，使公开 HTML 快照附带当前构建资源。前端页脚域名和备案可通过 `frontend/.env.local` 的 `VITE_*` 变量配置，修改后重新构建；这些值是公开展示信息，不能放密钥。
5. 反向代理保持同源：`/api/` 转发后端；`/assets/`、`/brand/` 从前端 dist 提供；公开教程、博客、作品、首页、留言、关于及 `/sitemap.xml`、`/robots.txt` 转发后端；`/useradmin`、英语及协议页面使用 SPA 回退。完整示例和媒体路径见后端 README。公开内容不存在时保留后端 404。
6. 验证登录、CSRF 写入、文章发布与删除、教程审核、上传、公开页面原始 HTML、Sitemap 与移动导航。确认无误后关闭管理员初始化。

已部署 V2 升级时，先备份数据库和媒体，再替换构建产物并启动，让 Flyway 校验和迁移。不要改已执行的迁移脚本。发生问题时停止新服务，恢复旧产物；涉及数据库状态回退时使用升级前备份，禁止用清库或强行删除迁移记录的方式回滚。公开性能指标需要在实际部署环境测量，本地构建体积不能代替线上 LCP、INP、CLS。

## 开源协议

源码采用 [MIT License](LICENSE)。前端主题动画包含第三方 MIT 代码，其版权与完整许可保留在 [前端 README](frontend/README.md#第三方许可)。部署者自行提供内容、品牌素材和账号资料，并确认其授权。

## 版本变迁

- **V1**：早期实现，已退出当前开发主线。
- **V2.0.0**：重建为 Vue 前端与 Spring Boot 多模块后端，统一学习、内容、账号与 SEO。
- **V2.0.1 及后续维护**：完善教程工作区、审核和版本隔离，修复博客删除确认，优化首页、查询和 SEO；修复阅读布局的样式覆盖，统一媒体选择与上传，支持正文直接上传图片。维护提交保持现有版本号，不新增版本标签。
