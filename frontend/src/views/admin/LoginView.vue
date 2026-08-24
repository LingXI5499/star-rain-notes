<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'
import type { ProblemDetail } from '@/api/http'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

onMounted(async () => {
  // If no admin exists yet, direct the visitor to setup first.
  if (await auth.fetchSetupRequired()) {
    router.replace({ name: 'admin-setup' })
  }
})

async function submit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码。')
    return
  }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    await router.push(redirect)
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '登录失败，请检查用户名和密码。')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-card">
      <h1 class="auth-card__title">星雨笔录 · 管理登录</h1>
      <el-form label-position="top" class="auth-card__form" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            autocomplete="current-password"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" class="auth-card__submit" :loading="loading" @click="submit">
          登录
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
  margin-bottom: var(--space-6);
  color: var(--text-primary);
}

.auth-card__submit {
  width: 100%;
}
</style>
