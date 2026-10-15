# 作品模块轻量化升级：交付与验收记录

日期：2026-10-15。开发分支：`codex/portfolio-lightweight`。

## 基线与范围

- GitHub 接口核对 `main` 与 `e67933515cf632cd3606ff8dbf595d48f4738688` 一致。终端直接 fetch 连接失败，使用已存在的同一提交建立开发分支；未宣称 fetch 成功。
- 开发前的工作区 HEAD 为 `88e67aa`，与上述 main 的源码无差异；唯一已有未跟踪内容为 `docs/tutorials/`，未改动或提交。
- 基线作品前端测试：4 个文件、15 项通过。基线 `mvn -pl star-rain-portfolio -am test -q` 通过。
- 未新增运行依赖、数据库迁移或自动导入功能；未连接或修改生产数据库，未部署、推送远端或发布真实作品。
- 仅参考 Nuxt Portfolio 的图文交错布局，没有复制其代码或演示图片。

## 实现与兼容性

- 列表：三个独立查询的精选作品交错展示，全部作品保留原分页总数，精选可以重复出现；普通卡片 16:9、最多三个技术标签；分类可直接选择，其余筛选折叠。原路由参数与详情地址不变。
- 详情：介绍、相关链接、视觉内容、正文、相邻作品导航。所有启用且安全的外链均可访问；按用途标注查看源码、在线体验、下载、演示或文档。
- 图片：只抑制自动插入的重复封面和截图，不修改作者正文；支持 Markdown 引用式图片。保留旧区块、截图与音频。
- 编辑：默认单页 Markdown 创作，复用现有编辑器、媒体库、历史和本地恢复。分类、形态、阶段、日期、精选、slug、SEO、静态原型等放入更多设置，选项来自现有 API。
- 保存：先创建并立即保留 ID，再保存正文、封面、链接；每步成功后更新本地记录，失败说明环节，重试继续同一作品。保存期间的新输入保留，重复提交与离开受到保护。
- 恢复：复用账户隔离的恢复组件保存基本信息、待关联封面和链接；Markdown 保留独立恢复与新文档 ID 迁移。刷新或离开前保留提醒。
- 新旧正文：存在任何区块（包括全隐藏区块）时仍优先使用区块，不回退暴露旧 Markdown；旧作品保留原编辑工作区。高级模板仍可主动选用，默认不创建区块，不执行格式转换。
- 封面：允许无封面发布、移除已发布作品的最后一张封面或改为截图，仍保留媒体权限、可用性、引用解除和版本检查。
- 发布：保存后检查必填项，再走现有发布/恢复接口。撤回、首次发布时间、后台权限、SEO 默认标题和简介沿用现有规则；无封面时不输出空的 Open Graph 图片。

## 自动化结果

| 检查 | 实际结果 |
| --- | --- |
| `npm ci` | 成功，审计 0 个漏洞 |
| `npm test` | 77 个文件、436 项通过 |
| `npm run build` | 通过 |
| `mvn -pl star-rain-portfolio,star-rain-seo -am test -q` | 通过，作品 36 项、SEO 21 项；所选模块无跳过 |
| Playwright 作品验收与原有 reliability 用例 | 23 项通过；Edge Chromium，1440px / 390px，日间与夜间 |

浏览器回归检查真实 Vue 路由、Vditor、保存重试、顺序版本请求、重新打开、发布/撤回/恢复、原有筛选滚动位置、手机目录键盘操作与页面横向溢出。接口使用隔离的内存服务，不访问真实业务数据库。

本机 Playwright 自带 Chromium 启动报 `spawn UNKNOWN`，改用已安装的 Edge 执行，未改项目默认浏览器配置。复现命令（PowerShell，`frontend` 目录）：

```powershell
$env:PLAYWRIGHT_CHANNEL = 'msedge'
$env:STAR_RAIN_PORTFOLIO_SOURCE = 'E:\ai-coding-portfolio'
npm run test:e2e -- portfolio.spec.js reliability.spec.js
```

原始结果位于本地忽略目录：`.local/portfolio-frontend-tests.log`、`.local/portfolio-build.log`、`.local/portfolio-backend.log`、`.local/portfolio-e2e-final.log`；浏览器截图位于 `frontend/test-results/portfolio-gallery-reading-and-editor-layout-*/`。

