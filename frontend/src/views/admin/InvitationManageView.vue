<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { createInvitation, deleteInvitation, fetchInvitations, resendInvitation, revokeInvitation, type AdminInvitation } from '@/api/account'

const invitations = ref<AdminInvitation[]>([])
const email = ref('')
const loading = ref(false)
const creating = ref(false)
const latestInviteLink = ref('')

async function load() {
  loading.value = true
  try {
    invitations.value = await fetchInvitations()
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!email.value.trim()) {
    ElMessage.warning('请输入邮箱。')
    return
  }
  creating.value = true
  try {
    const created = await createInvitation(email.value)
    email.value = ''
    latestInviteLink.value = created.inviteLink ?? ''
    ElMessage.success(latestInviteLink.value ? '邀请已创建，可复制注册链接。' : '邀请已创建。')
    await load()
  } finally {
    creating.value = false
  }
}

async function resend(item: AdminInvitation) {
  const updated = await resendInvitation(item.id)
  latestInviteLink.value = updated.inviteLink ?? ''
  ElMessage.success(latestInviteLink.value ? '邀请已重发，可复制新的注册链接。' : '邀请已重发。')
  await load()
}

async function copyLatestLink() {
  if (!latestInviteLink.value) return
  await navigator.clipboard.writeText(latestInviteLink.value)
  ElMessage.success('注册链接已复制。')
}

async function revoke(item: AdminInvitation) {
  await ElMessageBox.confirm(`撤销 ${item.email} 的邀请？`, '撤销邀请', { type: 'warning' })
  await revokeInvitation(item.id)
  ElMessage.success('邀请已撤销。')
  await load()
}

async function remove(item: AdminInvitation) {
  await ElMessageBox.confirm(`删除 ${item.email} 的这条邀请记录？审计日志仍会保留。`, '删除邀请记录', { type: 'warning' })
  await deleteInvitation(item.id)
  ElMessage.success('邀请记录已删除。')
  await load()
}

async function reinvite(item: AdminInvitation) {
  const created = await createInvitation(item.email)
  latestInviteLink.value = created.inviteLink ?? ''
  ElMessage.success('已生成新的邀请并重新发送。')
  await load()
}

onMounted(() => void load())
</script>

<template>
  <section class="account-admin">
    <header class="account-admin__header">
      <div>
        <p>SUPER ADMIN · 协作者邀请</p>
        <h1>邀请管理</h1>
        <span>通过邮箱邀请管理员注册，邀请有效期 72 小时。</span>
      </div>
      <form class="invite-form" @submit.prevent="submit">
        <el-input v-model="email" placeholder="管理员邮箱" />
        <el-button type="primary" native-type="submit" :loading="creating">发送邀请</el-button>
      </form>
    </header>

    <el-alert
      v-if="latestInviteLink"
      class="invite-link-alert"
      type="success"
      :closable="false"
      show-icon
    >
      <template #title>
        邀请链接已生成。真实邮箱未配置或发送失败时，也可以复制此链接发给协作者。
      </template>
      <div class="invite-link-alert__body">
        <code>{{ latestInviteLink }}</code>
        <el-button size="small" type="primary" @click="copyLatestLink">复制链接</el-button>
      </div>
    </el-alert>

    <div v-loading="loading" class="account-admin__list">
      <article v-for="item in invitations" :key="item.id" class="account-card">
        <div>
          <strong>{{ item.email }}</strong>
          <span>{{ item.status }} · 过期于 {{ item.expiresAt }}</span>
          <small v-if="item.acceptedAt">接受时间：{{ item.acceptedAt }}</small>
        </div>
        <footer>
          <template v-if="item.status === 'PENDING'">
            <el-button link type="primary" @click="resend(item)">重发</el-button>
            <el-button link type="danger" @click="revoke(item)">撤销</el-button>
          </template>
          <template v-if="item.status === 'REVOKED' || item.status === 'EXPIRED'">
            <el-button link type="primary" @click="reinvite(item)">重新邀请</el-button>
            <el-button link type="danger" @click="remove(item)">删除记录</el-button>
          </template>
        </footer>
      </article>
      <p v-if="!loading && !invitations.length" class="empty">暂无邀请。</p>
    </div>
  </section>
</template>

<style scoped>
.account-admin__header { display: flex; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.account-admin__header p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .16em; }
.account-admin__header h1 { margin: 6px 0; font-size: 32px; line-height: 1.18; }
.account-admin__header span { color: var(--text-secondary); }
.invite-form { display: flex; align-items: center; gap: 10px; min-width: 420px; }
.invite-link-alert { max-width: 980px; margin: 0 0 18px; }
.invite-link-alert__body { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.invite-link-alert__body code { flex: 1; overflow-wrap: anywhere; padding: 8px 10px; border-radius: 8px; background: var(--bg-page); color: var(--text-primary); }
.account-admin__list { display: grid; gap: 12px; max-width: 980px; }
.account-card { display: flex; justify-content: space-between; gap: 16px; padding: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); }
.account-card strong, .account-card span, .account-card small { display: block; }
.account-card span, .account-card small { margin-top: 6px; color: var(--text-secondary); }
.account-card footer { display: flex; align-items: center; flex-shrink: 0; }
.empty { color: var(--text-secondary); }
@media (max-width: 720px) { .account-admin__header, .invite-form, .account-card { display: block; } .invite-form > * + * { margin-top: 10px; } .account-card footer { margin-top: 12px; } }
</style>
