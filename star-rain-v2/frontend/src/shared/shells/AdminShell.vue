<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../modules/account/stores/authStore'
import { errorMessage } from '../../modules/account/api/http'
import * as accountApi from '../../modules/account/api/accountApi'
import { accountPath } from '../viewMode'

/*
 * 控制台外壳（唯一带侧栏的外壳），由账号树的 /useradmin/center 用户中心进入。
 *
 * 侧栏是**数据驱动的五组导航**，每组一个标题、组内层级一致（一级项 + 一级子项），
 * 按角色与权限隐藏 —— 是「看不见」，不是「点了报 403」：
 *
 *   概览    仪表盘、访问统计                 仅 SUPER_ADMIN
 *   内容    教程工作台、博客管理（+标签管理/专题管理）、英语内容（+五个方向）、
 *           作品管理、媒体库、作者资料       ADMIN 及以上，逐项按权限
 *   协作    审核中心、留言管理                按 review:read / message:read-admin
 *   站点    站点设置                          仅 SUPER_ADMIN
 *   账户    我的账户、学习记录（+今日/计划/复习/历史）、
 *           账户管理、管理员邀请、审计日志    按各自权限
 *
 * 上一版有三个真实缺陷，这次一并修掉：
 *   1. 「仪表盘 / 访问统计 / 我的账户 / 学习记录」四项裸露在标题之上 ——
 *      注释里分了「通用 / 统计总览」两组，模板里却从来没渲染过那两行标题。
 *   2. 同一块地方有两个名字：折叠态写「治理」，展开态写「站点治理」，
 *      页面内部又自称「站点治理」。现在组名只有一处定义。
 *   3. 分组深度不一致：内容编辑是平铺的 8 项，站点治理却是「折叠父项 + 子项」。
 *      现在统一为「组标题 + 一级项」，只有真正存在子页面的项才挂子项。
 *      同时删掉了 `item.pending` 那两条永不成立的「建设中」死分支。
 *
 * 子项（标签管理/专题管理、英语五个方向、学习四页）过去「有路由无入口」，
 * 只能靠手输地址访问；现在挂在各自父项下，父项所在组展开时可见。
 *
 * 返回前台指向**账号树前台**（accountPath('/') = /useradmin），不是裸 '/'：
 *   裸 '/' 是公开树首页，那是给大众的匿名站，一进去右上角账号区就整个消失了
 *   （公开树按设计永远匿名）。控制台里的「返回前台」是「从后台回到我刚才浏览内容的地方」，
 *   那个地方是账号树前台，因此必须用 accountPath('/') 带上前缀，不能写裸 '/'。
 *   路径由 viewMode.js 的工具函数生成，账号树的字面量只在 viewMode.js 里存在一处。
 * 仍然用整页链接（<a>）而不是 RouterLink：离开控制台时整页重载会顺带丢掉控制台自己的
 *   运行态（侧栏折叠、导航滚动位置、已挂载的各控制台 store），比 SPA 内跳更干净。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const mobileOpen = ref(false)
const navRef = ref(null)
const contentRef = ref(null)

/*
 * 分组展开状态。默认全展开；用户折叠过的组记在 localStorage。
 * 当前路由所在的组**永远强制展开** —— 否则「我明明在这个页面，侧栏里却找不到它」。
 */
const GROUP_STORAGE_KEY = 'admin-nav-groups'
const closedGroups = ref(readClosedGroups())

function readClosedGroups() {
  try {
    const stored = JSON.parse(localStorage.getItem(GROUP_STORAGE_KEY) || '[]')
    return Array.isArray(stored) ? stored.filter((key) => typeof key === 'string') : []
  } catch {
    return []
  }
}

function isGroupOpen(key) {
  if (closedGroups.value.includes(key)) return false
  return true
}

function toggleGroup(key) {
  closedGroups.value = closedGroups.value.includes(key)
    ? closedGroups.value.filter((item) => item !== key)
    : [...closedGroups.value, key]
  localStorage.setItem(GROUP_STORAGE_KEY, JSON.stringify(closedGroups.value))
}

watch(collapsed, (value) => localStorage.setItem('admin-sidebar-collapsed', String(value)))

function rememberNavScroll() {
  if (navRef.value) sessionStorage.setItem('admin-nav-scroll', String(navRef.value.scrollTop))
}

async function revealActiveItem() {
  await nextTick()
  navRef.value?.querySelector('.is-active, .admin-shell__nav-item--active')?.scrollIntoView({ block: 'nearest' })
}

