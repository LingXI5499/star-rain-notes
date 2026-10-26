# 静态部署与回滚

状态：本地开发与静态交付。没有执行生产部署、DNS 或证书变更。

1. 使用 Node 22.22+，在新项目执行 `npm ci`、`npm test`、`npm run build`。确认内容和构建校验都为 PASS。
2. 用 `npm run preview` 直接提供 `dist/`，检查首页、课程章节、博客、搜索、作品、英语和不存在的地址。源目录与原后端均不参与预览。
3. 检查 `site.json` 或 `SITE_URL` 是否对应正式访问域名；更换域名后重新构建。
4. 执行 `npm run package`，上传静态 ZIP 到独立发布目录，例如 `/www/wwwroot/star-rain-notes-site/releases/20261021/`，解压后应直接看到 `index.html`、`_astro/`、`tutorials/`、`blog/` 等目录。
5. 保留上一版本目录，以及原 Nginx 配置的备份。`current` 可指向某个独立版本目录；上传时不要覆盖当前版本中的一半文件。
6. 保留域名原有 HTTPS、证书和证书验证配置。在对应 server 中采用 `deploy/nginx-static.locations.conf` 的静态规则，明确去除原平台的 API 反向代理和 SPA 回退。本文件是需要合入现有 HTTPS server 的片段，不应直接替换整套 Nginx 配置。
7. 在服务器执行 `nginx -t`，通过后才执行 `nginx -s reload`。本次开发未在服务器执行这些命令。
8. 验收实际响应：已发布深层页面 200、无尾斜杠目录地址规范跳转、不存在地址 404、HTML 中包含正文、图片与脚本返回正确 MIME、Sitemap 和 robots 可访问、搜索仅请求本站静态索引。

回滚时把 `current` 恢复为上一版本目录；若修改过 Nginx，恢复备份并先运行 `nginx -t` 再 reload。旧版本与原平台目录应保留到新版本人工验收完成。没有数据库迁移、数据恢复或后端重启步骤。

带哈希的 `_astro` 资源可以长期缓存；固定名称的 HTML 使用重新验证策略。需要在 CDN 上另外确认实际缓存规则，不能把本地构建成功等同于线上缓存刷新成功。
