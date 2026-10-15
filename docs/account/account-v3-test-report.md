# Account V3.0 测试报告

日期：2026-10-15（Asia/Shanghai）。状态：**Freeze Candidate，未正式冻结**。

## 环境与证据

Windows、本机 Java 21、Maven、Node、MySQL 8.0.34。新建 loopback `127.0.0.1:13316` MySQL 数据目录，数据库为 `star_rain_ci`；账户全链路测试进一步创建随机 `sr_account_v3_test_*` schema，执行真实 Flyway 后清理。未读取本机业务库配置，未访问生产数据。

前端为 Vitest、Vue Test Utils、fake-indexeddb、Playwright；浏览器本地使用已安装 Chrome。默认附带的 Chromium 在此 Windows 环境报 spawn UNKNOWN，切换 `PLAYWRIGHT_CHANNEL=chrome` 后实际执行；CI 保留安装并使用 Chromium。浏览器替身回归与真实后端回归分开计数。

邮件覆盖包括真实 MIME/加密 Outbox/JavaMailSender 的可控失败，以及真实浏览器经 loopback SMTP 协议捕获邮件，并直接打开邮件中的实际重置链接、校验其 origin 与路径。后者不等于外部邮件服务投递验收。

运行证据保存在 `.local/account-v3/`（忽略提交），公开摘要保存于本目录 `account-v3-verification.json`。关键原始记录：`backend-verify.log`、`frontend-test.log`、`frontend-build.log`、`browser-mocked.log`、`browser-real.log`、`load.json`、`restore.json`、`protocol.json`；Java 明细为各模块 `target/surefire-reports/`。报告不写入密码、验证码或重置令牌。

## 命令与实际结果

最终结果已写入 [验证摘要](account-v3-verification.json)。以下均为本轮实测；生产项目单独列为 NOT VERIFIED。

| 检查 | 结果 |
| --- | --- |
| 前端单元/组件测试 | PASS：75 个文件，411 项通过 |
| 前端生产构建 | PASS：构建与体积检查通过 |
| Playwright 替身回归 | PASS：32 项通过 |
| Maven 全量 verify，含真实 MySQL | PASS：844 项通过，0 失败、0 错误、0 跳过；其中账户全链路 8 项 |
| Playwright 真实账户/后端 | PASS：6 项通过（含 2 项账户真实邮件流程） |
| 100 账户、4 并发本地适度负载 | PASS：100 个账户、400 请求，错误与 429 均为 0 |
| 隔离备份/恢复 | PARTIAL：107 账户、凭证/权限、3 份媒体一致；恢复登录与媒体读取通过，空学习表与生产备份不代表非空关联数据已验证 |
| GitHub Actions | NOT VERIFIED：已更新工作流，不能把本地通过视为远端 CI 全绿 |
| 正式 Nginx HTTPS / 外部 SMTP / 生产备份 | NOT VERIFIED：没有本轮生产连接和运行记录 |

本地负载为 4 并发、100 次登录与 300 次资料/学习/词汇读取，总耗时 10.543 秒；登录中位数/P95 为 380/437 ms，读取为 7/22 ms。Java 限定堆 384 MiB，测量时进程工作集峰值 371.8 MiB；11.06 秒测量窗口使用 41.5 CPU 秒，约 3.75 个逻辑核，测试机有 20 个逻辑处理器。这是本机轻量验收，不代表实际部署硬件已达标。

备份体积 1,350,146 字节，恢复至另一 schema 后，12 张关键表的行数与 checksum 一致；3 个媒体文件 SHA-256 一致。恢复应用上管理员/普通用户登录、个人中心、管理员列表和受保护媒体读取均 200，普通用户访问管理员 API 403。快照的计划、学习会话、学习进度、词汇记忆和英语写作表均为空，因此只证明空表结构及账户/媒体恢复，不能替代非空学习数据或生产恢复验收。

额外真实 HTTP 检查见 `protocol.json`：Session ID 登录时轮换、旧 ID 401；Cookie HttpOnly/Lax；错误 CSRF 的资料写入及缺失 CSRF 的登出均 403，未发生资料修改；正确登出 200，随后个人中心 401；四类公开 GET 200；已有/未知用户名的错误登录响应相同。这里使用本地 HTTP，未验证正式 Secure Cookie。

## AUTH-01～30

PASS 仅限表内明确说明的本地测试边界；PARTIAL 的剩余范围仍为 NOT VERIFIED。

