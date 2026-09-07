<script setup lang="ts">
import ArticleOutline from '@/components/ArticleOutline.vue'
import type { OutlineItem } from '@/types'

/**
 * Right-rail for public readers:「本页导航」+「学习信息」.
 * Matches the tutorial chapter layout — flat sections, no card shells,
 * Chinese labels for字数 / 预计阅读, optional extra info rows via props/slot.
 */
defineProps<{
  items: OutlineItem[]
  charCount: number
  readMinutes: number
  /** Extra dl rows under the default字数 / 预计阅读 (e.g. 阅读进度). */
  extraInfo?: Array<{ label: string; value: string }>
}>()
</script>

<template>
  <aside class="reading-aside">
    <div v-if="items.length" class="reading-aside__card">
      <p class="reading-aside__title">本页导航</p>
      <ArticleOutline :items="items" hide-title embedded />
    </div>
    <div class="reading-aside__card">
      <p class="reading-aside__title">学习信息</p>
      <dl class="reading-aside__info">
        <div>
          <dt>字数</dt>
          <dd>{{ charCount }}</dd>
        </div>
        <div>
          <dt>预计阅读</dt>
          <dd>{{ readMinutes }} 分钟</dd>
        </div>
        <div v-for="row in extraInfo ?? []" :key="row.label">
          <dt>{{ row.label }}</dt>
          <dd>{{ row.value }}</dd>
        </div>
      </dl>
      <slot name="info-extra" />
    </div>
    <slot />
  </aside>
</template>

<style scoped>
.reading-aside {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.reading-aside__card {
  padding: 0 0 var(--space-4);
}

.reading-aside__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

.reading-aside__info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.reading-aside__info div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: 13px;
}

.reading-aside__info dt {
  color: var(--text-muted);
  flex-shrink: 0;
}

.reading-aside__info dd {
  margin: 0;
  color: var(--text-primary);
  text-align: right;
  overflow-wrap: anywhere;
}
</style>
