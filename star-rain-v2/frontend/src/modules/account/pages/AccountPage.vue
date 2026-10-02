<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { changePassword, updateAccount, sendAccountEmailCode, confirmAccountEmail } from '../api/accountApi'
import { useEmailCode } from '../support/useEmailCode'
import { clearCsrf, errorMessage } from '../api/http'
import { roleLabel, statusLabel } from '../support/display'
import PendingInvitations from '../components/PendingInvitations.vue'

const auth = useAuthStore()
const router = useRouter()
const profile = reactive({ displayName: '' })
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileNotice = ref('')
const profileError = ref('')
const passwordError = ref('')
const emailCode = ref('')
const emailNotice = ref('')
const emailError = ref('')
const verifyingEmail = ref(false)
const { sending: sendingEmailCode, remaining: emailCooldown, send: sendEmailCode } = useEmailCode()

async function requestEmailCode() {
  emailError.value = ''
  emailNotice.value = ''
  try {
    if (await sendEmailCode(sendAccountEmailCode)) {
      emailNotice.value = '验证码邮件已发送至账户邮箱，10 分钟内有效。请查看收件箱及垃圾邮件。'
    }
  } catch (error) { emailError.value = errorMessage(error) }
}

async function verifyEmail() {
  if (verifyingEmail.value || sendingEmailCode.value) return
  verifyingEmail.value = true
  emailError.value = ''
  try {
    auth.currentUser = await confirmAccountEmail(emailCode.value)
    emailCode.value = ''
    emailNotice.value = '邮箱验证成功。'
  } catch (error) { emailError.value = errorMessage(error) }
  finally { verifyingEmail.value = false }
}

watch(() => auth.currentUser?.displayName, (value) => { profile.displayName = value || '' }, { immediate: true })

async function saveProfile() {
  savingProfile.value = true
  profileError.value = ''
  profileNotice.value = ''
  try {
    auth.currentUser = await updateAccount({ displayName: profile.displayName.trim() })
    profileNotice.value = '个人资料已保存。'
  } catch (error) {
    profileError.value = errorMessage(error)
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  savingPassword.value = true
  passwordError.value = ''
  try {
    await changePassword({ ...password })
    auth.currentUser = null
    auth.initialized = false
    clearCsrf()
    await router.replace({ path: '/login', query: { passwordChanged: '1' } })
  } catch (error) {
    passwordError.value = errorMessage(error)
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">MY ACCOUNT</p><h1>我的账户</h1><p>管理公开资料与登录密码。</p></div>
    <PendingInvitations />
    <div class="content-grid">
      <section class="surface-card">
        <div class="section-heading"><div><p class="eyebrow">PROFILE</p><h2>个人资料</h2></div><span class="status-chip">{{ statusLabel(auth.currentUser?.status) }}</span></div>
        <dl class="detail-list">
          <div><dt>用户名</dt><dd>{{ auth.currentUser?.username }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ auth.currentUser?.email }}</dd></div>
          <div><dt>邮箱验证</dt><dd>{{ auth.currentUser?.emailVerified ? '已验证' : '待验证' }}</dd></div>
          <div><dt>角色</dt><dd>{{ auth.currentUser?.roles?.map(roleLabel).join('、') }}</dd></div>
        </dl>
        <form class="form-stack" @submit.prevent="saveProfile">
          <label>显示名称<input v-model="profile.displayName" maxlength="80" required autocomplete="name" /></label>
          <p v-if="profileNotice" class="notice" role="status">{{ profileNotice }}</p>
          <p v-if="profileError" class="error" role="alert">{{ profileError }}</p>
          <button class="primary-button" type="submit" :disabled="savingProfile">{{ savingProfile ? '保存中…' : '保存资料' }}</button>
        </form>
      </section>
      <section class="surface-card">
        <div class="section-heading"><div><p class="eyebrow">SECURITY</p><h2>修改密码</h2></div></div>
        <p class="muted">修改成功后，当前会话会退出，请使用新密码重新登录。</p>
        <form class="form-stack" @submit.prevent="savePassword">
          <label>当前密码<input v-model="password.currentPassword" type="password" autocomplete="current-password" required /></label>
          <label>新密码<input v-model="password.newPassword" type="password" autocomplete="new-password" minlength="12" required /></label>
          <label>确认新密码<input v-model="password.confirmPassword" type="password" autocomplete="new-password" required /></label>
          <p v-if="passwordError" class="error" role="alert">{{ passwordError }}</p>
          <button class="primary-button" type="submit" :disabled="savingPassword">{{ savingPassword ? '提交中…' : '更新密码' }}</button>
        </form>
      </section>
    </div>
    <section class="surface-card detail-panel">
      <div class="section-heading"><div><p class="eyebrow">EMAIL VERIFICATION</p><h2>验证账户邮箱</h2></div></div>
      <p v-if="auth.currentUser?.emailVerified" class="notice" role="status">账户邮箱已验证。</p>
      <form v-else class="form-stack" @submit.prevent="verifyEmail">
        <p class="muted">当前邮箱：{{ auth.currentUser?.email }}。之前未经过验证码校验的账户，可在这里补充验证，无需重新注册。</p>
        <label>邮箱验证码<span class="verification-field"><input v-model.trim="emailCode" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" placeholder="6 位数字" required /><button class="secondary-button" type="button" :disabled="sendingEmailCode || verifyingEmail || emailCooldown > 0" @click="requestEmailCode">{{ sendingEmailCode ? '发送中…' : emailCooldown ? `${emailCooldown} 秒后重发` : '发送验证码' }}</button></span></label>
        <button class="primary-button" type="submit" :disabled="verifyingEmail || sendingEmailCode">{{ verifyingEmail ? '验证中…' : '验证邮箱' }}</button>
      </form>
      <p v-if="emailNotice" class="notice" role="status">{{ emailNotice }}</p>
      <p v-if="emailError" class="error" role="alert">{{ emailError }}</p>
    </section>
  </main>
</template>
