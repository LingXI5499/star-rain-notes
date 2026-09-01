<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="placeholder">
    <h1 class="placeholder__title">{{ title }}</h1>
    <p class="placeholder__body">
      {{ description ?? '该页面将在对应的 Vertical Slice 任务中实现（见 05-development-plan.md）。' }}
    </p>
  </section>
</template>

<style scoped>
.placeholder {
  padding-block: var(--space-10) var(--space-13);
}

.placeholder__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-4);
  color: var(--text-primary);
}

.placeholder__body {
  font-size: 17px;
  line-height: 30px;
  color: var(--text-secondary);
  max-width: 560px;
}
</style>
