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
      <div class="bundle-card__cover" :style="bundle.coverUrl ? { backgroundImage: `url(${bundle.coverUrl})` } : {}">
        <span>{{ bundle.title.slice(0, 1) }}</span>
      </div>
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
.bundle-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: var(--space-4); }
.bundle-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  color: var(--text-primary);
  transition: transform 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease;
}
.bundle-card__cover{display:grid;height:112px;margin:-12px -12px 6px;place-items:center;border-radius:13px;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-subtle)),color-mix(in srgb,var(--accent) 12%,var(--bg-surface)));background-position:center;background-size:cover;overflow:hidden}.bundle-card__cover span{font-size:38px;font-weight:800;color:color-mix(in srgb,var(--primary) 70%,white);text-shadow:0 2px 16px rgb(0 0 0/.12)}
.bundle-card:hover { transform: translateY(-3px); border-color: var(--primary); box-shadow: 0 16px 36px rgb(16 49 39 / 0.09); }
.bundle-card__top { display: flex; justify-content: space-between; align-items: center; }
.bundle-card__status { font-size: 10px; color: var(--text-muted); }
.bundle-card__title { font-size: 18px; margin: 8px 0 0; }
.bundle-card__summary { font-size: 13px; color: var(--text-secondary); line-height: 1.6; flex: 1; }
.bundle-card__cta { color: var(--primary); font-size: 13px; margin-top: auto; }
.bundle-cards__empty { color: var(--text-muted); padding: var(--space-6) 0; }
@media(prefers-reduced-motion:reduce){.bundle-card{transition:none}}
</style>
