<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { deleteListening, fetchListenings, publishListening, withdrawListening, type ListeningPage, type ListeningSummary } from '@/api/listening'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import CefrBadge from '@/components/english/CefrBadge.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const page = ref<ListeningPage | null>(null)
const loading = ref(true)
const taxonomy = ref<TaxonomyTerm[]>([])
const filters = reactive({ page: 1, pageSize: 20, q: '', status: '', level: '', cefr: '', topic: '', scene: '', format: '' })
const levelLabels: Record<number, string> = { 1: '语音识别', 2: '信息捕获', 3: '逻辑理解' }
const formatTime = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`

async function load() {
  loading.value = true
  try {
    page.value = await fetchListenings({
      page: filters.page, pageSize: filters.pageSize, q: filters.q || undefined, status: filters.status || undefined,
      level: filters.level ? Number(filters.level) : undefined, cefr: filters.cefr || undefined,
      topic: filters.topic ? Number(filters.topic) : undefined, scene: filters.scene ? Number(filters.scene) : undefined,
      format: filters.format ? Number(filters.format) : undefined,
    })
  } catch { ElMessage.error('加载听力材料失败。') } finally { loading.value = false }
}
function syncFromRoute() {
  filters.page = Math.max(1, Number(route.query.page ?? 1))
  filters.pageSize = Number(route.query.pageSize ?? 20) === 50 ? 50 : 20
  filters.q = String(route.query.q ?? '')
  filters.status = String(route.query.status ?? '')
  filters.level = String(route.query.level ?? '')
  filters.cefr = String(route.query.cefr ?? '')
  filters.topic = String(route.query.topic ?? '')
  filters.scene = String(route.query.scene ?? '')
  filters.format = String(route.query.format ?? '')
}
function writeFilters(pageNumber = 1) {
  void router.push({ query: {
    page: pageNumber > 1 ? String(pageNumber) : undefined,
    pageSize: filters.pageSize !== 20 ? String(filters.pageSize) : undefined,
    q: filters.q || undefined, status: filters.status || undefined,
    level: filters.level || undefined, cefr: filters.cefr || undefined,
    topic: filters.topic || undefined, scene: filters.scene || undefined,
    format: filters.format || undefined,
  } })
}
function search() { filters.page = 1; writeFilters(1) }
const tags = (dim: string) => taxonomy.value.filter((t) => t.dimension === dim && t.parentId === null)

async function setPublished(a: ListeningSummary, p: boolean) {
  try { p ? await publishListening(a.id) : await withdrawListening(a.id); ElMessage.success(p ? '已发布。' : '已撤回。'); await load() }
  catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '操作失败。') }
}
async function remove(a: ListeningSummary) {
  try {
    await ElMessageBox.confirm(`确定删除「${a.title}」？`, '删除确认', { type: 'warning' })
    await deleteListening(a.id); ElMessage.success('已删除。'); await load()
  } catch (e) { const d = (e as { response?: { data?: ProblemDetail } }).response?.data?.detail; if (d) ElMessage.error(d) }
}
function listQuery() { return { ...route.query } }
function openEditor(id?: number) {
  router.push({ name: id ? 'admin-listening-edit' : 'admin-listening-new', params: id ? { id } : undefined, query: listQuery() })
}
function openExercises(id: number) { router.push({ name: 'admin-listening-exercises', params: { id }, query: listQuery() }) }
function dup(a: ListeningSummary) { router.push({ name: 'admin-listening-new', query: { ...route.query, clone: a.id } }) }
function preview(a: ListeningSummary) { router.push(`/english/listening/${a.slug}`) }

onMounted(async () => {
  syncFromRoute()
  try { taxonomy.value = await fetchTaxonomy('tree') } catch { /* filters degrade */ }
  await load()
})
watch(() => route.query, () => { syncFromRoute(); void load() })
</script>

<template>
  <section class="listening-manage">
    <header class="listening-manage__hero">
      <div><p>ENGLISH LISTENING · 场景×形式×能力</p><h1>听力管理</h1><span>三段能力路线组织音频材料，逐句时间片段与安全练习。</span></div>
      <el-button type="primary" @click="openEditor()">新建材料</el-button>
    </header>

    <div v-if="page?.stats" class="listening-manage__stats">
      <div class="lis-stat"><b>{{ page.stats.total }}</b><span>总材料</span></div>
      <div class="lis-stat"><b>{{ page.stats.published }}</b><span>已发布</span></div>
      <div class="lis-stat"><b>{{ page.stats.draft }}</b><span>草稿</span></div>
      <div class="lis-stat"><b>{{ page.stats.withdrawn }}</b><span>已撤回</span></div>
      <div class="lis-stat"><b>{{ page.stats.missingAudio }}</b><span>缺音频</span></div>
    </div>

    <div class="listening-manage__filters">
      <el-input v-model="filters.q" placeholder="搜索标题/摘要" clearable style="width: 190px" @keyup.enter="search" @clear="search" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 110px" @change="search">
        <el-option label="草稿" value="DRAFT"/><el-option label="已发布" value="PUBLISHED"/><el-option label="已撤回" value="WITHDRAWN"/>
      </el-select>
      <el-select v-model="filters.level" placeholder="能力层级" clearable style="width: 120px" @change="search">
        <el-option v-for="(l, k) in levelLabels" :key="k" :label="l" :value="String(k)"/>
      </el-select>
      <el-select v-model="filters.cefr" placeholder="CEFR" clearable style="width: 100px" @change="search">
        <el-option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :label="lv" :value="lv"/>
      </el-select>
      <el-select v-model="filters.topic" placeholder="主题" clearable style="width: 120px" @change="search">
        <el-option v-for="t in tags('TOPIC')" :key="t.id" :label="t.name" :value="String(t.id)"/>
      </el-select>
      <el-select v-model="filters.scene" placeholder="场景" clearable style="width: 120px" @change="search">
        <el-option v-for="s in tags('SCENE')" :key="s.id" :label="s.name" :value="String(s.id)"/>
      </el-select>
      <el-select v-model="filters.format" placeholder="形式" clearable style="width: 120px" @change="search">
        <el-option v-for="f in tags('FORMAT')" :key="f.id" :label="f.name" :value="String(f.id)"/>
      </el-select>
      <el-button @click="search">搜索</el-button>
    </div>

    <div v-loading="loading" class="listening-manage__grid">
      <p v-if="!loading && !page?.items.length" class="listening-manage__empty">暂无材料。</p>
      <article v-for="item in page?.items" :key="item.id" class="listen-card">
        <div class="listen-card__cover">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy"/>
          <span v-else class="listen-card__fallback">听</span>
        </div>
        <div class="listen-card__body">
          <div class="listen-card__meta"><CefrBadge :level="item.cefrLevel"/><span class="listen-card__level">{{ levelLabels[item.listeningLevel] }}</span><span class="listen-card__status">{{ item.publishStatus }}</span></div>
          <h2 class="listen-card__title">{{ item.title }}</h2>
          <p class="listen-card__summary">{{ item.summary }}</p>
          <div class="listen-card__tags"><span v-for="t in item.tags" :key="t.id" class="listen-card__tag">{{ t.name }}</span></div>
          <p class="listen-card__metrics">{{ formatTime(item.durationSeconds) }} · {{ item.exerciseCount }} 练习 · {{ item.segmentCount }} 片段</p>
          <div class="listen-card__actions">
            <el-button link type="primary" @click="openEditor(item.id)">编辑</el-button>
            <el-button link @click="openExercises(item.id)">练习</el-button>
            <el-button v-if="item.publishStatus === 'PUBLISHED'" link @click="preview(item)">预览</el-button>
            <el-button v-if="auth.isSuperAdmin && item.publishStatus !== 'PUBLISHED'" link type="success" @click="setPublished(item, true)">发布</el-button>
            <el-button v-else-if="auth.isSuperAdmin" link type="warning" @click="setPublished(item, false)">撤回</el-button>
            <el-button link @click="dup(item)">复制</el-button>
            <el-button v-if="auth.isSuperAdmin" link type="danger" @click="remove(item)">删除</el-button>
          </div>
        </div>
      </article>
    </div>

    <el-pagination v-if="page && page.total > 0" v-model:current-page="filters.page" v-model:page-size="filters.pageSize" :total="page.total" :page-sizes="[20,50]" layout="total, sizes, prev, pager, next" style="margin-top: var(--space-6)" @change="writeFilters(filters.page)"/>
  </section>
</template>

<style scoped>
.listening-manage__hero{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:24px}
.listening-manage__hero p{color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.14em;margin:0}
.listening-manage__hero h1{font-size:28px;margin:6px 0}.listening-manage__hero span{color:var(--text-secondary);font-size:13px}
.listening-manage__stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(110px,1fr));gap:var(--space-3);margin-bottom:20px}
.lis-stat{padding:12px 14px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);display:flex;flex-direction:column;gap:2px}
.lis-stat b{font-size:22px;color:var(--primary)}.lis-stat span{font-size:12px;color:var(--text-muted)}
.listening-manage__filters{display:flex;flex-wrap:wrap;gap:var(--space-3);margin-bottom:20px}
.listening-manage__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:var(--space-4);min-height:100px}
.listening-manage__empty{color:var(--text-muted);padding:var(--space-6) 0;grid-column:1/-1}
.listen-card{display:flex;border:1px solid var(--border);border-radius:18px;overflow:hidden;background:var(--bg-surface)}
.listen-card__cover{width:110px;flex-shrink:0;background:var(--bg-subtle);display:grid;place-items:center}
.listen-card__cover img{width:100%;height:100%;object-fit:cover}.listen-card__fallback{font-size:32px;font-weight:800;color:var(--primary)}
.listen-card__body{padding:16px;flex:1;min-width:0}
.listen-card__meta{display:flex;align-items:center;gap:8px;margin-bottom:8px}.listen-card__level{font-size:12px;color:var(--text-secondary)}
.listen-card__status{font-size:11px;color:var(--text-muted)}.listen-card__title{font-size:17px;margin:0 0 6px}
.listen-card__summary{font-size:13px;color:var(--text-secondary);margin:0 0 8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.listen-card__tags{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:8px}.listen-card__tag{font-size:11px;padding:2px 8px;border-radius:999px;background:var(--bg-subtle);border:1px solid var(--border);color:var(--text-secondary)}
.listen-card__metrics{font-size:12px;color:var(--text-muted);margin:0 0 8px}.listen-card__actions{display:flex;flex-wrap:wrap;gap:2px}
@media(max-width:720px){
  .listening-manage__hero{align-items:flex-start;gap:16px}.listening-manage__hero span{display:block}
  .listening-manage__filters>*{width:100%!important}.listening-manage__grid{grid-template-columns:1fr}
  .listen-card{flex-direction:column}.listen-card__cover{width:100%;height:140px}
}
</style>
