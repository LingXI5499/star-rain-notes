# 星雨笔录 · Star Rain Notes

个人知识与作品展示系统，集教程、博客、作品集、英语学习和内容管理于一体。采用 Vue 3 前端与 Spring Boot 模块化单体后端。

## 当前版本

**v1.3.0 — 视觉主题 UI v3**

本版本源码基于开发提交 `2523be5`（2026-09-07），与本地 UI v3 部署包说明中记录的源码版本一致。GitHub 标签 `v1.3.0` 包含该版本前后端源码、统一后的版本元数据和本文档。

数据库内容、用户上传媒体和运行环境配置独立于源码管理，需在部署环境中保留。本仓库不包含生产数据或部署备份。

## 功能概览

| 模块 | 功能 |
| --- | --- |
| 教程与博客 | 分类、章节、标签、Markdown 内容、目录与阅读统计 |
| 作品集 | 项目展示、图片画廊、技术栈与项目说明 |
| 英语学习 | 词汇记忆与复习、语法、听力、阅读、写作、学习包及学习进度 |
| 个人主页 | 首页、关于页、品牌展示与个人信息维护 |
| 内容管理 | 内容编辑、媒体库、发布审核、站点设置 |
| 账号协作 | 邮箱激活、邀请注册、角色权限与审计日志 |
| 搜索与 SEO | 全站搜索、页面元信息、服务端 SEO HTML 与公开内容缓存 |

## v1.3.0 更新内容

- 统一视觉主题、配色变量、品牌标记和浏览器图标。
- 更新首页、英语入口、关于页的主题首屏及默认头像资源。
- 优化英语学习包卡片和公开页面布局。
- 简化作品详情页，保留项目画廊、正文阅读及目录；更新新建作品的五章说明模板。
- 完善后台 Logo、Favicon、头像和作品编辑提示。
- 同步前端与后端 SEO 默认图标。

本版本继承 v1.2.0 的阅读体验、作品画廊、响应式图片、会话内容缓存与前端分包优化。数据库迁移仍为 `V1` 至 `V29`，此次同步不增加迁移。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus |
| 内容编辑 | Vditor、Markdown-it、Highlight.js、DOMPurify |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus |
| 数据库 | MySQL 8.0、Flyway |
| 构建与部署 | Maven、npm、Nginx、Spring Boot JAR |

准确依赖版本以 `frontend/package-lock.json` 和 `backend/pom.xml` 为准。

## 仓库结构

```text
backend/
  pom.xml                  后端依赖与版本
  src/main/java/           业务逻辑、接口与安全配置
  src/main/resources/      环境配置模板、数据库迁移与 SEO 模板
  src/test/                已有测试源码
frontend/
  package.json             前端依赖与命令
  package-lock.json        依赖锁定文件
  public/brand/            页面使用的品牌与主题资源
  src/                     页面、组件、路由、状态与接口封装
README.md                  项目说明
```

根目录另保留 Git 忽略及文本属性配置。内部设计文档、数据库备份、部署包、运行日志和本地密钥不属于发布源码。

## 本地运行

### 环境要求

- Java 21 与 Maven 3.6.3 或更新版本。
- Node.js 22.12 或更新的兼容版本与 npm。
- MySQL 8.0；预先创建空数据库，例如 `star_rain_notes`。

### 后端配置

通过环境变量配置数据库与邮件服务；也可使用被 Git 忽略的 `backend/application-local-secret.yml`。已跟踪的 `application-local.yml` 仅包含模板和本地私密配置导入声明。

| 变量 | 用途 |
| --- | --- |
| `MYSQL_HOST`、`MYSQL_PORT` | 数据库地址和端口；默认 localhost、3306 |
| `MYSQL_DATABASE` | 数据库名；默认 star_rain_notes |
| `MYSQL_USER`、`MYSQL_PASSWORD` | 数据库账号和密码 |
| `APP_SUPER_ADMIN_EMAIL` | 首次激活的超级管理员邮箱 |
| `MAIL_HOST`、`MAIL_PORT` | SMTP 地址和端口 |
| `MAIL_USERNAME`、`MAIL_AUTH_CODE`、`MAIL_FROM` | 发信账号、授权码和发件地址 |
| `MAIL_SSL_ENABLED` | SMTP SSL 开关；默认 true |
| `MAIL_BASE_URL` | 前端访问地址，用于生成邀请链接 |
| `MEDIA_STORAGE_DIR` | 上传媒体存储目录 |

