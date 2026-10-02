<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'
import * as accountApi from '../../modules/account/api/accountApi'
import { accountPath } from '../viewMode'

/*
 * 控制台外壳（唯一带侧栏的外壳），由账号树的 /useradmin/center 用户中心进入。
 *
 * 侧栏分三层，按角色隐藏 —— 是「看不见」，不是「点了报 403」：
 *   通用        我的账户（/useradmin/center）、学习记录（占位，后续开发）
 *   内容编辑    ADMIN 及以上：教程编辑（占位）、博客管理、媒体库、作品管理（占位）、审核中心
 *   站点治理    SUPER_ADMIN：仪表盘、账户管理、管理员邀请、账户审计
 *
 * 三处刻意的取舍：
 * 1. 没有「英语管理」—— english 模块在 V2 仍为 PAUSED，导航里不出现，
 *    避免给出一个点进去什么也没有的入口。
 * 2. 教程编辑 / 作品管理只有位置、不能点：对应的后端模块还没做，
 *    做成 RouterLink 会直接落到兜底重定向，比「不可点 + 建设中」更糟。
 * 3. 内容编辑这一层的可见性用角色判断（ADMIN / SUPER_ADMIN），而不是逐个权限码：
 *    这一层里有教程编辑 / 作品管理两个占位项，它们没有权限码可判，
 *    角色正好表达「内容编辑者」这个层级；层内每个真实链接仍然各自再判一次权限，
 *    因此「有角色但缺某个权限」时不会看到进不去的入口。
 *
 * 返回前台是一个普通的整页链接（<a href="/">），不是前端路由跳转：
 * 这是唯一需要从账号树跨回公开树的方向，整页加载顺带保证公开树不会带着账号态渲染。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const governanceOpen = ref(localStorage.getItem('admin-governance-group-open') !== 'false')
const mobileOpen = ref(false)
const navRef = ref(null)
const contentRef = ref(null)

watch([collapsed, governanceOpen], () => {
  localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
  localStorage.setItem('admin-governance-group-open', String(governanceOpen.value))
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
// 导航（按角色分层）
// ---------------------------------------------------------------------

const personalItems = [
  { label: '我的账户', short: '我', to: accountPath('/center') },
  // 学习记录属于后续开发：留位置但不做成死链
  { label: '学习记录', short: '学', pending: '学习记录后续开发' },
]

// 内容编辑层：ADMIN 与 SUPER_ADMIN。角色只决定「这一层出现不出现」，
// 层内每一项再各自按权限判断，避免出现「看得见但进不去」的入口。
const CONTENT_EDITOR_ROLES = ['ADMIN', 'SUPER_ADMIN']
const isContentEditor = computed(() =>
  Boolean(auth.currentUser?.roles?.some((role) => CONTENT_EDITOR_ROLES.includes(role))))
const isSuperAdmin = computed(() => Boolean(auth.currentUser?.roles?.includes('SUPER_ADMIN')))

const contentItems = computed(() => [
  { label: '教程编辑', short: '教', pending: '教程模块后端未实现' },
  { label: '博客管理', short: '博', to: accountPath('/blog/manage'), visible: auth.hasPermission('blog:read-admin') },
  { label: '媒体库', short: '媒', to: accountPath('/media'), visible: auth.hasPermission('media:read') },
  { label: '作品管理', short: '品', pending: '作品模块后端未实现' },
  // 审核中心不在所有者给的侧栏清单里，但 review 模块已交付且 ADMIN 持有 review:read；
  // 漏掉它会让整个审核模块在控制台没有入口。见验收文档的偏差记录。
  { label: '审核中心', short: '审', to: accountPath('/reviews'), visible: auth.hasPermission('review:read') },
])
const visibleContentItems = computed(() => contentItems.value.filter((item) => item.pending || item.visible))

// 站点治理层：只有 SUPER_ADMIN。每一项仍按账户治理权限判断（canManage 要求既是超管又有该权限）
const governanceItems = computed(() => [
  { label: '仪表盘', short: '盘', to: accountPath('/dashboard'), visible: auth.canManage('account:read') },
  { label: '账户管理', short: '用', to: accountPath('/accounts'), visible: auth.canManage('account:read') },
  { label: '管理员邀请', short: '邀', to: accountPath('/invitations'), visible: auth.canManage('account:invite-admin') },
  { label: '账户审计', short: '计', to: accountPath('/audits'), visible: auth.canManage('account:audit-read') },
].filter((item) => item.visible))

const governanceActive = computed(() =>
  ['/dashboard', '/accounts', '/invitations', '/audits']
    .some((suffix) => route.path.startsWith(accountPath(suffix))))

function isActive(target) {
  return route.path === target || route.path.startsWith(`${target}/`)
}

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
    await router.replace(accountPath('/login'))
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
    await router.replace({ path: accountPath('/login'), query: { redirect: accountPath('/dashboard') } })
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
        <RouterLink :to="accountPath('/center')" class="admin-shell__brand-link">
          <span v-if="!collapsed">星雨笔录</span>
          <span v-else>星</span>
        </RouterLink>
      </div>

      <nav ref="navRef" class="admin-shell__nav" aria-label="控制台导航" @scroll.passive="rememberNavScroll">
        <!-- 通用：所有登录用户 -->
        <template v-for="item in personalItems" :key="item.label">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="admin-shell__nav-item"
            :class="{ 'admin-shell__nav-item--active': isActive(item.to) }"
            :title="collapsed ? item.label : undefined"
          >
            <span class="admin-shell__nav-short">{{ item.short }}</span>
            <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
          </RouterLink>
          <span
            v-else
            class="admin-shell__nav-item admin-shell__nav-item--pending"
            :title="collapsed ? `${item.label}（建设中）` : item.pending"
            aria-disabled="true"
          >
            <span class="admin-shell__nav-short">{{ item.short }}</span>
            <template v-if="!collapsed">
              <span class="admin-shell__nav-label">{{ item.label }}</span>
              <em class="admin-shell__nav-badge">建设中</em>
            </template>
          </span>
        </template>

        <!-- 内容编辑：ADMIN 及以上 -->
        <template v-if="isContentEditor && visibleContentItems.length">
          <p class="admin-shell__nav-caption">{{ collapsed ? '内' : '内容编辑' }}</p>
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
              :class="{ 'admin-shell__nav-item--active': isActive(item.to) }"
              :title="collapsed ? item.label : undefined"
            >
              <span class="admin-shell__nav-short">{{ item.short }}</span>
              <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
            </RouterLink>
          </template>
        </template>

        <!-- 站点治理：只有 SUPER_ADMIN -->
        <template v-if="isSuperAdmin && governanceItems.length">
          <p class="admin-shell__nav-caption">{{ collapsed ? '治' : '站点治理' }}</p>

          <!-- 折叠态：子项退化成单字图标，避免侧栏被撑开 -->
          <div v-if="collapsed" class="admin-shell__collapsed-subnav">
            <RouterLink
              v-for="item in governanceItems"
              :key="item.label"
              :to="item.to"
              class="admin-shell__nav-item"
              :class="{ 'admin-shell__nav-item--active': isActive(item.to) }"
              :title="item.label"
            >
              <span class="admin-shell__nav-short">{{ item.short }}</span>
            </RouterLink>
          </div>

          <template v-else>
            <button
              type="button"
              class="admin-shell__nav-item"
              :class="{ 'admin-shell__nav-item--active': governanceActive }"
              :aria-expanded="governanceOpen"
              @click="governanceOpen = !governanceOpen"
            >
              <span class="admin-shell__nav-short">治</span>
              <span class="admin-shell__nav-label">治理操作</span>
              <span class="admin-shell__nav-chevron">{{ governanceOpen ? '⌃' : '⌄' }}</span>
            </button>
            <div v-if="governanceOpen" class="admin-shell__subnav">
              <RouterLink
                v-for="item in governanceItems"
                :key="item.label"
                :to="item.to"
                class="admin-shell__subnav-item"
                :class="{ 'is-active': isActive(item.to) }"
              >{{ item.label }}</RouterLink>
            </div>
          </template>
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
          <button class="admin-shell__mobile-menu" type="button" aria-label="打开控制台导航" @click="mobileOpen = true">☰</button>
          <span class="admin-shell__header-title-long">星雨笔录 · 用户中心</span>
          <span class="admin-shell__header-title-short">用户中心</span>
        </div>
        <div class="admin-shell__header-actions">
          <!-- 返回前台是整页跳转：账号树 → 公开树是跨树切换，整页加载保证公开树不带账号态 -->
          <a class="admin-shell__header-action" href="/" title="返回前台（公开站首页）">
            <span class="admin-shell__header-action-long">返回前台</span>
            <span class="admin-shell__header-action-short">前台</span>
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
