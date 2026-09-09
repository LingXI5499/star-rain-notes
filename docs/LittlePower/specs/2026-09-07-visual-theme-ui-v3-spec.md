# Visual Theme UI V3 Spec

## Goal

公共站点在明暗模式下呈现「星雨笔录」视觉主题 V1.0 品牌气质：Design Tokens、Logo/Favicon、公共 Header 品牌区对齐文档色板与规范；Home / English / About 分别以主题 4 / 2 / 3 为电影感 Hero 主视觉，气质对齐、沿用现有页面壳与文案能力。

## Global Constraints

- 本轮只开发并推送 **Gitee**；不触碰 GitHub（除非用户另开指令）。
- 公共导航 **只换皮**：不增删菜单项、不改路由 path、不按主题稿重命名 IA。
- **不像素还原** 主题 PNG；不把稿内假文案/假导航硬拷进站。
- `主题1.png` 仅作品牌规范参考；**不**作为页面 Hero 大图。
- 教程 / 博客 / 作品列表与详情、后台管理视觉：**非本轮**。
- 实现顺序约定：Tokens → 品牌栏 → Home → English → About（可同一分支分提交，但验收按切片）。
- 核心行为必须通过真实公共入口验证；mock / stub / no-op / 占位 1×1 图不得冒充主题落地。
- 色板目标值（实现时映射到现有 CSS 变量名，**不要求**变量改名；验收按下列精确 hex，大小写不敏感）：
  - Starfield Green `#0F3D36` → 亮色 `--primary`；亮色 `--el-color-primary`
  - Mist Green `#7A9C8F` → 暗色 `--primary`；暗色 `--el-color-primary`（可读主色）
  - Moon White `#F8F6F1` → 亮色 `--bg-page`
  - Star Orange `#C46F4E` → 亮色与暗色 `--accent`
  - Earth Brown `#B89B84` → 可用作次级边框/装饰，本轮不强制绑到具名变量
  - Night Ink `#102C31` → 暗色 `--bg-page`
- 主题 Hero 静态资源目录（唯一约定）：`frontend/public/brand/themes/`
  - Home：`home-hero.webp`（源：`主题4.png`；允许同 stem 的 `.png` 替代，但路径前缀必须为此目录）
  - English：`english-hero.webp`（源：`主题2.png`；同上）
  - About：`about-hero.webp`（源：`主题3.png`；同上）
- 参考文档（仓库根，实现时可迁入 `docs/`）：`星雨笔录_视觉主题系统说明文档_V1.0.md`。

## Feature Slice 1: Design Tokens（明暗）

- [x] Implementation status: Done on `feat/visual-theme-ui-v3`

### Behavior

- 亮色主题下，公共页背景、主色、强调色呈现文档色板气质（月白纸感底、星夜绿主色、星芒橙强调）。
- 暗色主题（`html[data-theme="dark"]`）下，页背景趋 Night Ink 族，主色/强调色仍可读且保持同一品牌族，不串回旧「墨境」偏色。
- Element Plus 前台/与主题相关的 primary 映射跟随新主色，避免控件与站点主色明显割裂。
- 不借机大改间距尺度、容器宽度布局系统（除非为对比度/可读性的最小必要调整）。

### Public Interface

- CSS：`frontend/src/styles/tokens.css`（及若已消费 token 的 `public-theme.css` 等公共主题层）。
- 主题切换：现有 `ThemeControl` + `stores/theme`（`light` / `dark` / `system` → `data-theme`）。
- 无新 API / 无新路由。

### Error and Boundary Cases

- 在 light / dark / system 间切换时，关键表面（页背景、正文、主按钮、链接强调）不得出现未定义色或明显不可读对比。
- `system` 跟随 OS 时，解析结果须与手动 light/dark 使用同一套新 token。

### Non-goals

