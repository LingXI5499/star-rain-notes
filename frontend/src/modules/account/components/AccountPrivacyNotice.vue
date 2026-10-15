<script setup>
import { useAuthStore } from '../stores/authStore'
const auth = useAuthStore()
</script>
<template>
  <aside v-if="auth.cleanupError" class="account-privacy-notice" role="alert">
    <p>{{ auth.cleanupError }}</p>
    <button type="button" :disabled="auth.cleanupBusy" @click="auth.retryDraftCleanup()">{{ auth.cleanupBusy ? '正在清理…' : '重试清理本地副本' }}</button>
  </aside>
</template>
<style scoped>
.account-privacy-notice { position: fixed; bottom: 16px; left: 16px; right: 16px; z-index: 12000; padding: 16px; background: var(--bg-surface); color: var(--text-primary); border: 2px solid var(--border); border-radius: 8px; }
button { padding: 8px 16px; cursor: pointer; }
</style>
