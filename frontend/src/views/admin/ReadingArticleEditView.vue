<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { createReading, fetchReading, updateReading, type ReadingArticle } from '@/api/reading'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import SemanticTagPicker from '@/components/english/SemanticTagPicker.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import PublishChecklistDrawer, { type PublishCheck } from '@/components/english/PublishChecklistDrawer.vue'

const route = useRoute()
const router = useRouter()
const articleId = computed(() => Number(route.params.articleId ?? 0))
const cloneFrom = Number(route.query.clone ?? 0)

const taxonomy = ref<TaxonomyTerm[]>([])
const loading = ref(true)
const saving = ref(false)
const mediaOpen = ref(false)
const checkOpen = ref(false)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  bodyMarkdown: '',
  coverMediaId: null as number | null,
  readingLevel: 1,
  cefrLevel: 'B1',
  sourceName: '',
  sourceUrl: '',
  copyrightNote: '',
  sortOrder: 10,
  topicTagIds: [] as number[],
  genreTagIds: [] as number[],
  abilityTagIds: [] as number[],
  grammarLessonIds: [] as number[],
})

const checks = computed<PublishCheck[]>(() => [
  { key: 'title', label: '标题完整', passed: !!form.title.trim() },
  { key: 'slug', label: 'slug 稳定', passed: !!form.slug.trim() },
  { key: 'summary', label: '摘要完整', passed: !!form.summary.trim() },
  { key: 'body', label: '正文完整', passed: !!form.bodyMarkdown.trim() },
  { key: 'level', label: '能力层级合法', passed: [1, 2, 3].includes(form.readingLevel) },
  { key: 'cefr', label: 'CEFR 已选', passed: !!form.cefrLevel },
  { key: 'topic', label: '至少一个主题标签', passed: form.topicTagIds.length > 0 },
  { key: 'genre', label: '至少一个文体标签', passed: form.genreTagIds.length > 0 },
])

const topics = () => taxonomy.value.filter((t) => t.dimension === 'TOPIC' && t.parentId === null)
const genres = () => taxonomy.value.filter((t) => t.dimension === 'GENRE' && t.parentId === null)
const abilities = () => taxonomy.value.filter((t) => t.dimension === 'ABILITY' && t.parentId === null)

