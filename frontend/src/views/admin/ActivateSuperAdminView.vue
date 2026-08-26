<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { confirmActivation, fetchActivationStatus, sendActivationCode, type ActivationStatus } from '@/api/account'

const router = useRouter()
const status = ref<ActivationStatus | null>(null)
const loading = ref(true); const sending = ref(false)
const cooldown = ref(0)
const code = ref(''); const password = ref(''); const confirmPassword = ref('')
const submitting = ref(false)

const canSubmit = computed(() => code.value.length === 6 && password.value.length >= 10
  && password.value === confirmPassword.value)

async function load() {
  loading.value = true
  try { status.value = await fetchActivationStatus() } catch { status.value = { configured: false, activated: false, emailMasked: '' } }
  finally { loading.value = false }
}
async function send() {
  sending.value = true
  try { await sendActivationCode(); ElMessage.success('验证码已发送。'); cooldown.value = 60; const t = setInterval(() => { cooldown.value--; if (cooldown.value <= 0) clearInterval(t) }, 1000) }
  catch (e) { ElMessage.error((e as { response?: { data?: { detail?: string } } }).response?.data?.detail ?? '发送失败，请检查本机私密邮箱配置。') }
  finally { sending.value = false }
}
async function activate() {
  if (!canSubmit.value) { ElMessage.warning('请填写 6 位验证码，并保证两次密码一致且≥10位。'); return }
  submitting.value = true
  try { await confirmActivation(code.value, password.value); ElMessage.success('激活成功，请登录。'); await router.push({ name: 'admin-login' }) }
  catch (e) { ElMessage.error((e as { response?: { data?: { detail?: string } } }).response?.data?.detail ?? '激活失败。') }
  finally { submitting.value = false }
}
onMounted(load)
</script>

<template>
  <section v-loading="loading" class="activate-page">
    <div class="activate-card">
      <h1 class="activate-card__title">激活超级管理员</h1>
      <p class="activate-card__hint">服务器仅预设了唯一超级管理员邮箱，需通过真实邮箱验证码设置首次密码。</p>
      <p class="activate-card__email">固定邮箱：<b>{{ status?.emailMasked || '未配置' }}</b></p>
      <el-alert v-if="status?.activated" type="success" :closable="false" show-icon title="超级管理员已激活，初始化入口永久关闭。"/>
      <template v-else>
        <el-button v-if="status?.configured" :disabled="cooldown > 0" :loading="sending" @click="send">
          {{ cooldown > 0 ? `重新发送（${cooldown}s）` : '发送验证码' }}
        </el-button>
        <el-input v-model="code" placeholder="6 位验证码" maxlength="6" />
        <el-input v-model="password" type="password" placeholder="设置密码（≥10 位）" show-password />
        <el-input v-model="confirmPassword" type="password" placeholder="确认密码" show-password @keyup.enter="activate" />
        <el-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="activate">激活</el-button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.activate-page{min-height:100vh;display:grid;place-items:center;padding:24px}
.activate-card{width:min(100%,380px);display:flex;flex-direction:column;gap:14px;padding:30px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}
.activate-card__title{font-size:24px;margin:0}.activate-card__hint{font-size:13px;color:var(--text-secondary);line-height:1.7;margin:0}
.activate-card__email{font-size:14px;color:var(--text-primary);margin:0}
</style>
