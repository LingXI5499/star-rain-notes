# 星雨笔录 V2 · Works V1.1 开发与验收报告

日期：2026-10-08
开发分支：`codex/portfolio-works-v11`
依据：`星雨笔录_V2_作品系统完整开发文档_V1.1_当前项目对接版.md`

## 1. 完成范围

已完成文档定义的作品系统 MVP，并包含第二批四种区块。

| 能力 | 当前实现 |
| --- | --- |
| 一级分类 | 软件项目、AI Agent、工具、实验与研究、内容创作 |
| 作品形态 | 20 个基础形态，可与分类、标签组合筛选 |
| 标签 | 独立作品标签，多选绑定，不与技术栈合并 |
| 模板 | SOFTWARE、AI_AGENT、TOOL、EXPERIMENT、CONTENT、BLANK |
| 模板创建 | 选择只预览，保存才建草稿；模板内容复制为作品独立区块 |
| 基础元信息 | 副标题、角色、技术栈、项目阶段、开始与结束日期、排序、精选、SEO 标题与描述 |
| 区块编辑 | 左侧列表，右侧编辑；独立保存、增删改、上下排序、显示与隐藏 |
| 发布流程 | 草稿、发布、撤回、恢复发布；发布前完成后端校验 |
| 前台 | 分类、形态、标签、精选筛选与分页；区块详情、目录、前后作品导航 |
| 媒体 | 复用媒体库、相对 storageKey 与媒体引用；封面、图片、画廊、音频 |
| 原型 | 公开 ZIP 媒体绑定、自定义 HTML 入口、隔离 iframe 体验 |
| Search / SEO | 可见区块文本进入索引与 SEO；自定义 SEO、封面 OG、结构化数据、Sitemap |

12 种区块：MARKDOWN、IMAGE、GALLERY、AUDIO、CODE、TIMELINE、LINKS、QUOTE、FEATURE_LIST、TECH_STACK、STATS、CUSTOM。

代码区块复用已有 Markdown 渲染管线，支持高亮与复制。CUSTOM 使用经清理的正文渲染，不接收可直接执行的自定义 HTML。

## 2. 对当前 V2 的适配

文档中部分“当前基线”仍指向旧版单模块项目、`portfolio_project`、`/api/v1` 和 V35。实际仓库已采用 V2 Maven 多模块与 `sr_portfolio_work` 聚合，因此沿用当前模型升级：

- 聚合根仍为 `sr_portfolio_work`，保留 ID、slug、bodyMarkdown、typeDetail、原有媒体和链接。
- 前台仍为 `/portfolio` 与 `/portfolio/:slug`，登录端使用对应 `/useradmin` 路由。
- API 沿用 `/api/public/portfolio/works` 与 `/api/admin/portfolio/works`。
- 模板用种子数据中的 JSON 结构快照保存，再复制为作品区块；模板更新不会回写既有作品。
- 没有区块时仍渲染旧 bodyMarkdown；存在区块时使用区块内容，全部隐藏也不会重新显示旧正文。
- 旧阶段 `ONLINE` 继续兼容；新作品使用扩展后的项目阶段。
- 当前 V2 没有旧文档所称的 ZIP 原型入口，本次通过 Media 模块增加可选静态 ZIP 能力。

已应用迁移：

| 迁移 | 内容 |
| --- | --- |
| V2_032 | 作品元信息、分类、形态、标签、模板、区块、区块媒体及兼容分类回填 |
| V2_033 | 静态原型资产与入口、旧角色和项目阶段兼容回填 |

迁移前已将四张原有作品表备份到本机 `.local/portfolio-before-v11.sql`。未清空、重建或覆盖旧作品正文。数据库已验证到版本 2.033。

## 3. 使用流程

1. 在“作品管理”点击“新建作品”，选择模板并填写基本信息。
2. 保存创建草稿。选择模板本身不会提前生成数据库记录。
3. 在“分类与标签”选择分类、形态及标签，保存基本信息。
4. 在“内容区块”选择左侧区块，在右侧编辑并单独保存；空的可选区块可隐藏或删除。
5. 在“媒体与原型”选择公开封面；软件作品可绑定公开 ZIP，入口默认为 `index.html`。
6. 在“外部链接”和“SEO”完善信息。
7. 在“发布”预览并发布。发布后的内容编辑也受发布约束保护。

基础信息与区块分别保存；离开有未保存修改的页面或切换区块会提示。日期可明确清空。

## 4. 后端边界与安全

- 发布要求标题、摘要、合法 slug、分类、形态、公开封面，以及有效可见内容。旧版正文作品保留兼容通道。
- 区块限定类型、版本、字段、长度、结构条目及媒体类型；发布后的可见区块不能为空。
- 区块操作验证作品归属；排序要求完整且不重复的区块 ID。
- 精选最多 3 件，通过共享数据库行锁串行检查。
- 通过 MediaReferenceApi 建立与解除引用，媒体归档保护覆盖图片、画廊、音频、编辑器选择的正文图片和原型。
- 隐藏区块从公开 VO、Search 文本和 SEO 正文中排除；撤回作品不再进入公开详情、原型、索引和 Sitemap。
- 外链限定 http/https 且拒绝 URL 中的账户信息。Markdown 复用现有清理管线，代码和引用按文本处理。
- 原型 ZIP 限 25 MB、单项 10 MB、展开总量 100 MB、最多 2000 项，拒绝跨目录路径、重复路径和不支持的文件类型；按流读取，不解压到网站目录。
- 原型通过 `sandbox="allow-scripts"` 与 CSP 隔离，禁止网络连接、嵌套框架、表单提交及父页面同源访问。
- 全局安全链仅对 `/api/public/portfolio/works/*/live/**` 输出 `X-Frame-Options: SAMEORIGIN`；普通页面继续为 `DENY`，认证与 CSRF 规则保持有效。

