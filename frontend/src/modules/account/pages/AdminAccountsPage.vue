<script setup>
/**
 * AdminAccountsPage.vue：账号模块的页面组件。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/paginationScroll、vue、vue-router、../stores/authStore、../api/accountApi、../api/http。
 * 主要响应式状态：auth、filters、page、data、loading、error、notice、selected。
 * 阅读入口：load（加载当前操作）；search（搜索的处理函数）；changePage（change分页的处理函数）；choose（choose的处理函数）；run（运行当前操作）。
 */


import { useLatestAccountQuery } from '../support/useLatestAccountQuery'
import { usePaginationScroll } from '../../../shared/composables/paginationScroll'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import * as api from '../api/accountApi'
import { errorMessage } from '../api/http'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { roleLabel, statusLabel, dateLabel } from '../support/display'

/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ keyword: '', status: '' })
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({ items: [], total: 0, page: 1, pageSize: 20 })
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 选中；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selected = ref(null)
/**
 * 邀请地址；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const invitationUrl = ref('')
/**
 * 邀请到期时间；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const invitationExpiresAt = ref('')
/**
 * 动作Busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const actionBusy = ref(false)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))

/**
 * 加载当前操作。
 * 主要协作：api.listAccounts、errorMessage。
 * 更新的响应式状态包括 loading、error、data；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
const latestQuery = useLatestAccountQuery({ data, error, loading })
async function load() {
  return latestQuery(() => api.listAccounts({ ...filters, page: page.value, pageSize: 20 }))
}

/**
 * 搜索的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 */
function search() { page.value = 1; load() }
/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { page.value = next; load() }
/**
 * choose的处理函数。
 * 更新的响应式状态包括 selected、invitationUrl、invitationExpiresAt、notice；渲染由 Vue 的依赖跟踪触发。
 * @param account 账号；默认值、解构字段与可选参数见函数签名。
 */
function choose(account) {
  selected.value = account
  invitationUrl.value = ''
  invitationExpiresAt.value = ''
  notice.value = ''
}

