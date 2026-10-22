# 本地交付验证

验证日期：2026-10-21。项目目录：`E:\star-rain\star-rain-notes-site`。本报告记录本地执行结果，不代表生产部署或真实用户流量验收。

## 交付范围

已生成首页、教程列表/课程/章节、博客列表/专栏/标签/归档/正文、作品列表/详情、英语词汇/语法、关于、公开留言、全站搜索、隐私、条款及 404。使用原站公开布局、样式和本地化资源，并按用户要求简化搜索交互。

公开内容迁移为 16 篇博客、39 门课程、2,180 章教程、10 个作品、60 个主题、7,053 条主题词汇条目、42 个语法课节和 1 条公开留言。条目数不是跨主题去重后的单词数。生产访问只需要静态文件；迁移工具不参与浏览和日常构建。

## 实际命令与结果

| 命令 | 执行结果 |
| --- | --- |
| `npm test` | 7 项通过：内容安全、日期/路径、独立静态服务器、专栏/标签组合、URL 恢复、真实词条的模糊搜索。测试可在首次构建前执行。 |
| `npm run build` | PASS；包含本地目录同步、内容校验、类型检查、静态生成和全站产物检查。 |
| 内容校验 | 2,347 个内容 URL、2,252 份 HTML 正文；无错误。 |
| Astro/TypeScript | 49 个文件，0 errors、0 warnings、0 hints。 |
| 静态产物检查 | 2,458 个 HTML 页面、313,357 个内部链接/资源引用、4,965 个图片引用、2,348 条搜索索引；无错误。 |
| `node scripts/verify-runtime.mjs http://127.0.0.1:4321/` | 31 项 HTTP 验证通过，覆盖各模块、CSS/JS/图片 MIME、压缩、索引、Sitemap、robots、301 和真实 404。HTML 不含 Vite 开发客户端。 |
| `npm run package` | 生成静态 ZIP、源码 ZIP 与 SHA-256 文件；脚本检查归档完整性，要求构建检查先通过。 |

完整数据：[内容检查](content-validation.json)、[链接和 SEO 检查](build-verification.json)、[独立运行检查](runtime-verification.json)、[迁移范围](import-report.json)。

## 视觉与交互验证

- 本地可视检查：首页、教程阅读三栏、教程目录、博客专栏和标签筛选。样式以原站为基准；本文不声称逐像素比较完全一致。
- 八个专栏全部保留，按实际宽度排列，溢出项进入“更多”。实际打开过较窄页面的更多菜单并完成专栏切换。
- Java Web 专栏显示 2 篇。2026-10-21 对原站匿名公开专题接口的只读核对显示，Web、设计模式、小说、世界观专栏的关联数均为 0；本地保持相同关联，不猜测补入文章。空专栏显示“该专栏暂无公开文章”。
- 标签浮层支持搜索、单选、多选、清空、应用、取消和键盘关闭。Java + JavaScript 实际显示 9 篇，刷新后仍保留条件；专栏切换后，浏览器后退可恢复全部文章。
- 教程列表局部搜索框已删除；分类切换实际验证 Java 全栈为 4 门、前端为 12 门、全部为 39 门。
- 顶部原有搜索位置直接输入；下拉层只有匹配结果。实际输入 `StrngBuilder` 可匹配 StringBuilder 章节；方向键选中并回车已打开正确章节。查询只读取本站静态索引，Fuse 在浏览器进行模糊匹配。
- [单输入框搜索截图](inline-search-preview.png)、[标签浮层截图](tag-filter-preview.png)、[教程列表截图](tutorials-preview.png)。

## Lighthouse 与 Web Vitals

原始 HTML/JSON 报告分别为 [首页](lighthouse-home.report.html)、[博客](lighthouse-blog.report.html)、[教程章节](lighthouse-chapter.report.html)。详细得分和指标见 [性能报告](performance.md)。

环境为 Lighthouse 13.5.0、Chrome 156；移动模拟 412 × 823，DPR 1.75，模拟网络 RTT 150 ms、吞吐 1,638.4 Kbps、CPU 4 倍减速。测试目标为独立的 `dist/` 静态预览，启用与部署建议对应的文本 Gzip；未连接原后端。

已提供自适应宽度的首屏 WebP 图片。仅搜索需要时加载 Fuse 和索引，普通正文不水合 Vue。CLS 和 LCP 为实验室观测；TBT 是实验室指标，不能当作 INP。尚无该独立版本的生产 CrUX、真实用户 INP 或 75 分位 Web Vitals 数据，不据此宣称线上达标。

## 独立性及交付边界

原仓库保持只读；本次对原仓库执行 `git diff --stat`、`git status --short` 未发现原有受跟踪文件变更。新项目存于独立子目录。浏览器访问不调用原项目 API；构建后的 JS 未发现 `/api/public` 或 Axios 业务请求依赖。静态预览进程仅运行 `scripts/serve.mjs` 并读取 `dist/`。

原站 4 门课程本身没有公开章节；部分已公开章节为知识点提纲，按真实内容保留。不存在下载失败或人为占位替代的正文。

尚未执行生产上传、Nginx 语法/重载、DNS、证书、CDN 刷新、Search Console 或真实设备验收。生产部署属于独立操作，步骤及回滚见 [部署说明](deployment.md) 与 `deploy/nginx-static.locations.conf`。本地预览 Node 进程不是线上部署依赖。
