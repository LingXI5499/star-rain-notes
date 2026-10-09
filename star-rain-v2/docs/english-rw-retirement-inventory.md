# 英语阅读与写作收敛：影响清单

审计基线：`84683b3855f50bb318274cbdac35c7aefbc652df`，分支 `codex/vocabulary-intensive-learning`。只操作 `star-rain-v2`；工作区已有 Blog、Tutorial、Site 等未提交修改，独立保留。

## 代码与业务范围

- 阅读：`ReadingDto`、`ReadingServiceImpl`、`ReadingMapper.xml` 去除三项等级属性及默认值/校验。保持来源权益、列表正文减载、稳定 slug、并发版本、人工标注和历史恢复。历史快照的未知属性可忽略。
- 旧写作：删除 WritingResource/Prompt 的四个 Controller、两组 Service/Impl/Mapper/DTO 及 XML；移除 `EnglishPersistenceConfig` 的旧 mapper 扫描。旧路由不注册 Handler。保留全部 `writing/article/**`。
- 前端：删除 `EnglishDocumentPage`、`EnglishDocumentEditorPage`；`EnglishDocumentsPage`/Manage 收敛为纯阅读。旧素材、题目、通用编辑路由及请求删除；管理写作入口转向本人写作工作区。阅读与原创写作使用各自固定 API。
- 页面：首页去掉 CEFR 路径和分级/范文任务文案；前后台阅读无等级。写作公开与本人列表独立请求/错误，不因一侧失败而丢失另一侧。
  现有库的概览介绍/路线图仍含旧默认分级与任务文案；V2_037 用两字段 SHA-256 精确匹配旧默认值，仅将这份默认介绍与路线图换为真实阅读、自主创作，不覆盖管理员自编内容。
- 阅读交互：保持 Markdown→DOMPurify 管线及连续排版；三模式共享有效范围，单栏原位高亮、双栏组高亮（后续用户要求取消译文浮窗）；无人工映射仅临时局部高亮，禁止伪造翻译。移动无映射时明确原文/译文区分。增强请求独立失败降级。
- 数据：新增 V2_037 只移除等级列、删除旧写作两表；不改 V2_020/035/036。七表阅读清空在 `maintenance/english-reading-reset/`，默认无授权门禁与 ROLLBACK。词汇记忆、语法、261 分类、新原创写作及非英语不清理。

## 2026-10-09 本地只读清单

目标：`127.0.0.1:3306 / star_rain_v2`，Flyway 2.036。以下为备份与恢复演练时的实际值，执行前须重核。

| 阅读表 | 行数 |
|---|---:|
| revision | 1 |
| alignment | 7 |
| annotation | 1 |
| vocabulary | 3 |
| tag | 0 |
| rights | 7 |
| article | 7 |

旧写作 resource 6 条、prompt 6 条；保留原创 writing_article 2 条、writing_revision 6 条。七篇文章含 6 篇历史文章和 1 篇本地验收示例。

阅读/旧写作没有物理外键消费者；媒体引用现为 PORTFOLIO 的三种类型，搜索索引现为 BLOG、CHAPTER、PORTFOLIO、PROFILE、TUTORIAL，未登记英语阅读/旧写作来源。当前搜索/SEO 提供者没有英语内容注册；主题目录直接查询阅读/新写作并按可见性统计，无阅读内容缓存需要清理。未发现 V2 英语阅读/写作启动回填或 V1 导入执行器；未来导入必须明确排除旧内容，不能改写历史迁移。

正式执行前的最新完整备份在被忽略的 `.local/english-retirement/20261009094227/`；早期预览备份在 `.local/english-retirement/20261009090312/`。已恢复到受控隔离库，104 张表数量和全行 CHECKSUM 一致；演练默认门禁不删除、匹配门禁 ROLLBACK 还原、隔离 COMMIT 七表归零，其余 97 表数量及全行 CHECKSUM 一致。备份 SHA-256、详细 inventory、恢复前后摘要均在本地私密报告，禁止提交 dump 或私稿。

**用户已明确授权本机 `127.0.0.1:3306/star_rain_v2` 升级，2026-10-09 已完成限定清空与 V2_037。** 上表为升级前审计值。七张阅读表现为 0，旧写作两表及三等级列已删除；清空事务完成后其余 97 表全行校验不变，新原创写作 2 篇/6 版本、词汇 7053、语法 42、分类 261 保持。正式执行副本及实际报告只存本地忽略目录，仓库维护脚本仍默认 ROLLBACK。
