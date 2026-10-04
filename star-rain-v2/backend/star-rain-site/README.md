# Site 模块

Site 只拥有 `sr_site_config` 和 `sr_site_home_section`。公开首页通过教程、博客、作品、作者资料和统计模块的公开 API 聚合内容；后台仪表盘通过各模块实现的 `DashboardSource` 汇总，不读取其他模块的 Mapper 或 Entity。

- `GET /api/public/site/config`：公共名称、标题、简介、页脚和公开媒体 URL。
- `GET /api/public/site/home`：按启用区块的排序返回首页内容。可选区块独立限时读取；失败时只将该区块标为 `DEGRADED`。
- `PATCH /api/admin/site/config`、`PUT|DELETE /api/admin/site/logo|favicon`：仅超级管理员可修改。媒体必须 ACTIVE、PUBLIC、IMAGE；引用由 Media API 管理。配置变更提交后触发 SEO 首页刷新。
- `GET /api/admin/site/home-sections`、`PATCH /api/admin/site/home-sections/{sectionCode}`、`PUT /api/admin/site/home-sections/order`：区块代码受白名单约束，展示配置只允许 `limit` 与 `layout`。
- `GET /api/admin/site/dashboard`：超级管理员查看账户、内容、审核、留言及访问统计。

本地迁移为 `V2_017__site.sql` 与 `V2_018__site_default_copy.sql`；后者只补齐未改动的初始首页文案。生产数据库无需手工运行 SQL，应用启动由 Flyway 迁移。
