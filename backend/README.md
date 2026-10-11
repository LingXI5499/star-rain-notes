# 后端

Java 21 / Spring Boot 3.5 / Maven 多模块应用，使用 MySQL 8、MyBatis Mapper XML 与 Flyway。`star-rain-boot` 组装各模块并生成可运行 JAR，默认端口 8088。

## 模块

`common` 提供响应、异常和安全契约；`account` 管理身份、权限、邮箱及邀请；`tutorial` 管理教程、审核工作区与学习；`blog`、`portfolio`、`english`、`message`、`profile`、`site` 对应业务内容；`media` 管理存储及引用；`review` 管理审批；`analytics`、`search`、`seo` 提供统计、搜索及公开 HTML 派生数据。

跨模块通过 `api` 契约调用，Controller 校验输入与权限，Service 编排业务和事务，Mapper 绑定 SQL 参数。Service 接口和实现分开，DTO、VO 与值对象使用 Lombok POJO，禁止新建 Java record；新增业务异常继承项目异常基类。包按职责分类，不创建 support/misc 包。类型、字段和方法使用 Javadoc 说明职责、参数、返回值、约束及协作调用，流程内的普通注释解释业务原因。保留现有逻辑外键、角色与权限模型。

## 源码阅读与注释

每个 Java 源文件（包含测试）都有模块职责和入口说明。先读 Controller 的路由与输入约束，再读 Service 的业务步骤、事务及异常分支，最后按 Mapper 方法找到同名 XML 语句。DTO/VO/Entity 字段注释说明数据含义与校验、序列化规则；测试注释对应实际场景和断言。

Mapper XML 逐条说明读写目标、参数、结果类型、主键回填、行锁和动态条件；各模块 POM 标明聚合、依赖及插件职责。`application.yml` 与公开配置模板说明环境绑定和默认值，`tools/check_public_source.py` 说明扫描范围和失败报告方式。注释不包含真实配置或个人邮箱。

已发布的 40 份迁移 SQL 保留原文，以免改变 Flyway 校验值；每份迁移的职责、涉及对象和结构变更说明见[迁移目录 README](star-rain-boot/src/main/resources/db/migration/README.md)。修改数据库应追加迁移，不能改已执行脚本。

## 配置与启动

从本目录运行：

```bash
mvn -DskipTests package
java -jar star-rain-boot/target/star-rain-boot-2.0.1.jar
```

复制 `application-local-secret.example.yml` 为 `application-local-secret.yml` 并填写本机配置，或者由进程环境注入配置。配置文件相对于**进程工作目录**读取，不随 JAR 自动移动。请使用独立数据库，首次启动自动执行迁移。

| 变量 | 用途 |
| --- | --- |
| `STAR_RAIN_DB_URL / USERNAME / PASSWORD` | JDBC 连接、数据库用户与密码；三个完整变量均以 `STAR_RAIN_DB_` 开头 |
| `STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED / USERNAME / EMAIL / PASSWORD` | 首次创建唯一超级管理员；完整变量均以 `STAR_RAIN_BOOTSTRAP_ADMIN_` 开头，完成后关闭 ENABLED |
| `STAR_RAIN_SMTP_ENABLED` | 是否实际发送邮件；开发可关闭，注册验证需要可用邮件服务 |
| `STAR_RAIN_SMTP_HOST / PORT / USERNAME / PASSWORD / AUTH / SSL / STARTTLS` | 邮件服务器参数，完整变量以 `STAR_RAIN_SMTP_` 开头 |
| `STAR_RAIN_MAIL_FROM` | 发信邮箱，与管理员身份邮箱分开 |
| `STAR_RAIN_FRONTEND_ORIGIN` | 浏览器访问来源，生产填写 HTTPS 域名 |
| `STAR_RAIN_PUBLIC_BASE_URL` | Canonical 与站点地图来源，必须配置实际公网地址；默认本地地址 |
| `STAR_RAIN_FRONTEND_INDEX_PATH` | dist/index.html 路径，生产建议绝对路径 |
| `STAR_RAIN_MEDIA_ROOT` | 持久化媒体目录，默认 `../media` |
| `STAR_RAIN_COOKIE_SECURE` | 生产 HTTPS 下开启，开发 HTTP 保持 false |
| `STAR_RAIN_INDEXNOW_ENABLED / KEY` | 可选索引提交，未配置保持关闭；完整变量以 `STAR_RAIN_INDEXNOW_` 开头 |

