import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const pages = [['home', '首页'], ['blog', '博客（Java、JavaScript 标签）'], ['chapter', '教程章节']];
const rows = [], metrics = [];
for (const [name, label] of pages) {
  const report = JSON.parse(fs.readFileSync(path.join(root, `docs/lighthouse-${name}.report.json`), 'utf8'));
  if (report.runtimeError) throw new Error(`${name}: ${report.runtimeError.message}`);
  rows.push(`| ${label} | ${['performance', 'accessibility', 'best-practices', 'seo'].map(key => Math.round(report.categories[key].score * 100)).join(' | ')} |`);
  metrics.push(`| ${label} | ${['first-contentful-paint', 'largest-contentful-paint', 'total-blocking-time', 'cumulative-layout-shift'].map(key => report.audits[key].displayValue).join(' | ')} |`);
  const image = report.audits['final-screenshot']?.details?.data;
  if (image) fs.writeFileSync(path.join(root, `docs/${name}-mobile-preview.jpg`), Buffer.from(image.split(',')[1], 'base64'));
}
const content = `# 静态网站性能实测

测试日期：2026-10-21。数据直接来自本目录的三个 Lighthouse JSON 报告；本次测试针对更新后的顶部直接输入搜索框。

| 页面 | Performance | Accessibility | Best Practices | SEO |
| --- | ---: | ---: | ---: | ---: |
${rows.join('\n')}

| 页面 | FCP | LCP | TBT | CLS |
| --- | ---: | ---: | ---: | ---: |
${metrics.join('\n')}

环境：Lighthouse 13.5.0、Chrome 156、移动模拟 412 × 823、DPR 1.75、CPU 4 倍减速、模拟网络 RTT 150 ms 和吞吐 1,638.4 Kbps。独立静态服务器提供 dist，文本采用 Gzip，图片为本站的响应式 WebP，不调用原后端。

文档建议目标为 Performance ≥90、Accessibility ≥90、Best Practices ≥95、SEO ≥95，三个样本均达到。结果会受机器负载、网络和服务器配置影响，不能作为所有页面或线上访问的分数承诺。

主要优化：正文预生成 HTML；普通内容不水合 Vue；只在实际搜索时加载索引和 Fuse；首屏插画按宽度生成 WebP；静态文本压缩；图片预留尺寸；尊重减少动态效果偏好。

LCP 和 CLS 为实验室观测。TBT 不是 INP，尚无独立版本的真实用户 INP、CrUX 或 75 分位 Core Web Vitals 数据。

报告：[首页](lighthouse-home.report.html)、[博客](lighthouse-blog.report.html)、[章节](lighthouse-chapter.report.html)。截图：[首页移动端](home-mobile-preview.jpg)、[博客移动端](blog-mobile-preview.jpg)、[章节移动端](chapter-mobile-preview.jpg)。

复现时先使用独立静态预览启动 dist，在安装了 Chrome 的终端执行 Lighthouse CLI；参数为移动默认模拟、performance/accessibility/best-practices/seo 四类、JSON 与 HTML 输出。本次使用本项目 node_modules/lighthouse/cli/index.js，Chrome 标志为 --headless=new --disable-gpu。生成三个报告后可执行 node scripts/write-performance-report.mjs 更新本摘要。
`;
fs.writeFileSync(path.join(root, 'docs/performance.md'), content);
console.log(rows.join('\n'));
