# 星雨笔录 V2

V2 使用独立目录、独立数据库 `star_rain_v2`、独立会话 Cookie `STAR_RAIN_V2_SESSION`。当前账户模块待人工验收，其余 Maven 模块保留框架。功能开发从 `mainV2` 分出分支，人工验收通过后再汇入 `mainV2`。

## 本地启动

- JDK 21、Maven、Node.js、MySQL 8。
- 在 MySQL 中创建独立数据库 `star_rain_v2`，使用 utf8mb4。
- 将 `backend/application-local-secret.example.yml` 复制为 `backend/application-local-secret.yml`，填写本地数据库与 SMTP 配置。实际凭据禁止入 Git。
- 初次设置 bootstrap 管理员配置；首次启动自动建立账户表、内置角色、权限与超级管理员。完成后关闭 `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED`。
- `STAR_RAIN_BOOTSTRAP_ADMIN_EMAIL` 是唯一超级管理员的身份邮箱；`STAR_RAIN_MAIL_FROM` 是发信邮箱，两者独立配置，初始化不得用发信邮箱替代管理员身份。
- 在 V2 目录运行 `./deploy/start-local.ps1 -Build`；已有构建时运行 `./deploy/start-local.ps1`。
- 前端：<http://127.0.0.1:5174>；后端：<http://127.0.0.1:8088>。页面 API 经 Vite 代理到后端。
- 停止服务：`./deploy/stop-local.ps1`。重建前先停止服务，避免 Windows 锁定运行中的 JAR。

## 本次初始化

当前开发机的本地配置已参考 V1 初始化。初始登录信息仅保存在本地 `.local/access.json`；若已修改密码，以本人当前密码为准。配置与运行输出均被 Git 忽略。数据库脚本位于 `backend/star-rain-boot/src/main/resources/db/migration/`，采用逻辑外键。当前迁移版本为 `2.019`。

## 账户验收

详细步骤在 `docs/开发文档/账户模块验收.md`。注册必须先获取并填写邮箱验证码，验证码有效期 10 分钟，重发冷却 60 秒。已有未验证账户可在“我的账户 → 验证账户邮箱”补充验证，无需重新注册。

角色与权限由数据库迁移预置，不提供任意勾选授权页面。注册只授予 USER；ADMIN 通过超级管理员邀请取得；唯一 SUPER_ADMIN 由初始化建立，不能通过注册或邀请取得。账户治理操作只开放给 SUPER_ADMIN。其余业务模块的 ADMIN 能力随对应模块实现。

超级管理员在“账户管理”搜索已注册普通用户，点击“邀请管理员”。“管理员邀请”页面提供记录、重发和撤销；邀请有效期 48 小时，重发冷却 60 秒，重发成功后旧链接失效。受邀用户登录自己的账户，在“我的账户 → 待处理邀请”接受，随后重新登录取得 ADMIN 身份。超级管理员可取消管理员身份，相关旧会话随即失效。

邮件状态“已提交发送”表示 SMTP 接受，不等于收件箱送达。发送失败返回错误；本机开发环境的邀请通知不包含回环地址链接，受邀用户直接从账户页面处理邀请。部署时设置可访问的 `STAR_RAIN_FRONTEND_ORIGIN`。

2026-10-02 前后端构建通过；本轮 45 项邀请与固定 RBAC 的 HTTP、数据库及本地 SMTP 检查通过，浏览器完成接受邀请、重新登录与记录回显。收件人已确认真实验证码邮件和新版邀请通知均收到。检查结果仅说明已验证场景，不代表整个模块已获人工验收。回归使用独立临时数据库，人工验收继续使用 `star_rain_v2`。

`docs/`、`.local/`、真实配置、构建输出不进入版本库。

## 后端分层与包命名规范（强制）

依据《Java开发手册（嵩山版）》第 44–45 页的分层原则，以及本项目多模块垂直切分的既有约束。

### 两条铁律

1. **一个包只放一类东西**。同一种角色不允许散落在不同深度。
2. **接口与实现必须分开**：接口放在层包下，实现放在同层的 `impl` 子包
   （`service/` + `service/impl/`、`storage/` + `storage/impl/`、`api/` + `api/impl/`）。
   没有接口时不要为了对称硬造接口。

禁止使用 `support`、`misc` 这类没有标准语义的包名。

### common 模块

| 包 | 放什么 |
| --- | --- |
| constant | 常量类 |
| context | 上下文类 |
| enumeration | 枚举 |
| exception | 自定义异常 |
| handler | 处理器（全局异常处理器等） |
| json | JSON 转换 |
| properties | `@ConfigurationProperties` 配置属性类 |
| result | 返回结果封装 |
| security | 跨模块安全契约 |
| utils | 工具类 |

### 业务模块（account / media / tutorial / ...）

| 包 | 放什么 |
| --- | --- |
| api (+dto) | 模块对外契约：只有接口 + 契约 DTO，不放实现 |
| config | `@Configuration` 配置类 |
| constant | 常量类（权限码、usageCode 等） |
| context | 上下文与主体对象 |
| controller | Controller |
| dto | 请求 DTO 与层间传输对象 |
| entity | 数据库实体 |
| enumeration | 枚举 |
| exception | 自定义异常与错误码 |
| handler | 处理器 |
| interceptor | 拦截器 / 过滤器 |
| mapper | MyBatis Mapper 接口 |
| properties | `@ConfigurationProperties` |
| security | 安全声明（URL 边界、权限码语义） |
| service (+impl) | 服务接口 + 实现 |
| storage (+impl) | 可替换的基础设施抽象 |
| utils | 无状态工具类 |
| vo | 视图对象 |

按需建包，不创建空层。新增类别必须先补进本规范再建包。

### 注释约定（用户约定优先）

《Java开发手册》要求类与接口方法使用 Javadoc，本项目按用户明确要求执行：
**使用 `//` 与 `/* */`，不写文档注释**；注释解释业务原因与不明显约束。

### 编码时须一并遵守的嵩山手册条目

- 对象级权限检查；外部输入校验类型、范围、长度与业务约束；SQL 参数绑定，无法绑定的标识符用允许列表。
- 消耗资源的操作（邮件、上传等）设置频率或数量限制。
- 日志用 SLF4J 占位符，不拼接字符串，不输出敏感信息。
- 业务唯一性在数据库建唯一索引；查询明确列名，避免 `SELECT *`。
- 单元测试遵循 AIR（自动化、独立、可重复），核心增量代码必须有测试。
### 硬性编码约定（强制）

- **禁止使用 Java `record`**。VO / DTO / 值对象一律用 Lombok POJO：
  `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`，
  访问器统一为 JavaBean 的 `getXxx()`。原因：record 的 `x()` 访问器不符合本项目的 POJO 约定，
  也无法与按 JavaBean 约定工作的库配合。
- **业务异常使用继承体系**：公共基类持有 `code / message / httpStatus`，每个业务错误一个子类放在 `exception` 包；
  不再新增「集中式静态工厂 + 通用 ApiException」写法。
- Entity 用 `@Data`（MyBatis-Plus 需要可变对象）。
