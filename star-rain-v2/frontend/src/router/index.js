import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../modules/account/stores/authStore'
import {
  ROUTE_TREE,
  VIEW_MODE,
  accountPath,
  buildTreeRoutes,
  modeHomePath,
  resolveViewMode,
} from '../shared/viewMode'
import AuthPage from '../modules/account/pages/AuthPage.vue'
import AccountPage from '../modules/account/pages/AccountPage.vue'
import AdminAccountsPage from '../modules/account/pages/AdminAccountsPage.vue'
import AdminInvitationsPage from '../modules/account/pages/AdminInvitationsPage.vue'
import AccountAuditsPage from '../modules/account/pages/AccountAuditsPage.vue'
import MediaLibraryPage from '../modules/media/pages/MediaLibraryPage.vue'
import MediaDetailPage from '../modules/media/pages/MediaDetailPage.vue'
import ReviewCenterPage from '../modules/review/pages/ReviewCenterPage.vue'
import ReviewDetailPage from '../modules/review/pages/ReviewDetailPage.vue'
import HomePage from '../modules/site/pages/HomePage.vue'
import PendingPage from '../modules/site/pages/PendingPage.vue'
import DashboardPage from '../modules/site/pages/DashboardPage.vue'

/*
 * 路由表 —— 一个域名，两条路径树。
 *
 * 下面的清单是**唯一真源**：每条路由只写一次，`tree` 决定它进哪棵树，
 * 由 shared/viewMode.js 的 buildTreeRoutes() 生成实际记录：
 *   tree 省略（both）—— 两棵树各一条（同一组件，账号树补 /useradmin 前缀），公开页不再手抄第二份；
 *   tree 'account'   —— 只在账号树（登录、注册、用户中心、后台），公开树里根本不存在这些页面；
 *   tree 'public'    —— 只在公开树（当前没有这种路由，字段留给后续）。
 * 清单里的路径一律写**公开树形态**（'/blog'、'/center'），账号树的实际地址由 accountPath() 推出来。
 *
 * meta 语义：
 *   meta.tree          —— 路由所属的路径树，守卫先用它判模式；由 buildTreeRoutes 写入，不手写
 *   meta.authPage      —— 整屏认证页，不套外壳
 *   meta.console       —— 控制台页面，套 AdminShell（侧栏）
 *   meta.publicPage    —— 公开内容页（博客列表 / 阅读 / 归档），匿名可读
 *   meta.requiresAuth  —— 需要登录
 *   meta.permission    —— 只要求「具备该权限」，ADMIN 与 SUPER_ADMIN 都可通过
 *   meta.superAdminOnly—— 账户治理类页面，必须是 SUPER_ADMIN
 * 两者分开是因为 media:* 与 review:read 权限同时授予了 ADMIN，而账户治理权限只属于 SUPER_ADMIN。
 *
 * 审核模块的审批权限（review:approve / review:reject）只授予 SUPER_ADMIN，
 * 但那是「能否执行动作」的判断，由详情页按后端返回的 canApprove / canReject 渲染按钮，
 * 页面本身只要求 review:read —— 普通 ADMIN 依然应该能看待审列表与详情。
 *
 * 账号树的后台路径刻意不用 /useradmin/blog/posts/:postId 这一形状：
 * /useradmin/blog、/useradmin/blog/archive、/useradmin/blog/posts/:slug 已经被公开树镜像占用，
 * 后台再放一个同形的 :postId 记录会与 :slug 冲突。后台博客因此收进
 * /useradmin/blog/manage、/useradmin/blog/taxonomy、/useradmin/blog/editor/:postId，
 * 整块仍在所有者要求的 /useradmin/blog/** 之下。
 */
