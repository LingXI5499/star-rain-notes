# 英语阅读与写作收敛：验收记录

日期：2026-10-09。基线 `84683b3`。依据 20261008 业务收敛文档；仅修改 V2 英语相关实现与必要的统一 404 异常映射。其他工作区的 Blog/Tutorial/Site 修改保留。

## 已完成

移除阅读等级 DTO/默认值/校验/XML/前后台卡片/编辑表单/首页轨道；删除旧写作素材和题目的类、映射、页面、路由、请求与管理入口。新原创写作的归属、私稿、自动保存、完成、发布及历史版本保持原实现。

阅读 EN/ZH/BOTH 共享真实原位范围：按用户后续要求，单栏仅高亮当前语言片段，双栏按组同步高亮，一对多对应保留。无对应范围只做临时局部高亮，不创建映射或写入数据库。悬停不再生成译文浮窗。保留 Markdown 强调、链接、列表及 UTF-16 选区，跳过代码/公式/隐藏内容；缩写等不确定分句保守降为整段。手机无映射时明确区分原文/译文；仅有效映射段落穿插。正文先显示，辅助功能独立失败降级。

高亮支持键盘、Escape 清除、触屏点击固定与外部关闭；切换模式或进入人工选区模式时清除高亮。悬浮翻译、浮窗定位和复制代码已移除；点击人工精读标记仍可查看解析。

V2_037 删除三等级列和旧写作两表，精确匹配旧首页默认文案后更新介绍/路线图。管理员自编概览不会被覆盖。历史 V2_020/035/036 未修改。该迁移不清空阅读文章；一次性内容维护另存 `maintenance/english-reading-reset/`，默认门禁无效且 ROLLBACK。

## 业务收敛阶段实测结果（0485613）

| 验证 | 结果 |
|---|---|
| Maven 完整 reactor 测试 `mvn -B -pl star-rain-boot -am test` | 743 项通过，0 失败/错误/跳过 |
| 新增真实 MVC 未匹配路由测试 | 1 项通过：NoHandler/NoResource 均返回 404，不再落入 500 |
| 最终 V2_037 fresh/2.036 upgrade 定向重验 | 2 项通过；历史 checksum 有效，自编概览保留，后续阅读重启不被清空 |
| 后端 `package -DskipTests` | 通过 |
| 前端完整 `npm run test` | 41 文件、187 项通过 |
| 最终浮层边距/范围调整后的定向重验 | 2 文件、15 项通过 |
| 前端 `npm run build` | 通过 |
| 真 MySQL 阅读/写作服务契约 | 创建、公开、权益约束、CAS、标注失效、版本恢复、本人归属及私稿隔离通过 |
| 隔离运行时 HTTP | 七表清空后公开/管理阅读均为 0，旧 slug 404，四个旧写作 API 均 404；新写作公开 1 篇，本人 2 篇保留 |
| 默认安全/备份恢复/内容清空 | 恢复 104 表全行 CHECKSUM 一致；默认不删除、ROLLBACK 还原、隔离 COMMIT 阅读七表为 0；其余 97 表不变 |
| 真实浏览器 | EN 中文浮层、ZH 英文浮层、BOTH 双栏组高亮、一对多、无映射局部提示通过 |
| 390px 手机宽度 | 复制显示“已复制”，正文/浮层无横向溢出；真实可用宽度 375px 时浮层左右至少 12px |
| 长文与格式 | 100 段稳定、格式/链接/代码不破坏、注销移除事件监听，人工选区不受临时标记影响 |

## 数据和运行状态

用户于本次会话明确授权本机 `127.0.0.1:3306 / star_rain_v2` 升级后，停止原站写入，重新备份并恢复演练。2026-10-09 09:44 执行经门禁核验的独立 COMMIT 副本：七张阅读表归零，其余 97 张表行数及全行 CHECKSUM 不变。随后启动新版，Flyway 成功由 2.036 升至 2.037；等级三列与旧写作两表已物理删除。5174/8088 现运行新版。连续两次启动后阅读保持 0，没有历史回填。

