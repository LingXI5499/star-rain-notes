<script setup lang="ts">
defineProps<{
  items: Array<{ title: string; to: string; note?: string; kind: string }>
}>()
</script>

<template>
  <div class="evidence-grid" role="list">
    <a
      v-for="(item, index) in items"
      :key="`${item.to}-${index}`"
      class="evidence-card"
      role="listitem"
      :href="item.to"
    >
      <span class="evidence-card__kind">{{ item.kind }}</span>
      <span class="evidence-card__title">{{ item.title }}</span>
      <span v-if="item.note" class="evidence-card__note">{{ item.note }}</span>
      <span class="evidence-card__arrow" aria-hidden="true">→</span>
    </a>
  </div>
</template>

<style scoped>
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}
.evidence-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  transition: border-color var(--motion-fast) var(--ease-standard), box-shadow var(--motion-base) var(--ease-standard), transform var(--motion-base) var(--ease-out);
}
.evidence-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}
.evidence-card__kind {
  font: 700 9px var(--font-mono, monospace);
  letter-spacing: .16em;
  color: var(--accent);
}
.evidence-card__title {
  font-size: 17px;
  font-weight: 650;
  color: var(--text-primary);
}
.evidence-card__note {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}
.evidence-card__arrow {
  margin-top: auto;
  align-self: flex-start;
  color: var(--primary);
  transition: transform var(--motion-base) var(--ease-out);
}
.evidence-card:hover .evidence-card__arrow { transform: translateX(4px); }
@media (max-width: 640px) {
  .evidence-grid { grid-template-columns: 1fr; }
}
</style>
