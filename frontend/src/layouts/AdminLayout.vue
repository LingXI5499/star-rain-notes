<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import '@/styles/admin.css'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const englishOpen = ref(localStorage.getItem('admin-english-open') !== 'false')
const accountOpen = ref(localStorage.getItem('admin-account-open') !== 'false')
const mobileOpen = ref(false)
const navRef = ref<HTMLElement | null>(null)
const contentRef = ref<HTMLElement | null>(null)

function persistNavigation() {
  localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
  localStorage.setItem('admin-english-open', String(englishOpen.value))
  localStorage.setItem('admin-account-open', String(accountOpen.value))
}

function rememberNavScroll() {
  if (navRef.value) sessionStorage.setItem('admin-nav-scroll', String(navRef.value.scrollTop))
}

async function revealActiveNavigation() {
  await nextTick()
  const active = navRef.value?.querySelector<HTMLElement>('.is-active, .admin-shell__nav-item--active')
  active?.scrollIntoView({ block: 'nearest' })
}

watch([collapsed, englishOpen, accountOpen], persistNavigation)
watch(() => route.fullPath, async () => {
  mobileOpen.value = false
  contentRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  await revealActiveNavigation()
})

onMounted(async () => {
  await nextTick()
  if (navRef.value) navRef.value.scrollTop = Number(sessionStorage.getItem('admin-nav-scroll') || 0)
  await revealActiveNavigation()
})

const mainNavItems = [
  { to: '/admin', label: '仪表盘', short: '盘', match: (path: string) => path === '/admin' },
]

const contentNavItems = [
  { to: '/admin/tutorials', label: '教程工作台', short: '教', match: (path: string) => path.startsWith('/admin/tutorials') },
  { to: '/admin/blog', label: '博客管理', short: '博', match: (path: string) => path.startsWith('/admin/blog') },
  {
    to: '/admin/portfolio',
    label: '作品管理',
    short: '品',
    match: (path: string) => path.startsWith('/admin/portfolio'),
    superOnly: true,
  },
]

const accountItems = [
  { to: '/admin/users', label: '用户管理', badge: '', match: (path: string) => path.startsWith('/admin/users') },
  { to: '/admin/invites', label: '邀请管理', badge: '', match: (path: string) => path.startsWith('/admin/invites') },
  { to: '/admin/reviews', label: '审核中心', badge: '', match: (path: string) => path.startsWith('/admin/reviews') },
  { to: '/admin/audit-logs', label: '审计日志', badge: '', match: (path: string) => path.startsWith('/admin/audit-logs') },
]

const secondaryNavItems = [
  { to: '/admin/about', label: '关于管理', short: '关', match: (path: string) => path.startsWith('/admin/about'), superOnly: true },
  { to: '/admin/media', label: '媒体库', short: '媒', match: (path: string) => path.startsWith('/admin/media') },
  {
    to: '/admin/settings',
    label: '站点设置',
    short: '站',
    match: (path: string) => path.startsWith('/admin/settings'),
    superOnly: true,
  },
]

const englishItems = [
  { to: '/admin/english', label: '英语工作台', badge: '' },
  { to: '/admin/english/analytics', label: '学习分析', badge: '', superOnly: true },
  { to: '/admin/english/overview', label: '总览设置', badge: '' },
  { to: '/admin/english/vocabulary', label: '单词管理', badge: '' },
  { to: '/admin/english/vocabulary/families', label: '词族管理', badge: '' },
  { to: '/admin/english/grammar', label: '语法教程', badge: '' },
  { to: '/admin/english/taxonomy', label: '标签管理', badge: '' },
  { to: '/admin/english/bundles', label: '学习组合', badge: '' },
  { to: '/admin/english/reading', label: '阅读管理', badge: '' },
  { to: '/admin/english/listening', label: '听力管理', badge: '' },
  { to: '/admin/english/listening/pronunciation', label: '语音规则', badge: '' },
  { to: '/admin/english/writing', label: '写作管理', badge: '' },
]

const visibleMainNavItems = computed(() => mainNavItems)
const visibleContentNavItems = computed(() => contentNavItems.filter((item) => !item.superOnly || auth.isSuperAdmin))
const visibleSecondaryNavItems = computed(() => secondaryNavItems.filter((item) => !item.superOnly || auth.isSuperAdmin))
const visibleEnglishItems = computed(() => englishItems.filter((item) => !item.superOnly || auth.isSuperAdmin))

const passwordDialogOpen = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const passwordSaving = ref(false)

async function toggleTheme() {
  theme.setMode(theme.mode === 'dark' ? 'light' : 'dark')
}

async function handleLogout() {
  await auth.logout()
  await router.push({ name: 'admin-login' })
}

