/**
 * index.js：前端应用模块的应用配置与入口。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 主要依赖：vue-router、../modules/account/stores/authStore、../shared/routeLoadFailure、../shared/viewMode、../modules/site/pages/HomePage.vue。
 * 主要响应式状态：auth。
 * 阅读入口：AuthPage（认证分页的处理函数）；AccountPage（账号分页的处理函数）；AdminAccountsPage（后台账号集合分页的处理函数）；AdminInvitationsPage（后台Invitations分页的处理函数）；AccountAuditsPage（账号Audits分页的处理函数）。
 */

import { createRouter, createWebHistory } from 'vue-router'
import { resolveScrollPosition } from './scrollBehavior'
import { useAuthStore } from '../modules/account/stores/authStore'
import { handleRouteLoadError, routeLoadFailed } from '../shared/routeLoadFailure'
import { accountPath, canonicalPath } from '../shared/viewMode'
/**
 * 认证分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const AuthPage = () => import('../modules/account/pages/AuthPage.vue')
/**
 * 账号分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const AccountPage = () => import('../modules/account/pages/AccountPage.vue')
/**
 * 后台账号集合分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const AdminAccountsPage = () => import('../modules/account/pages/AdminAccountsPage.vue')
/**
 * 后台Invitations分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const AdminInvitationsPage = () => import('../modules/account/pages/AdminInvitationsPage.vue')
/**
 * 账号Audits分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const AccountAuditsPage = () => import('../modules/account/pages/AccountAuditsPage.vue')
/**
 * 媒体Library分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const MediaLibraryPage = () => import('../modules/media/pages/MediaLibraryPage.vue')
/**
 * 媒体详情分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const MediaDetailPage = () => import('../modules/media/pages/MediaDetailPage.vue')
/**
 * 审核Center分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const ReviewCenterPage = () => import('../modules/review/pages/ReviewCenterPage.vue')
/**
 * 审核详情分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const ReviewDetailPage = () => import('../modules/review/pages/ReviewDetailPage.vue')
import HomePage from '../modules/site/pages/HomePage.vue'
/**
 * 待处理分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const PendingPage = () => import('../modules/site/pages/PendingPage.vue')
/**
 * 概览分页的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const DashboardPage = () => import('../modules/site/pages/DashboardPage.vue')

// 每条页面路由只声明一次；公开、认证与管理用途由 meta 标识。
// requiresAuth / permission / superAdminOnly 保持原有权限契约。
export const routeDeclarations = [
  { path: '/', component: HomePage, meta: { publicPage: true } },
  { path: '/blog', component: () => import('../modules/blog/pages/BlogListPage.vue'), meta: { publicPage: true } },
  { path: '/blog/archive', component: () => import('../modules/blog/pages/BlogArchivePage.vue'), meta: { publicPage: true } },
  { path: '/blog/posts/:slug', component: () => import('../modules/blog/pages/BlogPostPage.vue'), meta: { publicPage: true } },
  // 专题页：与 /blog/posts/:slug 并列，不占用文章 slug 的路径空间
  { path: '/blog/topics/:slug', component: () => import('../modules/blog/pages/BlogTopicPage.vue'), meta: { publicPage: true } },

  // 教程已接入公开列表、详情与章节；其余内容模块仍使用建设中页面。
  { path: '/tutorials', component: () => import('../modules/tutorial/pages/public/TutorialCatalogPage.vue'), meta: { publicPage: true } },
  { path: '/tutorials/:tutorialSlug', component: () => import('../modules/tutorial/pages/public/TutorialDetailPage.vue'), meta: { publicPage: true } },
  { path: '/tutorials/:tutorialSlug/:chapterSlug', component: () => import('../modules/tutorial/pages/public/TutorialChapterPage.vue'), meta: { publicPage: true } },
  { path: '/portfolio', component: () => import('../modules/portfolio/pages/PortfolioListPage.vue'), meta: { publicPage: true } },
  { path: '/portfolio/:slug/live', component: () => import('../modules/portfolio/pages/WorkLivePage.vue'), meta: { publicPage: true, noindex: true } },
  { path: '/portfolio/:slug', component: () => import('../modules/portfolio/pages/WorkDetailPage.vue'), meta: { publicPage: true } },
  { path: '/messages', component: () => import('../modules/message/pages/MessageBoardPage.vue'), meta: { publicPage: true } },
  { path: '/about', component: () => import('../modules/profile/pages/ProfilePage.vue'), meta: { publicPage: true } },
  { path: '/terms', component: () => import('../modules/site/pages/LegalPage.vue'), props: { kind: 'terms' }, meta: { publicPage: true } },
  { path: '/privacy', component: () => import('../modules/site/pages/LegalPage.vue'), props: { kind: 'privacy' }, meta: { publicPage: true } },
  { path: '/search', component: () => import('../modules/search/pages/SearchResultPage.vue'), meta: { publicPage: true } },
  { path: '/english', component: () => import('../modules/english/pages/public/EnglishHomePage.vue'), meta: { publicPage: true } },
  { path: '/english/vocabulary', component: () => import('../modules/english/pages/public/VocabularyPage.vue'), meta: { publicPage: true } },
  /*
   * 学习页与进度页必须排在 /english/vocabulary/:themeId 之前：
   * 否则 study / progress 会被当成 themeId 吃掉，落到主题词卡页。
   * （vue-router 4 的静态段本身优先于参数段，这里的顺序是显式声明意图，不依赖打分实现。）
   */
  { path: '/english/vocabulary/study', component: () => import('../modules/english/pages/public/VocabularyStudyPage.vue'), meta: { publicPage: true } },
  { path: '/english/vocabulary/plan/confirm', component: () => import('../modules/english/pages/public/VocabularyPlanConfirmPage.vue'), meta: { publicPage: true } },
  { path: '/english/vocabulary/plan', component: () => import('../modules/english/pages/public/VocabularyPlanPage.vue'), meta: { publicPage: true } },
  { path: '/english/vocabulary/progress', component: () => import('../modules/english/pages/public/VocabularyProgressPage.vue'), meta: { publicPage: true } },
  { path: '/english/vocabulary/:themeId', component: () => import('../modules/english/pages/public/VocabularyThemePage.vue'), meta: { publicPage: true } },
  { path: '/english/grammar', component: () => import('../modules/english/pages/public/GrammarPage.vue'), meta: { publicPage: true } },
  { path: '/english/grammar/:slug', component: () => import('../modules/english/pages/public/GrammarLessonPage.vue'), meta: { publicPage: true } },
  { path: '/english/reading', component: () => import('../modules/english/pages/public/EnglishDocumentsPage.vue'), meta: { publicPage: true } },
  { path: '/english/reading/:slug', component: () => import('../modules/english/pages/public/EnglishReadingPage.vue'), props: { kind: 'reading' }, meta: { publicPage: true } },
  { path: '/english/writing', component: () => import('../modules/english/pages/public/EnglishWritingPage.vue'), meta: { publicPage: true } },
  { path: '/english/topics/:slug', component: () => import('../modules/english/pages/public/EnglishTopicPage.vue'), meta: { publicPage: true } },
  { path: '/english/writing/author/:slug', component: () => import('../modules/english/pages/public/EnglishReadingPage.vue'), props: { kind: 'writing-articles' }, meta: { publicPage: true } },
  { path: '/english/writing/articles/new', component: () => import('../modules/english/pages/public/EnglishWritingEditorPage.vue'), meta: { requiresAuth: true } },
  { path: '/english/writing/articles/:id/edit', component: () => import('../modules/english/pages/public/EnglishWritingEditorPage.vue'), meta: { requiresAuth: true } },
  { path: '/english/writing/articles/:id/revisions', component: () => import('../modules/english/pages/public/EnglishWritingEditorPage.vue'), meta: { requiresAuth: true } },

  // 认证页自带整屏布局。邀请仍要求登录受邀账户。
  { path: '/login', component: AuthPage, props: { mode: 'login' },
    meta: { authPage: true } },
  { path: '/register', component: AuthPage, props: { mode: 'register' },
    meta: { authPage: true } },
  { path: '/forgot-password', component: AuthPage, props: { mode: 'forgot' },
    meta: { authPage: true } },
  { path: '/reset-password', component: AuthPage, props: { mode: 'reset' },
    meta: { authPage: true } },
  // 邀请只能由被邀请的那个账户接受，因此这条要登录
  { path: '/invitation/accept', component: AuthPage, props: { mode: 'invite' },
    meta: { authPage: true, requiresAuth: true } },

  // console 页面使用带侧栏的管理外壳。
  { path: '/center', component: AccountPage,
    meta: { console: true, requiresAuth: true } },
  /*
   * 学习记录**不属于控制台**：它是个人学习数据的独立视图，和站点治理、内容编辑没有关系。
   * 因此这五条不带 meta.console —— 它们在 PublicShell 里渲染（带公开顶栏与页脚），
   * 用页内的 LearningNav 在这几页之间切换。把它们塞进控制台侧栏会让「学习」和
   * 「后台管理」看起来是同一件事，而学习者根本不需要侧栏里那一堆管理入口。
   * 仍然 requiresAuth：学习记录是账户私有的。
   */
  { path: '/learning', component: () => import('../modules/tutorial/pages/learning/LearningCenterPage.vue'),
    meta: { requiresAuth: true } },
  { path: '/learning/plans', component: () => import('../modules/tutorial/pages/learning/StudyPlanPage.vue'),
    meta: { requiresAuth: true } },
  { path: '/learning/today', redirect: '/learning', meta: { requiresAuth: true } },
  { path: '/learning/sessions/:sessionId', component: () => import('../modules/tutorial/pages/learning/LearningSessionHistoryPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/plans/:planId', component: () => import('../modules/tutorial/pages/learning/StudyPlanPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/tasks/:taskId', component: () => import('../modules/tutorial/pages/learning/StudyTaskPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/chapters/:chapterId', component: () => import('../modules/tutorial/pages/learning/ChapterStudyPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/mastery', component: () => import('../modules/tutorial/pages/learning/MasteryPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/review/:sessionId', component: () => import('../modules/tutorial/pages/learning/ReviewSessionPage.vue'), meta: { requiresAuth: true } },
  { path: '/learning/review', component: () => import('../modules/tutorial/pages/learning/ReviewPage.vue'),
    meta: { requiresAuth: true } },
  { path: '/learning/history', component: () => import('../modules/tutorial/pages/learning/LearningHistoryPage.vue'),
    meta: { requiresAuth: true } },
  // 控制台仪表盘由 Site 汇总各模块的后台摘要。
  { path: '/dashboard', component: DashboardPage,
    meta: { console: true, requiresAuth: true, permission: 'site:dashboard-read', superAdminOnly: true } },
  { path: '/site/settings', component: () => import('../modules/site/pages/admin/SiteSettingsPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'site:config-manage', superAdminOnly: true } },
  { path: '/analytics', component: () => import('../modules/analytics/pages/admin/AnalyticsDashboardPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'analytics:read', superAdminOnly: true } },
  { path: '/accounts', component: AdminAccountsPage,
    meta: { console: true, requiresAuth: true, permission: 'account:read', superAdminOnly: true } },
  { path: '/invitations', component: AdminInvitationsPage,
    meta: { console: true, requiresAuth: true, permission: 'account:invite-admin', superAdminOnly: true } },
  { path: '/audits', component: AccountAuditsPage,
    meta: { console: true, requiresAuth: true, permission: 'account:audit-read', superAdminOnly: true } },
  // 兼容旧的动态权限页路径，它重定向到账户管理
  { path: '/permissions', redirect: '/accounts', meta: { console: true } },
  { path: '/media', component: MediaLibraryPage,
    meta: { console: true, requiresAuth: true, permission: 'media:read' } },
  { path: '/media/:mediaAssetId', component: MediaDetailPage,
    meta: { console: true, requiresAuth: true, permission: 'media:read' } },
  { path: '/reviews', component: ReviewCenterPage,
    meta: { console: true, requiresAuth: true, permission: 'review:read' } },
  { path: '/reviews/:reviewId', component: ReviewDetailPage,
    meta: { console: true, requiresAuth: true, permission: 'review:read' } },
  { path: '/blog/manage', component: () => import('../modules/blog/pages/admin/BlogManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'blog:read-admin' } },
  { path: '/blog/taxonomy', component: () => import('../modules/blog/pages/admin/BlogTaxonomyPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'blog:taxonomy-manage' } },
  // 标签与专题拆成两页：标签在 /blog/taxonomy（旧路径不改，后台导航不用动），专题搬到这里
  { path: '/blog/topics', component: () => import('../modules/blog/pages/admin/BlogTopicManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'blog:taxonomy-manage' } },
  { path: '/blog/editor/:postId', component: () => import('../modules/blog/pages/admin/BlogEditorPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'blog:edit' } },
  { path: '/tutorials/manage', component: () => import('../modules/tutorial/pages/admin/TutorialManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/cards', component: () => import('../modules/tutorial/pages/admin/TutorialExercisesManagePage.vue'), props: { kind: 'cards' },
      meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/questions', component: () => import('../modules/tutorial/pages/admin/TutorialExercisesManagePage.vue'), props: { kind: 'questions' },
      meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/:tutorialId/chapters/:chapterId/cards', component: () => import('../modules/tutorial/pages/admin/ChapterExercisesPage.vue'), props: { kind: 'cards' },
      meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/:tutorialId/chapters/:chapterId/questions', component: () => import('../modules/tutorial/pages/admin/ChapterExercisesPage.vue'), props: { kind: 'questions' },
      meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/exercises', redirect: '/tutorials/cards' },
  { path: '/tutorials/:tutorialId/chapters/:chapterId/exercises', redirect: to => accountPath(`/tutorials/${to.params.tutorialId}/chapters/${to.params.chapterId}/cards`) },
  { path: '/tutorials/editor/:tutorialId', component: () => import('../modules/tutorial/pages/admin/TutorialEditPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/:tutorialId/preview', component: () => import('../modules/tutorial/pages/admin/TutorialPreviewPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/:tutorialId/curriculum', component: () => import('../modules/tutorial/pages/admin/TutorialCurriculumPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/:tutorialId/chapters/:chapterId', component: () => import('../modules/tutorial/pages/admin/ChapterEditPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/english/manage', component: () => import('../modules/english/pages/admin/EnglishManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:content-read-admin' } },
  { path: '/english/manage/overview', component: () => import('../modules/english/pages/admin/EnglishOverviewPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:overview-manage' } },
  { path: '/english/manage/vocabulary', component: () => import('../modules/english/pages/admin/VocabularyManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:content-read-admin' } },
  { path: '/english/manage/grammar', component: () => import('../modules/english/pages/admin/GrammarManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:content-read-admin' } },
  { path: '/english/manage/grammar/lesson/:lessonId', component: () => import('../modules/english/pages/admin/GrammarLessonEditorPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:content-edit' } },
  { path: '/english/manage/reading', component: () => import('../modules/english/pages/admin/EnglishDocumentsManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'english:content-read-admin' } },
  { path: '/english/manage/writing', redirect: () => accountPath('/english/writing'), meta: { requiresAuth: true } },
  { path: '/english/manage/taxonomy', component: () => import('../modules/english/pages/admin/EnglishTaxonomyManagePage.vue'), meta: { requiresAuth: true, console: true, permission: 'english:content-edit' } },
  { path: '/english/manage/editor/reading/:documentId', component: () => import('../modules/english/pages/admin/EnglishReadingEditorPage.vue'), meta: { requiresAuth: true, console: true, permission: 'english:content-edit' } },
  { path: '/portfolio/manage', component: () => import('../modules/portfolio/pages/admin/PortfolioManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'portfolio:read-admin', superAdminOnly: true } },
  { path: '/portfolio/preview/:workId', component: () => import('../modules/portfolio/pages/WorkDetailPage.vue'), meta: { requiresAuth: true, permission: 'portfolio:read-admin', superAdminOnly: true } },
  { path: '/portfolio/editor/:workId', component: () => import('../modules/portfolio/pages/admin/WorkEditorPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'portfolio:edit', superAdminOnly: true } },
  { path: '/messages/manage', component: () => import('../modules/message/pages/admin/MessageManagePage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'message:read-admin', superAdminOnly: true } },
  { path: '/profile/manage', component: () => import('../modules/profile/pages/admin/ProfileEditorPage.vue'),
    meta: { console: true, requiresAuth: true, permission: 'profile:read-admin', superAdminOnly: true } },
]

export const routes = [
  ...routeDeclarations,
  // 旧书签和邮件仅在此兼容；查询参数和片段交给路由器原样带到规范地址。
  { path: '/useradmin/:pathMatch(.*)*', redirect: to => ({
    path: canonicalPath(to.path), query: to.query, hash: to.hash, replace: true,
  }) },
  { path: '/:pathMatch(.*)*', component: () => import('../modules/site/pages/NotFoundPage.vue'),
    meta: { noindex: true } },
]

export function createAppRouter(history = createWebHistory()) {
  const router = createRouter({ history, routes, scrollBehavior: resolveScrollPosition })
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    // 首页和公开内容也显示账号入口；会话初始化不再取决于 URL 前缀。
    if (!auth.initialized) {
      try { await auth.initialize() } catch { /* 表单及具体数据请求负责显示连接错误。 */ }
    }
    if (to.meta.requiresAuth && !auth.currentUser) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    const permissionDenied = to.meta.permission && !(to.meta.superAdminOnly
      ? auth.canManage(to.meta.permission) : auth.hasPermission(to.meta.permission))
    const roleDenied = to.meta.superAdminOnly && !auth.currentUser?.roles?.includes('SUPER_ADMIN')
    if (permissionDenied || roleDenied) return { path: '/center' }
  })
  router.onError(handleRouteLoadError)
  router.afterEach(() => { routeLoadFailed.value = false })
  return router
}

export default createAppRouter()
