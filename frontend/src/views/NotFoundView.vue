<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found">
    <h1 class="not-found__code">404</h1>
    <p class="not-found__body">页面不存在或已被移除。</p>
    <RouterLink class="not-found__link" to="/">返回首页</RouterLink>
  </section>
</template>

<style scoped>
.not-found {
  padding-block: var(--space-13);
  text-align: center;
}

.not-found__code {
  font-size: 72px;
  line-height: 76px;
  color: var(--primary);
  margin-bottom: var(--space-4);
}

.not-found__body {
  color: var(--text-secondary);
  margin-bottom: var(--space-6);
}
</style>
