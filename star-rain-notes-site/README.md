# 星雨笔录 · 独立静态展示站

Astro 7 + Vue 3 + TypeScript。页面在构建时生成 HTML；发布目录只包含静态文件，不需要后端、数据库或线上 Node 服务。

视觉基准为 2026-10-21 线上星雨笔录：复用原版公共布局、主题、组件样式、插画和真实公开内容。保留原课程、章节、文章、作品 slug。标签名中的多余反引号及首尾引号已统一去除，slug 和计数保留。

## 启动

要求 Node.js 22.22+，本次环境为 Node 24.15.0、npm 11.12.1。

```powershell
cd E:\star-rain\star-rain-notes-site
npm ci
npm run dev
```

浏览器打开 `http://127.0.0.1:4321/`。开发服务器只用于本地编辑预览。

## 校验与生成

```powershell
npm run validate
npm run check
npm test
npm run build
```

`build` 依次执行本地目录索引更新、内容校验、Astro/TypeScript 检查、静态生成、全部产物的链接与 SEO 检查。错误会阻止构建成功。

```powershell
# 先停止占用 4321 的开发服务器，再预览最终产物
npm run preview
```

此预览只读取 `dist/`，不运行 Astro，不读取原项目，不请求任何后端。不存在的地址返回 404；无尾部斜杠的目录地址返回 301。也可设置 `$env:PORT='4322'` 使用其他端口。

## 内容与维护

- 博客：`src/content-data/blogs/<slug>/meta.json` 与 `body.html`。
- 教程：`src/content-data/tutorials/<slug>/course.json` 管理目录，`chapters/<slug>.html` 保存章节正文。
- 作品：`portfolio/<slug>/meta.json`、`body.html`，区块内容保存于 `sections.json`。
- 英语：`english/vocabulary/<id>.json`、`english/themes.json`，以及 `english/grammar/`。
- 个人资料：`profile.json`、`profile-view.json` 及 `profile/` 下各段 HTML。
- 品牌与图片：`public/brand/`、`public/images/content/`。
- 站点名称、域名和默认分享图：`src/content-data/site.json`。

正常修改只需编辑这些本地文件，然后构建。`catalog.json` 是由各篇元数据自动派生的目录，不需要手工维护。首页精选顺序由 `home.json` 管理。新文章和课程要保持元数据与正文文件引用一致。

HTML 只用于维护者审核后的正文，不支持嵌入脚本、事件处理器、表单或第三方 iframe。构建再次消毒正文并生成目录、代码高亮及公式。图片使用本站绝对路径。仅维护正文时不需要运行迁移脚本。

## 路由与交互

首页、教程目录/课程/章节、博客时间线/文章/标签/专题/归档、作品列表/详情、英语词汇/语法、关于、留言、搜索、隐私、条款和 404 均独立生成 HTML。

教程和词汇筛选、分页、目录锚点、代码复制、深浅主题、本地搜索、顶部搜索和移动导航在浏览器运行。Vue 仅用于搜索页面；普通页面由 Astro 输出静态 HTML。教程列表已删除局部搜索框；可直接在顶部搜索框输入，下面只展示模糊匹配结果，支持方向键选择、回车打开、Esc 收起及 Ctrl K 聚焦。

博客专栏全部保留，按实际宽度显示，剩余项收进“更多”。专栏、标签、月份在当前页面组合筛选；标签弹窗支持名称搜索、单选、多选（命中任一）、清空和应用。条件保存在 URL 中，刷新及前进后退可恢复，筛选后会重新分页。Web、设计模式、小说和世界观专栏的原公开关联数为 0，显示真实空结果；不根据标题猜测并修改文章归属。

为遵守开发文档，去除了账号、后台、学习计划、复习算法、个人进度、阅读与写作业务、在线留言提交和 GitHub 自动同步。留言页的原表单区域改为真实联系方式。英语的原版视觉保留，但入口只指向词汇和语法。

## 域名与 SEO

默认正式域名为 `https://yulanlin.cn`。若部署到其他域名，请修改 `site.json` 的 `url`，或在构建前设置：

```powershell
$env:SITE_URL='https://your-domain.example'
npm run build
```

Canonical、Open Graph、结构化数据、robots 与 Sitemap 会使用同一域名。搜索和协议页面设置 `noindex,follow` 并从 Sitemap 排除；404 不进入 Sitemap。不以 robots.txt 阻止爬虫读取搜索页的 noindex。

## 部署与回滚

详见 `docs/deployment.md` 与 `deploy/nginx-static.locations.conf`。生产服务器只上传 `dist/` 的内容，使用独立目录；保留现有域名 HTTPS 和证书配置。开发阶段未更改服务器、DNS、证书或原平台部署。

```powershell
# 构建校验通过后生成源码 ZIP 与静态 ZIP，要求 Python 3
npm run package
```

输出 `artifacts/star-rain-notes-site-static.zip`、`star-rain-notes-site-source.zip` 和 `SHA256SUMS.txt`。静态 ZIP 根目录即为可部署网站；源码 ZIP 排除依赖、缓存、临时参考服务器与构建产物。

## 实际迁移范围与验证报告

真实内容：16 篇博客、39 门课程、2,180 个章节、10 个作品、60 个词汇主题、7,053 条主题词汇、42 个语法课节、1 条公开留言、个人介绍及公开社交链接。31 张内容图片已本地化，另复用品牌资源。

原站 4 门课程尚未发布章节，保留真实课程目录并如实说明。部分已公开章节原本为提纲，按原内容保留，没有补写或虚构文章。

- `docs/import-report.json`：迁移数量、空课程与空章节。
- `docs/content-validation.json`：元数据、日期、正文、图片与引用检查。
- `docs/build-verification.json`：全站 HTML、资源与内部链接、SEO、索引检查。
- `docs/verification.md`：实际执行结果及验证边界。
- `docs/performance.md`：移动端 Lighthouse 实测摘要及原始报告入口。
- `docs/page-inventory.md`、`route-inventory.md`、`visual-reference.md`、`content-inventory.md`：实施前审计。

## 一次性迁移工具

`scripts/download-public.py` 只读获取匿名公开内容，`download-assets.py` 下载公开图片。`import-public.mjs` 把已缓存的原公开内容转换成本地文件。这些工具不参与日常构建和线上访问，也不是自动同步任务。再次导入会重新生成内容，因此手工修改前应保留备份，不要将其作为正常发布命令。

源码目录 `src/styles/original/` 保存从原站公共组件迁移的样式。保留了原 Vue scoped 样式的选择器优先级，避免静态化后被公共规则覆盖。不要重新运行原平台的部署脚本。
