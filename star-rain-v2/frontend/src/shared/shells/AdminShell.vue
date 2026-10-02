<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'
import * as accountApi from '../../modules/account/api/accountApi'

/*
 * 管理站外壳（控制台）：对齐 V1 layouts/AdminLayout.vue 的信息架构。
 *
 * 侧栏按分组组织：仪表盘 / 账户协作（用户管理·邀请管理·审核中心·审计日志）/
 * 教程工作台 / 博客管理 / 作品管理。
 *
 * 三处刻意的取舍：
 * 1. 没有「英语管理」—— english 模块在 V2 仍为 PAUSED，导航里不出现，
 *    避免给出一个点进去什么也没有的入口。
 * 2. 教程工作台、作品管理只有位置、不能点：对应的后端模块还没做，
 *    做成 RouterLink 会直接落到 404 兜底重定向，比「不可点」更糟。
 * 3. 审核中心属于用户站入口（entryRoutes.js 里 /admin/reviews/** 归 user），
 *    管理站域名下直接跳过去会被入口守卫挡回本入口首页，因此这里给出的是
 *    用户站的绝对地址。会话 Cookie 按域名隔离，跨站过去需要重新登录——
 *    这是三入口设计的既有行为，不是本页的缺陷。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const accountGroupOpen = ref(localStorage.getItem('admin-account-group-open') !== 'false')
const mobileOpen = ref(false)
const navRef = ref(null)
const contentRef = ref(null)

watch([collapsed, accountGroupOpen], () => {
  localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
  localStorage.setItem('admin-account-group-open', String(accountGroupOpen.value))
})

function rememberNavScroll() {
  if (navRef.value) sessionStorage.setItem('admin-nav-scroll', String(navRef.value.scrollTop))
}

async function revealActiveItem() {
  await nextTick()
  navRef.value?.querySelector('.is-active, .admin-shell__nav-item--active')?.scrollIntoView({ block: 'nearest' })
}

watch(() => route.fullPath, async () => {
  mobileOpen.value = false
  contentRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  await revealActiveItem()
})

onMounted(async () => {
  await nextTick()
  if (navRef.value) navRef.value.scrollTop = Number(sessionStorage.getItem('admin-nav-scroll') || 0)
  await revealActiveItem()
})

// ---------------------------------------------------------------------
// 导航
// ---------------------------------------------------------------------

const dashboardItem = { to: '/admin/dashboard', label: '仪表盘', short: '盘' }

/*
 * 账户协作分组：前三项是本入口的页面，第四项「审核中心」在用户站，
 * 用 external 标记渲染成普通 a 标签（见文件头第 3 条）。
 */
const accountItems = [
  { label: '用户管理', short: '用', to: '/admin/accounts', permission: 'account:read' },
  { label: '邀请管理', short: '邀', to: '/admin/invitations', permission: 'account:invite-admin' },
  { label: '审核中心', short: '审', permission: 'review:read', external: true },
  { label: '审计日志', short: '计', to: '/admin/audits', permission: 'account:audit-read' },
]

const visibleAccountItems = computed(() =>
  accountItems.filter((item) => (item.external ? auth.hasPermission(item.permission) : auth.canManage(item.permission))),
)

const contentItems = [
  // 后端模块未实现：保留侧栏位置，明确标注建设中且不可点
  { label: '教程工作台', short: '教', pending: true },
  { label: '博客管理', short: '博', to: '/admin/blog', permission: 'blog:read-admin' },
  { label: '作品管理', short: '品', pending: true },
]

const visibleContentItems = computed(() =>
  contentItems.filter((item) => (item.pending ? true : auth.hasPermission(item.permission))),
)

const accountGroupActive = computed(() =>
  ['/admin/accounts', '/admin/invitations', '/admin/audits', '/admin/reviews']
    .some((prefix) => route.path.startsWith(prefix)),
)
const blogActive = computed(() => route.path.startsWith('/admin/blog'))

// ---------------------------------------------------------------------
// 站点地址：管理站域名换掉前缀就是公开站 / 用户站
// ---------------------------------------------------------------------

function siblingOrigin(prefix) {
  const { protocol, hostname, port } = window.location
  const suffix = port ? `:${port}` : ''
  // admin.localhost → localhost（公开站）；admin.yulanlin.cn → user.yulanlin.cn（用户站）
  if (hostname.startsWith('admin.')) {
    const rest = hostname.slice('admin.'.length)
    return `${protocol}//${prefix ? `${prefix}.` : ''}${rest}${suffix}`
  }
  // 已经在公开站域名下（例如 127.0.0.1 直连）时，用户站没有独立域名，退回同源
  return `${protocol}//${prefix ? `${prefix}.` : ''}${hostname}${suffix}`
}

const publicSiteUrl = computed(() => {
  const configured = import.meta.env.VITE_PUBLIC_SITE_ORIGIN
  return typeof configured === 'string' && configured.trim() !== '' ? configured.trim() : siblingOrigin('')
})

/*
 * 页面需要「跳到公开站」的地方（例如列表里的文章预览）从外壳取同一个地址，
 * 避免每个页面各写一份域名推导。注入的是 computed，模板里会自动解包。
 */
