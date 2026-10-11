# 留言模块

`star-rain-message` 管理公开留言的提交、审核与展示。匿名访客和登录用户都可以通过 `POST /api/public/messages` 提交；返回 `id` 和 `PENDING`，审核通过前不会出现在公开列表。

## 状态与权限

`PENDING → PUBLIC/REJECTED`，`PUBLIC ↔ HIDDEN`，`PUBLIC/HIDDEN → DELETED`。拒绝与软删除为终态。每次变更在同一数据库事务写入 `sr_message_action`，审批使用带原状态条件的更新，避免重复决策。

超级管理员拥有 `message:read-admin`、`message:moderate`、`message:hide`、`message:delete`。后台接口位于 `/api/admin/messages`；匿名访问返回 401。`GET /api/public/messages` 只返回公开留言的编号、署名、纯文本内容和提交时间。

登录用户的署名从 `AccountReferenceApi.displayName` 读取快照，浏览器不能指定账号 ID。访客需要填写署名。联系邮箱可选，仅后台列表可见。提交端有每来源地址每分钟 5 次的进程内限流，限流键使用随机进程密钥计算摘要；数据库和日志不记录明文 IP。

数据库迁移：`V2_012__message.sql`。站点仪表盘未来通过 `MessageSummaryApi.pendingCount()` 聚合待审核数量，不能直接访问本模块的表。
