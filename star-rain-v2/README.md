# 星雨笔录 V2 · v2.0.0

V2 的首个正式源码版本，采用 Vue 3 前端与 Spring Boot 多模块后端，提供教程与学习计划、博客与主题、作品与静态原型、英语学习、账户管理、媒体、全站搜索与 SEO。

V2 使用独立目录、独立数据库 `star_rain_v2`、独立会话 Cookie `STAR_RAIN_V2_SESSION`。`mainV2` 为 V2 集成分支，GitHub 默认分支 `main` 为正式发布主分支，首版标签为 `v2.0.0`。更新内容见 [CHANGELOG.md](CHANGELOG.md)。

## 首版功能

- 教程目录、章节、问题与练习；学习记录、掌握度、复习、学习计划与任务。
- 博客主题、标签、时间线与 Markdown 阅读；作品内容区块、展示模板与隔离静态原型。
- 英语主题词库、发音、学习计划与评价；语法课程、双语阅读与私有写作，管理员原创可按权限公开。
- 邮箱验证、管理员邀请、固定角色权限、账户治理与媒体管理。
- 全站搜索与快捷搜索接入教程、博客、作品、作者，以及英语词汇、语法、阅读和公开原创写作；内容变更后同步索引，启动时补建英语索引。
- 公开路径与 `/useradmin` 账户路径、昼夜主题、分页筛选、公开内容 SEO HTML 与站点地图。

## 本地启动

- JDK 21、Maven、Node.js、MySQL 8。
- 在 MySQL 中创建独立数据库 `star_rain_v2`，使用 utf8mb4。
- 将 `backend/application-local-secret.example.yml` 复制为 `backend/application-local-secret.yml`，填写本地数据库与 SMTP 配置。实际凭据禁止入 Git。
- 初次设置 bootstrap 管理员配置；首次启动自动建立账户表、内置角色、权限与超级管理员。完成后关闭 `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED`。
- `STAR_RAIN_BOOTSTRAP_ADMIN_EMAIL` 是唯一超级管理员的身份邮箱；`STAR_RAIN_MAIL_FROM` 是发信邮箱，两者独立配置，初始化不得用发信邮箱替代管理员身份。
- 在 V2 目录运行 `./deploy/start-local.ps1 -Build`；已有构建时运行 `./deploy/start-local.ps1`。
- 前端：<http://127.0.0.1:5174>；后端：<http://127.0.0.1:8088>。页面 API 经 Vite 代理到后端。
- 停止服务：`./deploy/stop-local.ps1`。重建前先停止服务，避免 Windows 锁定运行中的 JAR。

## 数据库与配置

配置与运行输出均被 Git 忽略。数据库脚本位于 `backend/star-rain-boot/src/main/resources/db/migration/`，采用逻辑外键，首版迁移版本为 `2.037`。V2 不能直接使用 V1 数据库；升级前应备份数据库与媒体，并使用 V2 配置和迁移。

## 构建与验证

在 V2 目录执行：

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml -pl star-rain-boot -am -DskipTests package
cd frontend
npm ci
npm test
npm run build
```

后端产物为 `backend/star-rain-boot/target/star-rain-boot-2.0.0.jar`，前端产物为 `frontend/dist/`。配置变量以 `backend/application-local-secret.example.yml` 为准；生产环境使用 HTTPS 并启用 `STAR_RAIN_COOKIE_SECURE`。需连 MySQL 的集成测试会在数据库不可达时跳过，其执行情况应与单元测试分别核对。

## 账户验收

注册必须先获取并填写邮箱验证码，验证码有效期 10 分钟，重发冷却 60 秒。已有未验证账户可在“我的账户 → 验证账户邮箱”补充验证，无需重新注册。

角色与权限由数据库迁移预置，不提供任意勾选授权页面。注册只授予 USER；ADMIN 通过超级管理员邀请取得；唯一 SUPER_ADMIN 由初始化建立，不能通过注册或邀请取得。账户治理操作只开放给 SUPER_ADMIN。其余业务模块的 ADMIN 能力随对应模块实现。

超级管理员在“账户管理”搜索已注册普通用户，点击“邀请管理员”。“管理员邀请”页面提供记录、重发和撤销；邀请有效期 48 小时，重发冷却 60 秒，重发成功后旧链接失效。受邀用户登录自己的账户，在“我的账户 → 待处理邀请”接受，随后重新登录取得 ADMIN 身份。超级管理员可取消管理员身份，相关旧会话随即失效。

邮件状态“已提交发送”表示 SMTP 接受，不等于收件箱送达。发送失败返回错误；本机开发环境的邀请通知不包含回环地址链接，受邀用户直接从账户页面处理邀请。部署时设置可访问的 `STAR_RAIN_FRONTEND_ORIGIN`。

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
| api (+dto/vo/impl/event) | 模块对外契约：根包及 dto/vo/event 只放契约，实现只放 api/impl |
| config | `@Configuration` 配置类 |
| constant | 常量类（权限码、usageCode 等） |
| context | 上下文与主体对象 |
| controller | Controller |
| dto | 请求 DTO 与层间传输对象 |
| dashboard | 模块概览的数据源适配；聚合逻辑仍在 site/service |
| entity | 数据库实体 |
| enumeration | 枚举 |
| event | 领域事件的发布与消费适配；业务编排放在 service，跨模块事件类型放在 api/event |
| job | 定时任务入口；只负责调度 service |
| exception | 自定义异常与错误码 |
| handler | 处理器 |
| interceptor | 拦截器 / 过滤器 |
| lifecycle | 启动或关闭时的一次性协调 |
| mapper | MyBatis Mapper 接口 |
| notifier | 对外通知的基础设施适配 |
| properties | `@ConfigurationProperties` |
| provider (+impl) | 同一模块内多来源读取契约及其实现 |
| renderer | 将内容转换为 HTML 等呈现格式 |
| security | 安全声明与请求限流 |
| service (+impl) | 服务接口 + 实现 |
| storage (+impl) | 可替换的基础设施抽象 |
| utils | 无状态格式化与分类工具 |
| vo | 视图对象 |

`common/dashboard/api` 只放模块概览契约，避免 `site` 依赖各业务模块时出现反向依赖。模块的 `dashboard` 类只把本模块 service 结果转换为契约数据；`site/service` 负责聚合和降级。模块的 `event` 类只接收或发布提交后的领域事件，搜索与 SEO 的派生数据更新留在各自 service。跨模块调用继续通过目标模块的 `api` 契约。

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
