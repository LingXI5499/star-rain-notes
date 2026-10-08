<script setup>
import PublicSearch from '../../../shared/ui/PublicSearch.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
import PublicFilterTabs from '../../../shared/ui/PublicFilterTabs.vue'
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import { search } from '../api/searchApi'

const route = useRoute()
const router = useRouter()
const { contentPath } = useViewMode()
const q = ref(String(route.query.q || ''))
const loading = ref(false)
const error = ref('')
const result = ref({ items: [], total: 0, page: 1, pageSize: 20 })
const types = [
  { value: '', label: '全部' },
  { value: 'TUTORIAL', label: '教程' },
  { value: 'CHAPTER', label: '章节' },
  { value: 'BLOG', label: '博客' },
  { value: 'PORTFOLIO', label: '作品' },
  { value: 'PROFILE', label: '作者' },
]
const typeLabel = Object.fromEntries(types.map((item) => [item.value, item.label]))
const activeType = computed(() => String(route.query.type || ''))
const page = computed(() => publicPage(route.query.page))
const pageSize = computed(() => publicPageSize(route.query.pageSize, 20, [10, 20, 50]))
function changeSize(size) { navigate(sizeQuery(route.query, size, 20)) }
let request = null

function navigate(next = {}) {
  const query = { ...route.query, q: q.value.trim(), ...(activeType.value ? { type: activeType.value } : {}), ...next }
  if (!query.type) delete query.type
  if (!query.page || Number(query.page) === 1) delete query.page
  router.push({ path: contentPath('/search'), query })
}

watch(() => [route.query.q, route.query.type, route.query.page, route.query.pageSize], async () => {
  request?.abort()
  loading.value = false
  q.value = String(route.query.q || '')
  result.value = { items: [], total: 0, page: 1, pageSize: pageSize.value }
  error.value = ''
  if (q.value.trim().length < 2) return
  const current = new AbortController()
  request = current
  loading.value = true
  try {
    const data = await search(q.value.trim(), activeType.value, page.value, pageSize.value, current.signal)
    if (!current.signal.aborted) result.value = data
  } catch (failure) {
    if (!current.signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}, { immediate: true })

onBeforeUnmount(() => request?.abort())
</script>

<template>
  <main class="search-page">
    <header>
      <p class="eyebrow">SITE SEARCH · 全站检索</p>
      <h1>搜索</h1>
      <p>在已公开的教程、博客、作品与作者资料中查找内容。</p>
    </header>
    <PublicFilterBar label="搜索筛选" :active="Boolean(route.query.q || activeType)" @reset="q = ''; navigate({ q: undefined, type: undefined, page: undefined })">
      <PublicSearch v-model="q" label="搜索内容" placeholder="输入至少两个字符" @search="navigate({ page: undefined })" />
      <template #tabs><PublicFilterTabs label="内容类型" :model-value="activeType" :options="types" @update:model-value="navigate({ type: $event, page: undefined })" /></template>
    </PublicFilterBar>
    <p v-if="error" class="state error" role="alert">{{ error }}</p>
    <p v-else-if="loading" class="state" role="status">正在搜索…</p>
    <p v-else-if="!String(route.query.q || '').trim()" class="state">输入关键词开始搜索。</p>
    <p v-else-if="String(route.query.q || '').trim().length < 2" class="state">请输入至少两个字符。</p>
    <template v-else>
      <p class="count">找到 {{ result.total }} 条结果</p>
      <p v-if="!result.items.length" class="state">没有找到相关的公开内容。可以试试其他关键词或类型。</p>
      <ul v-else class="results">
        <li v-for="item in result.items" :key="item.contentType + ':' + item.contentId">
          <span class="type">{{ typeLabel[item.contentType] || item.contentType }}</span>
          <h2><RouterLink :to="contentPath(item.routePath)">{{ item.title }}</RouterLink></h2>
          <p v-if="item.summary">{{ item.summary }}</p>
        </li>
      </ul>
      <PublicPagination :page="page" :page-size="pageSize" :page-sizes="[10, 20, 50]" :total="result.total" :loading="loading" label="搜索分页" unit="条" @change="navigate({ page: $event })" @page-size="changeSize" />
    </template>
  </main>
</template>

<style scoped>
.search-page > .public-filters { margin-top: 28px; }
.search-page { max-width: 940px; margin: 0 auto; padding: clamp(32px, 5vw, 72px) 24px 100px; color: var(--text-primary); }
.eyebrow { color: var(--accent, var(--primary)); font-size: 11px; font-weight: 700; letter-spacing: .15em; }
h1 { margin: 8px 0 12px; font-size: clamp(34px, 5vw, 54px); }
header > p:last-child { color: var(--text-secondary); }
.search-form { display: flex; gap: 10px; margin-top: 32px; }
.search-form input { flex: 1; min-width: 0; min-height: 50px; padding: 0 16px; border: 1px solid var(--border-strong); border-radius: var(--radius-md); background: var(--bg-surface); color: var(--text-primary); font: inherit; }
.search-form button, .pagination button { padding: 0 20px; border: 1px solid var(--primary); border-radius: var(--radius-md); background: var(--primary); color: white; cursor: pointer; }
.filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0; }
.filters button { padding: 8px 14px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-surface); color: var(--text-secondary); cursor: pointer; }
.filters button.active { border-color: var(--primary); color: var(--primary); font-weight: 700; }
.state { padding: 36px 0; color: var(--text-secondary); }
.state.error { color: var(--danger, #a43d31); }
.count { color: var(--text-muted); font-size: 13px; }
.results { margin: 16px 0 28px; padding: 0; list-style: none; }
.results li { padding: 22px 0; border-bottom: 1px solid var(--border); }
.type { color: var(--primary); font-size: 12px; font-weight: 700; }
.results h2 { margin: 8px 0; font-size: 22px; }
.results a { color: inherit; text-decoration: none; }
.results a:hover { color: var(--primary); }
.results li p { margin: 0; color: var(--text-secondary); line-height: 1.7; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 18px; }
.pagination button { min-height: 38px; }
.pagination button:disabled { opacity: .4; cursor: default; }
</style>
