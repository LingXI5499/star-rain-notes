# 星雨笔录 V2 · 单词学习与高强度复习开发及验收

交付日期：2026-10-08。交付分支：`codex/vocabulary-intensive-learning`，基于已经远程交付的作品分支 `codex/portfolio-works-v11`。依据用户提供的《英语单词学习与高强度复习系统开发设计文档》实现，仅涉及英语词汇。

## 1. 完成的行为

- 主题词库每页 24 词，搜索、已学、四档熟练度、当前计划范围、方向与最近评价均由服务器筛选。支持跨页手选、筛选全选和排除、当前页反选、会话草稿保留。
- 创建计划之前先读取预览：总数、来源、已学及未学数量、首尾示例、分组数量和待选分页。最终确认需要勾选确认项，存在旧计划时显示替换警告；预览不会写入记忆或评分。
- 每账户只有一个计划槽位，含修订号。确认使用全量选择及词条内容指纹，集合或内容变化、旧修订号均返回 409。替换或取消只修改计划及明细。
- 默认 20 词一组，可设置 5～100 词。双语预览后按英译中、中译英、听音辨词训练，每轮分别记录完成位；默认入口由最早未完成组、方向、单词推导。允许临时跳组、切换方向和在预览中跳词。调整组大小保留已经开始的组，仅重分后续词。
- 计划词条保存内容快照。后台删词后可以看到失效状态并跳过，跳过不伪造评分；主题改名不改变已确认的计划来源快照。
- 真实评分有忘记、模糊、掌握三种；三个方向分别保存 EMA、评分次数、阶段和下一次复习。综合分采用三方向均值及前三次评分的置信度；满足跨方向、跨自然日和听音条件才能毕业。
- 普通间隔集中定义为 5 分钟至 28 天，毕业后固定 35 天。毕业后忘记回到 5 分钟，模糊回到 1 小时，并退出长期巩固。过期不会自动推进阶段。
- 今日背单词只读取已建立方向记忆且到期的卡片，每批 20/30/50 张；显示方向卡数、不同词数、跨日积压、今日评分及最早到期时间，不混入未评分新词。
- 听音首屏没有英文、中文、音标或泄题的可访问属性，训练时隐藏组内单词列表。上传音频、代理发音、浏览器合成依次回退；只有实际播放事件才允许揭晓和评分。失败不记成绩。
- 评分以账户和 UUID 幂等，完整比较词、方向、评价、来源、计划修订和播放标记。重放返回原结果快照，重复提交不再加次数。网络失败的前端重试保留原 UUID 和请求内容。

时间表与 EMA 是产品经验参数，不宣称是人人适用的遗忘曲线或语言能力测量。

## 2. 与当前仓库对接的选择

仓库实际使用 Spring Boot / MyBatis 和 `/api/account/english/vocabulary`，因此沿用这些实现与路由，不引入文档旧基线里的 JdbcTemplate 或 `/api/v1`。所有账户身份来自 `CurrentActorApi`，忽略客户端提供的账户编号。

新增 `/learning/words`、`/learning/states`、计划的预览/确认/分组/调整/取消/失效词处理、`/review-summary`。既有 `/review-queue` 和 `/words/{wordId}/reviews` 切换到新流程。旧的删除个人进度入口返回 409，提示通过新计划重新训练。

旧总次数、日期和日志保留，历史记录显示“历史记录无评分”。旧 ACTIVE 到期档案迁入英译中校准方向，评分次数及 EMA 为零；不补造过去的熟练度或评分。旧 MIXED 设置映射为英译中，旧每日配额字段保留兼容，不限制新计划规模。

正式计划与自评要求登录。游客继续浏览、预览词库和播放发音；原 IndexedDB 数据、显示偏好、导出及显式登录合并保留。没有继续在游客数据库里运行新评分流程，因而不建立新本地 store 或清库升级。登录不自动上传游客记录；显式合并按既有身份边界执行，旧记录不成为新评分。账号访问只有 401 才回退游客状态，网络错误不会被当作登出。

## 3. SQL 迁移及事务