async function changePassword() {
  if (!currentPassword.value || !newPassword.value) {
    ElMessage.warning('请填写当前密码与新密码。')
    return
  }
  passwordSaving.value = true
  try {
    await auth.changePassword(currentPassword.value, newPassword.value)
    ElMessage.success('密码已修改，请重新登录。')
    passwordDialogOpen.value = false
    await router.push({ name: 'admin-login' })
  } catch {
    ElMessage.error('修改失败，请检查当前密码。')
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <div class="admin-shell">
    <button v-if="mobileOpen" class="admin-shell__scrim" type="button" aria-label="关闭导航" @click="mobileOpen = false" />
    <aside class="admin-shell__sidebar" :class="{ 'admin-shell__sidebar--collapsed': collapsed, 'is-mobile-open': mobileOpen }">
      <div class="admin-shell__brand">
        <RouterLink to="/admin" class="admin-shell__brand-link">
          <span v-if="!collapsed">星雨笔录</span>
          <span v-else>星</span>
        </RouterLink>
      </div>

      <nav ref="navRef" class="admin-shell__nav" aria-label="管理导航" @scroll.passive="rememberNavScroll">
        <RouterLink
          v-for="item in visibleMainNavItems"
          :key="item.to"
          :to="item.to"
          class="admin-shell__nav-item"
          :class="{ 'admin-shell__nav-item--active': item.match($route.path) }"
          :title="collapsed ? item.label : undefined"
        >
          <span class="admin-shell__nav-short">{{ item.short }}</span>
          <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
        </RouterLink>

        <div v-if="auth.isSuperAdmin" class="admin-shell__nav-group">
          <button
            type="button"
            class="admin-shell__nav-item admin-shell__nav-parent"
            :class="{ 'admin-shell__nav-item--active': ['/admin/users', '/admin/invites', '/admin/reviews', '/admin/audit-logs'].some(prefix => $route.path.startsWith(prefix)) }"
            :title="collapsed ? '账号协作' : undefined"
            @click="collapsed ? router.push('/admin/users') : (accountOpen = !accountOpen)"
          >
            <span class="admin-shell__nav-short">账</span>
            <span v-if="!collapsed" class="admin-shell__nav-label">账号协作</span>
            <span v-if="!collapsed" class="admin-shell__nav-chevron">{{ accountOpen ? '⌃' : '⌄' }}</span>
          </button>
          <div v-if="!collapsed && accountOpen" class="admin-shell__subnav">
            <RouterLink
              v-for="item in accountItems"
              :key="item.to"
              :to="item.to"
              class="admin-shell__subnav-item"
              :class="{ 'is-active': item.match($route.path) }"
            >{{ item.label }}</RouterLink>
          </div>
        </div>

        <RouterLink
          v-for="item in visibleContentNavItems"
          :key="item.to"
          :to="item.to"
          class="admin-shell__nav-item"
          :class="{ 'admin-shell__nav-item--active': item.match($route.path) }"
          :title="collapsed ? item.label : undefined"
        >
          <span class="admin-shell__nav-short">{{ item.short }}</span>
          <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
        </RouterLink>

        <div class="admin-shell__nav-group">
          <button
            type="button"
            class="admin-shell__nav-item admin-shell__nav-parent"
            :class="{ 'admin-shell__nav-item--active': $route.path.startsWith('/admin/english') || $route.path.startsWith('/admin/vocabulary') }"
            :title="collapsed ? '英语管理' : undefined"
            @click="collapsed ? router.push('/admin/english') : (englishOpen = !englishOpen)"
          >
            <span class="admin-shell__nav-short">英</span>
            <span v-if="!collapsed" class="admin-shell__nav-label">英语管理</span>
            <span v-if="!collapsed" class="admin-shell__nav-chevron">{{ englishOpen ? '⌃' : '⌄' }}</span>
          </button>
          <div v-if="!collapsed && englishOpen" class="admin-shell__subnav">
            <template v-for="item in visibleEnglishItems" :key="item.label">
              <RouterLink
                v-if="item.to"
                :to="item.to"
                class="admin-shell__subnav-item"
                :class="{ 'is-active': $route.path === item.to || (item.to !== '/admin/english' && !visibleEnglishItems.some(other => other.to !== item.to && other.to.startsWith(`${item.to}/`) && $route.path.startsWith(other.to)) && $route.path.startsWith(`${item.to}/`)) }"
              >{{ item.label }}</RouterLink>
              <span v-else class="admin-shell__subnav-item is-disabled">{{ item.label }}<em>{{ item.badge }}</em></span>
            </template>
          </div>
        </div>
        <RouterLink
          v-for="item in visibleSecondaryNavItems"
          :key="item.to"
          :to="item.to"
          class="admin-shell__nav-item"
          :class="{ 'admin-shell__nav-item--active': item.match($route.path) }"
          :title="collapsed ? item.label : undefined"
        >
          <span class="admin-shell__nav-short">{{ item.short }}</span>
          <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
        </RouterLink>
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
          <RouterLink to="/" class="admin-shell__header-action" title="查看站点">
            <span class="admin-shell__header-action-long">查看站点</span>
            <span class="admin-shell__header-action-short">站点</span>
          </RouterLink>
          <button class="admin-shell__header-action" type="button" title="切换主题" @click="toggleTheme">
            主题
          </button>
          <el-dropdown trigger="click">
            <button class="admin-shell__header-action" type="button">
              {{ auth.username || '账号' }}
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="passwordDialogOpen = true">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main ref="contentRef" class="admin-shell__content">
        <RouterView />
      </main>
    </div>

    <el-dialog v-model="passwordDialogOpen" title="修改密码" width="420px">
      <el-form label-position="top" @submit.prevent="changePassword">
        <el-form-item label="当前密码">
          <el-input v-model="currentPassword" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" autocomplete="new-password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--bg-page);
}

