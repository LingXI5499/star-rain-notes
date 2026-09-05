<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchVocabularyLayers, type VocabularyLayer } from '@/api/vocabulary'

/**
 * Vocabulary category page (菜鸟教程-style card grid): layers as filter
 * chips on top, theme cards below. Click a theme to browse its word cards.
 */
const layers = ref<VocabularyLayer[]>([])
const loading = ref(true)
const error = ref(false)
const activeLayer = ref<string>('')

const visibleLayers = computed(() =>
  activeLayer.value ? layers.value.filter((l) => l.layer === activeLayer.value) : layers.value,
)

const totalWords = computed(() =>
  layers.value.reduce((n, layer) => n + layer.themes.reduce((m, t) => m + t.wordCount, 0), 0),
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
        按主题分类的英语词汇库 · {{ layers.length }} 个词层 / {{ totalWords }} 词
      </p>
    </header>

    <nav class="vocab__study-nav" aria-label="单词学习工具">
      <RouterLink to="/english/vocabulary/study"><strong>今日学习</strong><span>复习到期单词，或从主题开始新词</span></RouterLink>
      <RouterLink to="/english/vocabulary/progress"><strong>学习进度</strong><span>查看复习间隔与真实完成记录</span></RouterLink>
    </nav>

    <div v-if="loading" class="vocab__empty">加载中…</div>
    <div v-else-if="error" class="vocab__empty">加载失败，请稍后重试。</div>
    <template v-else>
      <div class="vocab__chips" role="group" aria-label="词层筛选">
        <button
          type="button"
          class="vocab__chip"
          :class="{ 'vocab__chip--active': activeLayer === '' }"
          @click="activeLayer = ''"
        >
          全部
        </button>
        <button
          v-for="layer in layers"
          :key="layer.layer"
          type="button"
          class="vocab__chip"
          :class="{ 'vocab__chip--active': activeLayer === layer.layer }"
          @click="activeLayer = layer.layer"
        >
          {{ layer.layer }}
        </button>
      </div>

      <div v-for="layer in visibleLayers" :key="layer.layer" class="vocab__group">
        <h2 v-if="visibleLayers.length > 1" class="vocab__group-title">{{ layer.layer }}</h2>
        <div class="vocab__grid">
          <RouterLink
            v-for="theme in layer.themes"
            :key="theme.id"
            :to="`/english/vocabulary/${theme.id}`"
            class="vocab__card"
          >
            <span class="vocab__card-layer">{{ layer.layer }}</span>
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
  margin-bottom: var(--space-3);
}

.vocab__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-2);
}

.vocab__subtitle {
  font-size: 16px;
  color: var(--text-secondary);
}

.vocab__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}
.vocab__study-nav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin:-12px 0 28px}.vocab__study-nav a{display:grid;gap:4px;padding:18px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}.vocab__study-nav a:hover{border-color:var(--primary)}.vocab__study-nav strong{font-size:17px;color:var(--text-primary)}.vocab__study-nav span{font-size:13px;color:var(--text-muted)}

/* layer filter chips */
.vocab__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
}

.vocab__chip {
  padding: var(--space-2) var(--space-4);
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.vocab__chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.vocab__chip--active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}

/* theme card grid (菜鸟教程-style) */
.vocab__group {
  margin-bottom: var(--space-10);
}

.vocab__group-title {
  font-size: 20px;
  line-height: 28px;
  margin-bottom: var(--space-5);
  color: var(--text-secondary);
}

.vocab__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-5);
}

.vocab__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.vocab__card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgb(0 0 0 / 0.08);
  border-color: var(--primary);
}

.vocab__card-layer {
  font-size: 12px;
  color: var(--text-muted);
}

.vocab__card-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
}

.vocab__card-count {
  font-size: 13px;
  color: var(--primary);
}
@media(max-width:640px){.vocab__study-nav{grid-template-columns:1fr}}
</style>