async function load() {
  loading.value = true
  try {
    taxonomy.value = await fetchTaxonomy('tree')
    if (articleId.value) {
      const article: ReadingArticle = cloneFrom ? { ...(await fetchReading(cloneFrom)), id: articleId.value } : await fetchReading(articleId.value)
      if (cloneFrom) {
        // clone: keep source fields, new slug hint
        Object.assign(form, {
          title: article.title, summary: article.summary, bodyMarkdown: article.bodyMarkdown,
          coverMediaId: article.coverMediaId, readingLevel: article.readingLevel, cefrLevel: article.cefrLevel,
          sourceName: article.sourceName, sourceUrl: article.sourceUrl, copyrightNote: article.copyrightNote,
          sortOrder: article.sortOrder, topicTagIds: article.tags.filter(t=>t.dimension==='TOPIC').map(t=>t.id),
          genreTagIds: article.tags.filter(t=>t.dimension==='GENRE').map(t=>t.id),
          abilityTagIds: article.tags.filter(t=>t.dimension==='ABILITY').map(t=>t.id),
          grammarLessonIds: article.grammarLessons.map(g=>g.id),
        })
        form.slug = `${article.slug}-copy`
      } else {
        Object.assign(form, {
          title: article.title, slug: article.slug, summary: article.summary, bodyMarkdown: article.bodyMarkdown,
          coverMediaId: article.coverMediaId, readingLevel: article.readingLevel, cefrLevel: article.cefrLevel,
          sourceName: article.sourceName, sourceUrl: article.sourceUrl, copyrightNote: article.copyrightNote,
          sortOrder: article.sortOrder, topicTagIds: article.tags.filter(t=>t.dimension==='TOPIC').map(t=>t.id),
          genreTagIds: article.tags.filter(t=>t.dimension==='GENRE').map(t=>t.id),
          abilityTagIds: article.tags.filter(t=>t.dimension==='ABILITY').map(t=>t.id),
          grammarLessonIds: article.grammarLessons.map(g=>g.id),
        })
      }
    }
  } catch {
    ElMessage.error('加载文章失败。')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写标题与 slug。')
    return
  }
  saving.value = true
  const payload = {
    title: form.title.trim(), slug: form.slug.trim(), summary: form.summary.trim(),
    bodyMarkdown: form.bodyMarkdown, coverMediaId: form.coverMediaId, readingLevel: form.readingLevel,
    cefrLevel: form.cefrLevel, sourceName: form.sourceName || null, sourceUrl: form.sourceUrl || null,
    copyrightNote: form.copyrightNote || null, sortOrder: form.sortOrder,
    topicTagIds: form.topicTagIds, genreTagIds: form.genreTagIds, abilityTagIds: form.abilityTagIds,
    grammarLessonIds: form.grammarLessonIds,
  }
  try {
    if (articleId.value) {
      await updateReading(articleId.value, payload)
      ElMessage.success('已更新。')
    } else {
      const created = await createReading(payload)
      router.replace({ name: 'admin-reading-edit', params: { articleId: created.id } })
      ElMessage.success('已创建。')
    }
    await router.push({ name: 'admin-reading' })
  } catch (error) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

function onCoverSelect(asset: { id: number }) {
  form.coverMediaId = asset.id
  mediaOpen.value = false
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="reading-edit">
    <header class="reading-edit__bar">
      <div class="reading-edit__title">
        <CefrBadge :level="form.cefrLevel" />
        <h1>{{ articleId ? '编辑文章' : '新建文章' }}</h1>
        <span>页面唯一 H1 为文章标题，正文目录收录 H2–H4。</span>
      </div>
      <div class="reading-edit__actions">
        <el-button @click="checkOpen = true">发布检查</el-button>
        <el-button @click="router.push({ name: 'admin-reading' })">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </header>

    <div class="reading-edit__layout">
      <div class="reading-edit__main">
        <div class="reading-edit__field"><label>标题</label><el-input v-model="form.title" /></div>
        <div class="reading-edit__field"><label>Slug</label><el-input v-model="form.slug" /></div>
        <div class="reading-edit__field"><label>摘要</label><el-input v-model="form.summary" type="textarea" :rows="2" /></div>
        <div class="reading-edit__field">
          <label>正文（Markdown）</label>
          <MarkdownEditor v-model="form.bodyMarkdown" placeholder="开始撰写正文…" />
        </div>
      </div>

      <aside class="reading-edit__side">
        <div class="reading-edit__field"><label>封面</label>
          <div class="reading-edit__cover">
            <img v-if="form.coverMediaId && form.coverMediaId" :src="`/uploads/${form.coverMediaId}`" alt="" />
            <span v-else>无封面</span>
            <el-button size="small" @click="mediaOpen = true">选择封面</el-button>
          </div>
        </div>
        <div class="reading-edit__field"><label>能力层级</label>
          <el-select v-model="form.readingLevel" style="width:100%">
            <el-option :value="1" label="基础阅读" /><el-option :value="2" label="结构阅读" /><el-option :value="3" label="深度阅读" />
          </el-select>
        </div>
        <div class="reading-edit__field"><label>CEFR</label>
          <el-select v-model="form.cefrLevel" style="width:100%">
            <el-option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :label="lv" :value="lv" />
          </el-select>
        </div>
        <div class="reading-edit__field"><label>来源名称</label><el-input v-model="form.sourceName" /></div>
        <div class="reading-edit__field"><label>来源 URL</label><el-input v-model="form.sourceUrl" /></div>
        <div class="reading-edit__field"><label>版权说明</label><el-input v-model="form.copyrightNote" /></div>
        <div class="reading-edit__field"><label>排序</label><el-input-number v-model="form.sortOrder" :min="1" :step="10" /></div>
      </aside>
    </div>

    <section class="reading-edit__tags">
      <h2>主题标签</h2>
      <SemanticTagPicker v-model="form.topicTagIds" :terms="topics()" dimension="TOPIC" />
      <h2>文体标签</h2>
      <SemanticTagPicker v-model="form.genreTagIds" :terms="genres()" dimension="GENRE" />
      <h2>能力标签</h2>
      <SemanticTagPicker v-model="form.abilityTagIds" :terms="abilities()" dimension="ABILITY" />
    </section>

    <MediaPicker v-model="mediaOpen" @select="onCoverSelect" />
    <PublishChecklistDrawer :open="checkOpen" :checks="checks" @close="checkOpen = false" />
  </section>
</template>

<style scoped>
.reading-edit__bar { position: sticky; top: 0; z-index: 5; display: flex; justify-content: space-between; align-items: center; padding: 16px 0 12px; background: var(--bg-page); }
.reading-edit__title { display: flex; align-items: center; gap: 10px; }
.reading-edit__title h1 { font-size: 22px; margin: 0; }
.reading-edit__title span { font-size: 12px; color: var(--text-secondary); }
.reading-edit__actions { display: flex; gap: 8px; }
.reading-edit__layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 24px; align-items: start; }
.reading-edit__main { min-width: 0; }
.reading-edit__side { display: flex; flex-direction: column; gap: 14px; border-left: 1px solid var(--border); padding-left: 24px; }
.reading-edit__field { display: flex; flex-direction: column; gap: 6px; }
.reading-edit__field label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.reading-edit__cover { display: flex; flex-direction: column; gap: 8px; }
.reading-edit__cover img { width: 100%; height: 130px; object-fit: cover; border-radius: 12px; }
.reading-edit__cover span { color: var(--text-muted); font-size: 13px; }
.reading-edit__tags { margin-top: 24px; }
.reading-edit__tags h2 { font-size: 15px; margin: 16px 0 8px; }
@media (max-width: 860px) { .reading-edit__layout { grid-template-columns: 1fr; } .reading-edit__side { border-left: none; padding-left: 0; } }
</style>
