# 英语模块升级实施与验收报告

> 本文记录首轮模块升级。后续两级筛选与圆形主题切换的当前实现和验证结果见 [界面改进报告](english-filter-refinement-report.md)。

日期：2026-10-22。正式基线为用户提供的《星雨笔录静态站：英语模块分类体系升级开发文档》（原文件名英语.md），SHA-256：`1a7cc135730d824e521b2ba4d275d77fae98a56ae531889a6ac7f86f6f063222`。

## 交付状态与边界

本地功能实现和本次适用校验通过。词汇原始记录迁移覆盖率 100%，自动规则确认分类 24.81%；尚有 5303 条记录需要继续人工整理，不能将其表述为已完成全部语义分类。世界和随笔均没有真实已发布文章，当前正式清单为空，页面展示真实空状态。

- 分支：`LingXI/static-site`；起点与结束 HEAD 均为 `385c3a51e9f507ef90ee0b0236f2b20db118ce7d`。
- 开始工作区干净；只在现有子项目开发，没有重建项目、切换分支、暂存、提交、推送、合并或创建拉取请求。
- 开始时远程目标分支引用与本地 HEAD 相同。未拉取或改写任何远程引用。
- 结束时 50 个文件新增或修改，均为本次本地未提交成果；暂存区为空。
- 保留 61 个旧词汇源文件、43 个语法文件，104 个原文件哈希与盘点基线一致。
- package.json、锁文件、后端及数据库均未修改；没有增加依赖。
- 预览为独立 dist 静态服务器；默认 4321 端口已占用，本次使用 4399，没有停止原有进程。

## 分阶段实施

|阶段|实施内容|实际验收|
|---|---|---|
|A 仓库与盘点|分支、干净状态、最近提交、目标远程引用；词汇实数、重复、空主题、原文件哈希|60 个旧主题、7053 条记录；58 为空；语法 10 章/42 课|
|B 分类与模型|按附录完整写入 T/R 分类；统一类型、路径、发布状态与计数查询|T=18/167，R=16/144，编号/父级/slug/唯一性通过|
|C 迁移与兼容|逐记录来源 ID、例句数组、语义规则证据、复核队列；旧主题兼容页面|7053/7053 保留；0 删除、0 合并；全部 60 个旧路径可访问|
|D 世界|独立知识地图、主题页、文章详情、来源、正文处理与空态|正式 0 篇；隔离发布/草稿样例验证通过|
|E 随笔|独立清单、按时间倒序、可选文体/标签/专栏、共享正文组件|正式 0 篇；隔离样例验证详情/排序；未混入 catalog.blogs|
|F 首页与全站能力|四入口、搜索类型、canonical/OG/schema、空页 noindex 与 sitemap、响应式|44 项布局检查无溢出，搜索/分页/语言/主题/导航通过|
|G 回归与报告|四项必跑命令、静态 HTTP 回归、来源复查、完整清单|全部通过；保留明确的数据待决项|

## 数据迁移结果

|口径|结果|
|---|---:|
|旧主题声明总数|7053|
|旧 JSON 实际记录|7053|
|迁移后独立来源记录|7053|
|已确认规则映射|1750|
|待复核|1163|
|未分类|4140|
|完全相同字段的疑似重复涉及记录|2242|
|重复拼写涉及记录|4345|
|删除 / 合并|0 / 0|
|examples 字符串转数组|7053|
|缺少美式 / 英式音标|136 / 81|

“唯一来源记录”按 entryId 计算，不以词形去重；多分类记录会计入各主题，不能把主题计数之和称为全站词汇量。完全相同字段组与重复拼写组口径不同，不相加。全部例句原始值为字符串形式的空数组，正式词卡未展示不存在的例句。

迁移使用原释义与场景释义的双重语义命中，冲突或证据不足时不确认。旧主题 55–60 的全部非空记录均进入复核，区分功能词、多词表达和需核实的普通词；主题 58 保持空页。原数据与迁移数据各自保留，原字段逐条比对，稳定 ID 为旧主题 ID 与原数组位置。

逐旧主题的声明/实际数、逐新主题实数、旧主题到新分类映射、空主题和完整待决清单分别见 [迁移报告](english-taxonomy-migration-report.md)、[机器统计](english-migration.json)、[复核队列](english-review-queue.json)、[原始盘点](english-inventory.json)。没有静默删除、词形合并或用候选映射充当已确认映射。

