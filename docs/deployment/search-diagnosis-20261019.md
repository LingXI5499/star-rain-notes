# 星雨笔录搜索收录排查

检查日期：2026-10-19，Asia/Shanghai。依据用户提供的 Search Console 截图、公开 HTTP GET、当前源码和受控浏览器实验。没有修改生产服务器或搜索平台账号。

## 结论

Google 并非完全没有收录网站。用户截图显示 16 个网页已编入索引，包含首页，首页上次抓取日期为 2026-10-09；截图中索引数量从约 120 降到 16，值得继续核对变更时间和索引报告。品牌词没有展示与网址未收录是不同问题。

本次确认了一项需要修正的抓取配置：robots.txt 禁止整个 `/api/`，但 Vue 挂载后依赖 `/api/public/` 加载公开正文。受控实验中，保留相同初始 HTML 和生产前端，屏蔽 API 后正文被“网络连接失败”替换；放行 `/api/public/` 后正文正常。应增加更具体的 `Allow: /api/public/`，保留对其余 API 和账号路径的排除。这个实验复现了渲染风险，并不等于已取得 Google 或百度当次实际抓取失败的日志。

截图中“被 noindex 标记排除”的 10 个公开教程示例，目前全部返回 HTTP 200、`index,follow`，没有 `X-Robots-Tag: noindex`。截图最后抓取日期均为 2026-09-23；这些记录与当前响应不一致，需用“测试实际网址”查看实时抓取及渲染结果，不能再以旧报告断定当前仍在输出 noindex。

## 当前公开响应

| 检查项 | 实测结果 |
| --- | --- |
| 首页、博客正文 | 200，`index,follow`，canonical 指向 `https://yulanlin.cn/` 下的对应地址 |
| 普通、Googlebot、Baiduspider User-Agent 抽查 | 首页和博客均可获取，返回内容一致；这不代表已验证真实蜘蛛 IP 的网络访问 |
| robots.txt | 200；当前 `Disallow: /api/` 会覆盖公开正文接口，需要增加公开接口例外 |
| sitemap.xml | 200；包含 2,260 个唯一网址 |
| `/blog/http-java-web`、`/blog/leetcode-20` | 301 到相应 `/blog/posts/…` 新网址；10 月 16 日记录的旧博客路由问题现已修正 |
| 不存在的新旧博客网址 | 404 |
| HTTP 首页 | 301 到 HTTPS |
| www 首页 | 200，canonical 已指向不带 www 的主域名；可进一步用 301 统一，但不能据此断定收录下降原因 |
| 首页原始 HTML | 只有简短介绍，没有通向教程、博客等模块的普通链接；导航依赖 JavaScript |

## 内容完整度

用户“已编入索引”截图中的 7 个 JavaSE 章节，本次均为 200、允许索引，并在 sitemap 内。但公开 API 的正文只包含 116–245 个字符，主要是“本章知识点”清单，HTML 正文与 API 一致，并非只在搜索快照中丢失了完整内容。

在 noindex 示例中，以下 3 页当前正文只有“待编写”：

- `/tutorials/java-enterprise-development/3-7`
- `/tutorials/java-ai-application-development/4-2`
- `/tutorials/java-ai-application-development/13-4`

应按章节实际完成度整理发布范围，先完善读者能够使用的解释、示例及练习，再申请收录；不应仅为增加字数填充文本，也不应为追求全站收录而统一移除所有排除规则。本次没有批量撤回、删除或改写教程内容。

首页可在服务端 HTML 快照中补充网站定位、作者介绍及教程、博客、作品等真实链接。这需要更新服务端快照生成逻辑并刷新快照，单独替换前端 dist 不会改变后端现有首页正文；本次没有把它标记为已完成。

## 宝塔修正抓取规则

本次包内 `robots-public-api.location.conf` 可以直接替换站点配置中已有的整个 `location = /robots.txt { … }`，不要再添加第二个同名 location。

1. 先备份当前站点 Nginx 配置。
2. 找到当前把 `/robots.txt` 转发给 `127.0.0.1:18088` 的精确 location，替换为提供的片段。
3. 在宝塔执行配置检查（服务器终端可用 `nginx -t`），通过后保存并重载 Nginx。
4. 打开 `https://yulanlin.cn/robots.txt`，确认同时包含 `Allow: /api/public/` 和 `Disallow: /api/`，并保留账号、站内搜索排除及正确 sitemap 地址。

此片段直接返回规则，无需更换 JAR 或重启 Java 服务。源码中的 `RobotsServiceImpl` 已同步增加例外，供以后正常构建后端时使用。只上传前端 ZIP 而不应用本片段，不会改变当前由后端提供的 robots.txt。

## Google 和百度后续验证

Google：在顶部网址检查栏输入一个已完成的公开内容网址，例如 `/tutorials/javascript/chapter-3-14`；点击“测试实际网址”，核对是否允许抓取和编入索引，并在测试结果中查看渲染 HTML、截图和加载的资源，确认正文存在、公开 API 没有因 robots 被屏蔽。确认后请求编入索引；再到 noindex 原因详情点击“验证修复情况”。账号、后台、站内搜索仍应排除。报告不会因为点击验证立即清零。

百度：用户已确认站点验证和 sitemap 提交完成。接下来在搜索资源平台使用“抓取诊断”检查首页和一个完整正文页，核对抓取是否成功、正文是否完整；结合索引量、抓取异常和服务器真实百度蜘蛛日志判断。Google 的截图不能证明百度的具体失败原因。

尚无搜索平台实时测试和真实蜘蛛日志，因此不能承诺本次规则修正就是索引下降的唯一原因，也不能承诺品牌词恢复日期。

## 依据

- [Google：网址检查工具及索引报告与实时测试的区别](https://support.google.com/webmasters/answer/9012289?hl=zh-Hans)
- [Google：JavaScript SEO 基础知识](https://developers.google.com/search/docs/crawling-indexing/javascript/javascript-seo-basics?hl=zh-cn)
- [Google：排查 JavaScript 搜索渲染问题](https://developers.google.com/search/docs/crawling-indexing/javascript/fix-search-javascript?hl=zh-cn)
- [Google：内容是否提供实质性、完整的解释](https://developers.google.com/search/docs/fundamentals/creating-helpful-content?hl=zh-cn)
- [Google：规范网址与重定向](https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls?hl=zh-cn)
- [百度：抓取诊断](https://ziyuan.baidu.com/crawltools/index)