Session Cookie 为 `STAR_RAIN_V2_SESSION`，HttpOnly、SameSite=Lax。注册只授予 USER，管理员通过邀请产生，账户治理及审核恢复需要超级管理员身份和对应权限。写接口需要 CSRF，请使用前端共享 HTTP 客户端。

## 部署路由示例

以下 Nginx 片段假设前端 dist 位于 `/srv/star-rain/frontend/dist`，后端只监听本机 8088。放入已经配置证书的 HTTPS server；根据自己的目录修改 root，并补齐日志、限流、超时和响应头。HTTPS 重定向、HSTS 与 CSP 应结合实际域名及图片、公式、原型资源验证后配置。

```nginx
root /srv/star-rain/frontend/dist;
client_max_body_size 520m;

location /api/ {
    proxy_pass http://127.0.0.1:8088;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
location /assets/ {
    try_files $uri =404;
    expires 1y;
    add_header Cache-Control "public, immutable";
}
location /brand/ { try_files $uri =404; }
location = /index.html { add_header Cache-Control "no-cache"; }
location ~ ^/(useradmin(?:/|$)|english(?:/|$)|terms/?$|privacy/?$) {
    try_files $uri /index.html;
    add_header X-Robots-Tag "noindex, nofollow";
}
location / {
    proxy_pass http://127.0.0.1:8088;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

媒体读取经 `/api` 的权限与公开可见性规则处理，不能把整个媒体目录直接公开。公开文档交给 SEO 后端；不存在的博客或教程返回 404。`/assets` 和 `/brand` 请求不可回退为 index.html，否则会掩盖拆包加载错误。

媒体上传返回 `MEDIA_STORAGE_WRITE_FAILED` 时，检查 Java 项目的运行用户能否创建媒体目录的年/月子目录，以及磁盘是否有剩余空间。默认 `../media` 相对于 Java 启动工作目录解析，部署时推荐沿用已存储文件所在目录的绝对路径，避免更换工作目录后找不到旧媒体。服务日志会记录存储根目录和异常，浏览器只展示安全提示。不要为解决写入权限而公开整个媒体目录。

启动日志中的 `Media storage initialized` 会打印实际解析的存储根目录；上传失败查看 `Media storage write failed` 及异常堆栈。读取返回 `MEDIA_CONTENT_MISSING` 表示当前存储根目录下找不到或无法读取该文件，应核对旧目录、文件和运行用户权限。升级请替换 JAR 和整份 dist，确认页面引用的新资源文件已生效。Markdown/TXT 使用 UTF-8 编码，服务会流式校验完整文本，不因 64KB 边界截断汉字而误拒绝。

## 迁移、审核及验证

迁移位于 `star-rain-boot/src/main/resources/db/migration/`，当前版本 `2.040`。既有脚本不可修改，新增迁移只追加。本次新增教程操作的应用失败原因列，审批结论与业务应用状态分开记录。

教程普通管理员变更通过 `review` 提交冻结操作；审批事务提交后，业务变更在独立事务中执行。失败不会撤销已提交的审批，审核详情展示 `FAILED` 或未完成的 `PENDING` 状态。超级管理员在详情页点击“重新核实并恢复”，系统重查审批目标、内容基线和版本；发生变化则拒绝执行，已经 `APPLIED` 的操作直接返回。恢复请求记录到审核轨迹，不自动重试写操作。恢复失败时先核实内容与服务日志；不要直接修改操作状态或数据库正文。

```bash
mvn verify
```

需要数据库的测试读取 `STAR_RAIN_DB_*` 或本地 secret 文件。测试库应先启动一次应用完成 Flyway 迁移，测试用户需具有创建临时 schema 的权限。测试会在临时 schema 中验证升级、事务回滚和真实 Mapper SQL；不要连接生产库。不可达时相应用例跳过，需结合 skipped 数量判断覆盖情况。

人工部署验收：备份数据库与媒体 → 上传新 JAR/dist → 配置实际公网来源和资源路径 → 启动并检查 Flyway → 验证权限、CSRF、上传、博客删除、教程审核与恢复 → 检查公开原始 HTML、robots、Sitemap、404 → 验证手机和双主题。回滚先停服务、恢复旧产物；数据库有状态回退需求时使用升级前备份。
