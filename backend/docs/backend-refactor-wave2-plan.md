# 星雨笔录后端重构第二轮可执行任务

> 文档类型：可执行工程改造规范
> 对照来源：`backend/docs/backend-layering-audit-report.md`（2026-09-24，201 条问题）
> 历史规范：`星雨笔录_English_V2后端重构与Agent-Ready完整开发计划.md`（2026-09-23）
> 部署形态：Modular Monolith。不拆微服务。
> 本轮目标：补上第一轮「类已拆开、职责未归位」的缺口，并按审计优先级处理非英语模块的正确性与跨模块边界。

---

## 0. 两份文档怎么一起用

English V2 计划描述的是 **2026-09-23 之前** 的上帝 Service。到审计日，PR-E00～E11 的结构目标已经落地：`application / domain / infrastructure`、三个 Facade、`EnglishContentRegistry`、`MediaPort`、复习策略与推荐引擎都已存在。对应记录在 `backend/docs/english-v2-*.md`。

分层审计读的是 **落地之后的代码**。它证明：

1. Controller 基本合格（44 个 Controller：0 处 SQL、0 处事务、0 处直连 Mapper）。
2. 英语模块的主要欠账从「Service 里写 SQL」变成了「Repository 里写业务规则、Application 变成空壳、子域之间仍直查对方表」。
3. 非英语 11 个模块仍是 `controller → service → mapper`，并且绕过已有的 `MediaAssetPort` 直连 Mapper 或 JOIN 他模块表。

因此第二轮 **不重做** 第一轮的拆包，也不把 SQL 搬回 Service。第二轮只做审计仍不合格、且第一轮验收清单未真正满足的项。

冻结契约全部继承 V2 计划第 4、38 节：

```text
不改 REST 路由
不改 DTO JSON 字段名与形状
不改历史 Flyway（只允许新增 V36+，且本轮默认 0 schema change）
不改复习间隔、推荐打分、nextReview、发布状态机、练习判分结果
不引入 Nacos / Gateway / MQ / Redis / LLM / Vector DB
不把架构重构和 Entity Lombok 化混在同一个 PR
一个 PR 只处理一个边界，每步可编译、可回归
```

每次只执行下面一个 PR。发现必须跨 PR 才能安全完成时，用临时 Port/adapter，不扩大范围。

---

## 1. 第一轮完成度（不再重做的部分）

| 原 PR | 结构状态 | 审计仍不合格 | 第二轮归入 |
|---|---|---|---|
| E00 baseline | 已有 `english-v2-refactor-baseline.md` | — | 不重做 |
| E01 Reading | Query/Command/Repository/Policy 已存在 | R-01～R-04：送审在 Controller；Policy 依赖仓储；仓储 JOIN grammar、直删 `english_exercise` | W2-12、W2-10 |
| E02 Listening | 内容/分段/发音/配对已拆类 | L-01～L-04：`ListeningRepository` 649 行上帝仓储；判分在仓储；直查 `english_reading_article` | W2-11、W2-10 |
| E03 Exercise DTO | 共享练习契约已存在 | 判分位置仍三处不一（reading 在 application，listening/writing 在 repository） | W2-14 |
| E04 Grammar | Query/Command/Ordering/Policy 已存在 | G-01、G-02：规则在 475 行仓储；送审在 Controller；写事务不统一 | W2-13、W2-10 |
| E05 Writing | Query/Command/Policy 已存在 | W-01、W-04～W-08：送审在 Controller；部分校验和判分仍在仓储；写事务不齐 | W2-14、W2-10 |
| E06 Vocabulary 归位 | 已在 `english.vocabulary` | V-01～V-10：三个 Application 是空壳，规则与组装在仓储，仓储反向依赖 Assembler | W2-15 |
| E07 Account English | Controller 已只依赖 Facade | 路由保留。`AccountEnglishController` 仍用裸 `Map`（A-15） | W2-30，非本波必须 |
| E08 复习算法 | `VocabularyReviewPolicy`、`FOR UPDATE`、session 幂等已在应用层 | V-12、V-14：学习查询缺只读事务；`settings()` 查询时 `INSERT IGNORE` | W2-15 |
| E09 Registry | Registry、RecommendationEngine、Taxonomy Query/Command 已存在 | S-08、S-09、A-01：taxonomy/content-state 仍直查业务表；推荐服务依赖 bundle 的 Repository | W2-16 |
| E10 Facade | 三个 Facade 已存在 | Facade 保持纯委托，不在本轮塞业务 | 不重做 |
| E11 内容事件 | `@EnglishContentChange` 已从业务方法上拆出 | S-17/E-08：切面用「第一个 Long 参数」猜内容 id | W2-16 末项，可单独后置 |
| 媒体边界 | English 已走 `MediaPort` → `MediaAssetPort` | blog/tutorial/portfolio/profile/site 仍直连 `MediaAssetMapper` | W2-20 起 |