watch(() => route.fullPath, async () => {
  mobileOpen.value = false
  contentRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  await revealActiveItem()
})

onMounted(async () => {
  await nextTick()
  if (navRef.value) navRef.value.scrollTop = Number(sessionStorage.getItem('admin-nav-scroll') || 0)
  await revealActiveItem()
})

// ---------------------------------------------------------------------
// 导航（按角色与权限过滤的五组结构）
//
// 角色只决定「这一层出现不出现」，组内每一项再各自按权限判断，
// 避免出现「看得见但进不去」的入口。canManage = 既是超管又持有该权限，
// hasPermission = 持有该权限即可（ADMIN 也能拿到 media:read / review:read）。
// ---------------------------------------------------------------------

const navGroups = computed(() => {
  const groups = [
    {
      key: 'overview',
      label: '概览',
      short: '览',
      items: [
        { label: '仪表盘', short: '盘', to: accountPath('/dashboard'), visible: auth.canManage('site:dashboard-read') },
        { label: '访问统计', short: '统', to: accountPath('/analytics'), visible: auth.canManage('analytics:read') },
      ],
    },
    {
      key: 'content',
      label: '内容',
      short: '容',
      items: [
        {
          label: '教程工作台', short: '教', to: accountPath('/tutorials/manage'),
          visible: auth.hasPermission('tutorial:read-admin'),
        },
        {
          label: '博客管理', short: '博', to: accountPath('/blog/manage'),
          visible: auth.hasPermission('blog:read-admin'),
          /*
           * 标签与专题拆成两条并列入口（用户要求「三者可以进行路由」）：
           * 它们本来就是两种不同的东西 —— 标签没有顺序也没有成员，
           * 专题有成员与人工顺序、可以整体删除；合在一个入口里点进去再分栏，等于把导航藏了一半。
           */
          children: [
            { label: '标签管理', to: accountPath('/blog/taxonomy'), visible: auth.hasPermission('blog:taxonomy-manage') },
            { label: '专题管理', to: accountPath('/blog/topics'), visible: auth.hasPermission('blog:taxonomy-manage') },
          ],
        },
        {
          label: '英语内容', short: '英', to: accountPath('/english/manage'),
          visible: auth.hasPermission('english:content-read-admin'),
          children: [
            { label: '词汇', to: accountPath('/english/manage/vocabulary'), visible: auth.hasPermission('english:content-read-admin') },
            { label: '语法', to: accountPath('/english/manage/grammar'), visible: auth.hasPermission('english:content-read-admin') },
            { label: '阅读', to: accountPath('/english/manage/reading'), visible: auth.hasPermission('english:content-read-admin') },
            { label: '听力', to: accountPath('/english/manage/listening'), visible: auth.hasPermission('english:content-read-admin') },
            { label: '写作', to: accountPath('/english/manage/writing'), visible: auth.hasPermission('english:content-read-admin') },
          ],
        },
        {
          label: '作品管理', short: '品', to: accountPath('/portfolio/manage'),
          visible: auth.canManage('portfolio:read-admin'),
        },
        { label: '媒体库', short: '媒', to: accountPath('/media'), visible: auth.hasPermission('media:read') },
        {
          label: '作者资料', short: '介', to: accountPath('/profile/manage'),
          visible: auth.canManage('profile:read-admin'),
        },
      ],
    },
    {
      key: 'collaboration',
      label: '协作',
      short: '协',
      items: [
        { label: '审核中心', short: '审', to: accountPath('/reviews'), visible: auth.hasPermission('review:read') },
        {
          label: '留言管理', short: '言', to: accountPath('/messages/manage'),
          visible: auth.canManage('message:read-admin'),
        },
      ],
    },
    {
      key: 'site',
      label: '站点',
      short: '站',
      items: [
        { label: '站点设置', short: '设', to: accountPath('/site/settings'), visible: auth.canManage('site:config-manage') },
      ],
    },
    {
      key: 'account',
      label: '账户',
      short: '户',
      items: [
        { label: '我的账户', short: '我', to: accountPath('/center'), visible: true },
        { label: '账户管理', short: '用', to: accountPath('/accounts'), visible: auth.canManage('account:read') },
        { label: '管理员邀请', short: '邀', to: accountPath('/invitations'), visible: auth.canManage('account:invite-admin') },
        { label: '审计日志', short: '计', to: accountPath('/audits'), visible: auth.canManage('account:audit-read') },
      ],
    },
  ]

  return groups
    .map((group) => ({
      ...group,
      items: group.items
        .filter((item) => item.visible)
        .map((item) => ({ ...item, children: (item.children || []).filter((child) => child.visible) })),
    }))
    .filter((group) => group.items.length)
})

