<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'
import ThemeControl from '../../modules/site/components/ThemeControl.vue'

/*
 * 用户站外壳：USER 与 ADMIN 共用。
 *
 * 导航只放「用户站入口」允许的页面：个人中心，以及 ADMIN 的内容协作能力（媒体库、审核中心）。
 * 账户治理与博客后台属于管理站，不在这里出现——即使当前登录的是超管也一样，
 * 否则用户站会变成管理站的影子入口，入口隔离就没有意义了。
 * 权限判断仍用 hasPermission：能不能看见由「有没有这个权限」决定，角色只是当前的授权来源。
 *
 * 侧栏与顶栏的视觉对齐 V1 `layouts/AdminLayout.vue` 的组织方式：
 * 品牌 → 分组（组标题 + 带短标的导航项）→ 顶栏右侧放主题切换与账号操作。
 * 这里刻意**不**放 V1 顶栏的「查看站点」入口：`/` 属于公开站，
 * 从用户站域名点过去会被入口守卫送回 /account，那会变成一个点了没反应的死链。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)
const logoutBusy = ref(false)
const logoutError = ref('')
const showWorkspace = computed(() => auth.hasPermission('media:read') || auth.hasPermission('review:read'))

const personalNav = [{ to: '/account', label: '我的账户', short: '我' }]
// media:* 与 review:read 权限同时授予 ADMIN 与 SUPER_ADMIN，故这里用 hasPermission 而不是 canManage
const workspaceNav = computed(() => [
  { to: '/admin/media', label: '媒体库', short: '媒', visible: auth.hasPermission('media:read') },
  // 审批按钮是否出现由详情页按后端返回的 canApprove/canReject 决定
  { to: '/admin/reviews', label: '审核中心', short: '审', visible: auth.hasPermission('review:read') },
].filter((item) => item.visible))

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
  <div class="user-shell">
    <button v-if="menuOpen" class="user-shell__scrim" type="button" aria-label="关闭导航" @click="menuOpen = false" />
    <aside class="user-shell__sidebar" :class="{ 'is-open': menuOpen }">
      <RouterLink to="/account" class="user-shell__brand">
        <img src="/brand/mark.svg" alt="" width="30" height="30" />
        <span>星雨笔录 <small>用户站</small></span>
      </RouterLink>

      <nav class="user-shell__nav" aria-label="个人导航" @click="menuOpen = false">
        <p class="user-shell__caption">个人</p>
        <RouterLink
          v-for="item in personalNav"
          :key="item.to"
          :to="item.to"
          class="user-shell__nav-item"
        >
          <span class="user-shell__nav-short" aria-hidden="true">{{ item.short }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>

        <template v-if="showWorkspace">
          <p class="user-shell__caption">内容协作</p>
          <RouterLink
            v-for="item in workspaceNav"
            :key="item.to"
            :to="item.to"
            class="user-shell__nav-item"
          >
            <span class="user-shell__nav-short" aria-hidden="true">{{ item.short }}</span>
            <span>{{ item.label }}</span>
          </RouterLink>
        </template>
      </nav>
    </aside>

    <div class="user-shell__body">
      <header class="user-shell__header">
        <div class="user-shell__title">
          <button class="user-shell__menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button>
          星雨笔录 · 用户站
        </div>
        <div class="user-shell__actions">
          <ThemeControl />
          <span v-if="auth.currentUser" class="user-shell__who">{{ auth.currentUser.displayName }}</span>
          <button v-if="auth.currentUser" class="user-shell__action" type="button" :disabled="logoutBusy" @click="logout">
            {{ logoutBusy ? '退出中…' : '退出' }}
          </button>
          <RouterLink v-else class="user-shell__action" to="/login">登录</RouterLink>
        </div>
      </header>
      <p v-if="logoutError" class="error" role="alert">{{ logoutError }}</p>
      <main class="user-shell__content">
        <!-- 页面由 App.vue 以插槽传入（外壳不自己渲染 RouterView，避免渲染两次） -->
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
/*
 * 侧栏与顶栏的类名沿用 account.css 里已有的 `.app-shell` / `.app-sidebar` 体系之外的新前缀，
 * 避免与后台外壳（AdminShell，属另一路改动）共用样式而互相牵动。
 */
.user-shell {
  display: flex;
  min-height: 100dvh;
  background: var(--bg-page);
}

.user-shell__sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 30;
  width: 236px;
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
}

.user-shell__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 750;
}

.user-shell__brand small { color: var(--accent); font-size: 10px; }

.user-shell__nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 12px 24px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-shell__caption {
  margin: 14px 0 6px;
  padding-inline: 12px;
  color: var(--text-muted);
  font-size: 10px;
  letter-spacing: 0.14em;
}

.user-shell__caption:first-child { margin-top: 0; }

.user-shell__nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 9px;
  color: var(--text-secondary);
  font-size: 14px;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.user-shell__nav-item:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.user-shell__nav-item.router-link-active {
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  color: var(--primary);
  font-weight: 650;
}

.user-shell__nav-short {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: var(--bg-subtle);
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
}

.user-shell__nav-item.router-link-active .user-shell__nav-short {
  background: var(--primary);
  color: var(--on-primary);
}

.user-shell__body {
  flex: 1;
  min-width: 0;
  margin-left: 236px;
  display: flex;
  flex-direction: column;
}

.user-shell__header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-inline: 28px;
  background: color-mix(in srgb, var(--bg-surface) 92%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.user-shell__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
  font-weight: 600;
}

.user-shell__menu {
  display: none;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.user-shell__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-shell__who {
  color: var(--text-secondary);
  font-size: 13px;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-shell__action {
  padding: 7px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
}

.user-shell__action:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.user-shell__content {
  flex: 1;
  min-width: 0;
}

.user-shell__scrim { display: none; }

@media (max-width: 960px) {
  .user-shell__sidebar {
    width: min(300px, 84vw);
    transform: translateX(-102%);
    transition: transform 0.2s ease;
    box-shadow: 0 24px 70px rgb(0 0 0 / 0.28);
  }

  .user-shell__sidebar.is-open { transform: translateX(0); }

  .user-shell__body { margin-left: 0; }

  .user-shell__menu { display: inline-grid; place-items: center; }

  .user-shell__header { padding-inline: 14px; }

  .user-shell__who { display: none; }

  .user-shell__scrim {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 25;
    border: 0;
    background: rgb(5 12 10 / 0.48);
    backdrop-filter: blur(2px);
  }
}
</style>
