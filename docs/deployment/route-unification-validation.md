# 路由统一验证记录

验证日期：2026-10-25（Asia/Shanghai）。范围为本地主应用，基线为远程 main `46cf75642efda7ff9a544acbce32cd60849444df`。工作分支 `LIngXI/route-unification`。远程 main 经只读查询仍为该提交；没有 fetch、pull、推送、远程分支、PR、合并或生产发布操作。静态网站分支与原工作区保持原样。

## 最终自动验证

| 检查 | 最终结果 | 失败 | 跳过 / 未执行 |
| --- | --- | --- | --- |
| `npm test` | 81 个文件，485 项通过 | 0 | 0 |
| `npm run build` | 生产构建成功 | 0 | 0 |
| `npm run test:e2e`（默认受控服务） | Chromium 57 项通过 | 0 | 0 |
| `STAR_RAIN_E2E_REAL=true` 下的 `npm run test:e2e` | 真实后端 / MySQL / SMTP，Chromium 7 项通过 | 0 | 0 |
| `mvn verify` | 全部 16 个模块成功，861 项通过 | 0 | 0 |
| 实际 Nginx 模板回环验证 | 92 项检查通过，覆盖 58 条私有页面声明及重定向别名、作品 live | 0 | 0 |
| 测试清理后的定向单测 | 英语写作与阅读目录 8 项通过 | 0 | 0 |
| 差异与部署同步检查 | `git diff --check` 通过；示例、两个部署片段与 41 条 robots 指令一致 | 0 | 0 |

默认和真实浏览器配置分别选择 57 与 7 项用例；这两组已全部执行，不将配置中的 testIgnore 当成已完成的真实后端测试。真实后端测试未拦截应用 API，使用临时 MySQL 实例中的独立测试数据库与回环 SMTP。Maven 使用 JDK 21，数据库执行全部 47 个已有迁移；账户测试另建随机 schema。没有连接生产数据库，也没有修改系统 JAVA_HOME。

## 验收覆盖

| 要求 | 核验证据 |
| --- | --- |
| 根路径与账号入口 | 生产路由保留 HomePage；受控及真实浏览器均验证前台外壳和账号入口，根路径不进入侧栏 |
| 认证页面直达 | 路由测试覆盖登录、注册、找回、重置；真实邮件浏览器流程覆盖注册、登录与重置 |
| 未登录跳转与登录返回 | 路由测试与浏览器验证完整 query/hash 返回；旧邀请链接经登录回到规范落地页 |
| 普通用户和管理权限 | USER 直接输入账户及博客管理 URL 回到 center；真实后端对应 API 返回 403；SUPER_ADMIN 与 ADMIN 权限区别保留 |
| 公开内容 | 匿名路由匹配、真实公开 HTML / Canonical / Sitemap、博客撤回与搜索可见性回归通过 |
| 旧前缀兼容 | 前端兼容、服务端 308、查询串、重复/空参数、嵌套返回目标与浏览器 hash 继承通过 |
| 邮件 | 直接邮件链接单测；真实异步重置邮件使用新路径并成功重置；邀请接受与撤权会话失效通过 |
| 索引与统计 | 私有元数据排除、无 Canonical 残留、公开页面恢复索引、失败导航不记录、后台及认证不计公开访问 |
| 404 | 原始公开 HTML 与 SEO 元数据缺失返回 404；缺失静态资源返回 404，管理地址正常 SPA，不被误当公开内容 |
| 手机与后台导航 | 手机菜单、课程目录、阅读目录、编辑器返回/侧栏/浏览器后退、390px 和中间桌面宽度回归通过 |

90 条原页面声明的组件、参数、props、重定向和权限元数据与基线逐条比对一致；首页公开标记和作品 live 明确 noindex 为本次补充。前端业务源码仅在兼容记录与规范路径工具中保留旧前缀字面量。

## 交付边界与部署注意

本地验收通过。生产部署、实际域名 HTTPS 与证书、线上 Nginx reload、线上数据库和搜索引擎收录状态未执行；这不是生产验证结果。用户审批推送与实际部署仍分别以当次明确指令为准。