function isActive(target) {
  return route.path === target || route.path.startsWith(`${target}/`)
}

/*
 * 父项自身的精确高亮：`/learning` 用 isActive 会把 `/learning/today` 也算进去，
 * 于是父项和子项同时高亮。父项只在自己正好是当前页（或当前页不属于任何子项）时高亮。
 */
function isItemActive(item) {
  if (!isActive(item.to)) return false
  if (!item.children?.length) return true
  return !item.children.some((child) => child.to !== item.to && route.path.startsWith(child.to))
}

// ---------------------------------------------------------------------
// 主题
//
// 主题目前只在控制台提供切换：tokens.css 已经声明了 [data-theme='dark'] 的整套变量，
// 这里只负责把选择写进 <html data-theme>，不引入额外的主题 store。
// 选择持久化在 localStorage，下次进入控制台立即生效（在 setup 里同步应用，避免闪白）。
// ---------------------------------------------------------------------

const theme = ref('light')

function applyStoredTheme() {
  const stored = localStorage.getItem('admin-theme')
  theme.value = stored === 'dark' || stored === 'light'
    ? stored
    : (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  document.documentElement.dataset.theme = theme.value
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  document.documentElement.dataset.theme = theme.value
  localStorage.setItem('admin-theme', theme.value)
}

applyStoredTheme()

// ---------------------------------------------------------------------
// 账户菜单 / 修改密码 / 退出
// ---------------------------------------------------------------------

const accountMenuOpen = ref(false)
const menuRef = ref(null)
const passwordDialog = ref(null)
const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordError = ref('')
const passwordBusy = ref(false)
const logoutBusy = ref(false)

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value
}

function closeAccountMenu(event) {
  if (accountMenuOpen.value && !menuRef.value?.contains(event.target)) accountMenuOpen.value = false
}

document.addEventListener('pointerdown', closeAccountMenu)
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeAccountMenu))
watch(() => route.fullPath, () => { accountMenuOpen.value = false })

async function logout() {
  if (logoutBusy.value) return
  logoutBusy.value = true
  try {
    await auth.logout()
  } catch {
    // 退出失败（会话已失效等）也把本地状态清掉，不要把人留在后台里
    auth.currentUser = null
  } finally {
    logoutBusy.value = false
    // 退出后回账号树前台（accountPath('/') = /useradmin），与前台账号菜单的退出落点统一。
    // 之前这里落 /useradmin/login：同在账号树内，但「我刚退出了」被送到登录页，
    // 与前台菜单的行为不一致；前台首页本来就不需要登录态，停在那里右上角自然变回「登录」。
    await router.replace(accountPath('/'))
  }
}

function closePasswordDialog() {
  passwordDialog.value?.close()
}

async function openPasswordDialog() {
  accountMenuOpen.value = false
  passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  await nextTick()
  passwordDialog.value?.showModal()
}

async function submitPassword() {
  const form = passwordForm.value
  if (!form.currentPassword || !form.newPassword) {
    passwordError.value = '请填写当前密码与新密码。'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致。'
    return
  }
  passwordBusy.value = true
  passwordError.value = ''
  try {
    await accountApi.changePassword(form)
    // 后端在修改密码后立即失效会话，这里引导重新登录
    passwordDialog.value?.close()
    auth.currentUser = null
    await router.replace({ path: accountPath('/login'), query: { redirect: accountPath('/dashboard') } })
  } catch (cause) {
    passwordError.value = errorMessage(cause)
  } finally {
    passwordBusy.value = false
  }
}
</script>