已确认合格、第二轮不要为了形式再改：

```text
GrammarOrderingService
ReadingCommandService 的字段校验与 ReadingTextStatistics
ListeningSegmentService.batchReplace → SegmentTimelinePolicy
RecommendationEngine、VocabularyReviewPolicy、LearningReviewPolicy
MediaPortAdapter、EnglishLearningAnalyticsQueryService
DashboardQueryService + DashboardRepository
MediaQueryService
TutorialTreeBuilder / TutorialCurriculumBuilder
AccountEnglishController → EnglishLearningFacade / EnglishVocabularyFacade 这条依赖方向
```

---

## 2. 执行顺序

```text
波次 A  正确性（审计 P0）     W2-01 → W2-02 → W2-03
波次 B  收口 English 未达标   W2-10 → W2-11 → W2-12 → W2-13 → W2-14 → W2-15 → W2-16
波次 C  跨模块 Port（审计 P1） W2-20 → W2-21 → W2-22 → W2-23 → W2-24 → W2-25
波次 D  一致性欠账（审计 P2）  W2-30，按子项拆 PR，不阻塞 A～C
```

A 的改动小、修的是事务和孤儿数据，先做。B 完成 V2 计划第 41 节里审计判失败的验收项。C 把英语模块已经有的 Port 模式铺到其余模块。D 不改变行为，只减重复和胖类。

每个 PR 结束跑：

```bash
mvn -q -f backend/pom.xml test
```

测试失败不得标记完成。

---

## 3. 波次 A — 正确性

### PR-W2-01 `fix(backend): close write-path transactions`

只补事务，不改业务分支。

| 位置 | 审计编号 | 动作 |
|---|---|---|
| `PortfolioCommandService.delete/publish/withdraw` | P4-1 | 补 `@Transactional`。`create/update` 已有，不要重写 |
| `MediaService.createArchiveAsset` | M4-1 | 一次插入即带最终 `publicUrl`，或用同一事务包住 insert + update。禁止 update 失败后只删文件、留下孤儿行 |
| `BlogTagService.create/update/delete` | B2 | 补 `@Transactional` |
| `AccountCredentialService.authenticate` | A-04 | 失败次数、锁定、最后登录同方法多次写，补 `@Transactional` |
| `VerificationCodeService.verify` | A-05 | 尝试次数与消费同事务 |
| `AccountActivationService.requestCode`、`AccountInvitationService.requestCode` | A-08 | 补 `@Transactional` |

完成标准：

```text
[ ] 上述写路径都有事务
[ ] createArchiveAsset 在 update 失败时不会留下无 publicUrl 的行
[ ] 路由、状态码、错误 code 不变
```

### PR-W2-02 `fix(account): send mail after commit`

审计 A-17、A-18。`VerificationCodeService.issue`、`AccountInvitationService.create/resend` 现在在事务里发 SMTP。

改为事务提交后再发（`TransactionSynchronization.afterCommit` 或已有事件机制）。回滚不得发信。邮件失败不得回滚已经提交的验证码/邀请行——保持「先落库、后通知」；失败记日志，不改 API。

### PR-W2-03 `fix(media,portfolio): document file-and-db consistency`

审计 P4-2、M4-2。`MediaService.upload`、`PortfolioPrototypeService.bind` 先落盘再写库，崩溃会留文件。

