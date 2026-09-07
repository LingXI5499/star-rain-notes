<script setup lang="ts">
import { RouterLink } from 'vue-router'
import CefrBadge from './CefrBadge.vue'
import type { LearningBundle } from '@/api/englishBundle'

const props = defineProps<{ bundles: LearningBundle[]; emptyHint?: string }>()
</script>

<template>
  <div v-if="props.bundles.length" class="bundle-cards">
    <RouterLink
      v-for="bundle in props.bundles"
      :key="bundle.id"
      :to="`/english/bundles/${bundle.slug}`"
      class="bundle-card"
    >
      <div class="bundle-card__top">
        <CefrBadge :level="bundle.primaryCefr" />
        <span class="bundle-card__status">READ · LISTEN · WRITE</span>
      </div>
      <h3 class="bundle-card__title">{{ bundle.title }}</h3>
      <p class="bundle-card__summary">{{ bundle.summary || '跨阅读、听力与写作的学习组合。' }}</p>
      <span class="bundle-card__cta">开始学习 →</span>
    </RouterLink>
  </div>
  <p v-else class="bundle-cards__empty">{{ props.emptyHint ?? '暂无学习组合。' }}</p>
</template>

<style scoped>
.bundle-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
}
.bundle-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 20px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
  color: var(--text-primary);
  transition: transform 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease;
}
.bundle-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
}
.bundle-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.bundle-card__status {
  font: 700 9px/1.2 var(--font-mono);
  letter-spacing: 0.08em;
  color: var(--text-muted);
}
.bundle-card__title {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
  letter-spacing: -0.02em;
}
.bundle-card__summary {
  margin: 0;
  flex: 1;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bundle-card__cta {
  margin-top: auto;
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
}
.bundle-cards__empty {
  color: var(--text-muted);
  padding: var(--space-6) 0;
}
@media (prefers-reduced-motion: reduce) {
  .bundle-card {
    transition: none;
  }
}
</style>
