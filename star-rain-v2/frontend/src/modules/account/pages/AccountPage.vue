<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { changePassword, updateAccount, sendAccountEmailCode, confirmAccountEmail } from '../api/accountApi'
import { useEmailCode } from '../support/useEmailCode'
import { clearCsrf, errorMessage } from '../api/http'
import { roleLabel, statusLabel } from '../support/display'
import { accountPath } from '../../../shared/viewMode'
import PendingInvitations from '../components/PendingInvitations.vue'

/*
 * 我的账户（用户站个人中心）。
 *
 * 本轮只做视觉与信息架构调整：功能与请求一字未改，仍是
 * 改显示名 / 改密码 / 验证账户邮箱 / 待处理邀请四件事。
 * 版式对齐 V1 个人中心的做法：顶部身份卡 + 页内小节导航 + 分区卡片，
 * 让「我是谁、我的邮箱验证了没有、还欠什么事」在第一屏就能看清。
 */
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

// 头像用显示名首字符（拉丁字母统一大写），登录名兜底；不额外请求任何资源
const initial = computed(() => {
  const source = (auth.currentUser?.displayName || auth.currentUser?.username || '我').trim() || '我'
  const first = source.slice(0, 1)
  return /[a-z]/.test(first) ? first.toUpperCase() : first
})
const rolesText = computed(() => (auth.currentUser?.roles || []).map(roleLabel).join('、') || '—')
const emailVerified = computed(() => Boolean(auth.currentUser?.emailVerified))

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
    await router.replace({ path: '/useradmin/login', query: { passwordChanged: '1' } })
  } catch (error) {
    passwordError.value = errorMessage(error)
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="account-page">
    <header class="account-hero">
      <p class="eyebrow">MY ACCOUNT</p>
      <h1>我的账户</h1>
      <p>管理公开资料、登录密码与账户邮箱。</p>
    </header>

    <section class="account-identity" aria-label="账户概览">
      <span class="account-avatar" aria-hidden="true">{{ initial }}</span>
      <div class="account-identity__main">
        <strong>{{ auth.currentUser?.displayName || auth.currentUser?.username }}</strong>
        <span class="account-identity__email">
          {{ auth.currentUser?.email }}
          <em :class="['account-chip', emailVerified ? 'account-chip--ok' : 'account-chip--warn']">
            {{ emailVerified ? '邮箱已验证' : '邮箱待验证' }}
          </em>
        </span>
        <span class="account-identity__meta">登录名 {{ auth.currentUser?.username }} · 角色 {{ rolesText }}</span>
      </div>
      <span class="account-chip">{{ statusLabel(auth.currentUser?.status) }}</span>
    </section>

    <nav class="account-nav" aria-label="页面小节">
      <a href="#profile">个人资料</a>
      <a href="#password">登录密码</a>
      <a href="#email">邮箱验证</a>
      <a href="#invitations">待处理邀请</a>
      <!-- 学习记录独立于控制台，是前台页面；这里给一个显式入口，不用先绕进后台侧栏 -->
      <RouterLink :to="accountPath('/learning')">学习记录 →</RouterLink>
    </nav>

    <div class="content-grid">
      <section id="profile" class="surface-card">
        <div class="section-heading"><div><p class="eyebrow">PROFILE</p><h2>个人资料</h2></div><span class="status-chip">{{ statusLabel(auth.currentUser?.status) }}</span></div>
        <dl class="detail-list">
          <div><dt>用户名</dt><dd>{{ auth.currentUser?.username }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ auth.currentUser?.email }}</dd></div>
          <div><dt>邮箱验证</dt><dd>{{ emailVerified ? '已验证' : '待验证' }}</dd></div>
          <div><dt>角色</dt><dd>{{ rolesText }}</dd></div>
        </dl>
        <form class="form-stack" @submit.prevent="saveProfile">
          <label>显示名称<input v-model="profile.displayName" maxlength="80" required autocomplete="name" /></label>
          <p v-if="profileNotice" class="notice" role="status">{{ profileNotice }}</p>
          <p v-if="profileError" class="error" role="alert">{{ profileError }}</p>
          <button class="primary-button" type="submit" :disabled="savingProfile">{{ savingProfile ? '保存中…' : '保存资料' }}</button>
        </form>
      </section>

      <section id="password" class="surface-card">
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

    <section id="email" class="surface-card detail-panel">
      <div class="section-heading"><div><p class="eyebrow">EMAIL VERIFICATION</p><h2>验证账户邮箱</h2></div></div>
      <p v-if="emailVerified" class="notice" role="status">账户邮箱已验证。</p>
      <form v-else class="form-stack" @submit.prevent="verifyEmail">
        <p class="muted">当前邮箱：{{ auth.currentUser?.email }}。之前未经过验证码校验的账户，可在这里补充验证，无需重新注册。</p>
        <label>邮箱验证码<span class="verification-field"><input v-model.trim="emailCode" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" placeholder="6 位数字" required /><button class="secondary-button" type="button" :disabled="sendingEmailCode || verifyingEmail || emailCooldown > 0" @click="requestEmailCode">{{ sendingEmailCode ? '发送中…' : emailCooldown ? `${emailCooldown} 秒后重发` : '发送验证码' }}</button></span></label>
        <button class="primary-button" type="submit" :disabled="verifyingEmail || sendingEmailCode">{{ verifyingEmail ? '验证中…' : '验证邮箱' }}</button>
      </form>
      <p v-if="emailNotice" class="notice" role="status">{{ emailNotice }}</p>
      <p v-if="emailError" class="error" role="alert">{{ emailError }}</p>
    </section>

    <div id="invitations" class="account-invitations">
      <PendingInvitations />
    </div>
  </div>
</template>

<style scoped>
.account-page {
  max-width: 1180px;
  margin-inline: auto;
  padding: 32px var(--space-6) var(--space-10);
}

.account-hero { margin-bottom: var(--space-6); }

.account-hero h1 {
  font-size: clamp(26px, 3vw, 34px);
  letter-spacing: -0.035em;
}

.account-hero > p:last-child { color: var(--text-secondary); }

.account-identity {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-5) var(--space-6);
  margin-bottom: var(--space-5);
  border: 1px solid var(--border);
  border-radius: 16px;
  background: linear-gradient(135deg, var(--bg-surface), color-mix(in srgb, var(--primary) 6%, var(--bg-surface)));
  box-shadow: 0 4px 20px rgb(27 47 39 / 0.03);
}

.account-avatar {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 18px;
  color: var(--on-primary);
  background: var(--primary);
  font-size: 22px;
  font-weight: 750;
}

.account-identity__main {
  display: grid;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.account-identity__main strong { font-size: 18px; }

.account-identity__email {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.account-identity__meta { color: var(--text-muted); font-size: 12px; }

.account-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.account-chip--ok { color: var(--success); background: color-mix(in srgb, var(--success) 12%, transparent); }
.account-chip--warn { color: var(--warning); background: color-mix(in srgb, var(--warning) 14%, transparent); }

.account-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: var(--space-6);
}

.account-nav a {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 12px;
}

.account-nav a:hover { border-color: var(--primary); color: var(--primary); }

.account-invitations { margin-top: var(--space-5); }

.account-page :deep(.detail-panel) { margin-top: 0; }

@media (max-width: 720px) {
  .account-page { padding: 22px 14px var(--space-9); }
  .account-identity { align-items: flex-start; flex-direction: column; gap: var(--space-3); }
}
</style>
