# 星雨笔录（star-rain-notes）后端分层职责审计报告

| 项目 | 内容 |
| --- | --- |
| 审计对象 | `backend/src/main/java/com/starrainnotes` 全部 **537** 个 Java 文件（12 个顶层模块） |
| 审计范围 | 仅后端。不含 `frontend/`、`prototype-samples/`、`deploy/` |
| 审计日期 | 2026-09-24 |
| 审计方式 | 全量静态阅读 + 量化统计（Glob/Grep/PowerShell 解析）+ 5 组并行子审计交叉核对 |
| 判定依据 | 项目既有约定文档 `backend/docs/english-backend-architecture.md`、`english-v2-*.md` 系列（共 19 篇） |

**一句话结论**：Controller 层整体合格（0 处直连 Mapper、0 处事务注解），但 **Service 层越界写 SQL / 上帝类、Mapper·Repository 层业务规则下沉、事务边界不统一、跨模块直连他模块 Mapper 与表** 四类问题系统性地存在，共记录 **201 条**问题（高 45 / 中 90 / 低 66）。英语模块并非无问题，其仓储层同样存在业务规则下沉与反向依赖。

---

## 一、审计范围与方法

### 1.1 代码规模

顶层模块文件数：

| 模块 | 文件数 | 模块 | 文件数 |
| --- | --- | --- | --- |
| english | 284 | tutorial | 54 |
| account | 45 | blog | 31 |
| portfolio | 24 | site | 21 |
| seo | 17 | auth | 15 |
| profile | 14 | media | 13 |
| common | 10 | search | 8 |
| **合计** | **537** | | |

按分层（以文件所在目录判定）：

| 分层 | 数量 | 说明 |
| --- | --- | --- |
| controller | 44 | HTTP 边界 |
| service | 49 | 传统模块业务服务（含 `MailGateway`/`SmtpMailGateway`） |
| application | 43 | 英语模块应用层用例服务 |
| mapper | 28 | MyBatis-Plus Mapper 接口 |
| infrastructure | 34 | 英语模块持久化实现（`*Repository`） |
| repository | 4 | 非英语模块的 `*Repository`（search 2、seo 2、site 1） |
| assembler | 3 | 仅 blog、tutorial 有 |
| entity | 28 | 持久化实体 |
| dto | 187 | 请求/响应模型（含 view/vo 13） |
| domain | 16 | 英语模块领域策略 |
| provider / api | 14 | 端口与外部服务提供者 |

### 1.2 两套并存的分层风格

- **英语模块（english，284 文件）**：`controller → application → domain/infrastructure`，配套 `api` 端口（`MediaPort`、`EnglishLearningFacade` 等）与 `MediaPortAdapter`。
- **其余 11 个模块（253 文件）**：`controller → service → mapper` 传统三层；site 额外混入 `repository`。

审计即以此「文档既定约定」为基准，逐文件核对职责归属。

### 1.3 问题分类与严重度口径

| 编号 | 类型 |
| --- | --- |
| 1 | Controller 越界（业务分支、SQL、事务、直连 Mapper、组装、会话管理） |
| 2 | Service 越界（写 SQL/JdbcTemplate、HTTP 类型、上帝类、事务失效、跨模块 Mapper） |
| 3 | Mapper/Repository 越界（业务规则、校验、判分、组装下沉到持久化层） |
| 4 | 事务边界（写操作缺 `@Transactional`、只读缺 `readOnly`、自调用失效、事务内副作用） |
| 5 | 分层/命名不一致（service vs application、mapper vs repository、缺 Assembler、Entity 外泄） |
| 6 | 依赖方向（跨模块直连 Mapper/表、domain→infrastructure、模块环、shared 反向依赖） |
| 7 | 校验缺失/重复（DTO 无 Bean Validation、同一校验多处重复） |
| 8 | 空壳类与胖类（纯转发服务、单类职责过载） |

严重度：**高**＝违反架构约定且已产生正确性/一致性风险（事务、数据边界、循环依赖）；**中**＝违反约定但当前无直接故障；**低**＝风格与可维护性欠账。

---

## 二、全项目量化统计

### 2.1 越界行为的硬性证据

以下为可机械验证的量化结果，是本次审计最可靠的结论来源：

| 检查项 | 结果 | 判定 |
| --- | --- | --- |
| Controller 直接调用 Mapper/Repository | **0 次**（44 个 Controller 全量核对） | ✅ 合格 |
| Controller 标注 `@Transactional` | **0 次** | ✅ 合格 |
| Controller 内写 SQL / 使用 JdbcTemplate | **0 次** | ✅ 合格 |
| Controller 返回 Entity（而非 DTO/VO） | 未发现 | ✅ 合格 |
| Controller 跨模块调用他模块 Service | **4 个文件**：`EnglishGrammarAdminController`、`WritingAdminController`、`ReadingAdminController`、`ListeningAdminController` → `account.review.service.ContentReviewService` | ⚠️ 越界 |
| Controller 内做会话/CSRF 管理 | **2 个文件**：`auth/AuthController`、`account/AccountAuthController` | ⚠️ 越界 |
| Service 层使用 `JdbcTemplate` 的类 | **4 个**：`ProfileQueryService`、`ProfileCommandService`、`SiteQueryService`、`account/review/ContentReviewService` | ❌ 越界 |
| `@Transactional` 总数 | 222 处 / 56 个文件 | — |
| `@Transactional(readOnly = true)` | **18 处，100% 位于英语模块** | ⚠️ 传统模块 0 处 |
| 直接依赖**他模块** Mapper 的服务类 | **10 个类**，涉及 5 个模块（profile、portfolio、site、blog、tutorial） | ❌ 越界 |
| 空壳路由（`throw gone()` 占位） | **7 个**（`EnglishLearningPublicController`） | ⚠️ 遗留 |

### 2.2 问题总量与分布

| 报告分册 | 覆盖模块 | 问题数 |
| --- | --- | --- |
| 01 | english/vocabulary、english/shared | 38 |
| 02 | english 的 grammar/listening/reading/writing/learning + 根目录 | 50 |
| 03 | blog、tutorial | 24 |
| 04 | portfolio、profile、site、media | 41 |
| 05 | account、auth、common、search、seo | 48 |
| **合计** | | **201** |

按类型 × 严重度：

| 类型 | 说明 | 合计 | 高 | 中 | 低 |
| --- | --- | --- | --- | --- | --- |
| 1 | Controller 越界 | 28 | 6 | 7 | 15 |
| 2 | Service 越界 | 35 | 9 | 16 | 10 |
| 3 | Mapper/Repository 越界 | 32 | 11 | 14 | 7 |
| 4 | 事务边界 | 25 | 4 | 15 | 6 |
| 5 | 分层/命名不一致 | 27 | 0 | 15 | 12 |
| 6 | **依赖方向/跨模块** | **23** | **13** | 8 | 2 |
| 7 | 校验缺失/重复 | 14 | 0 | 8 | 6 |
| 8 | 空壳类与胖类 | 17 | 2 | 7 | 8 |
| | **合计** | **201** | **45** | **90** | **66** |

**读数**：类型 6（跨模块依赖）问题数不是最多，但**高严重度占比最高（13/23，57%）**，且 45 条高严重度问题中它占 29%，是本次审计最值得优先处理的一类。

### 2.3 各模块问题密度

| 模块 | 文件数 | 问题数 | 密度（问题/文件） | 整体评价摘要 |
| --- | --- | --- | --- | --- |
| seo | 17 | 12 | 0.71 | 跨模块耦合最重：直读 16/11 张他模块表 |
| profile | 14 | 9 | 0.64 | 持久化与跨模块边界双越界 |
| english/vocabulary | 67 | 18 | 0.27 | 胖仓储、瘦服务，仓储反向依赖 Assembler |
| english/listening | 38 | — | — | `ListeningRepository` 649 行上帝类 |
| account | 45 | 21 | 0.47 | 审核服务越界写 SQL 并跨 8 模块编排 |
| search | 8 | 6 | 0.75 | 骨架清晰，但 Repository 跨模块 JOIN 13 张表 |
| site | 21 | 8 | 0.38 | 上帝查询服务 + 6 段手写 SQL |
| media | 13 | 8 | 0.62 | SQL 边界最干净，但 `MediaService` 446 行胖类 |
| tutorial | 54 | 14 | 0.26 | 胖类 + 组装错位 + 跨模块 Mapper |
| blog | 31 | 10 | 0.32 | 总体清晰，三处集中问题 |
| auth | 15 | 6 | 0.40 | 干净但职责错位（Controller 干服务的活） |
| common | 10 | 3 | 0.30 | 质量较高，仅 `SecurityConfig` 反向耦合 |
| portfolio | 24 | 10 | 0.42 | 四模块中最清晰，三处硬伤 |

---

## 三、八类问题结论

### 3.1 类型 1：Controller 越界（28 条，高 6）

Controller 层**基本面合格**（无 SQL、无事务、无 Mapper 直连、不返回 Entity），越界集中在两类「非典型」场景：

