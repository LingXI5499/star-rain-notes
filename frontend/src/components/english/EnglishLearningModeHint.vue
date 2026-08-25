<script setup lang="ts">
defineProps<{ isAuthenticated: boolean; email?: string | null }>()
</script>

<template>
  <p class="english-mode-hint">
    <template v-if="isAuthenticated">已登录，英语进度将同步到 <b>{{ email }}</b>。</template>
    <template v-else>当前为游客，英语进度仅保存在此浏览器。</template>
  </p>
</template>

<style scoped>
.english-mode-hint { font-size: 13px; color: var(--text-secondary); margin: 8px 0 0; }
.english-mode-hint b { color: var(--text-primary); }
</style>