- 不重做全站组件库；不新增第三套主题。
- 不改后台独立皮肤策略（后台可被动吃到 token，但不作为本切片验收对象）。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：加载前端样式 → 分别在 `data-theme=light` 与 `data-theme=dark` 下读取计算后的 CSS 变量（或等价的 tokens 源值断言）。
- 默认断言层级：CSS 自定义属性值 / 主题 store 写到 `document.documentElement` 的 `data-theme`；调试产物可落 `.tmp/ui-v3/`。
- 防 mock 逃逸禁令：不得仅改注释或文档而保留旧 hex；断言必须命中实际会进构建的 `tokens.css`（或运行时 computed style）。
- 仓库无 Playwright：本切片以 Vitest 为主；气质目视作辅助，不单独充当 AC 通过条件（除非标注降级）。

#### AC-1: 亮色色板与 EP primary

- 触发：以默认亮色加载公共样式（或挂载使用 token 的根样式）。
- 必须可观察：`:root` 下精确为 `--bg-page: #F8F6F1`、`--primary: #0F3D36`、`--accent: #C46F4E`、`--el-color-primary: #0F3D36`（大小写不敏感）；不得再以升级前默认值 `#f7f7f4` / `#28564d` / `#b86f47` 作为上述变量的默认值。
- 验证手段：Vitest 解析 `tokens.css` 或挂载后 `getComputedStyle` 断言上述四值。

#### AC-2: 暗色色板与 EP primary

- 触发：设置 `data-theme="dark"`（经 theme store 或测试直接设置，与生产同一属性名）。
- 必须可观察：`[data-theme=dark]` 下精确为 `--bg-page: #102C31`、`--primary: #7A9C8F`、`--accent: #C46F4E`、`--el-color-primary: #7A9C8F`（大小写不敏感）；`--text-primary` 仍有定义且非透明/空。
- 验证手段：同 AC-1，在 dark 属性下断言。

#### AC-3: 主题切换边界

- 触发：通过 `ThemeControl` / theme store 在 `light` ↔ `dark` ↔ `system` 间切换。
- 必须可观察：每次切换后 `document.documentElement` 的 `data-theme` 为解析后的 `light` 或 `dark`；无抛错；AC-1/AC-2 所列关键变量在对应主题下仍为约定 hex。
- 验证手段：扩展或复用 `ThemeControl.spec.ts` / theme store 测试，并复用 AC-1/AC-2 的变量断言。

### Done When

- AC-1～AC-3 对应验证通过（含 Element Plus `--el-color-primary`）。
- 未通过「只改 public-theme 注释、tokens 仍为旧值」一类路径勾选完成。

## Feature Slice 2: 品牌栏（Logo / Favicon / Header）

- [x] Implementation status: Done on `feat/visual-theme-ui-v3`

### Behavior

- 公共 Header 品牌区（标 + 站点名）气质对齐主题1 规范：可沿用并微调 `BrandMark` + 现有站点名数据源，不引入与文档冲突的临时字标。
- Favicon / `frontend/public/brand/` 相关资源与规范一致（更新 `favicon.svg` / `mark.svg` 等现有入口，避免残留明显旧标）。
- 菜单视觉（字重、间距、色、hover/active）跟随新 token；**菜单文案与目标路由不变**。

### Public Interface

- UI：`PublicHeader`、`BrandMark`、`MobilePublicNav`（若承载品牌/菜单）。
- 静态资源：`frontend/public/brand/*`；若 HTML 入口引用 favicon，保持路径兼容或同步更新引用。
- 站点名：继续现有 app/site store 或配置来源，不硬编码主题稿假名覆盖 CMS。

### Error and Boundary Cases

- 窄屏：品牌区与折叠导航仍可用，不严重溢出或遮挡主内容入口。
- 暗色下品牌标与字对比度可读。

### Non-goals

- 不重构导航信息架构；不按主题稿改成 ABOUT/LEARN/THINK 等文案体系。
- 不强制替换为位图 Logo（SVG 优先）。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：挂载 `PublicHeader`（及品牌子组件）于公共布局上下文；读取 `frontend/public/brand/mark.svg` 与 `favicon.svg` 文本内容。
- 默认断言层级：组件 DOM（品牌链接、菜单 `to`）、brand SVG 源码字符串；调试落 `.tmp/ui-v3/`。
- 防 mock 逃逸禁令：不得用空 SVG、仅改文件名、或 data-URI 占位冒充已更新品牌资源；菜单断言必须核对真实 router path。

