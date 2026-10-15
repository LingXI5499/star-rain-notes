<script setup>
/**
 * AccountPage.vue：账号模块的页面组件。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../stores/authStore、../api/accountApi、../support/useEmailCode、../api/http。
 * 主要响应式状态：auth、router、profile、password、savingProfile、savingPassword、profileNotice、profileError。
 * 阅读入口：requestEmailCode（请求邮箱代码的处理函数）；verifyEmail（验证邮箱）；saveProfile（保存资料）；savePassword（保存密码）。
 */


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
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 资料；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const profile = reactive({ displayName: '' })
/**
 * 密码参数；使用和保存方式由对应的密码处理流程决定；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
/**
 * saving资料；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const savingProfile = ref(false)
/**
 * saving密码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const savingPassword = ref(false)
/**
 * 资料Notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const profileNotice = ref('')
/**
 * 资料错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const profileError = ref('')
/**
 * 密码错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const passwordError = ref('')
/**
 * 邮箱代码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const emailCode = ref('')
/**
 * 邮箱Notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const emailNotice = ref('')
/**
 * 邮箱错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const emailError = ref('')
/**
 * verifying邮箱；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const verifyingEmail = ref(false)
const { sending: sendingEmailCode, remaining: emailCooldown, send: sendEmailCode } = useEmailCode()

// 头像用显示名首字符（拉丁字母统一大写），登录名兜底；不额外请求任何资源
/**
 * initial；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const initial = computed(() => {
  const source = (auth.currentUser?.displayName || auth.currentUser?.username || '我').trim() || '我'
  const first = source.slice(0, 1)
  return /[a-z]/.test(first) ? first.toUpperCase() : first
})
/**
 * 角色集合文本；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const rolesText = computed(() => (auth.currentUser?.roles || []).map(roleLabel).join('、') || '—')
/**
 * 邮箱Verified；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const emailVerified = computed(() => Boolean(auth.currentUser?.emailVerified))

/**
 * 请求邮箱代码的处理函数。
 * 主要协作：sendEmailCode、errorMessage。
 * 更新的响应式状态包括 emailError、emailNotice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function requestEmailCode() {
  emailError.value = ''
  emailNotice.value = ''
  try {
    if (await sendEmailCode(sendAccountEmailCode)) {
      emailNotice.value = '验证码请求已受理，验证码 10 分钟内有效。请稍后查看账户邮箱及垃圾邮件。'
    }
  } catch (error) { emailError.value = errorMessage(error) }
}

/**
 * 验证邮箱。
 * 主要协作：confirmAccountEmail、errorMessage。
 * 更新的响应式状态包括 verifyingEmail、emailError、emailCode、emailNotice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function verifyEmail() {
  if (verifyingEmail.value || sendingEmailCode.value) return
  verifyingEmail.value = true
  emailError.value = ''
  try {
    if (!await auth.updateCurrent(() => confirmAccountEmail(emailCode.value))) return
    emailCode.value = ''
    emailNotice.value = '邮箱验证成功。'
  } catch (error) { emailError.value = errorMessage(error) }
  finally { verifyingEmail.value = false }
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => auth.currentUser?.displayName, (value) => { profile.displayName = value || '' }, { immediate: true })

/**
 * 保存资料。
 * 主要协作：updateAccount、profile.displayName.trim、errorMessage。
 * 更新的响应式状态包括 savingProfile、profileError、profileNotice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveProfile() {
  if (savingProfile.value) return
  savingProfile.value = true
  profileError.value = ''
  profileNotice.value = ''
  try {
    if (!await auth.updateCurrent(() => updateAccount({ displayName: profile.displayName.trim() }))) return
    profileNotice.value = '个人资料已保存。'
  } catch (error) {
    profileError.value = errorMessage(error)
  } finally {
    savingProfile.value = false
  }
}

/**
 * 保存密码。
 * 主要协作：changePassword、clearCsrf、router.replace、errorMessage。
 * 更新的响应式状态包括 savingPassword、passwordError；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function savePassword() {
  if (savingPassword.value) return
  const identity = auth.identityVersion
  savingPassword.value = true
  passwordError.value = ''
  try {
    await changePassword({ ...password })
    if (identity !== auth.identityVersion) return
    auth.expireSession()
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.account-page。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-page {
  max-width: 1180px;
  margin-inline: auto;
  padding: 32px var(--space-6) var(--space-10);
}

/**
 * 样式范围：.account-hero。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-hero { margin-bottom: var(--space-6); }

/**
 * 样式范围：.account-hero h1。
 */
.account-hero h1 {
  font-size: clamp(26px, 3vw, 34px);
  letter-spacing: -0.035em;
}

/**
 * 样式范围：.account-hero > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-hero > p:last-child { color: var(--text-secondary); }

/**
 * 样式范围：.account-identity。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.account-avatar。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.account-identity__main。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
 */
.account-identity__main {
  display: grid;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

/**
 * 样式范围：.account-identity__main strong。
 */
.account-identity__main strong { font-size: 18px; }

/**
 * 样式范围：.account-identity__email。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-identity__email {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  overflow-wrap: anywhere;
}

/**
 * 样式范围：.account-identity__meta。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-identity__meta { color: var(--text-muted); font-size: 12px; }

/**
 * 样式范围：.account-chip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.account-chip--ok。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-chip--ok { color: var(--success); background: color-mix(in srgb, var(--success) 12%, transparent); }
/**
 * 样式范围：.account-chip--warn。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-chip--warn { color: var(--warning); background: color-mix(in srgb, var(--warning) 14%, transparent); }

/**
 * 样式范围：.account-nav。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: var(--space-6);
}

/**
 * 样式范围：.account-nav a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-nav a {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 12px;
}

/**
 * 样式范围：.account-nav a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-nav a:hover { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.account-invitations。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.account-invitations { margin-top: var(--space-5); }

/**
 * 样式范围：.account-page :deep(.detail-panel)。
 */
.account-page :deep(.detail-panel) { margin-top: 0; }

/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.account-page。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .account-page { padding: 22px 14px var(--space-9); }
  /**
   * 样式范围：.account-identity。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .account-identity { align-items: flex-start; flex-direction: column; gap: var(--space-3); }
}
</style>