/**
 * 运行当前操作。
 * 主要协作：action、load、data.value.items.find、errorMessage。
 * 更新的响应式状态包括 actionBusy、error、notice、selected；渲染由 Vue 的依赖跟踪触发。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @param success success；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function run(action, success) {
  if (actionBusy.value) return
  actionBusy.value = true
  error.value = ''
  notice.value = ''
  try {
    await action()
    notice.value = success
    await load()
    if (selected.value) {
      selected.value = data.value.items.find((item) => item.id === selected.value.id) || null
    }
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    actionBusy.value = false
  }
}

/**
 * 切换状态。
 * 主要协作：confirmDialog.value.ask、run、api.changeAccountStatus。
 * @param account 账号；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleStatus(account) {
  if (actionBusy.value) return
  const next = account.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (!await confirmDialog.value.ask(`确定${next === 'DISABLED' ? '停用' : '启用'}账户 ${account.username} 吗？`)) return
  run(() => api.changeAccountStatus(account.id, next), '账户状态已更新。')
}

/**
 * 判断能否Invite。
 * 主要协作：account.roles.includes。
 * @param account 账号；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function canInvite(account) {
  return account.status === 'ACTIVE' && account.roles.includes('USER')
    && !account.roles.includes('ADMIN') && !account.roles.includes('SUPER_ADMIN')
}

/**
 * invite的处理函数。
 * 主要协作：canInvite、confirmDialog.value.ask、choose、api.createInvitation、errorMessage。
 * 更新的响应式状态包括 actionBusy、error、invitationUrl、invitationExpiresAt、notice；渲染由 Vue 的依赖跟踪触发。
 * @param account = selected.value 账号选中值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function invite(account = selected.value) {
  if (actionBusy.value || !account || !canInvite(account)) return
  if (!await confirmDialog.value.ask(`向 ${account.username}（${account.email}）发送管理员邀请？用户接受邀请后才会获得管理员角色。`)) return
  choose(account)
  actionBusy.value = true
  error.value = ''
  invitationUrl.value = ''
  try {
    const invitation = await api.createInvitation(account.id)
    invitationUrl.value = invitation.invitationUrl
    invitationExpiresAt.value = invitation.expiresAt
    notice.value = '邀请已创建，邮件已提交发送。受邀用户也可登录“我的账户”，直接在待处理邀请中接受。'
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    actionBusy.value = false
  }
}

/**
 * 移除Administrator。
 * 主要协作：confirmDialog.value.ask、run、api.revokeAdministrator。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeAdministrator() {
  if (actionBusy.value) return
  if (!selected.value || !await confirmDialog.value.ask(`取消 ${selected.value.username} 的管理员身份？其现有会话将失效，账户保留为普通用户。`)) return
  run(() => api.revokeAdministrator(selected.value.id), '管理员身份已取消。')
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
usePaginationScroll(() => page.value, () => loading.value)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ACCOUNT MANAGEMENT</p><h1>账户管理</h1><p>查询账户、管理状态与角色，并发出管理员邀请。</p><p>邀请流程：用户先注册 → 点击用户行中的“邀请管理员” → 用户登录“我的账户”查看待处理邀请 → 接受后重新登录。</p></div>
    <p><RouterLink to="/useradmin/invitations">查看邀请记录、重发或撤销邀请 →</RouterLink></p>
    <section class="surface-card">
      <form class="toolbar" @submit.prevent="search">
        <label>关键词<input v-model.trim="filters.keyword" maxlength="100" placeholder="用户名或邮箱" /></label>
        <label>状态<select v-model="filters.status"><option value="">全部</option><option value="ACTIVE">正常</option><option value="DISABLED">已停用</option></select></label>
        <button class="primary-button" type="submit">查询</button>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <p v-if="loading" class="loading" role="status">正在加载账户…</p>
      <div class="table-scroll"><table><thead><tr><th>账户</th><th>邮箱</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="account in data.items" :key="account.id"><td><strong>{{ account.displayName }}</strong><small>@{{ account.username }} · #{{ account.id }}</small></td><td>{{ account.email }}</td><td>{{ account.roles?.map(roleLabel).join('、') }}</td><td><span :class="['status-chip', account.status === 'DISABLED' && 'status-chip--danger']">{{ statusLabel(account.status) }}</span></td><td>{{ dateLabel(account.createdAt) }}</td><td class="table-actions"><button class="link-button" type="button" :disabled="actionBusy" @click="choose(account)">查看</button><button v-if="auth.hasPermission('account:invite-admin') && canInvite(account)" class="link-button" type="button" :disabled="actionBusy" @click="invite(account)">邀请管理员</button><button v-if="auth.hasPermission('account:disable')" class="link-button" type="button" :disabled="actionBusy" @click="toggleStatus(account)">{{ account.status === 'ACTIVE' ? '停用' : '启用' }}</button></td></tr>
        <tr v-if="!loading && !data.items.length"><td colspan="6" class="empty-state">没有符合条件的账户。</td></tr></tbody></table></div>
      <div class="pagination"><span>共 {{ data.total }} 条</span><div><button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button></div></div>
    </section>
    <section v-if="selected" class="surface-card detail-panel">
      <div class="section-heading"><div><p class="eyebrow">ACCOUNT DETAILS</p><h2>{{ selected.displayName }} <small>@{{ selected.username }}</small></h2></div><button class="text-button" type="button" @click="selected = null">关闭</button></div>
      <p class="muted">账户 ID：{{ selected.id }} · {{ selected.email }}</p>
      <div class="content-grid">
        <div><h3>账户身份</h3><p>{{ selected.roles.map(roleLabel).join('、') }}</p><p class="muted">角色能力由系统预置。管理员通过邀请获得身份，超级管理员保持唯一。</p><button v-if="auth.canManage('account:revoke-admin') && selected.roles.includes('ADMIN') && !selected.roles.includes('SUPER_ADMIN')" class="secondary-button" type="button" :disabled="actionBusy" @click="removeAdministrator">取消管理员身份</button></div>
        <div v-if="auth.hasPermission('account:invite-admin')"><h3>管理员邀请</h3><p class="muted">向已注册的普通用户发出邀请，同时提交邮件通知。受邀用户可在“我的账户”的待处理邀请中接受；接受后需重新登录。</p><button class="secondary-button" type="button" :disabled="actionBusy || !canInvite(selected)" @click="invite(selected)">发送管理员邀请</button><template v-if="invitationUrl"><label class="invitation-link">备用邀请链接（点击输入框可全选复制）<input :value="invitationUrl" readonly @focus="$event.target.select()" /></label><p class="muted">到期时间：{{ dateLabel(invitationExpiresAt) }}。链接只能由该受邀账户使用一次。</p></template></div>
      </div>
    </section>
  </main>
  <ConfirmDialog ref="confirmDialog" />
</template>