新增 `V2_034__vocabulary_intensive_learning.sql`，不改写已经发布的旧 Flyway 脚本：

| 表 | 影响 |
| --- | --- |
| `sr_english_vocabulary_memory` | 增加综合分、境界、真实评分时间、最新评分及版本；原计数不重置 |
| `sr_english_vocabulary_review_log` | 增加评分、来源、计划修订、音频标记、新旧阶段/分数及原结果快照；旧行保留 |
| `sr_english_vocabulary_mode_memory` | 每账户、单词、方向唯一；到期索引覆盖账户、时间、词、方向 |
| `sr_english_vocabulary_rating_day` | 自然日覆盖记录，重复同日不增加天数 |
| `sr_english_vocabulary_plan` | 每账户一个槽位，取消后保留 NONE 修订号防过时确认 |
| `sr_english_vocabulary_plan_item` | 稳定顺序、分组、完成位、开始时间、内容快照 |

计划确认和评分分别在单一事务中完成，以账户计划槽位作为命令锁，再锁记忆档案、检查评分日志。评分与计划替换采用同一锁顺序。万词计划只扩展编号和指纹，内容每次 500 词批量读取、插入；前端不提交整词库 JSON。

时间保存为 UTC，JSON 日期带 Z；自然日按 Asia/Shanghai 计算。

## 4. 改动文件范围

| 范围 | 文件 |
| --- | --- |
| 后端核心 | `star-rain-english/.../learning/VocabularyLearningModels.java`、`VocabularyLearningService.java`、`VocabularyMasteryCalculator.java`、`VocabularyHighIntensityReviewPolicy.java`、`VocabularyLearningConfig.java` |
| 账户 API / SQL | `VocabularyLearningController.java`、`VocabularyStudyAccountController.java`、`VocabularyLearningMapper.java`、`VocabularyLearningMapper.xml` |
| 旧数据兼容 | `VocabularyStudyConstants.java`、`ReviewDirection.java`、`VocabularyStudySettingsRequestDTO.java`、`VocabularyStudyMapper.java/xml`、`VocabularyProgressImportServiceImpl.java`、`VocabularyStudyCommandServiceImpl.java`、`VocabularyStudyQueryServiceImpl.java` |
| 历史显示 | `VocabularyReviewHistoryRow.java`、`VocabularyReviewHistoryVO.java`、`VocabularyMemoryAssembler.java` |
| 数据库 | `star-rain-boot/.../V2_034__vocabulary_intensive_learning.sql` |
| 前端页面 | `VocabularyThemePage.vue`、`VocabularyPlanConfirmPage.vue`、`VocabularyPlanPage.vue`、`VocabularyStudyPage.vue`、`VocabularyPage.vue`、`VocabularyProgressPage.vue` |
| 前端组件及接口 | `VocabularyRecallCard.vue`、`VocabularyCard.vue`、`vocabularyRecallAudio.js`、`vocabularyLearningApi.js`、`englishApi.js`、`vocabularyDisplay.js`、`vocabularyStudyStorage.js` |
| 路由 | `frontend/src/router/index.js` 仅新增两条单词计划路由 |
| 测试 | `VocabularyLearningAlgorithmsTest`、`VocabularyLearningServiceTest`、`VocabularyLearningHttpTest`、`VocabularyIntensiveLearningIntegrationTest`；旧默认方向测试更新；前端 RecallCard / PlanPage / LearningApi / RecallAudio 四组测试 |

博客、教程、站点等其他未提交改动未归入本次交付。

## 5. 实际执行的验证

后端相关单元和接口测试共 60 项通过。实际 MySQL 独立库集成测试 9 项通过，Mapper XML 解析检查 4 项通过，没有数据库跳过项。

```powershell
# backend 目录
mvn -pl star-rain-boot -am '-Dtest=Vocabulary*Test,MapperXmlIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -q
```

独立测试库采用经过固定前缀校验的 `sr_vocab_probe_*` 名称，测试建立旧结构和旧数据再运行新迁移；结束回滚并删除仅该测试创建的库。未对现有应用库运行 V2_034，也未启动应用上下文或触发任务。