部署必须配套更新后端 JAR、完整 dist、Nginx SPA/SEO 顺序与 robots；特别保留重置/邀请旧链接查询串。旧 Nginx 如果只删除旧前缀规则，新管理路径会错误进入公开 SEO，不能直接上线。详见 [路由与部署说明](route-unification.md)。

新增开发约束已写入 [AGENTS.md](../../AGENTS.md)，且加入根目录文件跟踪白名单。账户为 LingXI5499；提交邮箱与该账户在 GitHub 已验证签名提交 `731f0b24cad17c62749935abb95f81b740635889` 的作者邮箱一致。本地临时运行文件、测试日志、依赖、构建产物及数据库凭据均不进入交付清单。

## 修改文件

共 111 个文件。多数页面和既有测试仅将内部导航或断言改为规范路径；核心逻辑集中在路由、路径工具、外壳、SEO/统计、邮件及部署配置。

<details>
<summary>完整文件清单</summary>

- [.gitignore](../../.gitignore)
- [AGENTS.md](../../AGENTS.md)
- [README.md](../../README.md)
- [backend/README.md](../../backend/README.md)
- [backend/deploy/account-v3/nginx-site.example.conf](../../backend/deploy/account-v3/nginx-site.example.conf)
- [backend/star-rain-account/src/main/java/com/starrainnotes/account/delivery/MailOutbox.java](../../backend/star-rain-account/src/main/java/com/starrainnotes/account/delivery/MailOutbox.java)
- [backend/star-rain-account/src/main/java/com/starrainnotes/account/service/impl/AccountMailServiceImpl.java](../../backend/star-rain-account/src/main/java/com/starrainnotes/account/service/impl/AccountMailServiceImpl.java)
- [backend/star-rain-account/src/test/java/com/starrainnotes/account/security/ModuleFrameHeadersTest.java](../../backend/star-rain-account/src/test/java/com/starrainnotes/account/security/ModuleFrameHeadersTest.java)
- [backend/star-rain-account/src/test/java/com/starrainnotes/account/service/impl/AccountMailServiceLinksTest.java](../../backend/star-rain-account/src/test/java/com/starrainnotes/account/service/impl/AccountMailServiceLinksTest.java)
- [backend/star-rain-boot/src/test/java/com/starrainnotes/boot/AccountV3IntegrationTest.java](../../backend/star-rain-boot/src/test/java/com/starrainnotes/boot/AccountV3IntegrationTest.java)
- [backend/star-rain-seo/src/main/java/com/starrainnotes/seo/service/impl/RobotsServiceImpl.java](../../backend/star-rain-seo/src/main/java/com/starrainnotes/seo/service/impl/RobotsServiceImpl.java)
- [backend/star-rain-seo/src/main/java/com/starrainnotes/seo/service/impl/SeoSnapshotServiceImpl.java](../../backend/star-rain-seo/src/main/java/com/starrainnotes/seo/service/impl/SeoSnapshotServiceImpl.java)
- [backend/star-rain-seo/src/test/java/com/starrainnotes/seo/SeoCoreTest.java](../../backend/star-rain-seo/src/test/java/com/starrainnotes/seo/SeoCoreTest.java)
- [docs/deployment/account-v3-deploy.md](../../docs/deployment/account-v3-deploy.md)
- [docs/deployment/robots-public-api.location.conf](../../docs/deployment/robots-public-api.location.conf)
- [docs/deployment/route-unification-validation.md](../../docs/deployment/route-unification-validation.md)
- [docs/deployment/route-unification.md](../../docs/deployment/route-unification.md)
- [docs/deployment/v2.2-release-checklist.md](../../docs/deployment/v2.2-release-checklist.md)
- [frontend/README.md](../../frontend/README.md)
- [frontend/e2e/account-real.spec.js](../../frontend/e2e/account-real.spec.js)
- [frontend/e2e/editor-selection.spec.js](../../frontend/e2e/editor-selection.spec.js)
- [frontend/e2e/editor-v3.spec.js](../../frontend/e2e/editor-v3.spec.js)
- [frontend/e2e/portfolio.spec.js](../../frontend/e2e/portfolio.spec.js)
- [frontend/e2e/reading-layout.spec.js](../../frontend/e2e/reading-layout.spec.js)
- [frontend/e2e/real-backend.spec.js](../../frontend/e2e/real-backend.spec.js)
- [frontend/e2e/reliability.spec.js](../../frontend/e2e/reliability.spec.js)
- [frontend/e2e/routes.spec.js](../../frontend/e2e/routes.spec.js)
- [frontend/playwright.config.js](../../frontend/playwright.config.js)
- [frontend/src/App.vue](../../frontend/src/App.vue)
- [frontend/src/main.js](../../frontend/src/main.js)
- [frontend/src/modules/account/components/AccountEntry.vue](../../frontend/src/modules/account/components/AccountEntry.vue)
- [frontend/src/modules/account/components/PendingInvitations.vue](../../frontend/src/modules/account/components/PendingInvitations.vue)
- [frontend/src/modules/account/pages/AccountPage.vue](../../frontend/src/modules/account/pages/AccountPage.vue)
- [frontend/src/modules/account/pages/AdminAccountsPage.vue](../../frontend/src/modules/account/pages/AdminAccountsPage.vue)
- [frontend/src/modules/account/pages/AdminInvitationsPage.vue](../../frontend/src/modules/account/pages/AdminInvitationsPage.vue)
- [frontend/src/modules/account/pages/AuthPage.vue](../../frontend/src/modules/account/pages/AuthPage.vue)
- [frontend/src/modules/analytics/tracking/publicTracking.js](../../frontend/src/modules/analytics/tracking/publicTracking.js)
- [frontend/src/modules/analytics/tracking/publicTracking.spec.js](../../frontend/src/modules/analytics/tracking/publicTracking.spec.js)
- [frontend/src/modules/blog/components/BlogTagList.vue](../../frontend/src/modules/blog/components/BlogTagList.vue)
- [frontend/src/modules/blog/components/BlogTimeline.vue](../../frontend/src/modules/blog/components/BlogTimeline.vue)
- [frontend/src/modules/blog/components/BlogTopicPanel.vue](../../frontend/src/modules/blog/components/BlogTopicPanel.vue)
- [frontend/src/modules/blog/pages/BlogArchivePage.vue](../../frontend/src/modules/blog/pages/BlogArchivePage.vue)
- [frontend/src/modules/blog/pages/BlogListPage.vue](../../frontend/src/modules/blog/pages/BlogListPage.vue)
- [frontend/src/modules/blog/pages/BlogPostPage.vue](../../frontend/src/modules/blog/pages/BlogPostPage.vue)
- [frontend/src/modules/blog/pages/BlogTopicPage.vue](../../frontend/src/modules/blog/pages/BlogTopicPage.vue)
- [frontend/src/modules/blog/pages/admin/BlogEditorPage.spec.js](../../frontend/src/modules/blog/pages/admin/BlogEditorPage.spec.js)
- [frontend/src/modules/blog/pages/admin/BlogEditorPage.vue](../../frontend/src/modules/blog/pages/admin/BlogEditorPage.vue)
- [frontend/src/modules/blog/pages/admin/BlogManagePage.vue](../../frontend/src/modules/blog/pages/admin/BlogManagePage.vue)
- [frontend/src/modules/blog/pages/admin/BlogTaxonomyPage.spec.js](../../frontend/src/modules/blog/pages/admin/BlogTaxonomyPage.spec.js)
- [frontend/src/modules/blog/pages/admin/BlogTaxonomyPage.vue](../../frontend/src/modules/blog/pages/admin/BlogTaxonomyPage.vue)
- [frontend/src/modules/blog/pages/admin/BlogTopicManagePage.vue](../../frontend/src/modules/blog/pages/admin/BlogTopicManagePage.vue)
- [frontend/src/modules/english/api/englishApi.js](../../frontend/src/modules/english/api/englishApi.js)
- [frontend/src/modules/english/pages/admin/EnglishDocumentsManagePage.spec.js](../../frontend/src/modules/english/pages/admin/EnglishDocumentsManagePage.spec.js)
- [frontend/src/modules/english/pages/public/EnglishHomePage.vue](../../frontend/src/modules/english/pages/public/EnglishHomePage.vue)
- [frontend/src/modules/english/pages/public/EnglishTopicPage.vue](../../frontend/src/modules/english/pages/public/EnglishTopicPage.vue)
- [frontend/src/modules/english/pages/public/EnglishWritingEditorPage.spec.js](../../frontend/src/modules/english/pages/public/EnglishWritingEditorPage.spec.js)
- [frontend/src/modules/english/pages/public/EnglishWritingPage.spec.js](../../frontend/src/modules/english/pages/public/EnglishWritingPage.spec.js)
- [frontend/src/modules/english/pages/public/EnglishWritingPage.vue](../../frontend/src/modules/english/pages/public/EnglishWritingPage.vue)
- [frontend/src/modules/english/pages/public/VocabularyThemePage.spec.js](../../frontend/src/modules/english/pages/public/VocabularyThemePage.spec.js)
- [frontend/src/modules/english/pages/public/VocabularyThemePage.vue](../../frontend/src/modules/english/pages/public/VocabularyThemePage.vue)
- [frontend/src/modules/english/support/vocabularySummary.js](../../frontend/src/modules/english/support/vocabularySummary.js)
- [frontend/src/modules/media/pages/MediaDetailPage.vue](../../frontend/src/modules/media/pages/MediaDetailPage.vue)
- [frontend/src/modules/media/pages/MediaLibraryPage.vue](../../frontend/src/modules/media/pages/MediaLibraryPage.vue)
- [frontend/src/modules/portfolio/components/WorkLibrary.spec.js](../../frontend/src/modules/portfolio/components/WorkLibrary.spec.js)
- [frontend/src/modules/portfolio/pages/admin/PortfolioManagePage.spec.js](../../frontend/src/modules/portfolio/pages/admin/PortfolioManagePage.spec.js)
- [frontend/src/modules/portfolio/pages/admin/WorkEditorPage.spec.js](../../frontend/src/modules/portfolio/pages/admin/WorkEditorPage.spec.js)
- [frontend/src/modules/profile/components/EvidenceGrid.vue](../../frontend/src/modules/profile/components/EvidenceGrid.vue)
- [frontend/src/modules/profile/pages/ProfilePage.vue](../../frontend/src/modules/profile/pages/ProfilePage.vue)
- [frontend/src/modules/review/pages/ReviewCenterPage.vue](../../frontend/src/modules/review/pages/ReviewCenterPage.vue)
- [frontend/src/modules/review/pages/ReviewDetailPage.vue](../../frontend/src/modules/review/pages/ReviewDetailPage.vue)
- [frontend/src/modules/search/pages/SearchResultPage.spec.js](../../frontend/src/modules/search/pages/SearchResultPage.spec.js)
- [frontend/src/modules/seo/seoMeta.js](../../frontend/src/modules/seo/seoMeta.js)
- [frontend/src/modules/seo/seoMeta.spec.js](../../frontend/src/modules/seo/seoMeta.spec.js)
- [frontend/src/modules/site/components/PublicFooter.vue](../../frontend/src/modules/site/components/PublicFooter.vue)
- [frontend/src/modules/site/components/PublicHeader.vue](../../frontend/src/modules/site/components/PublicHeader.vue)
- [frontend/src/modules/site/pages/DashboardPage.vue](../../frontend/src/modules/site/pages/DashboardPage.vue)
- [frontend/src/modules/site/pages/HomePage.vue](../../frontend/src/modules/site/pages/HomePage.vue)
- [frontend/src/modules/site/pages/LegalPage.vue](../../frontend/src/modules/site/pages/LegalPage.vue)
- [frontend/src/modules/site/pages/NotFoundPage.vue](../../frontend/src/modules/site/pages/NotFoundPage.vue)
- [frontend/src/modules/site/pages/PendingPage.vue](../../frontend/src/modules/site/pages/PendingPage.vue)
- [frontend/src/modules/tutorial/pages/admin/ChapterEditPage.spec.js](../../frontend/src/modules/tutorial/pages/admin/ChapterEditPage.spec.js)
- [frontend/src/modules/tutorial/pages/admin/ChapterEditPage.vue](../../frontend/src/modules/tutorial/pages/admin/ChapterEditPage.vue)
- [frontend/src/modules/tutorial/pages/admin/TutorialCurriculumPage.vue](../../frontend/src/modules/tutorial/pages/admin/TutorialCurriculumPage.vue)
- [frontend/src/modules/tutorial/pages/admin/TutorialEditPage.vue](../../frontend/src/modules/tutorial/pages/admin/TutorialEditPage.vue)
- [frontend/src/modules/tutorial/pages/admin/TutorialManagePage.vue](../../frontend/src/modules/tutorial/pages/admin/TutorialManagePage.vue)
- [frontend/src/modules/tutorial/pages/admin/TutorialPreviewPage.vue](../../frontend/src/modules/tutorial/pages/admin/TutorialPreviewPage.vue)
- [frontend/src/modules/tutorial/pages/learning/ChapterStudyPage.spec.js](../../frontend/src/modules/tutorial/pages/learning/ChapterStudyPage.spec.js)
- [frontend/src/modules/tutorial/pages/learning/LearningQueries.spec.js](../../frontend/src/modules/tutorial/pages/learning/LearningQueries.spec.js)
- [frontend/src/modules/tutorial/pages/learning/StudyPlanPage.spec.js](../../frontend/src/modules/tutorial/pages/learning/StudyPlanPage.spec.js)
- [frontend/src/modules/tutorial/pages/public/TutorialCatalogPage.vue](../../frontend/src/modules/tutorial/pages/public/TutorialCatalogPage.vue)
- [frontend/src/modules/tutorial/pages/public/TutorialChapterPage.vue](../../frontend/src/modules/tutorial/pages/public/TutorialChapterPage.vue)
- [frontend/src/modules/tutorial/pages/public/TutorialChapterProgress.spec.js](../../frontend/src/modules/tutorial/pages/public/TutorialChapterProgress.spec.js)
- [frontend/src/modules/tutorial/pages/public/TutorialDetailPage.vue](../../frontend/src/modules/tutorial/pages/public/TutorialDetailPage.vue)
- [frontend/src/modules/tutorial/support/planReader.spec.js](../../frontend/src/modules/tutorial/support/planReader.spec.js)
- [frontend/src/router/index.js](../../frontend/src/router/index.js)
- [frontend/src/router/index.spec.js](../../frontend/src/router/index.spec.js)
- [frontend/src/router/scrollBehavior.spec.js](../../frontend/src/router/scrollBehavior.spec.js)
- [frontend/src/shared/editor/EditorDocumentTools.vue](../../frontend/src/shared/editor/EditorDocumentTools.vue)
- [frontend/src/shared/reading/collectReadingNavigation.spec.js](../../frontend/src/shared/reading/collectReadingNavigation.spec.js)
- [frontend/src/shared/routeVisibility.js](../../frontend/src/shared/routeVisibility.js)
- [frontend/src/shared/shells/AdminShell.scroll.spec.js](../../frontend/src/shared/shells/AdminShell.scroll.spec.js)
- [frontend/src/shared/shells/AdminShell.vue](../../frontend/src/shared/shells/AdminShell.vue)
- [frontend/src/shared/shells/PublicShell.vue](../../frontend/src/shared/shells/PublicShell.vue)
- [frontend/src/shared/ui/MobilePublicNav.spec.js](../../frontend/src/shared/ui/MobilePublicNav.spec.js)
- [frontend/src/shared/ui/MobilePublicNav.vue](../../frontend/src/shared/ui/MobilePublicNav.vue)
- [frontend/src/shared/ui/PublicLists.spec.js](../../frontend/src/shared/ui/PublicLists.spec.js)
- [frontend/src/shared/viewMode.js](../../frontend/src/shared/viewMode.js)
- [frontend/src/shared/viewMode.spec.js](../../frontend/src/shared/viewMode.spec.js)
- [frontend/src/style.css](../../frontend/src/style.css)
- [tools/serve-integration-site.mjs](../../tools/serve-integration-site.mjs)
- [tools/test_route_nginx.mjs](../../tools/test_route_nginx.mjs)

</details>
