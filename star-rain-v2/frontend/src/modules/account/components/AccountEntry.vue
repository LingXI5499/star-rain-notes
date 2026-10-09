<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { errorMessage } from '../api/http'
import { accountPath } from '../../../shared/viewMode'

/*
 * 前台的账号区 —— 账号模式下顶栏右上角唯一与账号有关的东西。
 *
 * 未登录：**只有「登录」一个按钮，不再并列「注册」**。
 *   顶栏是导航区，不是注册入口：登录与注册并排，会让所有老用户长期看着一个
 *   自己再也用不上的按钮，也把「注册」抬成和「登录」同等常用的动作。
 *   注册是一次性动作，收进登录页底部那一行（见 AuthPage.vue 的「还没有账号？注册」），
 *   未注册的访客顺着那一行一样能找到；注册页本身保留（所有者明确要「可以有登录注册的页面」），
 *   只是不在顶栏并列。
 *
 * 已登录：显示**用户标识**（首字母圆标 + 用户名），点开是恰好三项的下拉菜单
 *   （进入后台 / 查看个人资料 / 退出登录）。为什么是这三项、为什么这样分组，写在模板里。
 *
 * 这个组件只由 PublicShell 在账号模式下渲染，公开模式根本不挂载它——
 * 「公开站永远匿名」因此在结构上成立，而不是靠组件内部再判一次模式。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const open = ref(false)
const root = ref(null)
const busy = ref(false)
const failure = ref('')

const displayName = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '')

/*
 * 用户标识里的首字母，与「我的账户」页（AccountPage.vue 的 initial）同一口径：
 * 拉丁字母统一大写，兜底「我」。圆标是纯 CSS 画的，不额外请求头像资源。
 */
const initial = computed(() => {
  const source = displayName.value.trim() || '我'
  const first = source.slice(0, 1)
  return /[a-z]/.test(first) ? first.toUpperCase() : first
})

/*
 * 「进入后台」的落点是**角色感知**的，不能写死一个地址。
 *
 * 原因：/useradmin/dashboard（仪表盘）在路由表里带 superAdminOnly，只有超管放行
 * （见 router/index.js 的守卫）。菜单若统一指向它，USER / ADMIN 点下去会被守卫改写成
 * /useradmin/center —— 从用户视角就是「点了没反应 / 点了又跳回原地」，
 * 而这正是所有者「按角色区分后台完善度」的口径要避免的。
 *
 * 因此这里直接复用它自己的能力判定 auth.canManage('account:read')：与守卫里
 * `to.meta.superAdminOnly ? auth.canManage(permission) : auth.hasPermission(permission)`
 * 是同一个函数、同一套角色数据，不再写一份角色字符串比较——
 * 组件里写死 'SUPER_ADMIN' 会在权限口径调整后与守卫悄悄脱钩，而这种脱钩没人会发现。
 * 能进仪表盘就去仪表盘，否则去所有登录用户都能进的控制台首页 /center。
 */
const consolePath = computed(() => (auth.canManage('account:read') ? accountPath('/dashboard') : accountPath('/center')))

function toggle() {
  open.value = !open.value
}

/*
 * 关闭行为：点组件外任何位置、按 Esc、以及路由变化都收起菜单。
 * 菜单是浮层，不关闭就会一直盖在页面内容上；路由变化时更要关，
 * 否则从菜单点进去以后菜单还停在原地，看起来像没生效。
 */
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
    // 退出后回到账号树前台（/useradmin），不是公开站也不是登录页：
    // 前台首页本来就不需要登录态，停在那里右上角自然变回「登录」，
    // 比被弹到登录页更符合「我只是退出了账号」的预期，也不会掉出账号外壳。
    await router.replace(accountPath('/'))
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
        <!-- 用户标识：首字母圆标 + 用户名。整体就是下拉菜单的触发器，点它展开菜单 -->
        <button
          class="account-entry__trigger"
          type="button"
          :aria-expanded="open"
          aria-haspopup="menu"
          @click="toggle"
        >
          <span class="account-entry__avatar" aria-hidden="true">{{ initial }}</span>
          <span class="account-entry__name">{{ displayName }}</span>
          <span aria-hidden="true">{{ open ? '⌃' : '⌄' }}</span>
        </button>
        <div v-if="open" class="account-entry__menu" role="menu">
          <!--
            下拉菜单**恰好三项**，按「去后台 / 看自己 / 离开」三种去向分组，不重复顶栏导航：
              1) 进入后台 —— 唯一一个把人从账号树前台送进控制台的入口，目标是角色感知的
                 （见 consolePath：超管去仪表盘，其余角色去控制台首页），因此谁点都不会被弹回。
                 前台不再另设「控制台」按钮，同一个目的地只在菜单里出现一次。
              2) 查看个人资料 —— 前台自己的账户页（/useradmin/center）。
                 它和「进入后台」并列而不是并成一个，是因为看资料是高频动作，
                 不该逼人先进控制台再从侧栏找回来；两条路各自最短。
                 （非超管角色的第 1 项会落到同一个 /center，这是有意的：他的后台首页就是这里，
                  与其给一个会被弹回的仪表盘入口，不如让第 1 项如实指向他真正能到的首页。）
              3) 退出登录 —— 有破坏性且不可撤销，固定放最后、用危险色，让人不会手滑点到。
            内容导航（博客 / 教程 / 作品）刻意不进这个菜单：它们已经在顶栏，
            再列一遍等于把账号菜单变成第二个导航；这个菜单只回答「我是谁、能去哪」。
          -->
          <RouterLink class="account-entry__item" role="menuitem" :to="consolePath">
            进入后台
          </RouterLink>
          <!--
            学习记录是**前台页面**（/useradmin/learning，在公开外壳里渲染，不在控制台侧栏），
            所以它是这个菜单里唯一一份「与后台无关、但我自己的数据」的入口。
            放在「进入后台」之后，是因为它承接的是「我要继续学」这个高频动作。
          -->
          <RouterLink class="account-entry__item" role="menuitem" :to="accountPath('/learning')">
            学习记录
          </RouterLink>
          <RouterLink class="account-entry__item" role="menuitem" :to="accountPath('/center')">
            查看个人资料
          </RouterLink>
          <p v-if="failure" class="account-entry__error" role="alert">{{ failure }}</p>
          <button class="account-entry__item is-danger" type="button" role="menuitem" :disabled="busy" @click="logout">
            {{ busy ? '退出中…' : '退出登录' }}
          </button>
        </div>
      </div>
    </template>

    <template v-else>
      <!-- 未登录只有一个入口：注册移到登录页底部（见本文件顶部说明） -->
      <RouterLink class="account-entry__link is-primary" :to="accountPath('/login')">登录</RouterLink>
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
  gap: 8px;
  min-height: 36px;
  padding: 0 12px 0 6px;
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

/* 首字母圆标：与「我的账户」页的头像同一视觉口径，只是尺寸按顶栏高度缩小 */
.account-entry__avatar {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 999px;
  color: var(--on-primary);
  background: var(--primary);
  font-size: 12px;
  font-weight: 700;
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