provide('adminPublicSiteUrl', publicSiteUrl)

// 审核中心在用户站入口，这里给绝对地址（见文件头第 3 条）
const reviewCenterUrl = computed(() => {
  const configured = import.meta.env.VITE_USER_SITE_ORIGIN
  const origin = typeof configured === 'string' && configured.trim() !== ''
    ? configured.trim().replace(/\/$/, '')
    : siblingOrigin('user')
  return `${origin}/admin/reviews`
})

// ---------------------------------------------------------------------
// 主题
//
// 主题目前只在控制台提供切换：tokens.css 已经声明了 [data-theme='dark'] 的整套变量，
// 这里只负责把选择写进 <html data-theme>，不引入额外的主题 store。
// 选择持久化在 localStorage，下次进入控制台立即生效（在 setup 里同步应用，避免闪白）。
// ---------------------------------------------------------------------

const theme = ref('light')

function applyStoredTheme() {
  const stored = localStorage.getItem('admin-theme')
  theme.value = stored === 'dark' || stored === 'light'
    ? stored
    : (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  document.documentElement.dataset.theme = theme.value
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  document.documentElement.dataset.theme = theme.value
  localStorage.setItem('admin-theme', theme.value)
}

applyStoredTheme()

// ---------------------------------------------------------------------
// 账户菜单 / 修改密码 / 退出
// ---------------------------------------------------------------------

const accountMenuOpen = ref(false)
const menuRef = ref(null)
const passwordDialog = ref(null)
const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordError = ref('')
const passwordBusy = ref(false)
const logoutBusy = ref(false)

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value
}

function closeAccountMenu(event) {
  if (accountMenuOpen.value && !menuRef.value?.contains(event.target)) accountMenuOpen.value = false
}

document.addEventListener('pointerdown', closeAccountMenu)
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeAccountMenu))
watch(() => route.fullPath, () => { accountMenuOpen.value = false })

async function logout() {
  if (logoutBusy.value) return
  logoutBusy.value = true
  try {
    await auth.logout()
  } catch {
    // 退出失败（会话已失效等）也把本地状态清掉，不要把人留在后台里
    auth.currentUser = null
  } finally {
    logoutBusy.value = false
    await router.replace('/login')
  }
}

function closePasswordDialog() {
  passwordDialog.value?.close()
}

async function openPasswordDialog() {
  accountMenuOpen.value = false
  passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  await nextTick()
  passwordDialog.value?.showModal()
}

async function submitPassword() {
  const form = passwordForm.value
  if (!form.currentPassword || !form.newPassword) {
    passwordError.value = '请填写当前密码与新密码。'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致。'
    return
  }
  passwordBusy.value = true
  passwordError.value = ''
  try {
    await accountApi.changePassword(form)
    // 后端在修改密码后立即失效会话，这里引导重新登录
    passwordDialog.value?.close()
    auth.currentUser = null
    await router.replace({ path: '/login', query: { redirect: '/admin/dashboard' } })
  } catch (cause) {
    passwordError.value = errorMessage(cause)
  } finally {
    passwordBusy.value = false
  }
}
</script>