<template>
  <div class="admin-shell">
    <button
      v-if="mobileOpen"
      class="admin-shell__scrim is-open"
      type="button"
      aria-label="关闭导航"
      @click="mobileOpen = false"
    />

    <aside :class="['admin-shell__sidebar', collapsed && 'admin-shell__sidebar--collapsed', mobileOpen && 'is-mobile-open']">
      <div class="admin-shell__brand">
        <RouterLink :to="accountPath('/center')" class="admin-shell__brand-link">
          <span v-if="!collapsed">星雨笔录</span>
          <span v-else>星</span>
        </RouterLink>
      </div>

      <nav ref="navRef" class="admin-shell__nav" aria-label="控制台导航" @scroll.passive="rememberNavScroll">
        <template v-for="group in navGroups" :key="group.key">
          <!-- 组标题。折叠态退化成单字，避免把 64px 宽的侧栏撑开 -->
          <button
            v-if="!collapsed"
            type="button"
            class="admin-shell__nav-caption admin-shell__nav-caption--toggle"
            :aria-expanded="isGroupOpen(group.key)"
            @click="toggleGroup(group.key)"
          >
            <span>{{ group.label }}</span>
            <span aria-hidden="true">{{ isGroupOpen(group.key) ? '▾' : '▸' }}</span>
          </button>
          <p v-else class="admin-shell__nav-caption">{{ group.short }}</p>

          <template v-if="collapsed || isGroupOpen(group.key)">
            <template v-for="item in group.items" :key="item.label">
              <RouterLink
                :to="item.to"
                class="admin-shell__nav-item"
                :class="{ 'admin-shell__nav-item--active': isItemActive(item) }"
                :title="collapsed ? `${group.label} · ${item.label}` : undefined"
              >
                <span class="admin-shell__nav-short">{{ item.short }}</span>
                <span v-if="!collapsed" class="admin-shell__nav-label">{{ item.label }}</span>
              </RouterLink>

              <!-- 子项：仅在展开态显示，折叠态放不下（侧栏只有图标宽度） -->
              <div v-if="!collapsed && item.children.length" class="admin-shell__subnav">
                <RouterLink
                  v-for="child in item.children"
                  :key="child.label"
                  :to="child.to"
                  class="admin-shell__subnav-item"
                  :class="{ 'is-active': route.path === child.to }"
                >{{ child.label }}</RouterLink>
              </div>
            </template>
          </template>
        </template>
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
          <button class="admin-shell__mobile-menu" type="button" aria-label="打开控制台导航" @click="mobileOpen = true">☰</button>
          <span class="admin-shell__header-title-long">星雨笔录 · 管理控制台</span>
          <span class="admin-shell__header-title-short">控制台</span>
        </div>
        <div class="admin-shell__header-actions">
          <!-- 返回前台 = 回账号树前台 /useradmin（accountPath('/')），不是公开树 '/'，理由见文件头注释 -->
          <a class="admin-shell__header-action" :href="accountPath('/')" title="返回前台（账号树首页）">
            <span class="admin-shell__header-action-long">返回前台</span>
            <span class="admin-shell__header-action-short">前台</span>
          </a>
          <button
            class="admin-shell__header-action"
            type="button"
            :title="theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
            @click="toggleTheme"
          >
            {{ theme === 'dark' ? '浅色' : '深色' }}<span class="admin-shell__header-action-long">主题</span>
          </button>
          <div ref="menuRef" class="admin-shell__account">
            <button
              class="admin-shell__header-action"
              type="button"
              :aria-expanded="accountMenuOpen"
              aria-haspopup="menu"
              @click="toggleAccountMenu"
            >
              {{ auth.currentUser?.displayName || auth.currentUser?.username || '账号' }}
            </button>
            <div v-if="accountMenuOpen" class="admin-shell__account-menu" role="menu">
              <button type="button" role="menuitem" @click="openPasswordDialog">修改密码</button>
              <hr />
              <button type="button" role="menuitem" class="is-danger" :disabled="logoutBusy" @click="logout">
                {{ logoutBusy ? '退出中…' : '退出登录' }}
              </button>
            </div>
          </div>
        </div>
      </header>

      <main ref="contentRef" class="admin-shell__content">
        <slot />
      </main>
    </div>

    <dialog ref="passwordDialog" aria-labelledby="password-title" @cancel.prevent="closePasswordDialog">
      <h2 id="password-title">修改密码</h2>
      <form class="form-stack" @submit.prevent="submitPassword">
        <label>当前密码<input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" /></label>
        <label>新密码<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" /></label>
        <label>确认新密码<input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" /></label>
        <p v-if="passwordError" class="error" role="alert">{{ passwordError }}</p>
        <p class="form-hint">修改成功后当前会话立即失效，需要重新登录。</p>
        <div class="dialog-actions">
          <button type="button" @click="closePasswordDialog">取消</button>
          <button class="primary-button" type="submit" :disabled="passwordBusy">{{ passwordBusy ? '提交中…' : '确认修改' }}</button>
        </div>
      </form>
    </dialog>
  </div>
</template>
