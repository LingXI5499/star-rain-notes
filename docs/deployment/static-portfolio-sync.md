# 静态站作品同步与英语管理界面

静态展示站的作品位于 `star-rain-notes-site/src/content-data/portfolio/`。主应用使用数据库中的作品聚合及媒体库。两套前端分别运行，同步不会修改静态站源文件或部署服务器。

## 同步流程

先准备现有 Python 环境中的 PyMySQL、PyYAML、Pillow，以及现有 MySQL 客户端和 JDK 21。配置文件和媒体目录必须在当前项目内，数据库必须为本机数据库。先使用独立测试数据库演练。

```powershell
node tools/export-static-portfolio.mjs
python tools/sync-static-portfolio.py --config backend/application-local-secret.yml
```

导出正文、摘要、分类、技术栈、内容区块、链接与图片。HTML 转为 Markdown 时检查可见文本一致；缺失图片或冲突图片会中止。默认预览会回滚数据库事务。

应用前将 `STAR_RAIN_MYSQLDUMP` 指向现有 MySQL 安装中的 `mysqldump`，将 `STAR_RAIN_JAVA_HOME` 指向现有 JDK 21。执行：

```powershell
python tools/sync-static-portfolio.py --config backend/application-local-secret.yml --apply
```

命令先在 `history/portfolio-sync-时间戳/` 保存完整数据库备份，再在事务内保存内容，输出 `pending-publication.json` 的位置。使用输出的计划继续：

```powershell
& backend/tools/publish-static-content.ps1 -Plan history/portfolio-sync-时间戳/pending-publication.json -Config backend/application-local-secret.yml
```

发布步骤复用现有作品发布校验及权限，重建搜索与 SEO 投影。计划会核对目标数据库和既有管理员。发布失败时保留备份、草稿和计划，修复后重新执行同一计划；不要删除草稿后重建。

按 slug 匹配原作品，保留其 ID、创建者及现有引用。未发布作品受到保护，不被同步覆盖；不删除来源之外的作品、链接或历史材料。图片注册为公开媒体，记录原文件摘要和引用，保存到配置的媒体目录。数据库备份、媒体运行文件及私密配置不进入公开源码提交。

当前来源为 10 件作品、77 个内容区块、22 张图片；原有两件作品保留编号，新增八件。完成后重复预览应显示零新增、零内容变化。部署主应用时应同时备份并迁移媒体文件，单独上传前端 dist 不能替代媒体部署。

## 英语管理界面

- 词汇管理与前台共用两级分类组件和词卡，支持显示语言、分类状态、搜索和分页；编辑沿用既有词条身份及并发版本校验。
- 世界管理按一级分类、二级主题筛选，并使用服务端分页列表。分类与分页写入地址栏，刷新和返回可恢复；编辑、发布与撤回沿用原权限。
- 随笔管理使用文章列表、状态、摘要、时间与操作栏。状态筛选在服务端执行，始终仅查询当前账户自己的随笔；草稿、完成及发布仍按现有状态机处理。
- 语法管理统一操作按钮、悬停与键盘焦点样式。搜索组件自身管理边框，页面通用表单样式不再给内部搜索输入框添加第二层边框。

## 本地验证

本次前端 498 项、后端相关 43 项、浏览器 24 项检查通过，均为零失败、零跳过。浏览器覆盖四个英语管理页面的桌面和手机布局、真实分页和筛选，以及十件作品详情及图片加载。前端生产构建通过。以上为本地验证，不代表服务器部署或生产验收。
