<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { useViewMode } from '../../../../shared/viewMode'
import { getVocabularySummary, getVocabularyThemes } from '../../api/englishApi'

/*
 * 词库总览。
 *
 * 结构对齐 V1 VocabularyView：标题 -> 两张学习入口卡 -> 词类筛选 chips -> 每个分类组的主题卡网格。
 * 主题卡是「标题在上、N 词 在下一行」的两行布局，网格固定 6 列（窄屏降级）。
 * 入口卡上的数字来自四格学习统计：未登录时是浏览器本地进度，登录后是账号进度。
 */
const { contentPath } = useViewMode()

const themes = ref([])
const summary = ref(null)
const activeLayer = ref('')
const loading = ref(true)
const error = ref('')

const groups = computed(() => {
  const grouped = new Map()
  for (const theme of themes.value) {
    if (!grouped.has(theme.layer)) grouped.set(theme.layer, [])
    grouped.get(theme.layer).push(theme)
  }
  return [...grouped].map(([name, items]) => ({ name, items }))
})
const visibleGroups = computed(() => activeLayer.value
  ? groups.value.filter((group) => group.name === activeLayer.value)
  : groups.value)
const total = computed(() => themes.value.reduce((sum, theme) => sum + Number(theme.wordCount ?? 0), 0))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, stats] = await Promise.all([getVocabularyThemes(), getVocabularySummary()])
    themes.value = list
    summary.value = stats
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="vocabulary-page">
    <header class="vocabulary-page__hero">
      <p class="public-eyebrow">VOCABULARY · THEMED</p>
      <h1>词汇</h1>
      <p class="vocabulary-page__subtitle">
        按主题分类的英语词汇库 · {{ groups.length }} 个分类组 / {{ total.toLocaleString() }} 条分类词汇
      </p>
    </header>

    <nav class="vocabulary-page__study-nav" aria-label="单词学习工具">
      <RouterLink :to="contentPath('/english/vocabulary/study')">
        <strong>今日学习</strong>
        <span>复习到期单词，或从主题开始新词</span>
        <em v-if="summary">到期 {{ summary.dueForReview }} · 计划内 {{ summary.inProgress + summary.completed }}</em>
      </RouterLink>
      <RouterLink :to="contentPath('/english/vocabulary/progress')">
        <strong>学习进度</strong>
        <span>查看复习间隔与真实完成记录</span>
        <em v-if="summary">已完成 {{ summary.completed }} · 总计划 {{ summary.total }}</em>
      </RouterLink>
    </nav>

    <p v-if="loading" class="vocabulary-page__state">正在读取词库…</p>
    <p v-else-if="error" class="vocabulary-page__state" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></p>
    <template v-else>
      <div class="vocabulary-page__chips" role="group" aria-label="词类筛选">
        <button type="button" :class="{ active: !activeLayer }" @click="activeLayer = ''">全部</button>
        <button
          v-for="group in groups"
          :key="group.name"
          type="button"
          :class="{ active: activeLayer === group.name }"
          @click="activeLayer = group.name"
        >{{ group.name }}</button>
      </div>
      <section v-for="group in visibleGroups" :key="group.name" class="vocabulary-page__group">
        <h2 v-if="visibleGroups.length > 1">{{ group.name }}</h2>
        <div class="vocabulary-page__grid">
          <RouterLink
            v-for="theme in group.items"
            :key="theme.id"
            :to="contentPath(`/english/vocabulary/${theme.id}`)"
            class="vocabulary-page__card"
          >
            <span class="vocabulary-page__card-name">{{ theme.name }}</span>
            <span class="vocabulary-page__card-count">{{ theme.wordCount }} 词</span>
          </RouterLink>
        </div>
      </section>
      <p v-if="!themes.length" class="vocabulary-page__state">词库暂无内容。</p>
    </template>
  </main>
</template>

<style scoped>
.vocabulary-page{max-width:1200px;margin:auto;padding-bottom:70px}
.vocabulary-page__hero{margin-bottom:26px}
.vocabulary-page h1{margin:8px 0 10px;font-size:clamp(36px,5vw,48px)}
.vocabulary-page__subtitle{max-width:42rem;color:var(--text-secondary)}
.vocabulary-page__state{padding:48px 0;color:var(--text-secondary)}
.vocabulary-page__state button{margin-left:10px;padding:6px 12px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}
.vocabulary-page__study-nav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin:0 0 28px}
.vocabulary-page__study-nav a{display:grid;gap:4px;padding:18px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
.vocabulary-page__study-nav a:hover{border-color:var(--primary)}
.vocabulary-page__study-nav strong{font-size:17px;color:var(--text-primary)}
.vocabulary-page__study-nav span{font-size:13px;color:var(--text-muted)}
.vocabulary-page__study-nav em{font-size:12px;font-style:normal;color:var(--accent)}
.vocabulary-page__chips{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:28px}
.vocabulary-page__chips button{padding:8px 14px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}
.vocabulary-page__chips button:hover{border-color:var(--primary);color:var(--primary)}
.vocabulary-page__chips button.active{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
.vocabulary-page__group{margin-bottom:36px}
.vocabulary-page__group h2{margin:0 0 14px;font-size:18px;color:var(--text-primary)}
.vocabulary-page__grid{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:12px}
.vocabulary-page__card{display:grid;gap:8px;padding:16px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);color:var(--text-primary)}
.vocabulary-page__card:hover{border-color:var(--primary);transform:translateY(-2px)}
.vocabulary-page__card-name{font-size:16px;font-weight:650;line-height:1.4}
.vocabulary-page__card-count{font-size:13px;color:var(--text-muted)}
@media (max-width:1180px){.vocabulary-page__grid{grid-template-columns:repeat(4,minmax(0,1fr))}}
@media (max-width:900px){.vocabulary-page__grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media (max-width:640px){.vocabulary-page__study-nav{grid-template-columns:1fr}.vocabulary-page__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
