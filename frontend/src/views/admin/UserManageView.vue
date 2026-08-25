<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { disableAccount, enableAccount, fetchSuperAdminUsers, type AccountUser } from '@/api/account'

const users = ref<AccountUser[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    users.value = await fetchSuperAdminUsers()
  } finally {
    loading.value = false
  }
}

async function disableUser(user: AccountUser) {
  const { value } = await ElMessageBox.prompt(`禁用管理员 ${user.email}`, '禁用账号', {
    inputPlaceholder: '可填写禁用原因',
    confirmButtonText: '禁用',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await disableAccount(user.id, value)
  ElMessage.success('账号已禁用。')
  await load()
}

async function enableUser(user: AccountUser) {
  await enableAccount(user.id)
  ElMessage.success('账号已启用。')
  await load()
}

onMounted(() => void load())
</script>

<template>
  <section class="account-admin">
    <header class="account-admin__header">
      <div>
        <p>SUPER ADMIN · 用户权限</p>
        <h1>用户管理</h1>
        <span>超级管理员拥有全部权限；管理员只能协作编辑教程、博客和英语内容。</span>
      </div>
    </header>

    <div v-loading="loading" class="account-admin__list">
      <article v-for="user in users" :key="user.id" class="account-card">
        <div>
          <strong>{{ user.email }}</strong>
          <span>{{ user.role }} · {{ user.accountStatus }}</span>
          <small v-if="user.disabledReason">禁用原因：{{ user.disabledReason }}</small>
        </div>
        <footer>
          <el-button v-if="user.accountStatus === 'DISABLED'" link type="success" @click="enableUser(user)">启用</el-button>
          <el-button v-else-if="user.role !== 'SUPER_ADMIN'" link type="danger" @click="disableUser(user)">禁用</el-button>
          <span v-else class="account-card__locked">受保护</span>
        </footer>
      </article>
    </div>
  </section>
</template>

<style scoped>
.account-admin__header { margin-bottom: 22px; }
.account-admin__header p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .16em; }
.account-admin__header h1 { margin: 6px 0; font-size: 32px; line-height: 1.18; }
.account-admin__header span { color: var(--text-secondary); }
.account-admin__list { display: grid; gap: 12px; max-width: 980px; }
.account-card { display: flex; justify-content: space-between; gap: 16px; padding: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); }
.account-card strong, .account-card span, .account-card small { display: block; }
.account-card span, .account-card small { margin-top: 6px; color: var(--text-secondary); }
.account-card footer { display: flex; align-items: center; flex-shrink: 0; }
.account-card__locked { color: var(--text-muted); font-size: 13px; }
@media (max-width: 720px) { .account-card { display: block; } .account-card footer { margin-top: 12px; } }
</style>
