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
    </aside>
    <div :class="showShell ? 'app-body' : ''">
      <header v-if="showShell" class="site-header">
        <div><button class="mobile-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button> 星雨笔录 · 账户中心</div>
        <div class="header-actions"><span>{{ auth.currentUser?.displayName }}</span><button v-if="auth.currentUser" class="text-button" type="button" :disabled="logoutBusy" @click="logout">{{ logoutBusy ? '退出中…' : '退出登录' }}</button><RouterLink v-else to="/login">登录</RouterLink></div>
      </header>
      <p v-if="logoutError" class="error" role="alert">{{ logoutError }}</p>
      <RouterView />
    </div>
  </div>
</template>