const routeDeclarations = [
  /*
   * ---------- 两棵树都有的公开内容页 ----------
   *
   * 公开树：/ 匿名站首页；账号树：/useradmin 账号模式首页（同一套内容，右上角带账号区）。
   * 两个入口各自有首页，因此 '/' 这条记录在两棵树上都生成。
   */
  { path: '/', component: HomePage },
  { path: '/blog', component: () => import('../modules/blog/pages/BlogListPage.vue'), meta: { publicPage: true } },
  { path: '/blog/archive', component: () => import('../modules/blog/pages/BlogArchivePage.vue'), meta: { publicPage: true } },
  { path: '/blog/posts/:slug', component: () => import('../modules/blog/pages/BlogPostPage.vue'), meta: { publicPage: true } },

  // 教程已接入公开列表、详情与章节；其余内容模块仍使用建设中页面。
  { path: '/tutorials', component: () => import('../modules/tutorial/pages/public/TutorialCatalogPage.vue'), meta: { publicPage: true } },
  { path: '/tutorials/:tutorialSlug', component: () => import('../modules/tutorial/pages/public/TutorialDetailPage.vue'), meta: { publicPage: true } },
  { path: '/tutorials/:tutorialSlug/:chapterSlug', component: () => import('../modules/tutorial/pages/public/TutorialChapterPage.vue'), meta: { publicPage: true } },
  { path: '/portfolio', component: () => import('../modules/portfolio/pages/PortfolioListPage.vue'), meta: { publicPage: true } },
  { path: '/portfolio/:slug', component: () => import('../modules/portfolio/pages/WorkDetailPage.vue'), meta: { publicPage: true } },
  { path: '/messages', component: () => import('../modules/message/pages/MessageBoardPage.vue'), meta: { publicPage: true } },
  { path: '/about', component: () => import('../modules/profile/pages/ProfilePage.vue'), meta: { publicPage: true } },
  { path: '/search', component: () => import('../modules/search/pages/SearchResultPage.vue'), meta: { publicPage: true } },

  /*
   * ---------- 只在账号树：认证页 ----------
   *
   * 公开树里没有这些路由，这是「公开站不暴露注册登录」这条保证的全部实现——
   * 域名方案下它是服务端保证（按 Host 对注册端点回 403），路径方案下服务端没有可依据的
   * 请求特征，因此降级成前端路由级保证。缺口写在
   * docs/开发文档/路径入口返工验收.md 的「已知缺口」里，不粉饰。
   *
   * meta.authPage 的页面自带整屏布局，不套外壳：它们是账号树里唯一没有顶栏的页面，
   * 因此也不会出现「右上角账号区里再放一个登录入口」的套娃。
   */
  { path: '/login', component: AuthPage, props: { mode: 'login' }, tree: ROUTE_TREE.ACCOUNT,
    meta: { authPage: true } },
  { path: '/register', component: AuthPage, props: { mode: 'register' }, tree: ROUTE_TREE.ACCOUNT,
    meta: { authPage: true } },
  { path: '/forgot-password', component: AuthPage, props: { mode: 'forgot' }, tree: ROUTE_TREE.ACCOUNT,
    meta: { authPage: true } },
  { path: '/reset-password', component: AuthPage, props: { mode: 'reset' }, tree: ROUTE_TREE.ACCOUNT,
    meta: { authPage: true } },
  // 邀请只能由被邀请的那个账户接受，因此这条要登录
  { path: '/invitation/accept', component: AuthPage, props: { mode: 'invite' }, tree: ROUTE_TREE.ACCOUNT,
    meta: { authPage: true, requiresAuth: true } },

  /*
   * ---------- 只在账号树：控制台 ----------
   *
   * meta.console 让 App.vue 套 AdminShell（带侧栏）。控制台从「用户中心」进入，
   * 侧栏项按角色隐藏，见 shared/shells/AdminShell.vue。
   */
  { path: '/center', component: AccountPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  { path: '/learning', component: () => import('../modules/tutorial/pages/learning/LearningCenterPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  { path: '/learning/plans', component: () => import('../modules/tutorial/pages/learning/StudyPlanPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  { path: '/learning/today', component: () => import('../modules/tutorial/pages/learning/TodayStudyPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  { path: '/learning/review', component: () => import('../modules/tutorial/pages/learning/ReviewPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  { path: '/learning/history', component: () => import('../modules/tutorial/pages/learning/LearningHistoryPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true } },
  // 控制台仪表盘由 Site 汇总各模块的后台摘要。
  { path: '/dashboard', component: DashboardPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'site:dashboard-read', superAdminOnly: true } },
  { path: '/site/settings', component: () => import('../modules/site/pages/admin/SiteSettingsPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'site:config-manage', superAdminOnly: true } },
  { path: '/analytics', component: () => import('../modules/analytics/pages/admin/AnalyticsDashboardPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'analytics:read', superAdminOnly: true } },
  { path: '/accounts', component: AdminAccountsPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'account:read', superAdminOnly: true } },
  { path: '/invitations', component: AdminInvitationsPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'account:invite-admin', superAdminOnly: true } },
  { path: '/audits', component: AccountAuditsPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'account:audit-read', superAdminOnly: true } },
  // 兼容旧的动态权限页路径，它重定向到账户管理
  { path: '/permissions', redirect: '/accounts', tree: ROUTE_TREE.ACCOUNT, meta: { console: true } },
  { path: '/media', component: MediaLibraryPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'media:read' } },
  { path: '/media/:mediaAssetId', component: MediaDetailPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'media:read' } },
  { path: '/reviews', component: ReviewCenterPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'review:read' } },
  { path: '/reviews/:reviewId', component: ReviewDetailPage, tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'review:read' } },
  { path: '/blog/manage', component: () => import('../modules/blog/pages/admin/BlogManagePage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'blog:read-admin' } },
  { path: '/blog/taxonomy', component: () => import('../modules/blog/pages/admin/BlogTaxonomyPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'blog:taxonomy-manage' } },
  { path: '/blog/editor/:postId', component: () => import('../modules/blog/pages/admin/BlogEditorPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'blog:edit' } },
  { path: '/tutorials/manage', component: () => import('../modules/tutorial/pages/admin/TutorialManagePage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/editor/:tutorialId', component: () => import('../modules/tutorial/pages/admin/TutorialEditPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/tutorials/:tutorialId/preview', component: () => import('../modules/tutorial/pages/admin/TutorialPreviewPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/:tutorialId/curriculum', component: () => import('../modules/tutorial/pages/admin/TutorialCurriculumPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'tutorial:read-admin' } },
  { path: '/tutorials/:tutorialId/chapters/:chapterId', component: () => import('../modules/tutorial/pages/admin/ChapterEditPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'tutorial:edit' } },
  { path: '/portfolio/manage', component: () => import('../modules/portfolio/pages/admin/PortfolioManagePage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'portfolio:read-admin', superAdminOnly: true } },
  { path: '/portfolio/editor/:workId', component: () => import('../modules/portfolio/pages/admin/WorkEditorPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'portfolio:edit', superAdminOnly: true } },
  { path: '/messages/manage', component: () => import('../modules/message/pages/admin/MessageManagePage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'message:read-admin', superAdminOnly: true } },
  { path: '/profile/manage', component: () => import('../modules/profile/pages/admin/ProfileEditorPage.vue'), tree: ROUTE_TREE.ACCOUNT,
    meta: { console: true, requiresAuth: true, permission: 'profile:read-admin', superAdminOnly: true } },
]

const routes = buildTreeRoutes(routeDeclarations)

/*
 * 兜底：未匹配的路径按**它自己所在的树**回到该树首页。
 *
 * 这一条承担了两件在验收里能看到的行为：
 *   1. /login、/account、/admin/blog 这类账号树路径（不带 /useradmin 前缀）在公开树里根本不存在，
 *      于是回落到公开树首页 /；
 *   2. /useradmin/不存在的东西 回落到 /useradmin，不会掉出账号外壳。
 * 路径树由前缀解析，因此这里不需要再判断「当前在哪棵树」。
 */
routes.push({
  path: '/:pathMatch(.*)*',
  redirect: (to) => modeHomePath(resolveViewMode(to.path)),
})

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/*
 * 导航守卫的判定顺序：先判模式，再判登录态，最后判权限。
 *
 * 模式必须最先判：账号树的控制台页面在公开树下没有任何意义，
 * 不该进入登录态与权限判断（否则未登录用户会被送进账号树的登录页）。
 *
 * 说明一处容易被误读的地方：在当前的路由生成方式下，路由记录的路径前缀与它声明的树**必然一致**
 * （account 记录都由 accountPath() 生成），所以第一段判断实际是一道不变量检查，正常导航不会命中它。
 * 让「公开树下输入 /login 落到 /」真正生效的是路由表本身——公开树里没有 /login 这条记录，
 * 未匹配路径落进上面的兜底重定向。这一段留在守卫里是为了让「先判模式」这条约束写在代码里，
 * 而不是只存在于生成规则中。
 */
router.beforeEach(async (to) => {
  const mode = resolveViewMode(to.path)

  if (to.meta.tree && to.meta.tree !== ROUTE_TREE.BOTH && to.meta.tree !== mode) {
    const home = modeHomePath(mode)
    if (to.path !== home) {
      return { path: home }
    }
  }

  const auth = useAuthStore()
  /*
   * 公开树刻意**不**初始化会话：不在公开树问「我是谁」，公开树就不可能渲染出账号 UI。
   * 这比「取回会话但什么都不渲染」更强一层，也是硬要求「公开站永远匿名」的纵深防御。
   * 账号树必须初始化：右上角账号区要知道该显示「登录」按钮还是用户标识菜单。
   * （公开模式干脆不渲染账号区，所以公开树这一层不成立，上面那条才写的是硬要求。）
   */
  if (!auth.initialized && (to.meta.requiresAuth || mode === VIEW_MODE.ACCOUNT)) {
    try {
      await auth.initialize()
    } catch {
      // 连接失败只影响「当前是否已登录」的判断，具体错误由登录表单在提交时提示
    }
  }

  if (to.meta.requiresAuth && !auth.currentUser) {
    return { path: accountPath('/login'), query: { redirect: to.fullPath } }
  }

  if (to.meta.permission) {
    const allowed = to.meta.superAdminOnly
      ? auth.canManage(to.meta.permission)
      : auth.hasPermission(to.meta.permission)
    /*
     * 权限不足落到用户中心（/useradmin/center），不是公开树首页：
     * 用户中心对所有角色开放且没有任何权限要求，因此不会形成重定向循环，
     * 也不会把已登录的人莫名其妙送回公开站。
     */
    if (!allowed) {
      return { path: accountPath('/center') }
    }
  }
})

export default router
