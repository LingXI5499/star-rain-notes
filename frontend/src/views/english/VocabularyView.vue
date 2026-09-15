<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchVocabularyLayers, type VocabularyLayer } from '@/api/vocabulary'
import { vocabularyFamilyName, vocabularyLayerTag } from '@/lib/vocabularyLayers'

interface ThemeCard {
  id: number
  name: string
  wordCount: number
  layer: string
  layerTag: string
}

interface FamilyGroup {
  family: string
  order: number
  themes: ThemeCard[]
}

/**
 * Vocabulary catalog: parent-family chips (ABC layers merged) + themed card grid.
 */
const layers = ref<VocabularyLayer[]>([])
const loading = ref(true)
const error = ref(false)
const activeFamily = ref('')

const families = computed((): FamilyGroup[] => {
  const map = new Map<string, FamilyGroup>()
  for (const layer of layers.value) {
    const family = vocabularyFamilyName(layer.layer)
    const existing = map.get(family)
    const themes = layer.themes.map((theme) => ({
      id: theme.id,
      name: theme.name,
      wordCount: theme.wordCount,
      layer: layer.layer,
      layerTag: vocabularyLayerTag(layer.layer),
    }))
    if (existing) {
      existing.order = Math.min(existing.order, layer.layerOrder)
      existing.themes.push(...themes)
    } else {
      map.set(family, { family, order: layer.layerOrder, themes: [...themes] })
    }
  }
  return [...map.values()].sort((a, b) => a.order - b.order || a.family.localeCompare(b.family, 'zh'))
})

const visibleFamilies = computed(() =>
  activeFamily.value ? families.value.filter((item) => item.family === activeFamily.value) : families.value,
)

const totalWords = computed(() =>
  families.value.reduce((sum, family) => sum + family.themes.reduce((n, theme) => n + theme.wordCount, 0), 0),
)

onMounted(async () => {
  try {
    layers.value = await fetchVocabularyLayers()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="vocab">
    <header class="vocab__hero">
      <p class="vocab__eyebrow">VOCABULARY · THEMED</p>
      <h1 class="vocab__title">词汇</h1>
      <p class="vocab__subtitle">
        按主题分类的英语词汇库 · {{ families.length }} 个词类 / {{ totalWords }} 词
      </p>
    </header>

    <nav class="vocab__study-nav" aria-label="单词学习工具">
      <RouterLink to="/english/vocabulary/study"><strong>今日学习</strong><span>复习到期单词，或从主题开始新词</span></RouterLink>
      <RouterLink to="/english/vocabulary/progress"><strong>学习进度</strong><span>查看复习间隔与真实完成记录</span></RouterLink>
    </nav>

    <div v-if="loading" class="vocab__empty">加载中…</div>
    <div v-else-if="error" class="vocab__empty">加载失败，请稍后重试。</div>
    <template v-else>
      <div class="vocab__chips" role="group" aria-label="词类筛选">
        <button
          type="button"
          class="vocab__chip"
          :class="{ 'vocab__chip--active': activeFamily === '' }"
          @click="activeFamily = ''"
        >
          全部
        </button>
        <button
          v-for="family in families"
          :key="family.family"
          type="button"
          class="vocab__chip"
          :class="{ 'vocab__chip--active': activeFamily === family.family }"
          @click="activeFamily = family.family"
        >
          {{ family.family }}
        </button>
      </div>

      <div v-for="family in visibleFamilies" :key="family.family" class="vocab__group">
        <h2 v-if="visibleFamilies.length > 1" class="vocab__group-title">{{ family.family }}</h2>
        <div class="vocab__grid">
          <RouterLink
            v-for="theme in family.themes"
            :key="theme.id"
            :to="`/english/vocabulary/${theme.id}`"
            class="vocab__card"
          >
            <span class="vocab__card-layer">{{ theme.layerTag }}</span>
            <span class="vocab__card-name">{{ theme.name }}</span>
            <span class="vocab__card-count">{{ theme.wordCount }} 词</span>
          </RouterLink>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.vocab__hero {
  margin-bottom: var(--space-9);
}

.vocab__eyebrow {
  font-size: 14px;
  letter-spacing: 0.18em;
  color: var(--accent);
}

.vocab__title {
  margin: 8px 0 10px;
  font-size: clamp(36px, 5vw, 48px);
}

.vocab__subtitle {
  color: var(--text-secondary);
  max-width: 42rem;
}

.vocab__empty {
  padding: 48px 0;
  color: var(--text-muted);
}

.vocab__study-nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: -12px 0 28px;
}

.vocab__study-nav a {
  display: grid;
  gap: 4px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
}

.vocab__study-nav a:hover {
  border-color: var(--primary);
}

.vocab__study-nav strong {
  font-size: 17px;
  color: var(--text-primary);
}

.vocab__study-nav span {
  font-size: 13px;
  color: var(--text-muted);
}

.vocab__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 28px;
}

.vocab__chip {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-surface);
  color: var(--text-secondary);
  cursor: pointer;
}

.vocab__chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.vocab__chip--active {
  border-color: var(--primary);
  background: var(--primary);
  color: var(--on-primary, #fff);
}

.vocab__group {
  margin-bottom: 36px;
}

.vocab__group-title {
  margin: 0 0 14px;
  font-size: 18px;
  color: var(--text-primary);
}

.vocab__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.vocab__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
}

.vocab__card:hover {
  border-color: var(--primary);
}

.vocab__card-layer {
  font-size: 12px;
  color: var(--accent);
  letter-spacing: 0.06em;
}

.vocab__card-name {
  font-size: 16px;
  font-weight: 650;
  color: var(--text-primary);
}

.vocab__card-count {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 640px) {
  .vocab__study-nav {
    grid-template-columns: 1fr;
  }
}
</style>