本 PR 不重写上传管线。只做两件事：

1. 在 `backend/docs/media-storage-consistency.md` 写明当前补偿策略（catch 删文件、崩溃残留、谁负责清理）。
2. 若能在不改 API 的前提下把「写库失败必删刚写入的文件」补到现有 catch 未覆盖的路径，只补这一处。不引入新存储抽象。

---

## 4. 波次 B — 收口 English V2

分层规则以 V2 计划第 3 节为准，并加一条审计结论：

```text
Repository 只做 SQL、行映射、分页查询、持久化。
发布条件、标签维度、slug 冲突的业务错误、判分、404/409 的 ApiException，放在 Application 或 Policy。
Repository 返回 Optional / 行数据，不抛带 HTTP 状态的 ApiException。
domain 不依赖 infrastructure，不 import HttpStatus。
子域不 SELECT/JOIN/DELETE 另一个子域的表，走该子域已有的 Port / ContentRegistry / Facade。
```

### PR-W2-10 `refactor(review): move submission branch out of controllers`

审计 G-01、L-01、R-01、W-01、A-01、A-02、A-21、B6、T12。这是同一模式复制多份，单独一个 PR，不顺手改各域仓储。

现状：

```text
English 四个 Admin Controller
  isSuperAdmin + ContentReviewService.isPublished + submitEnglishUpdate
BlogUpdateWorkflowService ↔ ContentReviewService ↔ BlogCommandService
TutorialChapterUpdateWorkflowService ↔ ContentReviewService ↔ TutorialNodeService
ContentReviewService 自己持有 JdbcTemplate，并 if-else 调用 8 个命令服务
```

目标：

```text
account.review.infrastructure.ContentReviewRepository   // 仅 content_review_request 的 SQL
account.review.api.ReviewSubmissionPort                 // submit / isPublished
account.review.api.ReviewedContentHandler               // 按 contentType 应用已通过的更新
```

各内容模块提供自己的 `ReviewedContentHandler`。`ContentReviewService` 不再 import blog/tutorial/english 的 CommandService，也不再写 SQL。

English 的 `update/updateLesson/updatePrompt/updateResource/updateRule`：Controller 只把 `Authentication` 转成 actorId + roles，送审分支进对应 CommandService。`ContentReviewService` 对非 SUPER_ADMIN 已发布内容的审核结果（202 + review 视图）保持不变。

禁止：

```text
改审核状态机
改 content_review_request 表
改前端依赖的 202 响应体
在本 PR 拆 ListeningRepository / GrammarRepository
```

### PR-W2-11 `refactor(english-listening): lift rules out of repositories`

状态：2026-09-24 已落地 L-001、L-003、L-004、L-005，以及 L-006 中判分与配对类型的编排。`ListeningIntegrationTest` 通过。L-002 未做：标签 SQL 仍在 `ListeningRepository`，分页 `LIMIT/OFFSET` 仍是已收口整数的拼接。

审计 L-02、L-03、L-04、L-05、L-06。

```text
L-001  ListeningPublishPolicy 收回 validateCefr/Level/Audio/Cover、标签维度、已发布禁删、禁撤草稿
L-002  ListeningRepository 只留 item 的查询、插入、更新、统计 SQL。目标拆出标签读取，类长回到可持续维护的范围（审计时 649 行）
L-003  判分从 ListeningExerciseRepository.check 上移到 ListeningExerciseApplicationService，计算仍调用 EnglishExerciseSafety，结果与现网一致
L-004  ListeningRelationRepository 不再 SELECT english_reading_article。存在性改为 Reading 已有查询端口（没有则本 PR 只加一个 requireExists/requirePublished 方法）
L-005  IN 列表改为占位符参数，表名/列名只用白名单，不拼接外部输入
L-006  空壳 Application 只在「规则上移后它真正编排」时保留；纯一行转发且无事务价值的，不要再包一层
```

`SegmentTimelinePolicy` 已有规则原样保留，不新增「不得超过总时长」之类产品规则。发音、分段、reading-pairs 路由不变。

### PR-W2-12 `refactor(english-reading): fix policy and cross-domain SQL`

