<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './modules/account/stores/authStore'
import { errorMessage } from './modules/account/api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const showShell = computed(() => !route.meta.authPage)
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
  <div :class="showShell ? 'app-shell' : ''">
    <aside v-if="showShell" :class="['app-sidebar', menuOpen && 'is-open']">
      <RouterLink to="/account" class="site-brand">
        <img src="/brand/mark.svg" alt="" width="34" height="34" />
        <span>星雨笔录 <small>V2</small></span>
      </RouterLink>
      <p class="sidebar-caption">账户工作区</p>
      <nav aria-label="账户导航" @click="menuOpen = false">
        <RouterLink to="/account">我的账户</RouterLink>
        <RouterLink v-if="auth.canManage('account:read')" to="/admin/accounts">账户管理</RouterLink>
        <RouterLink v-if="auth.canManage('account:invite-admin')" to="/admin/invitations">管理员邀请</RouterLink>
        <RouterLink v-if="auth.canManage('account:audit-read')" to="/admin/audits">账户审计</RouterLink>
      </nav>
      <!-- media:* 权限同时授予 ADMIN 与 SUPER_ADMIN，故这里用 hasPermission 而不是 canManage -->
      <p v-if="auth.hasPermission('media:read') || auth.hasPermission('review:read')" class="sidebar-caption">内容工作区</p>
      <nav
        v-if="auth.hasPermission('media:read') || auth.hasPermission('review:read')"
        aria-label="内容导航"
        @click="menuOpen = false"
      >
        <RouterLink v-if="auth.hasPermission('media:read')" to="/admin/media">媒体库</RouterLink>
        <!-- 审核查看权限同样授予 ADMIN；审批按钮是否出现由详情页按后端返回的 canApprove/canReject 决定 -->
        <RouterLink v-if="auth.hasPermission('review:read')" to="/admin/reviews">审核中心</RouterLink>
      </nav>
    </aside>
    <div :class="showShell ? 'app-body' : ''">
      <header v-if="showShell" class="site-header">
        <div><button class="mobile-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button> 星雨笔录 · 内容中心</div>
        <div class="header-actions"><span>{{ auth.currentUser?.displayName }}</span><button v-if="auth.currentUser" class="text-button" type="button" :disabled="logoutBusy" @click="logout">{{ logoutBusy ? '退出中…' : '退出登录' }}</button><RouterLink v-else to="/login">登录</RouterLink></div>
      </header>
      <p v-if="logoutError" class="error" role="alert">{{ logoutError }}</p>
      <RouterView />
    </div>
  </div>
</template>