## 八项作品的隔离验收

资料来源：用户提供的 `E:\ai-coding-portfolio` 和 [GitHub 仓库](https://github.com/LingXI5499/ai-coding-portfolio)。测试样本根据各项目 README 手工编排，不是产品中的导入功能。

| 作品 | 已验证 |
| --- | --- |
| AtlasFlow | 地图相册介绍、技术栈、源码链接、正文保存与重开 |
| Frontend Lab | 较长教学说明、Markdown 长文编辑与阅读 |
| Java Web Lab | 技术介绍、SQL 代码块与引用排版 |
| Resume Studio | 工具特性、备份与 PDF 说明、源码链接 |
| Asterway | 原型状态说明、仓库内 reading-demo.png 真实截图、正文图片与封面/画廊去重 |
| MusicLingXi | Android 播放器说明、技术栈与项目内容 |
| AI Intelligence Daily | Python 项目说明、运行代码与相关链接 |
| Starfall Ruins | 游戏流程介绍、Unity/C#/Blender 技术说明 |

所有样本验证编辑、保存、重新打开、模拟发布和详情渲染。其他项目仓库未提供对应成品截图，本次没有把贴图、图标或手势测试照片冒充作品截图。

## 人工验收与剩余边界

上线前，在隔离 MySQL 与测试媒体库中再执行：

1. 创建无封面作品，填写标题、简介、真实分类/形态、Markdown 与外链，保存、重开、预览、发布；确认数据库只有一条作品记录。
2. 在已发布作品中更换、移除封面；检查媒体引用可安全解除、正文不变、公开卡片显示占位。
3. 选择私有/不可用媒体、不安全链接，确认服务拒绝；用普通账号确认后台访问被拒绝。
4. 对旧区块作品执行编辑、预览、撤回、恢复；全隐藏区块不能展示旧 Markdown。发生版本冲突后核对服务器内容，不能自动覆盖。
5. 两个浏览器窗口并发编辑，验证真实 HTTP 版本头；断网后恢复当前字段、本地元信息、封面和链接。
6. 为 AtlasFlow、MusicLingXi、Starfall Ruins 等补充真实地图、移动端、游戏成品截图，再检查图片裁切与清晰度。

本轮没有连接独立 MySQL 或真实媒体服务，不能据此宣称已完成真实数据库事务、真实上传权限端到端或所有作品的成品截图验收。没有在 Android 真机或 Unity 中运行参考作品；这不属于作品模块运行功能。

## 修改文件与提交

前端：

- `frontend/src/modules/portfolio/components/WorkLibrary.vue`、`WorkLibrary.spec.js`、`WorkCard.vue`
- `frontend/src/modules/portfolio/pages/WorkDetailPage.vue`、`WorkDetailPage.spec.js`
- `frontend/src/modules/portfolio/pages/admin/WorkEditorPage.vue`、`WorkEditorPage.spec.js`
- `frontend/src/modules/portfolio/components/admin/WorkSectionEditor.vue`（向父组件暴露忙碌状态）
- `frontend/src/modules/portfolio/support/workPresentation.js`、`workVisualMedia.js`
- `frontend/e2e/portfolio.fixtures.js`、`portfolio.spec.js`
- `frontend/playwright.config.js`（真实后端模式排除新增的模拟接口验收）

后端：

- `backend/star-rain-portfolio/src/main/java/com/starrainnotes/portfolio/service/impl/PortfolioWorkServiceImpl.java`
- `backend/star-rain-portfolio/src/test/java/com/starrainnotes/portfolio/service/PortfolioWorkServiceImplTest.java`
- `backend/star-rain-portfolio/src/test/java/com/starrainnotes/portfolio/security/PortfolioAdminAuthorizationTest.java`
- `backend/star-rain-seo/src/test/java/com/starrainnotes/seo/SeoCoreTest.java`

阶段提交：

- `8fe998c`：列表画廊与交错精选。
- `a210dc6`：详情视觉内容和安全外链。
- `a479128`：单页编辑与可重试保存。
- `3a697c0`：封面可选与后端安全回归。
- 后续验收提交包含浏览器用例、恢复/导航边界补充与本记录；完整记录使用 `git log --oneline e679335..HEAD` 查看。