## 分类、页面与 SEO

词汇一级 18 个、已辨识二级 167 个。`expectedFromImage=168`、`enumerated=167`、`status=needs-confirmation` 同时保留；没有编造缺失分类。世界一级 16 个、二级 144 个，均通过数量、代码、父级、slug 及引用校验。

当前英语共 452 个 URL，新增 347 个：185 个词汇分类页、160 个世界分类页和世界/随笔两个总览。旧数字主题 60 个、语法课节 42 个与原英语入口继续存在。完整路径见 [路由清单](english-route-manifest.json)。

|路由模式|说明|
|---|---|
|`/english/`|词汇、语法、世界、随笔四入口|
|`/english/vocabulary/`|新词汇总览和旧资料入口|
|`/english/vocabulary/t01/`|一级词汇分类；T01–T18|
|`/english/vocabulary/t01/t01-01/`|实际附录二级分类，分页展示已确认词条|
|`/english/vocabulary/1/` … `/60/`|原始主题兼容页和全部原词条|
|`/english/grammar/` 与全部原课节|保留课程与正文；路线直接使用真实十章|
|`/english/world/`、`/r01/`、`/r01/r01-01/`|世界知识总览、一级及二级主题|
|`/english/world/articles/<slug>/`|仅真实 published 阅读生成详情|
|`/english/essays/` 与 `/<slug>/`|独立随笔目录；仅 published 生成详情|

构建检查到 175 个空页使用 noindex,follow 且排除 sitemap（空词汇主题、全部空世界分类/总览、空随笔总览及旧主题 58）；分类数据和可浏览页面仍然保留。正式草稿数为 0，因此另用隔离副本验证草稿不生成 URL、不进列表/索引/sitemap。正式阅读和随笔没有发布内容，不把隔离样例混入正式内容。

搜索保留旧词汇资料并标记“英语旧词汇”，新词汇/语法/阅读/随笔分别标记。索引共 3166 项，其中英语词汇 185、英语阅读 161、英语旧词汇 60、英语语法 42、英语随笔 1（随笔总览）。没有把全部词条灌入全站索引。所有搜索路径、内部链接及资源存在性均通过构建校验。

文章复用 renderBody 的清理、目录和高亮能力；元数据生成 canonical、description、OG、面包屑和 Article/BlogPosting。缺少作者或日期时不编造；有效发布日期是 published 的校验条件。文章分类/来源/翻译、文体、交际目的、难度均按独立字段管理。

## 实际测试结果

|命令或检查|结果|证据|
|---|---|---|
|npm run validate|PASS，保留 167/168 与人工复核两条明确警告|content-validation.json、english-validation.json|
|npm run check|PASS；68 个文件，0 errors / 0 warnings / 0 hints|最后一次 check 输出|
|npm test|PASS；12 tests，0 fail，0 skipped|最后一次 test 输出|
|npm run build|PASS；3275 页，424599 链接/资源，6599 图片引用|build-verification.json|
|隔离文章构建|PASS；3 篇 published、2 篇 draft，详情/正文/高亮/schema/索引/排序验证|english-fixture-verification.json|
|静态预览 HTTP 回归|PASS；477 项，覆盖全部新英语分类、60 旧主题、42 课节与既有主要模块|runtime-verification.json|
|浏览器布局|PASS；11 个模板页面 × 320/390/768/1440 宽度，共 44 项，无横向溢出且每页 1 个 h1|english-ui-verification.json|
|浏览器交互|PASS；分类搜索、词汇翻页/中文搜索/无结果/语言切换、全站类型筛选、主题切换、更多导航、键盘目录和页脚返回顶部|english-ui-verification.json|
|来源复核|PASS；104 个原词汇/语法文件哈希一致，7053 条原字段逐条一致|english-inventory.json 与迁移校验|
|git diff --check|PASS；没有空白错误|最终命令输出|

既有首页、教程、技术博客、标签/专题、详情、归档、作品、关于、搜索、隐私与条款 HTTP 回归通过；catalog 同步与既有测试保留。公共页脚“返回顶部”实测 scrollY 10917→0，沿用既有入口，没有引入额外浮动按钮。

