# Account V3.0 最终代码审计

日期：2026-10-15（Asia/Shanghai）。状态：**Account V3.0 Freeze Candidate**。

## 范围与源码基准

验收依据为用户提供的《账户体系.md》。本轮不新增业务功能，完成账户安全与稳定性收口。保留 Spring Security、Servlet Session、MySQL、MyBatis、Vue/Pinia、固定 USER/ADMIN/SUPER_ADMIN、公开注册和既有加密邮件队列。

实施前执行 `git fetch github main`，核实最新 main 为 `944071f8e07fdb6bd273923ba6136fc10c638c84`。原本地 HEAD 为 `ece7cb72401d7bf611da36256adec99f7e6aa6bb`，仅落后两个合并提交，文件内容一致；工作分支 `codex/account-v3-freeze` 已快进到该 main。用户原有未跟踪的 `docs/tutorials/` 未修改、未纳入提交。当前主线已含 V2_041～V2_046，因此本轮新增迁移使用 **V2_047**，没有改写历史 SQL。

## 实际缺口与修复

| 入口 / 源码位置 | 原行为 | 最终行为与依据 |
| --- | --- | --- |
| `EmailVerificationServiceImpl.send/sendCode`、`EmailVerificationCodeVO` | 已验证/停用邮箱返回 409，其他响应包含 existingAccount | 所有有效邮箱均执行同一验证码生成、BCrypt、行锁冷却、加密入队流程；响应仅含两个有效期字段。账户状态不参与匿名发送路径 |
| `AccountAuthServiceImpl.register` | 在验证邮箱所有权之前查询并报告重复身份 | 先校验验证码，再给出表单重复纠错；消费仍参与创建账户的事务。并发消费只有一方成功，失败不留下半成品 |
| `AccountAuthServiceImpl.login`、新增 `AccountThrottleServiceImpl` | 查询最近审计再认证，只有 IP 维度；并发请求可同时通过 | MySQL 独立事务按固定顺序锁定来源/身份计数，认证之前预占。用户名与邮箱解析到同一账户 ID，未知输入也有 HMAC 键；成功仅退还自己的预占次数 |
| `AccountThrottleServiceImpl.reserve` | 本轮测试中发现 JDBC Timestamp 受 JVM 时区转换影响 | 使用 JDBC LocalDateTime 直接读取 UTC DATETIME，并以数据库 UTC 时钟判断窗口；真实 MySQL 测试验证窗口恢复和成功退还 |
| `AccountAuthenticator.authenticate` | 未知/停用账户提前返回，未执行 BCrypt | 所有失败身份执行一次 BCrypt；返回统一 INVALID_CREDENTIALS，页面统一“用户名或密码错误” |
| `AccountAuthServiceImpl.requestPasswordReset`、`AccountMailServiceImpl.requestReset` | HTTP 线程判断邮箱是否存在；仅真实账户入队 | 先按来源和邮箱计数，再为所有邮箱执行相同的加密请求入队和审计。工作器异步解析账户；不存在/停用账户不发送重置邮件 |
| `MailOutbox.prepareReset/deliver` | 重置令牌在申请线程创建；需要保持既有重试与加密 | 账户加锁、一次性令牌创建和加密消息物化在同一事务提交；SMTP 重试复用原令牌。领取任务时校验消息快照、下次执行时间和有效期，避免旧快照重建令牌。旧四字段消息可继续读取 |
| `AccountAuthServiceImpl.confirmPasswordReset`、`AccountMapper.resetByHash` | 先锁令牌再锁账户 | 先读不可变归属，再按“账户→令牌”加锁，与邮件物化统一顺序。有效期、PENDING 状态、条件消费、auth_version 与事务均保留 |
| `authStore.js`、`shared/http.js` | 初始化、登录、刷新、退出及旧 401 回包可覆盖较新身份 | 身份代数隔离回包；登录/退出服务端请求按顺序执行；个人资料回包校验原身份；旧 401/CSRF 失败不清理新身份。退出失败可重试 |
| `useLatestAccountQuery` 及账户/邀请/审计列表 | 旧成功/失败/finally 可覆盖新查询 | 共用请求代数，同时检查身份和组件存活状态；只有最新查询可以更新 data/error/loading |
| `useEmailCode`、`AuthPage`、`AccountPage` | 验证码发送卸载后可启动定时器，部分写操作缺少重复保护 | 卸载/身份失效后忽略回包与失败，创建计时器前复查；表单与资料、密码、管理操作有 busy 防重，页面切换后旧表单不导航 |
| `authStore.logout`、`AdminShell.logout`、`AccountPrivacyNotice` | 读取/删除草稿错误被吞掉；后台外壳忽略取消 | 无法检查时明确选择继续退出或取消；清理失败不阻塞服务端退出，并显示跨路由保留的告警和重试入口。取消或网络失败不擅自清空身份/跳转 |
| `draftStore`、`EditorRecovery` | 写入可跨退出/重新登录；部分存储打开失败缓存拒绝结果 | 写入与迁移检查账户代数；代数和阻止状态通过浏览器存储协调标签页。异步摘要、事务查询和旧编辑器回调不能在清理后复活草稿。失败写入能被退出检查感知，存储恢复后可重试 |
| `application.yml`、`AccountMailServiceImpl` | 后端默认监听所有网卡；缺少限流密钥及邮件正式来源校验 | 后端默认仅 loopback；保留 NONE 与精确本机代理边界。Secure Cookie 环境必须有私密稳定限流密钥；启用发信时要求 HTTPS 公网来源 |

## 保留并回归的能力

注册默认 USER；BCrypt 强度 12；登录 Session ID 轮换；Session SecurityContext 持久化；CSRF；改密与启停后的 auth_version 失效；管理员邀请、重发、撤销和接受；最后一名启用超级管理员保护；固定权限码、方法鉴权和各业务资源所有权；验证码十分钟有效、每邮箱六十秒冷却、五次错误上限、一次性消费；AES-GCM 邮件 Outbox、受控重试和过期清理均继续使用。

没有修改编辑器内容模型、重做编辑器、替换认证框架、删除角色、清空数据库、限制第 101 个注册账户、引入 JWT/Redis/SSO/MFA。

## 验证边界

实际结果见 [测试报告](account-v3-test-report.md)，发布步骤见 [发布说明](account-v3-release.md)。本地单元测试、真实 MySQL、浏览器与 SMTP 捕获测试不能代替真实公网 HTTPS/Nginx、外部邮件服务或生产备份恢复验收。上述门槛没有证据前，不标记 FROZEN。

实施依据包括 [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) 的统一失败语义与账户维度限流，以及 [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html) 的通用申请响应、一次性令牌和会话失效要求。
