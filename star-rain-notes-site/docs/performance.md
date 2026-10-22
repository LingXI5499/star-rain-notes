# 静态网站性能实测

测试日期：2026-10-21。数据直接来自本目录的三个 Lighthouse JSON 报告；本次测试针对更新后的顶部直接输入搜索框。

| 页面 | Performance | Accessibility | Best Practices | SEO |
| --- | ---: | ---: | ---: | ---: |
| 首页 | 99 | 100 | 100 | 100 |
| 博客（Java、JavaScript 标签） | 100 | 100 | 100 | 100 |
| 教程章节 | 100 | 100 | 100 | 100 |

| 页面 | FCP | LCP | TBT | CLS |
| --- | ---: | ---: | ---: | ---: |
| 首页 | 1.2 s | 2.2 s | 0 ms | 0 |
| 博客（Java、JavaScript 标签） | 1.5 s | 1.5 s | 0 ms | 0.038 |
| 教程章节 | 1.2 s | 1.4 s | 0 ms | 0 |

环境：Lighthouse 13.5.0、Chrome 156、移动模拟 412 × 823、DPR 1.75、CPU 4 倍减速、模拟网络 RTT 150 ms 和吞吐 1,638.4 Kbps。独立静态服务器提供 dist，文本采用 Gzip，图片为本站的响应式 WebP，不调用原后端。

文档建议目标为 Performance ≥90、Accessibility ≥90、Best Practices ≥95、SEO ≥95，三个样本均达到。结果会受机器负载、网络和服务器配置影响，不能作为所有页面或线上访问的分数承诺。

主要优化：正文预生成 HTML；普通内容不水合 Vue；只在实际搜索时加载索引和 Fuse；首屏插画按宽度生成 WebP；静态文本压缩；图片预留尺寸；尊重减少动态效果偏好。

LCP 和 CLS 为实验室观测。TBT 不是 INP，尚无独立版本的真实用户 INP、CrUX 或 75 分位 Core Web Vitals 数据。

报告：[首页](lighthouse-home.report.html)、[博客](lighthouse-blog.report.html)、[章节](lighthouse-chapter.report.html)。截图：[首页移动端](home-mobile-preview.jpg)、[博客移动端](blog-mobile-preview.jpg)、[章节移动端](chapter-mobile-preview.jpg)。

复现时先使用独立静态预览启动 dist，在安装了 Chrome 的终端执行 Lighthouse CLI；参数为移动默认模拟、performance/accessibility/best-practices/seo 四类、JSON 与 HTML 输出。本次使用本项目 node_modules/lighthouse/cli/index.js，Chrome 标志为 --headless=new --disable-gpu。生成三个报告后可执行 node scripts/write-performance-report.mjs 更新本摘要。
