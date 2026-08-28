<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { deleteReading, fetchReadings, publishReading, withdrawReading, type ReadingArticleSummary, type ReadingPage } from '@/api/reading'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import CefrBadge from '@/components/english/CefrBadge.vue'
import AdminContentActions from '@/components/admin/AdminContentActions.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const page = ref<ReadingPage | null>(null)
const loading = ref(true)
const taxonomy = ref<TaxonomyTerm[]>([])

const filters = reactive({
  page: 1,
  pageSize: 20,
  q: '',
  status: '',
  level: '' as string,
  cefr: '',
  topic: '',
  genre: '',
})

const levelLabels: Record<number, string> = { 1: '基础阅读', 2: '结构阅读', 3: '深度阅读' }
const tagName = (id: number): string => {
  const term = taxonomy.value.find((t) => t.id === id)
  return term ? term.name : ''
}

async function load() {
  loading.value = true
  try {
    page.value = await fetchReadings({
      page: filters.page,
      pageSize: filters.pageSize,
      q: filters.q || undefined,
      status: filters.status || undefined,
      level: filters.level ? Number(filters.level) : undefined,
      cefr: filters.cefr || undefined,
      topic: filters.topic ? Number(filters.topic) : undefined,
      genre: filters.genre ? Number(filters.genre) : undefined,
    })
  } catch {
    ElMessage.error('加载阅读文章失败。')
  } finally {
    loading.value = false
  }
}

function syncFromRoute() {
  filters.page = Math.max(1, Number(route.query.page ?? 1))
  filters.pageSize = Number(route.query.pageSize ?? 20) === 50 ? 50 : 20
  filters.q = String(route.query.q ?? '')
  filters.status = String(route.query.status ?? '')
  filters.level = String(route.query.level ?? '')
  filters.cefr = String(route.query.cefr ?? '')
  filters.topic = String(route.query.topic ?? '')
  filters.genre = String(route.query.genre ?? '')
}

function writeFilters(page = 1) {
  void router.push({
    query: {
      page: page > 1 ? String(page) : undefined,
      pageSize: filters.pageSize !== 20 ? String(filters.pageSize) : undefined,
      q: filters.q || undefined,
      status: filters.status || undefined,
      level: filters.level || undefined,
      cefr: filters.cefr || undefined,
      topic: filters.topic || undefined,
      genre: filters.genre || undefined,
    },
  })
}

function search() { filters.page = 1; writeFilters(1) }

const topics = () => taxonomy.value.filter((t) => t.dimension === 'TOPIC' && t.parentId === null)
const genres = () => taxonomy.value.filter((t) => t.dimension === 'GENRE' && t.parentId === null)

async function setPublished(article: ReadingArticleSummary, published: boolean) {
  try {
    if (published) {
      await publishReading(article.id)
      ElMessage.success('已发布。')
    } else {
      await withdrawReading(article.id)
      ElMessage.success('已撤回。')
    }
    await load()
  } catch (error) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '操作失败。')
  }
}

async function remove(article: ReadingArticleSummary) {
  try {
    await ElMessageBox.confirm(`确定删除「${article.title}」？`, '删除确认', { type: 'warning' })
    await deleteReading(article.id)
    ElMessage.success('已删除。')
    await load()
  } catch (error) {
    const detail = (error as { response?: { data?: ProblemDetail } }).response?.data?.detail
    if (detail) ElMessage.error(detail)
  }
}

function duplicate(article: ReadingArticleSummary) {
  router.push({ name: 'admin-reading-new', query: { ...route.query, clone: article.id } })
}

function openEditor(articleId?: number) {
  router.push({
    name: articleId ? 'admin-reading-edit' : 'admin-reading-new',
    params: articleId ? { articleId } : undefined,
    query: { ...route.query },
  })
}

function preview(article: ReadingArticleSummary) {
  router.push({ name: 'admin-reading-preview', params: { articleId: article.id }, query: { ...route.query } })
}

