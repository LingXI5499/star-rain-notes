# V2 英语域：V1 对照基线

英语域在 V2 尚无正式 Schema / Use Case 规范。此文件记录可在仓库中复查的 V1 事实与实施顺序，不改变当前线上入口或模块状态。开始写入 Schema 前，需先确定每批功能的内容模型、学习记录归属和迁移口径。

## V1 范围

- 后端 `backend/src/main/java/com/starrainnotes/english/` 有 331 个 Java 文件、20 个 Controller、约 164 个映射方法。入口页、词汇、语法、阅读、听力、写作、学习记录与练习均已有实现。
- 前端公开页面位于 `frontend/src/views/english/`；后台页面位于 `frontend/src/views/admin/`。除了英语首页，还有词汇学习和进度、语法课程、阅读文章、听力材料与语音、写作内容等页面。
- V1 表分散在 `backend/src/main/resources/db/migration/V1__init_schema.sql`、`V3__create_vocabulary.sql`、`V8` 至 `V17`、`V23`、`V26`、`V27`。英语前缀表约 30 张，另有词汇及账号词汇复习表。迁移时不能只复制 `english_overview`。
- V1 首页 `EnglishService` 的 `english_overview` 是单例，管理端可修改标题、副标题、介绍、路线图；`current_stage` 不由该接口修改。

| V1 子域 | Java 文件 | Controller | V2 首要对照对象 |
| --- | ---: | ---: | --- |
| vocabulary | 69 | 4 | 主题、词卡、发音、词族、复习 |
| grammar | 27 | 2 | 课程、分组、课时 |
| reading | 33 | 2 | 文章、关联、练习 |
| listening | 46 | 2 | 音频、片段、语音规则、练习 |
| writing | 37 | 2 | 素材、范文、任务、练习 |
| learning | 32 | 2 | 学习记录与进度 |
| shared | 72 | 4 | CEFR、分类、通用练习与学习包 |

这些数值只是文件清点，不等同于 V2 工作量估算；共享接口与首页另计。

2026-10-05 对本机 V1 库做只读计数：英语/词汇相关表共 38 张；`english_overview` 1 行、`vocabulary_theme` 60 行、`vocabulary_word` 7053 行、`english_grammar_course` 1 行、`english_grammar_lesson` 42 行、`english_reading_article` 6 行、`english_listening_item` 7 行、`english_writing_resource` 6 行、`english_writing_prompt` 6 行，`english_learning_record` 0 行。这些是本机数据快照，不代表线上库；迁移时应重新清点并逐表校验。

`V2_019` 的概览行只是让全新 V2 库可启动的默认文案。本机 V1 的概览还包含非空介绍和路线图，不能把默认行当作历史数据迁移完成的证据；后续需单独导入并核对完整内容。

## 建议实施顺序

1. **规范与数据核对**：逐项确认 V1 页面、接口、表、状态机、媒体引用及内容数量；将 V2 的内容模型与学习模型分别定稿。V2 标识符以字符串传给前端，文章正文沿用全站统一编辑器；名称自动派生 slug，不要求用户手填。
2. **公共基础与入口**：实现英语概览、CEFR 与分类基础数据及公开读取和后台编辑。入口要等可用的内容页一起开放，避免导航指向空壳。
3. **内容子域**：依次实现词汇、语法、阅读、听力、写作。每一项包含 V1 页面布局与交互对照、后台编辑、媒体引用、发布状态和真实公开访问；需要审核的对象接入 ReviewTargetHandler。
4. **学习子域**：账号词汇记忆和复习、通用学习记录、练习与写作提交、进度和分析。与教程学习域共享交互组件时保持各自数据边界，不把实现类放在 service 接口包。
5. **历史数据迁移与验收**：用可重复执行的迁移方案核对 V1/V2 内容数量和状态，逐页做真实 HTTP 与浏览器验收；最后开放英语导航和搜索/SEO 接入。

每批实现遵循 V2 现有 Maven 模块边界，在 `star-rain-english` 内按内容子域和 `learning` 分包；不再拆顶级 Maven 模块。模块服务对外只暴露必要 API，具体实现放在 `impl` 或各子域专属包。每批完成后运行相关后端测试、前端构建和真实接口检查，再合入 `mainV2`。
