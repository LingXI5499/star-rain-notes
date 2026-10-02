import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../modules/account/stores/authStore'
import AuthPage from '../modules/account/pages/AuthPage.vue'
import AccountPage from '../modules/account/pages/AccountPage.vue'
import AdminAccountsPage from '../modules/account/pages/AdminAccountsPage.vue'
import AdminInvitationsPage from '../modules/account/pages/AdminInvitationsPage.vue'
import AccountAuditsPage from '../modules/account/pages/AccountAuditsPage.vue'
import MediaLibraryPage from '../modules/media/pages/MediaLibraryPage.vue'
import MediaDetailPage from '../modules/media/pages/MediaDetailPage.vue'
import ReviewCenterPage from '../modules/review/pages/ReviewCenterPage.vue'
import ReviewDetailPage from '../modules/review/pages/ReviewDetailPage.vue'
import BlogListPage from '../modules/blog/pages/BlogListPage.vue'
import BlogPostPage from '../modules/blog/pages/BlogPostPage.vue'
import BlogArchivePage from '../modules/blog/pages/BlogArchivePage.vue'
import BlogManagePage from '../modules/blog/pages/admin/BlogManagePage.vue'
import BlogEditorPage from '../modules/blog/pages/admin/BlogEditorPage.vue'
import BlogTaxonomyPage from '../modules/blog/pages/admin/BlogTaxonomyPage.vue'

/*
 * 路由表。
 *
 * meta.permission      —— 只要求「具备该权限」，ADMIN 与 SUPER_ADMIN 都可通过
 * meta.superAdminOnly  —— 账户治理类页面，必须是 SUPER_ADMIN
 * 两者分开是因为 media:* 与 review:read 权限同时授予了 ADMIN，而 account 治理权限只属于 SUPER_ADMIN。
 *
 * 审核模块的审批权限（review:approve / review:reject）只授予 SUPER_ADMIN，
 * 但那是「能否执行动作」的判断，由详情页按后端返回的 canApprove / canReject 渲染按钮，
 * 页面本身只要求 review:read —— 普通 ADMIN 依然应该能看待审列表与详情。
 * meta.publicPage      —— 前台公开页面（博客列表 / 阅读 / 归档），匿名可读，不套后台侧栏
 *
 * 三种分开是因为它们的判定口径不同：media:* 同时授予了 ADMIN，
 * account 治理权限只属于 SUPER_ADMIN，而博客前台根本不需要登录。
 * 博客权限当前只授予 SUPER_ADMIN，因此后台博客页面用 meta.permission 即可，
 * 再叠一层 superAdminOnly 只会把「权限模型」和「角色模型」混成一句话。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/account' },
    { path: '/login', component: AuthPage, props: { mode: 'login' }, meta: { authPage: true } },
    { path: '/register', component: AuthPage, props: { mode: 'register' }, meta: { authPage: true } },
    { path: '/forgot-password', component: AuthPage, props: { mode: 'forgot' }, meta: { authPage: true } },
    { path: '/reset-password', component: AuthPage, props: { mode: 'reset' }, meta: { authPage: true } },
    { path: '/invitation/accept', component: AuthPage, props: { mode: 'invite' },
      meta: { authPage: true, requiresAuth: true } },
    { path: '/account', component: AccountPage, meta: { requiresAuth: true } },
    { path: '/admin/accounts', component: AdminAccountsPage,
      meta: { requiresAuth: true, permission: 'account:read', superAdminOnly: true } },
    { path: '/admin/permissions', redirect: '/admin/accounts' },
    { path: '/admin/invitations', component: AdminInvitationsPage,
      meta: { requiresAuth: true, permission: 'account:invite-admin', superAdminOnly: true } },
    { path: '/admin/audits', component: AccountAuditsPage,
      meta: { requiresAuth: true, permission: 'account:audit-read', superAdminOnly: true } },
    { path: '/admin/media', component: MediaLibraryPage,
      meta: { requiresAuth: true, permission: 'media:read' } },
    { path: '/admin/media/:mediaAssetId', component: MediaDetailPage,
      meta: { requiresAuth: true, permission: 'media:read' } },
    { path: '/admin/reviews', component: ReviewCenterPage,
      meta: { requiresAuth: true, permission: 'review:read' } },
    { path: '/admin/reviews/:reviewId', component: ReviewDetailPage,
      meta: { requiresAuth: true, permission: 'review:read' } },
    // 前台博客：匿名可读，meta.publicPage 让外壳不渲染后台侧栏
    { path: '/blog', component: BlogListPage, meta: { publicPage: true } },
    { path: '/blog/archive', component: BlogArchivePage, meta: { publicPage: true } },
    { path: '/blog/posts/:slug', component: BlogPostPage, meta: { publicPage: true } },
    // 后台博客：权限码只授予 SUPER_ADMIN，见 V2_007__blog.sql
    { path: '/admin/blog', component: BlogManagePage,
      meta: { requiresAuth: true, permission: 'blog:read-admin' } },
    { path: '/admin/blog/taxonomy', component: BlogTaxonomyPage,
      meta: { requiresAuth: true, permission: 'blog:taxonomy-manage' } },
    { path: '/admin/blog/posts/:postId', component: BlogEditorPage,
      meta: { requiresAuth: true, permission: 'blog:edit' } },
    { path: '/:pathMatch(.*)*', redirect: '/account' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    try { await auth.initialize() } catch { /* The login form displays connection failures on submission. */ }
  }
  if (to.meta.requiresAuth && !auth.currentUser) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.permission) {
    const allowed = to.meta.superAdminOnly
      ? auth.canManage(to.meta.permission)
      : auth.hasPermission(to.meta.permission)
    if (!allowed) return { path: '/account' }
  }
})

export default router
