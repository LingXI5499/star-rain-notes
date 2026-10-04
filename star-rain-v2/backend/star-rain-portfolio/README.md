# 作品模块

`star-rain-portfolio` 在同一 Maven 模块内管理作品主信息、类型详情、媒体编排、外链与发布状态。数据库迁移为 `V2_011__portfolio.sql`。

## 业务规则

- 新建作品只有标题和类型必填，状态为 `DRAFT`；公开地址由标题自动生成，后续改标题不会自动改地址。
- 类型创建后不可直接切换。`detail_json` 必须通过对应的 `WorkDetailValidator`，未知字段会被拒绝。
- 发布前检查摘要、正文、类型详情、媒体可用性与外链地址。`DRAFT → PUBLISHED → WITHDRAWN → PUBLISHED`；恢复保留首次发布时间。
- 公开接口在 SQL 查询条件中只读取 `PUBLISHED`。草稿、已撤回与不存在的作品都返回 404。
- 作品媒体关系和 MediaReferenceApi 引用在同一事务中写入或解除；移除作品媒体、删除草稿不会删除媒体资产。
- 发布、撤回、恢复和删除产生提交后的 `WorkPublicationChangedEvent`，供后续 Search、SEO、Site 模块订阅。

## 接口

- 公开：`GET /api/public/portfolio/works`、`GET /api/public/portfolio/works/{slug}`。
- 后台：`/api/admin/portfolio/works` 下维护主信息、正文、类型详情、媒体、外链与发布状态；只有超级管理员拥有 `portfolio:*` 权限。
- 模块间读取只使用 `PortfolioPublicApi`、`PortfolioReferenceApi`、`PortfolioSearchSourceApi`；外部模块不读作品表。

作品的正文编辑复用前端博客的 Markdown 编辑器与渲染器。
