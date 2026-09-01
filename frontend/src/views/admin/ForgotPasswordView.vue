<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import { confirmPasswordReset, sendPasswordResetCode } from '@/api/account'

const router = useRouter()
const email = ref(''); const code = ref(''); const newPassword = ref(''); const confirmPassword = ref('')
const cooldown = ref(0); const sending = ref(false); const submitting = ref(false)
const step = ref<'email' | 'reset'>('email')
const canReset = computed(() => code.value.length === 6 && newPassword.value.length >= 10 && newPassword.value === confirmPassword.value)

async function send() {
  sending.value = true
  try { await sendPasswordResetCode(email.value); ElMessage.success('已发送重置验证码。'); step.value = 'reset'; cooldown.value = 60; const t = setInterval(() => { cooldown.value--; if (cooldown.value <= 0) clearInterval(t) }, 1000) }
  catch { ElMessage.error('发送失败（若邮箱未注册也统一提示）。') } finally { sending.value = false }
}
async function reset() {
  if (!canReset.value) { ElMessage.warning('请填写验证码并保证两次密码一致。'); return }
  submitting.value = true
  try { await confirmPasswordReset(email.value, code.value, newPassword.value); ElMessage.success('重置成功，请用新密码登录。'); await router.push({ name: 'admin-login' }) }
  catch (e) { ElMessage.error((e as { response?: { data?: { detail?: string } } }).response?.data?.detail ?? '重置失败。') }
  finally { submitting.value = false }
}
</script>

<template>
  <section class="forgot-page">
    <div class="forgot-card">
      <h1 class="forgot-card__title">找回密码</h1>
      <template v-if="step === 'email'">
        <el-input v-model="email" placeholder="管理员邮箱" @keyup.enter="send" />
        <el-button type="primary" :loading="sending" @click="send">发送重置验证码</el-button>
      </template>
      <template v-else>
        <p class="forgot-card__email">{{ email }}</p>
        <el-button :disabled="cooldown > 0" :loading="sending" @click="send">{{ cooldown > 0 ? `重新发送（${cooldown}s）` : '重新发送' }}</el-button>
        <el-input v-model="code" placeholder="6 位验证码" maxlength="6" />
        <el-input v-model="newPassword" type="password" placeholder="新密码（≥10 位）" show-password />
        <el-input v-model="confirmPassword" type="password" placeholder="确认新密码" show-password @keyup.enter="reset" />
        <el-button type="primary" :disabled="!canReset" :loading="submitting" @click="reset">确认重置</el-button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.forgot-page{min-height:100vh;display:grid;place-items:center;padding:24px}
.forgot-card{width:min(100%,380px);display:flex;flex-direction:column;gap:14px;padding:30px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}
.forgot-card__title{font-size:24px;margin:0}.forgot-card__email{font-size:14px;margin:0}
</style>
