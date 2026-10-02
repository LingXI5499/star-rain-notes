<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'

/*
 * 用户站外壳：USER 与 ADMIN 共用。
 *
 * 导航只放「用户站入口」允许的页面：个人中心，以及 ADMIN 的内容协作能力（媒体库、审核中心）。
 * 账户治理与博客后台属于管理站，不在这里出现——即使当前登录的是超管也一样，
 * 否则用户站会变成管理站的影子入口，入口隔离就没有意义了。
 * 权限判断仍用 hasPermission：能不能看见由「有没有这个权限」决定，角色只是当前的授权来源。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)
const logoutBusy = ref(false)
const logoutError = ref('')
const showWorkspace = computed(() => auth.hasPermission('media:read') || auth.hasPermission('review:read'))
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
      <RouterLink to="/account" class="site-brand">
        <img src="/brand/mark.svg" alt="" width="34" height="34" />
        <span>星雨笔录 <small>用户站</small></span>
      </RouterLink>
      <p class="sidebar-caption">个人</p>
      <nav aria-label="个人导航" @click="menuOpen = false">
        <RouterLink to="/account">我的账户</RouterLink>
      </nav>
      <!-- media:* 与 review:read 权限同时授予 ADMIN 与 SUPER_ADMIN，故这里用 hasPermission 而不是 canManage -->
      <template v-if="showWorkspace">
        <p class="sidebar-caption">内容协作</p>
        <nav aria-label="内容协作导航" @click="menuOpen = false">
          <RouterLink v-if="auth.hasPermission('media:read')" to="/admin/media">媒体库</RouterLink>
          <!-- 审批按钮是否出现由详情页按后端返回的 canApprove/canReject 决定 -->
          <RouterLink v-if="auth.hasPermission('review:read')" to="/admin/reviews">审核中心</RouterLink>
        </nav>
      </template>
    </aside>
    <div class="app-body">
      <header class="site-header">
        <div>
          <button class="mobile-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button>
          星雨笔录 · 用户站
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
