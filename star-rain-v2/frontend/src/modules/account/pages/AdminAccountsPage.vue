<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import * as api from '../api/accountApi'
import { errorMessage } from '../api/http'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { roleLabel, statusLabel, dateLabel } from '../support/display'

const auth = useAuthStore()
const filters = reactive({ keyword: '', status: '' })
const page = ref(1)
const data = ref({ items: [], total: 0, page: 1, pageSize: 20 })
const loading = ref(false)
const error = ref('')
const notice = ref('')
const selected = ref(null)
const invitationUrl = ref('')
const invitationExpiresAt = ref('')
const actionBusy = ref(false)
const confirmDialog = ref(null)
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await api.listAccounts({ ...filters, page: page.value, pageSize: 20 })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function search() { page.value = 1; load() }
function changePage(next) { page.value = next; load() }
function choose(account) {
  selected.value = account
  invitationUrl.value = ''
  invitationExpiresAt.value = ''
  notice.value = ''
}

async function run(action, success) {
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

async function toggleStatus(account) {
  const next = account.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (!await confirmDialog.value.ask(`确定${next === 'DISABLED' ? '停用' : '启用'}账户 ${account.username} 吗？`)) return
  run(() => api.changeAccountStatus(account.id, next), '账户状态已更新。')
}

function canInvite(account) {
  return account.status === 'ACTIVE' && account.roles.includes('USER')
    && !account.roles.includes('ADMIN') && !account.roles.includes('SUPER_ADMIN')
}

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

async function removeAdministrator() {
  if (!selected.value || !await confirmDialog.value.ask(`取消 ${selected.value.username} 的管理员身份？其现有会话将失效，账户保留为普通用户。`)) return
  run(() => api.revokeAdministrator(selected.value.id), '管理员身份已取消。')
}

onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ACCOUNT MANAGEMENT</p><h1>账户管理</h1><p>查询账户、管理状态与角色，并发出管理员邀请。</p><p>邀请流程：用户先注册 → 点击用户行中的“邀请管理员” → 用户登录“我的账户”查看待处理邀请 → 接受后重新登录。</p></div>
    <p><RouterLink to="/admin/invitations">查看邀请记录、重发或撤销邀请 →</RouterLink></p>
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
