<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { errorMessage } from '../api/http'
import { accountPath } from '../../../shared/viewMode'

/*
 * 前台的账号区 —— 账号模式下顶栏右上角唯一与账号有关的东西。
 *
 * 未登录：「登录 / 注册」；已登录：用户名 + 用户菜单（用户中心 / 退出）。
 * 「用户中心」就是进后台的那扇门：/useradmin/center 是控制台首页（带侧栏），
 * 因此前台不需要再单列一个「进入后台」按钮。
 *
 * 这个组件只由 PublicShell 在账号模式下渲染，公开模式根本不挂载它——
 * 「公开站永远匿名」因此在结构上成立，而不是靠组件内部再判一次模式。
 *
 * 退出后停在账号树的登录页：留在原页面上也可以（那时右上角会显示登录/注册），
 * 但把人送到登录页更符合「我刚退出了」的预期，也避免停在需要登录态的后台页上。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const open = ref(false)
const root = ref(null)
const busy = ref(false)
const failure = ref('')

const displayName = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '')

function toggle() {
  open.value = !open.value
}

function closeOnOutsideClick(event) {
  if (open.value && !root.value?.contains(event.target)) {
    open.value = false
  }
}

function onDocumentKeydown(event) {
  if (event.key === 'Escape' && open.value) {
    open.value = false
  }
}

document.addEventListener('pointerdown', closeOnOutsideClick)
document.addEventListener('keydown', onDocumentKeydown)
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', closeOnOutsideClick)
  document.removeEventListener('keydown', onDocumentKeydown)
})

watch(() => route.fullPath, () => { open.value = false })

async function logout() {
  if (busy.value) return
  busy.value = true
  failure.value = ''
  try {
    await auth.logout()
    await router.replace(accountPath('/login'))
  } catch (error) {
    failure.value = errorMessage(error)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="account-entry">
    <template v-if="auth.currentUser">
      <div ref="root" class="account-entry__menu-root">
        <button
          class="account-entry__trigger"
          type="button"
          :aria-expanded="open"
          aria-haspopup="menu"
          @click="toggle"
        >
          <span class="account-entry__name">{{ displayName }}</span>
          <span aria-hidden="true">{{ open ? '⌃' : '⌄' }}</span>
        </button>
        <div v-if="open" class="account-entry__menu" role="menu">
          <RouterLink class="account-entry__item" role="menuitem" :to="accountPath('/center')">
            用户中心
          </RouterLink>
          <p v-if="failure" class="account-entry__error" role="alert">{{ failure }}</p>
          <button class="account-entry__item is-danger" type="button" role="menuitem" :disabled="busy" @click="logout">
            {{ busy ? '退出中…' : '退出' }}
          </button>
        </div>
      </div>
    </template>

    <template v-else>
      <RouterLink class="account-entry__link" :to="accountPath('/login')">登录</RouterLink>
      <RouterLink class="account-entry__link is-primary" :to="accountPath('/register')">注册</RouterLink>
    </template>
  </div>
</template>

<style scoped>
.account-entry {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.account-entry__menu-root { position: relative; }

.account-entry__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--bg-surface) 82%, transparent);
  cursor: pointer;
  font-size: 13px;
}

.account-entry__trigger:hover,
.account-entry__trigger[aria-expanded='true'] {
  border-color: var(--border-strong);
  color: var(--primary);
}

.account-entry__name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-entry__menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: var(--z-header, 20);
  min-width: 156px;
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  box-shadow: var(--shadow-floating);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.account-entry__item {
  padding: 8px 10px;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  background: transparent;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
}

.account-entry__item:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.account-entry__item.is-danger:hover { color: var(--danger, #c0392b); }

.account-entry__error {
  padding: 4px 10px;
  color: var(--danger, #c0392b);
  font-size: 12px;
}

.account-entry__link {
  padding: 7px 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.account-entry__link:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.account-entry__link.is-primary {
  border-color: var(--border-strong);
  color: var(--text-primary);
}

.account-entry__link.is-primary:hover {
  border-color: var(--primary);
  color: var(--primary);
}
</style>