1. **送审分支越界（系统性模式，4 个模块同源）**：`EnglishGrammarAdminController`、`WritingAdminController`、`ReadingAdminController`、`ListeningAdminController` 的 `update/updateRule/updatePrompt` 方法内直接 `import com.starrainnotes.account.review.service.ContentReviewService`，在 Controller 里做 `isSuperAdmin + reviewService.isPublished` 组合判断决定是否进入审核流程。业务规则应下沉到对应 `*CommandService`。
2. **认证/会话职责错位（2 处，双向越界）**：[AuthController.java](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/auth/controller/AuthController.java) 直接操作 `SecurityContextRepository`/`CsrfTokenRepository` 完成认证落库、会话固定保护与 CSRF 轮换；[AccountAuthController.java](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/account/controller/AccountAuthController.java#L67) 同样调用 `securityContextRepository.saveContext(...)`。反向地，`AuthService` 又持有 `HttpServletRequest` 做会话失效——形成「Controller 干服务的活、Service 干 Web 的活」。

次要问题：`PortfolioAdminController#uploadPrototype/attachPrototype` 为取得 `publishStatus` 先调 `queryService.adminDetail` 再自行判断；`deletePrototype` 仅为存在性校验而空调一次查询。

### 3.2 类型 2：Service 越界（35 条，高 9）

最普遍、最实质的一类。三个子形态：

- **Service 手写 SQL（`JdbcTemplate`）**：`ProfileQueryService` 写跨表 UNION 并 JOIN `tutorial`/`blog_post`/`portfolio_project` 三张他模块表；`ProfileCommandService` 用裸 SQL 做 `profile_selected_content` 的 DELETE/INSERT（本模块已有 `ProfileSelectedContentMapper` 却未使用，沦为死代码）；`SiteQueryService` 6 处 `jdbc` 调用；`ContentReviewService` 254 行内写 SQL 并跨 8 个模块编排。
- **Service 暴露 HTTP 类型 / 承担 IO**：`PortfolioPrototypeService.upload(Long, MultipartFile, boolean)`、`MediaService.upload(MultipartFile)` 直接接收 `MultipartFile` 并做文件系统落盘；`AuthService` 持有 `HttpServletRequest`。
- **上帝类**：`MediaService`（446 行）混合校验、图像处理、文件 IO、DB、下载、srcSet；`PortfolioPrototypeService`（454 行）混合 ZIP 安全校验、解压落盘、目录复制、DB 管理、媒体联动删除；`SiteQueryService`（205 行）同时承载公开配置 + 首页 + 后台设置三类资源。

### 3.3 类型 3：Mapper/Repository 越界（32 条，高 11）

**「业务规则下沉到持久化层」是本次审计最反直觉的发现，且英语模块同样严重**——这与 `english-backend-architecture.md` 声称的「问题不在缺少注解，而在职责边界」相符，但实际修复并未覆盖仓储层：

- `english/listening/infrastructure/ListeningRepository`（**649 行**）承载标签校验、发布前置校验、字段校验、统计聚合与动态 SQL；判分逻辑落在 `ListeningExerciseRepository`。
- `english/grammar/infrastructure/GrammarRepository`（**475 行**）承载非空校验、跨章节禁止、发布过滤；对应 application 层却是纯转发空壳。
- `english/reading/infrastructure/ReadingRepository`（**405 行**）直接 JOIN grammar 表、直接删 `english_exercise`。
- `english/vocabulary` 的 `VocabularyCatalogRepository`/`VocabularyRepository`/`VocabularyThemeRepository` 把业务规则、分页、校验、404 判定与 DTO 组装全部塞进仓储，并**反向依赖 `english.vocabulary.service` 包下的 `VocabularyWordViewAssembler`**——基础设施层依赖服务层，依赖方向倒置。
- `TaxonomyRepository`、`EnglishContentStateRepository` 在基础设施层硬编码表名直查 reading/listening/writing/grammar 业务表，绕过既有 Port 约定。
- Mapper 内部类型外泄：`BlogTagMapper.BlogTagWithCountRow`、`BlogPostTagMapper.BlogPostTagRow` 被 Service 直接作为方法签名/局部类型使用。

### 3.4 类型 4：事务边界（25 条，高 4）

- **写操作缺 `@Transactional`（高）**：`PortfolioCommandService.delete/publish/withdraw`（仅标 `@SeoContentChange`）、`MediaService.createArchiveAsset`（`mapper.insert` + `mapper.updateById` 两条语句，`updateById` 失败时 catch 只删文件，**残留孤儿 DB 行**）、`BlogTagService.create/update/delete`、account 侧 2 处写路径。
- **文件系统与数据库无原子性**：`PortfolioPrototypeService.bind` 先落盘再写库，`MediaService.upload` 同样，仅靠 catch 手工删文件补偿，JVM 崩溃即残留。
- **只读事务缺失**：`readOnly=true` 全项目仅 18 处且全在英语模块；`PortfolioQueryService`、`ProfileQueryService`、`SiteQueryService`、blog/tutorial 各查询服务、search/seo 的 Repository 均未声明。
- **事务内副作用**：account 侧 2 处在事务内发送邮件，事务回滚后邮件不可撤回。

### 3.5 类型 5：分层/命名不一致（27 条，高 0）

无高危项，但普遍存在：同模块 `mapper` 与 `repository` 并存（site：`SiteSettingMapper` + `DashboardRepository`）；`service` 与 `application` 并存（英语 vocabulary：`service` 与 `application` 两包混用）；**除 blog、tutorial 外全部模块无 Assembler**，响应组装内联在 QueryService（portfolio、profile、site、seo 等）；`ImageVariantSupport`（纯图像工具类）放在 `media.service` 包；`WritingPublicController` 与三个 Repository 被压成单行密集代码，可读性显著低于其他模块。

### 3.6 类型 6：依赖方向 / 跨模块（23 条，高 13）——**最高优先级**

详见第四章专题。

### 3.7 类型 7：校验缺失/重复（14 条，高 0）

- **重复校验**：IMAGE 类型校验在 `PortfolioCommandService`、`ProfileCommandService`、`SiteCommandService` 各写一份；ZIP 魔数/体积校验在 `MediaService`、`PortfolioPrototypeService.validateArchive`、`bind` 三处重复。
- **DTO 缺 Bean Validation**：`UpdateSelectedContentRequest`、`WritingMoveRequest` 等仅靠 Service 兜底。
- **授权矩阵散落**：`SecurityConfig` + Service + Controller 三处各自硬编码角色清单。

### 3.8 类型 8：空壳类与胖类（17 条，高 2）

- **胖类**：`ListeningRepository` 649、`GrammarRepository` 475、`PortfolioPrototypeService` 454、`MediaService` 446、`ReadingRepository` 405、`TutorialNodeService` 317、`TutorialQueryService` 291、`LearningBundleItemService` 269。
- **空壳服务**：`VocabularyQueryService`、`VocabularyCommandService`、`VocabularyAudioService` 为纯转发（业务逻辑已下沉仓储）；`GrammarCommandService` 类同；7 个 `EnglishLearningPublicController` 路由全部 `throw gone()`。

---

## 四、跨模块依赖专题（最高风险）

### 4.1 项目已确立的正确做法

英语模块通过接口 + 适配器解耦媒体依赖，是项目内**唯一正确的跨模块范例**：

```
english 各仓储/服务 ──依赖──> english.api.MediaPort（接口）
                                      ▲ 实现
                            english.shared.media.MediaPortAdapter
                                      │ 委托
                                      ▼
                            media.api.MediaAssetPort（媒体模块对外能力）
```

相关文件：[MediaPort.java](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/english/api/MediaPort.java)、[MediaPortAdapter.java](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/english/shared/media/MediaPortAdapter.java)、[MediaAssetPort.java](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/media/api/MediaAssetPort.java)。

### 4.2 实际存在的越界依赖

| 调用方 | 被依赖对象 | 类型 | 位置 |
| --- | --- | --- | --- |
| portfolio（Command/Query Service） | `media.mapper.MediaAssetMapper` | 直连他模块 Mapper | [PortfolioCommandService.java:7](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/portfolio/service/PortfolioCommandService.java#L7)、[PortfolioQueryService.java:7](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/portfolio/service/PortfolioQueryService.java#L7) |
| portfolio | `media.service.MediaService` | 直连他模块 Service | `PortfolioQueryService:8` |
| profile（Command Service） | `media`/`blog`/`portfolio`/`tutorial` **四个模块的 Mapper** | 直连他模块 Mapper | [ProfileCommandService.java:5-17](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/profile/service/ProfileCommandService.java#L5-L17) |
| profile（Query Service） | `media.mapper.MediaAssetMapper` + 裸 SQL JOIN 3 张他模块表 | Mapper + 数据边界穿透 | `ProfileQueryService:5`、`:72` |
| site（Command Service） | `media.mapper.MediaAssetMapper` | 直连他模块 Mapper | [SiteCommandService.java:5](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/site/service/SiteCommandService.java#L5) |
| site（Query Service） | SQL 直读 `media_asset`/`portfolio_project`/`blog_post`/`tutorial`/`tutorial_node`/`profile` 6 张他模块表 | 数据边界穿透 | `SiteQueryService:52/81/112/133/150` |
| blog（Query Service） | `media.mapper.MediaAssetMapper` | 直连他模块 Mapper | [BlogQueryService.java:25](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/blog/service/BlogQueryService.java#L25) |
| tutorial（Query Service） | `media.mapper.MediaAssetMapper` | 直连他模块 Mapper | [TutorialQueryService.java:8](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/tutorial/service/TutorialQueryService.java#L8) |
| search（Repository） | 直接 JOIN **13 张**他模块表 | 数据边界穿透 | `SearchRepository` |
| seo（Repository/Aspect） | 直读 **16/11/3 张**他模块表，`@SeoContentChange` 注解以表名拼 SQL 且用 `execution` 表达式硬绑 site/profile 类名 | 数据边界穿透 | `SeoContentRepository`、`SeoSitemapRepository`、`SeoContentChangeAspect` |
| english/listening | 直查 `english_reading_article` | 模块内跨域 | `ListeningRepository` |
| english/reading | 直连 grammar 表、直接删 `english_exercise` | 模块内跨域 | `ReadingRepository` |
| english/learning | 依赖 `shared.bundle` 的 Repository | 依赖方向违规 | `RecommendationQueryService` |
| english/reading/domain | `ReadingPublishPolicy` 反向依赖 infrastructure 仓储 | domain→infrastructure 倒置 | `ReadingPublishPolicy` |
| media ↔ portfolio | 双向依赖，用 `@Lazy` 掩盖 | **模块循环依赖** | [MediaService.java:7](file:///e:/star-rain/backend/src/main/java/com/starrainnotes/media/service/MediaService.java#L7)、`:93` |
| english 四技能 Controller | `account.review.service.ContentReviewService` | Controller 跨模块 | grammar/writing/reading/listening Admin Controller |
| common | `SecurityConfig` import auth/account 服务与过滤器 | 横切层反向耦合业务 | `common/security/SecurityConfig.java` |

### 4.3 关键判断

1. **`MediaAssetPort` 已经存在却形同虚设**：portfolio、profile、site、blog、tutorial 五个模块全部绕过它直连 `MediaAssetMapper`。这说明问题不是「缺少解耦设施」，而是**既有设施未被遵循**。
2. **`@Lazy` 是症状不是方案**：media ↔ portfolio 的循环依赖用 `@Lazy` 掩盖，模块边界实际已被打破。
3. **数据边界穿透比 Mapper 直连更隐蔽**：profile/site/search/seo 用裸 SQL 直接 JOIN 他模块表，编译期完全无感，改表结构时会静默失败——这是最难排查、也最该优先收敛的一类。
4. **英语模块是「部分标杆」**：`MediaPort` 范例出自它，但它自身在仓储层同样存在业务规则下沉、反向依赖 Assembler、跨域直查表的问题，不能整体视为合规基线。

---

## 五、修复优先级建议

### P0 — 正确性风险（先修，改动小、收益确定）

1. 为缺事务的写路径补 `@Transactional`：`PortfolioCommandService.delete/publish/withdraw`、`MediaService.createArchiveAsset`、`BlogTagService.create/update/delete`、account 侧 2 处。
2. 修复 `MediaService.createArchiveAsset` 的 insert+update 孤儿行：改为一次插入即带最终 `publicUrl`，或加事务。
3. 明确「文件落盘 vs 数据库写入」的一致性策略并文档化（`MediaService.upload`、`PortfolioPrototypeService.bind`），消除 JVM 崩溃残留。
4. 把事务内发邮件移出事务（account 侧 2 处）。

### P1 — 架构边界（收益最大，需成体系推进）

5. **跨模块依赖收敛（类型 6）**：以 `MediaAssetPort` 为模板，为 profile/site/search/seo 分别定义投影端口，消除「裸 SQL JOIN 他模块表」。建议按 `english-v2-final-boundary-audit.md` 的既有方法论分域小步推进。
6. 解除 media ↔ portfolio 循环依赖：抽 `PrototypeCleanupPort` 由 portfolio 实现，删除 `@Lazy`。
7. 把 Service 层 `JdbcTemplate` 收拢到 Repository（profile 2 处、site 1 处、account/review 1 处），并启用已成为死代码的 `ProfileSelectedContentMapper`。
8. 送审分支从 4 个英语 Admin Controller 下沉到对应 `*CommandService`，统一收口。

### P2 — 职责与一致性（渐进式欠账）

9. 拆分胖类：`MediaService`、`PortfolioPrototypeService`、`SiteQueryService`、`TutorialNodeService`、`ListeningRepository`、`GrammarRepository`。
10. 补齐只读事务：所有查询服务与 Repository 类级 `@Transactional(readOnly = true)`。
11. 抽 Assembler：portfolio、profile、site、seo 的响应组装从 QueryService 移出。
12. 统一持久化命名：site 的 `mapper`/`repository` 混用；英语 vocabulary 的 `service`/`application` 混用。
13. 消除重复校验（IMAGE 类型、ZIP 魔数/体积）并收敛授权矩阵到单一定义点。
14. 补 DTO Bean Validation（`UpdateSelectedContentRequest`、`WritingMoveRequest` 等）。
15. 清理空壳：`VocabularyQueryService`/`CommandService`/`AudioService` 的纯转发方法、7 个 `throw gone()` 路由、死代码 `ProfileSelectedContentMapper`。

### 已确认合格、无需改动的部分

- 全部 44 个 Controller 无 SQL、无事务注解、无 Mapper 直连、不返回 Entity。
- 全部 28 个 Mapper 接口均为空 `BaseMapper`，无 default 业务方法、无互相调用。
- `DashboardQueryService` + `DashboardRepository`（site）是「Service 编排 + Repository 持久化」的正确范例。
- `MediaQueryService`、`MediaAssetPort`、`EnglishContentRegistry` + `EnglishContentDescriptorProvider`、`MediaPort`/`MediaPortAdapter`、`RecommendationEngine`、`ReadingTextStatistics`、`TutorialTreeBuilder`/`TutorialCurriculumBuilder` 均为职责清晰的正面样本。

---

## 六、附录：各模块明细报告

以下 A–E 为逐模块明细，包含**每个业务用例的完整链路**（HTTP 路由 → Controller 方法 → Service 方法 → Mapper/Repository 方法）、逐条问题的文件绝对路径与行号证据、模块统计与整体评价。

---

> **附录 A** — english/vocabulary 与 english/shared

# english/vocabulary 与 english/shared 分层审计

审计范围：
- `e:\star-rain\backend\src\main\java\com\starrainnotes\english\vocabulary\`（66 个 Java 文件）
- `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\`（64 个 Java 文件）

审计方式：逐文件实际阅读 Controller / application / service / mapper / infrastructure / assembler / entity / domain / dto / provider，并追踪每个路由到 Mapper/Repository 的调用链。所有行号、代码片段均来自真实文件。

---

## 模块：english/vocabulary

### 1. 业务逻辑清单

#### 1.1 HTTP 路由（vocabulary 包内 Controller）

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 词汇层/主题卡片目录 | GET `/api/v1/public/vocabulary/themes`、`/layers` | `VocabularyPublicController.themes` | `VocabularyQueryService.listLayers` | `VocabularyCatalogRepository.listLayers`（`VocabularyThemeMapper.selectList` + `JdbcTemplate.query` 统计 + `VocabularyWordViewAssembler`） | 否：仓储层做分层聚合与计数 | V-01,V-03 |
| 主题词条分页 | GET `/api/v1/public/vocabulary/themes/{themeId}/words` | `themeWords` | `VocabularyQueryService.listThemeWords` | `VocabularyCatalogRepository.listThemeWords`（`VocabularyThemeMapper.selectById`、`VocabularyWordMapper.selectCount/selectList`、Assembler） | 否：分页/上限/排序规则写在仓储 | V-03,V-04 |
| 单词卡片详情 | GET `/api/v1/public/vocabulary/words/{wordId}/study`、`/words/{wordId}` | `studyWord` | `VocabularyQueryService.getWord` | `VocabularyCatalogRepository.getWord`→`requireWord`（`VocabularyWordMapper.selectById`）+ Assembler | 否：404 判定在仓储 | V-03,V-06 |
| 单词发音音频代理 | GET `/api/v1/public/vocabulary/pronunciation` | `pronunciation` | `VocabularyPronunciationService.audio` | 无持久化；`PronunciationProvider` + `HttpClient` + 本地文件缓存 | 是（服务职责清晰） | V-09 |
| 批量按 id 取词 | GET `/api/v1/public/vocabulary/words/batch` | `wordsByIds` | `VocabularyQueryService.getWords` | `VocabularyCatalogRepository.getWords`（`VocabularyWordMapper.selectList`） | 否：id 解析/去重/限长/异常处理在 Controller | V-05 |
| 批量按 id 取词（兼容别名） | GET `/api/v1/public/vocabulary/words` | `wordsByIdsCompatibility` | 直接自调用 `wordsByIds` | 同 `getWords` | 否：Controller 内部自调用转发 | V-05 |
| 公共记忆 +1（已废弃） | POST `/api/v1/public/vocabulary/words/{wordId}/memory` | `incrementMemory` | `LegacyVocabularyMemoryService.incrementLegacyMemory` | `VocabularyCatalogRepository.incrementMemory`（`UpdateWrapper.setSql` + Assembler） | 否：写业务在仓储 | V-03 |
| 词典预览 | GET `/api/v1/admin/vocabulary/words/{wordId}/dictionary-preview` | `dictionaryPreview` | 无（直接 `DictionaryProvider.preview`）+ `VocabularyQueryService.getWord` | `VocabularyCatalogRepository.getWord` | 否：Controller 直接调用 Provider | V-07 |
| 后台词条搜索分页 | GET `/api/v1/admin/vocabulary/words` | `words` | `VocabularyQueryService.listWords` | `VocabularyRepository.listWords`（`VocabularyWordMapper.selectCount/selectList`、`VocabularyThemeRepository.idsForLayer`、Assembler） | 否：层号校验、LIKE 转义在仓储 | V-03,V-08 |
| 新建主题 | POST `/api/v1/admin/vocabulary/themes` | `createTheme` | `VocabularyCommandService.createTheme` | `VocabularyThemeRepository.create`（`VocabularyThemeMapper.insert`） | 否：排序规则在仓储 | V-02,V-03 |
| 更新主题 | PUT `/api/v1/admin/vocabulary/themes/{themeId}` | `updateTheme` | `VocabularyCommandService.updateTheme` | `VocabularyThemeRepository.update` | 否：重复查询 + 规则在仓储 | V-02,V-03 |
| 删除主题 | DELETE `/api/v1/admin/vocabulary/themes/{themeId}` | `deleteTheme` | `VocabularyCommandService.deleteTheme` | `VocabularyThemeRepository.delete`（非空校验在仓储） | 否：业务规则在仓储 | V-02,V-03 |
| 新建词条 | POST `/api/v1/admin/vocabulary/words` | `createWord` | `VocabularyCommandService.createWord` | `VocabularyRepository.createWord`（`VocabularyWordMapper.insert`、`JdbcTemplate` 取 sort_order） | 否：清洗/默认值/排序在仓储 | V-02,V-03 |
| 更新词条 | PUT `/api/v1/admin/vocabulary/words/{wordId}` | `updateWord` | `VocabularyCommandService.updateWord` | `VocabularyRepository.updateWord`（`VocabularyWordMapper.updateById`） | 否：同上 | V-02,V-03 |
| 删除词条 | DELETE `/api/v1/admin/vocabulary/words/{wordId}` | `deleteWord` | `VocabularyCommandService.deleteWord` | `VocabularyRepository.deleteWord`（`VocabularyWordMapper.deleteById`） | 否 | V-02,V-03 |
| 追加例句 | POST `/api/v1/admin/vocabulary/words/{wordId}/examples` | `addExample` | `VocabularyCommandService.addExample` | `VocabularyRepository.addExample`（`VocabularyWordMapper.updateById`） | 否 | V-02,V-03 |
| 删除例句 | DELETE `/api/v1/admin/vocabulary/words/{wordId}/examples/{index}` | `removeExample` | `VocabularyCommandService.removeExample` | `VocabularyRepository.removeExample` | 否：越界判定在仓储 | V-02,V-03 |
| 设置记忆数（旧计数器） | PUT `/api/v1/admin/vocabulary/words/{wordId}/memory` | `setMemory` | `LegacyVocabularyMemoryService.setLegacyMemory` | `VocabularyRepository.setMemory` | 否 | V-02,V-03 |
| 新增音频 | POST `/api/v1/admin/vocabulary/words/{wordId}/audio` | `addAudio` | `VocabularyAudioService.addAudio` | `VocabularyAudioRepository.add`（`JdbcTemplate` INSERT、`MediaPort`） | 否：AUDIO 类型校验在仓储 | V-02,V-10 |
| 删除音频 | DELETE `/api/v1/admin/vocabulary/words/{wordId}/audio/{audioId}` | `deleteAudio` | `VocabularyAudioService.deleteAudio` | `VocabularyAudioRepository.delete` | 否 | V-02,V-10 |
| 设为主音频 | PUT `/api/v1/admin/vocabulary/words/{wordId}/audio/{audioId}/primary` | `setPrimaryAudio` | `VocabularyAudioService.setPrimaryAudio` | `VocabularyAudioRepository.setPrimary` | 否 | V-02,V-10 |
| 词族公开详情 | GET `/api/v1/public/english/vocabulary/families/{slug}` | `WordFamilyPublicController.get` | `WordFamilyService.publicGet` | `WordFamilyRepository.idBySlug`→`get`（`JdbcTemplate`） | 是 | — |
| 词族列表 | GET `/api/v1/admin/english/vocabulary/families` | `WordFamilyAdminController.list` | `WordFamilyService.list` | `WordFamilyRepository.ids`→`get` | 是 | — |
| 词族详情 | GET `.../families/{id}` | `get` | `WordFamilyService.get` | `WordFamilyRepository.get` | 是 | — |
| 新建词族 | POST `.../families` | `create` | `WordFamilyService.create` | `WordFamilyRepository.slugExists/insert/replaceLinks` | 是 | — |
| 更新词族 | PUT `.../families/{id}` | `update` | `WordFamilyService.update` | `WordFamilyRepository.update/replaceLinks` | 是 | — |
| 删除词族 | DELETE `.../families/{id}` | `delete` | `WordFamilyService.delete` | `WordFamilyRepository.delete` | 是 | — |
| 新增词族成员 | POST `.../families/{id}/members` | `member` | `WordFamilyService.addMember` | `WordFamilyRepository.nextMemberOrder/insertMember` | 是 | — |
| 更新词族成员 | PUT `.../families/{id}/members/{memberId}` | `member` | `WordFamilyService.updateMember` | `WordFamilyRepository.ownsMember/updateMember` | 是 | — |
| 删除词族成员 | DELETE `.../families/{id}/members/{memberId}` | `deleteMember` | `WordFamilyService.deleteMember` | `WordFamilyRepository.deleteMember` | 是 | — |
| 词族成员排序 | POST `.../families/{id}/members/{memberId}/move` | `move` | `WordFamilyService.move` | `WordFamilyRepository.memberIds/setMemberOrder` | 否：请求体为裸 Map，无校验 | V-11 |

#### 1.2 学习域用例（Service 在 vocabulary 包，HTTP 入口在 `account.english.controller.AccountEnglishController`，属本模块业务逻辑但路由不在审计目录内）

| 业务功能 | HTTP 方法+路由（account 模块） | Controller 方法 | 调用的 Service#方法 | 涉及的 Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 记忆快照 | GET `/api/v1/account/english/vocabulary/memory` | `vocabularyMemory` | `DefaultEnglishVocabularyFacade.memorySnapshot`→`VocabularyStudyQueryService.memorySnapshot` | `VocabularyStudyRepository.memorySnapshot` | 否：只读无 readOnly 事务 | V-12 |
| 设置记忆数 | PUT `/api/v1/account/english/vocabulary/words/{wordId}/memory` | `putVocabularyMemory` | `...putMemoryCount`→`VocabularyStudyCommandService.putMemoryCount` | `VocabularyStudyRepository.putMemoryCount` | 是 | V-13 |
| 学习设置读取 | GET `.../vocabulary/settings` | `vocabularySettings` | `...settings`→`VocabularyStudyQueryService.settings` | `VocabularyStudyRepository.settings`（含 `INSERT IGNORE`） | 否：查询方法内写库 | V-14 |
| 学习设置更新 | PUT `.../vocabulary/settings` | `updateVocabularySettings` | `...updateSettings`→`VocabularyStudyCommandService.updateSettings` | `VocabularyStudyRepository.updateSettings` | 是 | — |
| 词状态批量 | GET `.../vocabulary/states` | `vocabularyStates` | `...memories`→`VocabularyStudyQueryService.memories` | `VocabularyStudyRepository.memories` | 否：只读无 readOnly | V-12 |
| 复习队列 | GET `.../vocabulary/review-queue` | `vocabularyReviewQueue` | `...queue`→`VocabularyStudyQueryService.queue` | `VocabularyStudyRepository.dueCount/dueIds/introducedSince/newWordIds` + `VocabularyQueryService.getWords` | 是（编排逻辑合理） | V-12 |
| 开始学习 | POST `.../vocabulary/words/{wordId}/start` | `startVocabularyWord` | `...start`→`VocabularyStudyCommandService.start` | `VocabularyStudyRepository.start/memory` | 是 | — |
| 提交复习 | POST `.../vocabulary/words/{wordId}/reviews` | `reviewVocabularyWord` | `...completeReview`→`VocabularyStudyCommandService.completeReview` | `VocabularyReviewLogRepository.findBySession/insert`、`VocabularyStudyRepository.lockMemory/advance` | 是（事务+`FOR UPDATE` 正确） | — |
| 重置进度 | DELETE `.../vocabulary/words/{wordId}/progress` | `resetVocabularyWord` | `...reset` | `VocabularyStudyRepository.reset` | 是 | — |
| 设置展示模式 | PUT `.../vocabulary/words/{wordId}/display` | `setVocabularyDisplay` | `...setDisplay` | `VocabularyStudyRepository.setDisplay/memory` | 是 | — |
| 清除展示模式 | DELETE `.../vocabulary/words/{wordId}/display` | `clearVocabularyDisplay` | `...clearDisplay` | `VocabularyStudyRepository.clearDisplay` | 是 | — |
| 学习统计 | GET `.../vocabulary/statistics` | `vocabularyStatistics` | `...progress`→`VocabularyStudyQueryService.progress` | `VocabularyStudyRepository.*Count` + `VocabularyReviewLogRepository.count/countSince/recent` | 否：只读无 readOnly | V-12 |
| 本地进度导入 | POST `.../vocabulary/import-local` | `importLocalVocabulary` | `...importLocal`→`VocabularyProgressImportService.importLocal` | `VocabularyStudyRepository.wordExists/importMemory`、`VocabularyReviewLogRepository.importReview` | 否：裸 Map 入参、手工解析 | V-15 |

> 说明：`VocabularyStudyQueryService` 另有 `memory/optionalMemory`，仅被同包 CommandService 内部调用，不直接对外暴露路由。

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| V-01 | 8 空壳类 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\vocabulary\application\VocabularyQueryService.java:20-24` | `public List<VocabularyLayerView> listLayers() { return catalog.listLayers(); }` | 5 个方法全部是「一行纯转发」，无任何编排/规则；Service 层形同虚设，业务实际落在 `VocabularyCatalogRepository`。 | 中 | 要么把仓储中的规则上移到本服务，要么承认 Repository 即应用层并删除本空壳。 |
| V-02 | 8 空壳类 | `...\vocabulary\application\VocabularyCommandService.java:13-20` | `public VocabularyThemeView createTheme(VocabularyThemeRequest request) { return themes.create(request); }` | 8 个写方法全部纯转发到仓储；`@Transactional` 也写在仓储而非服务边界。 | 中 | 同上，把事务与用例规则收回服务。 |
| V-03 | 3 仓储越界 | `...\vocabulary\infrastructure\VocabularyCatalogRepository.java:62-104` | `for (VocabularyLayer layer : VocabularyLayer.values()) byLayer.put(layer.label(), new ArrayList<>());`（第73行）；`int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);`（第91行） | 仓储承担：分层聚合、主题计数、分页上限、排序、`LIMIT/OFFSET` 拼接（第100行 `wrapper.last("LIMIT " + safeSize + " OFFSET " + ...)`）、以及废弃业务写操作 `incrementMemory`（107-118）。持久层含大量业务规则。 | 高 | 规则/分页/DTO 组装上移到应用服务；仓储只保留 SQL 与行映射。 |
| V-04 | 3 仓储越界 | `...\vocabulary\infrastructure\VocabularyRepository.java:45-76,162-177` | `if (layerOrder < 1 \|\| layerOrder > 6) { throw new ApiException(HttpStatus.BAD_REQUEST, "VOCABULARY_LAYER_INVALID", ...`（53-55）；`private int nextWordOrder(long themeId) { Integer value = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_word WHERE theme_id=?", ...`（173-176） | 仓储内做层号范围校验、LIKE 转义、排序号计算、字段清洗/默认值；同时 `requireTheme` 委托另一仓储（162 行 `themeRepository.requireTheme`）→ 仓储依赖仓储。 | 高 | 校验与清洗移入服务；跨仓储依赖改为依赖 Mapper 或由服务编排。 |
| V-05 | 1 Controller 越界 | `...\vocabulary\controller\VocabularyPublicController.java:76-91` | `parsed = Arrays.stream(ids.split(",")).map(String::trim).filter(...).map(Long::valueOf).distinct().limit(100).toList();`（79-80）；`catch (NumberFormatException ex) { throw new org.springframework.web.server.ResponseStatusException(...`（81-84） | Controller 内做字符串解析、去重、限长、异常到状态码的转换（业务规则）；且 `wordsByIdsCompatibility`（88-91）在 Controller 内部自调用转发，重复路由逻辑。 | 中 | 解析与校验移入服务或独立 DTO（`@RequestParam List<Long> ids` 交给 Spring 转换 + 异常处理器）。 |
| V-06 | 3 仓储越界 | `...\vocabulary\infrastructure\VocabularyCatalogRepository.java:142-158` | `throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_THEME_NOT_FOUND", ...)`（145-146） | 仓储直接抛带 HTTP 语义的 `ApiException`，把「资源不存在」这一应用层判定放进持久层。 | 中 | 仓储返回 `Optional`，由服务抛业务异常。 |
| V-07 | 1 Controller 越界 | `...\vocabulary\controller\VocabularyAdminController.java:58-61` | `return dictionaryProvider.preview(vocabularyQueryService.getWord(wordId).word());` | Controller 直接调用外部 Provider（跨层）并串接两处调用，绕过了服务边界。 | 中 | 新增 `VocabularyDictionaryService` 承载编排，Controller 只调服务。 |
| V-08 | 3 仓储越界 | `...\vocabulary\infrastructure\VocabularyThemeRepository.java:47-65` | `long count=words.selectCount(new QueryWrapper<VocabularyWord>().eq("theme_id",id)); if(count>0) throw new ApiException(HttpStatus.CONFLICT,"VOCABULARY_THEME_NOT_EMPTY",...`（61-63） | 「非空主题不可删除」业务规则写在仓储；`update` 先 `selectById` 再 `requireTheme(id)` 重复查询（49 行）。 | 中 | 规则上移到 `VocabularyCommandService`。 |
| V-09 | 3/5 仓储越界+分层倒置 | `...\vocabulary\infrastructure\VocabularyCatalogRepository.java:44,101` | `private final VocabularyWordViewAssembler wordViewAssembler;`（44）；`List<VocabularyWordView> items = wordViewAssembler.toViews(wordMapper.selectList(wrapper));`（101） | 仓储反向依赖 `service` 包的 Assembler 生成响应 DTO；基础设施→服务层依赖方向倒置，且响应组装下沉到持久层。 | 中 | 组装上移到应用服务，仓储只返回实体。 |
| V-10 | 3 仓储越界 | `...\vocabulary\infrastructure\VocabularyAudioRepository.java:27-31` | `if(!"AUDIO".equals(type)) throw audioRequired();`（31） | 音频类型业务校验写在仓储；另 `SELECT LAST_INSERT_ID()`（35 行）与 `@Transactional` 亦在仓储。 | 中 | 类型校验上移服务。 |
| V-11 | 7 校验缺失 | `...\vocabulary\family\controller\WordFamilyAdminController.java:19` | `@PostMapping("/{id}/members/{memberId}/move")... @RequestBody java.util.Map<String,Integer>r){service.move(id,memberId,r.getOrDefault("targetIndex",0));}` | 排序接口用裸 `Map` 接收请求体，无 DTO、无 Bean Validation；`WordFamilyService.move` 才做 clamp。 | 中 | 定义 `WordFamilyMemberMoveRequest(@NotNull @Min(0) Integer targetIndex)`。 |
| V-12 | 4 事务边界 | `...\vocabulary\learning\application\VocabularyStudyQueryService.java:24-119` | `@Service\npublic class VocabularyStudyQueryService {`（24-25，全类无 `@Transactional`） | 大量只读查询（`queue/progress/memories/settings/memorySnapshot`）无 `readOnly=true` 事务；对比同域写服务有 `@Transactional`，读写边界不对称。 | 中 | 类级加 `@Transactional(readOnly = true)`。 |
| V-13 | 4 事务边界 | `...\vocabulary\family\service\WordFamilyService.java:27-43` | `public List<WordFamilyView> list(String query) {`（27）；`public WordFamilyView get(Long id) {`（33） | 只读方法 `list/get/publicGet` 无 `readOnly=true`（同类的写方法均有 `@Transactional`）。 | 低 | 只读方法补 `@Transactional(readOnly = true)`。 |
| V-14 | 2 Service 越界 | `...\vocabulary\learning\infrastructure\VocabularyStudyRepository.java:31-39` | `public VocabularyStudySettingsView settings(long accountId) { jdbc.update("INSERT IGNORE INTO account_vocabulary_study_setting(account_id) VALUES (?)", accountId); ...` | 名为「查询」的方法在读取时隐式写库（懒初始化），使只读语义被破坏，也让上层难以安全加只读事务。 | 中 | 把默认设置初始化移入显式命令用例，或改用 `INSERT ... ON DUPLICATE` 于写路径。 |
| V-15 | 7 校验缺失/2 胖方法 | `...\vocabulary\learning\application\VocabularyProgressImportService.java:24-67` | `Object raw = payload == null ? null : (payload.containsKey("memory") ? payload.get("memory") : payload.get("vocabulary"));`（25） | 导入接口入参为裸 `Map<String,Object>`（Controller 无 `@Valid`），服务内手工做类型判断、时间解析、枚举白名单；单个方法 40+ 行、职责过多。 | 中 | 定义导入 DTO 并在 HTTP 边界用 Bean Validation，服务只做合并规则。 |
| V-16 | 5 分层命名 | `...\vocabulary\service\VocabularyWordViewAssembler.java:17`（Assembler 在 `service` 包）；`...\vocabulary\application\VocabularyQueryService.java`、`VocabularyCommandService.java`（同名 Service 在 `application` 包） | `@Component\npublic class VocabularyWordViewAssembler {`（16-17） | 同模块内 `service` 与 `application` 两套包并存且职责重叠；Assembler 未放入独立 `assembler` 包。 | 低 | 统一包约定（`application` 放用例、`assembler` 放组装）。 |
| V-17 | 2 Service 越界 | `...\vocabulary\learning\application\VocabularyStudyQueryService.java:53-86` | `int dueCount = repository.dueCount(accountId, now); List<Long> dueIds = repository.dueIds(...); ... List<VocabularyWordView> words = vocabulary.getWords(orderedIds);` | 队列方法内自行组装卡片视图（`VocabularyStudyCardView`）与排序合并，属于响应组装；虽为用例编排尚可，但组装未走 Assembler，与项目「复杂组装放 Assembler」约定不一致。 | 低 | 抽出队列 Assembler。 |
| V-18 | 2 依赖方向 | `...\vocabulary\learning\application\VocabularyStudyQueryService.java:32,69` | `private final VocabularyQueryService vocabulary;`（32）；`List<VocabularyWordView> words = vocabulary.getWords(orderedIds);`（69） | 学习子域直接依赖 vocabulary 目录服务（同模块内），可接受但需注意 `DefaultEnglishVocabularyFacade` 与 `VocabularyStudyCommandService` 均重复注入 `VocabularyQueryService`，存在多点耦合。 | 低 | 待确认：是否应统一经 `EnglishVocabularyFacade` 或共享读端口。 |

### 3. 该模块统计

**类数量（按包角色）**
- controller：4（`VocabularyPublicController`、`VocabularyAdminController`、`WordFamilyPublicController`、`WordFamilyAdminController`）
- application：8（含 `learning\application` 3 个：`VocabularyStudyQueryService`、`VocabularyStudyCommandService`、`VocabularyProgressImportService`）
- service：3（`VocabularyPronunciationService`、`VocabularyWordViewAssembler`、`WordFamilyService`）
- mapper：2（`VocabularyWordMapper`、`VocabularyThemeMapper`）
- infrastructure：6（`VocabularyCatalogRepository`、`VocabularyRepository`、`VocabularyThemeRepository`、`VocabularyAudioRepository`、`VocabularyWordRelationRepository`、`WordFamilyRepository`）
- repository：0（全部落在 `infrastructure`，无独立 `repository` 包）
- assembler：1（`VocabularyWordViewAssembler`，实际位于 `service` 包）
- entity：3（`VocabularyWord`、`VocabularyTheme`、`VocabularyExample`）
- domain：4（`VocabularyLayer` + `learning\domain` 3 个）
- provider：6；config：2；dto：25（含 family 4、learning 10）
- 合计 66 个 Java 文件

**越界计数**
- Controller 直接调用 Mapper：0 次
- Controller 含 `@Transactional`：0 次
- Service 使用 `JdbcTemplate`：0 次
- 仓储层（infrastructure）承载业务规则/校验/DTO 组装的类：4 个（`VocabularyCatalogRepository`、`VocabularyRepository`、`VocabularyThemeRepository`、`VocabularyAudioRepository`）
- 纯转发空壳服务：3 个（`VocabularyQueryService`、`VocabularyCommandService`、`VocabularyAudioService`）
- 只读方法缺 `readOnly=true` 的类：3 个（`VocabularyStudyQueryService`、`WordFamilyService`、`LegacyVocabularyMemoryService`）

**跨模块依赖对方 Mapper 清单**
- 无。词汇模块对外仅依赖 `english.api.MediaPort` 端口（`VocabularyWordRelationRepository:5`、`VocabularyAudioRepository:7`），符合既定 Port 约定。

**行数最多 5 个类**
1. `learning\infrastructure\VocabularyStudyRepository.java` — 245
2. `infrastructure\VocabularyRepository.java` — 191
3. `family\infrastructure\WordFamilyRepository.java` — 176
4. `family\service\WordFamilyService.java` — 168
5. `entity\VocabularyWord.java` — 167（实体样板）；非实体第 5 为 `infrastructure\VocabularyCatalogRepository.java` / `service\VocabularyPronunciationService.java` — 160

**public 方法最多 5 个类**（实体 getter/setter 计入，故实体类数值偏高）
1. `learning\infrastructure\VocabularyStudyRepository.java` — 22
2. `family\infrastructure\WordFamilyRepository.java` — 18
3. `application\DefaultEnglishVocabularyFacade.java` — 15
4. `controller\VocabularyAdminController.java` — 14
5. `family\service\WordFamilyService.java` — 10

### 4. 整体评价（200 字以内）
词汇模块路由与 Port 依赖方向基本正确，学习域（`learning`）事务、并发锁与策略拆分质量较高。主要问题是「应用层空心化」：`VocabularyQueryService/CommandService/AudioService` 为纯转发空壳，业务规则、分页、校验、404 判定与响应组装大量沉淀在 `VocabularyCatalogRepository/VocabularyRepository/VocabularyThemeRepository`，形成「胖仓储、瘦服务」；仓储还反向依赖 service 包的 Assembler。此外只读查询缺 `readOnly=true`、Controller 内做参数解析与异常转换、裸 Map 请求体无校验。建议把规则与组装上移到服务、仓储只留 SQL。

---

## 模块：english/shared

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 公共元数据聚合 | GET `/api/v1/public/english/meta` | `EnglishMetaController.meta` | `EnglishTaxonomyService.tree`、`CefrService.list`、`EnglishExercisePolicy.questionTypes` | `TaxonomyRepository.all`（`EnglishTaxonomyTermMapper`）；`EnglishCefrStandardMapper.selectList` | 否：Controller 内 new 视图 | S-01 |
| 学习包列表（后台） | GET `/api/v1/admin/english/bundles` | `BundleAdminController.list` | `LearningBundleService.list` | `LearningBundleRepository.list`（`JdbcTemplate`） | 是 | — |
| 新建学习包 | POST `.../bundles` | `create` | `LearningBundleService.create` | `LearningBundleRepository.slugExists/nextOrder/insert`（`EnglishLearningBundleMapper`） | 是 | — |
| 学习包详情 | GET `.../bundles/{id}` | `get` | `LearningBundleService.get` | `LearningBundleRepository.detail` | 是 | — |
| 更新学习包 | PUT `.../bundles/{id}` | `update` | `LearningBundleService.update` | `LearningBundleRepository.update` | 是 | — |
| 删除学习包 | DELETE `.../bundles/{id}` | `delete` | `LearningBundleService.delete` | `LearningBundleRepository.delete` | 是 | — |
| 发布学习包 | POST `.../bundles/{id}/publish` | `publish` | `LearningBundleService.publish`→`LearningBundleItemService.assertPublishable` | `LearningBundleItemRepository.metadata/members`、`LearningBundleRepository.publish` | 是 | S-04 |
| 撤回学习包 | POST `.../bundles/{id}/withdraw` | `withdraw` | `LearningBundleService.withdraw` | `LearningBundleRepository.withdraw` | 是 | — |
| 学习包条目列表 | GET `.../bundles/{id}/items` | `items` | `LearningBundleItemService.list` | `LearningBundleItemRepository.members` + `EnglishContentRegistry.require`（各业务模块 Provider） | 是（Port 模式） | — |
| 学习包内容目录（分页筛选） | GET `.../bundles/{id}/catalog` | `catalog` | `LearningBundleItemService.catalog` | `EnglishContentRegistry.catalog`（各模块 Repository）+ `LearningBundleItemRepository.members` | 否：目录排序/分页/视图组装在服务 | S-05 |
| 学习包可发布性 | GET `.../bundles/{id}/readiness` | `readiness` | `LearningBundleItemService.readiness` | `LearningBundleItemRepository.metadata/members` | 是 | — |
| 添加条目 | POST `.../bundles/{id}/items` | `addItem` | `LearningBundleItemService.add` | `LearningBundleItemRepository.add/members` | 是 | — |
| 条目排序 | POST `.../bundles/{id}/items/{contentType}/{contentId}/move` | `moveItem` | `LearningBundleItemService.move` | `LearningBundleItemRepository.members/updateOrder` | 是 | — |
| 删除条目 | DELETE `.../bundles/{id}/items/{contentType}/{contentId}` | `removeItem` | `LearningBundleItemService.remove` | `LearningBundleItemRepository.remove/members/updateOrder` | 是 | — |
| 公开学习包列表 | GET `/api/v1/public/english/bundles` | `BundlePublicController.list` | `LearningBundleService.publicList` | `LearningBundleRepository.publishedList` + `LearningBundleItemService.publiclyAccessible` | 是 | — |
| 公开学习包详情 | GET `.../bundles/{slug}` | `get` | `LearningBundleService.publicGet` | `LearningBundleRepository.publishedBySlug` | 是 | — |
| 公开学习包条目 | GET `.../bundles/{slug}/items` | `items` | `LearningBundleItemService.publicList` | `LearningBundleItemRepository.publishedIdBySlug/members` | 否：吞掉所有异常 | S-06 |
| 分类树/平铺 | GET `/api/v1/admin/english/taxonomy` | `TaxonomyAdminController.list` | `EnglishTaxonomyService.tree/flat` | `TaxonomyRepository.all` | 否：Controller 内分支 | S-02 |
| 新建分类 | POST `.../taxonomy` | `create` | `EnglishTaxonomyService.create`→`TaxonomyCommandService.create` | `TaxonomyRepository.slugExists/nextOrder/insert` | 是 | — |
| 更新分类 | PUT `.../taxonomy/{id}` | `update` | `...update`→`TaxonomyCommandService.update` | `TaxonomyRepository.countChildren/slugExists/update` | 是 | — |
| 分类排序 | POST `.../taxonomy/{id}/move` | `move` | `...move`→`TaxonomyCommandService.move` | `TaxonomyRepository.siblingIds/normalizeOrder` | 否：仓储拼 SQL | S-07 |
| 删除分类 | DELETE `.../taxonomy/{id}` | `delete` | `...delete`→`TaxonomyCommandService.delete` | `TaxonomyRepository.countChildren/referencedByContent/delete` | 否：跨模块表直查 | S-08 |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| S-01 | 1 Controller 越界 | `...\english\shared\controller\EnglishMetaController.java:32-34` | `return new EnglishMetaView(taxonomyService.tree(), cefrService.list(), exerciseService.questionTypes());` | Controller 直接 new 并组装响应 DTO（3 个来源聚合），响应组装未走服务/Assembler。 | 低 | 抽出 `EnglishMetaService.meta()` 或 Assembler。 |
| S-02 | 1 Controller 越界 | `...\english\shared\taxonomy\controller\TaxonomyAdminController.java:37-39` | `return "flat".equalsIgnoreCase(view) ? service.flat() : service.tree();` | 视图模式分支（业务选择）写在 Controller。 | 低 | 分支下沉到服务，按枚举参数路由。 |
| S-03 | 8 胖类 | `...\english\shared\bundle\service\LearningBundleItemService.java:29-268` | `public class LearningBundleItemService {`（30），269 行、9 个 public 方法 | 单类同时承担：条目 CRUD、排序归一、内容目录分页/排序/筛选组装、可发布性清单、公开可访问判定。职责过载，是最胖类。 | 中 | 拆为 `BundleItemCommandService` / `BundleCatalogService` / `BundleReadinessPolicy`。 |
| S-04 | 2 Service 越界 | `...\english\shared\bundle\service\LearningBundleItemService.java:116-127` | `public void assertPublishable(Long bundleId) { if (!readiness(bundleId).ready()) { throw new ApiException(...` | 发布清单规则（`SUMMARY_REQUIRED`/`READING_REQUIRED`…）与「已发布两模块包仍可读」的兼容逻辑（124-127）混在条目服务中，属于发布策略，应独立。 | 中 | 抽 `BundlePublishPolicy`。 |
| S-05 | 2 Service 越界 | `...\english\shared\bundle\service\LearningBundleItemService.java:54-93` | `matches.sort(Comparator.comparingInt((ContentDescriptor descriptor) -> statusOrder(descriptor.publishStatus()))...`（74-77）；`new BundleCatalogItemView(...)`（85-89） | 服务内做跨模块候选合并、排序、`subList` 分页、以及 DTO 组装（无 Assembler）。 | 中 | 组装抽 Assembler，分页排序收敛为独立查询组件。 |
| S-06 | 2 Service 越界 | `...\english\shared\bundle\service\LearningBundleItemService.java:167-176` | `try { id = repository.publishedIdBySlug(slug); } catch (Exception ex) { throw unavailable(); }` | 用 `catch (Exception)` 兜住仓储异常并转成 404，把「异常捕获当业务逻辑」，会掩盖真实故障（如连接失败被报为 404）。 | 中 | 捕获 `EmptyResultDataAccessException` 等具体异常。 |
| S-07 | 3 仓储越界 | `...\english\shared\taxonomy\infrastructure\TaxonomyRepository.java:77-85` | `jdbc.update("UPDATE english_taxonomy_term SET sort_order=100000 WHERE id IN (" + String.join(",", ids.stream().map(String::valueOf).toList()) + ")");` | 仓储内做两阶段排序归一，且把 id 列表字符串拼接进 SQL（`IN (...)` 非参数化）；`100000` 魔数体现业务规则下沉。 | 中 | 用 `IN (?,?,…)` 参数化；归一算法上移服务。 |
| S-08 | 6 依赖方向/3 仓储越界 | `...\english\shared\taxonomy\infrastructure\TaxonomyRepository.java:17-19,99-112` | `private static final List<String> CONTENT_REFERENCE_TABLES = List.of("english_reading_article_tag", "english_listening_item_tag", "english_writing_resource_tag", "english_writing_prompt_tag");`（17-19） | shared 基础设施直接枚举并查询 reading/listening/writing 业务模块的关联表，绕过各业务模块边界；`referencedByContent` 亦在仓储内做引用判定。 | 高 | 通过各业务模块暴露的 Port（如 `ContentReferencePort`）判断引用，禁止 shared 直连业务表。 |
| S-09 | 6 依赖方向/3 仓储越界 | `...\english\shared\events\infrastructure\EnglishContentStateRepository.java:16-30` | `String table = switch (kind) { case READING -> "english_reading_article"; case LISTENING -> "english_listening_item"; case GRAMMAR_LESSON -> "english_grammar_lesson"; ...`（17-25） | shared 基础设施通过硬编码表名直接读取四个业务模块的发布状态表，与「各域通过 Provider/Port 暴露描述符」的既有约定冲突，形成 shared→业务表 的隐式耦合。 | 高 | 复用 `EnglishContentDescriptorProvider.require(id)`（已有 `published()`）替代直查表。 |
| S-10 | 6 分层/5 命名 | `...\english\shared\exercise\domain\EnglishExercisePolicy.java:6-7`、`...\english\shared\taxonomy\domain\TaxonomyPolicy.java:5-6` | `import org.springframework.http.HttpStatus;`（EnglishExercisePolicy:6）；`throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID", ...`（TaxonomyPolicy:18-19） | `domain` 包内对象被 `@Component` 标注并依赖 HTTP 状态码（`HttpStatus`），使领域层耦合 Web 层；按约定状态码应由 Controller/异常处理器决定。 | 中 | 领域抛领域异常（无 HTTP 语义），在边界层映射状态码。 |
| S-11 | 5 分层命名 | `...\english\shared\exercise\service\EnglishExerciseSafety.java:31-32` | `@Service\npublic class EnglishExerciseSafety {` | 纯 JSON 变换 + 评分（无持久化、无外部依赖）的领域逻辑被放在 `service` 包，与 `domain` 包中的 `EnglishExercisePolicy` 分离，职责归类不一致。 | 低 | 若确无 I/O，移入 `domain` 或重命名以反映「策略/工具」性质。 |
| S-12 | 5 分层命名 | `...\english\shared\taxonomy\service\EnglishTaxonomyService.java:15-34` | `/** Compatibility facade for existing taxonomy controllers. */`（15）；`public List<TaxonomyTermView> tree() { return queries.tree(); }`（27） | `EnglishTaxonomyService` 为纯转发兼容门面（7 个方法全转发），与 `TaxonomyQueryService/TaxonomyCommandService` 三套并存；命名上 `service` 与 `application` 混用。 | 低 | 兼容层标注 `@Deprecated` 并规划删除，统一到 `application` 包。 |
| S-13 | 2 Service 越界 | `...\english\shared\cefr\service\CefrService.java:28-40` | `.sorted(Comparator.comparingInt(level -> ORDER.indexOf(level.getLevel())))`（30）；`return new CefrLevelView(c.getLevel(), c.getVocabMin(), ...`（36-39） | CEFR 排序规则（硬编码 `ORDER` 列表）与视图组装都在服务内完成，且直接依赖 Mapper（简单只读尚可，但排序属业务规则）。 | 低 | 排序规则可用 `sort_order` 列或枚举表达；组装抽 Assembler。 |
| S-14 | 2 Service 越界 | `...\english\shared\taxonomy\application\TaxonomyQueryService.java:81-88` | `public int depthOf(Long id) { return require(id).getParentId() == null ? 0 : 1; }`；`public List<Long> ancestorIds(Long id) { Long parentId = require(id).getParentId(); return parentId == null ? List.of() : List.of(parentId); }` | 用「0/1」硬编码表达最多两层的层级模型，属业务规则写死在查询服务；若层级规则变化需改动多处（`TaxonomyPolicy` 亦重复该假设）。 | 低 | 由 `TaxonomyPolicy` 统一提供层级语义。 |
| S-15 | 4 事务边界 | `...\english\shared\bundle\service\LearningBundleService.java:35-48`；`...\LearningBundleItemService.java:42-127` | `public List<BundleView> list() { return repository.list(); }`（35）；`public List<BundleItemView> list(Long bundleId, boolean publishedOnly) {`（42） | 大量只读方法（`list/publicList/get/publicGet/catalog/readiness`）未加 `readOnly=true`，而同模块写方法有 `@Transactional`。 | 中 | 类级补 `@Transactional(readOnly = true)`，写方法覆盖为可写。 |
| S-16 | 3 仓储越界 | `...\english\shared\bundle\infrastructure\LearningBundleItemRepository.java:93-109` | `private String table(String type) { return switch (type) { case "READING" -> "english_learning_bundle_reading_item"; ... default -> throw new IllegalArgumentException("Unsupported bundle content type: " + type); }; }` | 仓储内维护类型→表名/列名的业务映射，并以字符串拼接方式构造 SQL 表名；映射规则属应用层知识。 | 低 | 由服务把已校验的枚举传入，或用常量枚举集中管理映射。 |
| S-17 | 2 Service 越界 | `...\english\shared\events\EnglishContentChangeAspect.java:23-41` | `Long id = firstLong(invocation.getArgs());`（24）；`private Long firstLong(Object[] args) { for (Object arg : args) if (arg instanceof Long value) return value; return null; }`（38-41） | 切面用「第一个 Long 参数即内容 id」的启发式推断业务语义，脆弱（参数顺序变化即静默失效）；事件发布属于横切逻辑但含业务假设。 | 低 | 通过注解显式声明 id 参数位置或参数名（SpEL）。 |
| S-18 | 5/6 分层 | `...\english\shared\content\DefaultEnglishContentFacade.java:31-42` | `if (!descriptor.published()) { return new ContentReadiness(descriptor, false, List.of("CONTENT_UNPUBLISHED")); }`（33-35） | 门面实现内含「可用性判定 + 依赖未发布」业务规则并以 `ApiException` 控制流（try/catch）实现；`CONTENT_UNPUBLISHED/DEPENDENCY_UNPUBLISHED` 语义散落在门面，未抽策略。 | 低 | 抽 `ContentReadinessPolicy`。 |
| S-19 | 7 校验缺失 | `...\english\shared\bundle\controller\BundleAdminController.java:79-89` | `@RequestParam(defaultValue = "PUBLISHED") String status, ... int page, int pageSize` | 目录查询的 `status/page/pageSize` 无约束（无 `@Min`/枚举校验），校验依赖服务内 `optionalStatus` 兜底，Controller 与 Service 校验职责不清晰。 | 低 | 用枚举 + `@Min(1)` 约束，或统一由服务校验并移除重复。 |
| S-20 | 1 Controller 越界 | `...\english\shared\bundle\controller\BundleAdminController.java:74-114` | `public java.util.List<com.starrainnotes.english.shared.bundle.dto.BundleItemView> items(...)`（75） | Controller 使用全限定类名内联、且同一控制器同时注入 `LearningBundleService` 与 `LearningBundleItemService` 两个服务承担两个资源的 CRUD（14 个路由），偏胖；属组织问题。 | 低 | 拆分为 bundle 与 bundle-item 两个控制器，规范 import。 |

### 3. 该模块统计

**类数量（按包角色）**
- controller：4（`EnglishMetaController`、`BundleAdminController`、`BundlePublicController`、`TaxonomyAdminController`）
- service：5（`LearningBundleService`、`LearningBundleItemService`、`CefrService`、`EnglishTaxonomyService`、`EnglishExerciseSafety`）
- application：2（`TaxonomyQueryService`、`TaxonomyCommandService`）
- infrastructure：4（`LearningBundleRepository`、`LearningBundleItemRepository`、`TaxonomyRepository`、`EnglishContentStateRepository`）
- mapper：4（`EnglishLearningBundleMapper`、`EnglishTaxonomyTermMapper`、`EnglishCefrStandardMapper`、`EnglishExerciseMapper`）
- assembler：0
- entity：4；domain：2（`EnglishExercisePolicy`、`TaxonomyPolicy`）
- content/events/media：9（`EnglishContentRegistry`、`DefaultEnglishContentFacade`、`DefaultEnglishReviewContentPort`、`EnglishContentDescriptorProvider`、`ContentDescriptor`、`EnglishContentType`、`ContentCatalogFilter`、`ContentCatalogSlice`、`TagMatchCandidate`、`EnglishContentChangeAspect`、`MediaPortAdapter` 等）
- dto：17；合计 64 个 Java 文件

**越界计数**
- Controller 直接调用 Mapper：0 次
- Controller 含 `@Transactional`：0 次
- Service 使用 `JdbcTemplate`：0 次
- 无 Assembler 类：响应组装散落在 `EnglishMetaController`、`LearningBundleItemService.catalog`、`CefrService`、`TaxonomyQueryService`
- 只读方法缺 `readOnly=true` 的类：2 个（`LearningBundleService`、`LearningBundleItemService`）

**跨模块依赖对方 Mapper 清单**
- 无（未 import 任何业务模块的 Mapper/Repository）。
- 但存在**跨模块直查业务表**（非 Mapper，同为越界）：
  - `TaxonomyRepository:17-19,104` 直查 `english_reading_article_tag`、`english_listening_item_tag`、`english_writing_resource_tag`、`english_writing_prompt_tag`
  - `EnglishContentStateRepository:17-26` 直查 `english_reading_article`、`english_listening_item`、`english_grammar_lesson`、`english_writing_prompt`、`english_writing_resource`、`english_listening_pronunciation_rule`

**行数最多 5 个类**
1. `bundle\service\LearningBundleItemService.java` — 269
2. `exercise\domain\EnglishExercisePolicy.java` — 210
3. `bundle\service\LearningBundleService.java` — 171
4. `exercise\service\EnglishExerciseSafety.java` — 171
5. `taxonomy\infrastructure\TaxonomyRepository.java` — 132

**public 方法最多 5 个类**（实体 getter/setter 计入，故实体类偏高）
1. `bundle\infrastructure\LearningBundleRepository.java` — 13
2. `taxonomy\infrastructure\TaxonomyRepository.java` — 13
3. `taxonomy\application\TaxonomyQueryService.java` — 10
4. `bundle\service\LearningBundleItemService.java` — 9
5. `bundle\service\LearningBundleService.java` — 9

### 4. 整体评价（200 字以内）
shared 模块整体分层规范：Controller 无 SQL/无事务、无反向依赖具体业务模块的 Mapper，学习包经 `EnglishContentDescriptorProvider` 端口访问各域，事务边界基本清晰。主要问题集中在两处：一是 `TaxonomyRepository` 与 `EnglishContentStateRepository` 在基础设施层硬编码表名直查 reading/listening/writing/grammar 业务表，绕过既有 Port 约定，是最高风险的隐式耦合；二是 `LearningBundleItemService`（269 行）胖类，目录分页/排序/组装、发布清单、公开可访问判定混于一体，且用 `catch (Exception)` 兜底。此外 domain 对象依赖 `HttpStatus`、只读方法缺 `readOnly` 事务、组装类缺失。建议优先收敛跨模块表访问与胖类拆分。


---

> **附录 B** — english 技能层：grammar / listening / reading / writing / learning

# english 技能模块与学习层分层审计

审计范围（`e:\star-rain\backend\src\main\java\com\starrainnotes\english\`）：

- `grammar\`（19 个 Java 文件）、`listening\`（38）、`reading\`（25）、`writing\`（28）、`learning\`（32）
- 根目录 `service\`(1) / `controller\`(2) / `mapper\`(1) / `entity\`(1) / `dto\`(2) / `api\`(7) / 根级 1 个，以及 `shared\`(60) 中直接承载 english 编排的 bundle / content / events / taxonomy / cefr / exercise

审计方式：逐文件实际阅读 Controller / application / service / domain / infrastructure / mapper / entity / dto，并追踪每个路由方法到最终 Mapper/Repository 方法。所有行号、代码片段均来自真实文件；不确定处以「待确认」标注。

审计基准文档：`english-backend-architecture.md`、`english-v2-api-facades.md`、`english-v2-final-boundary-audit.md` 及各 `english-v2-*-refactor.md`。

问题类型编号：1 Controller 越界｜2 Service 越界｜3 Mapper/Repository 越界｜4 事务边界｜5 分层/命名不一致｜6 依赖方向｜7 校验缺失或重复｜8 空壳类与胖类。

---

## 模块一：english/grammar

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 公开课程大纲 | GET `/api/v1/public/english/grammar` | `EnglishGrammarPublicController.curriculum` | `GrammarQueryService.publicCurriculum` | `GrammarRepository.publicCurriculum`→`buildCurriculum` | 否：发布过滤在仓储 | G-02 |
| 公开课时详情 | GET `/api/v1/public/english/grammar/lessons/{slug}` | `lesson` | `GrammarQueryService.publicLesson` | `GrammarRepository.publicLesson`（`requirePublished`） | 否：发布判定在仓储 | G-02 |
| 课程单例查询 | GET `/api/v1/admin/english/grammar` | `EnglishGrammarAdminController.course` | `GrammarQueryService.course` | `GrammarRepository.course` | 是 | — |
| 课程更新 | PUT `/api/v1/admin/english/grammar` | `updateCourse` | `GrammarCommandService.updateCourse` | `GrammarRepository.updateCourse` | 否：无事务注解 | G-03,G-04 |
| 课程发布 | POST `/api/v1/admin/english/grammar/publish` | `publishCourse` | `GrammarCommandService.publishCourse` | `GrammarRepository.publishCourse` + `GrammarPublishPolicy` | 否：策略校验与写库分散无事务 | G-03 |
| 课程撤回 | POST `/api/v1/admin/english/grammar/withdraw` | `withdrawCourse` | `GrammarCommandService.withdrawCourse` | `GrammarRepository.withdrawCourse` | 否：同上 | G-03 |
| 大纲（管理） | GET `/api/v1/admin/english/grammar/curriculum` | `curriculum` | `GrammarQueryService.curriculum` | `GrammarRepository.curriculum` | 是 | — |
| 新建章节 | POST `/api/v1/admin/english/grammar/sections` | `createSection` | `GrammarCommandService.createSection` | `GrammarRepository.createSection` | 否：无事务注解 | G-03,G-04 |
| 更新章节 | PUT `/api/v1/admin/english/grammar/sections/{sectionId}` | `updateSection` | `GrammarCommandService.updateSection` | `GrammarRepository.updateSection` | 否：同上 | G-03,G-04 |
| 删除章节 | DELETE `/api/v1/admin/english/grammar/sections/{sectionId}` | `deleteSection` | `GrammarCommandService.deleteSection` | `GrammarRepository.deleteSection`（非空校验在仓储） | 否：业务规则在仓储 | G-02,G-03 |
| 章节排序 | POST `/api/v1/admin/english/grammar/sections/{sectionId}/move` | `moveSection` | `GrammarCommandService.moveSection` | `GrammarOrderingService.moveSection`→`GrammarRepository.moveSection` | 是（好范例） | — |
| 新建课时 | POST `/api/v1/admin/english/grammar/lessons` | `createLesson` | `GrammarCommandService.createLesson` | `GrammarRepository.createLesson` | 否：无事务注解 + slug 三重校验 | G-03,G-06 |
| 课时详情（管理） | GET `/api/v1/admin/english/grammar/lessons/{lessonId}` | `lesson` | `GrammarQueryService.lesson` | `GrammarRepository.lesson` | 是 | — |
| 课时更新（含送审分支） | PUT `/api/v1/admin/english/grammar/lessons/{lessonId}` | `updateLesson` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `GrammarCommandService.updateLesson` | `GrammarRepository.updateLesson`（跨章节校验在仓储） | 否：Controller 越界 + 业务规则在仓储 | G-01,G-02 |
| 删除课时 | DELETE `/api/v1/admin/english/grammar/lessons/{lessonId}` | `deleteLesson` | `GrammarCommandService.deleteLesson` | `GrammarRepository.deleteLesson` | 否：无事务注解 | G-03,G-04 |
| 课时发布 | POST `/api/v1/admin/english/grammar/lessons/{lessonId}/publish` | `publishLesson` | `GrammarCommandService.publishLesson` | `GrammarRepository.publishLesson` + `GrammarPublishPolicy` | 部分（service 有 @Transactional） | — |
| 课时撤回 | POST `/api/v1/admin/english/grammar/lessons/{lessonId}/withdraw` | `withdrawLesson` | `GrammarCommandService.withdrawLesson` | `GrammarRepository.withdrawLesson` + `GrammarPublishPolicy` | 部分 | — |
| 课时排序 | POST `/api/v1/admin/english/grammar/lessons/{lessonId}/move` | `moveLesson` | `GrammarCommandService.moveLesson` | `GrammarOrderingService.moveLesson`→`GrammarRepository.moveLesson` | 是 | — |
| 课时跨章节迁移 | POST `/api/v1/admin/english/grammar/lessons/{lessonId}/reassign` | `reassignLesson` | `GrammarCommandService.reassignLesson` | `GrammarOrderingService.reassignLesson`→`GrammarRepository.reassignLesson` | 是 | — |

> 说明：`EnglishGrammarAdminController` 共 17 个路由方法（GET/PUT 根、publish、withdraw、curriculum、sections×4、lessons×9），`EnglishGrammarPublicController` 2 个，合计 19 个路由，已全覆盖。

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| G-01 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\controller\EnglishGrammarAdminController.java:93-103` | `if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_GRAMMAR_LESSON", lessonId)) { ... return ResponseEntity.accepted().body(review); }` | Controller 内做「是否超管 + 内容是否已发布」业务分支，并直接依赖 `account.review.service.ContentReviewService`、构造 `ResponseEntity`，违反「Controller 只做 HTTP 绑定」。`isSuperAdmin`/`actorId` 私有方法也在 Controller 中。 | 高 | 把送审编排下沉到 `GrammarCommandService.updateLesson`，Controller 只返回统一视图/状态码；review 依赖通过 Port 收口 |
| G-02 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\infrastructure\GrammarRepository.java:159-170` | `if (lessons != null && lessons > 0) { throw new ApiException(..., "GRAMMAR_SECTION_NOT_EMPTY", "Section is not empty", "Move or delete all lessons before deleting this section."); }` | 仓储层承担「章节非空不可删」业务规则；同类还有 `updateLesson` 的跨章节编辑禁止（行 208-213）、slug 冲突校验（行 350-364）、`buildCurriculum` 发布过滤（行 280-300）、`validateCover`（行 366-372）。 | 高 | 业务规则上移 `GrammarPublishPolicy`/`GrammarCommandService`，仓储只保留 CRUD 与查询 |
| G-03 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\application\GrammarCommandService.java:25-31,37` | `public GrammarCourseView updateCourse(...) { return repository.updateCourse(...); }`（无 `@Transactional`，仅 updateLesson/publishLesson/withdrawLesson/move/reassign 有注解） | 事务边界不统一：同一 Service 内部分方法声明事务、部分依赖 Repository 上的 `@Transactional`；`publishLesson` 为「策略校验 + 写库」两步却无 service 级事务，边界分散在两层。 | 中 | 写方法统一在 Service 声明 `@Transactional`，Repository 移除事务注解 |
| G-04 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\application\GrammarQueryService.java`（全类） | `public GrammarCourseView course() { return repository.course(); }` 等 8 个方法全为一行转发 | `GrammarQueryService` 与 `GrammarCommandService` 大量方法为纯转发空壳，未承载编排价值（未做参数校验/组合）。 | 中 | 与仓储合并或仅保留真正编排；避免「为分层而分层」 |
| G-05 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\application\GrammarQueryService.java`（类级） | 类上无 `@Transactional(readOnly = true)` | 只读服务缺只读事务注解，与 `ListeningQueryService`、`ReadingQueryService` 的 `readOnly = true` 约定不一致。 | 低 | 统一加 `@Transactional(readOnly = true)` |
| G-06 | 7 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\grammar\infrastructure\GrammarRepository.java:174-200` | `String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null)); assertSlugFree(slug, null); ... } catch (DuplicateKeyException ex) { throw slugConflict(); }` | slug 唯一性做「生成器预检 + assertSlugFree 再查 + 捕获 DuplicateKey」三重校验，重复且分布在 Service/Repository 两处。 | 低 | 保留 DB 唯一约束兜底，其余收敛为一处 |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| Java 文件总数 | 19 |
| Controller / application(Service) / infrastructure(Repository) / domain(Policy) 类数 | 2 / 5 / 1 / 1 |
| Controller 路由方法总数 | 19（Public 2 + Admin 17） |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0 |
| 跨模块依赖对方 Mapper 清单 | 无（但 Admin Controller 依赖 `account.review.ContentReviewService`，见 G-01） |
| 行数最多 5 类 | `GrammarRepository`(475)、`EnglishGrammarAdminController`(≈140)、`GrammarQueryService`(≈40)、`GrammarCommandService`(≈45)、`GrammarOrderingService`(≈60)（后三者为阅读估算，待确认） |
| public 方法最多 5 类 | `GrammarRepository`(25)、`EnglishGrammarAdminController`(17 路由)、`GrammarCommandService`(12)、`GrammarQueryService`(8)、`GrammarOrderingService`(3) |

### 4. 整体评价

grammar 模块分层骨架成立，`GrammarOrderingService` 与 `GrammarPublishPolicy` 是本次范围内较规范的范例（排序/发布规则独立于持久化）。主要缺陷是「职责下沉过度」：业务规则（非空校验、跨章节禁止、发布过滤）堆在 475 行的 `GrammarRepository`，而 application 层却是纯转发空壳；同时 `updateLesson` 的送审分支写在 Controller 并直连 account 模块。事务边界在 Service 与 Repository 间不统一，只读事务注解缺失。整体属「分层形式具备、职责实质错位」。

---

## 模块二：english/listening

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 听力首页 | GET `/api/v1/public/english/listening` | `ListeningPublicController.home` | `ListeningQueryService.home` | `ListeningRepository.home` | 是 | — |
| 听力列表 | GET `/api/v1/public/english/listening/items` | `list` | `ListeningQueryService.list` | `ListeningRepository.list`（`buildWhere`+`tagExists` 拼接） | 否：动态 SQL 在仓储 | L-05 |
| 听力详情 | GET `/api/v1/public/english/listening/items/{slug}` | `detail` | `ListeningQueryService.publicDetail` | `ListeningRepository.publicBySlug`/`requirePublished` | 否：发布判定在仓储 | L-02 |
| 分段列表 | GET `/api/v1/public/english/listening/items/{slug}/segments` | `segments` | `ListeningSegmentService.publicSegments` | `ListeningSegmentRepository.publicBySlug` | 否：空壳 service | L-06 |
| 题目列表 | GET `/api/v1/public/english/listening/items/{slug}/exercises` | `exercises` | `ListeningExerciseApplicationService.publicList` | `ListeningExerciseRepository.publicList` | 否：空壳 service | L-06 |
| 提交判分 | POST `/api/v1/public/english/listening/items/{slug}/check` | `check` | `ListeningExerciseApplicationService.check` | `ListeningExerciseRepository.check`（**在仓储内判分**） | 否：判分在仓储 | L-03 |
| 发音规则列表 | GET `/api/v1/public/english/listening/pronunciation` | `rules` | `PronunciationRuleQueryService.list` | `PronunciationRuleRepository.list` | 部分 | L-08 |
| 发音规则详情 | GET `/api/v1/public/english/listening/pronunciation/{slug}` | `rule` | `PronunciationRuleQueryService.publicBySlug` | `PronunciationRuleRepository.ruleBySlug` | 部分 | L-08 |
| 后台听力列表 | GET `/api/v1/admin/english/listening/items` | `ListeningAdminController.list` | `ListeningQueryService.adminList` | `ListeningRepository.adminList` | 是 | — |
| 新建听力 | POST `/api/v1/admin/english/listening/items` | `create` | `ListeningCommandService.create` | `ListeningRepository.create` | 是（有事务+事件） | — |
| 听力详情（管理） | GET `.../items/{id}` | `get` | `ListeningQueryService.get` | `ListeningRepository.get` | 是 | — |
| 听力更新（含送审分支） | PUT `.../items/{id}` | `update` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `ListeningCommandService.update` | `ListeningRepository.update` | 否：Controller 越界 | L-01 |
| 删除听力 | DELETE `.../items/{id}` | `delete` | `ListeningCommandService.delete` | `ListeningRepository.delete`（PUBLISHED 禁删在仓储） | 否：规则在仓储 | L-02 |
| 发布听力 | POST `.../items/{id}/publish` | `publish` | `ListeningCommandService.publish` | `ListeningRepository.publish`（`publishPolicy.problems` 前置校验在仓储） | 否：规则在仓储 | L-02 |
| 撤回听力 | POST `.../items/{id}/withdraw` | `withdraw` | `ListeningCommandService.withdraw` | `ListeningRepository.withdraw` | 是 | — |
| 分段列表（管理） | GET `.../items/{id}/segments` | `segments` | `ListeningSegmentService.adminList` | `ListeningSegmentRepository.adminList` | 否：空壳 service | L-06 |
| 新建分段 | POST `.../items/{id}/segments` | `createSegment` | `ListeningSegmentService.create` | `ListeningSegmentRepository.create` | 是 | — |
| 更新分段 | PUT `.../items/{id}/segments/{segmentId}` | `updateSegment` | `ListeningSegmentService.update` | `ListeningSegmentRepository.update` | 是 | — |
| 删除分段 | DELETE `.../items/{id}/segments/{segmentId}` | `deleteSegment` | `ListeningSegmentService.delete` | `ListeningSegmentRepository.delete` | 是 | — |
| 分段排序 | POST `.../items/{id}/segments/{segmentId}/move` | `moveSegment` | `ListeningSegmentService.move` | `ListeningSegmentRepository.move` | 否：默认值在 Controller | L-10 |
| 批量替换分段 | PUT `.../items/{id}/segments/batch` | `batchSegments` | `ListeningSegmentService.batchReplace` | `ListeningSegmentRepository.batchReplace`（时间轴校验委托 Policy，好） | 是 | — |
| 题目列表（管理） | GET `.../items/{id}/exercises` | `exercises` | `ListeningExerciseApplicationService.adminList` | `ListeningExerciseRepository.adminList` | 否：空壳 service | L-06 |
| 新建题目 | POST `.../items/{id}/exercises` | `createExercise` | `ListeningExerciseApplicationService.create` | `ListeningExerciseRepository.create` | 是 | — |
| 更新题目 | PUT `.../items/{id}/exercises/{exerciseId}` | `updateExercise` | `ListeningExerciseApplicationService.update` | `ListeningExerciseRepository.update` | 是 | — |
| 删除题目 | DELETE `.../items/{id}/exercises/{exerciseId}` | `deleteExercise` | `ListeningExerciseApplicationService.delete` | `ListeningExerciseRepository.delete` | 是 | — |
| 题目排序 | POST `.../items/{id}/exercises/{exerciseId}/move` | `moveExercise` | `ListeningExerciseApplicationService.move` | `ListeningExerciseRepository.move`（`joinIds` 拼接） | 否：默认值在 Controller + SQL 拼接 | L-05,L-10 |
| 阅读配对列表 | GET `.../items/{id}/reading-pairs` | `readingPairs` | `ListeningRelationService.list` | `ListeningRelationRepository.list` | 否：跨模块表访问 | L-04 |
| 新增阅读配对 | POST `.../items/{id}/reading-pairs` | `addReadingPair` | `ListeningRelationService.add` | `ListeningRelationRepository.add`（校验 + 直查 reading 表） | 否：规则在仓储 + 跨模块 | L-04 |
| 删除阅读配对 | DELETE `.../items/{id}/reading-pairs/{readingId}` | `removeReadingPair` | `ListeningRelationService.remove` | `ListeningRelationRepository.remove` | 部分 | — |
| 发音规则列表（管理） | GET `.../pronunciation` | `rules` | `PronunciationRuleQueryService.adminList` | `PronunciationRuleRepository.adminList` | 是 | — |
| 新建发音规则 | POST `.../pronunciation` | `createRule` | `PronunciationRuleCommandService.create` | `PronunciationRuleRepository.create` | 是 | — |
| 发音规则详情（管理） | GET `.../pronunciation/{id}` | `rule` | `PronunciationRuleQueryService.require` | `PronunciationRuleRepository.ruleById`（`publishedOnly` 被忽略） | 否：死参数 | L-08 |
| 发音规则更新（含送审分支） | PUT `.../pronunciation/{id}` | `updateRule` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `PronunciationRuleCommandService.update` | `PronunciationRuleRepository.update` | 否：Controller 越界 | L-01 |
| 删除发音规则 | DELETE `.../pronunciation/{id}` | `deleteRule` | `PronunciationRuleCommandService.delete` | `PronunciationRuleRepository.deleteRule`（禁删已发布在仓储） | 否：规则在仓储 | L-02 |
| 发布发音规则 | POST `.../pronunciation/{id}/publish` | `publishRule` | `PronunciationRuleCommandService.publish` | `PronunciationRuleRepository.publish` | 是 | — |
| 撤回发音规则 | POST `.../pronunciation/{id}/withdraw` | `withdrawRule` | `PronunciationRuleCommandService.withdraw` | `PronunciationRuleRepository.withdrawRule`（禁撤草稿在仓储） | 否：规则在仓储 | L-02 |
| 发音规则排序 | POST `.../pronunciation/{id}/move` | `moveRule` | `PronunciationRuleCommandService.move` | `PronunciationRuleRepository.move` | 否：默认值在 Controller | L-10 |

> `ListeningAdminController` 共 30 个路由方法（items 8 + segments 6 + exercises 5 + reading-pairs 3 + pronunciation 8），`ListeningPublicController` 8 个，合计 38 个路由，已全覆盖。

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| L-01 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\controller\ListeningAdminController.java:102-110` | `if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_LISTENING_ITEM", id)) { ContentReviewView review = reviewService.submitEnglishUpdate(...); return ResponseEntity.accepted().body(review); }` | Controller 内做送审业务分支并直连 `account.review` 模块；`updateRule`（行 233-242）同款问题。 | 高 | 送审编排下沉 application 层，经 Port 访问 review |
| L-02 | 3,8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\ListeningRepository.java:365-374` | `private void requireEnabledTag(Long termId, String dimension) { ... if (found.isEmpty()) throw new ApiException(..., "ENGLISH_TAXONOMY_NOT_FOUND", ...); if (!dimension.equals(found.get(0))) throw ... }` | 649 行的上帝类：item CRUD + catalogDescriptors + list/home/adminStats + tags + publish/withdraw/delete + 发布前置校验 + `validateCefr/validateLevel/validateAudio/validateCover`（行 448-471）+ 标签维度校验全塞在仓储；PUBLISHED 禁删（行 299-310）等业务规则也在仓储。 | 高 | 拆分 Repository（item / 标签 / 统计），业务规则上移 domain/service |
| L-03 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\ListeningExerciseRepository.java:148-169` | `public CheckResultView check(...) { ... // 在仓储内比对答案、累计分数、判定通过 }` | 判分/判重业务逻辑写在 Repository，与 `EnglishExerciseSafety` 共享判分职责重叠，且与 reading 把判分放 application 层的做法不一致。 | 高 | 判分上移 application service，仓储只提供题目数据 |
| L-04 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\ListeningRelationRepository.java:63-77` | `JOIN english_reading_article ar ON ar.id=rp.reading_article_id ... Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article WHERE id=?", Integer.class, readingId);` | listening 仓储直接查询 reading 模块的 `english_reading_article` 表，绕过 Port，违反「模块间不直接依赖对方表」。 | 高 | 通过 `ReadingContentPort`/Facade 校验阅读文章存在性 |
| L-05 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\ListeningRepository.java:538-565` | `String in = String.join(",", ids.stream().map(String::valueOf).toList()); ... "SELECT " + column + ", COUNT(*) FROM " + table + " WHERE " + column + " IN (" + in + ") GROUP BY " + column` | `loadTags`/`countBy` 手写 IN 列表与动态表名/列名拼接；`tagExists`（行 584-588）还把维度名拼进 SQL。虽值可控，但属易错模式。 | 中 | 用参数化占位符（`Collections.nCopies`）与白名单枚举，收敛到 Mapper |
| L-06 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\application\`（`ListeningSegmentService`、`ListeningRelationService`、`PronunciationRuleQueryService`、`PronunciationRuleCommandService`、`ListeningExerciseApplicationService`、`ListeningPairQueryService`） | 各方法均形如 `public List<X> list(Long id) { return repository.list(id); }` | 6 个 application 类为纯转发空壳，仅承担事务注解，无编排/校验，分层收益为负。 | 中 | 合并同域 service，或让空壳承载真实编排 |
| L-07 | 6,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\ListeningRepository.java:26,29` | `import com.starrainnotes.english.listening.mapper.ListeningItemMapper;` / `import com.starrainnotes.english.listening.infrastructure.ListeningRelationRepository;` | 仓储依赖同模块其它仓储（repository→repository），并直接注入 `shared.exercise.mapper.EnglishExerciseMapper`（跨模块 mapper，行 121、309）。 | 中 | 通过聚合服务编排，跨模块走 Port |
| L-08 | 7,8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\infrastructure\PronunciationRuleRepository.java:154-156` | `public PronunciationRuleView ruleById(Long id, boolean publishedOnly) { return requireRule(id); }` | `publishedOnly` 参数被完全忽略（死参数），调用方无法按发布态过滤，易误导；对比同文件 `ruleBySlug`（行 158-166）正确使用该参数。 | 中 | 删除死参数或实现其语义 |
| L-09 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\domain\SegmentTimelinePolicy.java:15-33` | `throw new ApiException(..., "时间轴区间无效", ...)`（中文 detail） | listening 的 `SegmentTimelinePolicy`/`ListeningPublishPolicy` 抛中文错误信息，而 grammar/reading/writing 用英文，错误信息语言不统一。 | 低 | 统一错误文案语言 |
| L-10 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\listening\controller\ListeningAdminController.java:155,193,262` | `service.move(id, segmentId, request.targetIndex() == null ? 0 : request.targetIndex())` | 把「null→0」的默认值语义写在 Controller，业务默认值应属 service/domain。 | 低 | 默认值下沉 service |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| Java 文件总数 | 38 |
| Controller / application(Service) / infrastructure(Repository) / domain(Policy) 类数 | 2 / 8 / 6 / 2 |
| Controller 路由方法总数 | 38（Public 8 + Admin 30） |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0 |
| 跨模块依赖对方 Mapper 清单 | `ListeningRepository` → `shared.exercise.mapper.EnglishExerciseMapper`；`ListeningRelationRepository` 直接读 `english_reading_article` 表 |
| 行数最多 5 类 | `ListeningRepository`(649)、`ListeningAdminController`(275)、`ListeningExerciseRepository`(272)、`PronunciationRuleRepository`(217)、`ListeningSegmentRepository`(148) |
| public 方法最多 5 类 | `ListeningAdminController`(30 路由)、`ListeningRepository`(≈20)、`ListeningExerciseRepository`(≈11)、`PronunciationRuleRepository`(≈9)、`ListeningSegmentRepository`(≈8)（后四者阅读估算，待确认） |

### 4. 整体评价

listening 是本次范围内问题最集中的模块。`ListeningRepository` 已演化为 649 行上帝类，把标签校验、发布前置、字段校验、统计聚合、动态 SQL 全部揽入，同时直接依赖 `EnglishExerciseMapper` 与同模块仓储；判分逻辑落在 `ListeningExerciseRepository`。此外 Controller 送审分支越界（L-01）与 listening 直接查询 reading 表（L-04）两处明确破坏边界约定。相对而言，`ListeningSegmentService.batchReplace` 委托 `SegmentTimelinePolicy`、`ListeningCommandService` 带 `@Transactional`+`@EnglishContentChange` 是较规范的部分。6 个 application 空壳使分层形式化。

---

## 模块三：english/reading

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 阅读首页 | GET `/api/v1/public/english/reading` | `ReadingPublicController.home` | `ReadingQueryService.home` | `ReadingRepository.home` | 是 | — |
| 文章列表 | GET `/api/v1/public/english/reading/articles` | `list` | `ReadingQueryService.list` | `ReadingRepository.list` | 是 | — |
| 文章详情 | GET `/api/v1/public/english/reading/articles/{slug}` | `detail` | `ReadingQueryService.publicDetail` | `ReadingRepository.publicBySlug`（`requirePublished` 在仓储） | 否：发布判定在仓储 | R-03 |
| 题目列表 | GET `/api/v1/public/english/reading/articles/{slug}/exercises` | `exercises` | Controller 先 `ReadingQueryService.publicGet(slug)` 再 `ReadingExerciseApplicationService.publicList` | `ReadingRepository.publicGet` + `ReadingExerciseRepository.publicList` | 否：Controller 编排两步 | R-08 |
| 提交判分 | POST `/api/v1/public/english/reading/articles/{slug}/check` | `check` | Controller 先取 id，再 `ReadingExerciseApplicationService.check`（判分在 application） | `ReadingExerciseRepository.check` | 部分 | R-05,R-08 |
| 后台文章列表 | GET `/api/v1/admin/english/reading/articles` | `ReadingAdminController.list` | `ReadingQueryService.adminList` | `ReadingRepository.adminList` | 是 | — |
| 新建文章 | POST `/api/v1/admin/english/reading/articles` | `create` | `ReadingCommandService.create` | `ReadingRepository.create` + `ReadingRelationRepository` | 是（业务在 service，好范例） | — |
| 文章详情（管理） | GET `.../articles/{id}` | `get` | `ReadingQueryService.get` | `ReadingRepository.get` | 是 | — |
| 文章更新（含送审分支） | PUT `.../articles/{id}` | `update` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `ReadingCommandService.update` | `ReadingRepository.update` | 否：Controller 越界 | R-01 |
| 删除文章 | DELETE `.../articles/{id}` | `delete` | `ReadingCommandService.delete` | `ReadingRepository.delete`（**直接 DELETE english_exercise**） | 否：跨模块删表 | R-03 |
| 发布文章 | POST `.../articles/{id}/publish` | `publish` | `ReadingCommandService.publish` | `ReadingRepository.publish` + `ReadingPublishPolicy.violations` | 部分 | R-02 |
| 撤回文章 | POST `.../articles/{id}/withdraw` | `withdraw` | `ReadingCommandService.withdraw` | `ReadingRepository.withdraw` | 是 | — |
| 题目列表（管理） | GET `.../articles/{id}/exercises` | `exercises` | `ReadingExerciseApplicationService.adminList` | `ReadingExerciseRepository.adminList` | 是 | — |
| 新建题目 | POST `.../articles/{id}/exercises` | `createExercise` | `ReadingExerciseApplicationService.create` | `ReadingExerciseRepository.create` | 是 | — |
| 更新题目 | PUT `.../articles/{id}/exercises/{exerciseId}` | `updateExercise` | `ReadingExerciseApplicationService.update` | `ReadingExerciseRepository.update` | 是 | — |
| 题目排序 | POST `.../articles/{id}/exercises/{exerciseId}/move` | `moveExercise` | `ReadingExerciseApplicationService.move` | `ReadingExerciseRepository.move`（IN 拼接） | 部分 | R-03 |
| 删除题目 | DELETE `.../articles/{id}/exercises/{exerciseId}` | `deleteExercise` | `ReadingExerciseApplicationService.delete` | `ReadingExerciseRepository.delete` | 是 | — |

> `ReadingAdminController` 12 个路由 + `ReadingPublicController` 5 个，合计 17 个路由，已全覆盖。

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| R-01 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\controller\ReadingAdminController.java:77-86` | `if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_READING_ARTICLE", id)) { ... return ResponseEntity.accepted().body(review); }` | 同 grammar/listening，Controller 做送审业务分支并直连 account 模块。 | 高 | 编排下沉 application，经 Port 访问 review |
| R-02 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\domain\ReadingPublishPolicy.java:4,13-15` | `import com.starrainnotes.english.reading.infrastructure.ReadingRelationRepository;` … `private final ReadingRelationRepository relations;` | domain 策略类反向依赖 infrastructure 仓储（读标签维度），违反依赖方向（domain 应不依赖基础设施）。 | 高 | 把「标签维度是否齐备」查询结果作为入参传入 policy，或抽 Port |
| R-03 | 3,6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\infrastructure\ReadingRepository.java:380-385` | `mapper.deleteById(id); exerciseIds.forEach(exerciseId -> jdbc.update("DELETE FROM english_exercise WHERE id=?", exerciseId));` | 仓储删除文章时直接删除共享的 `english_exercise` 表；同类还有 `loadArticleIdsWithExercises` IN 拼接（行 303-315）、`requirePublished` 业务判定（行 398-404）。 | 高 | 题目删除通过 shared.exercise 的 Port/Mapper 收口 |
| R-04 | 3,6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\infrastructure\ReadingRelationRepository.java:43,76-83` | `jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE id=?", ...)` / `SELECT gl.id, gl.title, gl.slug FROM english_reading_article_grammar_lesson agl JOIN english_grammar_lesson gl ON gl.id=agl.lesson_id` | reading 仓储直接 join/查询 grammar 模块表，且 `hasEnabledDimension`/`enabledTagDimensions`/`grammarLessonExists` 业务校验写在仓储。 | 高 | 经 grammar Port 校验课时存在性；维度校验上移 service |
| R-05 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\application\ReadingExerciseApplicationService.java:103-123` | `// 在 application service 内比对答案、计算得分` | reading 判分在 application 层，而 listening/writing 在 repository 层，同一能力三种位置，分层不一致。 | 中 | 统一判分归属（建议 application + 共享 `EnglishExerciseSafety`） |
| R-06 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\domain\ReadingPublishPolicy.java:19-36` | `if (blank(article.getTitle())) problems.add("标题不能为空"); ...` | 发布违规文案为中文，与 grammar/writing 的英文错误信息不一致（正面：规则已抽到 policy）。 | 低 | 统一文案语言 |
| R-07 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\application\ReadingQueryService.java`（全类） | 类上 `@Transactional(readOnly = true)`，方法全为一行转发 | 只读服务为纯转发空壳（相比 grammar/writing 至少补了 readOnly 注解）。 | 低 | 视情况合并 |
| R-08 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\reading\controller\ReadingPublicController.java:60-67` | `public List<ExercisePublicView> exercises(@PathVariable String slug) { return exerciseService.publicList(articleService.publicGet(slug).id()); }` | Controller 内串联「按 slug 取文章 → 用 id 取题目」两步用例编排，编排应属 application。 | 中 | 提供 `ReadingExerciseApplicationService.publicListBySlug(slug)` |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| Java 文件总数 | 25 |
| Controller / application(Service) / infrastructure(Repository) / domain(Policy) 类数 | 2 / 4 / 4 / 1 |
| Controller 路由方法总数 | 17（Public 5 + Admin 12） |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0 |
| 跨模块依赖对方 Mapper 清单 | `ReadingRelationRepository` 直接读 `english_grammar_lesson`；`ReadingRepository` 直接删 `english_exercise` |
| 行数最多 5 类 | `ReadingRepository`(405)、`ReadingArticle`(entity,212)、`ReadingExerciseRepository`(185)、`ReadingCommandService`(≈140)、`ReadingExerciseApplicationService`(≈125)（后三者估算，待确认） |
| public 方法最多 5 类 | `ReadingArticle`(entity,42)、`ReadingRepository`(≈18)、`ReadingExerciseRepository`(≈9)、`ReadingCommandService`(≈8)、`ReadingRelationService`(≈4) |

### 4. 整体评价

reading 是五个技能模块中分层最接近约定的：`ReadingCommandService` 把 stats 分析、slug/cefr/level/media 校验、字段应用都留在 application 并声明事务，`ReadingExerciseApplicationService` 把判分放在 application，`ReadingTextStatistics` 独立成工具类。主要问题是依赖方向被打破——domain 的 `ReadingPublishPolicy` 依赖 infrastructure 仓储（R-02），仓储直接 join grammar 表、直接删 `english_exercise`（R-03/R-04）；同时 Controller 承担两步编排（R-08）。判分位置与 listening/writing 不一致（R-05）。

---

## 模块四：english/writing

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 写作资源列表 | GET `/api/v1/public/english/writing/resources` | `WritingPublicController.resources` | `WritingResourceQueryService.publicList` | `WritingResourceRepository.publicList` | 部分 | W-03 |
| 写作资源详情 | GET `/api/v1/public/english/writing/resources/{slug}` | `resource` | `WritingResourceQueryService.publicGet` | `WritingResourceRepository.publicGet` | 部分 | W-03 |
| 写作题目列表 | GET `/api/v1/public/english/writing/prompts` | `prompts` | `WritingPromptQueryService.publicList` | `WritingPromptRepository.publicList` | 部分 | W-03 |
| 写作题目详情 | GET `/api/v1/public/english/writing/prompts/{slug}` | `prompt` | `WritingPromptQueryService.publicGet` | `WritingPromptRepository.publicGet` | 部分 | W-03 |
| 题目题目列表 | GET `.../prompts/{slug}/exercises` | `exercises` | `WritingExerciseApplicationService.publicList` | `WritingExerciseRepository.publicList` | 是 | — |
| 提交判分 | POST `.../prompts/{slug}/check` | `check` | `WritingExerciseApplicationService.check` | `WritingExerciseRepository.check`（**在仓储判分**） | 否：判分在仓储 | W-08 |
| 后台资源列表 | GET `/api/v1/admin/english/writing/resources` | `WritingAdminController.resources` | `WritingResourceQueryService.adminList` | `WritingResourceRepository.adminList` | 部分 | W-03 |
| 新建资源 | POST `/api/v1/admin/english/writing/resources` | `createResource` | `WritingResourceCommandService.create` | `WritingResourceRepository.create` | 否：无事务注解 | W-04 |
| 资源详情（管理） | GET `.../resources/{id}` | `resource` | `WritingResourceQueryService.get` | `WritingResourceRepository.get` | 部分 | W-03 |
| 资源更新（含送审分支） | PUT `.../resources/{id}` | `updateResource` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `WritingResourceCommandService.update` | `WritingResourceRepository.update` | 否：Controller 越界 | W-01 |
| 删除资源 | DELETE `.../resources/{id}` | `deleteResource` | `WritingResourceCommandService.delete` | `WritingResourceRepository.delete`（in-use 校验在仓储） | 否：规则在仓储 + 无事务 | W-04,W-06 |
| 发布资源 | POST `.../resources/{id}/publish` | `publishResource` | `WritingResourceCommandService.publish` | `WritingResourceRepository.publish` + `WritingResourcePolicy` | 是 | — |
| 撤回资源 | POST `.../resources/{id}/withdraw` | `withdrawResource` | `WritingResourceCommandService.withdraw` | `WritingResourceRepository.withdraw` | 是 | — |
| 资源排序 | POST `.../resources/{id}/move` | `moveResource` | `WritingResourceCommandService.move` | `WritingResourceRepository.move`（**DTO 无校验**） | 否：无事务 + 无校验 | W-04,W-05 |
| 后台题目列表 | GET `/api/v1/admin/english/writing/prompts` | `prompts` | `WritingPromptQueryService.adminList` | `WritingPromptRepository.adminList` | 部分 | W-03 |
| 新建题目 | POST `/api/v1/admin/english/writing/prompts` | `createPrompt` | `WritingPromptCommandService.create` | `WritingPromptRepository.create` | 否：无事务注解 | W-04 |
| 题目详情（管理） | GET `.../prompts/{id}` | `prompt` | `WritingPromptQueryService.get` | `WritingPromptRepository.get` | 部分 | W-03 |
| 题目更新（含送审分支） | PUT `.../prompts/{id}` | `updatePrompt` | Controller 分支→`ContentReviewService.submitEnglishUpdate` 或 `WritingPromptCommandService.update` | `WritingPromptRepository.update` | 否：Controller 越界 | W-01 |
| 删除题目 | DELETE `.../prompts/{id}` | `deletePrompt` | `WritingPromptCommandService.delete` | `WritingPromptRepository.delete` | 否：无事务注解 | W-04 |
| 发布题目 | POST `.../prompts/{id}/publish` | `publishPrompt` | `WritingPromptCommandService.publish` | `WritingPromptRepository.publish` + `WritingPromptPolicy` | 是 | — |
| 撤回题目 | POST `.../prompts/{id}/withdraw` | `withdrawPrompt` | `WritingPromptCommandService.withdraw` | `WritingPromptRepository.withdraw` | 是 | — |
| 题目排序 | POST `.../prompts/{id}/move` | `movePrompt` | `WritingPromptCommandService.move` | `WritingPromptRepository.move`（**DTO 无校验**） | 否：无事务 + 无校验 | W-04,W-05 |
| 题目题目列表（管理） | GET `.../prompts/{id}/exercises` | `exercises` | `WritingExerciseApplicationService.adminList` | `WritingExerciseRepository.adminList` | 是 | — |
| 新建题目题目 | POST `.../prompts/{id}/exercises` | `createExercise` | `WritingExerciseApplicationService.create` | `WritingExerciseRepository.create` | 是 | — |
| 更新题目题目 | PUT `.../prompts/{id}/exercises/{exerciseId}` | `updateExercise` | `WritingExerciseApplicationService.update` | `WritingExerciseRepository.update` | 是 | — |
| 题目题目排序 | POST `.../prompts/{id}/exercises/{exerciseId}/move` | `moveExercise` | `WritingExerciseApplicationService.move` | `WritingExerciseRepository.move` | 是 | — |
| 删除题目题目 | DELETE `.../prompts/{id}/exercises/{exerciseId}` | `deleteExercise` | `WritingExerciseApplicationService.delete` | `WritingExerciseRepository.delete` | 是 | — |

> `WritingAdminController` 21 个路由 + `WritingPublicController` 6 个，合计 27 个路由，已全覆盖。

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| W-01 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\controller\WritingAdminController.java:84-94` | `if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_WRITING_RESOURCE", id)) { ... return ResponseEntity.accepted().body(review); }` | Controller 送审业务分支越界（`updatePrompt` 行 140-150 同款）。 | 高 | 编排下沉 application，经 Port 访问 review |
| W-02 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\controller\WritingPublicController.java:10-19` | `@RestController @RequestMapping("/api/v1/public/english/writing") public class WritingPublicController { ... @GetMapping("/resources/{slug}") public WritingResourceView resource(@PathVariable String slug){return resources.publicGet(slug);} ... }` | 整个 Controller 被压缩成单行密集写法（类声明与注解同行、方法体同行、全限定名内联），可读性差，与其它模块风格严重不一致。 | 中 | 恢复常规格式化风格 |
| W-03 | 4,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\application\WritingPromptQueryService.java`、`WritingResourceQueryService.java`（类级） | 类上无 `@Transactional(readOnly = true)` | 只读服务缺只读事务注解，与 listening/reading 不一致。 | 中 | 补 `@Transactional(readOnly = true)` |
| W-04 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\application\WritingPromptCommandService.java`、`WritingResourceCommandService.java` | `public WritingResourceView create(WritingResourceRequest request) { return repository.create(request); }`（create/delete/move 无 `@Transactional`） | 写方法未声明事务，依赖 Repository 上的注解；同 Service 内其余方法却有事务，边界不一致。 | 中 | 写方法统一声明 `@Transactional` |
| W-05 | 7 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\dto\WritingMoveRequest.java:1-2` | `public record WritingMoveRequest(Integer targetIndex) {}` | 排序请求 DTO 无任何校验；对比 `GrammarMoveRequest`（`@NotNull @Min(0)`）缺失，非法/缺省值直达 service。 | 中 | 补 `@NotNull @Min(0)` 并统一 move 请求契约 |
| W-06 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\infrastructure\WritingPromptRepository.java:77` | `// tags 允许 TOPIC/GENRE/FUNCTION/ABILITY，含维度校验与 publishedResource/cefr/cover/hasTag 判定` | 业务校验（维度白名单、发布资源判定、hasTag、删除 in-use）写在 102 行的 Repository；`WritingResourceRepository.delete`（行 47）同款。 | 中 | 校验上移 policy/service |
| W-07 | 7 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\infrastructure\WritingResourceRepository.java:27-28` | `// 重复定义 KINDS / LEVELS 常量（与 WritingResourcePolicy 同名常量重复）` | 常量在 policy 与 repository 各定义一份，存在漂移风险。 | 低 | 单一来源（引用 policy 常量） |
| W-08 | 3,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\infrastructure\WritingExerciseRepository.java:42` | `public CheckResultView check(...)`（在 repository 内判分） | 判分在仓储（与 listening 相同、与 reading 不同）；且整个文件 55 行极端压缩单行写法。 | 中 | 判分上移 application；恢复可读格式 |
| W-09 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\writing\application\`（`WritingPromptQueryService`、`WritingResourceQueryService`、`WritingExerciseApplicationService`） | 方法均一行转发 | 三个 application 类为纯转发空壳。 | 低 | 视情况合并 |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| Java 文件总数 | 28 |
| Controller / application(Service) / infrastructure(Repository) / domain(Policy) 类数 | 2 / 5 / 4 / 2 |
| Controller 路由方法总数 | 27（Public 6 + Admin 21） |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0 |
| 跨模块依赖对方 Mapper 清单 | 无（判分依赖 `shared.exercise` 相关类；Controller 依赖 account.review，见 W-01） |
| 行数最多 5 类 | `WritingAdminController`(215)、`WritingPromptRepository`(102)、`WritingResourcePolicy`(≈75)、`WritingResourceRepository`(73)、`WritingExerciseRepository`(55)（后三者估算，待确认） |
| public 方法最多 5 类 | `WritingAdminController`(21 路由)、`WritingPromptRepository`(≈12)、`WritingResourceRepository`(≈11)、`WritingResourcePolicy`(≈5)、`WritingPromptCommandService`(≈5) |

### 4. 整体评价

writing 模块的问题集中在「风格与一致性」：`WritingPublicController` 与三个 Repository 被压成单行密集代码，可读性明显低于其它模块；查询/写服务缺事务注解；`WritingMoveRequest` 无校验。分层上，`WritingPromptPolicy`/`WritingResourcePolicy` 承担了发布与请求校验（较好），但部分校验仍留在 Repository，且 policy 与 repository 常量重复。Controller 送审分支越界与 grammar/listening/reading 同源，属系统性模式问题。判分落在 Repository 与 listening 一致、与 reading 不一致。

---

## 模块五：english/learning

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 学习分析看板 | GET `/api/v1/admin/english/analytics` | `EnglishLearningAdminController.analytics` | `EnglishLearningAnalyticsQueryService.overview` | `LearningAnalyticsRepository.overview/trend/modules` | 是（校验/时区/rate 在 service） | — |
| 学习记录查询（废弃） | GET `/api/v1/public/english/learning/records/{contentType}/{contentId}` | `EnglishLearningPublicController.get` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 学习记录批量（废弃） | GET `/api/v1/public/english/learning/records/batch` | `batch` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 学习记录保存（废弃） | PUT `/api/v1/public/english/learning/records/{contentType}/{contentId}` | `save` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 学习摘要（废弃） | GET `/api/v1/public/english/learning/summary` | `summary` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 学习洞察（废弃） | GET `/api/v1/public/english/learning/insights` | `insights` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 写作提交查询（废弃） | GET `/api/v1/public/english/learning/writing-submissions/{promptId}` | `submission` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |
| 写作提交保存（废弃） | PUT `/api/v1/public/english/learning/writing-submissions/{promptId}` | `saveSubmission` | 无（`throw gone()`） | 无 | 否：空壳 | A-02 |

> 说明：`EnglishLearningFacade`（`DefaultEnglishLearningFacade`）对外提供 `recordQueries/recordCommands/insights/submissions/migration` 五组能力，被其它模块经 api 调用，无独立 HTTP 路由；其真实编排位于 `LearningRecordQueryService`、`LearningRecordCommandService`、`LearningInsightQueryService`、`WritingSubmissionService`、`LearningProgressMigrationService`、`RecommendationQueryService`。

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| A-01 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\application\RecommendationQueryService.java:7,34,39` | `import com.starrainnotes.english.shared.bundle.infrastructure.LearningBundleItemRepository;` … `private final LearningBundleItemRepository bundles;` | application 服务直接依赖 shared.bundle 模块的 **Repository**（而非 Service/Facade），跨模块依赖方向违规，且与「推荐读取不得反向依赖具体实现」的约定相悖。 | 高 | 经 `LearningBundleItemService`/Port 获取学习路径成员 |
| A-02 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\controller\EnglishLearningPublicController.java:33-72` | `public Object get(@RequestHeader(value = "X-Learner-Key", required = false) String key, ...) { throw gone(); }` | 7 个公开路由全部 `throw gone()`（410），方法参数（key、contentType、contentId 等）完全未使用，属已废弃空壳；路由仍注册。 | 中 | 删除已废弃路由或明确标注 @Deprecated 并移除无用参数 |
| A-03 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\infrastructure\WritingSubmissionRepository.java:44-57` | `// save(...) 内计算 word_count 后写入` | 写作提交的 `word_count` 计算（业务规则）写在 Repository，边界模糊（同类：`requirePublishedPrompt` 发布判定）。 | 中 | 字数统计上移 service |
| A-04 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\application\RecommendationQueryService.java`（全类，218 行） | `// route() 内硬编码前端路径（如 /english/reading/{slug}）` | 218 行的推荐编排类中硬编码前端路由，后端耦合前端契约。 | 中 | 前端路径改由前端映射，后端只返回 type+slug |
| A-05 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\application\LearningProgressMigrationService.java:4,15` | `import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyCommandService;` | 跨模块依赖 vocabulary 的 **application service**（属允许范围，比依赖仓储更规范），此处为正面参照。 | 低 | 保持 |
| A-06 | 8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\learning\application\DefaultEnglishLearningFacade.java`（全类） | 5 个方法均一行委托对应 service | Facade 纯委托属设计本意（收口），但无任何聚合/裁剪逻辑，价值有限。 | 低 | 保持，或合并到 facade 内直接编排 |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| Java 文件总数 | 32 |
| Controller / application(Service) / infrastructure(Repository) / domain 类数 | 2 / 10 / 6 / 2（domain 含 `RecommendationEngine`） |
| Controller 路由方法总数 | 8（Admin 1 + Public 7，Public 全部已废弃） |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0 |
| 跨模块依赖对方 Mapper 清单 | 无直接 Mapper；`RecommendationQueryService` 依赖 `shared.bundle.infrastructure.LearningBundleItemRepository`（跨模块仓储，A-01） |
| 行数最多 5 类 | `RecommendationQueryService`(218)、`LearningAnalyticsRepository`(≈150)、`LearningInsightRepository`(≈140)、`LearningRecordRepository`(≈120)、`LearnerProfileRepository`(≈90)（后四者估算，待确认） |
| public 方法最多 5 类 | `RecommendationQueryService`(≈8)、`LearningInsightRepository`(≈7)、`LearningAnalyticsRepository`(≈6)、`LearningRecordQueryService`(≈6)、`EnglishLearningPublicController`(7 路由) |

### 4. 整体评价

learning 模块整体分层较健康：分析查询（`EnglishLearningAnalyticsQueryService`）把 days/type 校验、时区窗口、rate 计算留在 service 并带只读事务；`RecommendationEngine` 是纯 domain 逻辑；迁移服务跨模块依赖的是 vocabulary 的 application service 而非仓储，方向正确。主要问题：推荐编排类直接依赖 shared.bundle 的 Repository（A-01，依赖方向违规）并在其中硬编码前端路径（A-04）；7 个公开路由全部为 `throw gone()` 空壳（A-02）；写作提交的字数计算落在仓储（A-03）。

---

## 模块六：english 根目录 / shared / api 其他

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| English 概览（公开） | GET `/api/v1/public/english` | `EnglishPublicController.get` | `EnglishService.get` | `EnglishOverviewMapper.selectById` | 是 | — |
| English 概览（管理） | GET `/api/v1/admin/english` | `EnglishAdminController.get` | `EnglishService.get` | `EnglishOverviewMapper.selectById` | 是 | — |
| English 概览更新 | PUT `/api/v1/admin/english` | `EnglishAdminController.update` | `EnglishService.update` | `EnglishOverviewMapper.updateById` | 否：写操作无事务 | E-01 |
| 元数据（taxonomy+cefr+题型） | GET `/api/v1/public/english/meta` | `EnglishMetaController.meta` | 无（Controller 内组装 3 个 service 结果） | `TaxonomyQueryService`、`CefrService` 等 | 否：Controller 组装 View | E-03 |
| 分类树/扁平列表 | GET `/api/v1/admin/english/taxonomy` | `TaxonomyAdminController.list` | `EnglishTaxonomyService.tree/flat` | `TaxonomyRepository.all` | 否：Controller 内 view 分支 | E-04 |
| 新建分类 | POST `/api/v1/admin/english/taxonomy` | `create` | `EnglishTaxonomyService.create`→`TaxonomyCommandService.create` | `TaxonomyRepository`（slugExists/insert） | 是（业务在 service+policy） | — |
| 更新分类 | PUT `/api/v1/admin/english/taxonomy/{id}` | `update` | `EnglishTaxonomyService.update`→`TaxonomyCommandService.update` | `TaxonomyRepository` | 是 | — |
| 分类排序 | POST `/api/v1/admin/english/taxonomy/{id}/move` | `move` | `EnglishTaxonomyService.move`→`TaxonomyCommandService.move` | `TaxonomyRepository.siblingIds/normalizeOrder` | 是 | — |
| 删除分类 | DELETE `/api/v1/admin/english/taxonomy/{id}` | `delete` | `EnglishTaxonomyService.delete`→`TaxonomyCommandService.delete` | `TaxonomyRepository.referencedByContent`（`usageCount` 动态表名） | 部分 | E-06 |
| 学习包列表（管理） | GET `/api/v1/admin/english/bundles` | `BundleAdminController.list` | `LearningBundleService.list` | `LearningBundleRepository.list` | 是 | — |
| 新建学习包 | POST `/api/v1/admin/english/bundles` | `create` | `LearningBundleService.create` | `LearningBundleRepository`（slugExists/insert/cefrExists） | 是 | — |
| 学习包详情 | GET `/api/v1/admin/english/bundles/{id}` | `get` | `LearningBundleService.get` | `LearningBundleRepository.detail` | 是 | — |
| 更新学习包 | PUT `/api/v1/admin/english/bundles/{id}` | `update` | `LearningBundleService.update` | `LearningBundleRepository.update` | 是 | — |
| 删除学习包 | DELETE `/api/v1/admin/english/bundles/{id}` | `delete` | `LearningBundleService.delete` | `LearningBundleRepository.delete` | 是 | — |
| 发布学习包 | POST `/api/v1/admin/english/bundles/{id}/publish` | `publish` | `LearningBundleService.publish` | `LearningBundleItemService.assertPublishable` + `LearningBundleRepository.publish` | 是 | — |
| 撤回学习包 | POST `/api/v1/admin/english/bundles/{id}/withdraw` | `withdraw` | `LearningBundleService.withdraw` | `LearningBundleRepository.withdraw` | 是 | — |
| 学习包条目 | GET `/api/v1/admin/english/bundles/{id}/items` | `items` | `LearningBundleItemService.list` | `LearningBundleItemRepository.members` + `EnglishContentRegistry` | 是 | — |
| 内容目录（可加入项） | GET `/api/v1/admin/english/bundles/{id}/catalog` | `catalog` | `LearningBundleItemService.catalog` | `LearningBundleItemRepository.members` + registry catalog | 是 | — |
| 发布就绪检查 | GET `/api/v1/admin/english/bundles/{id}/readiness` | `readiness` | `LearningBundleItemService.readiness` | `LearningBundleItemRepository.metadata` | 是 | — |
| 添加条目 | POST `/api/v1/admin/english/bundles/{id}/items` | `addItem` | `LearningBundleItemService.add` | `LearningBundleItemRepository.add`（动态表名） | 是 | — |
| 条目排序 | POST `.../{contentType}/{contentId}/move` | `moveItem` | `LearningBundleItemService.move` | `LearningBundleItemRepository.updateOrder` | 是 | — |
| 删除条目 | DELETE `.../{contentType}/{contentId}` | `removeItem` | `LearningBundleItemService.remove` | `LearningBundleItemRepository.remove` | 是 | — |
| 公开学习包列表 | GET `/api/v1/public/english/bundles` | `BundlePublicController.list` | `LearningBundleService.publicList` | `LearningBundleRepository.publishedList` + `LearningBundleItemService.publiclyAccessible` | 是 | — |
| 公开学习包详情 | GET `/api/v1/public/english/bundles/{slug}` | `get` | `LearningBundleService.publicGet` | `LearningBundleRepository.publishedBySlug` | 是 | — |
| 公开学习包条目 | GET `/api/v1/public/english/bundles/{slug}/items` | `items` | `LearningBundleItemService.publicList` | `LearningBundleItemRepository.publishedIdBySlug` | 是 | — |

### 2. 问题明细

| 编号 | 类型 | 位置（文件绝对路径:行号） | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| E-01 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\service\EnglishService.java:29-38` | `public EnglishView update(UpdateEnglishRequest request) { EnglishOverview overview = requireSingleton(); ... mapper.updateById(overview); return toView(overview); }` | 写操作（单例更新）无 `@Transactional`；同文件 `get` 为读操作也无只读注解，事务边界缺失。 | 中 | 补 `@Transactional` / `readOnly = true` |
| E-02 | 3,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\service\EnglishService.java:19-23` | `private final EnglishOverviewMapper mapper;` | 根目录 `EnglishService` 直接用 Mapper、无 Repository 层，与各技能模块「service→repository→mapper」三层结构不一致（简单单表 CRUD 可接受，但命名/分层不统一）。 | 低 | 统一为 repository 层或明确豁免说明 |
| E-03 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\controller\EnglishMetaController.java:31-34` | `return new EnglishMetaView(taxonomy.tree(), cefr.list(), questionTypes());` | Controller 内组装多个 service 结果成 View（含题型常量），属响应组装越界（应由 Assembler/application 完成）。 | 低 | 抽 `EnglishMetaAssembler` 或 application 方法 |
| E-04 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\taxonomy\controller\TaxonomyAdminController.java:37-39` | `return "flat".equalsIgnoreCase(view) ? service.flat() : service.tree();` | Controller 内用 `view` 参数做分支选择返回结构，属轻量业务分支。 | 低 | 分支下沉 service |
| E-05 | 3,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\cefr\service\CefrService.java:6,22-33` | `import com.starrainnotes.english.shared.cefr.mapper.EnglishCefrStandardMapper;` … `mapper.selectList(new LambdaQueryWrapper<EnglishCefrStandard>())` | CEFR 只读服务直接用 Mapper，无 Repository；排序规则（ORDER 常量）写在 service（可接受），但分层与其它模块不一致。 | 低 | 抽只读 Repository 或统一豁免 |
| E-06 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\taxonomy\infrastructure\TaxonomyRepository.java:99-123` | `for (String table : CONTENT_REFERENCE_TABLES) { if (!tableExists(table)) continue; ... "SELECT COUNT(*) FROM " + table + " WHERE term_id=?" ... }` | 仓储遍历 4 张内容引用表做动态表名统计，并用 `information_schema` 探表；引用关系（业务概念）散落在仓储 SQL。 | 中 | 由各内容模块经 Port 上报引用数，避免集中跨表查询 |
| E-07 | 2,8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\content\DefaultEnglishContentFacade.java:36-41` | `try { ... } catch (ApiException ex) { // 用异常判定 DEPENDENCY_UNPUBLISHED }` | 用捕获 `ApiException` 作为「依赖未发布」的控制流，属异常当分支，可读性与性能欠佳。 | 低 | 返回结构化 readiness 结果而非抛异常 |
| E-08 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\events\EnglishContentChangeAspect.java:38-41` | `private Long firstLong(Object[] args) { for (Object arg : args) if (arg instanceof Long value) return value; return null; }` | 切面用「取第一个 Long 实参」的启发式推断内容 ID，脆弱：方法签名变化即失效；错误信息提示需 content ID 但机制隐式。 | 中 | 注解显式声明 id 参数位置/SpEL |
| E-09 | 1,5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\bundle\controller\BundleAdminController.java:30,75-114` | `private final com.starrainnotes.english.shared.bundle.service.LearningBundleItemService items;` … `public java.util.List<com.starrainnotes.english.shared.bundle.dto.BundleItemView> items(...)` | Controller 内大量内联全限定类名（未 import），且同时注入两个 service（bundle + item），风格与其它模块不一致、可读性差。 | 低 | 规范化 import；必要时合并为单一 bundle application service |
| E-10 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\bundle\infrastructure\LearningBundleItemRepository.java:93-109` | `private String table(String type) { return switch (type) { case "READING" -> "english_learning_bundle_reading_item"; ... default -> throw new IllegalArgumentException(...); }; }` | 动态表名/列名由白名单 switch 拼接（安全），但属仓储承担内容类型映射规则；`LearningBundleItemRepository.members` 手工合并三张表并排序（业务排序语义在仓储）。 | 低 | 映射规则抽常量/枚举，排序语义上移 |
| E-11 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\english\shared\events\infrastructure\EnglishContentStateRepository.java:17-25` | `String table = switch (kind) { case READING -> "english_reading_article"; ... case BUNDLE -> "english_learning_bundle"; };` | 集中映射 7 张内容表名（白名单，安全），跨模块表名耦合于 shared 层；作为「共享状态查询」可接受但需保持白名单。 | 低 | 保持白名单，禁止外部传表名 |

### 3. 该模块统计

| 指标 | 值 |
|---|---|
| 涉及文件数 | `service\`(1) + `controller\`(2) + `mapper\`(1) + `entity\`(1) + `dto\`(2) + `api\`(7) + `shared\`(60) 中 bundle/content/events/taxonomy/cefr/exercise/controller/media ≈ 25 个承载逻辑的类 |
| Controller / application(Service) / infrastructure(Repository) 类数 | 6（English×2、Meta、Taxonomy、BundleAdmin、BundlePublic）/ 约 12（EnglishService、EnglishTaxonomyService、Taxonomy×2、CefrService、LearningBundle×2、MediaPortAdapter 等）/ 约 5（TaxonomyRepository、LearningBundleRepository、LearningBundleItemRepository、EnglishContentStateRepository、TaxonomyMapper 等） |
| Controller 路由方法总数 | 2(English) + 1(Meta) + 5(Taxonomy) + 12(BundleAdmin) + 3(BundlePublic) = 23 |
| Controller 直接调用 Mapper 次数 | 0 |
| Controller 含 `@Transactional` 次数 | 0 |
| Service 使用 `JdbcTemplate` 次数 | 0（`JdbcTemplate` 全部出现在 `infrastructure` 仓储，共 32 个文件） |
| 跨模块依赖对方 Mapper 清单 | 无（`MediaPortAdapter`→`MediaAssetPort` 经 Port，合规；`EnglishContentRegistry` 经 provider 接口，合规） |
| 行数最多 5 类 | `LearningBundleItemService`(269)、`TaxonomyRepository`(132)、`LearningBundleRepository`(119)、`EnglishMetaController`(≈40)、`LearningBundleService`(171)（部分估算，待确认） |
| public 方法最多 5 类 | `LearningBundleItemService`(≈9)、`TaxonomyCommandService`(5)、`TaxonomyAdminController`(5)、`EnglishContentRegistry`(≈6)、`LearningBundleRepository`(≈10) |

### 4. 整体评价

english 根目录与 shared 层的收口设计基本到位：`EnglishContentRegistry` + `EnglishContentDescriptorProvider` 用注册表解耦各模块内容，`MediaPort`/`MediaPortAdapter` 是标准的 Port/Adapter 范例，三个 Facade 只做委托不含 SQL，`LearningBundleItemService` 把发布就绪/目录分页/成员校验留在 service（269 行但职责集中在 bundle 域，属合理的胖类）。问题多为轻量级：根目录 `EnglishService` 缺事务且无 repository 层（E-01/E-02）、`EnglishMetaController` 组装 View（E-03）、切面 `firstLong` 启发式取 ID（E-08）、`DefaultEnglishContentFacade` 用异常作控制流（E-07）、以及 bundle controller 内联全限定名的风格问题（E-09）。

---

## 全量汇总

### 问题数量与严重度

| 模块 | 高 | 中 | 低 | 小计 |
|---|---|---|---|---|
| grammar | 2（G-01,G-02） | 3 | 1 | 6 |
| listening | 4（L-01,L-02,L-03,L-04） | 5 | 1 | 10 |
| reading | 3（R-01,R-02,R-03,R-04 中高 4） | 2 | 2 | 8（含 R-04 归高，实为高 4） |
| writing | 1（W-01） | 6 | 2 | 9 |
| learning | 1（A-01） | 3 | 2 | 6 |
| english 根/shared | 0 | 3 | 8 | 11 |
| **合计** | **11** | **22** | **16** | **50**（含 1 条正面参照 A-05） |

### 系统性模式（跨模块共性问题）

1. **Controller 送审分支越界（类型 1，高）**：grammar/listening/reading/writing 四个 Admin Controller 的 `update`/`updateRule`/`updatePrompt` 均含 `isSuperAdmin + reviewService.isPublished + submitEnglishUpdate` 分支并直连 `account.review` 模块（G-01、L-01、R-01、W-01），是同一模式复制四份。
2. **业务规则下沉到 Repository（类型 3）**：grammar/listening/reading/writing 的仓储普遍承担非空校验、发布前置、slug 冲突、字段校验、判分、标签维度校验（G-02、L-02/L-03、R-03、W-06/W-08）。
3. **Repository 上帝类 + 动态 SQL 拼接（类型 3/8）**：`ListeningRepository`(649)、`GrammarRepository`(475)、`ReadingRepository`(405) 体量过大；IN 列表/表名拼接集中在 listening（L-05）。
4. **跨模块直接访问对方表/仓储（类型 6，高）**：listening 读 `english_reading_article`（L-04）、reading join `english_grammar_lesson` 并删 `english_exercise`（R-03/R-04）、learning 依赖 `shared.bundle` 的 Repository（A-01）、reading 的 domain 反向依赖 infrastructure（R-02）。
5. **application 空壳化（类型 8）**：grammar/listening/writing 大量 service 为一行转发（G-04、L-06、W-09、R-07）。
6. **事务边界不一致（类型 4）**：同模块内部分方法有 `@Transactional`、部分依赖 Repository 注解；只读服务注解缺失（grammar/writing），`EnglishService.update` 完全无事务（E-01）。
7. **判分位置三处不一（类型 5）**：reading 在 application、listening/writing 在 repository（R-05、L-03、W-08）。
8. **校验/常量重复或缺失（类型 7）**：`WritingMoveRequest` 无校验（W-05）、writing 常量在 policy 与 repository 重复（W-07）、grammar slug 三重校验（G-06）、`PronunciationRuleRepository.ruleById` 死参数（L-08）。
9. **错误文案语言不统一（类型 5）**：listening/reading 的中文 detail 与 grammar/writing 的英文并存（L-09、R-06）。
10. **风格压缩（类型 5）**：writing 的 Controller 与多个 Repository 单行密集写法（W-02、W-08）。

### 正面范例（职责清晰的实现）

- `GrammarOrderingService`：排序/迁移逻辑集中在 application 并带 `@Transactional`。
- `ReadingCommandService`：校验、stats 分析、字段应用都在 application。
- `ListeningSegmentService.batchReplace` → `SegmentTimelinePolicy`：校验委托 domain。
- `TaxonomyPolicy`、`EnglishExercisePolicy`、`RecommendationEngine`：纯 domain 逻辑，无持久化依赖。
- `MediaPortAdapter`：标准 Port/Adapter，纯委托。
- `LearningProgressMigrationService`：跨模块依赖 vocabulary 的 application service（而非仓储），方向正确。
- `EnglishLearningAnalyticsQueryService`：校验/时区/rate 计算在 service 且 `readOnly = true`。


---

> **附录 C** — blog 与 tutorial

# blog 与 tutorial 分层审计

审计范围：`e:\star-rain\backend\src\main\java\com\starrainnotes\blog\`（31 个 Java 文件）、`e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\`（54 个 Java 文件）、`e:\star-rain\backend\src\main\resources\mapper\blog\*.xml`（2 个）。

参照基准：`e:\star-rain\backend\docs\english-backend-architecture.md` 及英语模块既有实现（如 `english/*/application/*QueryService` 使用 `@Transactional(readOnly = true)`、`english/api/MediaPort` + `english/shared/media/MediaPortAdapter` 通过 Port 访问媒体）。所有文件均已实际打开阅读，行号与代码片段为真实内容。

---

## 模块：blog

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 后台文章列表 | GET `/api/v1/admin/blog/posts` | `BlogAdminController#list` | `BlogQueryService#adminList` | `BlogPostMapper#selectPage`、`BlogPostTagMapper#selectPostIdsByTagSlug`、`#selectTagsByPostIds`、`MediaAssetMapper#selectBatchIds` | 基本清晰（读模型与组装混在 Service） | B9 |
| 创建文章 | POST `/api/v1/admin/blog/posts` | `BlogAdminController#create` | `BlogCommandService#create` | `BlogPostMapper#selectCount`、`#insert`、`BlogTagMapper#selectBatchIds`、`#selectOne`、`#insertIgnore`、`BlogPostTagMapper#deleteByPostId`、`#insertBatch` | 清晰 | B1、B7 |
| 文章详情 | GET `/api/v1/admin/blog/posts/{postId}` | `BlogAdminController#detail` | `BlogQueryService#adminDetail` | `BlogPostMapper#selectById`、`BlogPostTagMapper#selectTagsByPostId`、`MediaAssetMapper#selectById` | 清晰（跨模块 Mapper） | B1 |
| 更新文章 | PUT `/api/v1/admin/blog/posts/{postId}` | `BlogAdminController#update` | `BlogUpdateWorkflowService#update` →（`BlogQueryService#publishStatus` / `ContentReviewService#submitBlogUpdate`）或 `BlogCommandService#update` | `BlogPostMapper#selectById`、`#selectCount`、`#updateById`、`BlogTagMapper#selectBatchIds`、`BlogPostTagMapper#deleteByPostId`、`#insertBatch` | 不清晰（HTTP 状态码在 Service 决定；跨模块双向依赖） | B5、B6 |
| 删除文章 | DELETE `/api/v1/admin/blog/posts/{postId}` | `BlogAdminController#delete` | `BlogCommandService#delete` | `BlogPostMapper#selectById`、`#deleteById` | 清晰 | - |
| 发布文章 | POST `/api/v1/admin/blog/posts/{postId}/publish` | `BlogAdminController#publish` | `BlogCommandService#publish` | `BlogPostMapper#selectById`、`#updateById`、`BlogQueryService#adminDetail` | 清晰 | B10 |
| 撤回文章 | POST `/api/v1/admin/blog/posts/{postId}/withdraw` | `BlogAdminController#withdraw` | `BlogCommandService#withdraw` | `BlogPostMapper#selectById`、`#updateById` | 清晰 | B10 |
| 后台标签列表 | GET `/api/v1/admin/blog/tags` | `BlogTagAdminController#list` | `BlogTagService#listAll` | `BlogTagMapper#selectAllWithPostCount` | 清晰（Mapper 内部类泄漏） | B3 |
| 创建标签 | POST `/api/v1/admin/blog/tags` | `BlogTagAdminController#create` | `BlogTagService#create` | `BlogTagMapper#selectCount`、`#insert` | 不清晰（缺事务） | B2 |
| 更新标签 | PUT `/api/v1/admin/blog/tags/{tagId}` | `BlogTagAdminController#update` | `BlogTagService#update` | `BlogTagMapper#selectById`、`#selectCount`、`#updateById`、`BlogPostTagMapper#countByTagId` | 不清晰（缺事务） | B2 |
| 删除标签 | DELETE `/api/v1/admin/blog/tags/{tagId}` | `BlogTagAdminController#delete` | `BlogTagService#delete` | `BlogTagMapper#selectById`、`#deleteById`、`BlogPostTagMapper#countByTagId` | 不清晰（缺事务） | B2 |
| 公开文章列表 | GET `/api/v1/public/blog/posts` | `BlogPublicController#posts` | `BlogQueryService#publicList` | `BlogPostMapper#selectPage`、`BlogPostTagMapper#selectPostIdsByTagSlug`、`#selectTagsByPostIds`、`MediaAssetMapper#selectBatchIds` | 基本清晰 | B1、B9 |
| 公开文章详情 | GET `/api/v1/public/blog/posts/{slug}` | `BlogPublicController#postDetail` | `BlogQueryService#publicDetail` | `BlogPostMapper#selectOne`、`BlogPostTagMapper#selectTagsByPostId`、`MediaAssetMapper#selectById` | 清晰（跨模块 Mapper） | B1 |
| 公开标签 | GET `/api/v1/public/blog/tags` | `BlogPublicController#tags` | `BlogTagService#publicTags` | `BlogTagMapper#selectPublishedWithPostCount` | 清晰（SQL 内嵌业务判定） | B8 |
| 博客日历 | GET `/api/v1/public/blog/calendar` | `BlogPublicController#calendar` | `BlogQueryService#calendar` | `BlogPostMapper#selectList` | 清晰 | - |
| 博客归档 | GET `/api/v1/public/blog/archive` | `BlogPublicController#archive` | `BlogQueryService#archive` | `BlogPostMapper#selectList` | 清晰 | - |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| B1 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogQueryService.java:25`、`:49`、`:161`、`:170` | `import com.starrainnotes.media.mapper.MediaAssetMapper;` / `private final MediaAssetMapper mediaAssetMapper;` / `return mediaAssetMapper.selectBatchIds(distinct).stream().collect(...)` / `MediaAsset media = mediaAssetMapper.selectById(mediaId);` | blog 模块直接依赖 media 模块的 Mapper，绕过模块边界。media 已提供 `media/api/MediaAssetPort`（含 `publicUrl`/`publicUrls`），英语模块正是通过 `english/shared/media/MediaPortAdapter` 使用 Port。 | 高 | 改为依赖 `MediaAssetPort`（注入 Port，调用 `publicUrl`/`publicUrls`），删除对 `MediaAssetMapper` 的直接依赖。 |
| B2 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogTagService.java:37`、`:46`、`:57` | `public BlogTagWithPostCountVO create(CreateTagRequest request) {` / `public BlogTagWithPostCountVO update(Long tagId, UpdateTagRequest request) {` / `public void delete(Long tagId, boolean force) {`（三处均无 `@Transactional`） | 标签写用例为「校验 + 唯一性查询 + 写入 / 计数查询 + 删除」多语句，缺事务边界。`delete` 的 `countByTagId` 与 `deleteById` 之间存在竞态（并发加标签可致误删/漏保护）。 | 高 | 在 `create`/`update`/`delete` 上加 `@Transactional`；`resolveNames` 保留「由调用方事务包裹」的注释说明，或显式声明传播行为。 |
| B3 | 3 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogQueryService.java:10`、`:152`；`e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogTagService.java:82` | `import com.starrainnotes.blog.mapper.BlogPostTagMapper.BlogPostTagRow;` / `for (BlogPostTagRow row : postTagMapper.selectTagsByPostIds(postIds)) {` / `private BlogTagWithPostCountVO toCountVO(BlogTagMapper.BlogTagWithCountRow row) {` | Mapper 的内部 `record`（`BlogPostTagRow`、`BlogTagWithCountRow`）作为返回类型泄漏到 Service 层，Service 与持久化投影结构耦合。 | 中 | 将投影 record 上移为独立 DTO/VO（如 `TagWithCountRow` 放到 dto 包），或让 Mapper 直接返回 VO 所需的最小投影类型；Service 不直接引用 Mapper 内部类。 |
| B4 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogQueryService.java`（整类，无 `@Transactional`） | 类声明 `public class BlogQueryService {`（第 46 行），全文无 `@Transactional(readOnly = true)` | 约定要求只读查询用只读事务；英语模块所有 `*QueryService` 均标注 `@Transactional(readOnly = true)`（如 `english/reading/application/ReadingQueryService.java:16`），blog 查询服务完全缺失。 | 中 | 在 `BlogQueryService`（及 `BlogTagService` 的只读方法）类级加 `@Transactional(readOnly = true)`。 |
| B5 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogUpdateWorkflowService.java:9`、`:37-40`；`e:\star-rain\backend\src\main\java\com\starrainnotes\blog\controller\BlogAdminController.java:62` | `import org.springframework.http.HttpStatus;` / `public sealed interface BlogUpdateOutcome permits Updated, ReviewSubmitted { HttpStatus status(); Object body(); }` / `return ResponseEntity.status(outcome.status()).body(outcome.body());` | Service 返回 `HttpStatus` 与 `Object body` 并决定 HTTP 状态码（200/202），HTTP 关注点渗入服务层；Controller 退化为透传。 | 中 | Service 返回领域结果（如 `sealed` 结果 + 视图对象），由 Controller 决定 HTTP 状态码映射。 |
| B6 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogUpdateWorkflowService.java:4`、`:19`、`:23`；`e:\star-rain\backend\src\main\java\com\starrainnotes\account\review\service\ContentReviewService.java:8`、`:43`、`:166` | `import com.starrainnotes.account.review.service.ContentReviewService;` / `return new ReviewSubmitted(reviewService.submitBlogUpdate(actorId(actor), postId, request));` / `import com.starrainnotes.blog.service.BlogCommandService;` / `blogService.update(review.contentId(), request);` | blog 与 account.review 形成双向依赖：`BlogUpdateWorkflowService → ContentReviewService`，而 `ContentReviewService → BlogCommandService`（且 `ContentReviewService` 内部直接用 `JdbcTemplate` 写审核表）。模块间出现循环依赖。 | 中 | 抽出审核提交的 Port 接口（由 account.review 实现）供 blog 依赖；account.review 回调内容更新也应通过 Port/事件，避免反向依赖具体 Service。 |
| B7 | 7 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\dto\CreatePostRequest.java:22`；`e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogCommandService.java:45-46` | `Long coverMediaId,` / `.bodyMarkdown(request.bodyMarkdown()).coverMediaId(request.coverMediaId()).publishStatus(DRAFT).build();` | `coverMediaId` 无 Bean Validation，Service 也未校验其是否为存在的图片资产；media 已提供 `MediaAssetPort#requireImageIfPresent`（英语阅读模块 `ReadingCommandService:46/65` 有调用），blog 未调用，业务不变量缺失。 | 低 | 在 `BlogCommandService.create/update` 中通过 `MediaAssetPort#requireImageIfPresent` 校验封面。 |
| B8 | 3 | `e:\star-rain\backend\src\main\resources\mapper\blog\BlogTagMapper.xml:17` | `WHERE p.publish_status = 'PUBLISHED'` | XML SQL 内嵌业务判定（公开标签仅统计已发布文章），状态字面量散落在 SQL 中，规则变更需改 SQL。 | 低 | 可保留（简单单表统计），或将状态作为参数传入，使「已发布」这一业务常量集中在 Service 层。 |
| B9 | 8/5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogQueryService.java:53-69`、`:80-104` | `List<BlogPostAdminSummaryVO> items = result.getRecords().stream().map(post -> assembler.toAdminSummary(...)).toList(); return new BlogPostAdminPageVO(items, safePage, safeSize, result.getTotal(), pages(result.getTotal(), safeSize));` | 查询服务同时承担分页参数规整、分页数学、读模型编排与 Page 信封组装（192 行、7 个 public 方法），超出「读模型」职责；分页/信封组装未下沉到 Assembler。 | 低 | 将分页信封与 VO 组装收敛到 Assembler/独立组装方法，Service 只负责取数与规则。 |
| B10 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogCommandService.java:30-32`；`e:\star-rain\backend\src\main\java\com\starrainnotes\blog\service\BlogQueryService.java:85`、`:108`、`:118`、`:127` | `public static final String PUBLISHED = "PUBLISHED";` / `.eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED)` | 状态常量定义在 CommandService，却被 QueryService 反向引用（查询依赖命令服务的常量），分层方向不自然，且同一常量语义在多处重复表达。 | 低 | 将发布状态常量抽到独立枚举/常量类（如 `BlogPublishStatus`），供命令与查询共用。 |

### 3. 该模块统计

- 类数量：controller 3、service 4、mapper 3、assembler 1、entity 2、dto 4、vo 13、package-info 1（合计 31）。
- Controller 直接调用 Mapper 次数：0。
- Controller 含 `@Transactional` 次数：0。
- Service 使用 `JdbcTemplate` 次数：0。
- 跨模块依赖对方 Mapper 清单：`blog.service.BlogQueryService` → `media.mapper.MediaAssetMapper`（B1）。
- 行数最多的 5 个类：`BlogQueryService` 192、`BlogTagService` 128、`BlogCommandService` 126、`BlogAdminController` 68、`BlogPostAssembler` 64。
- public 方法数最多的 5 个类：`BlogCommandService` 8（含 3 个 `public static final` 常量）、`BlogPostAssembler` 7、`BlogQueryService` 7、`BlogAdminController` 7、`BlogTagService` 6。

### 4. 整体评价（200 字以内）

blog 分层总体清晰：Controller 仅绑定参数与状态码、返回 VO 不返回 Entity，Mapper 无业务规则，写用例有 `@Transactional`，复杂映射已抽出 `BlogPostAssembler`。主要问题集中在三处：查询服务直接依赖 media 的 `MediaAssetMapper` 造成跨模块 Mapper 依赖（B1，media 已有 `MediaAssetPort`）；`BlogTagService` 写操作缺事务（B2）；Mapper 内部 record 泄漏到 Service（B3）。此外查询服务普遍缺 `readOnly=true`，工作流服务把 `HttpStatus` 与响应体决策留在服务层，并与 account.review 形成双向依赖。建议优先修 B1/B2/B3。

---

## 模块：tutorial

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 后台教程列表 | GET `/api/v1/admin/tutorials` | `TutorialAdminController#list` | `TutorialQueryService#adminList` | `TutorialMapper#selectPage`、`TutorialCategoryMapper#selectList`、`TutorialNodeMapper#selectList` | 基本清晰（内联组装） | T4 |
| 创建教程 | POST `/api/v1/admin/tutorials` | `TutorialAdminController#create` | `TutorialCommandService#create` | `TutorialCategoryMapper#selectById`、`TutorialMapper#selectCount`、`#selectList`、`#insert` | 清晰 | - |
| 教程详情 | GET `/api/v1/admin/tutorials/{tutorialId}` | `TutorialAdminController#detail` | `TutorialQueryService#adminDetail` | `TutorialMapper#selectById`、`TutorialCategoryMapper#selectList` | 清晰 | - |
| 更新教程 | PUT `/api/v1/admin/tutorials/{tutorialId}` | `TutorialAdminController#update` | `TutorialCommandService#update` | `TutorialMapper#selectById`、`#selectCount`、`#selectList`、`#updateById` | 清晰 | - |
| 删除教程 | DELETE `/api/v1/admin/tutorials/{tutorialId}` | `TutorialAdminController#delete` | `TutorialCommandService#delete` | `TutorialMapper#selectById`、`#deleteById`、`TutorialNodeMapper#selectCount` | 基本清晰（跨聚合直接用 node Mapper） | T10 |
| 教程排序移动 | POST `/api/v1/admin/tutorials/{tutorialId}/move` | `TutorialAdminController#move` | `TutorialCommandService#move` | `TutorialMapper#selectById`、`#selectList`、`#updateById` | 清晰 | - |
| 发布教程 | POST `/api/v1/admin/tutorials/{tutorialId}/publish` | `TutorialAdminController#publish` | `TutorialCommandService#publish` | `TutorialMapper#selectById`、`#updateById` | 清晰 | - |
| 撤回教程 | POST `/api/v1/admin/tutorials/{tutorialId}/withdraw` | `TutorialAdminController#withdraw` | `TutorialCommandService#withdraw` | `TutorialMapper#selectById`、`#updateById` | 清晰 | - |
| 后台分类树 | GET `/api/v1/admin/tutorial-categories/tree` | `TutorialCategoryAdminController#tree` | `TutorialCategoryQueryService#adminTree` | `TutorialCategoryMapper#selectList` | 清晰（与 Command 重复私有方法） | T9 |
| 创建分类 | POST `/api/v1/admin/tutorial-categories` | `TutorialCategoryAdminController#create` | `TutorialCategoryCommandService#create` | `TutorialCategoryMapper#selectCount`、`#insert` | 清晰 | - |
| 更新分类 | PUT `/api/v1/admin/tutorial-categories/{categoryId}` | `TutorialCategoryAdminController#update` | `TutorialCategoryCommandService#update` | `TutorialCategoryMapper#selectById`、`#selectCount`、`#updateById` | 清晰 | - |
| 分类排序移动 | POST `/api/v1/admin/tutorial-categories/{categoryId}/move` | `TutorialCategoryAdminController#move` | `TutorialCategoryCommandService#move` | `TutorialCategoryMapper#selectById`、`#selectList`、`#updateById` | 清晰 | - |
| 删除分类 | DELETE `/api/v1/admin/tutorial-categories/{categoryId}` | `TutorialCategoryAdminController#delete` | `TutorialCategoryCommandService#delete` | `TutorialCategoryMapper#selectById`、`#deleteById`、`TutorialMapper#selectCount` | 清晰 | - |
| 节点树 | GET `/api/v1/admin/tutorials/{tutorialId}/nodes` | `TutorialNodeAdminController#tree` | `TutorialNodeQueryService#tree` | `TutorialMapper#selectById`、`TutorialNodeMapper#selectList`（+`TutorialTreeBuilder`） | 清晰 | - |
| 课程大纲 | GET `/api/v1/admin/tutorials/{tutorialId}/curriculum` | `TutorialNodeAdminController#curriculum` | `TutorialNodeQueryService#curriculum` | `TutorialMapper#selectById`、`TutorialCategoryMapper#selectById`、`TutorialNodeMapper#selectList` | 基本清晰（内联组装） | T4 |
| 创建分组 | POST `/api/v1/admin/tutorials/{tutorialId}/groups` | `TutorialNodeAdminController#createGroup` | `TutorialNodeService#createGroup` | `TutorialMapper#selectById`、`TutorialNodeMapper#selectList`、`#insert` | 不清晰（上帝类） | T2 |
| 更新分组 | PUT `/api/v1/admin/tutorials/{tutorialId}/groups/{groupId}` | `TutorialNodeAdminController#updateGroup` | `TutorialNodeService#updateGroup` | `TutorialNodeMapper#selectById`、`#updateById` | 不清晰（上帝类） | T2 |
| 删除分组 | DELETE `/api/v1/admin/tutorials/{tutorialId}/groups/{groupId}` | `TutorialNodeAdminController#deleteGroup` | `TutorialNodeService#deleteGroup` | `TutorialNodeMapper#selectById`、`#selectCount`、`#deleteById`、`#selectList`、`#updateById` | 不清晰（上帝类） | T2 |
| 分组排序移动 | POST `/api/v1/admin/tutorials/{tutorialId}/groups/{groupId}/move` | `TutorialNodeAdminController#moveGroup` | `TutorialNodeService#moveGroup` | `TutorialNodeMapper#selectById`、`#selectList`、`#updateById` | 不清晰（上帝类） | T2 |
| 创建章节 | POST `/api/v1/admin/tutorials/{tutorialId}/chapters` | `TutorialNodeAdminController#createChapter` | `TutorialNodeService#createChapter` | `TutorialMapper#selectById`、`TutorialNodeMapper#selectById`、`#selectCount`、`#selectList`、`#insert` | 不清晰（上帝类） | T2 |
| 章节详情 | GET `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}` | `TutorialNodeAdminController#getChapter` | `TutorialNodeQueryService#chapter` | `TutorialNodeMapper#selectById` | 清晰 | - |
| 更新章节 | PUT `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}` | `TutorialNodeAdminController#updateChapter` | `TutorialChapterUpdateWorkflowService#update` →（`TutorialNodeQueryService#chapterPublishStatus` / `ContentReviewService#submitTutorialChapterUpdate`）或 `TutorialNodeService#updateChapter` | `TutorialNodeMapper#selectById`、`#selectCount`、`#updateById` | 不清晰（HTTP 状态码在 Service；跨模块双向依赖） | T11、T12 |
| 删除章节 | DELETE `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}` | `TutorialNodeAdminController#deleteChapter` | `TutorialNodeService#deleteChapter` | `TutorialNodeMapper#selectById`、`#deleteById`、`#selectList`、`#updateById` | 不清晰（上帝类） | T2 |
| 发布章节 | POST `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}/publish` | `TutorialNodeAdminController#publishChapter` | `TutorialNodeService#publishChapter` | `TutorialNodeMapper#selectById`、`TutorialMapper#selectById`、`TutorialNodeMapper#updateById` | 不清晰（上帝类） | T2 |
| 撤回章节 | POST `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}/withdraw` | `TutorialNodeAdminController#withdrawChapter` | `TutorialNodeService#withdrawChapter` | `TutorialNodeMapper#selectById`、`#updateById` | 不清晰（上帝类） | T2 |
| 章节排序移动 | POST `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}/move` | `TutorialNodeAdminController#moveChapter` | `TutorialNodeService#moveChapter` | `TutorialNodeMapper#selectById`、`#selectList`、`#updateById` | 不清晰（上帝类） | T2 |
| 章节换组 | POST `/api/v1/admin/tutorials/{tutorialId}/chapters/{chapterId}/reassign` | `TutorialNodeAdminController#reassignChapter` | `TutorialNodeService#reassignChapter` | `TutorialNodeMapper#selectById`、`#selectList`、`#updateById` | 不清晰（上帝类） | T2 |
| 节点通用移动 | POST `/api/v1/admin/tutorials/{tutorialId}/nodes/{nodeId}/move` | `TutorialNodeAdminController#moveNode` | `TutorialNodeService#moveNode`（自调用 `moveGroup`/`moveChapter`） | `TutorialNodeMapper#selectById`、`#selectList`、`#updateById` | 不清晰（自调用致事务注解失效） | T2、T6 |
| 公开分类树 | GET `/api/v1/public/tutorial-categories/tree` | `TutorialPublicController#categoryTree` | `TutorialCategoryQueryService#publicTree` | `TutorialCategoryMapper#selectList`、`TutorialMapper#selectList` | 基本清晰（魔法字符串） | T8 |
| 公开教程列表 | GET `/api/v1/public/tutorials` | `TutorialPublicController#tutorials` | `TutorialQueryService#publicList` | `TutorialMapper#selectList`、`TutorialCategoryMapper#selectList`、`TutorialNodeMapper#selectList`、`MediaAssetMapper#selectBatchIds` | 不清晰（跨模块 Mapper；内联组装） | T1、T4 |
| 公开教程详情 | GET `/api/v1/public/tutorials/{tutorialSlug}` | `TutorialPublicController#tutorialDetail` | `TutorialQueryService#publicDetail` | `TutorialMapper#selectOne`、`TutorialCategoryMapper#selectById`、`TutorialNodeMapper#selectList`、`MediaAssetMapper#selectById`（+`TutorialCurriculumBuilder`） | 不清晰（跨模块 Mapper；内联组装） | T1、T4 |
| 公开章节阅读 | GET `/api/v1/public/tutorials/{tutorialSlug}/chapters/{chapterSlug}` | `TutorialPublicController#chapterDetail` | `TutorialQueryService#publicChapter` | `TutorialMapper#selectOne`、`TutorialNodeMapper#selectList`（+`TutorialCurriculumBuilder`） | 不清晰（面包屑/前后篇逻辑在查询服务内联） | T3 |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| T1 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialQueryService.java:8`、`:58`、`:304`、`:312` | `import com.starrainnotes.media.mapper.MediaAssetMapper;` / `private final MediaAssetMapper mediaAssetMapper;` / `return mediaAssetMapper.selectBatchIds(distinct).stream()...` / `MediaAsset media = mediaAssetMapper.selectById(mediaId);` | tutorial 直接依赖 media 的 Mapper，绕过模块边界；media 已提供 `MediaAssetPort#publicUrls/publicUrl`。 | 高 | 注入 `MediaAssetPort` 替换 `MediaAssetMapper`，移除跨模块 Mapper 依赖。 |
| T2 | 2/8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialNodeService.java:57`、`:91`、`:205`（整类 317 行、12 个 public 方法） | `public AdminTreeNodeView createGroup(Long tutorialId, CreateGroupRequest request) {` / `public ChapterDetailView createChapter(Long tutorialId, CreateChapterRequest request) {` / `public void moveNode(Long tutorialId, Long nodeId, MoveNodeRequest request) {` | 单个 Service 同时承担 GROUP 与 CHAPTER 两类资源的 CRUD、排序移动、换组与通用移动，是典型上帝类；职责过载、改动影响面大。 | 高 | 按资源拆分：`TutorialGroupCommandService`（分组 CRUD/排序）与 `TutorialChapterCommandService`（章节 CRUD/发布/换组/排序），共享私有查询下沉到 Repository/QueryService。 |
| T3 | 2/8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialQueryService.java:66`、`:107`、`:158`、`:321`（整类 291 行） | `public TutorialPageView adminList(...)` / `public List<PublicTutorialSummaryView> publicList(String categorySlug) {` / `public PublicChapterView publicChapter(String tutorialSlug, String chapterSlug) {` / `private String formatUtc(LocalDateTime utc) {` | 查询服务同时承担 admin 与 public 两套读模型、面包屑/前后篇构建、时区格式化，`formatUtc` 与 `TutorialAssembler#format` 重复实现；职责过载且与 Assembler 重叠。 | 中 | 拆分 admin/public 查询服务；时间格式化统一走 Assembler；面包屑/前后篇可抽出专门的组装器。 |
| T4 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialQueryService.java:86`、`:129`、`:150`、`:206`；`e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\assembler\TutorialAssembler.java:19` | `map(t -> new AdminTutorialSummaryView(...)).toList();` / `new PublicTutorialSummaryView(...)` / `return new PublicTutorialDetailView(...)` / `return new PublicChapterView(...)`；Assembler 仅有 `toAdminDetail` | Assembler 只承担了 admin 详情一处映射，列表/公开详情/公开章节等响应组装仍在 Service 内联 `new`，Assembler 名不副实（对比 blog 的 `BlogPostAssembler` 覆盖了 4 类映射）。 | 中 | 把 `AdminTutorialSummaryView`/`PublicTutorialSummaryView`/`PublicTutorialDetailView`/`PublicChapterView`/`AdminCurriculumView` 的映射补入 `TutorialAssembler`，Service 只做取数与编排。 |
| T5 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialQueryService.java`、`TutorialNodeQueryService.java`、`TutorialCategoryQueryService.java`（三类均无 `@Transactional`） | `public class TutorialQueryService {`（第 50 行）/ `public class TutorialNodeQueryService {`（第 29 行）/ `public class TutorialCategoryQueryService {`（第 20 行），全文无 `readOnly=true` | 三个只读查询服务均缺只读事务；英语模块 `*QueryService` 已统一 `@Transactional(readOnly = true)`。 | 中 | 三个查询服务类级加 `@Transactional(readOnly = true)`。 |
| T6 | 4 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialNodeService.java:212`、`:220` | `moveGroup(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));` / `moveChapter(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));` | `moveNode`（`@Transactional`）自调用同类 `@Transactional` 方法，代理失效、内部注解不生效。当前外层事务仍存在，实际风险低，但属脆弱写法。 | 低 | 抽取无事务注解的私有实现方法，由公共入口方法统一包裹事务。 |
| T7 | 5 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialQueryService.java:31` | `import java.sql.Timestamp;`（全文未使用） | 查询服务残留未使用的持久化类型 import，属层次不洁的轻微信号。 | 低 | 删除未使用 import。 |
| T8 | 5/8 | `TutorialNodeService.java:45-49`、`TutorialQueryService.java:53`、`TutorialNodeQueryService.java:30-32`、`TutorialCurriculumBuilder.java:53/60/62`、`TutorialCategoryQueryService.java:28` | `private static final String PUBLISHED = "PUBLISHED";`（多处重复）/ `"GROUP".equals(node.getNodeType())` / `.eq(Tutorial::getPublishStatus, "PUBLISHED")` | `PUBLISHED`/`DRAFT`/`WITHDRAWN`/`GROUP`/`CHAPTER` 状态字面量在 5 个类中各自重复声明或直接内联，规则与文案无单一来源。 | 中 | 抽出 `TutorialNodeType`、`TutorialPublishStatus` 枚举/常量类统一引用。 |
| T9 | 5/8 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialCategoryCommandService.java:102`、`TutorialCategoryQueryService.java:40`；`TutorialNodeService.java:272`、`TutorialNodeQueryService.java:69` | `private CategoryNodeView toAdminNode(TutorialCategory category) {`（两个类各一份）/ `private TutorialNode requireChapter(Long tutorialId, Long chapterId) {`（两个类各一份） | 命令/查询两个服务各自复制了 `toAdminNode`、`loadAll`、`requireChapter` 等私有方法，同一映射与校验存在两份实现，易漂移。 | 中 | 合并到共享 Assembler（映射）与共享校验组件/Repository（取数与存在性校验）。 |
| T10 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialCommandService.java:34`、`:75` | `private final TutorialNodeMapper nodeMapper;` / `Long nodes = nodeMapper.selectCount(new LambdaQueryWrapper<TutorialNode>().eq(TutorialNode::getTutorialId, tutorialId));` | 教程命令服务越过节点聚合边界，直接用 `TutorialNodeMapper` 统计节点，与 `TutorialNodeService` 的职责重叠，同一聚合被两个服务直接读写。 | 低 | 通过 `TutorialNodeQueryService`（或专门的节点 Repository）暴露「是否存在子节点」能力，避免跨聚合直连 Mapper。 |
| T11 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialChapterUpdateWorkflowService.java:9`、`:36-38`；`e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\controller\TutorialNodeAdminController.java:108-109` | `import org.springframework.http.HttpStatus;` / `public sealed interface Outcome permits Updated, ReviewSubmitted { HttpStatus status(); Object body(); }` / `ResponseEntity.status(outcome.status()).body(outcome.body());` | 与 B5 同型：Service 决定 HTTP 状态码并携带响应体，HTTP 关注点泄漏到服务层。 | 中 | Service 返回领域结果，Controller 负责状态码映射。 |
| T12 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialChapterUpdateWorkflowService.java:19`、`:23`；`e:\star-rain\backend\src\main\java\com\starrainnotes\account\review\service\ContentReviewService.java:25`、`:44`、`:170` | `private final ContentReviewService reviewService;` / `reviewService.submitTutorialChapterUpdate(...)` / `import com.starrainnotes.tutorial.service.TutorialNodeService;` / `tutorialNodeService.updateChapter(tutorialId, review.contentId(), request);` | tutorial 与 account.review 形成双向依赖（工作流 → 审核服务；审核服务 → `TutorialNodeService`）。 | 中 | 通过 Port 接口解耦审核提交与内容回写，消除模块循环依赖。 |
| T13 | 7 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\dto\CreateTutorialRequest.java:20`；`e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\service\TutorialCommandService.java:43` | `Long coverMediaId,` / `.summary(request.summary()).coverMediaId(request.coverMediaId())` | `coverMediaId` 无 Bean Validation，Service 亦未校验是否为图片资产（未调用 `MediaAssetPort#requireImageIfPresent`），业务不变量缺失。 | 低 | 在 `TutorialCommandService.create/update` 中校验封面媒体类型。 |
| T14 | 1/风格 | `e:\star-rain\backend\src\main\java\com\starrainnotes\tutorial\controller\TutorialPublicController.java:45` | `public com.starrainnotes.tutorial.dto.PublicChapterView chapterDetail(...)` | Controller 方法签名内联全限定类名而非 import，风格不一致（不影响分层，仅可读性）。 | 低 | 改为顶部 import。 |

### 3. 该模块统计

- 类数量：controller 4、service 9、mapper 3、assembler 2、entity 3、dto 32、package-info 1（合计 54）。
- Controller 直接调用 Mapper 次数：0。
- Controller 含 `@Transactional` 次数：0。
- Service 使用 `JdbcTemplate` 次数：0。
- 跨模块依赖对方 Mapper 清单：`tutorial.service.TutorialQueryService` → `media.mapper.MediaAssetMapper`（T1）。
- 行数最多的 5 个类：`TutorialNodeService` 317、`TutorialQueryService` 291、`TutorialNodeAdminController` 133、`TutorialCommandService` 123、`TutorialCategoryCommandService` 91。
- public 方法数最多的 5 个类：`TutorialNodeAdminController` 15、`TutorialNodeService` 12、`TutorialAdminController` 8、`TutorialCommandService` 6、`TutorialCategoryAdminController` 5。

### 4. 整体评价（200 字以内）

tutorial 的路由与 Controller 边界干净（无直接调 Mapper、无 `@Transactional`、返回 View 不返回 Entity），命令服务事务边界基本完整，`TutorialTreeBuilder`/`TutorialCurriculumBuilder` 作为无状态构建器是亮点。主要问题是「胖类 + 组装错位 + 跨模块 Mapper」：`TutorialNodeService`（317 行、12 方法）同时管分组与章节两类资源，`TutorialQueryService`（291 行）混合 admin/public 读模型与内联组装，使 `TutorialAssembler` 形同虚设；查询服务普遍缺 `readOnly=true`；`MediaAssetMapper` 被直接依赖。建议优先拆 T2、收敛 T4、修 T1/T5。

---

## 附：跨模块依赖与事务边界补充

- 两个模块的查询服务均未使用 `@Transactional(readOnly = true)`，与 `english-backend-architecture.md` 第 14 行「内容查询统一使用只读事务」的既有约定不一致。
- `BlogCommandService` / `TutorialCommandService` / `TutorialNodeService` / `TutorialCategoryCommandService` 的写方法均已正确标注 `@Transactional`；`BlogTagService` 是 blog 侧唯一缺事务的写服务。
- 两个 `*UpdateWorkflowService` 自身未声明事务，而是依赖下游 `ContentReviewService` 与命令服务的 `@Transactional`；由于先读 `publishStatus`/`chapterPublishStatus` 再决定分支，读与写分处两个事务，存在 TOCTOU 竞态（低风险，待确认是否需在同一事务内完成判定）。
- 跨模块 Mapper 依赖共 2 处（blog、tutorial 各 1），media 模块已提供 `media/api/MediaAssetPort` 作为替代端口。
- 无 Controller 直接调用 Mapper、无 Controller 使用 `@Transactional`、无 blog/tutorial Service 使用 `JdbcTemplate`。


---

> **附录 D** — portfolio / profile / site / media

# portfolio / profile / site / media 分层审计

审计对象：`com.starrainnotes` 下 portfolio、profile、site、media 四个「传统分层」模块。
对比基准：英语模块通过 `MediaPort` / `MediaPortAdapter` 解耦媒体依赖（见 `english-v2-final-boundary-audit.md`）；本报告中的「跨模块依赖对方 Mapper」问题即以此为对照。
审计方式：逐文件实际阅读，行号与代码片段均来自当前工作区文件。行数以实际文件为准（与任务描述中的概数略有差异，已在统计节标注）。

问题类型编号（下同）：
1 Controller 越界 · 2 Service 越界 · 3 Mapper/Repository 越界 · 4 事务边界 · 5 分层/命名不一致 · 6 依赖方向 · 7 校验缺失/重复 · 8 空壳类与胖类

---

## 模块：portfolio

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 后台项目分页列表 | GET `/api/v1/admin/portfolio/projects` | PortfolioAdminController#list | PortfolioQueryService#adminList | PortfolioProjectMapper#selectPage、MediaAssetMapper#selectBatchIds（经 mediaLinks） | 基本清晰（组装内联） | P5、P6 |
| 后台新建项目 | POST `/api/v1/admin/portfolio/projects` | PortfolioAdminController#create | PortfolioCommandService#create | PortfolioProjectMapper#insert/selectCount/selectList、PortfolioProjectMediaMapper#insert、MediaAssetMapper#selectById | 清晰 | P6 |
| 后台项目详情 | GET `/api/v1/admin/portfolio/projects/{projectId}` | PortfolioAdminController#detail | PortfolioQueryService#adminDetail | PortfolioProjectMapper#selectById、PortfolioProjectMediaMapper#selectList、MediaAssetMapper#selectBatchIds、PortfolioProjectPrototypeMapper#selectOne（经 prototypeService.view） | 组装散落在 QueryService | P5 |
| 后台更新项目 | PUT `/api/v1/admin/portfolio/projects/{projectId}` | PortfolioAdminController#update | PortfolioCommandService#update | PortfolioProjectMapper#updateById/selectCount、PortfolioProjectMediaMapper#updateById/insert/deleteById、MediaAssetMapper#selectById | 清晰 | P6 |
| 后台删除项目 | DELETE `/api/v1/admin/portfolio/projects/{projectId}` | PortfolioAdminController#delete | PortfolioCommandService#delete → PortfolioPrototypeService#delete | PortfolioProjectPrototypeMapper#selectOne/deleteById、PortfolioProjectMapper#deleteById | 缺事务 | P4-1 |
| 发布项目 | POST `/api/v1/admin/portfolio/projects/{projectId}/publish` | PortfolioAdminController#publish | PortfolioCommandService#publish → PortfolioPrototypeService#publish | PortfolioProjectMapper#selectById/updateById、PortfolioProjectPrototypeMapper#selectOne | 缺事务 + 文件/DB 不一致 | P4-1、P4-2 |
| 撤回项目 | POST `/api/v1/admin/portfolio/projects/{projectId}/withdraw` | PortfolioAdminController#withdraw | PortfolioCommandService#withdraw | PortfolioProjectMapper#selectById/updateById | 缺事务 | P4-1 |
| 上传 ZIP 原型 | POST `/api/v1/admin/portfolio/projects/{projectId}/prototype` | PortfolioAdminController#uploadPrototype | PortfolioQueryService#adminDetail + PortfolioPrototypeService#upload | MediaService#createArchiveAsset（MediaAssetMapper#insert/updateById）、PortfolioProjectPrototypeMapper#insert/updateById | Controller 含业务分支 | P1-1、P4-2、P7-3 |
| 绑定已有 ZIP 资源为原型 | POST `/api/v1/admin/portfolio/projects/{projectId}/prototype/media/{mediaId}` | PortfolioAdminController#attachPrototype | PortfolioQueryService#adminDetail + PortfolioPrototypeService#attach | PortfolioProjectPrototypeMapper#selectOne/insert/updateById、MediaService#requireAsset（MediaAssetMapper#selectById） | Controller 含业务分支 | P1-1、P6 |
| 删除原型 | DELETE `/api/v1/admin/portfolio/projects/{projectId}/prototype` | PortfolioAdminController#deletePrototype | PortfolioQueryService#adminDetail + PortfolioPrototypeService#delete | PortfolioProjectPrototypeMapper#selectOne/deleteById、MediaService#delete | Controller 做存在性探测 | P1-2 |
| 原型文件预览 | GET `/api/v1/admin/portfolio/projects/{projectId}/prototype-preview/{*path}` | PortfolioAdminController#previewPrototype | PortfolioPrototypeService#previewResource | PortfolioProjectPrototypeMapper#selectOne + 文件系统读取 | 基本清晰 | P4-3 |
| 公开项目列表 | GET `/api/v1/public/portfolio/projects` | PortfolioPublicController#list | PortfolioQueryService#publicList | PortfolioProjectMapper#selectList、MediaAssetMapper#selectBatchIds | 基本清晰 | P6 |
| 公开项目详情 | GET `/api/v1/public/portfolio/projects/{slug}` | PortfolioPublicController#detail | PortfolioQueryService#publicDetail | PortfolioProjectMapper#selectOne/selectList、MediaAssetMapper#selectById、PortfolioProjectPrototypeMapper#selectOne | 组装散落 | P5 |

### 2. 问题明细

| 编号 | 类型 | 位置 | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| P1-1 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\portfolio\controller\PortfolioAdminController.java:87` | `AdminProjectDetailView project = queryService.adminDetail(projectId);`<br>`return prototypeService.upload(projectId, file, "PUBLISHED".equals(project.publishStatus()));` | Controller 为取得「是否已发布」这一业务状态，先额外调用 QueryService 再在 Controller 内做 `"PUBLISHED".equals(...)` 分支判断，属业务规则泄漏到 HTTP 层（第 95 行 attachPrototype 同样）。 | 中 | 把 `published` 判定下沉到 `PortfolioPrototypeService.upload/attach`，Controller 只传 `projectId`。 |
| P1-2 | 1 | `...\portfolio\controller\PortfolioAdminController.java:100` | `public void deletePrototype(@PathVariable Long projectId) { queryService.adminDetail(projectId); prototypeService.delete(projectId); }` | Controller 先调用 `queryService.adminDetail` 仅做存在性校验并丢弃返回值，属于把「资源是否存在」的用例规则放进 Controller，且造成一次多余查询。 | 低 | 由 `PortfolioPrototypeService.delete` 内部校验项目存在性。 |
| P2-1 | 2 | `...\portfolio\service\PortfolioPrototypeService.java:55` | `public ProjectPrototypeView upload(Long projectId, MultipartFile archive, boolean published) {` | Service 直接接收 `MultipartFile`（HTTP 层类型），且方法体内做文件系统 IO、ZIP 安全校验、DB 写入，职责过载。 | 中 | 定义领域入参（byte[]/InputStream + 元数据），HTTP 类型仅在 Controller 出现。 |
| P2-2 | 2 | `...\portfolio\service\PortfolioPrototypeService.java:375` | `private static boolean isZip(MultipartFile file) {` | 同上，静态工具方法直接依赖 `MultipartFile`。 | 低 | 见 P2-1。 |
| P3-1 | 3 | `...\portfolio\mapper\PortfolioProjectMapper.java:8` | `public interface PortfolioProjectMapper extends BaseMapper<PortfolioProject> {` | 三个 Mapper 均为空 BaseMapper（无 default 业务方法），本身不越界；但被跨模块直接注入（见 P6），属依赖方向问题而非 Mapper 内部越界。此处记录为「无越界」。 | 低 | 保持；跨模块改为走 Service/Port。 |
| P4-1 | 4 | `...\portfolio\service\PortfolioCommandService.java:105` | `@SeoContentChange(table = "portfolio_project", pathPrefix = "/portfolio/")`<br>`public void delete(Long projectId) {` | `delete`（105）、`publish`（112）、`withdraw`（127）三个写操作均只有 `@SeoContentChange`，**缺少 `@Transactional`**；而 `create`（64）、`update`（85）有。删除会级联删文件+原型行+项目行，跨多条语句却无事务边界。 | 高 | 为 `delete/publish/withdraw` 补 `@Transactional`。 |
| P4-2 | 4 | `...\portfolio\service\PortfolioPrototypeService.java:124` | `private ProjectPrototypeView bind(Long projectId, MediaAsset mediaAsset, byte[] bytes, boolean published) {` | `bind` 先落盘（`Files.move`/`copyTree`，136-146 行）再写 DB（`mapper.insert/updateById`，158-161 行），整个过程**不在事务内**；`upload`（55）由 Controller 直接调用，也无事务。文件系统与数据库不具备原子性（仅靠 catch 手动删除补偿，且若 JVM 崩溃则残留）。 | 高 | 明确「先写 DB 记录（PENDING）再落盘、失败回滚」或使用补偿事务/临时目录；至少在文档中固化一致性策略。 |
| P4-3 | 4 | `...\portfolio\service\PortfolioQueryService.java:38` | `public class PortfolioQueryService {` | 该 QueryService 全部为只读查询，但类/方法上**没有任何 `@Transactional(readOnly = true)`**（同模块 `PortfolioCommandService` 已用 `@Transactional`），不符合「只读查询 readOnly=true」约定。 | 中 | 类级加 `@Transactional(readOnly = true)`。 |
| P5-1 | 5 | `...\portfolio\service\PortfolioQueryService.java:62` | `List<AdminProjectSummaryView> items = result.getRecords().stream()`<br>`.map(project -> new AdminProjectSummaryView(...))` | 响应组装（Entity→View 字段映射、封面批量解析、srcSet 拼装）直接内联在 QueryService，模块内**没有 Assembler**；对比教程模块已有 `TutorialAssembler`/`TutorialNodeAssembler`。 | 中 | 抽 `PortfolioViewAssembler` 统一组装。 |
| P5-2 | 5 | `...\portfolio\service\PortfolioQueryService.java:189` | `private String usableDemoUrl(String rawDemoUrl, String rawRepositoryUrl) {` | `normalizeHttpUrl`/`usableDemoUrl`/`withoutTrailingSlash`/`invalidExternalUrl` 与 `PortfolioCommandService.java:240-276` 完全重复实现，URL 规则散落两处。 | 中 | 抽公共 `PortfolioUrlPolicy`，两服务共用。 |
| P6-1 | 6 | `...\portfolio\service\PortfolioCommandService.java:7` | `import com.starrainnotes.media.mapper.MediaAssetMapper;` | portfolio 直接依赖 media 模块的 Mapper（`PortfolioCommandService:7/60`、`PortfolioQueryService:7/45`），而英语模块是通过 `MediaPort`/`MediaPortAdapter` 解耦的。跨模块 Mapper 直连破坏了边界。 | 高 | 参照英语，通过 `MediaAssetPort`（media 已存在）或专用 Port 访问媒体。 |
| P6-2 | 6 | `...\portfolio\service\PortfolioQueryService.java:8` | `import com.starrainnotes.media.service.MediaService;` | portfolio 依赖 media 的 Service（`srcSetOf`），同时 media 又依赖 portfolio 的 Service（见 M6-1），形成 **portfolio ↔ media 跨模块循环依赖**（media 侧用 `@Lazy` 破环）。 | 高 | 抽出 `MediaAssetPort`/`srcSet` 能力接口，消除环。 |
| P6-3 | 6 | `...\portfolio\service\PortfolioQueryService.java:20` | `import com.starrainnotes.site.service.SiteSettingsTimezone;` | portfolio 依赖 site 模块的 `SiteSettingsTimezone` 组件；site 又依赖 media Mapper。时间/时区能力被当作跨模块公共组件，依赖方向不清晰。 | 中 | 将时区能力提升到 common/shared 层，或定义只读 Port。 |
| P7-3 | 7 | `...\portfolio\service\PortfolioPrototypeService.java:108` | `public void validateArchive(byte[] bytes) {` | ZIP 魔数/体积校验在 `PortfolioPrototypeService.validateArchive`（109-112 行）、`bind`（125 行）、`MediaService.createArchiveAsset`（231 行）三处重复；路径穿越/软链接/解包体积限制写在 Service 的静态工具方法里（`safeRelative` 398、`assertNoUnixSymlinks` 383）。 | 中 | 抽独立 `PrototypeArchivePolicy` 领域组件，消除重复校验。 |
| P8-1 | 8 | `...\portfolio\service\PortfolioPrototypeService.java:31` | `public class PortfolioPrototypeService {` | 454 行、12 个 public 方法，同时承担：ZIP 安全校验、解压落盘、私有/公开目录复制、DB 行管理、媒体资源联动删除、预览资源解析。典型胖类。 | 高 | 拆分：安全校验、文件存储、原型元数据（DB）三类职责。 |

### 3. 该模块统计

- 类数量：controller 2、service 3、mapper 3、repository 0、assembler 0、entity 3、dto 10、config/filter 1、package-info 1（合计 23 个 Java 文件）。
- Controller 直接调用 Mapper 次数：**0**。
- Controller 含 `@Transactional` 次数：**0**。
- Service 使用 `JdbcTemplate` 次数：**0**。
- 跨模块依赖对方 Mapper 清单：`PortfolioCommandService` → `media.mapper.MediaAssetMapper`；`PortfolioQueryService` → `media.mapper.MediaAssetMapper`。
- 跨模块依赖对方 Service 清单：`PortfolioQueryService` → `media.service.MediaService`、`site.service.SiteSettingsTimezone`。
- 行数最多的 5 个类：`PortfolioPrototypeService` 454、`PortfolioCommandService` 369、`PortfolioQueryService` 235、`PortfolioAdminController` 110、`PortfolioProjectMedia` 56。
- public 方法/成员最多的 5 个类：`PortfolioProjectMedia` 21（getter/setter）、`PortfolioProjectPrototype` 12（getter/setter）、`PortfolioAdminController` 12、`PortfolioPrototypeService` 12、`PortfolioProject` 1（Lombok，实际字段 18）。

### 4. 整体评价

portfolio 是四模块中边界最清晰的一个：Controller 无 SQL、无 Mapper 直连，Mapper 干净，DTO 校验完备。但存在三处硬伤：写操作 `delete/publish/withdraw` 缺 `@Transactional`；`PortfolioPrototypeService` 胖类（454 行）把 ZIP 安全、文件落盘、DB 混在一起且落盘与写库无原子性；跨模块直连 media 的 Mapper/Service 并与之形成循环依赖，与英语模块的 `MediaPort` 基准背离。QueryService 无只读事务、无 Assembler、URL 规则与 Command 重复，属可接受的渐进式欠账。

---

## 模块：profile

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 后台读取 About | GET `/api/v1/admin/about` | AboutAdminController#get | ProfileQueryService#getAdmin | ProfileMapper#selectById、`JdbcTemplate.query`（loadSelectedRows） | 持久化细节在 Service | R2-1、R2-2 |
| 后台更新 About | PUT `/api/v1/admin/about` | AboutAdminController#update | ProfileCommandService#update | MediaAssetMapper#selectById、ProfileMapper#updateById、ProfileMapper#selectById（经 getAdmin） | 清晰 | R6-1 |
| 后台更新选中内容 | PUT `/api/v1/admin/about/selected-content` | AboutAdminController#updateSelectedContent | ProfileCommandService#updateSelectedContent | TutorialMapper#selectBatchIds、BlogPostMapper#selectBatchIds、PortfolioProjectMapper#selectBatchIds、`JdbcTemplate.update`（DELETE/INSERT） | Service 写 SQL + 跨模块 Mapper | R2-2、R2-3、R6-2 |
| 公开读取 About | GET `/api/v1/public/about` | AboutPublicController#get | ProfileQueryService#getPublic | ProfileMapper#selectById、`JdbcTemplate.query`、MediaAssetMapper#selectById | 持久化细节在 Service | R2-1、R2-2 |

### 2. 问题明细

| 编号 | 类型 | 位置 | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| R2-1 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\profile\service\ProfileQueryService.java:72` | `return jdbc.query("""`<br>`SELECT 'TUTORIAL' AS type, sc.tutorial_id AS content_id, t.title, t.slug,`<br>`t.publish_status, sc.sort_order` | Service 内手写跨表 UNION SQL，且**直接 JOIN `tutorial`/`blog_post`/`portfolio_project` 三张他模块的表**——既是持久化细节混入业务层，又是跨模块的数据边界穿透。 | 高 | 抽 `ProfileSelectedContentRepository`，并改为经各模块 Service/Port 取发布态。 |
| R2-2 | 2 | `...\profile\service\ProfileCommandService.java:83` | `jdbc.update("DELETE FROM profile_selected_content WHERE profile_id = 1");`<br>`int order = 10;`<br>`for (Long id : tutorials) {`<br>`jdbc.update("INSERT INTO profile_selected_content (profile_id, tutorial_id, sort_order) VALUES (1, ?, ?)", id, order);` | CommandService 用 `JdbcTemplate` 手写 DELETE/INSERT（83、86、91、96 行），持久化语句出现在业务层；同表另有 `ProfileSelectedContentMapper` 却未使用。 | 高 | 用 `ProfileSelectedContentMapper` 承担 CRUD，或抽 Repository。 |
| R2-3 | 2 | `...\profile\service\ProfileCommandService.java:79` | `validateExist(tutorials, tutorialMapper);`<br>`validateExist(blogs, blogPostMapper);`<br>`validateExist(projects, portfolioProjectMapper);` | 用泛型 `BaseMapper<?>` 跨模块校验存在性（141-149 行），直接依赖 tutorial/blog/portfolio 三个模块的 Mapper。 | 高 | 经各模块 Service/Port 暴露「是否存在/已发布」能力。 |
| R3-1 | 3 | `...\profile\mapper\ProfileSelectedContentMapper.java:8` | `public interface ProfileSelectedContentMapper extends BaseMapper<ProfileSelectedContent> {` | 该 Mapper 已定义但全工程仅此一处出现（grep 确认无注入点），属**死代码**；同时其对应表的读写被 ProfileCommandService 的裸 SQL 取代。 | 低 | 要么启用它替换裸 SQL，要么删除。 |
| R4-1 | 4 | `...\profile\service\ProfileQueryService.java:22` | `public class ProfileQueryService {` | 只读查询服务，无 `@Transactional(readOnly = true)`；其 `loadSelectedRows` 是跨表 JOIN，未声明只读事务。 | 中 | 加类级 `@Transactional(readOnly = true)`。 |
| R5-1 | 5 | `...\profile\service\ProfileQueryService.java:44` | `return new PublicAboutView(`<br>`profile.getDisplayName(), profile.getHeadline(), profile.getBio(),`<br>`mediaUrl(profile.getAvatarMediaId()), ...` | Entity→View 组装、发布态过滤（36 行 `if (!PUBLISHED.equals(...)) continue;`）内联在 QueryService，无 Assembler；`getPublic` 还内联了按 type 分流的业务规则。 | 中 | 抽 `AboutViewAssembler`。 |
| R6-1 | 6 | `...\profile\service\ProfileCommandService.java:8` | `import com.starrainnotes.media.mapper.MediaAssetMapper;` | profile 直接依赖 media Mapper（`ProfileCommandService:8/38`、`ProfileQueryService:5/27`）。 | 高 | 经 `MediaAssetPort` 访问。 |
| R6-2 | 6 | `...\profile\service\ProfileCommandService.java:5` | `import com.starrainnotes.blog.mapper.BlogPostMapper;`<br>`import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;`<br>`import com.starrainnotes.tutorial.mapper.TutorialMapper;` | 单文件跨模块直连 blog/portfolio/tutorial/media 四个模块的 Mapper，是四模块中最严重的边界穿透点。 | 高 | 逐一替换为对应模块 Service/Port 接口。 |
| R7-1 | 7 | `...\profile\dto\UpdateSelectedContentRequest.java:10` | `public record UpdateSelectedContentRequest(`<br>`List<Long> tutorialIds,`<br>`List<Long> blogPostIds,`<br>`List<Long> portfolioProjectIds) {` | 该请求 DTO **无任何 Bean Validation**（仅靠 Service 的 `dedupe` + 数量校验兜底），与约定「DTO 在 HTTP 边界用 Bean Validation」不符。 | 低 | 加 `@Size(max=3)` 等约束。 |
| R8-1 | 8 | `...\profile\service\ProfileCommandService.java:33` | `public class ProfileCommandService {` | 157 行，同时承担单例 About 写入与 selected-content 编排（含跨三模块存在性校验、裸 SQL），职责偏多但尚可接受。 | 低 | 拆 About 写入与 selected-content 两个用例服务。 |

### 3. 该模块统计

- 类数量：controller 2、service 2、mapper 2（其中 1 个死代码）、repository 0、assembler 0、entity 2、dto 5、package-info 1（合计 14 个 Java 文件）。
- Controller 直接调用 Mapper 次数：**0**。
- Controller 含 `@Transactional` 次数：**0**。
- Service 使用 `JdbcTemplate` 次数：**2**（`ProfileCommandService`、`ProfileQueryService`）。
- 跨模块依赖对方 Mapper 清单：`ProfileCommandService` → `media.mapper.MediaAssetMapper`、`blog.mapper.BlogPostMapper`、`portfolio.mapper.PortfolioProjectMapper`、`tutorial.mapper.TutorialMapper`；`ProfileQueryService` → `media.mapper.MediaAssetMapper`（另 `loadSelectedRows` 的 SQL 直接 JOIN 三张他模块表）。
- 行数最多的 5 个类：`ProfileCommandService` 157、`ProfileQueryService` 109、`ProfileSelectedContent` 83、`Profile` 40、`UpdateAboutRequest` 22。
- public 方法/成员最多的 5 个类：`ProfileSelectedContent` 15（getter/setter）、`ProfileCommandService` 3、`AboutAdminController` 4、`ProfileQueryService` 3、`AboutPublicController` 2。

### 4. 整体评价

profile 模块最突出的问题是**持久化与跨模块边界双越界**：QueryService 手写 UNION SQL 直接 JOIN tutorial/blog_post/portfolio_project，CommandService 用 `JdbcTemplate` 做 selected-content 的 DELETE/INSERT，并直连四个模块的 Mapper，而本模块自有的 `ProfileSelectedContentMapper` 沦为死代码。这与英语模块「不直接命名/查询他模块表、经 Port 访问」的基准完全相反。此外只读查询缺 `readOnly`、无 Assembler、请求 DTO 校验缺失。事务方面写方法已正确标注 `@Transactional`，是相对做得好的部分。

---

## 模块：site

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 后台仪表盘 | GET `/api/v1/admin/dashboard` | AdminSiteController#dashboard | DashboardQueryService#getDashboard | DashboardRepository#counts/recentContent | 清晰（唯一合规的 Repository 分层） | S5-1 |
| 后台读取站点设置 | GET `/api/v1/admin/site-settings` | AdminSiteController#getSiteSettings | SiteQueryService#getAdminSettings | SiteSettingMapper#selectById | 清晰 | — |
| 后台更新站点设置 | PUT `/api/v1/admin/site-settings` | AdminSiteController#updateSiteSettings | SiteCommandService#updateAdminSettings | MediaAssetMapper#selectById、SiteSettingMapper#selectById/updateById、SiteSettingMapper#selectById（经 getAdminSettings） | 清晰 | S6-1 |
| 公开站点配置 | GET `/api/v1/public/site` | PublicSiteController#site | SiteQueryService#getPublicSite | `JdbcTemplate.queryForMap`（JOIN media_asset） | Service 写 SQL | S2-1 |
| 公开首页数据 | GET `/api/v1/public/home` | PublicSiteController#home | SiteQueryService#getHome | `JdbcTemplate.query` ×3（publishedChapters/publishedBlogs/publishedPortfolio）、profile 表 | Service 写 SQL、跨模块 JOIN、胖服务 | S2-1、S2-2、S8-1 |

### 2. 问题明细

| 编号 | 类型 | 位置 | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| S2-1 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\site\service\SiteQueryService.java:52` | `Map<String, Object> row = jdbc.queryForMap("""`<br>`SELECT s.site_name, s.tagline, s.site_url, s.footer_text, s.github_url,`<br>`s.default_seo_description, s.timezone,`<br>`m_logo.public_url AS logo_url, m_fav.public_url AS favicon_url` | Service 内手写 SQL（52、81、100、112、133、150 共 6 处 `jdbc` 调用），持久化细节混入查询服务。 | 高 | 收拢到 `SiteSettingRepository`（可参照同模块 `DashboardRepository`）。 |
| S2-2 | 2 | `...\site\service\SiteQueryService.java:81` | `List<FeaturedProjectView> featured = jdbc.query("""`<br>`SELECT p.id, p.title, p.slug, p.summary, p.project_status,`<br>`m.public_url AS cover_url`<br>`FROM portfolio_project p` | site 的 Service 直接查询 portfolio 模块的表 `portfolio_project`、blog 表、tutorial 表，跨模块数据边界穿透（`publishedBlogs` 133、`publishedPortfolio` 150、`publishedChapters` 112）。 | 高 | 经各模块 Service/Port 取「最新/精选」数据。 |
| S4-1 | 4 | `...\site\service\SiteQueryService.java:39` | `public class SiteQueryService {` | 只读查询服务无 `@Transactional(readOnly = true)`。 | 中 | 加类级只读事务。 |
| S5-1 | 5 | `...\site\repository\DashboardRepository.java:11` | `@Repository`<br>`public class DashboardRepository {` | 同模块内 **mapper（`SiteSettingMapper`）与 repository（`DashboardRepository`）并存**，持久化风格不统一（对比 portfolio/profile/media 仅有 mapper）。 | 中 | 统一持久化命名/风格，或在模块内明确「简单单表用 Mapper、复杂只读模型用 Repository」的书面约定。 |
| S5-2 | 5 | `...\site\service\SiteQueryService.java:61` | `return new PublicSiteView(`<br>`str(row, "site_name"),`<br>`str(row, "tagline"), ...` | 响应组装（Map→View、时间格式化）内联在 Service，无 Assembler。 | 中 | 抽 `SiteViewAssembler`。 |
| S6-1 | 6 | `...\site\service\SiteCommandService.java:5` | `import com.starrainnotes.media.mapper.MediaAssetMapper;` | site 直接依赖 media Mapper 做 IMAGE 校验（63-74 行），重复了 portfolio/profile/media 各自也有的同款 `validateImageMedia` 逻辑。 | 高 | 经 `MediaAssetPort.requireImageIfPresent` 复用。 |
| S7-1 | 7 | `...\site\service\SiteCommandService.java:63` | `private void validateImageMedia(Long mediaId, String field) {` | IMAGE 类型校验在 portfolio（`PortfolioCommandService:204`）、profile（`ProfileCommandService:116`）、site（本处）三模块各写一份，重复校验。 | 中 | 统一走 media 的 Port。 |
| S8-1 | 8 | `...\site\service\SiteQueryService.java:39` | `public class SiteQueryService {` | 205 行，同时承担「公开站点配置 + 公开首页（最新/精选/关于预览）+ 后台站点设置」多个资源的读取，属上帝服务。 | 中 | 拆为 `PublicSiteQueryService` 与 `AdminSiteSettingQueryService`。 |

### 3. 该模块统计

- 类数量：controller 2、service 4（含 `SiteSettingsTimezone` 组件）、mapper 1、repository 1、assembler 0、entity 1、dto 10、package-info 1（合计 20 个 Java 文件）。
- Controller 直接调用 Mapper 次数：**0**。
- Controller 含 `@Transactional` 次数：**0**。
- Service 使用 `JdbcTemplate` 次数：**1**（`SiteQueryService`；另 `DashboardRepository` 亦用 `JdbcTemplate`，但位于 repository 层，合规）。
- 跨模块依赖对方 Mapper 清单：`SiteCommandService` → `media.mapper.MediaAssetMapper`。
- 跨模块依赖对方表（SQL 层）清单：`SiteQueryService` 的 SQL 直接读 `media_asset`、`portfolio_project`、`blog_post`、`tutorial`、`tutorial_node`、`profile`。
- 行数最多的 5 个类：`SiteQueryService` 205、`SiteSetting` 128、`SiteCommandService` 75、`DashboardRepository` 57、`AdminSiteController` 44。
- public 方法/成员最多的 5 个类：`SiteSetting` 25（getter/setter）、`SiteCommandService` 2、`DashboardQueryService` 2、`AdminSiteController` 4、`SiteSettingsTimezone` 5。

### 4. 整体评价

site 模块内部有一处亮点：`DashboardQueryService` + `DashboardRepository` 是四模块里唯一把 JDBC 读模型正确放到 repository 层的写法。但其余部分问题集中：`SiteQueryService` 既是上帝服务（公开配置+首页+后台设置）又手写 6 段 SQL，且跨模块直接查 portfolio/blog/tutorial/media 的表，与英语模块的 Port 基准冲突；同模块 mapper 与 repository 混用、缺只读事务、无 Assembler、IMAGE 校验与另外两模块重复。写操作 `updateAdminSettings` 已正确标注 `@Transactional`。

---

## 模块：media

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 媒体分页列表 | GET `/api/v1/admin/media-assets` | MediaAdminController#list | MediaQueryService#list | MediaAssetMapper#selectPage | 清晰 | — |
| 媒体分类统计 | GET `/api/v1/admin/media-assets/summary` | MediaAdminController#summary | MediaQueryService#summary | MediaAssetMapper#selectCount ×5 | 清晰 | — |
| 下载 ZIP 资源 | GET `/api/v1/admin/media-assets/{mediaId}/download` | MediaAdminController#download | MediaService#downloadArchive | MediaAssetMapper#selectById + 文件系统读取 | 清晰 | — |
| 上传媒体 | POST `/api/v1/admin/media-assets` | MediaAdminController#upload | MediaService#upload | MediaAssetMapper#insert、ImageVariantSupport（文件 IO）、PortfolioPrototypeService#validateArchive（ZIP 分支） | 胖服务 | M2-1、M4-1、M7-1 |
| 删除媒体 | DELETE `/api/v1/admin/media-assets/{mediaId}` | MediaAdminController#delete | MediaService#delete | MediaAssetMapper#selectById/deleteById、PortfolioPrototypeService#cleanupExtractedByMediaAssetId、文件删除 | 胖服务 + 跨模块 Service | M2-1、M6-1 |

补充（非路由，供跨模块调用）：
| 能力 | 入口 | 落点 | 说明 |
|---|---|---|---|
| 创建 ARCHIVE 资源 | MediaService#createArchiveAsset | MediaAssetMapper#insert/updateById + 私有目录落盘 | 被 portfolio 原型上传与 media 库 ZIP 复用 |
| 资源读取/校验 | MediaService#requireAsset、resolveStoragePath、srcSetOf | MediaAssetMapper#selectById、文件系统 | 被 portfolio 多处调用 |
| 媒体能力 Port | MediaAssetPort（isImage/isType/assetType/publicUrl/publicUrls/requireImageIfPresent） | MediaAssetMapper#selectCount/selectById/selectBatchIds | **合规的对外能力接口**，但本审计范围内 portfolio/profile/site 均未使用它 |

### 2. 问题明细

| 编号 | 类型 | 位置 | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| M2-1 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\media\service\MediaService.java:49` | `public class MediaService {` | 446 行、9 个 public 方法，单个类同时承担：扩展名/MIME/魔数校验、图片解码与尺寸读取、WebP 头解析调度、文件落盘与图片变体生成、DB 插入、删除补偿、下载资源解析、srcSet 构建。典型职责过载。 | 高 | 拆为 `MediaUploadService`、`MediaStorageService`、`MediaValidationPolicy`、`MediaViewAssembler`。 |
| M2-2 | 2 | `...\media\service\MediaService.java:111` | `public MediaAssetView upload(MultipartFile file) {` | Service 方法签名直接暴露 HTTP 层 `MultipartFile`；文件系统 IO（`Files.write`、`ImageVariantSupport.writeVariants`）混在业务服务内。 | 中 | 领域入参 + 存储组件抽象。 |
| M4-1 | 4 | `...\media\service\MediaService.java:261` | `try {`<br>`mapper.insert(asset);`<br>`asset.setPublicUrl("/api/v1/admin/media-assets/" + asset.getId() + "/download");`<br>`mapper.updateById(asset);`<br>`} catch (RuntimeException ex) {` | `createArchiveAsset` 连续执行 insert + updateById 两条语句但**无 `@Transactional`**；若 `updateById` 失败，catch 只删文件，**已插入的 DB 行会成为孤儿记录**。 | 高 | 加 `@Transactional`，或先算好 publicUrl 一次插入。 |
| M4-2 | 4 | `...\media\service\MediaService.java:111` | `public MediaAssetView upload(MultipartFile file) {` | `upload` 同样是「先写文件后写库」且无事务，仅靠 catch 手动删文件补偿（199-210 行），文件与 DB 不具备原子性。 | 中 | 明确一致性策略（先写库/补偿/临时目录）。 |
| M6-1 | 6 | `...\media\service\MediaService.java:7` | `import com.starrainnotes.portfolio.service.PortfolioPrototypeService;` | media 依赖 portfolio 的 Service（`validateArchive`、`cleanupExtractedByMediaAssetId`、`delete` 联动），而 portfolio 又依赖 media 的 Service/Mapper，构成 **media ↔ portfolio 循环依赖**（`MediaService` 构造函数用 `@Lazy`，93 行）。 | 高 | 抽 `PrototypeCleanupPort` 由 portfolio 实现，media 只依赖接口。 |
| M6-2 | 6 | `...\media\service\MediaService.java:93` | `@Lazy PortfolioPrototypeService prototypeService,` | `@Lazy` 是对循环依赖的**技术性规避**而非解耦，说明模块边界确实存在环。 | 中 | 见 M6-1。 |
| M7-1 | 7 | `...\media\service\MediaService.java:231` | `if (bytes.length < 2 || bytes[0] != 'P' || bytes[1] != 'K') {` | ZIP 魔数/体积校验在此与 `PortfolioPrototypeService.validateArchive`（109 行）、`bind`（125 行）三处重复；`MediaService.upload` 的 archive 分支先 `prototypeService.validateArchive(bytes)` 再 `createArchiveAsset` 又各自校验一遍。 | 中 | 收敛为单一 `PrototypeArchivePolicy`。 |
| M8-1 | 8 | `...\media\service\ImageVariantSupport.java:32` | `public final class ImageVariantSupport {` | 154 行纯静态工具类，承担图片缩放/JPEG 编码/变体路径与 srcSet 生成；放在 `service` 包但实为图像处理工具，与业务服务混层。 | 低 | 移到 `media.image` 支撑包，作为独立基础设施组件。 |

补充说明（第 3 类 Mapper 越界排查结论）：`MediaAssetMapper` 本身为空 `BaseMapper`，无 default/静态业务方法、无 Mapper 互调、未被 Controller 直接调用；但**被 site/portfolio/profile 三个模块直接注入**，且其 Javadoc 明确写着「used for IMAGE-type validation and public URL resolution by the site module」（`MediaAssetMapper.java:8-9`），等于把「被跨模块使用」固化进注释——问题在于调用方未走 `MediaAssetPort`，而非 Mapper 自身越界。

### 3. 该模块统计

- 类数量：controller 1、service 4（`MediaService`、`MediaQueryService`、`ImageVariantSupport`、`WebpHeaderReader`）、mapper 1、repository 0、assembler 0、api/Port 1（`MediaAssetPort`）、entity 1、dto 3、config 1、package-info 1（合计 13 个 Java 文件）。
- Controller 直接调用 Mapper 次数：**0**。
- Controller 含 `@Transactional` 次数：**0**。
- Service 使用 `JdbcTemplate` 次数：**0**（media 全部走 MyBatis-Plus，是四模块中 SQL 边界最干净的一个）。
- 跨模块依赖对方 Service 清单：`MediaService` → `portfolio.service.PortfolioPrototypeService`（`validateArchive` / `cleanupExtractedByMediaAssetId` / `delete`）；`MediaService`、`MediaQueryService`、`PortfolioQueryService` → `site.service.SiteSettingsTimezone`。
- 对外被依赖情况：`MediaAssetPort`（合规接口）与 `MediaService`（portfolio 直接依赖其 Service）并存。
- 行数最多的 5 个类：`MediaService` 446、`ImageVariantSupport` 154、`WebpHeaderReader` 73、`MediaAdminController` 72、`MediaQueryService` 55。
- public 方法/成员最多的 5 个类：`MediaService` 9、`MediaAssetPort` 8、`ImageVariantSupport` 7、`MediaAdminController` 6、`MediaQueryService` 3。

### 4. 整体评价

media 是四模块中 SQL 边界最干净的（Service 层零 `JdbcTemplate`，全部 MyBatis-Plus），并且已提供合规的 `MediaAssetPort` 对外能力接口——但该 Port 在本次审计的 portfolio/profile/site 中**一处都没用上**，三个模块反而直连 `MediaAssetMapper`/`MediaService`，使 Port 形同虚设。核心问题是 `MediaService` 胖类（446 行）把校验、图像处理、文件 IO、DB、下载混在一起，`createArchiveAsset` 的 insert+update 缺事务会产生孤儿记录，以及与 portfolio 的跨模块循环依赖（用 `@Lazy` 掩盖）。`MediaQueryService` 干净，可作正例。

---

## 跨模块汇总与最严重问题

1. **跨模块 Mapper 直连（类型 6，高）**：portfolio（2 处）、profile（4 处 + 1 处裸 SQL JOIN 他模块表）、site（1 处 + 6 段 SQL 直读他模块表）都绕过 `MediaAssetPort` 及各模块 Service 直连 Mapper/表，与英语模块「通过 `MediaPort`/`MediaPortAdapter` 解耦」的既定基准直接冲突。
2. **事务边界缺失（类型 4，高）**：`PortfolioCommandService.delete/publish/withdraw` 三个写操作缺 `@Transactional`；`PortfolioPrototypeService.bind`、`MediaService.upload/createArchiveAsset` 的文件落盘与 DB 写入无原子性，`createArchiveAsset` 的 insert+update 失败会残留孤儿行。
3. **Service 越界写 SQL（类型 2，高）**：`ProfileQueryService`、`ProfileCommandService`、`SiteQueryService` 共 3 个服务直接用 `JdbcTemplate` 手写 SQL，其中 profile 的 UNION 与 site 的 6 段查询还跨模块 JOIN 他模块表。
4. **胖类（类型 8，高）**：`PortfolioPrototypeService`（454 行/12 方法）、`MediaService`（446 行/9 方法）职责过载；`SiteQueryService`（205 行）为上帝服务。
5. **正向对照**：`DashboardQueryService` + `DashboardRepository`（site）是唯一正确的「Service 编排 + Repository 持久化」范例；`MediaQueryService`、四个模块的 Controller（0 处 Mapper 直连、0 处 `@Transactional`、无 SQL）与全部 Mapper 均为干净分层；`MediaAssetPort` 已具备，只差被调用。
6. **命名/风格不一致（类型 5，中）**：site 内 mapper 与 repository 混用；四模块均无 Assembler，组装散落在 QueryService。
7. **校验（类型 7，中/低）**：IMAGE 类型校验在 portfolio/profile/site 重复三份；ZIP 魔数/体积校验在 media 与 portfolio 重复三处；`UpdateSelectedContentRequest` 无 Bean Validation。


---

> **附录 E** — account / auth / common / search / seo

# account / auth / common / search / seo 分层审计

审计范围：`backend/src/main/java/com/starrainnotes/{account,auth,common,search,seo}`
审计基准：`docs/english-backend-architecture.md`、`docs/english-v2-final-boundary-audit.md`
审计方式：逐文件实读（本报告所有行号与代码片段均来自实读，未运行测试）。无法确认之处标注「待确认」。

---

## 模块：account

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 账号登录（会话建立） | POST `/api/v1/auth/account/login` | `AccountAuthController#login` | `AccountCredentialService#authenticate`、`AccountService#capabilities`、`AuditLogService#record` | `AccountUserMapper#selectOne/updateById`、`AuditLogMapper#insert` | 否（Controller 落会话） | A-03 |
| 修改密码（当前账号） | PUT `/api/v1/auth/account/password` | `AccountAuthController#changePassword` | `AccountCredentialService#changePassword`、`AuditLogService#record` | `AccountUserMapper#selectOne/updateById` | 基本清晰 | A-03 |
| 超管激活状态 | GET `/api/v1/auth/super-admin-activation/status` | `AccountActivationController#status` | `AccountQueryService#configuredSuperAdminEmail`、`#superAdminActivated` | `AccountUserMapper#selectCount` | 否（规则在 Controller） | A-09 |
| 发送激活验证码 | POST `/api/v1/auth/super-admin-activation/verification-codes` | `AccountActivationController#sendCode` | `AccountActivationService#requestCode` → `VerificationCodeService#issue` | `AccountUserMapper#selectOne/insert`、`EmailVerificationChallengeMapper#selectList/updateById/insert` | 部分（事务缺失） | A-08、A-17 |
| 确认超管激活 | POST `/api/v1/auth/super-admin-activation/confirm` | `AccountActivationController#confirm` | `AccountActivationService#confirm` → `VerificationCodeService#verify` | `AccountUserMapper#selectOne/updateById`、`EmailVerificationChallengeMapper#selectList/updateById` | 是 | — |
| 邀请状态查询 | GET `/api/v1/auth/invitations/{token}` | `AccountInvitationController#status` | `AccountInvitationService#byToken` | `AdminInvitationMapper#selectOne/updateById` | 否（Controller 组装/格式化） | A-12 |
| 发送注册验证码 | POST `/api/v1/auth/invitations/{token}/verification-codes` | `AccountInvitationController#sendCode` | `AccountInvitationService#requestCode` → `VerificationCodeService#issue` | `AdminInvitationMapper#selectOne`、`EmailVerificationChallengeMapper#*` | 部分（无事务） | A-08 |
| 邀请注册管理员 | POST `/api/v1/auth/invitations/{token}/register` | `AccountInvitationController#register` | `AccountInvitationService#register` → `VerificationCodeService#verify` | `AdminInvitationMapper#selectOne/updateById`、`AccountUserMapper#insert` | 是 | — |
| 请求重置验证码 | POST `/api/v1/auth/password-reset/verification-codes` | `PasswordResetController#sendCode` | `AccountCredentialService#requestPasswordReset` → `VerificationCodeService#issue` | `AccountUserMapper#selectOne`、`EmailVerificationChallengeMapper#*` | 是 | — |
| 确认重置密码 | POST `/api/v1/auth/password-reset/confirm` | `PasswordResetController#confirm` | `AccountCredentialService#confirmPasswordReset` → `VerificationCodeService#verify` | `AccountUserMapper#selectOne/updateById`、`EmailVerificationChallengeMapper#*` | 是 | — |
| 用户列表 | GET `/api/v1/super-admin/users` | `SuperAdminAccountController#users` | `AccountQueryService#listUsers` | `AccountUserMapper#selectList` | 是 | A-06 |
| 邀请列表 | GET `/api/v1/super-admin/invitations` | `SuperAdminAccountController#invitations` | `AccountInvitationService#list` | `AdminInvitationMapper#selectList/updateById` | 是 | — |
| 创建邀请 | POST `/api/v1/super-admin/invitations` | `SuperAdminAccountController#invite` | `AccountInvitationService#create` | `AdminInvitationMapper#insert`、`AccountUserMapper#selectOne` | 是 | A-14、A-18 |
| 重发邀请 | POST `/api/v1/super-admin/invitations/{id}/resend` | `SuperAdminAccountController#resend` | `AccountInvitationService#resend` | `AdminInvitationMapper#selectById/updateById` | 是 | A-18 |
| 撤销邀请 | POST `/api/v1/super-admin/invitations/{id}/revoke` | `SuperAdminAccountController#revoke` | `AccountInvitationService#revoke` | `AdminInvitationMapper#selectById/updateById` | 是 | — |
| 删除邀请 | DELETE `/api/v1/super-admin/invitations/{id}` | `SuperAdminAccountController#deleteInvitation` | `AccountInvitationService#delete` | `AdminInvitationMapper#selectById/deleteById` | 是 | — |
| 停用账号 | POST `/api/v1/super-admin/users/{id}/disable` | `SuperAdminAccountController#disable` | `AccountAdministrationService#disable` | `AccountUserMapper#selectById/updateById` | 否（默认值在 Controller） | A-10 |
| 启用账号 | POST `/api/v1/super-admin/users/{id}/enable` | `SuperAdminAccountController#enable` | `AccountAdministrationService#enable` | `AccountUserMapper#selectById/updateById` | 是 | — |
| 审计日志分页 | GET `/api/v1/super-admin/audit-logs` | `SuperAdminAccountController#auditLogs` | `AuditLogService#list` | `AuditLogMapper#selectList` | 否（直接返回 Entity） | A-11 |
| 内容审核列表 | GET `/api/v1/super-admin/content-reviews` | `ContentReviewController#list` | `ContentReviewService#list` | JdbcTemplate 直查 `content_review_request` | 否（Service 写 SQL） | A-01、A-07 |
| 审核通过 | POST `/api/v1/super-admin/content-reviews/{id}/approve` | `ContentReviewController#approve` | `ContentReviewService#approve` → 8 个模块 CommandService | JdbcTemplate 直查/更新 `content_review_request` | 否（上帝类） | A-01、A-02 |
| 审核驳回 | POST `/api/v1/super-admin/content-reviews/{id}/reject` | `ContentReviewController#reject` | `ContentReviewService#reject` | JdbcTemplate 直查/更新 `content_review_request` | 否（上帝类） | A-01、A-02 |
| 学习记录分页 | GET `/api/v1/account/english/learning/records` | `AccountEnglishController#records` | `EnglishLearningFacade#records` | （english 模块内部） | 是 | A-16 |
| 学习记录批量 | GET `/api/v1/account/english/learning/records/batch` | `AccountEnglishController#batchRecords` | `EnglishLearningFacade#batchRecords` | （english 模块内部） | 是 | — |
| 学习汇总 | GET `/api/v1/account/english/learning/summary` | `AccountEnglishController#summary` | `EnglishLearningFacade#summary` | （english 模块内部） | 是 | — |
| 学习洞察 | GET `/api/v1/account/english/learning/insights` | `AccountEnglishController#insights` | `EnglishLearningFacade#insights` | （english 模块内部） | 是 | — |
| 单条学习记录 | GET `/api/v1/account/english/learning/records/{type}/{contentId}` | `AccountEnglishController#record` | `EnglishLearningFacade#record` | （english 模块内部） | 是 | — |
| 保存学习记录 | PUT `/api/v1/account/english/learning/records/{type}/{contentId}` | `AccountEnglishController#putRecord` | `EnglishLearningFacade#saveRecord` | （english 模块内部） | 否（Map 入参） | A-15 |
| 词汇记忆快照 | GET `/api/v1/account/english/vocabulary/memory` | `AccountEnglishController#vocabularyMemory` | `EnglishVocabularyFacade#memorySnapshot` | （english 模块内部） | 是 | — |
| 写入记忆次数 | PUT `/api/v1/account/english/vocabulary/words/{wordId}/memory` | `AccountEnglishController#putMemory` | `EnglishVocabularyFacade#putMemoryCount` | （english 模块内部） | 否（Map 入参） | A-15 |
| 词汇设置读取 | GET `/api/v1/account/english/vocabulary/settings` | `AccountEnglishController#vocabularySettings` | `EnglishVocabularyFacade#settings` | （english 模块内部） | 是 | — |
| 词汇设置更新 | PUT `/api/v1/account/english/vocabulary/settings` | `AccountEnglishController#updateVocabularySettings` | `EnglishVocabularyFacade#updateSettings` | （english 模块内部） | 是 | — |
| 词汇状态查询 | GET `/api/v1/account/english/vocabulary/states` | `AccountEnglishController#vocabularyStates` | `EnglishVocabularyFacade#memories` | （english 模块内部） | 是 | — |
| 复习队列 | GET `/api/v1/account/english/vocabulary/review-queue` | `AccountEnglishController#vocabularyReviewQueue` | `EnglishVocabularyFacade#queue` | （english 模块内部） | 是 | — |
| 开始学习单词 | POST `/api/v1/account/english/vocabulary/words/{wordId}/start` | `AccountEnglishController#startVocabularyWord` | `EnglishVocabularyFacade#start` | （english 模块内部） | 是 | — |
| 完成单词复习 | POST `/api/v1/account/english/vocabulary/words/{wordId}/reviews` | `AccountEnglishController#reviewVocabularyWord` | `EnglishVocabularyFacade#completeReview` | （english 模块内部） | 是 | — |
| 重置单词进度 | DELETE `/api/v1/account/english/vocabulary/words/{wordId}/progress` | `AccountEnglishController#resetVocabularyWord` | `EnglishVocabularyFacade#reset` | （english 模块内部） | 是 | — |
| 设置展示信息 | PUT `/api/v1/account/english/vocabulary/words/{wordId}/display` | `AccountEnglishController#setVocabularyDisplay` | `EnglishVocabularyFacade#setDisplay` | （english 模块内部） | 是 | — |
| 清除展示信息 | DELETE `/api/v1/account/english/vocabulary/words/{wordId}/display` | `AccountEnglishController#clearVocabularyDisplay` | `EnglishVocabularyFacade#clearDisplay` | （english 模块内部） | 是 | — |
| 词汇统计 | GET `/api/v1/account/english/vocabulary/statistics` | `AccountEnglishController#vocabularyStatistics` | `EnglishVocabularyFacade#progress` | （english 模块内部） | 是 | — |
| 本地词汇导入 | POST `/api/v1/account/english/vocabulary/import-local` | `AccountEnglishController#importLocalVocabulary` | `EnglishVocabularyFacade#importLocal` | （english 模块内部） | 否（Map 入参） | A-15 |
| 写作提交读取 | GET `/api/v1/account/english/writing-submissions/{promptId}` | `AccountEnglishController#writingSubmission` | `EnglishLearningFacade#writingSubmission` | （english 模块内部） | 是 | — |
| 写作提交保存 | PUT `/api/v1/account/english/writing-submissions/{promptId}` | `AccountEnglishController#saveWritingSubmission` | `EnglishLearningFacade#saveWritingSubmission` | （english 模块内部） | 是 | — |
| 本地进度导入 | POST `/api/v1/account/english/import-local-progress` | `AccountEnglishController#importLocal` | `EnglishLearningFacade#importLocalProgress` | （english 模块内部） | 否（Map 入参） | A-15 |
| 认领旧版进度 | POST `/api/v1/account/english/claim-legacy-progress` | `AccountEnglishController#claimLegacy` | `EnglishLearningFacade#claimLegacyProgress` | （english 模块内部） | 否（Map 入参） | A-15 |
| 内部用例（非 HTTP）：博客更新送审 | 无路由（由 blog 模块调用） | — | `ContentReviewService#submitBlogUpdate` | JdbcTemplate 直写 `content_review_request` | 否 | A-01、A-21 |
| 内部用例：英文内容送审 | 无路由（由 english 各 Admin Controller 调用） | — | `ContentReviewService#submitEnglishUpdate` | JdbcTemplate 直写 `content_review_request` | 否 | A-01、A-21 |
| 内部用例：教程章节送审 | 无路由（由 tutorial 模块调用） | — | `ContentReviewService#submitTutorialChapterUpdate` | JdbcTemplate 直写 `content_review_request` | 否 | A-01、A-21 |
| 内部用例：发布状态查询 | 无路由（由 english Admin Controller 调用） | — | `ContentReviewService#isPublished` → `EnglishReviewContentPort#isPublished` | `EnglishContentStateRepository#state` | 是（符合 Port 基准） | — |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| A-01 | 2 | `e:\star-rain\backend\src\main\java\com\starrainnotes\account\review\service\ContentReviewService.java:41,87-88` | `private final JdbcTemplate jdbc;` … `return jdbc.query("SELECT * FROM content_review_request" + where + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?", this::map, args);` | Service 直接持有 `JdbcTemplate` 并手写 SQL（含 WHERE 字符串拼接、`LAST_INSERT_ID()`、`UTC_TIMESTAMP(6)`），违反“Service 不含 SQL、Mapper 只负责持久化”。与 `english-v2-final-boundary-audit.md` 声明的「Controller/Service 无 SQL」基准相冲突（该基准只覆盖 english 模块）。 | 高 | 抽 `ContentReviewRepository`（MyBatis-Plus 或 XML），Service 只做编排 |
| A-02 | 2、8 | `...\account\review\service\ContentReviewService.java:39-78,160-193` | `private final BlogCommandService blogService;` … `grammarService/readingService/listeningService/pronunciationRuleCommands/writingResourceService/writingPromptService`；`if ("BLOG_POST".equals(...)) ... else if ("ENGLISH_GRAMMAR_LESSON"...` 8 段分支 | 上帝类：同时编排 blog/tutorial/english 六个子域 + 自管审核表持久化；254 行、9 个 public 方法；审批用长 if-else 链按 content_type 分发。 | 高 | 审核持久化下沉 Repository；审批分发改为注册式 Handler/Port |
| A-03 | 1 | `...\account\controller\AccountAuthController.java:59-68` | `SecurityContext context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context); securityContextRepository.saveContext(context, request, response); request.changeSessionId();` | Controller 直接承担认证结果落库、会话固定保护（changeSessionId）、SecurityContext 管理——属于认证/会话职责，而非 HTTP 参数绑定。与 `auth\controller\AuthController#login` 形成两套并行实现（同一职责重复）。 | 高 | 下沉到 `AccountAuthenticationService`（或与 auth 统一），Controller 只调用一次 |
| A-04 | 4 | `...\account\service\AccountCredentialService.java:33-68` | `userMapper.updateById(user);`（失败计数、`lockedUntil`、`lastLoginAt` 均在同一方法内多次写） | `authenticate` 是写路径（更新失败次数/锁定/最后登录）却**无 `@Transactional`**，多字段更新非原子。 | 中 | 加 `@Transactional`（或拆只读校验 + 写记账） |
| A-05 | 4 | `...\account\service\VerificationCodeService.java:72-95` | `challenge.setAttemptCount(challenge.getAttemptCount() + 1); mapper.updateById(challenge);` | `verify` 写 attemptCount/consumedAt 却**无 `@Transactional`**；并发下尝试次数自增与消费不原子。 | 中 | 加 `@Transactional` |
| A-06 | 4 | `...\account\service\AccountQueryService.java:21-45` | `Long count = userMapper.selectCount(...)` / `userMapper.selectList(...)` / `userMapper.selectOne(...)` | 三个只读查询均未声明 `@Transactional(readOnly = true)`。 | 低 | 补 `readOnly = true` |
| A-07 | 4 | `...\account\review\service\ContentReviewService.java:80-89` | `public List<ContentReviewView> list(String status, int page, int pageSize) {` | 只读列表未声明只读事务（且同类写方法均为可写事务，风格不一致）。 | 低 | 补 `readOnly = true` |
| A-08 | 4 | `...\account\service\AccountActivationService.java:37-51` | `ensurePendingSuperAdmin(email); codeService.issue(email, "SUPER_ADMIN_ACTIVATION", null, ip);` | `requestCode` 内含 insert（`ensurePendingSuperAdmin`）+ 验证码写库，但方法**无 `@Transactional`**；`AccountInvitationService#requestCode:94-97` 同类问题。 | 中 | 加 `@Transactional` |
| A-09 | 1 | `...\account\controller\AccountActivationController.java:32-35` | `boolean configured = email != null && !email.isBlank() && AccountService.validEmail(email);` … `AccountService.mask(email)` | Controller 内做邮箱有效性业务规则判断与脱敏视图字段计算。 | 低 | 移入 `AccountQueryService`/View 工厂 |
| A-10 | 1 | `...\account\controller\SuperAdminAccountController.java:89-90` | `String reason = body == null \|\| body.reason() == null \|\| body.reason().isBlank() ? null : body.reason();` | Controller 内做业务默认值/清洗。 | 低 | 移入 `AccountAdministrationService#disable` |
| A-11 | 1、5 | `...\account\controller\SuperAdminAccountController.java:99-104` | `public List<AuditLog> auditLogs(...) { return auditLogService.list(page, pageSize, action); }` | Controller 直接对外返回持久化 Entity `AuditLog`（含 `actorId` 等内部字段），缺少 View/DTO 层。 | 中 | 返回 `AuditLogView` |
| A-12 | 1 | `...\account\controller\AccountInvitationController.java:36-38` | `return new InvitationStatusView(AccountService.mask(inv.getEmail()), inv.getStatus(), timezone.atSite(inv.getExpiresAt()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));` | Controller 组装视图并做时区格式化（Assembler 缺失，组装散落）。 | 低 | 抽 `InvitationViewAssembler` |
| A-13 | 2、5 | `...\account\service\AccountService.java:26-71` | `public List<String> capabilities(String role)` + `private void requireSuperAdmin(...)` + `public static String normalize/validEmail/mask(...)` + `public static ApiException fail(...)` | 单一类混合：授权规则、静态工具（邮箱规范化/脱敏/异常工厂）、Mapper 访问；与 `AccountQueryService`（同读 `AccountUserMapper`）职责重叠，account 存在 10 个 Service 的拆分过度/重叠。 | 中 | 抽 `AccountPolicy`（授权）、`AccountEmails`（工具）；合并 `AccountService`/`AccountQueryService` |
| A-14 | 2、5 | `...\account\service\AccountInvitationService.java:186-194` 与 `...\account\service\AccountService.java:34-39` | `private void requireSuperAdmin(Long accountId, String detail) { AccountUser user = userMapper.selectById(accountId); ... }` | 超管校验规则在两处独立实现，易漂移（且 `AccountService.requireSuperAdminOr401`、`AccountInvitationService.create` 各有一份）。 | 中 | 统一到 `AccountPolicy` |
| A-15 | 7 | `...\account\controller\AccountEnglishController.java:79-83,93-95,155-156,173-179` | `@RequestBody Map<String, Object> body` … `body.get("timeSpentSeconds") == null ? null : ((Number) body.get("timeSpentSeconds")).intValue()` | 请求体用裸 `Map`，无 DTO 与 Bean Validation，手工取值+强转，校验缺失。 | 中 | 定义请求 DTO 并加 `@Valid` |
| A-16 | 2、8 | `...\account\controller\AccountEnglishController.java:33-180` | `private final EnglishLearningFacade english; private final EnglishVocabularyFacade vocabulary;`（23 个路由方法） | 胖 Controller：181 行、25 个 public 方法，横跨 learning/vocabulary/writing 三类资源。 | 中 | 按资源拆分为 2-3 个 Controller |
| A-17 | 2 | `...\account\service\VerificationCodeService.java:69` | `mailGateway.sendVerificationCode(email, purpose, code);`（位于 `@Transactional public void issue(...)` 内） | 外部副作用（SMTP 发送）发生在事务内，SMTP 超时（5000ms）会拉长事务/占用连接。 | 中 | 改为 `AFTER_COMMIT` 事件或事务外发送 |
| A-18 | 2 | `...\account\service\AccountInvitationService.java:68` | `mailGateway.sendInvitationLink(invitation.getEmail(), link);`（位于 `@Transactional create()` 内；`resend:138` 同） | 同上：事务内发送邮件。 | 中 | 同上 |
| A-19 | 8 | `...\account\dto\InvitationActionRequest.java:5` | `public record InvitationActionRequest(@NotNull Long invitationId) { }` | 全仓库无任何引用（grep 仅命中定义处），死代码空壳。 | 低 | 删除 |
| A-20 | 2 | `...\account\service\AccountSessionService.java:12-17` | `Authentication authentication=SecurityContextHolder.getContext().getAuthentication();` | Service 直接读取安全上下文（web 关注点渗入服务层），仅被 `AccountEnglishController` 用于取 accountId。 | 低 | Controller 用 `@AuthenticationPrincipal AccountPrincipal` 传入 id |
| A-21 | 5、6 | `...\account\review\service\ContentReviewService.java:111-134` 与 `...\english\writing\controller\WritingAdminController.java:88-93` | `if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_WRITING_RESOURCE", id)) { ContentReviewView review = reviewService.submitEnglishUpdate(...); }` | account 的审核 DTO/服务被 english、blog、tutorial 的 Controller/Service 直接注入，且“是否送审”的分支判断落在各 Admin Controller（Controller 越界）；跨模块 DTO 泄漏。 | 中 | 提供 `ReviewSubmissionPort` 与统一 Workflow 服务，移除 Controller 分支 |

### 3. 该模块统计

- 类数量：controller **7**、service **11 类 + 1 接口**（AccountService、AccountQueryService、AccountActivationService、AccountAdministrationService、AccountCredentialService、AccountInvitationService、AccountSessionService、VerificationCodeService、AuditLogService、ContentReviewService、SmtpMailGateway；接口 MailGateway）、mapper **4**、repository **0**、assembler **0**（View record 内置 `from()`）、entity **4**（AccountUser、AdminInvitation、EmailVerificationChallenge、AuditLog）、dto **17**（account/dto 15 + review/dto 2）、config 1、security 2。
- Controller 直接调用 Mapper 次数：**0**；Controller 直接调用 Repository 次数：**0**；Controller 含 `@Transactional` 次数：**0**；Service 使用 `JdbcTemplate` 次数：**1**（ContentReviewService）。
- 跨模块依赖对方 Mapper/表清单：
  - 无（account 未直接引用其他模块的 Mapper）。
  - 依赖其他模块 Service：`SiteSettingsTimezone`（site，AccountInvitationController、ContentReviewService）、`EnglishReviewContentPort`（english，符合基准）、blog/tutorial/english 各 CommandService（ContentReviewService）。
  - 被其他模块反向依赖：blog/tutorial/english 的 Controller/WorkflowService 直接注入 `ContentReviewService` 与 `ContentReviewView`（A-21）。
- 行数最多 5 个类：ContentReviewService **254**、AccountInvitationService **232**、AccountEnglishController **181**、VerificationCodeService **157**、AccountCredentialService **116**。
- public 方法最多 5 个类：AccountEnglishController **25**、AccountService **14**、AccountInvitationService **11**、SuperAdminAccountController **11**、ContentReviewService **9**。

### 4. 整体评价

account 的用户/邀请/验证码/审计四类用例分层基本到位：Controller 无 SQL、无 Mapper、无事务注解，DTO 基本带 Bean Validation。主要问题集中在两点：其一，`ContentReviewService` 是明确的越界点——Service 里写 SQL 并跨 8 个模块编排，是上帝类兼持久化实现；其二，`AccountAuthController` 把认证结果落库、会话固定保护、SecurityContext 管理搬进 Controller，与 `auth/AuthController` 重复。另有 2 处写路径缺 `@Transactional`（authenticate、verify）、2 处事务内发邮件、10 个 Service 存在授权规则重复与职责重叠，整体可用但边界不够干净。

---

## 模块：auth

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 获取 CSRF 令牌 | GET `/api/v1/auth/csrf` | `AuthController#csrf` | 无（直接 `CsrfTokenRepository#loadToken/generateToken/saveToken`） | 无 | 否（Controller 管 CSRF） | B-01 |
| 当前会话视图 | GET `/api/v1/auth/session` | `AuthController#session` | `AuthService#currentSessionView` | 无 | 是 | — |
| V1 管理员登录 | POST `/api/v1/auth/login` | `AuthController#login` | `AuthService#recordLastLogin`（+ `AuthenticationManager` 直接调用） | `AdminUserMapper#selectOne/updateById` | 否（认证+会话+CSRF 轮换在 Controller） | B-02 |
| 登出 | POST `/api/v1/auth/logout` | `AuthController#logout` | `AuthService#logout` | 无 | 部分（Service 收 HttpServletRequest） | B-03 |
| 修改管理员密码 | PUT `/api/v1/auth/password` | `AuthController#changePassword` | `AuthService#changePassword` | `AdminUserMapper#selectOne/updateById` | 部分（Service 收 HttpServletRequest） | B-03 |
| 初始化状态 | GET `/api/v1/setup/status` | `SetupController#status` | `SetupService#isSetupRequired` | `AdminUserMapper#selectCount` | 是 | — |
| 创建首个管理员 | POST `/api/v1/setup/admin` | `SetupController#createAdmin` | `SetupService#createAdmin` | `AdminUserMapper#selectCount/insert` | 部分（开关判断在 Controller） | B-04 |
| （安全链）加载管理员 | 无 HTTP（`UserDetailsService`） | — | `AdminUserDetailsService#loadUserByUsername` | `AdminUserMapper#selectOne` | 是 | B-06 |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| B-01 | 1 | `e:\star-rain\backend\src\main\java\com\starrainnotes\auth\controller\AuthController.java:62-68` | `CsrfToken token = csrfTokenRepository.loadToken(request); if (token == null) { token = csrfTokenRepository.generateToken(request); csrfTokenRepository.saveToken(token, request, response); }` | Controller 直接操作 CSRF 令牌仓库（生成/保存），属安全基础设施职责而非 HTTP 绑定。 | 中 | 抽 `CsrfService` 或交由 `CsrfTokenRepository` 的 `DeferredCsrfToken` 机制 |
| B-02 | 1 | `...\auth\controller\AuthController.java:84-94` | `Authentication authentication = authenticationManager.authenticate(...); SecurityContextHolder.setContext(context); securityContextRepository.saveContext(context, httpRequest, httpResponse); httpRequest.changeSessionId(); rotateCsrf(...);` | Controller 承担认证、会话建立、会话固定保护、CSRF 轮换——认证/会话职责集中在 Controller；与 `AccountAuthController#login` 重复（A-03）。 | 高 | 抽 `AuthenticationService`（登录用例），两个入口共用 |
| B-03 | 2 | `...\auth\service\AuthService.java:64-83` | `public void logout(HttpServletRequest request)` / `public void changePassword(String currentPassword, String newPassword, HttpServletRequest request)` | Service 引用 `HttpServletRequest`/`HttpSession` 并执行会话失效，web 关注点渗入服务层（`logout` 还被 `changePassword` 自调用）。 | 中 | 会话失效抽 `SessionInvalidator` 接口，或由 Controller 在服务返回后处理 |
| B-04 | 1 | `...\auth\controller\SetupController.java:39-58` | `assertEnabled();`（两个端点各调用一次） | 遗留开关（`legacySetupEnabled`）的业务判断落在 Controller；`createAdmin` 返回 void，状态码/视图在 Controller 拼装（可接受但边界偏薄）。 | 低 | 开关判断移入 `SetupService` |
| B-05 | 5 | `...\auth\dto\AuthSessionView.java:19-27` 与 `...\account\service\AccountService.java:26-32` | `if ("ROLE_SUPER_ADMIN".equals(role) \|\| "SUPER_ADMIN".equals(role)) { return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE", "SUPER_ADMIN", "REVIEW"); }` | 权限/能力清单在两处硬编码（auth 的 `AuthSessionView` 与 account 的 `AccountService#capabilities`），双份维护，易不一致。 | 中 | 抽公共 `Capabilities` 组件 |
| B-06 | 5、8 | `...\auth\security\AdminUserDetailsService.java:31-34` | `.roles("SUPER_ADMIN")` | V1 管理员角色硬编码在 `UserDetailsService`，与 account 的角色体系（`AccountPrincipal`）并存，两套角色来源。 | 低 | 待确认（V1 冻结设计，`04-api-design.md §7`） |

### 3. 该模块统计

- 类数量：controller **2**、service **2**、security **1**、mapper **1**、repository **0**、assembler **0**、entity **1**、dto **7**。
- Controller 直接调用 Mapper 次数：**0**；Controller 含 `@Transactional` 次数：**0**；Service 使用 `JdbcTemplate` 次数：**0**。
- 跨模块依赖对方 Mapper/表清单：auth 自身无跨模块表访问；但 `auth.security.AdminUserDetailsService` 被 `common.security.SecurityConfig` 装配（common→auth 反向依赖）。
- 行数最多 5 个类：AuthController **117**、AuthService **85**、SetupService **75**、AdminUser **75**、SetupController **60**。
- public 方法最多 5 个类：AdminUser **13**、AuthController **7**、AuthService **6**、SetupController **4**、SetupService **4**。

### 4. 整体评价

auth 是最小的 V1 冻结切片，Service 无 SQL、无 JdbcTemplate，DTO 校验齐全，整体干净。核心问题是职责错位：`AuthController` 一手包办认证、SecurityContext 落库、会话固定保护与 CSRF 轮换（B-02/B-01），与 account 的登录实现重复；`AuthService` 反过来又持有 `HttpServletRequest` 做会话失效（B-03），形成「Controller 干服务的活、Service 干 web 的活」的双向越界。另有能力清单与 account 重复硬编码。建议以「登录/会话」为一个显式用例服务收敛。

---

## 模块：common

### 1. 业务逻辑清单

common 不含 Controller，无对外业务路由。承载的内容为横切基础设施：

| 功能 | 载体 | 依赖 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|
| 安全链装配（CSRF/Session/授权矩阵/401-403） | `common\security\SecurityConfig.java` | 直接 import `auth.security.AdminUserDetailsService`、`account.security.AccountSessionValidationFilter`、`account.service.AccountQueryService` | 否（反向依赖业务模块） | C-01、C-02 |
| MyBatis-Plus 分页拦截 | `common\config\MybatisPlusConfig.java` | 无 | 是 | — |
| 匿名公共 GET 缓存头 | `common\web\PublicApiCacheHeadersFilter.java` | 无 | 部分 | C-03 |
| 全局异常 → RFC 9457 | `common\error\GlobalExceptionHandler.java` | `ApiException`/`ApiProblem` | 是 | — |
| 401/403 渲染 | `RestAuthenticationEntryPoint`、`RestAccessDeniedHandler` | `ApiProblem` | 是 | — |
| 数字 slug 生成 | `common\slug\NumericSlugGenerator.java` | `ApiException` | 是 | — |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| C-01 | 6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\common\security\SecurityConfig.java:3-5,95-105` | `import com.starrainnotes.auth.security.AdminUserDetailsService;` `import com.starrainnotes.account.security.AccountSessionValidationFilter;` `import com.starrainnotes.account.service.AccountQueryService;` … `accountSessionValidationFilter(AccountQueryService accountQueries)` | 横切基础设施层反向依赖 auth/account 业务模块，common 不再“纯粹”；新增业务模块的认证都要改 common。属依赖方向问题（非循环，但耦合方向不利）。 | 中 | 将安全装配移到 `app`/`bootstrap` 包，或以 `SecurityCustomizer` 接口由各模块贡献 |
| C-02 | 1（设计一致性） | `...\common\security\SecurityConfig.java:116-154` | `.requestMatchers("/api/v1/super-admin/**").hasRole("SUPER_ADMIN")` … `.requestMatchers(new AntPathRequestMatcher("/api/v1/admin/**", "DELETE")).hasRole("SUPER_ADMIN")` | 授权矩阵全部内嵌在配置类，与项目中 `@AuthenticationPrincipal`/服务内 `requireSuperAdmin` 的授权风格并存，出现三处授权来源（配置、Service、Controller）。 | 低 | 待确认（V1 冻结设计）；建议统一到一处声明 |
| C-03 | 5 | `...\common\web\PublicApiCacheHeadersFilter.java:38-39` | `if (path.contains("/account") \|\| path.contains("/learning") \|\| path.contains("/vocabulary/memory") \|\| path.contains("/vocabulary/pronunciation")) { return; }` | 用 `contains` 字符串判断敏感路径，脆弱（任何含 `/account` 的公共路径都会被跳过缓存，语义不清）。 | 低 | 用精确/前缀匹配或正则 |

### 3. 该模块统计

- 类数量：config **2**、error **3**、security **2**、web **1**、slug **1**（共 9 个类 + `package-info`）；无 controller/service/mapper/repository/entity/dto。
- Controller 直接调用 Mapper 次数：**0**；Controller 含 `@Transactional` 次数：**0**；Service 使用 `JdbcTemplate` 次数：**0**。
- 跨模块依赖对方 Mapper/表清单：common 无表访问；但 `SecurityConfig` 依赖 auth/account 的类（见 C-01）。
- 行数最多 5 个类：SecurityConfig **170**、GlobalExceptionHandler **114**、RestAccessDeniedHandler **49**、NumericSlugGenerator **48**、PublicApiCacheHeadersFilter **48**。
- public 方法最多 5 个类：GlobalExceptionHandler **11**、SecurityConfig **8**、NumericSlugGenerator **4**、RestAccessDeniedHandler **3**、RestAuthenticationEntryPoint **3**。

### 4. 整体评价

common 的错误处理与工具类职责单一、质量较高：`GlobalExceptionHandler`、`ApiProblem`、`NumericSlugGenerator` 都符合横切定位，未出现 SQL 或业务规则。唯一实质问题是依赖方向——`SecurityConfig` 作为基础设施却直接 import auth/account 的服务与过滤器，使 common 反向耦合业务；这虽在单体中常见，但与“common 只做横切”的约定相悖。另有授权矩阵散落（配置 + Service + Controller 三处）和缓存过滤器的字符串路径判断两个小瑕疵。

---

## 模块：search

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| 全局公开搜索 | GET `/api/v1/public/search` | `SearchPublicController#search` | `SearchService#search` | `SearchRepository#tutorials`、`#chapters`、`#blogs`、`#portfolios`、`#grammar`、`#reading`、`#listeningMaterials`、`#pronunciationRules`、`#writing`、`#vocabulary` | 否（Repository 跨模块读表） | D-01、D-02、D-03 |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| D-01 | 3、6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\search\repository\SearchRepository.java:19-125` | `jdbc.query("SELECT id, title, summary, slug, updated_at FROM tutorial WHERE publish_status = 'PUBLISHED' AND (title LIKE ? OR summary LIKE ?)", ...)`；`... FROM english_grammar_lesson l JOIN english_grammar_course c ...`；`... FROM vocabulary_word w JOIN vocabulary_theme t ...` | Repository 用 `JdbcTemplate` 直接跨模块读取 13 张表（tutorial、tutorial_node、blog_post、portfolio_project、english_grammar_*、english_reading_article、english_listening_item、english_listening_pronunciation_rule、english_writing_resource、english_writing_prompt、vocabulary_word、vocabulary_theme），绕过各模块 Service/Port，与 `english-v2-final-boundary-audit.md` 的 Port 解耦基准相悖。 | 高 | 各模块提供搜索投影 Port（如 `SearchIndexPort`），search 只聚合 |
| D-02 | 3 | `...\search\repository\SearchRepository.java:119-123` | `String summary = themeName == null \|\| themeName.isBlank() ? translation : translation + " · " + themeName;` | 持久化层做展示字段拼装（“翻译 · 主题名”），属展示/业务逻辑。 | 中 | 移入 `SearchService`/Assembler |
| D-03 | 3 | `...\search\repository\SearchRepository.java:95-110` | `List<SearchDocument> documents = jdbc.query(... english_writing_resource ...); documents.addAll(jdbc.query(... english_writing_prompt ... CONCAT(background_markdown,'\n',requirements_markdown) ...)); return documents;` | 单个 Repository 方法承担“多资源聚合 + 列拼接”职责，跨资源聚合越界。 | 中 | 拆分按资源查询，聚合上移到 Service |
| D-04 | 4 | `...\search\service\SearchService.java:32-66` | `public SearchPageView search(String rawQuery, String type, int page, int pageSize) {` | 只读查询未声明 `@Transactional(readOnly = true)`。 | 低 | 补 `readOnly = true` |
| D-05 | 2 | `...\search\service\SearchService.java:68-83` | `if (allTypes \|\| "tutorial".equals(type)) { result.addAll(tutorials); result.addAll(chapters); }` … `"word".equals(type)` | 业务规则在 Service 中（位置正确），但 `type` 用魔法字符串 if-else 链，且“listening 归并 pronunciation”的规则与 counts 计算重复表达。 | 低 | 用枚举 + 映射表 |
| D-06 | 8 | `...\search\repository\SearchRepository.java:127-130` | `private static SearchDocument document(String type, Long id, ...) { return new SearchDocument(type, id, title, ...); }` | 纯转发构造的空壳方法（无逻辑）。 | 低 | 直接 `new SearchDocument(...)` |

### 3. 该模块统计

- 类数量：controller **1**、service **1**、repository **2**（`SearchRepository` + 投影 record `SearchDocument`）、dto **3**、mapper **0**、assembler **0**、entity **0**。
- Controller 直接调用 Mapper 次数：**0**；Controller 含 `@Transactional` 次数：**0**；Service 使用 `JdbcTemplate` 次数：**0**（SQL 在 Repository）。
- 跨模块依赖对方 Mapper/表清单：`SearchRepository` 直接读 13 张他模块表（tutorial、tutorial_node、blog_post、portfolio_project、english_grammar_lesson、english_grammar_course、english_reading_article、english_listening_item、english_listening_pronunciation_rule、english_writing_resource、english_writing_prompt、vocabulary_word、vocabulary_theme）；未引用他模块 Mapper/Entity。
- 行数最多 5 个类：SearchRepository **131**、SearchService **118**、SearchPublicController **29**、SearchItemView **17**、SearchCountsView/SearchPageView **16**。
- public 方法最多 5 个类：SearchRepository **12**、SearchService **2**、SearchPublicController **2**、其余 record 各 1。

### 4. 整体评价

search 的三层骨架（Controller→Service→Repository）清晰，Controller 只做参数绑定、Service 承担校验/评分/排序/分页，SQL 集中在 Repository，没有 Controller 越界或事务问题。核心缺陷是 Repository 的**跨模块表访问**：它绕过各业务模块直接 JOIN 13 张表，正是 `english-v2-final-boundary-audit.md` 所要消除的模式；另外 Repository 内混入了展示字段拼装与多资源聚合，只读事务也缺失。建议以各模块搜索投影 Port 收敛。

---

## 模块：seo

### 1. 业务逻辑清单

| 业务功能 | HTTP 方法+路由 | Controller 方法 | 调用的 Service#方法 | 涉及的 Mapper/Repository#方法 | 职责是否清晰 | 问题编号 |
|---|---|---|---|---|---|---|
| SEO 页面预渲染（首页/教程/博客/作品/英语/关于/搜索等 11 个入口 + `{*path}` 动态页） | GET `/`、`/tutorials`、`/tutorials/{*path}`、`/blog`、`/blog/{*path}`、`/portfolio`、`/portfolio/{*path}`、`/english`、`/english/{*path}`、`/about`、`/search` | `SeoController#page` | 无 Service（直接 `SeoDocumentCache#page` → `SeoContentRepository#resolve` → `SeoHtmlRenderer#render`） | `SeoContentRepository#resolve/home/listing/article/...`（JdbcTemplate） | 否（Controller→Repository，跳层） | E-01、E-07、E-08 |
| sitemap.xml | GET `/sitemap.xml` | `SeoController#sitemap` | `SeoSitemapService#sitemap` → `#buildSitemap` | `SeoSitemapRepository#publishedEntries` | 部分（Repository 跨模块读表） | E-06、E-11 |
| robots.txt | GET `/robots.txt` | `SeoController#robots` | 无（读 `SeoProperties`） | 无 | 是 | — |
| IndexNow key 文件 | GET `/indexnow-key.txt` | `SeoController#indexNowKey` | 无（读 `SeoProperties`） | 无 | 是 | — |
| 内容变更 → 缓存失效 | 无 HTTP（`@SeoContentChange` 切面） | — | `SeoContentChangeAspect#publishAfterChange` → 发布 `SeoContentChangedEvent` | `JdbcTemplate` 直查业务表（tutorial/blog_post/portfolio_project） | 否（跨模块读表） | E-04、E-05 |
| 站点/个人资料变更 → 失效 | 无 HTTP（切面 execution 表达式） | — | `SeoContentChangeAspect#identityChanged` | 无 | 否（硬编码他模块类名） | E-05 |
| 英文内容变更 → 失效 | 无 HTTP（`@EventListener`） | — | `EnglishContentSeoListener#contentChanged` | 无 | 是 | — |
| 文档缓存失效 | 无 HTTP（`@TransactionalEventListener`） | — | `SeoDocumentCache#contentChanged`、`SeoContentRepository#contentChanged`、`SeoSitemapService#contentChanged` | 无 | 部分（Repository 监听事件） | E-02 |
| 搜索索引主动通知（IndexNow/百度） | 无 HTTP（`@Async` 事件） | — | `SeoNotificationService#changed` → `submitIndexNow/submitBaidu` | 无 | 部分（外部 IO 在业务包内） | E-10 |

### 2. 问题明细

| 编号 | 类型(1-8) | 位置(文件绝对路径:行号) | 证据代码 | 问题说明 | 严重度 | 建议 |
|---|---|---|---|---|---|---|
| E-01 | 3、6 | `e:\star-rain\backend\src\main\java\com\starrainnotes\seo\SeoContentRepository.java:29,35,50-58,103,117-125,158,163,174` | `jdbc.query("SELECT site_name,tagline,default_seo_description,github_url FROM site_setting WHERE id=1", ...)`；`"SELECT title,slug,summary,updated_at FROM blog_post WHERE publish_status='PUBLISHED' ..."`；`"... FROM english_writing_prompt p LEFT JOIN media_asset m ON m.id=p.cover_media_id ..."` | `@Repository` 用 `JdbcTemplate` 直接跨模块读取 site_setting、profile、tutorial、tutorial_category、tutorial_node、media_asset、blog_post、portfolio_project、english_grammar_*、english_reading_article、english_listening_item、english_listening_pronunciation_rule、english_writing_resource、english_writing_prompt、english_learning_bundle 共 16 张他模块表，绕过各模块 Service/Port。 | 高 | 各模块暴露 SEO 内容 Port（标题/摘要/正文/封面/发布时间），seo 只组装 |
| E-02 | 5、2 | `...\seo\SeoContentRepository.java:38-43` | `@org.springframework.transaction.event.TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true) @Order(-100) public void contentChanged(SeoContentChangedEvent ignored) { identity.clear(); author.clear(); }` | 持久化组件（`@Repository`）上挂事务事件监听，承担缓存编排职责，层次语义混乱（同类职责在 `SeoDocumentCache`、`SeoSitemapService` 各有一份）。 | 中 | 抽独立 `SeoCacheInvalidationListener` |
| E-03 | 8、3 | `...\seo\SeoContentRepository.java:45-82,191-222` | `public SeoPage resolve(String rawPath) { ... switch (path) { case "/" -> home(); case "/tutorials" -> listing(...); ... default -> dynamic(path); } }` + `cards()/links()/esc()` HTML 拼装 | 226 行胖类，同时承担路由解析（12+12 分支）、多表 SQL、HTML 片段拼装、缓存；`resolve` 一方法内嵌 12 个页面分支。 | 高 | 按页面类型拆 Handler（首页/列表/文章/项目/写作…），渲染交 `SeoHtmlRenderer` |
| E-04 | 3、6 | `...\seo\SeoContentChangeAspect.java:37-41` | `private ContentState state(String table, Long id) { if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Unsafe SEO table"); return jdbc.query("SELECT slug,publish_status FROM " + table + " WHERE id=?", ...); }` | Aspect 用 `JdbcTemplate` 直接查业务表，表名由注解传入并拼接进 SQL；跨模块读数据（`tutorial`/`blog_post`/`portfolio_project`）。 | 高 | 由各模块 Port 提供「内容 slug + 发布状态」，Aspect 不碰 SQL |
| E-05 | 6 | `...\seo\SeoContentChangeAspect.java:43-49` | `@Around("execution(* com.starrainnotes.site.service.SiteCommandService.updateAdminSettings(..)) \|\| execution(* com.starrainnotes.profile.service.ProfileCommandService.update(..)) \|\| execution(* com.starrainnotes.profile.service.ProfileCommandService.updateSelectedContent(..))")` | 切面用 execution 表达式硬编码 site/profile 的具体类与方法名，跨模块强耦合（改名即失效，编译期无保护）。 | 中 | 改为注解标记或领域事件 |
| E-06 | 3、6 | `...\seo\repository\SeoSitemapRepository.java:19-33` | `add(entries, "SELECT CONCAT('/tutorials/',slug),updated_at FROM tutorial WHERE publish_status='PUBLISHED'");` … 共 11 条跨模块 SQL | Repository 用 `JdbcTemplate` 拼接 11 条跨模块查询（tutorial、tutorial_node、blog_post、portfolio_project、english_grammar_*、english_reading_article、english_listening_item、english_listening_pronunciation_rule、english_writing_resource、english_writing_prompt、english_learning_bundle），越界承担其他模块的数据读取。 | 高 | 通过各模块 Port 汇总 sitemap 条目 |
| E-07 | 1、3 | `...\seo\SeoController.java:23,34` | `private final SeoContentRepository content;` … `SeoPage page = cache.page(path, () -> content.resolve(path));` | Controller 直接依赖并调用 Repository，跳过 Service 层（无 `SeoPageService`）。 | 中 | 增加 `SeoPageService` 作为用例边界 |
| E-08 | 1 | `...\seo\SeoController.java:35-38` | `if (page == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)...; if (!page.indexable()) response.header("X-Robots-Tag", page.robots().replace(",", ", "));` | Controller 内做“是否可索引”的业务分支与响应头拼装。 | 低 | 上移到 Service/渲染器 |
| E-09 | 2、3 | `...\seo\SeoHtmlRenderer.java:28,36,101-107,128-149` | `private final SeoContentRepository content;` … `String siteName = content.site().name();` … `Files.getLastModifiedTime(path).toMillis()` / `Files.readString(path, StandardCharsets.UTF_8)` | 渲染组件反向依赖 Repository 读取站点/作者信息；同时直接做文件系统 IO（读前端 `index.html` shell）。渲染器应只依赖数据入参或窄接口。 | 中 | 站点/作者信息通过 Port 或入参注入；shell 加载抽 `ShellProvider` |
| E-10 | 2 | `...\seo\SeoNotificationService.java:34-53` | `@Async @TransactionalEventListener(...) public void changed(SeoContentChangedEvent event) { ... submitIndexNow(...); submitBaidu(...); }` 内部 `http.send(request, ...)` | 外部副作用（HTTP 通知第三方）以 `@Service` 形式与领域组件同包，缺少独立的 infrastructure/adapter 包；功能上隔离尚可。 | 低 | 迁至 `seo/infrastructure`，接口化便于测试 |
| E-11 | 4 | `...\seo\SeoSitemapService.java:27-43` | `public String sitemap() { return cache.get("sitemap", this::buildSitemap); }`（`buildSitemap` 内调 `repository.publishedEntries()`） | 只读查询路径未声明 `@Transactional(readOnly = true)`。 | 低 | 补 `readOnly = true` |
| E-12 | 3 | `...\seo\SeoContentRepository.java:117-155` | `String sql = "SELECT x.title,x.summary," + bodyColumn + " body,..." + cover + " image FROM " + table + " x" + coverJoin + " WHERE x.slug=? AND x.publish_status='PUBLISHED'";` / `"SELECT title,summary,published_at,updated_at FROM " + table + " WHERE slug=? ..."` | 持久化层用参数拼表名/列名生成动态 SQL（当前入参为内部常量，风险可控，但违背“SQL 应静态可审计”的原则）。 | 中 | 用枚举 → 固定 SQL/XML 映射 |

### 3. 该模块统计

- 类数量：controller **1**、service **2**（SeoSitemapService、SeoNotificationService）、repository **2**（SeoContentRepository、SeoSitemapRepository）、aspect **1**、listener **1**、renderer **2**、cache **2**（SeoDocumentCache、BoundedSeoCache）、config **2**（SeoAsyncConfig、SeoProperties）、record/annotation **4**（SeoPage、SeoBreadcrumb、SeoContentChangedEvent、SeoContentChange）；mapper **0**、entity **0**、dto **0**、assembler **0**。
- Controller 直接调用 Mapper 次数：**0**；Controller 直接调用 Repository 次数：**1**（`SeoController`→`SeoContentRepository`）；Controller 含 `@Transactional` 次数：**0**；Service 使用 `JdbcTemplate` 次数：**0**（但 `SeoContentChangeAspect` 作为组件使用 JdbcTemplate **1** 处，见 E-04）。
- 跨模块依赖对方 Mapper/表清单：
  - `SeoContentRepository` → 16 张他模块表（见 E-01）。
  - `SeoSitemapRepository` → 11 张他模块表（见 E-06）。
  - `SeoContentChangeAspect` → `tutorial`、`blog_post`、`portfolio_project`（E-04），并以 execution 表达式依赖 `site.service.SiteCommandService`、`profile.service.ProfileCommandService`（E-05）。
  - `EnglishContentSeoListener` → 仅依赖 english 的 `EnglishContentChangedEvent`（事件解耦，合规）。
- 行数最多 5 个类：SeoContentRepository **226**、SeoHtmlRenderer **161**、SeoNotificationService **76**、SeoController **67**、SeoSitemapService/SeoContentChangeAspect **52**。
- public 方法最多 5 个类：SeoContentRepository **7**、SeoController **6**、SeoSitemapService **4**、SeoContentChangeAspect **4**、SeoNotificationService **3**。

### 4. 整体评价

seo 是本次审计中跨模块耦合最重的模块。`SeoContentRepository` 与 `SeoSitemapRepository` 用 `JdbcTemplate` 直读 16/11 张他模块表，`SeoContentChangeAspect` 更以注解表名拼 SQL 且用 execution 表达式硬绑 site/profile 类名——三处都绕过了 `english-v2-final-boundary-audit.md` 所确立的 Port 解耦基准。`SeoContentRepository`（226 行）同时承担路由解析、SQL、HTML 拼装与缓存，是典型胖类；`SeoController` 还直接依赖 Repository 跳过了 Service。唯一合规的是 `EnglishContentSeoListener`（纯事件）。建议以「各模块内容 Port + seo 内部 Handler/Service 分层」重排。

---

## 跨模块汇总

- **最严重 3 个问题**
  1. `account/review/service/ContentReviewService`（A-01/A-02）：Service 内写 SQL + 跨 8 个模块编排，上帝类兼持久化实现，254 行。
  2. `seo` 三处跨模块直读表（E-01/E-04/E-06）：`SeoContentRepository`、`SeoContentChangeAspect`、`SeoSitemapRepository` 用 `JdbcTemplate` 直查 16/11/3 张他模块表，违反既有 Port 边界基准。
  3. `auth/AuthController` 与 `account/AccountAuthController`（B-02/A-03）：Controller 承担认证、SecurityContext 落库、会话固定保护与 CSRF 轮换，且两套并行实现。
- **统计口径（本 5 模块）**：Controller 直接调用 Mapper **0** 次；Controller 直接调用 Repository **1** 次；Controller 含 `@Transactional` **0** 次；Service 使用 `JdbcTemplate` **1** 处（ContentReviewService）+ 组件 1 处（SeoContentChangeAspect）。
- **事务边界缺口**：account `AccountCredentialService#authenticate`（A-04）、`VerificationCodeService#verify`（A-05）、`AccountActivationService#requestCode`/`AccountInvitationService#requestCode`（A-08）缺 `@Transactional`；account/search/seo 共 4 处只读查询缺 `readOnly = true`（A-06/A-07/D-04/E-11）。
- **依赖方向**：common→auth/account（C-01）；search/seo→各业务模块表（D-01/E-01/E-04/E-06）；seo→site/profile 具体类（E-05）。未发现循环依赖。
- **合规亮点**：`EnglishReviewContentPort` / `DefaultEnglishReviewContentPort` 与 `EnglishContentSeoListener` 符合既定边界基准；`GlobalExceptionHandler`、`ApiProblem`、`NumericSlugGenerator`、`MailGateway`/`SmtpMailGateway` 抽象（接口 + SMTP 实现）设计合理。


---
