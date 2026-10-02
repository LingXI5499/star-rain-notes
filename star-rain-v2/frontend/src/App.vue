<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './modules/account/stores/authStore'
import { errorMessage } from './modules/account/api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
// 登录/注册页与前台公开页都不套后台侧栏；前台页另有一套极简页头
const showShell = computed(() => !route.meta.authPage && !route.meta.publicPage)
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
      <p v-if="auth.hasPermission('media:read')" class="sidebar-caption">内容工作区</p>
      <nav v-if="auth.hasPermission('media:read')" aria-label="内容导航" @click="menuOpen = false">
        <RouterLink to="/admin/media">媒体库</RouterLink>
      </nav>
      <!--
        博客权限当前只授予 SUPER_ADMIN，但仍用 hasPermission：
        页面该不该显示由「有没有这个权限」决定，角色只是当前唯一的授权来源。
      -->
      <nav v-if="auth.hasPermission('blog:read-admin')" aria-label="博客导航" @click="menuOpen = false">
        <RouterLink to="/admin/blog">博客文章</RouterLink>
        <RouterLink v-if="auth.hasPermission('blog:taxonomy-manage')" to="/admin/blog/taxonomy">分类与专题</RouterLink>
      </nav>
      <nav aria-label="前台入口" @click="menuOpen = false">
        <RouterLink to="/blog">博客前台</RouterLink>
        <RouterLink to="/blog/archive">归档浏览</RouterLink>
      </nav>
    </aside>
    <div :class="showShell ? 'app-body' : ''">
      <header v-if="showShell" class="site-header">
        <div><button class="mobile-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button> 星雨笔录 · 内容中心</div>
        <div class="header-actions"><span>{{ auth.currentUser?.displayName }}</span><button v-if="auth.currentUser" class="text-button" type="button" :disabled="logoutBusy" @click="logout">{{ logoutBusy ? '退出中…' : '退出登录' }}</button><RouterLink v-else to="/login">登录</RouterLink></div>
      </header>
      <!-- 前台公开页面的极简页头：匿名读者也能在博客与归档之间切换 -->
      <header v-else-if="route.meta.publicPage" class="site-header public-header">
        <RouterLink to="/blog" class="site-brand">星雨笔录 <small>博客</small></RouterLink>
        <nav class="public-nav">
          <RouterLink to="/blog">文章</RouterLink>
          <RouterLink to="/blog/archive">归档</RouterLink>
          <RouterLink v-if="auth.hasPermission('blog:read-admin')" to="/admin/blog">后台</RouterLink>
          <RouterLink v-else-if="!auth.currentUser" to="/login">登录</RouterLink>
        </nav>
      </header>
      <p v-if="logoutError" class="error" role="alert">{{ logoutError }}</p>
      <RouterView />
    </div>
  </div>
</template>
