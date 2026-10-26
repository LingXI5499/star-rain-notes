# 星雨笔录 Account V3.0 部署手册

适用于当前 Spring Boot + MySQL + Vue 单机部署。业务版本保持 2.0.1，账户状态为 **Account V3.0 Freeze Candidate**；本地测试通过不代表已经正式冻结或上线。

## 1. 部署包和前置条件

解压后目录为 `star-rain-account-v3/`：

```text
backend/star-rain-boot-2.0.1.jar      Java 21 可运行后端，内含 Flyway
frontend/dist/                      完整前端静态文件，包含编辑器资源和许可
deploy/                            私密配置、Nginx、systemd 的公开示例
docs/account/                      审计、测试、冻结候选发布说明与历史验证摘要
DEPLOY.md                          本手册
BUILD-MANIFEST.json                 本包实际 Git SHA、构建身份、制品摘要
SHA256SUMS                         包内逐文件 SHA-256 校验清单
LICENSE                            开源协议
```

服务器需要 Java 21、MySQL 8、现有 HTTPS/Nginx 和持久化媒体目录。服务器无需 Node、Maven 或源码。仅对已有 V2 数据库升级；V1 数据库不能直接使用本包。

ZIP 旁的 `.sha256` 用于上传后校验 ZIP；解压后在包根目录执行 `sha256sum -c SHA256SUMS` 校验全部文件。`BUILD-MANIFEST.json` 的 Git 提交必须与 GitHub 推送分支对应。包里不包含数据库导出、媒体业务数据、本机密钥或测试账号凭据。

本包前端使用仓库公开构建默认值。页脚域名、备案等 `VITE_*` 属于构建时公开设置；如需改动，应从相同提交设置这些公开变量后重建前端，并重新生成校验清单，不能把密码放进 VITE 变量。

## 2. 已有宝塔 Java 项目：升级顺序

优先沿用原来的宝塔 Java 项目、运行用户、工作目录、数据库和媒体路径。不要同时新增第二个 Java 守护进程，也不要将已有媒体迁往空目录。

1. 记录旧 JAR/dist、Java 启动参数、进程工作目录、数据库版本和媒体绝对路径。单独保存现有私密配置，尤其是 `STAR_RAIN_MAIL_OUTBOX_KEY`。
2. 暂停站点写入，备份数据库和整个媒体目录，并在隔离环境试恢复。保留旧制品和备份；账号、角色、凭证、学习数据、审计和排队邮件都不能清空。
3. 将本包上传到独立的发布目录并校验。停止原 Java 项目，替换 JAR 和整份 dist；只替换某些 JS 会留下旧入口与资源不一致的问题。
4. 对照下一节补齐私密配置，不要直接覆盖原配置。核实真实媒体路径、HTTPS 域名和前端 index.html 绝对路径。
5. 保持原进程工作目录，启动参数可显式指定私密配置：

   ```bash
   /实际的Java21路径/bin/java -Xms128m -Xmx384m \
     -jar /实际发布目录/backend/star-rain-boot-2.0.1.jar \
     --spring.config.import=file:/私密配置目录/application-local-secret.yml
   ```

   宝塔中分别填写 Java 路径、JAR、工作目录和 JVM/应用参数，避免把上述整行放错输入框。已有项目的内存参数可沿用；384 MiB 只是本地验收使用的最大堆，不是生产硬件保证。
6. 检查启动日志和 Flyway，成功后再更新/重载 Nginx并恢复写入。应用失败时先排障，不要删除迁移记录或执行清库。

## 3. 私密配置

参考 `deploy/application-local-secret.example.yml`，另存为服务器私密文件。运行用户需要读权限；例如 root 管理、应用组可读的 `0640`。不要把真实配置放进网站静态目录或 Git。

