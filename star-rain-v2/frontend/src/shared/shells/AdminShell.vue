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
 *   通用        我的账户（/useradmin/center）、学习记录
 *   统计总览    SUPER_ADMIN：仪表盘（导航第一项）
 *   内容编辑    ADMIN 及以上：教程编辑、博客管理、媒体库、审核中心；作品管理仅超管可见
 *   站点治理    SUPER_ADMIN：账户管理、管理员邀请、账户审计
 *
 * 三处刻意的取舍：
 * 1. 没有「英语管理」—— english 模块在 V2 仍为 PAUSED，导航里不出现，
 *    避免给出一个点进去什么也没有的入口。
 * 2. 内容编辑这一层按角色显示，层内每个真实链接仍按权限判可见性；
 *    作品管理只向持有 portfolio:read-admin 的超管显示。
 *
 * 返回前台指向**账号树前台**（accountPath('/') = /useradmin），不是裸 '/'：
 *   裸 '/' 是公开树首页，那是给大众的匿名站，一进去右上角账号区就整个消失了
 *   （公开树按设计永远匿名）。控制台里的「返回前台」是「从后台回到我刚才浏览内容的地方」，
 *   那个地方是账号树前台，因此必须用 accountPath('/') 带上前缀，不能写裸 '/'。
 *   路径由 viewMode.js 的工具函数生成，账号树的字面量只在 viewMode.js 里存在一处。
 * 仍然用整页链接（<a>）而不是 RouterLink：离开控制台时整页重载会顺带丢掉控制台自己的
 *   运行态（侧栏折叠、导航滚动位置、已挂载的各控制台 store），比 SPA 内跳更干净。
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
  { label: '学习记录', short: '学', to: accountPath('/learning') },
]

// 内容编辑层：ADMIN 与 SUPER_ADMIN。角色只决定「这一层出现不出现」，
// 层内每一项再各自按权限判断，避免出现「看得见但进不去」的入口。
const CONTENT_EDITOR_ROLES = ['ADMIN', 'SUPER_ADMIN']
const isContentEditor = computed(() =>
  Boolean(auth.currentUser?.roles?.some((role) => CONTENT_EDITOR_ROLES.includes(role))))
const isSuperAdmin = computed(() => Boolean(auth.currentUser?.roles?.includes('SUPER_ADMIN')))

const contentItems = computed(() => [
  { label: '教程编辑', short: '教', to: accountPath('/tutorials/manage'), visible: auth.hasPermission('tutorial:read-admin') },
  { label: '博客管理', short: '博', to: accountPath('/blog/manage'), visible: auth.hasPermission('blog:read-admin') },
  { label: '媒体库', short: '媒', to: accountPath('/media'), visible: auth.hasPermission('media:read') },
  { label: '作品管理', short: '品', to: accountPath('/portfolio/manage'), visible: auth.hasPermission('portfolio:read-admin') },
  // 审核中心处理内容发布前的审核，放在内容编辑区末尾，按 review:read 控制可见性。
  { label: '审核中心', short: '审', to: accountPath('/reviews'), visible: auth.hasPermission('review:read') },
  { label: '留言管理', short: '言', to: accountPath('/messages/manage'), visible: auth.canManage('message:read-admin') },
])
const visibleContentItems = computed(() => contentItems.value.filter((item) => item.visible))

// 站点治理层：只有 SUPER_ADMIN。每一项仍按账户治理权限判断（canManage 要求既是超管又有该权限）
const governanceItems = computed(() => [
  { label: '账户管理', short: '用', to: accountPath('/accounts'), visible: auth.canManage('account:read') },
  { label: '管理员邀请', short: '邀', to: accountPath('/invitations'), visible: auth.canManage('account:invite-admin') },
  { label: '账户审计', short: '计', to: accountPath('/audits'), visible: auth.canManage('account:audit-read') },
].filter((item) => item.visible))

const governanceActive = computed(() =>
  ['/accounts', '/invitations', '/audits']
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
    // 退出后回账号树前台（accountPath('/') = /useradmin），与前台账号菜单的退出落点统一。
    // 之前这里落 /useradmin/login：同在账号树内，但「我刚退出了」被送到登录页，
    // 与前台菜单的行为不一致；前台首页本来就不需要登录态，停在那里右上角自然变回「登录」。
    await router.replace(accountPath('/'))
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
        <RouterLink
          v-if="auth.canManage('account:read')"
          :to="accountPath('/dashboard')"
          class="admin-shell__nav-item"
          :class="{ 'admin-shell__nav-item--active': isActive(accountPath('/dashboard')) }"
          :title="collapsed ? '仪表盘' : undefined"
        >
          <span class="admin-shell__nav-short">盘</span>
          <span v-if="!collapsed" class="admin-shell__nav-label">仪表盘</span>
        </RouterLink>
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
          <!-- 返回前台 = 回账号树前台 /useradmin（accountPath('/')），不是公开树 '/'，理由见文件头注释 -->
          <a class="admin-shell__header-action" :href="accountPath('/')" title="返回前台（账号树首页）">
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
