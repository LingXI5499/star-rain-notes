# English Reading / Writing V2 实施报告
日期：2026-10-08；状态：核心开发、联调及验收完成。未进行生产部署。
基线：93e6e3dcd9a564abda142f7bf3f616134d07de54；分支：codex/vocabulary-intensive-learning。
依据：开发指南及 star-rain-codex-v2 的 00—07 文档、分类 JSON、DDL；90 仅作需求追溯。
范围：仅提交英语读写、共享筛选禁用状态、相关路由和测试；保留其他模块未提交修改。

## 已实现
- 共享分类：TOPIC 16 根 / 144 子，GENRE 9 根 / 74 子，PURPOSE 18；全部 261 项与 JSON 一致。
- 分类管理支持新增、编辑、停用；稳定 slug 与维度校验；旧引用和历史快照保留。
- 阅读保留 reading-{id} 地址；完整英文、可选中文，英文/中文/双语三模式。
- 桌面左右全文双栏；手机按人工映射交错成组；支持一段英文对应多段中文。
- UTF-16 文本范围及正文/段落 SHA-256 校验；正文变化使标注失效，旧标注不错误高亮。
- 精选精读、本文词汇独立维护；保留 Markdown 行内格式；同词不同词性可并存。
- 外部文章须核查公开依据，BLOCKED 禁公开；来源、许可及配置的版权联系入口可见。
- 未评定文章不显示默认 A1；公开 DTO 不返回内部 rightsBasis。
- 自由写作独立旧素材/任务；账号私有草稿，可空正文创建，完成/公开需英文正文。
- 约 1000ms 自动保存，单请求队列与 CAS；409 暂停并保留草稿，显式核对合并；失败可重试。
- 保存/预览/离开前读取编辑器当前内容，避免延迟回调漏掉最后输入。
- 阅读与写作不可变快照、分页版本列表、按需两版对比；恢复生成新版本。
- SQL 写作所有权由当前账号强制，ID+owner+rowVersion 更新；创建时限定原创公开资格。
- 普通用户私稿不进入公开列表、主题聚合、计数、搜索、SEO 或 sitemap。
- 主题公开阅读/原创统一 SQL 分页；账号下“我的创作”独立查询、独立分页。
- 默认每页 12，可选 12/24/48；复用网站主题控件，兼容手机与深浅主题。
- 阅读管理五个工作区；写作素材、练习任务及词汇学习旧入口保留。

## 主要改动路径
- backend/star-rain-english：taxonomy、knowledge、reading、writing/article、对应 Mapper XML 与权限规则。
- backend/star-rain-boot：035/036 迁移、EnglishRwIntegrationTest、EnglishMapperXmlTest。
- frontend/src/modules/english：API、正文/分类/版本组件、读写/主题/管理页面、样式及回归测试。
- frontend/src/router/index.js；frontend/src/shared/ui/PublicFilterTabs.vue。

## 数据库
- V2_035__english_knowledge_taxonomy.sql：新增 sr_english_taxonomy_term，261 项种子。
- V2_036__english_reading_writing_extensions.sql：扩展 sr_english_reading_article。
- 新增 sr_english_reading_tag、sr_english_reading_rights、sr_english_reading_alignment。
- 新增 sr_english_reading_annotation、sr_english_reading_vocabulary、sr_english_reading_revision。
- 新增 sr_english_writing_article、sr_english_writing_article_tag、sr_english_writing_revision。
- 向已有账号 sr_permission/sr_role_permission 表增加 english:content-publish 权限及管理员授权。
- 无物理外键；归档保留数据与版本。旧阅读默认 EXTERNAL / PENDING。

## API
- /api/public/english/taxonomy、taxonomy/{slug}；/api/admin/english/taxonomy。
- /api/public/english/knowledge：公开主题读写聚合。
- /api/public|admin/english/content/reading：正文；alignments/annotations/vocabulary 子资源。
- /api/admin/english/content/reading/{id}/revisions：快照、详情、恢复。
- /api/account/english/writing/articles：本人 CRUD、complete、revisions 及 restore。
- /api/public/english/content/writing-articles：公开原创列表及详情。
- /api/admin/english/content/writing-articles/{id}/publish|withdraw：受权限与所有权限制。

## 实际验收
- mvn.cmd -B -pl star-rain-english -am test：通过；65 项英语模块测试。
- mvn.cmd -B -pl star-rain-boot -am test：工作区及独立交付副本通过。
- 独立副本使用新建 MySQL 测试库，Flyway 实际升级至 2.036；后端共 690 项，0 失败/错误/跳过。
- 专用隔离测试覆盖旧版升级、版权、维度、去重分页、跨账号访问、CAS、失效标注、不可变恢复。
- 独立库补齐原有测试依赖的 1 篇博客 / 10 个词汇夹具；测试结束删除整个命名探针库。
- mvn.cmd -B -pl star-rain-boot -am package -DskipTests：通过；最新本地服务已重启。
- npm.cmd run test：工作区 37 文件 / 165 项通过；独立副本 26 文件 / 106 项通过。
- npm.cmd run build：工作区与独立副本均通过；git diff --cached --check 通过。
- 真实页面验证刷新保留草稿、立即存版本、版本差异、恢复新增快照及 409 显式合并。
- 390px 手机 / 1280px 桌面无横向溢出；手机一对多顺序正确，深色主题可读。
- 素材、练习、词汇、博客、教程、作品 API smoke 全部 200；原词汇高强度集成回归通过。
- 开发库包含其他模块新结构，旧基线教程断言不兼容；已用独立库验证原基线，未改动该模块。
- 未通过项目：无；没有做生产部署或生产数据变更。

## 本地数据与版权清单
- 本地新增验收阅读 43、公开原创写作 1、私有冲突/版本草稿 2，标题均明确标注本地验收示例。
- 旧阅读 1—6 均为 PENDING，公开全文已隐藏；须由管理者逐篇提供权利依据后再发布。
- 1 A Day at the Community Library；2 Planning a Weekend Train Trip；3 Building a Healthier Digital Routine。
- 4 How a Small Team Works Remotely；5 What AI Should and Should Not Do in Education。
- 6 Responsibility in Algorithmic Public Decisions。未推定为原创，未批量设为 CLEARED。

## 回滚
先停写并备份数据库；应用回退基线需停用英语阅读/写作公开入口，避免旧代码绕过新增权益约束。
保留增量表和列可保存草稿/历史；不要直接修改已执行 Flyway 迁移或自动执行 down SQL。
如需物理回退数据库，使用迁移前备份，在隔离环境验证后按独立运维流程实施。