| ID | 结果 | 证据与边界 |
| --- | --- | --- |
| AUTH-01 | PASS | `AccountV3IntegrationTest.registrationEnumerationSessionCsrfAndPublicAccess`：真实验证码→注册→USER→登录→退出；真实浏览器对应 `account-real.spec.js` |
| AUTH-02 | PASS | `verificationExpiryFailureBudgetAndConcurrentConsumption`：并发注册只有一个成功，无重复账户；重复邮箱需先提供有效验证码 |
| AUTH-03 | PASS | `registrationEnumerationSessionCsrfAndPublicAccess`：已注册/未注册响应完全相同，均无 existingAccount；相同发送代码路径 |
| AUTH-04 | PASS | `verificationExpiryFailureBudgetAndConcurrentConsumption`：错误、五次锁定、过期、一次性消费 |
| AUTH-05 | PARTIAL | `resetGenericRequestSmtpRetryConcurrentUseAndSessionInvalidation`、既有 `AuditUpgradeMapperTest`：入队事务、AES-GCM、失败重试、令牌不重复创建；外部 SMTP NOT VERIFIED |
| AUTH-06 | PASS | 真实用户名/邮箱登录、错误凭据统一；`atomicLoginBudgetsAcrossIpsAndIdentifiersRecover` |
| AUTH-07 | PASS | 16 个并行请求、不同来源、用户名/邮箱别名：10 个认证失败，6 个限流；窗口恢复后可登录 |
| AUTH-08 | PASS | 来源维度触发与另一来源不受影响；测试用受控 SQL 加速填满来源预算；不将它写成执行了 100 次恶意 BCrypt |
| AUTH-09 | PARTIAL | `TrustedProxyTest` 真实 Tomcat RemoteIpValve、账户请求伪造 XFF；正式 Nginx 边界仍 NOT VERIFIED |
| AUTH-10 | PASS | 真实安全链中登录前后 Session ID 变化 |
| AUTH-11 | PASS | 缺失/错误 CSRF 写入及缺失 CSRF 登出 403；匿名公开 GET 200；真实 HTTP `protocol.json` 与既有安全规则测试 |
| AUTH-12 | PASS | 真实改密/找回后旧密码 401、另一旧 Session 401、新密码 200 |
| AUTH-13 | PASS | 已知/未知邮箱申请返回相同状态和响应；HTTP 线程不查询账户存在性 |
| AUTH-14 | PASS | 并行重置仅一个 200、另一个 409；过期、无效和重复令牌拒绝；MySQL 行锁与条件更新 |
| AUTH-15 | PASS | 账户停用使旧 Session 失效且拒绝登录；重新启用后可登录 |
| AUTH-16 | PASS | 邀请接受、过期拒绝、重发、撤销后拒绝；真实控制器与数据库 |
| AUTH-17 | PASS | 最后一名启用超级管理员停用请求 409，账户保留 |
| AUTH-18 | PASS | 撤销 ADMIN 后旧 Session 401；管理员不能请求超级管理员账户管理接口 |
| AUTH-19 | PASS | 公开教程、博客、作品、站点 GET；浏览器公开页回归不依赖身份初始化 |
| AUTH-20 | PASS | 他人计划/任务/会话 ID、伪造 accountId 查询及取消动作均拒绝；数据库原计划保留。既有 `TutorialEvidenceIntegrationTest` 补充学习事实隔离 |
| AUTH-21 | PARTIAL | 私人英语写作读取/写入/删除/版本均拒绝；既有真实 MySQL 词汇与英语测试补充账户隔离。英语全部私有接口的逐项人工签字仍 NOT VERIFIED |
| AUTH-22 | PASS | USER / ADMIN 对超级管理员账户 API 403，后端为最终裁决 |
| AUTH-23 | PASS | Auth Store 乱序初始化、旧失败、登录后退出串行、旧资料回包、重复提交；旧身份 401 不影响新登录 |
| AUTH-24 | PASS | 共享 latest-query 测试及三个实际页面组件乱序回包，过期成功/失败/finally 不覆盖 |
| AUTH-25 | PASS | 发送中卸载、迟到失败不创建定时器；重复发送、失败重试、倒计时结束 |
| AUTH-26 | PASS | 有恢复稿退出确认、取消保留身份、不请求服务端；正常退出删除账户草稿 |
| AUTH-27 | PASS | 读取拒绝须选择继续/取消；清理失败在退出后明确提示并可重试；Quota 错误保留当前输入 |
| AUTH-28 | PASS | IndexedDB 账户/文档/字段隔离；退出后迟到写入、同账户重新登录的旧代数、跨标签共享代数均阻止复活 |
| AUTH-29 | NOT VERIFIED | **NOT VERIFIED**：正式 Nginx HTTPS、Cookie Secure、端口与防火墙、宝塔限制需要部署环境证据；本地 HTTP/代理单测不能替代 |
| AUTH-30 | PARTIAL | 本地隔离备份恢复演练结果以摘要为准；**生产备份及其隔离恢复 NOT VERIFIED**，不能据本地演练签署正式冻结 |

## 首轮问题与重跑

开发过程没有将首轮失败隐藏为通过：新迁移需接续已有 2.046 使用 2.047；旧测试固定版本断言同步；测试邮件健康检查改为禁用传输探测（SMTP 业务仍测试）；隔离库名按旧测试保护条件使用 star_rain_ci；限流恢复测试发现 Timestamp 时区偏移，生产代码改用 UTC LocalDateTime/数据库时钟；测试上下文在临时 schema 删除后及时关闭。修正后重新运行的报告才作为最终证据。

正式冻结仍需真实 CI、生产反代、外部 SMTP、实际部署资源与生产备份恢复记录，并由站点维护者签字。当前交付不是生产上线证明。