| 项目 | 升级要求 |
| --- | --- |
| 数据库 | 保留现有 `STAR_RAIN_DB_URL/USERNAME/PASSWORD`，不要填写隔离测试库。Flyway 用户须有本次 CREATE TABLE 权限 |
| 邮件加密密钥 | **必须保留原 `STAR_RAIN_MAIL_OUTBOX_KEY`**。不要为升级重新生成它，否则已有邮件无法解密 |
| 登录限流密钥 | 设置稳定高熵 `STAR_RAIN_AUTH_THROTTLE_KEY`（至少 32 字符），或沿用现有 Outbox 密钥回退。不能每次重启随机更换 |
| HTTPS/Cookie | `STAR_RAIN_COOKIE_SECURE=true`；HttpOnly、SameSite=Lax 由应用配置保留 |
| 站点来源 | `STAR_RAIN_FRONTEND_ORIGIN`、`STAR_RAIN_PUBLIC_BASE_URL` 填实际 HTTPS origin，例如 `https://notes.example.com`，不带路径、查询、凭据 |
| 前端 HTML | `STAR_RAIN_FRONTEND_INDEX_PATH` 指向本次整份 dist 的 `index.html` 绝对路径；SEO 后端会读取它 |
| 媒体 | `STAR_RAIN_MEDIA_ROOT` 必须继续指向原有用户媒体目录，运行用户需要读写权限 |
| 监听/代理 | 后端仅 `127.0.0.1:8088`；管理端口仅本机。单机 Nginx 使用 NATIVE，只信任 loopback 精确正则 |
| SMTP | 填可用主机、发件人、身份认证和授权码。465 通常用隐式 SSL；587 通常用 STARTTLS，按邮件服务实际要求选择，不能照搬错误组合 |
| Bootstrap | 已有管理员的站点保持 `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED=false`，不改原账号/密码 |
| 可选任务 | 示例关闭 IndexNow 和媒体孤儿清理；已有站点核对原策略后保留其原配置，不必照模板改变 |

仅全新安装需要生成新密钥：`openssl rand -base64 32` 用于 Outbox，`openssl rand -hex 32` 用于限流。**已有 Outbox 密钥的升级不执行替换**。首次创建超级管理员时填写真实管理员邮箱和符合策略的随机密码，临时开启 Bootstrap；创建成功后立即关闭，再重启确认。SMTP 发件人与管理员身份邮箱分别配置。

## 4. Nginx 与宝塔反代

`deploy/nginx-site.example.conf` 的目录和证书路径都是示例。宝塔已有 HTTPS 站点应保留现有域名/证书配置，合并对应 location；避免重复 location 和冲突的旧全站 SPA 回退。

将 `deploy/nginx-http-log-format.conf` 的 `log_format` 加到 Nginx 的 **http 块**（不能放 server 块），站点使用 `star_rain_safe` 日志格式。它不记录查询参数、请求体、Cookie 和 Referer，避免重置链接令牌落入访问日志。模板同时设置 Referrer-Policy。

主要路由必须保持：

- `/api/` 转发 `http://127.0.0.1:8088`；原样保留 Cookie、CSRF 头，设置 Host、X-Forwarded-Proto。
- `X-Forwarded-For` 使用 `$remote_addr` **覆盖**外来值，不使用任意客户端提供的值；不扩大 Tomcat 可信代理范围。
- `/assets/`、`/brand/`、`/legal/` 从 dist 提供，缺失文件返回 404；`/assets/` 不能回退为 HTML。
- 账号及管理路由（如 `/login`、`/center`、`/learning`、`/blog/manage`）、英语、协议、搜索、作品原型 live 页面使用 SPA 回退；公开首页/教程/博客/作品、robots、sitemap 交给后端 SEO，保留真实 404。
- 媒体只能经过 `/api` 权限入口，不能公开整个媒体目录；管理端口和 `/actuator/` 不代理到公网。

检查并重载：`nginx -t` 成功后才运行 `systemctl reload nginx`，宝塔也可使用面板的配置检查/重载。若现有部署还有其他可信代理、CDN或多机链路，此模板不直接适用，应先核对真实边界，不能直接扩大 XFF 信任。

## 5. 可选 systemd 布局

仅已经使用 systemd，或确实由 systemd 管理服务时采用。宝塔托管 Java 时跳过此节。

建议发布目录 `/srv/star-rain/releases/<发布标识>/star-rain-account-v3`，`/srv/star-rain/current` 指向当前整包；媒体独立保存在原有持久化目录。私密配置保存于 `/etc/star-rain/application-local-secret.yml`。先创建应用用户/组和目录，确保包可读、媒体可写，然后调整 `deploy/star-rain.service` 的 Java、JAR、工作目录和私密配置路径。

