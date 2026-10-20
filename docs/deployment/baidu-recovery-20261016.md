# 百度收录排查与旧博客跳转修复

2026-10-16，Asia/Shanghai。

## 已确认的问题

本地保留的昨日配置 `.local/account-v3-config-review/nginx-site.ready.conf`（2026-10-15 21:29）只把 `/blog`、`/blog/archive`、`/blog/posts/{slug}`、`/blog/topics/{slug}` 转发给后端，遗漏 V1 的 `/blog/{slug}`。旧链接落入最后的 Vue `try_files`，返回没有文章正文和 Canonical 的空壳 200。

本次线上 GET 抽查：

| 网址路径 | 实测响应 |
| --- | --- |
| `/blog/http-java-web` | 200，无 Location、正文 main、Canonical |
| `/blog/leetcode-20` | 200，无 Location、正文 main、Canonical |
| `/blog/posts/http-java-web` | 200，有对应正文及 Canonical |
| `/blog/posts/leetcode-20` | 200，有对应正文及 Canonical |
| `/blog/posts/seo-absent-20261016` | 404 |

后端现有 `LegacyBlogRedirectController` 已为有对应已发布正文的旧地址提供 301；不存在的正文返回 404。修正 Nginx 路由即可让该逻辑接收到旧链接请求，不需要为此更换 JAR、前端或数据库。

该问题可能影响旧链接的抓取与迁移，但不能仅凭它断定百度搜索消失的唯一原因。索引量、真实百度蜘蛛抓取、搜索排名仍需站长后台和服务器日志确认。

## 在宝塔修改一行

打开对应 Java 项目/网站的 Nginx 配置，先保存当前配置备份。找到“公开内容页面由后端输出SEO HTML”下这一行：

```nginx
location ~ "^/(?:blog(?:/archive|/posts/[^/]+|/topics/[^/]+)?|tutorials(?:/[^/]+)?(?:/[^/]+)?|portfolio(?:/[^/]+)?|about|messages)$" {
```

替换为：

```nginx
location ~ "^/(?:blog(?:/[^/]+|/posts/[^/]+|/topics/[^/]+)?|tutorials(?:/[^/]+)?(?:/[^/]+)?|portfolio(?:/[^/]+)?|about|messages)$" {
```

变化仅为博客分支的 `/archive` 改为 `/[^/]+`，同时覆盖归档与旧文章；此 location 内的 `proxy_pass http://127.0.0.1:18088;` 和其它设置保持原样。保存，通过宝塔配置检查后重载 Nginx。服务器上也可先运行 `nginx -t`，成功后再 `nginx -s reload`。

交付目录中的 `nginx-site.fixed.conf` 基于昨日保存的整份配置，仅替换上述一行；`nginx-site.diff` 可核对差异。优先在当前配置改这一行，避免覆盖昨日之后其它配置变动。无需新增全站 rewrite，也不要把不存在的文章统一跳到首页。

## 修复后验证

在服务器终端执行以下 GET 检查，不加 `-L`，以便看到第一跳状态：

```bash
curl -sS -D - -o /dev/null https://yulanlin.cn/blog/http-java-web
curl -sS -D - -o /dev/null https://yulanlin.cn/blog/leetcode-20
curl -sS -D - -o /dev/null https://yulanlin.cn/blog/posts/http-java-web
curl -sS -D - -o /dev/null https://yulanlin.cn/blog/seo-absent-20261016
```

前两个应分别是 301，Location 指向对应的 `/blog/posts/http-java-web`、`/blog/posts/leetcode-20`（相对路径也有效）；新文章是 200；不存在的旧文章应为 404。再确认 `/blog`、`/blog/archive`、教程和作品仍正常。若旧文章仍为 200，说明新规则未生效或另有更优先的规则；若为 404，应进一步检查实际运行应用和对应正文，不能以浏览器最终打开页面代替第一跳状态检查。

## 百度后台的下一步

1. 使用原百度搜索资源平台站点，确认所有权验证仍有效。
2. 通过账号当前开放的提交渠道核对/提交 `https://yulanlin.cn/sitemap.xml` 和首页、重要正文。
3. 在“网站改版”中提交已验证生效的新旧 URL 关系，例如 `/blog/http-java-web` → `/blog/posts/http-java-web`。只有路径发生变化的内容使用该工具。
4. 对照索引量、抓取异常和搜索展现变化，确认下降发生时间。长期保留有效 301；提交成功不等于恢复收录，不能承诺搜索恢复日期。

首页原始 HTML 当前只有简短介绍，后续可补充教程、博客等语义链接。这是抓取入口优化，不是修复旧链接的前置条件；本次配置修复不修改首页内容。

依据：[Nginx location 匹配规则](https://nginx.org/en/docs/http/ngx_http_core_module.html#location)、[百度网站改版说明](https://ziyuan.baidu.com/rewrite)。

## 验证边界

本地核对单行差异及路由匹配范围，并完成上述线上修复前的 GET 抽查。本次没有修改服务器、重载生产 Nginx或登录百度提交；生产 `nginx -t` 和修复后响应需在应用配置后确认。
