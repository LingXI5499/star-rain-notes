# Account V3.0 发布与冻结候选说明

**功能不增加，安全与稳定性收口，Account V3.0 Freeze Candidate。**

本轮已完成本地 844 项后端测试、411 项前端测试、32 项浏览器回归、6 项真实后端浏览器测试，以及 100 账户负载和有限范围的隔离恢复演练。原始制品摘要见 [验证记录](account-v3-verification.json)。

本轮代码状态与实测结果以 [审计](account-v3-final-audit.md)、[测试报告](account-v3-test-report.md) 和验证摘要为准。生产发布、真实 SMTP、公网反代与生产恢复尚未验收时，状态保持 Freeze Candidate，不标记 FROZEN。

## 配置与制品

正式发布前从最终提交重新构建，设置 `STAR_RAIN_BUILD_COMMIT` 为提交 SHA 并归档 JAR/dist 摘要。本轮实测制品是在分批提交前的开发工作树构建，验证摘要保留其真实构建标记，不把它冒充已发布制品。

仍部署同源前端 `dist/` 与 Java 21 Spring Boot JAR。既有数据不迁移到新认证体系，不删除任何账户、角色、密码摘要、关联学习记录、媒体和审计。

| 配置 | 要求 |
| --- | --- |
| `STAR_RAIN_DB_*` | 仅服务器私密配置提供；CI/本地验收使用独立 MySQL。不能将测试指向生产 |
| `STAR_RAIN_AUTH_THROTTLE_KEY` | 至少 32 字符的高熵私密随机值；缺省回退到 `STAR_RAIN_MAIL_OUTBOX_KEY`。Secure Cookie 环境缺失会拒绝启动；开发缺失时使用临时密钥并告警 |
| `STAR_RAIN_MAIL_OUTBOX_KEY` | Base64 编码的 32 字节 AES 密钥。升级必须保留原密钥，否则存量加密邮件无法解密。密钥轮换另行运维，不在本轮自动执行 |
| `STAR_RAIN_LOGIN_SOURCE_LIMIT` | 默认 100 次，包含尚未完成的认证预占；成功只退还本次次数 |
| `STAR_RAIN_LOGIN_IDENTITY_LIMIT` | 默认 10 次；用户名/邮箱共享同一账户额度；不会永久停用账户 |
| `STAR_RAIN_LOGIN_WINDOW_SECONDS` | 默认 900 秒。重置申请另有来源 30 次/小时、邮箱 3 次/小时，包含未知账户 |
| `STAR_RAIN_COOKIE_SECURE` | 正式 HTTPS 环境必须 true；Session Cookie 继续 HttpOnly、SameSite=Lax |
| `STAR_RAIN_FRONTEND_ORIGIN` | 实际公网 HTTPS origin，不能带路径/凭据/查询/片段；重置地址为该来源下 `/useradmin/reset-password` |
| SMTP / MAIL_FROM | 私密 SMTP 配置与可用发件人；“请求已受理”仅表示安全入队，不保证送达 |
| `STAR_RAIN_SERVER_ADDRESS` | 默认 127.0.0.1；不得暴露 8088 或管理端口。MySQL 及宝塔面板也应限制访问 |
| `STAR_RAIN_FORWARD_HEADERS_STRATEGY` | 默认 NONE；单机 Nginx 部署经核对后启用 NATIVE |
| `STAR_RAIN_TRUSTED_PROXIES` | 只信任 loopback 的精确正则；Nginx 使用 `X-Forwarded-For $remote_addr` 覆盖外来值，不能信任任意来源的转发头 |
| `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED` | 已初始化站点保持 false，不要重新初始化管理员 |

账号计数键是分域 HMAC，不含明文用户名、邮箱或密码；短期计数定时清理，审计保留。邮件失败日志只包含任务 ID 和异常类别；按 Outbox attempts、next_attempt_at、expires_at 与服务告警排障，不打印解密正文、验证码、token 或 SMTP 密码。

## 迁移与变更范围

新增 **V2_047__account_request_throttle.sql**，仅创建 `sr_login_throttle`；V2_001～V2_046 保持原文。Flyway 启动时校验并执行。测试中原有三个固定迁移版本断言同步为 2.047。

主要变更位于后端账户认证/邮箱/Outbox/Mapper、限流服务及配置；前端 Auth Store、账户三个管理列表、验证码 Hook、个人账户与身份入口；草稿仅修改必要的写入隔离与退出反馈。新增 MySQL 全链路测试、前端竞态测试、真实浏览器账户测试及 loopback SMTP 捕获器。CI 的真实后端作业启用隔离 SMTP 捕获、指定实际前端来源与非 TLS 本地传输，并继续要求零跳过数据库测试。公开源码检查仅新增账户 Markdown 报告目录和这一个固定 JSON 证据路径的许可，所有内容继续执行私密值扫描。

## 发布前验收

1. 在服务器备份 MySQL（含角色、凭证、Flyway、Outbox、审计）和整个持久化媒体目录，记录旧 JAR/dist、配置、密钥及权限。备份存放于受控位置，不能入 Git。
2. 在隔离环境恢复该备份，验证用户数、角色分配、密码摘要与关联学习/英语/媒体记录，使用测试账户确认可登录。不能只检查 SQL 导入退出码。
3. 上传经过测试的完整 JAR/dist，保留既有媒体路径、Outbox 密钥和全部私密配置，重启后确认 Flyway 2.047，后台仍只监听 loopback。
4. 经实际 Nginx HTTPS 请求 `/api/auth/csrf`、登录、个人中心、写操作、退出、管理员列表；核对 Session Cookie、CSRF、会话 ID 轮换、真实 IP 和伪造转发头。匿名教程/博客/作品继续可读。
5. 在非生产隔离环境用受控测试邮箱完成一次真实外部 SMTP 注册/找回，验证链接来源、邮件内容和过期；本地 SMTP 捕获器不代替这一步。
6. 在实际单机部署环境用约 100 个测试账户进行适度登录/资料/学习记录访问，记录资源、延迟、429 与错误率。不得关闭 BCrypt/CSRF/权限。

## 回滚

停止接收账户写入并停止新应用，再恢复旧 JAR/dist 与对应配置。新增限流表可以保留供重新升级使用；不能删除 Flyway 历史或清库来回滚。

新版本队列兼容读取旧四字段消息，物化完成的普通消息也保持旧结构。**尚未物化的 RESET_REQUEST 是新格式**：回退旧工作器之前必须在新版本排空这些请求；若无法排空，保留备份并按已验证恢复方案处理，不能删除排队邮件冒充完成回滚。涉及数据状态回退时，只能使用经过恢复验证的升级前备份，并明确备份之后新增业务数据的处置。

## 冻结条件

负责人：站点维护者，正式冻结时签字确认。代码和本地证据完成后仍需补齐测试报告中 NOT VERIFIED 项、真实 CI 运行链接、正式代理/SMTP/备份恢复记录，才允许将状态改为 **FROZEN**。

冻结后仅因已证实安全问题、影响业务的生产故障或受支持版本更新需求重新维护，不追加一般后台功能。生产操作没有本轮运行证据，不宣称已经上线或完成正式冻结。
