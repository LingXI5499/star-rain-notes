<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { searchPublic, resolveSearchResultRoute, SEARCH_TYPE_LABELS, type SearchItem } from '@/api/search'
import HighlightText from '@/components/search/HighlightText.vue'

/**
 * Full search page (02 §13): query/type/page live in the URL; type filters;
 * safe client-side highlighting; server-side pagination; 250ms debounce and
 * stale-request abort. No search history / hot search.
 */
const route = useRoute()
const router = useRouter()

const input = ref('')
const items = ref<SearchItem[]>([])
const counts = ref({ tutorial: 0, chapter: 0, blog: 0, portfolio: 0, grammar: 0, reading: 0, listening: 0, writing: 0 })
const total = ref(0)
const page = ref(1)
const totalPages = ref(0)
const loading = ref(false)
const searched = ref(false)
const error = ref(false)

let debounceTimer: number | undefined
let controller: AbortController | null = null

const typeLabels: Record<string, string> = SEARCH_TYPE_LABELS

const filterOptions = [
  { value: '', label: '全部' },
  { value: 'tutorial', label: '教程' },
  { value: 'grammar', label: '英语语法' },
  { value: 'reading', label: '英语阅读' },
  { value: 'listening', label: '英语听力' },
  { value: 'writing', label: '英语写作' },
  { value: 'blog', label: '博客' },
  { value: 'portfolio', label: '作品' },
]

function currentType(): string {
  return typeof route.query.type === 'string' ? route.query.type : ''
}

function syncFromQuery() {
  input.value = typeof route.query.q === 'string' ? route.query.q : ''
  page.value = typeof route.query.page === 'string' ? Math.max(Number(route.query.page) || 1, 1) : 1
}

async function load() {
  const q = input.value.trim()
  controller?.abort()
  if (q.length < 2) {
    items.value = []
    total.value = 0
    totalPages.value = 0
    searched.value = false
    error.value = false
    loading.value = false
    return
  }
  controller = new AbortController()
  loading.value = true
  searched.value = true
  error.value = false
  try {
    const result = await searchPublic(
      { q, type: currentType() || undefined, page: page.value, pageSize: 10 },
      controller.signal,
    )
    items.value = result.items
    counts.value = result.counts
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (err) {
    if ((err as Error)?.name === 'CanceledError') {
      return
    }
    items.value = []
    total.value = 0
    error.value = true
  } finally {
    loading.value = false
  }
}

function pushQuery(patch: { q?: string; type?: string; page?: number }) {
  const query: Record<string, string> = {}
  const q = patch.q ?? input.value.trim()
  const type = patch.type ?? currentType()
  const p = patch.page ?? page.value
  if (q) query.q = q
  if (type) query.type = type
  if (p > 1) query.page = String(p)
  router.replace({ query })
}

function onInput() {
  window.clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => pushQuery({ q: input.value, page: 1 }), 250)
}

function selectType(type: string) {
  pushQuery({ type, page: 1 })
}

function goPage(next: number) {
  pushQuery({ page: next })
}

watch(
  () => route.query,
  () => {
    syncFromQuery()
    void load()
  },
)

onMounted(() => {
  syncFromQuery()
  void load()
})

onBeforeUnmount(() => {
  window.clearTimeout(debounceTimer)
  controller?.abort()
})
</script>

<template>
  <section class="search-page">
    <header class="search-page__hero">
      <p class="public-eyebrow">DISCOVER · KNOWLEDGE INDEX</p>
      <h1 class="search-page__title">搜索知识坐标</h1>
      <p>在教程、英语学习、博客与作品之间，快速找到已经沉淀的内容。</p>
    </header>

    <div class="search-page__bar">
      <el-input
        v-model="input"
        placeholder="输入关键词搜索教程、英语学习、博客或作品"
        clearable
        size="large"
        @input="onInput"
        @keyup.enter="pushQuery({ page: 1 })"
      />
    </div>

    <div class="search-page__filters" role="group" aria-label="类型筛选">
      <button
        v-for="option in filterOptions"
        :key="option.value"
        type="button"
        class="search-page__chip"
        :class="{ 'is-active': currentType() === option.value }"
        @click="selectType(option.value)"
      >
        {{ option.label }}
      </button>
    </div>

    <div v-if="loading" class="search-page__state"><span aria-hidden="true">✦</span><strong>正在搜索知识库…</strong></div>
    <div v-else-if="error" class="search-page__state"><span aria-hidden="true">!</span><strong>搜索失败，请稍后重试。</strong></div>
    <div v-else-if="searched && total === 0" class="search-page__state"><span aria-hidden="true">○</span><strong>没有找到相关内容</strong><small>尝试缩短关键词或切换内容类型。</small></div>
    <div v-else-if="!searched" class="search-page__guide"><span>⌘ K</span><div><strong>从一个关键词开始</strong><p>支持标题与摘要检索，至少输入两个字符。</p></div></div>
    <template v-else-if="searched">
      <p class="search-page__meta">
        共 {{ total }} 条结果
        <span v-if="currentType()">（教程 {{ counts.tutorial }} · 章节 {{ counts.chapter }} · 语法 {{ counts.grammar }} · 阅读 {{ counts.reading }} · 听力 {{ counts.listening }} · 写作 {{ counts.writing }} · 博客 {{ counts.blog }} · 作品 {{ counts.portfolio }}）</span>
      </p>

      <ul class="search-page__results">
        <li v-for="item in items" :key="`${item.type}-${item.id}`" class="search-result">
          <RouterLink :to="resolveSearchResultRoute(item)" class="search-result__link">
            <span class="search-result__type">{{ typeLabels[item.type] ?? item.type }}</span>
            <span class="search-result__title"><HighlightText :text="item.title" :query="input" /></span>
            <span v-if="item.summary" class="search-result__summary"><HighlightText :text="item.summary" :query="input" /></span>
          </RouterLink>
        </li>
      </ul>

      <el-pagination
        v-if="totalPages > 1"
        :current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        @current-change="goPage"
      />
    </template>
  </section>
