import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../modules/account/stores/authStore'
import AuthPage from '../modules/account/pages/AuthPage.vue'
import AccountPage from '../modules/account/pages/AccountPage.vue'
import AdminAccountsPage from '../modules/account/pages/AdminAccountsPage.vue'
import AdminInvitationsPage from '../modules/account/pages/AdminInvitationsPage.vue'
import AccountAuditsPage from '../modules/account/pages/AccountAuditsPage.vue'

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
      meta: { requiresAuth: true, permission: 'account:read' } },
    { path: '/admin/permissions', redirect: '/admin/accounts' },
    { path: '/admin/invitations', component: AdminInvitationsPage,
      meta: { requiresAuth: true, permission: 'account:invite-admin' } },
    { path: '/admin/audits', component: AccountAuditsPage,
      meta: { requiresAuth: true, permission: 'account:audit-read' } },
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
  if (to.meta.permission && !auth.canManage(to.meta.permission)) {
    return { path: '/account' }
  }
})

export default router