```bash
cd backend
mvn spring-boot:run
```

默认端口为 `24680`。Flyway 在启动时执行数据库迁移。

### 前端启动

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`。开发服务器将 `/api`、`/actuator` 和 `/uploads` 转发到后端；可用 `VITE_API_TARGET` 修改开发代理目标。

全新数据库首次使用时，打开 `http://localhost:5173/admin/activate`，通过配置邮箱接收验证码并设置超级管理员密码，随后在 `/admin/login` 登录。系统不提供默认超级管理员密码。

## 数学公式

博客、教程、作品和其他使用共享 Markdown 组件的正文支持 KaTeX 数学公式。后台编辑器同步提供公式预览，内容仍以原始 Markdown 保存。

行内公式使用一对 `$`，例如：

```markdown
时间复杂度为 $\Theta(n)$，总开销为 $\boxed{\Theta(n+b)}$。
```

独立公式使用 `$$`，建议将分隔符单独成行：

```markdown
$$
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
$$
```

也支持标记为 `math` 的代码围栏。普通代码块和行内代码中的公式保持原样；要显示普通美元符号，可写成 `\$`。行内公式的 `$` 内侧不要留空格。含 `\`、`^`、`_` 或 `{}` 的公式即使紧贴英文单词（如 `$\Theta(n^2)$Princeton`）也会正常渲染；纯占位写法如 `$variable$name` 仍不会被当成公式。

公式脚本、样式和字体随前端构建提供，无需单独配置 KaTeX CDN。长独立公式可横向滚动，错误或不支持的公式保留文本提示，不中断正文渲染。支持范围以 [KaTeX 支持表](https://katex.org/docs/support_table.html) 为准，不提供完整 LaTeX 文档编译能力。

已有正文中符合上述写法的公式会在新版前端部署后自动渲染，不需要修改数据库。若公式本身位于普通代码块中，应先移出代码块。

## 多语言代码组（力扣式切换）

同一题若有多种语言实现，用 `::: code-group` 包住多个标准代码围栏。前台会渲染成语言标签，点击后只显示对应代码；复制按钮只复制当前语言。

```markdown
::: code-group

```c
int bubble(int* a, int n) { return n; }
```

```java
int bubble(int[] a) { return a.length; }
```

```python
def bubble(a):
    return len(a)
```

:::
```

内容仍以原始 Markdown 存库；解析与 Tab 交互只发生在前台渲染。普通单独代码块行为不变。后台编辑器按纯文本编写即可，无需额外插件。

## 构建与部署

以下命令供部署时使用，后端构建命令显式跳过测试。

```bash
# 前端：产物位于 frontend/dist/
cd frontend
npm ci
npm run build
```

```bash
# 从仓库根目录构建后端，跳过测试编译和执行
mvn -f backend/pom.xml -Dmaven.test.skip=true package
```

后端产物为 `backend/target/star-rain-notes-backend-1.3.0.jar`。生产启动设置 `SPRING_PROFILES_ACTIVE=prod`，由部署环境注入数据库及邮件凭据。

Nginx 提供前端静态资源、单页应用路由回退和后端反向代理。按实际环境配置 `SEO_SITE_ORIGIN`、`SEO_FRONTEND_INDEX_PATH`、`MAIL_BASE_URL` 和 `MEDIA_STORAGE_DIR`；HTTPS 环境保持 `SESSION_COOKIE_SECURE=true`。升级时应保留现有数据库、上传媒体及环境配置。

## 敏感信息管理

- 密码、SMTP 授权码、令牌和个人邮箱只通过环境变量或本地私密配置提供。
- 不提交 `.env`、私钥、数据库备份、运行日志、用户上传文件和构建产物。
- `VITE_*` 变量可能进入浏览器产物，不得用于保存服务端凭据。
- `backend/src/main/resources/db/migration/` 中的 SQL 为版本化数据库迁移，应随源码保留。

## 版本记录

| 版本 | 说明 |
| --- | --- |
| v1.4.0 | 数学公式定界修复、力扣式多语言代码组、去掉非作品封面 UI |
| v1.3.0 | 同步 UI v3 最新前后端源码，统一版本信息与 README |
| v1.2.0 | 阅读体验、作品展示与公开内容性能优化 |
