<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPublicReadings, fetchPublicReadingHome, type ReadingPage } from '@/api/reading'
import { fetchPublicMeta, type CefrLevel, type TaxonomyTerm } from '@/api/englishMeta'
import CefrBadge from '@/components/english/CefrBadge.vue'

const route = useRoute()
const router = useRouter()

const page = ref<ReadingPage | null>(null)
const home = ref<{ total: number; byLevel: Record<number, number>; byCefr: Record<string, number> } | null>(null)
const topics = ref<TaxonomyTerm[]>([])
const genres = ref<TaxonomyTerm[]>([])
const cefrList = ref<CefrLevel[]>([])
const loading = ref(true)
const error = ref(false)

const levelLabels: Record<number, string> = { 1: '基础阅读', 2: '结构阅读', 3: '深度阅读' }

const filters = reactive({ q: '', level: '', cefr: '', topic: '', genre: '' })

function syncFromRoute() {
  filters.q = String(route.query.q ?? '')
  filters.level = String(route.query.level ?? '')
  filters.cefr = String(route.query.cefr ?? '')
  filters.topic = String(route.query.topic ?? '')
  filters.genre = String(route.query.genre ?? '')
}

async function load() {
  loading.value = true
  error.value = false
  try {
    page.value = await fetchPublicReadings({
      page: Number(route.query.page ?? 1), pageSize: 20,
      q: filters.q || undefined, level: filters.level ? Number(filters.level) : undefined,
      cefr: filters.cefr || undefined, topic: filters.topic ? Number(filters.topic) : undefined,
      genre: filters.genre ? Number(filters.genre) : undefined,
    })
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  router.push({ query: { q: filters.q || undefined, level: filters.level || undefined, cefr: filters.cefr || undefined, topic: filters.topic || undefined, genre: filters.genre || undefined } })
}

function goPage(p: number) {
  router.push({ query: { ...route.query, page: String(p) } })
}

watch(() => route.query, () => { syncFromRoute(); void load() })

onMounted(async () => {
  syncFromRoute()
  try {
    const meta = await fetchPublicMeta()
    topics.value = meta.taxonomy.filter((t) => t.dimension === 'TOPIC' && t.parentId === null)
    genres.value = meta.taxonomy.filter((t) => t.dimension === 'GENRE' && t.parentId === null)
    cefrList.value = meta.cefr
    home.value = await fetchPublicReadingHome()
  } catch {
    // meta failure shouldn't block the list
  }
  await load()
})
</script>

<template>
  <section class="reading-center">
    <header class="reading-center__hero">
      <div>
        <p>ENGLISH READING · 分级精读</p>
        <h1>阅读中心</h1>
        <span>能力×主题×文体×CEFR 探索文章，正文统计由系统精确生成。</span>
      </div>
      <div v-if="home" class="reading-center__hero-stat"><b>{{ home.total }}</b><span>篇精读文章</span></div>
    </header>

    <div v-if="home" class="reading-center__route">
      <RouterLink v-for="(count, level) in home.byLevel" :key="level" :to="{ query: { level } }" class="route-pill">
        <b>{{ count }}</b><span>{{ levelLabels[Number(level)] }}</span>
      </RouterLink>
    </div>

    <div class="reading-center__filters">
      <el-input v-model="filters.q" placeholder="搜索标题/摘要" clearable style="width: 200px" @keyup.enter="applyFilter" @clear="applyFilter" />
      <el-select v-model="filters.genre" placeholder="文体" clearable style="width: 130px" @change="applyFilter">
        <el-option v-for="g in genres" :key="g.id" :label="g.name" :value="String(g.id)" />
      </el-select>
      <el-select v-model="filters.cefr" placeholder="CEFR" clearable style="width: 110px" @change="applyFilter">
        <el-option v-for="c in cefrList" :key="c.level" :label="c.level" :value="c.level" />
      </el-select>
      <el-select v-model="filters.topic" placeholder="主题" clearable style="width: 130px" @change="applyFilter">
        <el-option v-for="t in topics" :key="t.id" :label="t.name" :value="String(t.id)" />
      </el-select>
    </div>

    <div v-if="error" class="reading-center__empty">加载失败，请稍后重试。</div>
    <div v-else v-loading="loading" class="reading-center__grid">
      <p v-if="!loading && !page?.items.length" class="reading-center__empty">暂无文章。</p>
      <RouterLink v-for="article in page?.items" :key="article.id" :to="`/english/reading/${article.slug}`" class="article-card">
        <div class="article-card__cover">
          <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" loading="lazy" />
          <span v-else class="article-card__cover-fallback"><i>{{ levelLabels[article.readingLevel]?.[0] ?? '读' }}</i><em>{{ article.cefrLevel }}</em></span>
        </div>
        <div class="article-card__body">
          <div class="article-card__meta"><CefrBadge :level="article.cefrLevel" /><span class="article-card__level">{{ levelLabels[article.readingLevel] }}</span></div>
          <h2 class="article-card__title">{{ article.title }}</h2>
          <p class="article-card__summary">{{ article.summary }}</p>
          <div class="article-card__tags"><span v-for="t in article.tags" :key="t.id" class="article-card__tag">{{ t.name }}</span></div>
          <span class="article-card__cta">{{ article.wordCount }} 词 · {{ article.estimatedMinutes }} 分钟 · 开始阅读 →</span>
        </div>
      </RouterLink>
    </div>

    <el-pagination v-if="page && page.total > 0" :current-page="Number(route.query.page ?? 1)" :total="page.total" :page-size="20" layout="prev, pager, next" style="margin: 24px 0" @current-change="goPage" />
  </section>
</template>

<style scoped>
.reading-center__hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; }
.reading-center__hero p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .14em; margin: 0; }
.reading-center__hero h1 { font-size: 34px; margin: 7px 0; }
.reading-center__hero span { color: var(--text-secondary); font-size: 14px; }
.reading-center__hero-stat { text-align: center; padding: 14px 22px; border: 1px solid var(--border); border-radius: 16px; background: var(--bg-surface); }
.reading-center__hero-stat b { display: block; font-size: 30px; color: var(--primary); }
.reading-center__hero-stat span { font-size: 12px; color: var(--text-muted); }
.reading-center__route { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
.route-pill { display: flex; align-items: center; gap: 8px; padding: 8px 14px; border: 1px solid var(--border); border-radius: 999px; color: var(--text-secondary); transition: all .15s ease; }
.route-pill:hover { border-color: var(--primary); color: var(--primary); }
.route-pill b { color: var(--primary); }
.reading-center__filters { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-bottom: 20px; }
.reading-center__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: var(--space-4); min-height: 100px; }
.reading-center__empty { color: var(--text-muted); padding: var(--space-6) 0; grid-column: 1 / -1; }
.article-card { display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 18px; overflow: hidden; background: var(--bg-surface); color: inherit; transition: transform .16s ease, border-color .16s ease; }
.article-card:hover { transform: translateY(-3px); border-color: var(--primary); }
.article-card__cover { height: 140px; background: var(--bg-subtle); position: relative; }
.article-card__cover img { width: 100%; height: 100%; object-fit: cover; }
.article-card__cover-fallback { display: grid; place-items: center; height: 100%; background: linear-gradient(135deg, color-mix(in srgb, var(--primary) 18%, var(--bg-surface)), var(--bg-surface)); }
.article-card__cover-fallback i { font-size: 42px; font-style: normal; font-weight: 800; color: var(--primary); }
.article-card__cover-fallback em { position: absolute; bottom: 10px; right: 12px; font-style: normal; font-size: 11px; color: var(--text-secondary); }
.article-card__body { padding: 16px; }
.article-card__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.article-card__level { font-size: 12px; color: var(--text-secondary); }
.article-card__title { font-size: 18px; margin: 0 0 6px; }
.article-card__summary { font-size: 13px; color: var(--text-secondary); margin: 0 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.article-card__tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 10px; }
.article-card__tag { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--bg-subtle); border: 1px solid var(--border); color: var(--text-secondary); }
.article-card__cta { font-size: 13px; color: var(--primary); }
</style>