状态：2026-09-24 已落地。发布策略不再依赖仓储；文章删除经共享练习端口；课时引用经 `GrammarLessonPort`；公开练习与判分按 slug 进入用例。

审计 R-02、R-03、R-04、R-05、R-08。R-01 已在 W2-10。

```text
R-001  ReadingPublishPolicy 不再注入 ReadingRelationRepository。标签维度是否齐备由 Application 查好后传入，或经本模块 relation 端口的只读方法
R-002  删除文章时对 english_exercise 的删除改走 shared exercise 端口，ReadingRepository 不写 DELETE FROM english_exercise
R-003  grammar 课时存在性与课时引用改走 grammar 端口，ReadingRelationRepository 不 JOIN english_grammar_lesson
R-004  ReadingPublicController 不再自己先 publicGet 再取练习。提供按 slug 的用例方法，行为与现在相同（草稿详情仍 404）
R-005  判分保持在 application（这是三个技能里正确的位置），补与 listening 上移后的对照测试
```

`ReadingCommandService` 里已经正确的 stats、slug、媒体校验不要搬进仓储。

### PR-W2-13 `refactor(english-grammar): lift rules and unify transactions`

状态：2026-09-24 已落地。章节非空、跨章节编辑、封面、公开大纲过滤和 slug 冲突码在策略或命令服务；仓储写方法不再声明事务。`COURSE_ID = 1` 未改。`GrammarOrderingService` 未改。

审计 G-02、G-03、G-04、G-05、G-06。G-01 已在 W2-10。

```text
G-001  「章节非空不可删」「禁止跨章节编辑」「slug 冲突」「封面校验」「公开大纲只含已发布」从 GrammarRepository 上移到 GrammarCommandService / GrammarPublishPolicy
G-002  全部写方法在 CommandService 声明 @Transactional；Repository 去掉事务注解
G-003  GrammarQueryService 补 @Transactional(readOnly = true)
G-004  slug 只保留「生成器预检 + 数据库唯一约束」。删除重复的第三次预检，冲突时的错误 code 保持不变
G-005  COURSE_ID = 1 不变
```

`GrammarOrderingService` 保持不动。

### PR-W2-14 `refactor(english-writing): lift rules and unify scoring`

状态：2026-09-24 已落地。维度白名单、资源被引用不可删、发布前的标签与范文/模板检查改由策略决定；判分在应用服务。`WritingMoveRequest.targetIndex` 缺省或小于 0 现在返回 400，不再把 null 当成 0。仓储仍是紧凑写法，公开控制器已恢复常规格式。

审计 W-03～W-09。W-01 已在 W2-10。

```text
W-001  维度白名单、被引用不可删、publishedResource/hasTag 上移到 WritingPromptPolicy / WritingResourcePolicy。Policy 与 Repository 里重复的 KINDS/LEVELS 只留 Policy 一处
W-002  WritingExerciseRepository.check 的判分上移到 WritingExerciseApplicationService，与 reading 相同，结果不变
W-003  create/delete/move 补 @Transactional；两个 QueryService 补 readOnly
W-004  WritingMoveRequest 补上与 GrammarMoveRequest 相同的 @NotNull @Min(0)。缺省字段的拒绝行为若与现网不同，先记在 PR 说明里，不要悄悄把 null 变成 0
W-005  恢复 WritingPublicController 与被压成单行的 Repository 的常规格式。不改逻辑
```

rubric、checklist、wordMin/wordMax、TEMPLATE/MODEL_ESSAY 规则与现网逐条一致。

### PR-W2-15 `refactor(english-vocabulary): fill the hollow application layer`

状态：2026-09-24 已落地。`VocabularyIntegrationTest`、`VocabularyReviewPolicyParityTest`、`VocabularyPronunciationIntegrationTest` 通过。分页上限、层号 1～6、主题非空、例句下标、单词空白和音频类型在服务层；目录仓储不再组装视图；学习设置查询不再 `INSERT IGNORE`，没有记录时返回表默认值（showEnglish/showChinese 为 true，方向 MIXED，每日新词 20、复习 200）。词族、复习间隔、FOR UPDATE 和公开路由未改。字段 trim 仍在词汇与音频仓储；主题/单词不存在仍由仓储抛原有 `VOCABULARY_*`。

