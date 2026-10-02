<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'

/*
 * 管理站外壳：SUPER_ADMIN 的控制台。
 *
 * 只放管理站入口的页面：账户治理（账户 / 邀请 / 审计）与博客后台。
 * 媒体库与审核中心属于用户站入口，即使超管有全部权限也不在这里出现，
 * 保持「超管入口与普通用户入口不同」这条设计。
 * 账户治理用 canManage（超管专属），博客后台用 hasPermission（权限码当前只授予超管）。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)
const logoutBusy = ref(false)
const logoutError = ref('')
watch(() => route.fullPath, () => { menuOpen.value = false })

async function logout() {
  if (logoutBusy.value) return
  logoutBusy.value = true
  logoutError.value = ''
  try {
    await auth.logout()
    await router.replace('/login')
  } catch (error) {
    logoutError.value = errorMessage(error)
  } finally {
    logoutBusy.value = false
  }
}
</script>

<template>
  <div class="app-shell">
    <aside :class="['app-sidebar', menuOpen && 'is-open']">
      <RouterLink to="/admin/accounts" class="site-brand">
        <img src="/brand/mark.svg" alt="" width="34" height="34" />
        <span>星雨笔录 <small>管理站</small></span>
      </RouterLink>
      <p class="sidebar-caption">账户治理</p>
      <nav aria-label="账户治理导航" @click="menuOpen = false">
        <RouterLink v-if="auth.canManage('account:read')" to="/admin/accounts">账户管理</RouterLink>
        <RouterLink v-if="auth.canManage('account:invite-admin')" to="/admin/invitations">管理员邀请</RouterLink>
        <RouterLink v-if="auth.canManage('account:audit-read')" to="/admin/audits">账户审计</RouterLink>
      </nav>
      <!--
        博客权限当前只授予 SUPER_ADMIN，但仍用 hasPermission：
        页面该不该显示由「有没有这个权限」决定，角色只是当前唯一的授权来源。
      -->
      <template v-if="auth.hasPermission('blog:read-admin')">
        <p class="sidebar-caption">内容治理</p>
        <nav aria-label="博客导航" @click="menuOpen = false">
          <RouterLink to="/admin/blog">博客文章</RouterLink>
          <RouterLink v-if="auth.hasPermission('blog:taxonomy-manage')" to="/admin/blog/taxonomy">分类与专题</RouterLink>
        </nav>
      </template>
    </aside>
    <div class="app-body">
      <header class="site-header">
        <div>
          <button class="mobile-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button>
          星雨笔录 · 管理站
        </div>
        <div class="header-actions">
          <span>{{ auth.currentUser?.displayName }}</span>
          <button v-if="auth.currentUser" class="text-button" type="button" :disabled="logoutBusy" @click="logout">{{ logoutBusy ? '退出中…' : '退出' }}</button>
          <RouterLink v-else to="/login">登录</RouterLink>
        </div>
      </header>
      <p v-if="logoutError" class="error" role="alert">{{ logoutError }}</p>
      <slot />
    </div>
  </div>
</template>
