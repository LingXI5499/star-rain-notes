<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'
import type { ProblemDetail } from '@/api/http'
import { fetchActivationStatus } from '@/api/account'
import BrandMark from '@/components/brand/BrandMark.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const serviceUnavailable = ref(false)

onMounted(async () => {
  try {
    const activation = await fetchActivationStatus()
    if (!activation.activated) {
      router.replace({ name: 'admin-activate' })
    }
  } catch {
    serviceUnavailable.value = true
  }
})

async function submit() {
  if (!email.value || !password.value) {
    ElMessage.warning('请输入邮箱和密码。')
    return
  }
  loading.value = true
  serviceUnavailable.value = false
  try {
    await auth.login(email.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    await router.push(redirect)
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    if (error instanceof AxiosError && !error.response) serviceUnavailable.value = true
    else ElMessage.error({ message: problem?.detail ?? '登录失败，请检查邮箱和密码。', grouping: true })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-story" aria-label="星雨笔录管理控制台介绍">
      <RouterLink to="/" class="auth-brand"><BrandMark :size="32" decorative /><span>星雨笔录</span></RouterLink>
      <div class="auth-story__copy">
        <p>INKSPACE · CONTENT STUDIO</p>
        <h1>把知识整理成<br><em>可持续的作品。</em></h1>
        <span>在一个安静、专注的工作区中维护教程、博客、作品与学习内容。</span>
      </div>
      <ul>
        <li><b>01</b><span><strong>结构化创作</strong>统一管理内容、媒体与发布状态</span></li>
        <li><b>02</b><span><strong>安全协作</strong>基于角色权限与完整审计记录</span></li>
        <li><b>03</b><span><strong>稳定发布</strong>让每一次更新都清晰、可追踪</span></li>
      </ul>
      <small>STAR RAIN NOTES · ADMIN CONSOLE</small>
    </section>

    <section class="auth-workspace">
      <div class="auth-card">
        <header>
          <span class="auth-card__mark"><BrandMark :size="28" decorative /></span>
          <p>WELCOME BACK</p>
          <h2>登录管理后台</h2>
          <small>使用管理员邮箱继续你的内容工作。</small>
        </header>

        <div v-if="serviceUnavailable" class="auth-service-error" role="alert">
          <strong>后台服务暂不可用</strong>
          <span>无法连接 API 服务，请确认后端与数据库已经启动。</span>
        </div>

        <el-form label-position="top" class="auth-card__form" @submit.prevent="submit">
          <el-form-item label="邮箱地址">
            <el-input v-model="email" size="large" autocomplete="username" placeholder="name@example.com" />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input v-model="password" size="large" type="password" autocomplete="current-password" placeholder="请输入密码" show-password @keyup.enter="submit" />
          </el-form-item>
          <div class="auth-form__assist"><span>仅授权管理员可访问</span><RouterLink to="/admin/forgot-password">忘记密码？</RouterLink></div>
          <el-button type="primary" size="large" class="auth-card__submit" :loading="loading" @click="submit">进入控制台 <span aria-hidden="true">→</span></el-button>
        </el-form>
        <footer><RouterLink to="/">← 返回公开站点</RouterLink><span>连接受安全会话保护</span></footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(420px, .92fr) minmax(520px, 1.08fr);
  background: #f4f3ed;
}
.auth-story{position:relative;display:flex;min-height:100dvh;overflow:hidden;flex-direction:column;padding:clamp(34px,5vw,72px);color:#eef4f0;background:#123b34}
.auth-story::before{position:absolute;inset:-25%;background:radial-gradient(circle at 28% 24%,rgb(129 184 165/.24),transparent 30%),radial-gradient(circle at 75% 75%,rgb(190 102 58/.18),transparent 28%);content:''}
.auth-story::after{position:absolute;inset:0;background:linear-gradient(90deg,transparent 49.8%,rgb(255 255 255/.045) 50%,transparent 50.2%),linear-gradient(transparent 49.8%,rgb(255 255 255/.04) 50%,transparent 50.2%);background-size:110px 110px;content:''}
.auth-story>*{position:relative;z-index:1}.auth-brand{display:flex;align-items:center;gap:12px;color:#fff;font-size:18px;font-weight:750;letter-spacing:.04em}.auth-story__copy{margin:auto 0 46px;max-width:600px}.auth-story__copy>p{margin:0 0 22px;color:#d68b62;font:750 10px/1.5 var(--font-family);letter-spacing:.2em}.auth-story__copy h1{margin:0;color:#fff;font-size:clamp(44px,5vw,76px);line-height:1.06;letter-spacing:-.055em}.auth-story__copy h1 em{color:#9bc7b9;font-style:normal}.auth-story__copy>span{display:block;max-width:450px;margin-top:26px;color:#b9cac4;font-size:15px;line-height:1.9}.auth-story ul{display:grid;gap:0;margin:0 0 42px;padding:0;border-top:1px solid rgb(255 255 255/.13);list-style:none}.auth-story li{display:grid;grid-template-columns:42px 1fr;gap:14px;padding:16px 0;border-bottom:1px solid rgb(255 255 255/.13)}.auth-story li b{color:#d68b62;font-size:10px}.auth-story li span{color:#9eb2ab;font-size:11px}.auth-story li strong{display:block;margin-bottom:3px;color:#fff;font-size:13px}.auth-story>small{color:#78958b;font-size:9px;letter-spacing:.16em}
.auth-workspace{display:grid;place-items:center;padding:48px;background:radial-gradient(circle at 85% 12%,rgb(168 88 49/.08),transparent 30%),#f4f3ed}
.auth-card{width:min(440px,100%);padding:clamp(30px,4vw,46px);border:1px solid #d8ddd8;border-radius:22px;background:rgb(255 255 252/.9);box-shadow:0 26px 70px rgb(27 47 39/.10)}
.auth-card header{margin-bottom:30px}.auth-card__mark{display:grid;width:52px;height:52px;margin-bottom:24px;place-items:center;border-radius:15px;background:#e4ece8}.auth-card header p{margin:0;color:#a85831;font-size:9px;font-weight:800;letter-spacing:.18em}.auth-card header h2{margin:8px 0 6px;color:#111814;font-size:28px;letter-spacing:-.035em}.auth-card header small{color:#707a75;font-size:13px}.auth-service-error{display:flex;gap:4px;margin-bottom:20px;padding:13px 15px;border:1px solid #efcfc8;border-radius:11px;flex-direction:column;color:#8c3e30;background:#fff1ed}.auth-service-error strong{font-size:13px}.auth-service-error span{font-size:11px}.auth-card :deep(.el-form-item__label){padding-bottom:7px;color:#34453e;font-size:12px;font-weight:650}.auth-card :deep(.el-input__wrapper){min-height:46px;border-radius:10px;box-shadow:0 0 0 1px #ccd5d0 inset}.auth-card :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px #24584d inset,0 0 0 3px rgb(36 88 77/.1)}.auth-form__assist{display:flex;justify-content:space-between;margin:-4px 0 22px;color:#818c86;font-size:11px}.auth-form__assist a{color:#24584d;font-weight:650}
.auth-card__submit{width:100%;height:48px;border:0;border-radius:11px!important;background:#24584d!important;font-weight:700}.auth-card__submit span{margin-left:auto}.auth-card footer{display:flex;justify-content:space-between;gap:16px;margin-top:28px;padding-top:20px;border-top:1px solid #e1e4df;color:#8b938e;font-size:10px}.auth-card footer a{color:#53645d}

@media(max-width:900px){.auth-page{grid-template-columns:1fr}.auth-story{min-height:auto;padding:28px 24px 34px}.auth-story__copy{margin:70px 0 28px}.auth-story__copy h1{font-size:clamp(38px,11vw,58px)}.auth-story ul,.auth-story>small{display:none}.auth-workspace{padding:38px 20px 64px}.auth-card{margin-top:-8px}}
@media(max-width:480px){.auth-story__copy{margin:48px 0 16px}.auth-story__copy>span{font-size:13px}.auth-card{padding:28px 22px;border-radius:18px}.auth-card footer{align-items:flex-start;flex-direction:column}.auth-workspace{padding-inline:14px}}
@media(prefers-reduced-motion:reduce){.auth-page *{scroll-behavior:auto!important;transition:none!important}}

[data-theme='dark'] .auth-workspace{background:#102c31}[data-theme='dark'] .auth-card{border-color:#294349;background:#16343a}[data-theme='dark'] .auth-card header h2{color:#f0f2ef}[data-theme='dark'] .auth-card header small,[data-theme='dark'] .auth-form__assist{color:#b0b7b3}[data-theme='dark'] .auth-card :deep(.el-form-item__label){color:#dbe3df}[data-theme='dark'] .auth-card footer{border-color:#294349}
</style>
