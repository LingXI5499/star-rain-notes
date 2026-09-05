<script setup lang="ts">
defineProps<{
  headline: string | null
  focus: string[]
  featured: string | null
  status: string
}>()
</script>

<template>
  <dl class="career-snapshot">
    <div class="career-snapshot__cell">
      <dt>CURRENT</dt>
      <dd>{{ headline || '持续学习与构建中' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>DIRECTION</dt>
      <dd>{{ focus.length ? focus.join(' / ') : '全栈软件工程' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>FEATURED</dt>
      <dd>{{ featured || '星雨笔录' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>STATUS</dt>
      <dd>{{ status }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.career-snapshot {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin: 0;
}
.career-snapshot__cell {
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
}
.career-snapshot__cell dt {
  font: 700 9px var(--font-mono, monospace);
  letter-spacing: .16em;
  color: var(--accent);
  margin-bottom: var(--space-3);
}
.career-snapshot__cell dd {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
}
@media (max-width: 768px) {
  .career-snapshot { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
