<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import * as api from '../api/accountApi'
import { clearCsrf, errorMessage } from '../api/http'
import { useEmailCode } from '../support/useEmailCode'
// 认证页全部落在账号树，路径一律由 viewMode 的 accountPath() 推出来，不写死 '/useradmin' 字面量
import { accountPath } from '../../../shared/viewMode'

const props = defineProps({ mode: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({
  username: '', email: '', identifier: '', password: '', confirmPassword: '',
  newPassword: '', token: '', verificationCode: '',
})
const busy = ref(false)
const error = ref('')
const notice = ref('')
const showPassword = ref(false)
const emailInput = ref(null)
const { sending: sendingCode, remaining: codeCooldown, send: sendCode } = useEmailCode()
const title = computed(() => ({
  login: '登录星雨笔录',
  register: '创建账户',
  forgot: '找回密码',
  reset: '重置密码',
  invite: '接受管理员邀请',
})[props.mode])

watch(() => props.mode, () => {
  error.value = ''
  notice.value = ''
  form.password = ''
  form.confirmPassword = ''
  form.newPassword = ''
  form.verificationCode = ''
})

watch(() => form.email, () => { form.verificationCode = ''; notice.value = '' })

async function requestRegistrationCode() {
  if (busy.value || sendingCode.value || codeCooldown.value) return
  if (!emailInput.value?.reportValidity()) return
  error.value = ''
  notice.value = ''
  try {
    const result = await sendCode(() => api.sendRegistrationCode(form.email))
    if (result) {
      notice.value = result.existingAccount
        ? '该邮箱已有账户，验证码邮件已发送。请登录后在“我的账户 → 验证账户邮箱”完成验证，无需重新注册。'
        : `验证码邮件已发送至 ${form.email}，10 分钟内有效。请查看收件箱及垃圾邮件。`
    }
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function submit() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    if (props.mode === 'login') {
      await auth.login(form.identifier, form.password)
      /*
       * 登录成功后的落点分两种，这是本轮刻意改的地方：
       *   1. 守卫送来的（URL 带 redirect，例如未登录访问 /useradmin/center）——
       *      按 redirect 回到他原本要去的那一页，不能把人丢到别处；
       *   2. 自己点「登录」进来的 —— 留在 / 回到账号树前台（/useradmin），**不进后台**。
       *      后台是管理动作，绝大多数登录只是来看内容；直接弹进控制台会让人一进来
       *      就要先找「怎么回去」。进后台改由右上角用户菜单里的「进入后台」自己决定。
       */
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      await router.replace(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : accountPath('/'))
    } else if (props.mode === 'register') {
      await api.register({
        username: form.username, email: form.email, password: form.password,
        confirmPassword: form.confirmPassword,
        verificationCode: form.verificationCode,
      })
      await router.replace({ path: accountPath('/login'), query: { registered: '1' } })
    } else if (props.mode === 'forgot') {
      await api.requestReset(form.email)
      notice.value = '如果邮箱对应有效账户，重置邮件会发送到该邮箱。'
    } else if (props.mode === 'reset') {
      const token = typeof route.query.token === 'string' ? route.query.token : form.token
      await api.confirmReset({
        token, newPassword: form.newPassword, confirmPassword: form.confirmPassword,
      })
      clearCsrf()
      await router.replace({ path: accountPath('/login'), query: { reset: '1' } })
    } else if (props.mode === 'invite') {
      const token = typeof route.query.token === 'string' ? route.query.token : form.token
      await api.acceptInvitation(token)
      auth.currentUser = null
      auth.initialized = true
      clearCsrf()
      await router.replace({ path: accountPath('/login'), query: { invited: '1' } })
    }
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}

async function switchInvitationAccount() {
  error.value = ''
  try {
    await auth.logout()
    auth.initialized = true
    await router.replace({ path: accountPath('/login'), query: { redirect: route.fullPath } })
  } catch (cause) { error.value = errorMessage(cause) }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-story" aria-label="星雨笔录介绍">
      <RouterLink :to="accountPath('/center')" class="auth-brand">
        <img src="/brand/mark.svg" alt="" width="34" height="34" />
        <span>星雨笔录</span>
      </RouterLink>
      <div class="auth-story__copy">
        <p>INKSPACE · STAR RAIN NOTES</p>
        <h1>把知识整理成<br /><em>可持续的作品。</em></h1>
        <span>在一个安静、专注的空间中记录学习、创作与成长。</span>
      </div>
      <ul>
        <li><b>01</b><span><strong>结构化创作</strong>让知识逐步沉淀</span></li>
        <li><b>02</b><span><strong>安全账户</strong>个人数据与管理权限分明</span></li>
        <li><b>03</b><span><strong>持续学习</strong>在星雨笔录留下自己的轨迹</span></li>
      </ul>
      <small>STAR RAIN NOTES · V2.0</small>
    </section>

    <section class="auth-workspace">
      <div class="auth-card">
        <header>
          <span class="auth-card__mark"><img src="/brand/mark.svg" alt="" width="28" height="28" /></span>
          <p>WELCOME</p>
          <h2>{{ title }}</h2>
          <small>使用星雨笔录账户继续。</small>
        </header>

        <p v-if="route.query.registered" class="notice" role="status">注册成功，请登录。</p>
        <p v-if="route.query.reset" class="notice" role="status">密码已重置，请重新登录。</p>
        <p v-if="route.query.passwordChanged" class="notice" role="status">密码已修改，请重新登录。</p>
        <p v-if="route.query.expired" class="notice" role="status">登录已失效，请重新登录。</p>
        <p v-if="route.query.permissionChanged" class="notice" role="status">角色权限已保存，请重新登录以刷新权限。</p>
        <p v-if="route.query.invited" class="notice" role="status">邀请已接受，请重新登录以刷新权限。</p>
        <p v-if="notice" class="notice" role="status">{{ notice }}</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>

        <form class="form-stack" @submit.prevent="submit">
          <template v-if="mode === 'login'">
            <label>用户名或邮箱<input v-model.trim="form.identifier" autocomplete="username" required /></label>
            <label>密码<span class="password-field"><input v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" required /><button type="button" :aria-pressed="showPassword" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></span></label>
            <div class="form-assist">
              <RouterLink :to="accountPath('/forgot-password')">忘记密码？</RouterLink>
            </div>
          </template>
          <template v-else-if="mode === 'register'">
            <p class="form-hint">用户名为 3–50 位字母、数字或下划线；密码至少 12 个字符，UTF-8 编码不超过 72 字节。</p>
            <label>用户名<input v-model.trim="form.username" autocomplete="username" minlength="3" maxlength="50" required /></label>
            <label>邮箱<input ref="emailInput" v-model.trim="form.email" type="email" autocomplete="email" maxlength="128" :disabled="busy || sendingCode" required /></label>
            <label>邮箱验证码<span class="verification-field"><input v-model.trim="form.verificationCode" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="6 位数字" required /><button class="secondary-button" type="button" :disabled="busy || sendingCode || codeCooldown > 0" @click="requestRegistrationCode">{{ sendingCode ? '发送中…' : codeCooldown ? `${codeCooldown} 秒后重发` : '发送验证码' }}</button></span></label>
            <p class="form-hint">先发送验证码并验证邮箱，再创建账户。</p>
            <label>密码<input v-model="form.password" type="password" autocomplete="new-password" minlength="12" required /></label>
            <label>确认密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required /></label>
          </template>
          <template v-else-if="mode === 'forgot'">
            <p class="form-hint">输入注册邮箱。如果账户有效，系统会发送一次性重置链接。</p>
            <label>注册邮箱<input v-model.trim="form.email" type="email" autocomplete="email" required /></label>
          </template>
          <template v-else-if="mode === 'reset'">
            <label v-if="!route.query.token">重置令牌<input v-model.trim="form.token" required /></label>
            <label>新密码<input v-model="form.newPassword" type="password" autocomplete="new-password" minlength="12" required /></label>
            <label>确认新密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required /></label>
          </template>
          <template v-else-if="mode === 'invite'">
            <p class="form-hint">当前登录账户：{{ auth.currentUser?.email }}。邀请只能由指定账户接受。</p>
            <button class="secondary-button" type="button" :disabled="busy" @click="switchInvitationAccount">切换到受邀账户登录</button>
            <label v-if="!route.query.token">邀请令牌<input v-model.trim="form.token" required /></label>
          </template>
          <button class="primary-button" type="submit" :disabled="busy || sendingCode">
            {{ busy ? '处理中…' : title }} <span aria-hidden="true">→</span>
          </button>
        </form>
        <p class="muted">使用账号服务前，请阅读<RouterLink :to="accountPath('/terms')">用户协议</RouterLink>与<RouterLink :to="accountPath('/privacy')">隐私政策</RouterLink>。</p>
        <!--
          注册入口按所有者口径收在登录页底部这一行：顶栏不再并列「登录 / 注册」
          （见 modules/account/components/AccountEntry.vue），但注册页本身保留，
          未注册的访客在登录页就能看到这条路。其他认证模式（注册 / 找回 / 重置 / 邀请）
          仍然给「返回登录」，避免在它们身上出现指向自己的注册链接。
        -->
        <footer>
          <RouterLink v-if="mode === 'login'" :to="accountPath('/register')">还没有账号？注册</RouterLink>
          <RouterLink v-else :to="accountPath('/login')">← 返回登录</RouterLink>
          <span>连接受安全会话保护</span>
        </footer>
      </div>
    </section>
  </main>
</template>
