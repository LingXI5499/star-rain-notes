<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import '@/styles/admin.css'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const collapsed = ref(false)

const navItems = [
  { to: '/admin', label: '仪表盘', short: '盘', match: (path: string) => path === '/admin' },
  { to: '/admin/tutorials', label: '教程工作台', short: '教', match: (path: string) => path.startsWith('/admin/tutorials') },
  { to: '/admin/blog', label: '博客管理', short: '博', match: (path: string) => path.startsWith('/admin/blog') },
  {
    to: '/admin/portfolio',
    label: '作品管理',
    short: '品',
    match: (path: string) => path.startsWith('/admin/portfolio'),
  },
  { to: '/admin/english', label: '英语管理', short: '英', match: (path: string) => path.startsWith('/admin/english') },
  {
    to: '/admin/vocabulary',
    label: '词汇管理',
    short: '词',
    match: (path: string) => path.startsWith('/admin/vocabulary'),
  },
  { to: '/admin/about', label: '关于管理', short: '关', match: (path: string) => path.startsWith('/admin/about') },
  { to: '/admin/media', label: '媒体库', short: '媒', match: (path: string) => path.startsWith('/admin/media') },
  {
    to: '/admin/settings',
    label: '站点设置',
    short: '站',
    match: (path: string) => path.startsWith('/admin/settings'),
  },
]

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
    <aside class="admin-shell__sidebar" :class="{ 'admin-shell__sidebar--collapsed': collapsed }">
      <div class="admin-shell__brand">
        <RouterLink to="/admin" class="admin-shell__brand-link">
          <span v-if="!collapsed">星雨笔录</span>
          <span v-else>星</span>
        </RouterLink>
      </div>

      <nav class="admin-shell__nav" aria-label="管理导航">
        <RouterLink
          v-for="item in navItems"
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
        <div class="admin-shell__header-title">星雨笔录 · 管理控制台</div>
        <div class="admin-shell__header-actions">
          <RouterLink to="/" class="admin-shell__header-action" title="查看站点">查看站点</RouterLink>
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

      <main class="admin-shell__content">
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
  min-height: 100vh;
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
}

.admin-shell__header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-inline: var(--space-6);
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border);
}

.admin-shell__header-title {
  font-weight: 600;
  color: var(--text-primary);
}

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
}

.admin-shell__header-action:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.admin-shell__content {
  flex: 1;
  padding: var(--space-8);
}
</style>