## 5. API 入口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /api/public/portfolio/taxonomy | 分类、形态、标签 |
| GET | /api/admin/portfolio/templates | 模板与结构预览 |
| GET | /api/public/portfolio/works | 分页与组合筛选 |
| GET | /api/public/portfolio/works/{slug} | 公开详情 |
| POST / PATCH | /api/admin/portfolio/works、/api/admin/portfolio/works/{id} | 创建与元信息编辑 |
| POST | /api/admin/portfolio/works/{workId}/sections | 新增区块 |
| PUT / DELETE | /api/admin/portfolio/works/{workId}/sections/{sectionId} | 修改或删除区块 |
| PUT | /api/admin/portfolio/works/{workId}/sections/order | 完整区块排序 |
| PUT | /api/admin/portfolio/works/{workId}/prototype | 绑定或解除原型 |
| GET | /api/public/portfolio/works/{slug}/live/{path} | 原型静态资源 |

已有作品媒体、外链、发布、撤回及恢复接口继续使用原契约。

## 6. 验证结果

### 自动测试与构建

- 前端：23 个测试文件、108 项测试通过；Vite 生产构建通过。
- 后端专项：47 项测试通过，包括作品服务、真实 MySQL Mapper、原型 ZIP、安全响应头、权限规则及 SEO。
- 后端 Maven 打包通过；本机运行包已更新。
- `git diff --check` 通过。

后端专项组成：PortfolioContentMapperTest 1、PortfolioProfileMapperXmlTest 12、StaticPrototypeApiTest 4、PortfolioContentServiceTest 10、PortfolioPrototypeServiceTest 2、PortfolioWorkServiceImplTest 6、SeoCoreTest 4、ModuleFrameHeadersTest 2、SecurityRulePlanTest 6。

覆盖的关键风险包括：旧正文回退、隐藏内容、模板复制、跨作品区块归属、排序、媒体引用合法编码与保护、空区块发布、精选上限、日期清空、ZIP 路径与容量限制、默认框架保护以及旧 typeDetail 缺失时的后台渲染。

### 真实浏览器与 HTTP 验收

| 流程 | 结果 |
| --- | --- |
| 选择模板只预览 | 未提前创建草稿 |
| 空白作品保存 | 生成独立作品，日期、分类、形态、标签正确保存 |
| 软件模板保存 | 生成 14 个独立区块，正文修改保存后重新读取正确 |
| 代码、成果数据、隐藏引用 | 独立保存成功，公开页面只显示可见区块 |
| 图片区块与现有媒体库 | 成功绑定、保存并调整排序 |
| 基础信息日期清空 | 重新读取为 null |
| 发布、撤回、恢复发布 | 状态更新成功，撤回后详情、原型和 SEO 返回 404，Search 不再命中 |
| 分类与 Java 标签筛选 | URL 条件和实际结果一致 |
| Search / SEO | 可见内容进入索引，隐藏标记不在索引；SEO 有正文、OG 封面与 JSON-LD |
| Sitemap | 发布时包含验收作品；撤回后排除 |
| 原型 ZIP 绑定 | 媒体库选择后成功绑定 index.html |
| 原型运行 | iframe 中 CSS、ES module 正常加载，点击计数从 0 变为 1 |
| 安全响应头 | 原型 SAMEORIGIN，普通作品 API DENY |
| 原有作品 | 两件旧作品的原 slug、正文和公开详情仍可访问 |

验收中发现并修复了媒体引用 usageCode 格式、默认 X-Frame-Options 覆盖、旧 typeDetail 为 null 时后台列表报错，并分别补充测试。

## 7. 验收数据与交付状态

- 本机作品 145：“Works V1.1 验收作品”，已撤回，保留区块与原型绑定便于复核。
- 本机作品 152：“Works V1.1 模板验收草稿”，保持草稿，包含软件模板及已填写的第一块正文。
- 原型资产 162 为本次生成的纯测试 ZIP，无个人信息。媒体引用仍受归档保护。
- 最终公开列表保留原有两件作品；验收样例不进入公开列表、Search 或 Sitemap。
- 截图在 `.local/works-proof/`：editor.jpg、template-editor.jpg、public-blocks.jpg、prototype.jpg。
- 2026-10-08 补充远程交付：作品及必要的 Media、Account 安全、Search、SEO 改动整理为独立提交，交付分支 `codex/portfolio-works-v11`。博客、教程等其他未提交工作保持原状，未混入作品提交。
- 本次收尾从 Git 暂存区导出独立源码副本验证：作品前端 4 个测试文件、14 项测试通过，Vite 生产构建通过。后端专项 34 项通过，13 项数据库测试因独立副本没有数据库配置而跳过；日志见本机 `.local/works-release-backend.log`。真实数据库验收仍以本报告前述原开发过程中的记录为准。
- 作品代码提交 `7f457fa` 已推送到 GitHub 的 `codex/portfolio-works-v11` 分支，收尾文档随该分支交付。
- 远程分支提交不等于合并 main 或生产部署；正式合并时需要与尚在本地的教程迁移 V2_027～V2_031 一并核对迁移顺序，避免已有高版本数据库漏跑低版本迁移。

## 8. 文档中明确暂缓的范围

分类、形态、标签和模板目前采用种子数据与只读查询，未做完整的词表 / 模板后台 CRUD，符合文档 MVP 范围。暂未引入本地视频上传、OSS、AI 自动生成、复杂拖拽画布或独立 Architecture 编辑器。
