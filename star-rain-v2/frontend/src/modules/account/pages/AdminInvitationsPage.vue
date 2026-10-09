<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as api from '../api/accountApi'
import { errorMessage } from '../api/http'
import { dateLabel } from '../support/display'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const data = ref({ items: [], total: 0, pageSize: 20 })
const page = ref(1)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const latestLink = ref('')
const confirm = ref(null)
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / 20)))
const statusLabel = (status) => ({ PENDING: '待接受', ACCEPTED: '已接受', REVOKED: '已撤销', EXPIRED: '已过期' })[status] || status

async function load() {
  loading.value = true
  error.value = ''
  try { data.value = await api.listInvitations({ page: page.value, pageSize: 20 }) }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function changePage(value) { page.value = value; await load() }

async function act(item, action) {
  if (busy.value) return
  const message = action === 'resend'
    ? `向 ${item.targetEmail} 重发邀请通知？重发成功后旧链接失效，新的邀请有效期为 48 小时。`
    : `撤销 ${item.targetEmail} 的邀请？撤销后该邀请不能再被接受。`
  if (!await confirm.value.ask(message)) return
  busy.value = true
  error.value = ''; notice.value = ''; latestLink.value = ''
  try {
    if (action === 'resend') {
      const result = await api.resendInvitation(item.id)
      latestLink.value = result.invitationUrl
      notice.value = '邀请通知已重新提交发送。若未收到邮件，受邀用户仍可在“我的账户 → 待处理邀请”中接受。'
    } else {
      await api.revokeInvitation(item.id)
      notice.value = '邀请已撤销。'
    }
    await load()
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ADMIN INVITATIONS</p><h1>管理员邀请</h1><p>查看邀请状态，重发通知或撤销待处理邀请。</p><RouterLink to="/useradmin/accounts">到“账户管理”选择普通用户并发出邀请 →</RouterLink></div>
    <section class="surface-card">
      <div class="section-heading"><h2>邀请记录</h2><button class="secondary-button" type="button" :disabled="loading || busy" @click="load">刷新</button></div>
      <p class="muted">邮件状态表示是否已提交发送，不能证明收件箱已收到。受邀用户可直接登录“我的账户”处理邀请。</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p><p v-if="error" class="error" role="alert">{{ error }}</p>
      <label v-if="latestLink" class="invitation-link">备用邀请链接（点击可全选复制）<input :value="latestLink" readonly @focus="$event.target.select()" /></label>
      <p v-if="loading" class="loading" role="status">正在加载邀请…</p>
      <div class="table-scroll"><table><thead><tr><th>邀请</th><th>受邀邮箱</th><th>状态</th><th>邮件状态</th><th>到期时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in data.items" :key="item.id"><td>#{{ item.id }}</td><td>{{ item.targetEmail }}<small>账户 #{{ item.targetAccountId }}</small></td><td>{{ statusLabel(item.status) }}</td><td>{{ item.mailSubmissionStatus === 'SUBMITTED' ? '已提交发送，送达未确认' : '未记录投递结果' }}<small>{{ dateLabel(item.lastSentAt) }}</small></td><td>{{ dateLabel(item.expiresAt) }}</td><td class="table-actions"><template v-if="['PENDING', 'EXPIRED'].includes(item.status)"><button class="link-button" type="button" :disabled="busy" @click="act(item, 'resend')">重发通知</button><button class="link-button" type="button" :disabled="busy" @click="act(item, 'revoke')">撤销邀请</button></template><span v-else>已处理</span></td></tr>
          <tr v-if="!loading && !data.items.length"><td colspan="6" class="empty-state">暂无管理员邀请。</td></tr></tbody></table></div>
      <div class="pagination"><span>共 {{ data.total }} 条</span><div><button type="button" :disabled="page <= 1 || loading || busy" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button type="button" :disabled="page >= totalPages || loading || busy" @click="changePage(page + 1)">下一页</button></div></div>
    </section>
  </main>
  <ConfirmDialog ref="confirm" />
</template>
