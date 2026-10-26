# 英文随笔博客界面实施报告

日期：2026-10-22。分支：`LingXI/static-site`。本轮在已有英语升级成果上，根据“随笔像博客一样，只是英文的”这一反馈改造随笔列表与阅读页面。

## 已实现的界面与交互

- 随笔首页采用现有博客的标题区、日期时间线、文章摘要、阅读入口及标签／归档／统计侧栏。
- 博客和随笔共用时间线文章组件与文章阅读布局；两者继续从各自的数据源取文章，随笔没有引用技术博客的文章、标签数量或写作统计。
- 随笔模块内的面包屑、搜索、分页、日期、空状态、目录、阅读信息及前后篇导航使用英文。全站公共导航和页脚沿用本站语言。
- 默认每页 10 篇，支持调整每页数量、紧凑页码及上一页／下一页。搜索和筛选会重置页码；翻页定位到结果区域，年份在当前可见时间线中重新显示。
- 支持标题搜索、标签、月份及实际存在的文体筛选，并可组合使用。网址保存搜索词、筛选和每页数量，支持直接访问及刷新恢复。再次点击已选标签或月份可取消，一键清除可恢复全部随笔。
- 文章详情采用博客现有排版、桌面目录、手机折叠目录、阅读进度和前后篇导航。英文词数从正文提取，排除代码控件文字，以每分钟 200 词估算阅读时间。
- 可选的作者、文体、专栏、目的、难度和来源信息继续保留；可选中文翻译折叠展示，译文标题锚点添加前缀，避免与英文正文冲突。
- 保留独立文章路径、静态 HTML、草稿隔离、文章结构化数据、搜索索引和站点地图规则，无新增运行依赖、后端或数据库。

## 本轮修改文件

|文件|变更|
|---|---|
|`src/components/astro/OriginalEssays.astro`|英文博客式随笔列表、真实数据侧栏、搜索与分页|
|`src/components/astro/JournalTimelineCard.astro`|提取博客和随笔共享的时间线文章展示，支持中英文|
|`src/components/astro/OriginalBlogList.astro`|使用共享文章展示组件，保留博客既有筛选接口|
|`src/layouts/JournalPost.astro`|共享文章详情、目录、进度、前后篇与可选译文|
|`src/layouts/OriginalBlogPost.astro`|适配共享布局，保持中文博客数据与默认文案|
|`src/layouts/OriginalEssayPost.astro`|独立随笔详情数据、实际词数、英文导航与随笔标签链接|
|`src/components/astro/OriginalReadingAside.astro`|增加可选英文目录与阅读信息，默认中文|
|`src/components/astro/OriginalPagination.astro`|增加英文分页与无障碍标签，处理单复数|
|`src/layouts/BaseLayout.astro`|英文阅读区域的代码复制反馈使用英文|
|`src/utils/filter.ts`|标签／月份组合筛选、状态恢复、英文计数、时间线年份与翻页定位|
|`src/utils/essay-model.ts`|仅从已发布随笔计算标签、归档、文体、首发日期及英文词数|
|`src/utils/english-page.ts`|随笔列表和文章的英文标题、描述及结构化面包屑|
|`src/styles/essays.css`|随笔专用排版及移动端适配，修正旧全局样式对搜索框的影响|
|`tests/english.test.mjs`|新增随笔归档与标签统计测试，覆盖草稿排除、重复标签与真实日期|
|`scripts/verify-english-fixtures.mjs`|隔离副本增加 12 篇发布随笔，验证英文布局、分页、译文锚点和草稿隔离|
|`docs/english-essays-ui-verification.json`|浏览器实际执行的 15 项交互与 28 项布局记录|
|`docs/build-verification.json`、`content-validation.json`、`runtime-verification.json`、`english-fixture-verification.json` 及英语校验报告|本轮最终构建、数据和静态预览检查结果|

## 最终验证结果

|检查|结果|
|---|---|
|`npm run validate`|PASS，2818 个内容路径、2795 个 HTML 正文，原数据待确认警告保留|
|`npm run check`|PASS，77 个文件，0 errors／0 warnings／0 hints；最终 build 内再次通过|
|`npm test`|PASS，19 项全部通过，0 失败、0 跳过|
|`npm run build`|PASS，3275 个静态页面，444794 个链接／资源引用，6599 个图片引用，3166 项搜索索引|
|静态预览 HTTP 检查|PASS，477 项页面、资源、重定向与 404 检查；正式预览为 4399 端口|
|隔离文章构建|PASS，14 篇世界阅读＋12 篇随笔发布样例、2 篇草稿；发布正文、SEO、索引和站点地图正常，草稿不进入公开产物|
|浏览器交互|PASS，15 项：分页、年份、标签与月份组合、文体零结果、搜索、重置、刷新、每页数量、共享网址、译文、相邻随笔、标签跳转、手机目录、英文单复数及中文博客默认文案|
|响应式布局|PASS，320／390／768／1440 四档宽度，共 28 项，每页一个 h1，无横向溢出|
|Git 检查|`git diff --check` 通过；没有暂存、提交、推送、合并或创建 PR|

布局验收覆盖隔离副本中的随笔列表、随笔详情、博客列表、中文博客详情、语法课节、词汇页，以及正式空随笔页。完整观测值见 `english-essays-ui-verification.json`。桌面和手机正式空页面截图保存在本地 `.cache/english-essays-desktop.jpg` 与 `.cache/english-essays-mobile.jpg`。

本轮首次 `validate` 曾在写入 `docs/content-validation.json` 时返回文件系统 `UNKNOWN`。检查时文件为普通 Archive 属性，完整重跑成功，之后完整构建内的校验也通过；具体外部原因仍未定位。没有删除报告或跳过校验。

## 数据保护、当前状态与待确认事项

- 正式 `essays.json` 和 `world-articles.json` 仍是 0 篇文章。正式随笔页如实显示英文空状态和零统计，没有加入测试文章。
- 测试文章只写入被忽略的隔离副本。正式发布数据、旧词汇 JSON、词条释义、语法课程和原路径未修改；原有未提交修改继续保留。
- 本轮没有再次迁移词汇。既有 7053 条记录仍全部保留：1750 条确认映射、1163 条待复核、4140 条未分类，未删除或合并词条。
- 词汇分类参考图的 168 与可辨识 167 差异仍待原始资料确认，未补造分类。数据复核任务和旧报告中的待确认项继续有效。
- 当前没有阻塞界面使用的问题。后续发布真实随笔后，时间线、标签、归档和统计会随静态构建生成；本轮没有对外发布网站。
- 工作分支和 HEAD 保持原值，未修改主分支。你当前打开的 `/english/essays/` 已刷新为本轮构建结果。
