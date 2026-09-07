<script setup lang="ts">
withDefaults(
  defineProps<{
    rows?: number
    label?: string
  }>(),
  { rows: 3, label: '内容加载中' },
)
</script>

<template>
  <div class="skeleton" role="status" :aria-label="label">
    <div v-for="n in rows" :key="n" class="skeleton__row" :style="{ width: `${88 - (n - 1) * 12}%` }" />
  </div>
</template>

<style scoped>
.skeleton {
  display: grid;
  gap: 12px;
  padding: 8px 0;
}
.skeleton__row {
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    var(--bg-subtle) 0%,
    color-mix(in srgb, var(--bg-surface) 70%, var(--bg-subtle)) 50%,
    var(--bg-subtle) 100%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}
@keyframes skeleton-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
@media (prefers-reduced-motion: reduce) {
  .skeleton__row { animation: none; }
}
</style>
