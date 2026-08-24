<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import { http } from '@/api/http'
import type { ProblemDetail } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const setupToken = ref('')
const loading = ref(false)

onMounted(async () => {
  // Setup is only available while no administrator exists.
  if (!(await auth.fetchSetupRequired())) {
    router.replace({ name: 'admin-login' })
  }
})

async function submit() {
  if (!username.value || !password.value || !setupToken.value) {
    ElMessage.warning('请填写完整：用户名、密码与设置令牌。')
    return
  }
  loading.value = true
  try {
    await http.post(
      '/setup/admin',
      { username: username.value, password: password.value },
      { headers: { 'X-Setup-Token': setupToken.value } },
    )
    ElMessage.success('管理员创建成功，请登录。')
    await router.replace({ name: 'admin-login' })
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '初始化失败，请检查设置令牌与输入。')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-card">
      <h1 class="auth-card__title">星雨笔录 · 初始化管理员</h1>
      <p class="auth-card__hint">首次部署需使用 APP_SETUP_TOKEN 创建唯一管理员。</p>
      <el-form label-position="top" class="auth-card__form" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="设置令牌（APP_SETUP_TOKEN）">
          <el-input v-model="setupToken" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="auth-card__submit" :loading="loading" @click="submit">
          创建管理员
        </el-button>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
  padding: var(--space-6);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--space-8);
}

.auth-card__title {
  font-size: 22px;
  line-height: 30px;
  margin-bottom: var(--space-3);
  color: var(--text-primary);
}

.auth-card__hint {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-muted);
  margin-bottom: var(--space-6);
}

.auth-card__submit {
  width: 100%;
}
</style>