审计 V-01～V-10、V-12、V-14。

```text
V-001  分层聚合、分页上限、层号 1～6、主题非空才可删、例句下标、AUDIO 类型校验、字段清洗，从四个仓储上移到 Query/Command/Audio 服务
V-002  仓储返回实体或 Optional，由服务抛 VOCABULARY_* 异常。code 与 message 保持不变
V-003  VocabularyCatalogRepository 不再依赖 VocabularyWordViewAssembler。组装留在 Assembler，由服务调用
V-004  事务注解放在服务边界，仓储不再声明业务事务
V-005  VocabularyStudyQueryService 补 readOnly。settings() 里的 INSERT IGNORE 移到显式写路径（首次更新设置或独立 ensure），查询方法不再写库
V-006  词族、复习完成、FOR UPDATE、review_session_id 幂等、5 分钟～60 天间隔，本 PR 不改
V-007  /api/v1/public/vocabulary/** 与 legacy memory 接口保留
```

### PR-W2-16 `refactor(english-shared): stop shared SQL from naming other tables`

状态：2026-09-24 已落地。分类引用计数改由阅读、听力、写作题目和写作资源各自统计；发布状态由各模块提供，共享层不再写业务表名；推荐查询改为调用学习包服务。练习配置和分类规则抛 `EnglishRuleViolation`，状态码仍由 `GlobalExceptionHandler` 映射，code 与 HTTP 状态不变。`EnglishContentChangeAspect` 的参数扫描留 `TODO(W2-16b)`。`EnglishMetaAndTaxonomyIntegrationTest`、`RecommendationEngineTest`、`RecommendationQueryIntegrationTest`、`ReadingIntegrationTest` 通过。

审计 S-08、S-09、A-01、S-10。

```text
S-001  TaxonomyRepository.referencedByContent 改为各内容模块端口的引用计数。删除对 english_*_tag 表名的硬编码
S-002  EnglishContentStateRepository 改为调用 EnglishContentRegistry / DescriptorProvider，不再 switch 表名
S-003  RecommendationQueryService 改为依赖学习包的查询端口或 Service，禁止依赖 LearningBundleItemRepository
S-004  EnglishExercisePolicy、TaxonomyPolicy 改为抛领域异常（现有 code 字符串保留）。HTTP 状态由 GlobalExceptionHandler 或应用层映射
S-005  推荐打分、排序、条数与现有 RecommendationEngine 测试一致
```

`EnglishContentChangeAspect` 的 `firstLong` 启发式（S-17）若本 PR 放不下，单独留 `TODO(W2-16b)`：注解显式声明 id 参数，不在本 PR 改事件 payload。

B 波完成时，V2 计划第 41 节里这些项必须为真：

```text
[ ] Application 不含 JdbcTemplate，Repository 不含发布/判分/标签维度规则
[ ] domain 不依赖 infrastructure
[ ] listening 不读 english_reading_article
[ ] reading 不 JOIN english_grammar_lesson，不直接删 english_exercise
[ ] learning 不依赖 bundle infrastructure
[ ] shared 不硬编码业务表名
[ ] 四个英语 Admin Controller 不再 import ContentReviewService
```

---

## 5. 波次 C — 跨模块 Port

样板已经存在，不要新造第二套媒体接口：

```text
media.api.MediaAssetPort
  isImage / isType / assetType / publicUrl / publicUrls / requireImageIfPresent
english.api.MediaPort + english.shared.media.MediaPortAdapter
```

调用方缺的能力（例如 srcSet）只加到 `MediaAssetPort`，不让调用方继续注入 `MediaAssetMapper` 或 `MediaService`。

### PR-W2-20 `refactor(content): read media through MediaAssetPort`

一个模块一个提交，按这个顺序，避免一个 PR 同时改五处：