检查边界：全站链接/SEO/HTML 为自动静态校验；44 项布局是代表模板的真实浏览器检查，并非逐页截图检查全部 3275 页。文章浏览器检查使用隔离测试内容，正式文章清单为空。当前仅本地验收，没有部署或线上验证。

## 修改文件与职责

M 为修改，?? 为新增且未暂存。以下清单覆盖本次全部 50 个候选文件。

|状态|文件|职责|
|---|---|---|
|M|`astro.config.mjs`|从真实内容计算空分类 sitemap 排除集合；保持静态输出和尾斜杠。|
|M|`docs/build-verification.json`|全站构建、资源、SEO 和搜索结果。|
|M|`docs/content-validation.json`|更新后的全站内容校验结果。|
|M|`docs/runtime-verification.json`|477 项静态预览 HTTP、资源、重定向和 404 检查结果。|
|M|`scripts/validate-content.mjs`|接入英语分类、文章、来源与迁移一致性校验。|
|M|`scripts/verify-build.mjs`|保留全站 SEO/链接校验，接入英语发布与空内容验收。|
|M|`scripts/verify-runtime.mjs`|预览回归覆盖全部英语分类、旧词汇页和语法课节。|
|M|`src/components/astro/OriginalEnglish.astro`|收敛到语法目录与课节；学习路线改为真实十章。|
|M|`src/components/vue/StaticSearch.vue`|区分新旧词汇、语法、阅读、随笔；保留英语聚合筛选。|
|M|`src/layouts/BaseLayout.astro`|加载模块样式；为文章可选输出 og:type=article。|
|M|`src/pages/[...route].astro`|新页面分发、数据驱动 SEO、完整分类面包屑与文章结构化数据。|
|M|`src/pages/search-index.json.ts`|收录新分类与已发布文章，不索引逐条词汇或草稿。|
|M|`src/utils/content.ts`|集中导出新英语数据，保留现有 HTML 处理链。|
|M|`src/utils/filter.ts`|允许文章使用标题专用搜索文本，原筛选逻辑保持。|
|M|`src/utils/routes.ts`|追加新静态路由，并对全站 route 重复直接报错。|
|??|`docs/english-baseline.json`|正式基线文档哈希、分支、起点提交及初始干净状态。|
|??|`docs/english-build-verification.json`|345 个新增分类页及旧路径、课程、空页 SEO 的构建结果。|
|??|`docs/english-fixture-verification.json`|隔离环境中 3 篇发布样例和 2 篇草稿的集成验收结果。|
|??|`docs/english-implementation-report.md`|本次完整实施与验收报告。|
|??|`docs/english-inventory.json`|全部旧主题实数、缺失字段、疑似重复组及原文件 SHA-256。|
|??|`docs/english-migration.json`|逐旧主题映射表、每个新主题计数及空主题清单。|
|??|`docs/english-review-queue.json`|全部 5303 条待复核/未分类记录，含来源、释义、建议和原因。|
|??|`docs/english-route-manifest.json`|452 个当前英语 URL 的完整清单，其中新增 347 个。|
|??|`docs/english-taxonomy-migration-report.md`|可阅读的数据盘点、迁移统计与主题对照报告。|
|??|`docs/english-ui-verification.json`|44 项真实浏览器布局检查和交互检查结果及覆盖边界。|
|??|`docs/english-validation.json`|分类、文章元数据、来源与迁移校验的实际结果。|
|??|`scripts/english-inventory.mjs`|只读盘点旧数据、缺失字段、重复记录及 104 个原文件哈希。|
|??|`scripts/english-mapping-rules.mjs`|可审阅的逐记录语义匹配规则。|
|??|`scripts/english-rules.mjs`|分类树、文章状态、本地正文、合法日期、来源覆盖与迁移字段校验。|
|??|`scripts/migrate-english.mjs`|确定性迁移、例句规范化、复核队列和统计报告生成；支持 --check。|
|??|`scripts/verify-english-build.mjs`|旧词条展示完整性、课程路径、索引、文章 schema 和 sitemap 校验。|
|??|`scripts/verify-english-fixtures.mjs`|在忽略的隔离副本验证发布/草稿、阅读详情及随笔排序，不改正式数据。|
|??|`src/components/astro/EnglishArticle.astro`|复用 renderBody 的共享正文、来源、可选翻译、目录和文章导航。|
|??|`src/components/astro/EnglishArticleList.astro`|仅发布文章卡片、标题搜索、分页及真实空状态。|
|??|`src/components/astro/EnglishBreadcrumbs.astro`|可换行的英语模块面包屑。|
|??|`src/components/astro/EnglishHome.astro`|真实统计的四入口英语首页。|
|??|`src/components/astro/EnglishWordList.astro`|24 条分页、搜索、语言切换、真实例句和来源展示。|
|??|`src/components/astro/OriginalEssays.astro`|独立随笔列表与详情，按发布日期倒序。|
|??|`src/components/astro/OriginalVocabulary.astro`|18/167 词汇目录、主题页面和数字旧路径兼容。|
|??|`src/components/astro/OriginalWorld.astro`|16/144 世界知识地图、一级/二级主题与阅读详情分发。|
|??|`src/content-data/english/essays.json`|独立个人随笔元数据清单，当前为空。|
|??|`src/content-data/english/vocabulary-entries.json`|7053 条逐来源保留的规范化记录、映射证据与整理状态。|
|??|`src/content-data/english/vocabulary-taxonomy.json`|附录 A 全部 18/167 分类及 168/167 待确认元数据。|
|??|`src/content-data/english/world-articles.json`|正式阅读元数据清单，当前为空；不发布示例文章。|
|??|`src/content-data/english/world-taxonomy.json`|附录 B 全部 16/144 分类。|
|??|`src/styles/english-upgrade.css`|复用站点变量的响应式分类、词卡、文章与可访问性样式。|
|??|`src/utils/english-model.ts`|统一类型、路径、分类查询、发布时间排序、真实计数和空页判定。|
|??|`src/utils/english-page.ts`|英语页面标题、摘要、面包屑、文章信息与 noindex 策略。|
|??|`src/utils/english-search.ts`|数据驱动的英语静态搜索条目生成。|
|??|`tests/english.test.mjs`|真实数据保留、错误拒绝、草稿隔离、排序、搜索和路由测试。|

