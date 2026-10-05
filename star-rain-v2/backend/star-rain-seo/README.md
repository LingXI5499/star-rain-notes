# SEO 模块

公开内容来自 Site、Tutorial、Blog、Portfolio、Profile 的 `api` 公开契约。`sr_seo_page_snapshot` 是可重建的派生快照；后台内容和个人学习数据不进入快照。首次启动且快照为空时自动建立。超级管理员可调用 `POST /api/admin/seo/rebuild` 全量重建，或带 `route=/blog/posts/{slug}` 更新单页。

模块内按 `controller/service/mapper/entity/dto/exception` 分层；快照、HTML、站点地图、robots 与通知不各建一套三层目录。对外契约在 `api`。读取 Blog、Site 等模块公开 API 的 `*SeoSourceProvider` 是 SEO 模块自己的 Service 实现；`StaticPageSeoSourceProvider` 负责静态列表页和首页的 SEO 来源，首页配置通过 Site 公开 API 读取，Site 不依赖 SEO。`SeoSourceProvider` 有五个实现，是实际使用的内部选择接口；HTML 渲染器只有一个实现，直接使用具体类。没有对应职责时不创建空的 `utils` 或 `vo` 目录。

- `GET /sitemap.xml` 只读取 ACTIVE 快照；`GET /robots.txt` 使用同一可信域名配置。
- 公开页面的 GET 路由在后端返回带 title、description、canonical 和真实核心内容的 HTML。客户端路由切换也从 `/api/public/seo/meta?route=...` 更新标题、描述和 canonical；账号路径和搜索页标记 `noindex`。
- 发布、恢复和撤回事件提交后刷新对应快照。教程同步处理其章节；失败记录日志，不回滚内容事务。
- IndexNow 通知默认关闭。设置 `STAR_RAIN_INDEXNOW_ENABLED=true`、`STAR_RAIN_INDEXNOW_KEY` 以及经批准的 HTTPS endpoint 后才会入队发送；失败按退避重试，最多五次。密钥只从环境配置读取，不进入数据库或日志。启用前还需按提供方要求完成站点密钥验证。

`STAR_RAIN_PUBLIC_BASE_URL` 指定 canonical 公网来源，默认 `https://yulanlin.cn`，不使用请求 Host。`STAR_RAIN_FRONTEND_INDEX_PATH` 指向前端构建的 `index.html`，后端将其中的本地构建资源标签附到语义快照上。生产反向代理需将公开页面文档路径以及 `/sitemap.xml`、`/robots.txt` 转发到后端，将 `/assets/` 指向前端构建文件，将 `/api/` 转发到后端。这样同一个 canonical URL 首次响应已有可索引语义 HTML，随后 Vue 接管交互。开发环境的 Vite 仍在 5174 提供 SPA，直接访问 8088 可检查后端 HTML。

本地验收：Flyway `2.016`、2,238 个公开快照；Sitemap 无后台/API/搜索路径且全部使用配置域名；重复重建计数一致；页面 HTML 和 meta 可读；恶意 Host 不能改变 canonical；公开树 `index,follow`、账号树 `noindex,nofollow`。完整 Maven 构建、SEO 单测和前端构建通过。