最新正式执行前备份及 SHA-256/逐表摘要、批准的执行副本、实际执行报告在被忽略的 `.local/english-retirement/20261009094227/`。此备份已完整恢复验证 104 张表，支持恢复升级前状态。较早的交互预览备份在 `.local/english-retirement/20261009090312/`。升级程序单独连接恢复的隔离库 `sr_english_retirement_restore_20261009090312`，8089/5175，已升至 2.037。验证归零后只在此隔离库新建 `【隔离预览】A Reading Journal`（reading-50），供交互查看；不会向实际开发库补入验收文章。

预览：[阅读交互](http://127.0.0.1:5175/english/reading/reading-50)、[写作中心](http://127.0.0.1:5175/english/writing)。截图在被忽略的 `.local/english-retirement/screenshots/`。预览关闭了共享媒体的孤儿文件清理任务，未删除媒体文件。

## 目标环境验收与边界

文档 T4 所要求的具体目标许可已获得并执行；正式运行时公开/管理阅读均为 0，旧 reading-43 与四个旧素材/题目 API 均 404。新写作本人 2 篇、公开 1 篇、历史版本 6 条保持；词汇 7053、语法 42、分类 261 保持；这些表及原创写作文章/标签/版本在迁移后与清空前备份的全行 CHECKSUM 一致。正文示例只在隔离库，原开发库未补入验收文章。生产库未连接、未部署、未删除。

旧首页默认文案已在隔离库验证更新；若其他环境是管理员自编内容，其文案需单独审阅，不会被精确匹配迁移覆盖。任何新媒体/索引依赖、计数或版本清单漂移都必须重新预检。

## 本次修改文件

以下路径相对仓库根；不包含其他模块在开始本任务前已有的改动，也不提交 dump、私稿或登录配置。

```text
star-rain-v2/backend/star-rain-boot/src/main/resources/db/migration/V2_037__retire_english_reading_levels_and_legacy_writing.sql
star-rain-v2/backend/star-rain-boot/src/test/java/com/starrainnotes/boot/ApiNotFoundContractTest.java
star-rain-v2/backend/star-rain-boot/src/test/java/com/starrainnotes/boot/EnglishMapperXmlTest.java
star-rain-v2/backend/star-rain-boot/src/test/java/com/starrainnotes/boot/EnglishRetirementContractTest.java
star-rain-v2/backend/star-rain-boot/src/test/java/com/starrainnotes/boot/EnglishRetirementMigrationTest.java
star-rain-v2/backend/star-rain-boot/src/test/java/com/starrainnotes/boot/EnglishRwIntegrationTest.java
star-rain-v2/backend/star-rain-common/src/main/java/com/starrainnotes/common/handler/GlobalApiExceptionHandler.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/config/EnglishPersistenceConfig.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/reading/dto/ReadingDto.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/reading/service/impl/ReadingServiceImpl.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/controller/WritingPromptAdminController.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/controller/WritingPromptPublicController.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/controller/WritingResourceAdminController.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/controller/WritingResourcePublicController.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/dto/WritingPromptDto.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/dto/WritingResourceDto.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/mapper/WritingPromptMapper.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/mapper/WritingResourceMapper.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/service/WritingPromptService.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/service/WritingResourceService.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/service/impl/WritingPromptServiceImpl.java
star-rain-v2/backend/star-rain-english/src/main/java/com/starrainnotes/english/writing/service/impl/WritingResourceServiceImpl.java
star-rain-v2/backend/star-rain-english/src/main/resources/mapper/english/ReadingMapper.xml
star-rain-v2/backend/star-rain-english/src/main/resources/mapper/english/WritingPromptMapper.xml
star-rain-v2/backend/star-rain-english/src/main/resources/mapper/english/WritingResourceMapper.xml
star-rain-v2/docs/english-rw-retirement-inventory.md
star-rain-v2/docs/english-rw-retirement-validation.md
star-rain-v2/frontend/src/modules/english/api/englishApi.js
star-rain-v2/frontend/src/modules/english/api/englishRwApi.js
star-rain-v2/frontend/src/modules/english/components/EnglishBilingualProse.spec.js
star-rain-v2/frontend/src/modules/english/components/EnglishBilingualProse.vue
star-rain-v2/frontend/src/modules/english/lib/readingAnchors.js
star-rain-v2/frontend/src/modules/english/lib/readingAnchors.spec.js
star-rain-v2/frontend/src/modules/english/pages/admin/EnglishDocumentEditorPage.vue
star-rain-v2/frontend/src/modules/english/pages/admin/EnglishDocumentsManagePage.spec.js
star-rain-v2/frontend/src/modules/english/pages/admin/EnglishDocumentsManagePage.vue
star-rain-v2/frontend/src/modules/english/pages/admin/EnglishManagePage.vue
star-rain-v2/frontend/src/modules/english/pages/admin/EnglishReadingEditorPage.vue
star-rain-v2/frontend/src/modules/english/pages/public/EnglishDocumentPage.vue
star-rain-v2/frontend/src/modules/english/pages/public/EnglishDocumentsPage.vue
star-rain-v2/frontend/src/modules/english/pages/public/EnglishHomePage.vue
star-rain-v2/frontend/src/modules/english/pages/public/EnglishReadingPage.spec.js
star-rain-v2/frontend/src/modules/english/pages/public/EnglishReadingPage.vue
star-rain-v2/frontend/src/modules/english/pages/public/EnglishWritingPage.spec.js
star-rain-v2/frontend/src/modules/english/pages/public/EnglishWritingPage.vue
star-rain-v2/frontend/src/router/index.js
star-rain-v2/frontend/src/shared/shells/AdminShell.vue
star-rain-v2/frontend/src/shared/ui/PublicLists.spec.js
star-rain-v2/maintenance/english-reading-reset/01_inventory.sql
star-rain-v2/maintenance/english-reading-reset/02_backup_and_restore.md
star-rain-v2/maintenance/english-reading-reset/03_reset_once.sql
star-rain-v2/maintenance/english-reading-reset/04_verify.sql
```

## 2026-10-09 后续界面调整

根据用户三个后续要求完成：博客标签移除 #，保留 postCount，数量徽标、悬停上浮、点击反馈、选中高亮及键盘焦点；筛选弹层同步移除 # 并改为选择标记。已生效的标签条件也不显示 #。原单/多标签筛选及再次点击取消行为保留。

写作按钮被普通链接颜色覆盖，现提高英语按钮样式优先级，明确普通/主按钮/悬停/焦点状态。主按钮使用主题 on-primary，夜间公开外壳补齐深色文字令牌。真实浏览器日间为浅字/深绿底，夜间为深字/浅绿底；开始写作目标仍是原私人草稿入口。

阅读取消英/中模式的译文浮窗，仅高亮当前语言；双语模式同步高亮对应片段。上表的浮窗验收是 0485613 的历史结果，后续行为以此节为准，不再保留复制译文或无映射提示浮窗。

验证命令：`npm test -- src/modules/english/components/EnglishBilingualProse.spec.js src/modules/english/lib/readingAnchors.spec.js src/modules/english/pages/public/EnglishReadingPage.spec.js src/modules/english/pages/public/EnglishWritingPage.spec.js`，4 文件 25 项通过；`npm run build` 通过。真实浏览器确认英文仅 3 个英文格式片段高亮、中文仅 2 个中文对应段落高亮、BOTH 同时 3 EN + 2 ZH；三模式均无译文浮窗。精读 note 点击与人工选区模式有回归验证。标签选择/取消、数量保留及筛选弹层已实测；主题最终恢复日间。

本轮额外涉及 BlogSidebar.vue、BlogTagOverlay.vue、BlogListPage.vue、englishRw.css、public-theme.css；仅提交本轮相关的标签外观；其余博客、教程等已有修改保留工作区。截图存于忽略目录 `.local/ui-polish/screenshots/`。