## 尚未解决及需确认事项

1. 参考图第 168 个词汇二级主题仍无法辨认，需要补充确认其编号与名称。当前保持 167 项，不新增猜测项。
2. 1163 条待复核与 4140 条未分类记录需要人工审阅原词性、释义、语境和分类。全部 5303 条可在复核队列定位，原始记录继续在旧主题页面公开可查；不能称为全部词条分类完成。
3. 旧主题 55–60 的语言系统材料需要决定长期归属；没有将其直接删除或当作普通主题词汇强行迁移。
4. 136 条美式、81 条英式音标缺失，以及重复/词义不一致属于源数据现状。未补造音标、改写释义或擅自合并。重复组保留为审阅依据。
5. 世界阅读与个人随笔需要站长提供真实文章及来源/版权信息。当前没有正式文章；所有分类与内容管线已可用。

## 后续内容维护

阅读索引使用 world-articles.json，随笔使用 essays.json，均为 schemaVersion=1 与 articles 数组。正文为本地 body.html；阅读 bodyPath 使用 english/world/articles/<slug>/body.html，随笔使用 english/essays/<slug>/body.html。slug 为稳定小写短横线标识；published 必须有有效的 YYYY-MM-DD 发布日期及非空正文，draft 也必须指向存在的本地正文。可选中文译文使用同目录 translation.html 和 translationBodyPath。

阅读指定一个有效 R 二级主题，可选 relatedTopicCodes；随笔不套世界分类。genre、purpose、difficulty、author、source、tags、series、cover 等按实际信息填写，留空时不制造元数据。source 使用 title 与可选 https/http url；cover 为本地 public 资源路径。difficulty 使用 A1–C2，未评估则不填。

迁移规则调整后运行 node scripts/migrate-english.mjs，再审阅生成的记录、证据、复核队列和统计；node scripts/migrate-english.mjs --check 可检查产物是否与原数据/规则一致。原文件盘点基线禁止静默覆盖。后续任何内容变更继续运行四项项目检查，再进行独立静态预览。