| 顺序 | 模块 | 审计编号 | 替换 |
|---|---|---|---|
| 20a | blog | B1、B7 | 已落地：查询走 `publicUrl/publicUrls`；创建和更新封面走 `requireImageIfPresent`。不存在或非图片封面返回 422 `INVALID_COVER_MEDIA` |
| 20b | tutorial | T1、T13 | 已落地：查询走 `publicUrl/publicUrls`；创建和更新封面走 `requireImageIfPresent` |
| 20c | site | S6-1、S7-1 | 已落地。logo/favicon 不存在或非图片现在都是 422 `INVALID_COVER_MEDIA`，不再区分 `MEDIA_NOT_FOUND` 与 `MEDIA_TYPE_INVALID` |
| 20d | portfolio | P6-1 | 已落地。封面与图集走 `MediaAssetPort`，含宽高和 srcSet。封面不存在或非图片为 422 `INVALID_COVER_MEDIA`。`MediaService` 与原型的循环依赖留到 W2-21 |
| 20e | profile | R6-1 | 已落地。头像与简历经 `MediaAssetPort`，错误码仍是 `MEDIA_NOT_FOUND` / `MEDIA_TYPE_INVALID`。选中内容的跨模块 Mapper 留到 W2-22 |

### PR-W2-21 `refactor(media): break the portfolio cycle`

状态：2026-09-24 已落地。ZIP 体积和魔数在 `PrototypeArchivePolicy`。删除 ARCHIVE 时媒体只调 `PrototypeCleanupPort`，不再 `@Lazy` 注入作品集原型服务。`MediaService` 上传主流程未拆。

审计 M6-1、M6-2、P6-2、P7-3、M7-1。

```text
media 定义 PrototypeArchivePolicy（魔数、体积）与 PrototypeCleanupPort
portfolio 实现 CleanupPort，media 删除 ARCHIVE 时只调端口
删除 MediaService 上的 @Lazy PortfolioPrototypeService
ZIP 校验只留一份
```

不在本 PR 拆完 446 行的 `MediaService`。上传主流程留到 W2-30。

### PR-W2-22 `refactor(profile): own selected-content persistence`

审计 R2-1、R2-2、R2-3、R6-2、R3-1。

```text
ProfileSelectedContent 的 DELETE/INSERT 改走已有 ProfileSelectedContentMapper（现在是死代码）或一个 ProfileSelectedContentRepository
存在性校验改为 tutorial/blog/portfolio 的「按 id 是否存在」端口，ProfileCommandService 不再注入这三个 Mapper
公开 About 的 UNION 改为：本表只存 id 和顺序，标题/slug/发布态由对方端口批量提供
QueryService 不再持有 JdbcTemplate
```

响应字段保持 `PublicAboutView` / admin About 的现有 JSON。

已落地。选中行由 `ProfileSelectedContentRepository` 按 `sort_order` 读写。存在性与标题、slug、发布态分别走 `TutorialLookupPort`、`BlogLookupPort`、`PortfolioLookupPort`。`ProfileCommandService` 与 `ProfileQueryService` 不再注入对方 Mapper 或 `JdbcTemplate`。公开页仍跳过未发布和已删除内容；管理端仍只返回 id，顺序与原来按类型归组一致。

### PR-W2-23 `refactor(site): move home SQL behind ports`

审计 S2-1、S2-2。`DashboardRepository` 是正例，不要改它的分层。

```text
站点设置 + logo/favicon：SQL 进 SiteSettingRepository，媒体 URL 走 MediaAssetPort
首页精选项目 / 最新博客 / 最新章节：各模块提供「已发布摘要」端口，SiteQueryService 只做拼装
SiteQueryService 不再出现 jdbc.query
```

首页 JSON 字段不变。

已落地。站点设置读取进 `SiteSettingRepository`，logo 与 favicon 的公开地址走 `MediaAssetPort`。最新章节、最新博客、精选项目、关于预览分别由 `TutorialPublishedPort`、`BlogPublishedPort`、`PortfolioPublishedPort`、`ProfilePreviewPort` 提供。`SiteQueryService` 只做合并、时区格式化和封面地址解析，不再持有 `JdbcTemplate`。排序、条数和跳过未发布内容与原来一致。`DashboardRepository` 未改。