#### AC-4: 品牌区与 Favicon

- 触发：打开任意使用 `BaseLayout` / `PublicHeader` 的公共页（测试中挂载等价树）；并读取 `mark.svg` / `favicon.svg`。
- 必须可观察：Header 可见品牌标与站点名；`mark.svg` 与 `favicon.svg` 源码均包含 `#0F3D36` 与 `#C46F4E`（大小写不敏感）；二者均**不再**以旧强调色 `#b86f47` / `#B86F47` 作为填充或描边主色出现（允许历史注释，不允许作为 `fill`/`stroke`/`stop-color` 值）。
- 验证手段：`BrandMark.spec.ts` / `PublicHeader.spec.ts` + 对两个 SVG 文件的内容字符串断言。

#### AC-5: 导航 IA 不变

- 触发：渲染公共 Header 菜单。
- 必须可观察：既有入口（教程 / 博客 / 作品 / 英语 / 关于等，以当前产品菜单为准）仍指向原 path；无本轮新增/删除/改名菜单项。
- 验证手段：`PublicHeader.spec.ts`（或等价）断言链接列表与 path。

#### AC-6: 窄屏品牌与导航

- 触发：移动导航/窄屏 Header 路径（现有 `MobilePublicNav` 行为）。
- 必须可观察：仍可打开导航并到达原路由；品牌区不导致不可用遮挡。
- 验证手段：复用/扩展 `MobilePublicNav.spec.ts`；必要时目视窄屏（目视不单独作为通过条件）。

### Done When

- AC-4～AC-6 通过；菜单 path 快照与升级前 IA 一致（允许 class/样式变，不允许 path/文案 IA 变）。

## Feature Slice 3: 三页电影感 Hero

- [x] Implementation status: Done on `feat/visual-theme-ui-v3`

### Behavior

- **Home**（`/` → `HomeView`）：Hero 主视觉使用 `/brand/themes/home-hero.webp`（或同 stem `.png`）；边缘雾化/渐隐进页面；保留现有标题、简介、CTA 与 CMS/接口驱动能力；双栏壳可保留，但主视觉位不得仅靠 `EditorialMotif` 充当「已上主题图」。
- **English**（`/english` → `EnglishView`）：主视觉使用 `/brand/themes/english-hero.webp`（或同 stem `.png`）；原 `StarfallScene`（若占主视觉）替换或降级为次要装饰。
- **About**（`/about` → `AboutView`）：主视觉使用 `/brand/themes/about-hero.webp`（或同 stem `.png`）；同样处理 Starfall/冲突主视觉。
- 暗色下对大图压暗或加雾，保证叠加文案可读。
- 窄屏可改为单栏；大图仍完整可读，不依赖仅桌面可见的构图。
- 资源可经压缩/裁切；源文件分别为仓库根 `主题4.png` / `主题2.png` / `主题3.png`。
- **必须**实现可触发的加载失败降级 UI（例如 `img` `@error` 或等价状态）：失败时显示可读色块/既有 motif，文案与壳仍在。

### Public Interface

- 路由/页面：`/`、`/english`、`/about`。
- 静态资源（唯一目录）：`frontend/public/brand/themes/{home,english,about}-hero.webp`（允许同 stem `.png`）；运行时 URL 前缀为 `/brand/themes/`。
- 文案：继续现有页面文案与 site/内容接口；不要求为本轮新增后端字段。

### Error and Boundary Cases

- 主题图资源缺失或加载失败：页面不白屏；走上述强制降级 UI，并仍显示文案与导航。
- `prefers-reduced-motion: reduce`：不依赖动画才能理解 Hero 内容（动画可减弱或关闭）。
- 精选作品封面等 **非** Hero 主视觉槽位可继续用封面/`EditorialMotif`，不要求改为主题世界观图。

### Non-goals