onMounted(async () => {
  syncFromRoute()
  try {
    taxonomy.value = await fetchTaxonomy('tree')
  } catch {
    // meta load failure shouldn't block list; filters degrade gracefully
  }
  await load()
})

watch(() => route.query, () => { syncFromRoute(); void load() })
</script>

<template>
  <section class="reading-manage">
    <header class="reading-manage__hero">
      <div>
        <p>ENGLISH READING · 分级精读</p>
        <h1>阅读管理</h1>
        <span>能力×主题×文体×CEFR 四维组织文章，后端精确统计并约束发布。</span>
      </div>
      <el-button type="primary" @click="openEditor()">新建文章</el-button>
    </header>

    <div v-if="page?.stats" class="reading-manage__stats">
      <div class="reading-stat"><b>{{ page.stats.total }}</b><span>总文章</span></div>
      <div class="reading-stat"><b>{{ page.stats.published }}</b><span>已发布</span></div>
      <div class="reading-stat"><b>{{ page.stats.draft }}</b><span>草稿</span></div>
      <div class="reading-stat"><b>{{ page.stats.withdrawn }}</b><span>已撤回</span></div>
      <div class="reading-stat"><b>{{ page.stats.missingExercise }}</b><span>缺练习</span></div>
      <div class="reading-stat reading-stat--cefr">
        <span>CEFR</span>
        <em v-for="(count, level) in page.stats.byCefr" :key="level">{{ level }}·{{ count }}</em>
      </div>
    </div>

    <div class="reading-manage__filters">
      <el-input v-model="filters.q" placeholder="搜索标题/摘要" clearable style="width: 220px" @keyup.enter="search" @clear="search" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /><el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
      <el-select v-model="filters.level" placeholder="能力层级" clearable style="width: 140px" @change="search">
        <el-option v-for="(label, lv) in levelLabels" :key="lv" :label="label" :value="String(lv)" />
      </el-select>
      <el-select v-model="filters.cefr" placeholder="CEFR" clearable style="width: 120px" @change="search">
        <el-option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :label="lv" :value="lv" />
      </el-select>
      <el-select v-model="filters.topic" placeholder="主题" clearable style="width: 140px" @change="search">
        <el-option v-for="t in topics()" :key="t.id" :label="t.name" :value="String(t.id)" />
      </el-select>
      <el-select v-model="filters.genre" placeholder="文体" clearable style="width: 140px" @change="search">
        <el-option v-for="g in genres()" :key="g.id" :label="g.name" :value="String(g.id)" />
      </el-select>
      <el-button @click="search">搜索</el-button>
    </div>

    <div v-loading="loading" class="reading-manage__grid">
      <p v-if="!loading && !page?.items.length" class="reading-manage__empty">暂无文章。</p>
      <article v-for="article in page?.items" :key="article.id" class="reading-card">
        <div class="reading-card__cover">
          <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" loading="lazy" />
          <span v-else class="reading-card__cover-fallback">{{ levelLabels[article.readingLevel]?.[0] ?? '读' }}</span>
        </div>
        <div class="reading-card__body">
          <div class="reading-card__meta">
            <CefrBadge :level="article.cefrLevel" />
            <span class="reading-card__level">{{ levelLabels[article.readingLevel] }}</span>
            <span class="reading-card__status" :class="`is-${article.publishStatus.toLowerCase()}`">{{ article.publishStatus === 'PUBLISHED' ? '已发布' : article.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</span>
          </div>
          <h2 class="reading-card__title">{{ article.title }}</h2>
          <p class="reading-card__summary">{{ article.summary }}</p>
          <div class="reading-card__tags">
            <span v-for="t in article.tags" :key="t.id" class="reading-card__tag">{{ t.name }}</span>
          </div>
          <p class="reading-card__metrics">{{ article.wordCount }} 词 · {{ article.estimatedMinutes }} 分钟 · {{ article.hasExercises ? '有练习' : '缺少练习' }}</p>
          <AdminContentActions :permission-note="auth.isSuperAdmin ? '' : '发布和删除由超级管理员操作'">
            <el-button type="primary" plain @click="openEditor(article.id)">编辑</el-button><el-button @click="preview(article)">预览</el-button><el-button @click="router.push({ name: 'admin-reading-exercises', params: { articleId: article.id }, query: { ...route.query } })">练习</el-button>
            <el-button v-if="auth.isSuperAdmin && article.publishStatus !== 'PUBLISHED'" type="success" plain @click="setPublished(article, true)">{{ article.publishStatus === 'WITHDRAWN' ? '重新发布' : '发布' }}</el-button><el-button v-else-if="auth.isSuperAdmin" type="warning" plain @click="setPublished(article, false)">撤回</el-button>
            <template #more><el-dropdown-item @click="duplicate(article)">复制</el-dropdown-item><el-dropdown-item v-if="auth.isSuperAdmin" class="is-danger" @click="remove(article)">删除</el-dropdown-item></template>
          </AdminContentActions>
        </div>
      </article>
    </div>

    <el-pagination
      v-if="page && page.total > 0"
      v-model:current-page="filters.page"
      v-model:page-size="filters.pageSize"
      :total="page.total"
      :page-sizes="[20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: var(--space-6)"
      @change="writeFilters(filters.page)"
    />
  </section>
</template>

<style scoped>
.reading-manage__hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.reading-manage__hero p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .14em; margin: 0; }
.reading-manage__hero h1 { font-size: 28px; margin: 6px 0; }
.reading-manage__hero span { color: var(--text-secondary); font-size: 13px; }
.reading-manage__stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: var(--space-3); margin-bottom: 20px; }
.reading-stat { padding: 14px 16px; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-surface); display: flex; flex-direction: column; gap: 2px; }
.reading-stat b { font-size: 24px; color: var(--primary); }
.reading-stat span { font-size: 12px; color: var(--text-muted); }
.reading-stat--cefr em { font-size: 11px; color: var(--text-secondary); font-style: normal; }
.reading-manage__filters { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-bottom: 20px; }
.reading-manage__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--space-4); min-height: 100px; }
.reading-manage__empty { color: var(--text-muted); padding: var(--space-6) 0; grid-column: 1 / -1; }
.reading-card { display: flex; border: 1px solid var(--border); border-radius: 18px; overflow: hidden; background: var(--bg-surface); }
.reading-card__cover { width: 120px; flex-shrink: 0; background: var(--bg-subtle); display: grid; place-items: center; }
.reading-card__cover img { width: 100%; height: 100%; object-fit: cover; }
.reading-card__cover-fallback { font-size: 32px; font-weight: 800; color: var(--primary); }
.reading-card__body { padding: 16px; flex: 1; min-width: 0; }
.reading-card__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.reading-card__level { font-size: 12px; color: var(--text-secondary); }
.reading-card__status { font-size: 11px; color: var(--text-muted); }
.reading-card__status.is-published { color: var(--primary); }
.reading-card__status.is-draft { color: var(--text-secondary); }
.reading-card__status.is-withdrawn { color: var(--danger); }
.reading-card__title { font-size: 17px; margin: 0 0 6px; }
.reading-card__summary { font-size: 13px; color: var(--text-secondary); margin: 0 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.reading-card__tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.reading-card__tag { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--bg-subtle); border: 1px solid var(--border); color: var(--text-secondary); }
.reading-card__metrics { font-size: 12px; color: var(--text-muted); margin: 0 0 8px; }
@media (max-width: 720px) {
  .reading-manage__hero { align-items: flex-start; gap: 12px; }
  .reading-manage__hero span { display: none; }
  .reading-manage__stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .reading-manage__filters > * { width: 100% !important; }
  .reading-manage__grid { grid-template-columns: minmax(0, 1fr); }
  .reading-card { flex-direction: column; }
  .reading-card__cover { width: 100%; height: 120px; }
}
</style>