### PR-W2-24 `refactor(search): aggregate module search ports`

审计 D-01、D-02、D-03。

每个被搜模块提供只读投影（id、title、summary、slug、updatedAt、type），`SearchRepository` 不再写 13 张表的 SQL。展示用的「翻译 · 主题名」移到 `SearchService`。排序、分页、type 过滤保持现有行为。

若一次改 13 张表风险过大，按内容域拆提交：站点内容（tutorial/blog/portfolio）→ English → vocabulary。每步搜索结果与改前一致。

已落地。教程、博客、作品集、语法、阅读、听力、写作、词汇各自提供搜索端口，SQL 留在所属模块。`SearchRepository` 已删除。`SearchService` 负责合并、打分、类型过滤、分页，以及词汇摘要「翻译 · 主题名」和写作练习正文拼接。博客仍用 `published_at` 作为活动时间。排序、分页、类型过滤和 JSON 字段不变。

### PR-W2-25 `refactor(seo): read content through ports`

审计 E-01、E-03、E-04、E-05、E-06、E-07。

```text
各模块提供 SEO 投影：path、title、summary、body、slug、publishStatus、updatedAt、coverUrl
SeoContentRepository / SeoSitemapRepository 不再点名他模块表
SeoContentChangeAspect 不再拼接表名，也不再 execution() 绑定 SiteCommandService / ProfileCommandService
站点与关于页的变更改为发布已有 SeoContentChangedEvent（或模块内注解），与 EnglishContentSeoListener 同一条路
SeoController 改为依赖 SeoPageService，不直接调用 Repository
```

HTML 壳、sitemap URL、robots 文本保持现网输出。`EnglishContentSeoListener` 已是事件收口，不要改回切面扫表。

已落地。各模块提供 SEO 卡片和详情投影，封面地址由 `MediaAssetPort` 解析。`SeoPageService` 负责页面拼装，`SeoController` 不再调用 Repository。站点设置与关于页改为发布领域事件，由 `IdentitySeoListener` 转成 `SeoContentChangedEvent`。切面按 kind 向教程、博客、作品集查询可见性，不再拼接表名。

---

## 6. 波次 D — 一致性（不阻塞上线边界）

这些是审计 P2。每条单独 PR，禁止和 A～C 混提。

| 编号 | 内容 | 审计编号 |
|---|---|---|
| W2-30a | 传统模块查询服务补类级 `@Transactional(readOnly = true)`：blog、tutorial、portfolio、profile、site、search、seo、account 查询 | B4、T5、P4-3、R4-1、S4-1、D-04、E-11、A-06 |
| W2-30b | 登录与会话：`AuthController` 与 `AccountAuthController` 的 SecurityContext 落库、`changeSessionId`、CSRF 轮换收成一个用例服务。`AuthService` 不再接收 `HttpServletRequest` | A-03、B-01、B-02、B-03 |
| W2-30c | 拆 `TutorialNodeService` 为分组命令与章节命令。`moveNode` 改为调用无事务的私有实现，避免自调用使事务注解失效 | T2、T6 |
| W2-30d | 拆 `MediaService` 的校验 / 存储 / 上传用例。`ImageVariantSupport` 移出 `service` 包。W2-21 已抽的 Policy 复用 | M2-1、M8-1 |
| W2-30e | 拆 `PortfolioPrototypeService` 的校验、文件、原型行。W2-03 的一致性策略保持 | P8-1 |
| W2-30f | portfolio/profile/site 的 View 组装抽 Assembler。不改 JSON | P5-1、R5-1、S5-2 |
| W2-30g | `EnglishLearningPublicController` 的 7 个 `throw gone()`：确认前端无调用后再删路由；有调用则保留 410，去掉无用参数 | A-02 |
| W2-30h | Account English 的裸 `Map` 改为记录型请求 DTO，JSON 键名不变 | A-15、V-15 |
| W2-30i | 能力清单（`AuthSessionView` 与 `AccountService.capabilities`）收成一处 | B-05、A-13 |

