<script setup>
/*
 * 工程证据卡片网格 —— 从 V1 `components/about/EvidenceGrid.vue` 移植。
 *
 * V1 用 `<a href>`，V2 必须是 RouterLink：账号树下要落在 /useradmin 前缀，
 * 所以 `to` 由调用方经 useViewMode().contentPath() 生成，本组件不做路径加工。
 */
import { RouterLink } from 'vue-router'

defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <div class="evidence-grid" role="list">
    <RouterLink
      v-for="(item, index) in items"
      :key="`${item.to}-${index}`"
      class="evidence-card"
      role="listitem"
      :to="item.to"
    >
      <span class="evidence-card__kind">{{ item.kind }}</span>
      <span class="evidence-card__title">{{ item.title }}</span>
      <span v-if="item.note" class="evidence-card__note">{{ item.note }}</span>
      <span class="evidence-card__arrow" aria-hidden="true">→</span>
    </RouterLink>
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
  color: var(--text-primary);
  background: var(--bg-surface);
  transition:
    border-color var(--motion-fast) var(--ease-standard),
    box-shadow var(--motion-base) var(--ease-standard),
    transform var(--motion-base) var(--ease-out);
}

.evidence-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

.evidence-card__kind {
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

.evidence-card__title {
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 650;
}

.evidence-card__note {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.evidence-card__arrow {
  align-self: flex-start;
  margin-top: auto;
  color: var(--primary);
  transition: transform var(--motion-base) var(--ease-out);
}

.evidence-card:hover .evidence-card__arrow { transform: translateX(4px); }

@media (max-width: 640px) {
  .evidence-grid { grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  .evidence-card,
  .evidence-card__arrow { transition: none; }
}
</style>
