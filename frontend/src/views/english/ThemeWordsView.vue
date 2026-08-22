<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  fetchThemeWords,
  fetchVocabularyLayers,
  formatMemoryTime,
  incrementMemory,
  type VocabularyWord,
} from '@/api/vocabulary'

/**
 * Theme vocabulary cards: responsive grid (mobile = one card per row).
 * Each card shows part of speech, word, US phonetic, translation,
 * inflections/derivatives, user-added examples and the personal memory
 * record (count + last time, +1 button persisted server-side).
 */
const route = useRoute()
const themeId = Number(route.params.themeId)

const themeName = ref('')
const themeLayer = ref('')
const items = ref<VocabularyWord[]>([])
const total = ref(0)
const page = ref(1)
const totalPages = ref(0)
const loading = ref(true)
const error = ref(false)
const memorizing = ref<Set<number>>(new Set())
const rememberedOnly = ref(false)
const PAGE_SIZE = 24

async function load() {
  loading.value = true
  try {
    const result = await fetchThemeWords(themeId, {
      page: page.value,
      pageSize: PAGE_SIZE,
      remembered: rememberedOnly.value || undefined,
    })
    items.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
    error.value = false
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function selectFilter(remembered: boolean) {
  if (rememberedOnly.value === remembered) return
  rememberedOnly.value = remembered
  page.value = 1
  void load()
}

async function remember(word: VocabularyWord) {
  if (memorizing.value.has(word.id)) return
  memorizing.value.add(word.id)
  try {
    const updated = await incrementMemory(word.id)
    const index = items.value.findIndex((w) => w.id === word.id)
    if (index >= 0) items.value[index] = updated
  } catch {
    // keep the old count; the card stays usable
  } finally {
    memorizing.value.delete(word.id)
  }
}

function goPage(next: number) {
  if (next < 1 || next > totalPages.value) return
  page.value = next
}

watch(page, () => void load())

onMounted(async () => {
  try {
    const layers = await fetchVocabularyLayers()
    for (const layer of layers) {
      const theme = layer.themes.find((t) => t.id === themeId)
      if (theme) {
        themeName.value = theme.name
        themeLayer.value = layer.layer
        break
      }
    }
  } catch {
    // theme name is decorative; the word list load below reports errors
  }
  await load()
})

const pageLabel = computed(() => (total.value ? `${page.value} / ${totalPages.value} 页 · 共 ${total.value} 词` : ''))
</script>

<template>
  <section class="words">
    <header class="words__header">
      <RouterLink to="/english/vocabulary" class="words__back">← 词汇</RouterLink>
      <p class="words__layer">{{ themeLayer }}</p>
      <h1 class="words__title">{{ themeName }}</h1>
      <p v-if="!loading && !error" class="words__meta">{{ pageLabel }}</p>
    </header>

    <div class="words__chips" role="group" aria-label="记忆筛选">
      <button
        type="button"
        class="words__chip"
        :class="{ 'words__chip--active': !rememberedOnly }"
        @click="selectFilter(false)"
      >
        全部
      </button>
      <button
        type="button"
        class="words__chip"
        :class="{ 'words__chip--active': rememberedOnly }"
        @click="selectFilter(true)"
      >
        已记忆
      </button>
    </div>

    <div v-if="loading" class="words__empty">加载中…</div>
    <div v-else-if="error" class="words__empty">加载失败，请稍后重试。</div>
    <template v-else>
      <div v-if="!items.length" class="words__empty">该主题暂无词汇。</div>
      <div v-else class="words__grid">
        <article v-for="word in items" :key="word.id" class="word-card">
          <div class="word-card__head">
            <span class="word-card__pos">{{ word.partOfSpeech }}</span>
            <h2 class="word-card__word">{{ word.word }}</h2>
          </div>
          <p v-if="word.phoneticUs" class="word-card__phonetic">{{ word.phoneticUs }}</p>
          <p class="word-card__translation">{{ word.translation }}</p>
          <p v-if="word.inflections" class="word-card__inflections">{{ word.inflections }}</p>

          <div v-if="word.examples.length" class="word-card__examples">
            <p class="word-card__label">例句</p>
            <p v-for="(example, i) in word.examples" :key="i" class="word-card__example">
              {{ example.sentence }}
              <span v-if="example.translation" class="word-card__example-trans">{{ example.translation }}</span>
            </p>
          </div>

          <div class="word-card__memory">
            <span class="word-card__memory-info">
              记忆 {{ word.memoryCount }} 次<span v-if="word.lastMemoryAt">
                · 最近 {{ formatMemoryTime(word.lastMemoryAt) }}</span>
            </span>
            <button
              type="button"
              class="word-card__memory-btn"
              :disabled="memorizing.has(word.id)"
              @click="remember(word)"
            >
              {{ memorizing.has(word.id) ? '…' : '+1 记忆' }}
            </button>
          </div>
        </article>
      </div>

      <nav v-if="totalPages > 1" class="words__pager" aria-label="分页">
        <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="words__pager-label">{{ pageLabel }}</span>
        <button type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.words__header {
  margin-bottom: var(--space-8);
}

.words__back {
  display: inline-block;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
}

.words__back:hover {
  color: var(--primary);
}

.words__layer {
  font-size: 14px;
  letter-spacing: 0.12em;
  color: var(--accent);
  margin-bottom: var(--space-2);
}

.words__title {
  font-size: 36px;
  line-height: 44px;
  margin-bottom: var(--space-2);
}

.words__meta {
  font-size: 14px;
  color: var(--text-muted);
}

/* memory filter chips */
.words__chips {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.words__chip {
  padding: var(--space-1) var(--space-4);
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.words__chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.words__chip--active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}

.words__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

/* word cards: auto-fill grid; on phones (≤~640px) exactly one column */
.words__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.word-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.word-card:hover {
  border-color: var(--border-strong);
  box-shadow: 0 6px 20px rgb(0 0 0 / 0.05);
}

.word-card__head {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.word-card__pos {
  font-size: 12px;
  color: var(--accent);
  border: 1px solid color-mix(in srgb, var(--accent) 45%, transparent);
  border-radius: 4px;
  padding: 1px 6px;
  white-space: nowrap;
}

.word-card__word {
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.word-card__phonetic {
  font-size: 15px;
  color: var(--text-secondary);
}

.word-card__translation {
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.6;
}

.word-card__inflections {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.word-card__examples {
  margin-top: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--border);
}

.word-card__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: var(--space-1);
}

.word-card__example {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.word-card__example-trans {
  color: var(--text-muted);
}

.word-card__memory {
  margin-top: auto;
  padding-top: var(--space-3);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.word-card__memory-info {
  font-size: 12px;
  color: var(--text-muted);
}

.word-card__memory-btn {
  border: 1px solid var(--primary);
  border-radius: 999px;
  background: transparent;
  color: var(--primary);
  font-size: 13px;
  padding: var(--space-1) var(--space-4);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.word-card__memory-btn:hover:not(:disabled) {
  background: var(--primary);
  color: var(--on-primary);
}

.word-card__memory-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.words__pager {
  margin-top: var(--space-8);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-5);
}

.words__pager button {
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  padding: var(--space-2) var(--space-5);
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease;
}

.words__pager button:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.words__pager button:disabled {
  opacity: 0.4;
  cursor: default;
}

.words__pager-label {
  font-size: 14px;
  color: var(--text-muted);
}
</style>
