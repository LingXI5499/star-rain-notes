# 路由统一与部署验收

本次基于 main 提交 `46cf75642efda7ff9a544acbce32cd60849444df`，只调整主应用路由、导航、SEO、邮件落地地址、部署示例与回归测试。静态网站分支独立维护。本地成果按 [项目开发约束](../../AGENTS.md) 验收，用户明确批准推送后才合入远程 main；此文档不表示线上已部署。

## 页面与权限

| 用途 | 规范地址 | 行为 |
| --- | --- | --- |
| 首页 | `/` | 现有 HomePage，正常显示账号入口；不会自动进入后台 |
| 认证 | `/login`、`/register`、`/forgot-password`、`/reset-password` | 可直接访问；整屏布局；noindex |
| 邀请 | `/invitation/accept?token=...` | 仍需登录受邀账户；未登录时完整目标保存在登录 redirect 中 |
| 个人中心 | `/center` | 需要登录，保留原侧栏外壳 |
| 个人学习 | `/learning` 及全部原有子路径 | 需要登录，继续使用前台外壳与页内学习导航 |
| 账户治理 | `/dashboard`、`/accounts`、`/invitations`、`/audits`、`/site/settings`、`/analytics` | 原有登录、权限和 SUPER_ADMIN 限制全部保留 |
| 内容管理 | `/blog/manage`、`/blog/taxonomy`、`/blog/topics`、`/blog/editor/:postId`、`/tutorials/manage`、全部教程编辑/练习/章节管理地址、`/portfolio/manage`、作品编辑/预览地址、`/english/manage/**`、`/messages/manage`、`/profile/manage` | 保留原有组件、动态参数和权限；管理页优先于公开内容网关 |
| 其它后台 | `/media`、`/media/:mediaAssetId`、`/reviews`、`/reviews/:reviewId` | 仍按原有权限限制，普通 ADMIN 的媒体/审核读取权限不变 |
| 公开内容 | `/blog`、`/tutorials`、`/portfolio`、`/english`、`/about`、`/messages` 及原有公开子路径 | 可匿名阅读；规范 URL；正常公开内容可索引 |

原有 90 条页面声明逐条保留，每条只生成一份记录；再增加一个旧前缀兼容记录和一个未知页面记录。`requiresAuth`、`permission`、`superAdminOnly` 保留原值；仅首页补充公开页标记，作品 live 补充明确 noindex 标记。`/permissions`、`/learning/today`、旧练习入口和英语写作管理跳转均保留原重定向语义。

`accountPath()` 与 `contentPath()` 共用规范化工具。组件不再根据 URL 前缀隐藏账号入口、会话或已登录学习功能。控制台侧栏仍由 `meta.console` 决定，认证页由 `meta.authPage` 决定。Spring Security、Session、CSRF 和后端接口权限未放宽；输入后台地址不代表有权调用后台接口。

## 旧链接与邮件

| 旧地址 | 新地址 |
| --- | --- |
| `/useradmin`、`/useradmin/` | `/` |
| `/useradmin/center?tab=profile#details` | `/center?tab=profile#details` |
| `/useradmin/blog/manage` | `/blog/manage`，未登录仍需先登录 |
| `/useradmin/reset-password?token=...#reset` | `/reset-password?token=...#reset` |
| `/useradmin/invitation/accept?token=...#accept` | `/invitation/accept?token=...#accept`，未登录时经登录返回 |
| 其它 `/useradmin/**` | 去掉完整路径段前缀后继续原有匹配、权限和重定向 |

前端兼容记录传递 query 与 hash；登录返回目标也规范化旧前缀。Vue Router 可能规范化等价的百分号编码，例如 `%3D` 变为 `=`，解析后的令牌、重复参数和返回地址语义不变。路径工具不会修改 query/hash 的原始后缀。Nginx 308 使用 `$is_args$args` 保留原始查询串；响应 Location 不指定新片段，浏览器继承原 hash，已做实际浏览器验证。

新密码重置邮件统一使用 `/reset-password?token=...`，同步修改直接发信服务与异步发件箱；新邀请邮件使用 `/invitation/accept?token=...`。已有队列、历史邮件和旧书签继续走兼容跳转。

## SEO 与访问统计