<template>
  <div class="admin-shell">
    <button
      v-if="mobileOpen"
      class="admin-shell__scrim is-open"
      type="button"
      aria-label="关闭导航"
      @click="mobileOpen = false"
    />

    <aside :class="['admin-shell__sidebar', collapsed && 'admin-shell__sidebar--collapsed', mobileOpen && 'is-mobile-open']">
      <div class="admin-shell__brand">
        <RouterLink to="/admin/dashboard" class="admin-shell__brand-link">
          <span v-if="!collapsed">星雨笔录</span>
          <span v-else>星</span>
        </RouterLink>
      </div>

      <nav ref="navRef" class="admin-shell__nav" aria-label="管理导航" @scroll.passive="rememberNavScroll">
        <RouterLink
          :to="dashboardItem.to"
          class="admin-shell__nav-item"
          :class="{ 'admin-shell__nav-item--active': route.path === dashboardItem.to }"
          :title="collapsed ? dashboardItem.label : undefined"
        >
          <span class="admin-shell__nav-short">{{ dashboardItem.short }}</span>
          <span v-if="!collapsed" class="admin-shell__nav-label">{{ dashboardItem.label }}</span>
        </RouterLink>

        <div v-if="visibleAccountItems.length" class="admin-shell__nav-group">
          <button
            type="button"
            class="admin-shell__nav-item"
            :class="{ 'admin-shell__nav-item--active': accountGroupActive }"
            :title="collapsed ? '账户协作' : undefined"
            @click="collapsed ? router.push('/admin/accounts') : (accountGroupOpen = !accountGroupOpen)"
          >
            <span class="admin-shell__nav-short">账</span>
            <span v-if="!collapsed" class="admin-shell__nav-label">账户协作</span>
            <span v-if="!collapsed" class="admin-shell__nav-chevron">{{ accountGroupOpen ? '⌃' : '⌄' }}</span>
          </button>

          <!-- 折叠态：子项退化成单字图标，避免侧栏被撑开 -->
          <div v-if="collapsed" class="admin-shell__collapsed-subnav">
            <template v-for="item in visibleAccountItems" :key="item.label">
              <RouterLink
                v-if="item.to"
                :to="item.to"
                class="admin-shell__nav-item"
                :class="{ 'admin-shell__nav-item--active': route.path.startsWith(item.to) }"
                :title="item.label"
              >
                <span class="admin-shell__nav-short">{{ item.short }}</span>
              </RouterLink>
              <a v-else :href="reviewCenterUrl" class="admin-shell__nav-item" title="审核中心（用户站）">
                <span class="admin-shell__nav-short">{{ item.short }}</span>
              </a>
            </template>
          </div>

          <div v-else-if="accountGroupOpen" class="admin-shell__subnav">
            <template v-for="item in visibleAccountItems" :key="item.label">
              <RouterLink
                v-if="item.to"
                :to="item.to"
                class="admin-shell__subnav-item"
                :class="{ 'is-active': route.path.startsWith(item.to) }"
              >{{ item.label }}</RouterLink>
              <a
                v-else
                :href="reviewCenterUrl"
                class="admin-shell__subnav-item admin-shell__subnav-item--external"
                title="审核中心在用户站入口；会话 Cookie 按域名隔离，过去需要重新登录"
              >{{ item.label }} ↗</a>
            </template>
          </div>
        </div>

        <template v-for="item in visibleContentItems" :key="item.label">
          <span
            v-if="item.pending"
            class="admin-shell__nav-item admin-shell__nav-item--pending"
            :title="collapsed ? `${item.label}（建设中）` : '模块建设中，暂未开放'"
            aria-disabled="true"
          >
            <span class="admin-shell__nav-short">{{ item.short }}</span>
            <template v-if="!collapsed">
              <span class="admin-shell__nav-label">{{ item.label }}</span>
              <em class="admin-shell__nav-badge">建设中</em>
            </template>
          </span>
          <RouterLink
            v-else
            :to="item.to"
            class="admin-shell__nav-item"
            :class="{ 'admin-shell__nav-item--active': item.to === '/admin/blog' && blogActive }"
            :title="collapsed ? item.label : undefined"
          >
            <span class="admin-shell__nav-short">{{ item.short }}</span>
            <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
          </RouterLink>
        </template>
      </nav>

      <button
        class="admin-shell__collapse"
        type="button"
        :title="collapsed ? '展开侧栏' : '收起侧栏'"
        @click="collapsed = !collapsed"
      >
        {{ collapsed ? '»' : '«' }}
      </button>
    </aside>

    <div class="admin-shell__body">
      <header class="admin-shell__header">
        <div class="admin-shell__header-title">
          <button class="admin-shell__mobile-menu" type="button" aria-label="打开管理导航" @click="mobileOpen = true">☰</button>
          <span class="admin-shell__header-title-long">星雨笔录 · 管理控制台</span>
          <span class="admin-shell__header-title-short">管理台</span>
        </div>
        <div class="admin-shell__header-actions">
          <a class="admin-shell__header-action" :href="publicSiteUrl" title="查看站点（公开站）">
            <span class="admin-shell__header-action-long">查看站点</span>
            <span class="admin-shell__header-action-short">站点</span>
          </a>
          <button
            class="admin-shell__header-action"
            type="button"
            :title="theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
            @click="toggleTheme"
          >
            {{ theme === 'dark' ? '浅色' : '深色' }}<span class="admin-shell__header-action-long">主题</span>
          </button>
          <div ref="menuRef" class="admin-shell__account">
            <button
              class="admin-shell__header-action"
              type="button"
              :aria-expanded="accountMenuOpen"
              aria-haspopup="menu"
              @click="toggleAccountMenu"
            >
              {{ auth.currentUser?.displayName || auth.currentUser?.username || '账号' }}
            </button>
            <div v-if="accountMenuOpen" class="admin-shell__account-menu" role="menu">
              <button type="button" role="menuitem" @click="openPasswordDialog">修改密码</button>
              <hr />
              <button type="button" role="menuitem" class="is-danger" :disabled="logoutBusy" @click="logout">
                {{ logoutBusy ? '退出中…' : '退出登录' }}
              </button>
            </div>
          </div>
        </div>
      </header>

      <main ref="contentRef" class="admin-shell__content">
        <slot />
      </main>
    </div>

    <dialog ref="passwordDialog" aria-labelledby="password-title" @cancel.prevent="closePasswordDialog">
      <h2 id="password-title">修改密码</h2>
      <form class="form-stack" @submit.prevent="submitPassword">
        <label>当前密码<input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" /></label>
        <label>新密码<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" /></label>
        <label>确认新密码<input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" /></label>
        <p v-if="passwordError" class="error" role="alert">{{ passwordError }}</p>
        <p class="form-hint">修改成功后当前会话立即失效，需要重新登录。</p>
        <div class="dialog-actions">
          <button type="button" @click="closePasswordDialog">取消</button>
          <button class="primary-button" type="submit" :disabled="passwordBusy">{{ passwordBusy ? '提交中…' : '确认修改' }}</button>
        </div>
      </form>
    </dialog>
  </div>
</template>