.admin-shell__sidebar {
  width: var(--admin-sidebar-width);
  flex-shrink: 0;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  height: 100%;
  position: relative;
  z-index: 30;
}

.admin-shell__sidebar--collapsed {
  width: var(--admin-sidebar-collapsed);
}

.admin-shell__brand {
  padding: var(--space-6) var(--space-5);
  border-bottom: 1px solid var(--border);
}

.admin-shell__brand-link {
  font-weight: 700;
  font-size: 18px;
  color: var(--primary);
  white-space: nowrap;
}

.admin-shell__nav {
  flex: 1;
  padding: var(--space-4) var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.admin-shell__nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 15px;
  white-space: nowrap;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.admin-shell__nav-item:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.admin-shell__nav-item--active {
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  color: var(--primary);
  font-weight: 600;
}

.admin-shell__nav-short {
  width: 20px;
  text-align: center;
  font-weight: 600;
}
.admin-shell__nav-parent { width: 100%; border: 0; cursor: pointer; text-align: left; }
.admin-shell__nav-chevron { margin-left: auto; font-size: 12px; }
.admin-shell__subnav { margin: 3px 0 7px 42px; padding-left: 12px; border-left: 1px solid var(--border); display: grid; gap: 2px; }
.admin-shell__subnav-item { padding: 7px 9px; border-radius: 7px; color: var(--text-muted); font-size: 13px; transition: color .16s ease, background-color .16s ease; }
.admin-shell__subnav-item:hover,.admin-shell__subnav-item.is-active { color: var(--primary); background: color-mix(in srgb,var(--primary) 8%,transparent); }
.admin-shell__subnav-item.is-disabled { display:flex; justify-content:space-between; cursor:not-allowed; opacity:.56; }
.admin-shell__subnav-item em { font-style:normal; font-size:10px; }

.admin-shell__collapse {
  margin: var(--space-4);
  padding: var(--space-2);
  border: none;
  border-radius: 8px;
  background: var(--bg-subtle);
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.admin-shell__collapse:hover {
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  color: var(--primary);
}

.admin-shell__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.admin-shell__header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-inline: var(--space-6);
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.admin-shell__header-title {
  font-weight: 600;
  color: var(--text-primary);
}
.admin-shell__header-title-short, .admin-shell__header-action-short { display: none; }

.admin-shell__header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.admin-shell__header-action {
  font-size: 14px;
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-shell__header-action:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.admin-shell__content {
  flex: 1;
  padding: var(--space-8);
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}
.admin-shell__mobile-menu,.admin-shell__scrim { display: none; }
@media (max-width: 720px) {
  .admin-shell__sidebar { position:fixed;inset:0 auto 0 0;width:min(310px,86vw);transform:translateX(-102%);box-shadow:0 24px 70px rgba(0,0,0,.28);transition:transform .2s ease; }
  .admin-shell__sidebar.is-mobile-open { transform:translateX(0); }
  .admin-shell__sidebar--collapsed { width:min(310px,86vw); }
  .admin-shell__brand { padding: 18px 20px; }
  .admin-shell__nav { padding: 12px; }
  .admin-shell__nav-item { justify-content: flex-start; padding: 10px 12px; }
  .admin-shell__nav-label, .admin-shell__nav-chevron { display: inline; }
  .admin-shell__subnav { display:grid; }
  .admin-shell__collapse { display: none; }
  .admin-shell__scrim { display:block;position:fixed;inset:0;z-index:20;border:0;background:rgba(5,12,10,.48);backdrop-filter:blur(2px); }
  .admin-shell__mobile-menu { display:inline-grid;place-items:center;width:34px;height:34px;margin-right:8px;border:1px solid var(--border);border-radius:8px;background:var(--bg-subtle);color:var(--text-primary); }
  .admin-shell__header-title { display:flex;align-items:center; }
  .admin-shell__header { padding-inline: 14px; }
  .admin-shell__header-title { font-size: 13px; }
  .admin-shell__header-title-long, .admin-shell__header-action-long { display: none; }
  .admin-shell__header-title-short, .admin-shell__header-action-short { display: inline; }
  .admin-shell__header-actions { gap: 2px; }
  .admin-shell__header-action { max-width: 92px; padding: 7px 5px; font-size: 12px; }
  .admin-shell__content { padding: 18px 14px; }
}
</style>