- 不重做首页下方作品索引/更新流的信息架构。
- 不重做 English 子路由（词汇/听力等）的内页视觉。
- 不引入新的 CMS 媒体类型专管主题 Hero（本轮静态资源即可）。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：挂载或静态分析 `HomeView` / `EnglishView` / `AboutView` 对 `/brand/themes/*-hero.*` 的引用；确认 `frontend/public/brand/themes/` 下对应文件存在且非空（>1KB）；本地目视三路由 × 明暗。
- 默认断言层级：模板/样式中的资源 URL、磁盘文件字节长度；目视清单落 `.tmp/ui-v3/`（可选）。
- 防 mock 逃逸禁令：不得用 1×1 透明图、空 file、或仍只渲染 Motif/Starfall 作为主视觉却声称 AC 通过；断言路径必须命中 Global Constraints 钉死的文件名。
- 仓库仅 Vitest：无 E2E 时，资源引用 + 非空文件为自动化主证据；雾化/电影感/暗色压暗以清单式目视补充（见各 AC 降级理由）。

#### AC-7: Home Hero 主题4

- 触发：进入 `/`（测试挂载 `HomeView` 或断言其模板/样式引用）。
- 必须可观察：Hero **主视觉槽位**的 `<img src>` 或等价背景引用为 `/brand/themes/home-hero.webp` 或 `/brand/themes/home-hero.png`；对应磁盘文件存在且大小 >1KB；文案区标题/简介/主要 CTA 仍渲染（数据源可为空时的既有空态除外）。
- 验证手段：Vitest 断言上述 URL + `fs.stat` 大小 + 组件渲染；气质目视辅助。
- 降级理由：无 Playwright；电影感/雾化无法稳定截图像素断言，故自动化盯资源契约，气质靠目视清单。

#### AC-8: English / About Hero 主题2/3

- 触发：进入 `/english`、`/about`。
- 必须可观察：English 主视觉槽位引用 `/brand/themes/english-hero.webp|.png`；About 引用 `/brand/themes/about-hero.webp|.png`；二者磁盘文件存在且 >1KB；主视觉槽位不以 `StarfallScene` 为唯一主画面（可降级为次要装饰）。
- 验证手段：同 AC-7 针对两页。
- 降级理由：同 AC-7。

#### AC-9: 暗色 Hero 可读

- 触发：`data-theme=dark` 下打开三页。
- 必须可观察：Hero 区域存在可指认的压暗/遮罩/雾化处理（样式规则或 overlay 节点），文案叠在处理层之上仍可读；不得仅有空的 dark 选择器。
- 验证手段：断言 dark 相关样式或 overlay 节点存在且含非空 `opacity`/`background`/`filter` 等实际声明 + 目视。
- 降级理由：对比度无统一 E2E 工具；自动化断言「非空暗色处理」，完整可读性目视确认。

#### AC-10: 缺图与减动效边界

- 触发：（1）经公共失败入口触发 Hero 图加载失败（测试中 dispatch `img` `error` 事件，或传入必失败 `src`，二者须走到同一降级渲染路径）；（2）启用 `prefers-reduced-motion: reduce`。
- 必须可观察：（1）降级 UI 可见（色块或 motif），文案与页面壳仍在，不整页白屏；（2）Hero 文案与主 CTA 在无动画时仍完整可见。
- 验证手段：三页（或抽取的共享 Hero 子组件）必须暴露上述失败路径并由 Vitest 触发；断言降级节点出现；减动效断言相关 CSS `@media (prefers-reduced-motion: reduce)` 规则存在或动画被禁用。
- 降级理由：仓库无真实 HTTP 404 的浏览器 E2E；用同源 `error` 事件/失败 src 触发生产降级路径仍算公共入口等价物，但**禁止**因「未实现降级槽」而跳过本 AC。

### Done When

- AC-7～AC-10 通过；`frontend/public/brand/themes/` 下三份非空 Hero 资源可定位。
- 未将教程/博客/作品/后台视觉列入完成条件。

## Candidate Future Work（非本 Spec）

- 教程 / 博客 / 作品列表与详情的全站视觉扫尾。
- 后台视觉对齐。
- 导航文案/IA 按主题稿重构。
- 主题 Hero 改由 CMS/媒体库配置。
- Playwright 视觉回归。