</template>

<style scoped>
.search-page{max-width:1000px;margin:auto;padding:34px 0 90px}.search-page__hero{position:relative;padding:44px 0 38px;border-bottom:1px solid var(--border)}.search-page__hero::after{position:absolute;right:2%;bottom:25px;width:140px;height:70px;border-top:1px solid color-mix(in srgb,var(--primary) 42%,transparent);border-radius:50%;content:''}.search-page__hero>p:last-child{max-width:650px;color:var(--text-secondary);font-size:14px;line-height:1.8}
.search-page__title {
  margin: 10px 0 12px;
  font-size: clamp(38px,6vw,66px);
  line-height: 1.08;
  letter-spacing: -.055em;
}

.search-page__bar {
  max-width: 760px;
  margin: 30px 0 var(--space-5);
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
  box-shadow: var(--shadow-sm);
}
.search-page__bar :deep(.el-input__wrapper){min-height:48px;border-radius:11px;background:var(--bg-elevated);box-shadow:none!important}.search-page__bar :deep(.el-input__inner){font-size:15px}

.search-page__filters {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  overflow-x:auto;
  padding-bottom:2px;
  scrollbar-width:none;
  -ms-overflow-style:none;
}
.search-page__filters::-webkit-scrollbar{display:none}

.search-page__chip {
  padding: var(--space-2) var(--space-4);
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}

.search-page__chip:hover,
.search-page__chip.is-active {
  border-color: var(--primary);
  color: var(--primary);
}

.search-page__state {
  display:grid;
  min-height:260px;
  place-content:center;
  gap:7px;
  color:var(--text-muted);
  text-align:center;
}
.search-page__state>span{color:var(--accent);font:700 34px var(--font-mono)}.search-page__state>strong{color:var(--text-primary);font-size:17px}.search-page__state>small{font-size:12px}.search-page__guide{display:flex;align-items:center;gap:18px;min-height:180px;padding:24px;border:1px dashed var(--border-strong);border-radius:18px;background:color-mix(in srgb,var(--bg-surface) 50%,transparent)}.search-page__guide>span{display:grid;width:64px;height:52px;place-items:center;border:1px solid var(--border);border-radius:12px;color:var(--primary);background:var(--bg-surface);font:700 13px var(--font-mono);box-shadow:var(--shadow-xs)}.search-page__guide strong{font-size:17px}.search-page__guide p{color:var(--text-muted);font-size:12px}

.search-page__meta {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--space-4);
}

.search-page__results {
  list-style: none;
  display: flex;
  flex-direction:column;
  gap:10px;
}

.search-result__link {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding:18px;
  border:1px solid var(--border);
  border-radius:15px;
  color: var(--text-primary);
  background:var(--bg-surface);
  transition:transform 160ms ease,border-color 160ms ease,box-shadow 160ms ease;
}
.search-result__link:hover{transform:translateY(-2px);border-color:var(--primary);box-shadow:var(--shadow-sm)}

.search-result__type {
  font-size: 12px;
  color: var(--accent);
}

.search-result__title {
  font-size: 18px;
  font-weight: 600;
}

.search-result__summary {
  font-size: 14px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media(max-width:600px){.search-page{padding-top:10px}.search-page__hero{padding-top:24px}.search-page__hero::after{display:none}.search-page__filters{margin-inline:-20px;padding-inline:20px}.search-page__chip{flex:none}.search-page__guide{align-items:flex-start}.search-result__summary{white-space:normal;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2}}
</style>