SEO 与公开统计共用路由可见性判断：认证、需要登录、带权限、超级管理员、控制台以及显式 noindex 路由都排除。公开统计还保留原有公开入口白名单，不扩大统计业务范围；失败的导航不记访问。搜索页保持 noindex，作品 live 保持 noindex。私有页不请求公开 SEO 元数据，并清除上一个公开页遗留的 Canonical；公开页继续使用后端提供的规范地址。

robots 后端与独立 Nginx 片段同步更新：移除旧 `/useradmin/` 禁止规则，改为规范认证、学习、后台、编辑、预览、邀请地址；保留 `/api/public/` 放行及其余 `/api/` 排除。没有禁止整棵公开博客、教程、作品或英语路径。Sitemap 仍只使用现有公开发布数据与规范路径。

真实后端回归额外发现：不存在的公开内容在 provider 内转换为“未找到”后，其读取事务会把外围 SEO 事务标记为 rollback-only，导致 HTTP 500。SEO 快照可见性查询改为不加入外围事务，使已有 Optional.empty → HTTP 404 语义真正生效；未更改内容发布规则。回归同时检查原始 HTML 与 SEO 元数据 404。

## Nginx 应用顺序

以 [完整示例](../../backend/deploy/account-v3/nginx-site.example.conf) 为真源，同步到后端 README 与 V2.2 部署清单。示例域名、证书、根目录和后端端口必须按实际部署保留；不要整份覆盖既有服务器配置。

1. `/api/`、assets、brand、legal 等使用现有专用位置，API 不受静态资源正则覆盖。
2. 旧前缀 308 跳转；站点验证文件必须真实存在，缺失静态资源返回 404。
3. 认证、个人、后台以及与公开内容路径重叠的管理地址先进入 SPA，响应附带 noindex、no-cache 和 no-referrer。使用 `try_files /index.html =404` 直接读取文件，避免内部跳转丢失 X-Robots-Tag。
4. 作品 live 使用 SPA 并排除索引；公开英语、协议与搜索使用 SPA，不对整个英语目录设置 noindex。
5. 其余请求交给后端公开 SEO，保留 HTML 快照、Canonical、旧博客 301 与真实 404；不得全站 `try_files ... /index.html`。

这些规则保留 `/blog/topics` 管理页与 `/blog/topics/:slug` 公开专题的不同用途，也覆盖教程动态章节、练习管理及作品编辑/预览。管理地址中已经保留的静态段属于路由保留段，不应作为新的公开内容 slug。

独立 robots 配置使用 [robots-public-api.location.conf](robots-public-api.location.conf) 替换原有同名 location，不叠加第二个块。只上传 dist 不会改变服务器已有 robots 或 Nginx 分流。

## 验收方法

本地自动验证结果与修改文件清单见 [验证记录](route-unification-validation.md)。可重复执行：

```bash
cd frontend
npm ci
npm test
npm run build
npm run test:e2e
```

后端使用 JDK 21、已迁移的独立测试 MySQL 和 `STAR_RAIN_DB_URL/USERNAME/PASSWORD` 执行 `mvn verify`。账户集成测试只接受项目约定的 CI 库名并自行创建随机临时 schema，禁止使用生产库。真实浏览器测试另启独立数据库和回环 SMTP，按现有 `STAR_RAIN_E2E_REAL` 配置执行；默认浏览器测试中的受控接口不代替真实安全验收。

Nginx 可使用本机二进制执行（无需连接生产）：

```bash
NGINX_BIN=/path/to/nginx node tools/test_route_nginx.mjs
```

该脚本从实际示例提取 server 配置，使用临时端口、目录和受控 SEO 上游，执行语法检查、管理分流、公开网关、404、查询参数及浏览器 hash 继承测试；不修改系统或生产 Nginx。需先安装前端依赖及项目 Chromium。

用户批准实际部署后仍须：备份当前产物和配置 → 部署配套后端/dist → 合并 Nginx 与 robots 规则 → `nginx -t` → reload → 实际 HTTPS 域名上复核首页账号入口、认证及后台刷新、旧邮件、公开 HTML/Canonical/Sitemap/404、手机菜单和侧栏。部署、线上数据库、搜索引擎平台收录结果均未在本任务中操作或验证。
