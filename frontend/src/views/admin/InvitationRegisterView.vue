<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { fetchInvitationStatus, registerByInvitation, sendInvitationCode, type InvitationStatus } from '@/api/account'

const route = useRoute(); const router = useRouter()
const token = String(route.params.token)
const status = ref<InvitationStatus | null>(null)
const loading = ref(true); const cooldown = ref(0); const sending = ref(false)
const code = ref(''); const password = ref(''); const confirmPassword = ref('')
const submitting = ref(false)
const canSubmit = computed(() => code.value.length === 6 && password.value.length >= 10 && password.value === confirmPassword.value)
const unavailable = computed(() => status.value && (status.value.status === 'REVOKED' || status.value.status === 'ACCEPTED' || status.value.status === 'EXPIRED'))

async function load() {
  loading.value = true
  try { status.value = await fetchInvitationStatus(token) } catch { status.value = { email: '', status: 'EXPIRED', expiresAt: '' } }
  finally { loading.value = false }
}
async function send() {
  sending.value = true
  try { await sendInvitationCode(token); ElMessage.success('验证码已发送。'); cooldown.value = 60; const t = setInterval(() => { cooldown.value--; if (cooldown.value <= 0) clearInterval(t) }, 1000) }
  catch { ElMessage.error('发送失败。') } finally { sending.value = false }
}
async function register() {
  if (!canSubmit.value) { ElMessage.warning('请填写验证码并保证两次密码一致。'); return }
  submitting.value = true
  try { await registerByInvitation(token, '', code.value, password.value); ElMessage.success('注册成功，请登录。'); await router.push({ name: 'admin-login' }) }
  catch (e) { ElMessage.error((e as { response?: { data?: { detail?: string } } }).response?.data?.detail ?? '注册失败。') }
  finally { submitting.value = false }
}
onMounted(load)
</script>

<template>
  <section v-loading="loading" class="invite-page">
    <div class="invite-card">
      <h1 class="invite-card__title">接受管理员邀请</h1>
      <p v-if="unavailable" class="invite-card__unavailable">该邀请已{{ status?.status === 'REVOKED' ? '撤销' : status?.status === 'EXPIRED' ? '过期' : '使用' }}。</p>
      <template v-else>
        <p class="invite-card__email">受邀邮箱：<b>{{ status?.email || '—' }}</b></p>
        <el-button :disabled="cooldown > 0" :loading="sending" @click="send">{{ cooldown > 0 ? `重新发送（${cooldown}s）` : '发送验证码' }}</el-button>
        <el-input v-model="code" placeholder="6 位验证码" maxlength="6" />
        <el-input v-model="password" type="password" placeholder="设置密码（≥10 位）" show-password />
        <el-input v-model="confirmPassword" type="password" placeholder="确认密码" show-password @keyup.enter="register" />
        <el-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="register">完成注册</el-button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.invite-page{min-height:100vh;display:grid;place-items:center;padding:24px}
.invite-card{width:min(100%,380px);display:flex;flex-direction:column;gap:14px;padding:30px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}
.invite-card__title{font-size:24px;margin:0}.invite-card__email{font-size:14px;margin:0}.invite-card__unavailable{color:var(--danger);margin:0;font-size:14px}
</style>