覆盖：45 词分成 20/20/5、预览零写入、旧次数和无评分日志、三方向独立排期、组内三轮默认入口、替换和取消保留记忆、过时修订、筛选全选排除、词条变化指纹冲突、同日刷分限制、跨日毕业、35 天巩固及失败降级、UUID 原结果重放、事务回滚、已开始组冻结、删词快照和无评分跳过、不同方向并发及相同 UUID 并发重放。

规模测试建立 10,000 词计划和 30,000 方向记忆。每次计划读取 20 词、复习读取 20 卡；下一游标批次不重叠。实际到期查询包含词条存在检查，EXPLAIN 使用 `idx_sr_vocab_mode_due`，没有 filesort。该测试证明此数据量的功能和索引使用，不代表生产并发吞吐或延迟 SLA。

前端全套已有及新增交互测试 120 项通过，另新增实际播放事件测试 3 项通过；生产构建通过。

```powershell
# frontend 目录
npm test -- --run
npm test -- --run src/modules/english/lib/vocabularyRecallAudio.spec.js
npm run build
```

前端验证包含双语预览无自评、英译中和中译英遮题、听音 DOM/属性不泄题、播放失败无法揭晓或评分、成功播放才能评分、跨页选择和大编号字符串、筛选全选只保留排除项、草稿恢复、默认入口、网络评分重试保留 UUID。实际播放事件测试还检查仅 play Promise 成功不记播放、切词取消不回退旧任务、全链路失败返回未播放。

日志保存在本机 `.local/vocabulary-*.log`；临时验证文件、数据库凭据和构建产物不提交。

收尾还从 Git 暂存区导出独立源码副本，排除了工作区中尚未提交的博客、教程等改动。该交付副本后端共 73 项通过（词汇单元及接口 60、MySQL 集成 9、Mapper XML 解析 4），无失败或跳过；前端 17 个测试文件、64 项通过，生产构建通过。工作区的 120 项全套测试包含其他模块尚未交付的测试，因此与交付副本数量不同。独立验证日志为 `.local/vocabulary-release-backend.log`、`vocabulary-release-frontend.log`、`vocabulary-release-build.log`。

## 6. 可操作验收路径

在已完成迁移的 V2 环境登录后：

1. 打开 `/english/vocabulary` → 任一主题；应用个人筛选，跨页手选或选择全部匹配词，再排除几个词。
2. 点击查看待选；核对总数、首尾示例、每组词数、已学数量。取消或返回词库不会修改当前计划或记忆。
3. 勾选确认项并创建；已有计划时检查替换警告。进入 `/english/vocabulary/plan`，最初显示双语预览，点击开始默认训练。
4. 英译中揭晓后自评，再进行中译英及听音。听音播放失败时不允许评价，成功后才可揭晓。完成一组三轮后继续下一组。
5. 临时跳后面的组后刷新，默认入口仍回到最早待完成组。调整后续组大小，已开始组成员不变。
6. 打开 `/english/vocabulary/study`：只有到期方向卡，批次可切换 20/30/50，没有到期时引导继续计划。
7. 取消或换新计划后重新查看今日复习和 `/english/vocabulary/progress`，已评分次数及日志保留。
8. 未登录浏览词库，确认旧本机显示偏好与记录可读；正式建计划提示登录。登录合并由用户显式触发。

## 7. 上线与交付边界

代码与迁移交付到开发分支，尚未合并 main，也未部署到生产。上述交互由组件和接口测试验证；本次没有宣称完成新流程的浏览器人工端到端验收。

上线前备份当前库，核对 Flyway 历史与本地尚未交付的教程 V2_027～V2_031。作品分支已有 V2_032/033，不能让已迁移高版本的数据库遗漏后来合入的低版本脚本。应先统一迁移序列，再在备份副本验证正常启动和 Flyway 校验；不要盲目开启 outOfOrder 或改写已执行脚本。

新评分产生后不能用删新表方式回滚，否则丢失真实记录。回退时先停评分写入、保留完整备份和新增表，再确定应用兼容或恢复方案。