波次 D 已落地。查询服务补了类级只读事务。登录会话的 SecurityContext、换会话号和 CSRF 轮换收进 `SessionEstablishment`，`AuthService` 不再接收请求对象。分组与章节命令分别在 `TutorialGroupCommandService`、`TutorialChapterCommandService`；`moveNode` 在自己的事务里调用无事务的 `placeGroup` / `placeChapter`。媒体校验在 `MediaContentCheck`，落盘在 `MediaFileStore`，`ImageVariantSupport` 在 `media.image`。原型复制、删除和相对路径在 `PortfolioPrototypeFiles`。作品集、站点、关于页的视图组装分别在 `PortfolioViewAssembler`、`SiteViewAssembler`、`ProfileViewAssembler`。游客学习路由仍返回 410。账号英语写请求是记录型 DTO，能力名称在 `AccountCapabilities`。

明确放到更后面、本文件不排期：

```text
SecurityConfig 迁出 common（C-01）
seo HTML 拼装与动态 SQL 的全面 Handler 化（E-03 的剩余部分，W2-25 只切断跨表）
EnglishService 单表 CRUD 是否再包 Repository（E-02，V2 计划写明不要为形式主义先改 Overview）
错误文案中英统一（L-09、R-06）
物理包名再迁移
```

---

## 7. 高严重度闭合清单

第二轮做完 A～C 后，审计里的高严重度项应按下表闭合。未列出的中/低项属于波次 D 或明确后置。

| 审计编号 | 闭合 PR |
|---|---|
| P4-1、M4-1、B2、A-04、A-05、A-08 | W2-01 |
| A-17、A-18 | W2-02 |
| P4-2、M4-2 | W2-03（策略 + 已有 catch 补洞，不要求原子文件系统） |
| G-01、L-01、R-01、W-01、A-01、A-02 | W2-10 |
| L-02、L-03、L-04 | W2-11 |
| R-02、R-03、R-04 | W2-12 |
| G-02 | W2-13 |
| V-03、V-04 | W2-15 |
| S-08、S-09、学习侧 A-01 | W2-16 |
| B1、T1、S6-1、P6-1、R6-1 | W2-20 |
| M6-1、P6-2 | W2-21 |
| R2-1、R2-2、R2-3、R6-2 | W2-22 |
| S2-1、S2-2 | W2-23 |
| D-01 | W2-24 |
| E-01、E-04、E-06 | W2-25 |
| T2、P8-1、M2-1、E-03、B-02 | 波次 D 或 W2-25 的后续，不算 A～C 的完成条件 |

---

## 8. 单次执行模板

```text
你正在修改 LingXI5499/star-rain-notes。

严格按照 backend/docs/backend-refactor-wave2-plan.md 执行 PR-W2-XX。

约束：
1. 先读当前文件。V2 计划里的旧上帝 Service 名称若已不存在，以当前代码为准。
2. 不改变 REST route 与 DTO JSON。
3. 不修改历史 Flyway。
4. 不改变复习间隔、推荐打分、nextReview、判分结果、发布状态机。
5. 不把 SQL 搬回 Application。把规则从 Repository 上移。
6. 不新增无关依赖，不拆微服务。
7. 跑 mvn -q -f backend/pom.xml test。
8. 输出：改动文件、上移的规则、删掉的跨模块依赖、兼容性、测试结果、遗留 adapter。
```

---

## 9. 本轮完成定义

A～C 完成后应同时满足：

```text
写路径要么有事务，要么在 W2-03 文档里说明为何文件 IO 不能进事务
事务回滚不会发出验证码或邀请邮件
英语子域之间不再互相点名对方的业务表
shared 与 learning 不再绕过 Registry / 端口读业务表
四个英语写 Controller 不再决定是否送审
词汇规则在应用服务，仓储不组装 VocabularyWordView
blog、tutorial、site、portfolio、profile 的媒体读取走 MediaAssetPort
media 与 portfolio 不再 @Lazy 互注入
profile、site、search、seo 的 Service/Repository 不再 JOIN 他模块业务表
审核服务不再持有 JdbcTemplate，也不再依赖各模块 CommandService
对外路由、JSON、Flyway 历史、学习算法与现网一致
```