把服务文件安装到 `/etc/systemd/system/star-rain.service` 后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now star-rain
sudo systemctl status star-rain --no-pager
sudo journalctl -u star-rain -n 100 --no-pager
```

升级时先停服务，再把 current 链接切向已校验的新目录，然后启动。不要覆盖未知的真实目录；保留上一个发布目录供回滚。使用应用运行用户检查媒体目录权限，不能用 `chmod -R 777` 解决问题。

## 6. 数据库迁移、启动与验收

本轮仅新增 `V2_047__account_request_throttle.sql` 创建短期限流表；完整 JAR 含所有历史迁移。不要手工执行部分 SQL，也不要改 V2_001～V2_046。数据库必须是原 V2 数据库，启动由 Flyway 校验并迁移。若站点落后于 2.046，本次还会执行包内尚未应用的历史迁移，应先在备份副本试升级。

服务器本机先检查（域名示例需替换）：

```bash
java -version
curl --fail http://127.0.0.1:18089/actuator/health/readiness
curl --fail https://notes.example.com/api/auth/csrf
curl --fail https://notes.example.com/api/public/site/config
```

按顺序在实际 HTTPS 浏览器验收：

1. 匿名教程/博客/作品可读；不存在的公开内容仍为 404。
2. 登录前后 Session ID 变化；Cookie 为 Secure、HttpOnly、SameSite=Lax；刷新后仍登录。
3. 个人中心修改显示名，缺失/错误 CSRF 写入被拒绝；正常登出后个人接口 401。
4. 测试邮箱完成验证码注册和找回；邮件链接为实际 HTTPS 域名下 `/reset-password`；旧密码/旧会话失效。
5. 超管查询账户、邀请/撤销/接受管理员、启停测试账号；普通用户直接访问管理员接口 403；最后一名超管不能停用。
6. 编辑器输入、手动保存、上传和媒体读取正常；有本地恢复稿退出时有明确选择，清理失败提示可重试。
7. 确认 8088、MySQL 与管理端口未直接对公网开放，宝塔面板访问受限；伪造 XFF 不能改变真实源 IP。

邮件“请求已受理”只代表安全入队。投递失败查看任务 ID、异常类别及队列 attempts/next_attempt_at/expires_at；不要把验证码、token、SMTP 密码或解密正文写入排障日志。

## 7. 回滚与冻结

停止新写入与应用，切回保留的旧 JAR/dist 和匹配配置。新增限流表可以保留，禁止删除 Flyway 历史。新队列中未物化的 `RESET_REQUEST` 不能交给旧工作器，回退前应在新版本排空；无法排空时按已验证备份恢复方案处理，不删除邮件来假装成功。需要回退数据库状态时，使用升级前备份，并明确备份之后新数据如何处理。

本地已通过的 844 项后端、411 项前端、32 项浏览器回归、6 项真实后端浏览器测试及本地恢复记录见包内报告。正式环境 HTTPS/Nginx、外部 SMTP、真实 CI、生产数据备份恢复和实际硬件验收仍须记录。全部门槛完成并由站点维护者确认后，才将状态从 Freeze Candidate 改为 FROZEN。

## 路由统一后的部署补充

使用当前 `nginx-site.example.conf` 的 location 顺序，替换旧前缀 SPA 规则：旧地址 308 重定向 → 静态资源真实 404 → 认证/个人/后台 SPA → 作品 live SPA → 公开英语/协议/搜索 SPA → 后端公开 SEO。管理页 `/blog/manage`、`/tutorials/manage`、`/portfolio/manage` 及编辑/预览/章节管理路径必须先于公开内容网关；不能仅删除旧规则。SPA 直接读取 index 文件，避免内部跳转丢失私有页的 X-Robots-Tag。

新重置邮件为 `/reset-password?token=...`，新邀请邮件为 `/invitation/accept?token=...`；异步发件箱同样使用新路径。已有旧链接保留兼容。只在用户批准实际部署后应用配置，先 `nginx -t` 再 reload，并核对公开 HTML、真实 404、规范跳转及 query/hash；详细清单见 [路由统一说明](route-unification.md)。
