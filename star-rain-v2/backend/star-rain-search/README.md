# Search 模块

公开搜索入口为 `GET /api/public/search`、`/quick`、`/suggestions`。完整搜索支持 `q`、逗号分隔的 `type`、`page` 与 `pageSize`；只返回 `ACTIVE` 索引。查询词须为 2–100 个字符，公开接口按来源地址限流。前端 `/search` 与顶栏搜索入口共用这组 API。

`sr_search_document` 是可重建的公开派生索引，业务内容仍归教程、博客、作品和作者资料模块。索引仅通过这些模块的 `api` 公开契约读取公开版本；章节的参考答案不参与索引。发布和撤回事件在事务提交后同步，失败记录日志，不影响原发布事务。

模块内直接按 `controller/service/mapper/vo/exception` 分层：查询、索引写入与重建都是 Search 模块的服务用例，不再各自创建一套三层目录。跨模块只使用目标模块的 `api/event`，`config/security` 负责装配与 URL 边界。

本地数据库迁移为 `V2_015__search.sql`。第一次启动且索引为空时自动从公开源建立索引。超级管理员可调用 `POST /api/admin/search/rebuild` 全量重建，或传 `type=BLOG|TUTORIAL|CHAPTER|PORTFOLIO|PROFILE` 重建对应类型；教程与章节因共享发布快照会一起重建。

MySQL 使用 InnoDB FULLTEXT ngram 检索正文。少数高频英文词的全文相关度可能为零，因此标题与摘要还用 `LOCATE` 匹配，避免明确写在标题中的内容消失；不会对正文 `LONGTEXT` 做通配扫描。所有输入经参数绑定。

本地验证：完整 Maven 构建、前端构建通过；首轮从公开源得到 2,232 条索引，重复重建计数不变；“Java”与“排序算法”均可搜索；类型筛选、Quick Search、建议、分页、非法参数及未授权后台访问经 HTTP 检查；公开博客和教程索引与业务状态核对均无未公开记录。
