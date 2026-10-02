<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { myInvitations, acceptMyInvitation } from '../api/accountApi'
import { clearCsrf, errorMessage } from '../api/http'
import { dateLabel } from '../support/display'
import ConfirmDialog from './ConfirmDialog.vue'

const auth = useAuthStore()
const router = useRouter()
const items = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const confirm = ref(null)
async function load() {
  loading.value = true; error.value = ''
  try { items.value = await myInvitations() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}
async function accept(item) {
  if (busy.value || !await confirm.value.ask('接受这条管理员邀请？接受后将获得管理员身份，并需要重新登录。')) return
  busy.value = true; error.value = ''
  try {
    await acceptMyInvitation(item.id)
    auth.currentUser = null
    auth.initialized = true
    clearCsrf()
    await router.replace({ path: '/login', query: { invited: '1' } })
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <section class="surface-card detail-panel">
    <div class="section-heading"><div><p class="eyebrow">PENDING INVITATIONS</p><h2>待处理邀请</h2></div><button class="secondary-button" type="button" :disabled="loading || busy" @click="load">刷新邀请</button></div>
    <p v-if="loading" class="loading" role="status">正在查询邀请…</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="!loading && !error && !items.length" class="muted">当前账户没有待处理的管理员邀请。</p>
    <article v-for="item in items" :key="item.id" class="invitation-card">
      <h3>管理员邀请 #{{ item.id }}</h3><p>超级管理员邀请当前账户成为管理员。你可以在这里接受，无需等待邮件。</p><p class="muted">受邀邮箱：{{ item.targetEmail }} · 有效期至 {{ dateLabel(item.expiresAt) }}</p>
      <button class="primary-button" type="button" :disabled="busy" @click="accept(item)">{{ busy ? '处理中…' : '接受管理员邀请' }}</button>
    </article>
  </section>
  <ConfirmDialog ref="confirm" />
</template>
