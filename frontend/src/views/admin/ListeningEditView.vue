<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { addReadingPair, batchSegments, createListening, createSegment, deleteSegment, fetchListening, removeReadingPair, updateListening, type ListeningItem, type ListeningSegment, type ReadingPairRef } from '@/api/listening'
import { fetchReadings, type ReadingArticleSummary } from '@/api/reading'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import type { MediaAsset } from '@/api/media'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import SemanticTagPicker from '@/components/english/SemanticTagPicker.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import PublishChecklistDrawer, { type PublishCheck } from '@/components/english/PublishChecklistDrawer.vue'

const route = useRoute(); const router = useRouter()
const itemId = computed(() => Number(route.params.id ?? 0))
const cloneFrom = Number(route.query.clone ?? 0)
const taxonomy = ref<TaxonomyTerm[]>([])
const loading = ref(true); const saving = ref(false)
const mediaOpen = ref(false); const coverOpen = ref(false); const checkOpen = ref(false)
const audioUrl = ref<string | null>(null); const coverUrl = ref<string | null>(null)

const form = reactive({
  title: '', slug: '', summary: '', transcriptMarkdown: '', cefrLevel: 'B1', listeningLevel: 1,
  audioMediaId: null as number | null, coverMediaId: null as number | null, durationSeconds: 60,
  sourceName: '', sourceUrl: '', copyrightNote: '', sortOrder: 10,
  topicTagIds: [] as number[], sceneTagIds: [] as number[], formatTagIds: [] as number[],
  abilityTagIds: [] as number[], functionTagIds: [] as number[],
})
const segments = ref<ListeningSegment[]>([])
const readingOptions = ref<ReadingArticleSummary[]>([]); const readingPairs = ref<ReadingPairRef[]>([])
const pairDraft = reactive({ readingArticleId: null as number|null, relationType: 'SAME_TOPIC' })

const checks = computed<PublishCheck[]>(() => [
  { key: 'title', label: '标题完整', passed: !!form.title.trim() },
  { key: 'slug', label: 'slug 稳定', passed: !!form.slug.trim() },
  { key: 'summary', label: '摘要完整', passed: !!form.summary.trim() },
  { key: 'level', label: '能力层级合法', passed: [1, 2, 3].includes(form.listeningLevel) },
  { key: 'cefr', label: 'CEFR 已选', passed: !!form.cefrLevel },
  { key: 'audio', label: '音频资源存在且为 AUDIO', passed: !!form.audioMediaId },
  { key: 'scene', label: '至少一个场景标签', passed: form.sceneTagIds.length > 0 },
  { key: 'format', label: '至少一个形式标签', passed: form.formatTagIds.length > 0 },
])

const tags = (dim: string) => taxonomy.value.filter((t) => t.dimension === dim && t.parentId === null)

async function load() {
  loading.value = true
  try {
    taxonomy.value = await fetchTaxonomy('tree')
    if (itemId.value || cloneFrom) {
      const item: ListeningItem = await fetchListening(cloneFrom || itemId.value)
      Object.assign(form, {
        title: item.title, slug: cloneFrom ? `${item.slug}-copy` : item.slug, summary: item.summary, transcriptMarkdown: item.transcriptMarkdown ?? '',
        cefrLevel: item.cefrLevel, listeningLevel: item.listeningLevel, audioMediaId: item.audioMediaId,
        coverMediaId: item.coverMediaId, durationSeconds: item.durationSeconds, sourceName: item.sourceName ?? '',
        sourceUrl: item.sourceUrl ?? '', copyrightNote: item.copyrightNote ?? '', sortOrder: item.sortOrder,
        topicTagIds: item.tags.filter(t=>t.dimension==='TOPIC').map(t=>t.id),
        sceneTagIds: item.tags.filter(t=>t.dimension==='SCENE').map(t=>t.id),
        formatTagIds: item.tags.filter(t=>t.dimension==='FORMAT').map(t=>t.id),
        abilityTagIds: item.tags.filter(t=>t.dimension==='ABILITY').map(t=>t.id),
        functionTagIds: item.tags.filter(t=>t.dimension==='FUNCTION').map(t=>t.id),
      })
      audioUrl.value = item.audioUrl; coverUrl.value = item.coverUrl
      segments.value = cloneFrom ? [] : item.segments
      readingPairs.value = cloneFrom ? [] : item.readingPairs
      if (!cloneFrom) {
        const readingPage = await fetchReadings({ page: 1, pageSize: 50 })
        readingOptions.value = readingPage.items
      }
    }
  } catch { ElMessage.error('加载材料失败。') } finally { loading.value = false }
}

async function save() {
  if (!form.title.trim() || !form.slug.trim()) { ElMessage.warning('请填写标题与 slug。'); return }
  saving.value = true
  const payload = {
    title: form.title.trim(), slug: form.slug.trim(), summary: form.summary.trim(), transcriptMarkdown: form.transcriptMarkdown,
    cefrLevel: form.cefrLevel, listeningLevel: form.listeningLevel, audioMediaId: form.audioMediaId, coverMediaId: form.coverMediaId,
    durationSeconds: form.durationSeconds, sourceName: form.sourceName || null, sourceUrl: form.sourceUrl || null,
    copyrightNote: form.copyrightNote || null, sortOrder: form.sortOrder, topicTagIds: form.topicTagIds,
    sceneTagIds: form.sceneTagIds, formatTagIds: form.formatTagIds, abilityTagIds: form.abilityTagIds, functionTagIds: form.functionTagIds,
  }
  try {
    if (itemId.value) { await updateListening(itemId.value, payload); ElMessage.success('已更新。') }
    else { await createListening(payload); ElMessage.success('已创建。') }
    await router.push({ name: 'admin-listening', query: listQuery() })
  } catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。') }
  finally { saving.value = false }
}

async function addSegment() {
  if (!itemId.value) { ElMessage.warning('请先保存材料。'); return }
  const lastEnd = segments.value.at(-1)?.endMs ?? 0
  if (form.durationSeconds > 0 && lastEnd >= form.durationSeconds * 1000) { ElMessage.warning('没有可用时间，请先调整材料时长或现有片段。'); return }
  const endMs = form.durationSeconds > 0 ? Math.min(lastEnd + 3000, form.durationSeconds * 1000) : lastEnd + 3000
  try { await createSegment(itemId.value, { startMs: lastEnd, endMs, transcriptText: '待补充原文' }); await reloadSegments() }
  catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '添加片段失败。') }
}
async function reloadSegments() { const r = await import('@/api/listening'); segments.value = await r.fetchSegments(itemId.value) }

async function addPair() {
  if (!itemId.value || !pairDraft.readingArticleId) { ElMessage.warning('请先选择阅读文章。'); return }
  try {
    await addReadingPair(itemId.value, pairDraft.readingArticleId, pairDraft.relationType)
    const selected = readingOptions.value.find((article) => article.id === pairDraft.readingArticleId)
    if (selected) readingPairs.value.push({ readingArticleId: selected.id, readingTitle: selected.title, readingSlug: selected.slug, relationType: pairDraft.relationType })
    pairDraft.readingArticleId = null; ElMessage.success('已关联精读文章。')
  } catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '关联失败。') }
}
async function removePair(pair: ReadingPairRef) {
  try { await removeReadingPair(itemId.value, pair.readingArticleId); readingPairs.value = readingPairs.value.filter((item) => item.readingArticleId !== pair.readingArticleId) }
  catch { ElMessage.error('解除关联失败。') }
}

async function saveSegments() {
  if (!itemId.value || !segments.value.length) return
  try {
    segments.value = await batchSegments(itemId.value, segments.value.map((seg) => ({
      startMs: seg.startMs, endMs: seg.endMs, transcriptText: seg.transcriptText.trim(),
      translationText: seg.translationText?.trim() || null,
    })))
    ElMessage.success('片段内容与顺序已保存。')
  } catch (e) {
    ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '片段保存失败。')
  }
}
function moveSegmentLocal(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= segments.value.length) return
  const next = [...segments.value]
  ;[next[index], next[target]] = [next[target], next[index]]
  segments.value = next
}
function listQuery() {
  const { clone: _clone, ...query } = route.query
  return query
}
function cancel() { void router.push({ name: 'admin-listening', query: listQuery() }) }
function onCover(asset: MediaAsset) {
  if (asset.assetType !== 'IMAGE') { ElMessage.warning('封面只能选择图片。'); return }
  form.coverMediaId = asset.id; coverUrl.value = asset.publicUrl; coverOpen.value = false
}
function onAudio(asset: MediaAsset) {
  if (asset.assetType !== 'AUDIO') { ElMessage.warning('听力音频只能选择音频文件。'); return }
  form.audioMediaId = asset.id; audioUrl.value = asset.publicUrl; mediaOpen.value = false
}
function removeAudio() { form.audioMediaId = null; audioUrl.value = null }
function removeCover() { form.coverMediaId = null; coverUrl.value = null }

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="listening-edit">
    <header class="listening-edit__bar">
      <div class="listening-edit__title"><CefrBadge :level="form.cefrLevel"/><h1>{{ itemId ? '编辑材料' : '新建材料' }}</h1><span>正文目录收录 H2–H4，页面唯一 H1 为标题。</span></div>
      <div class="listening-edit__actions"><el-button @click="checkOpen = true">发布检查</el-button><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></div>
    </header>

    <div class="listening-edit__layout">
      <div class="listening-edit__main">
        <div class="le-field"><label>标题</label><el-input v-model="form.title"/></div>
        <div class="le-field"><label>Slug</label><el-input v-model="form.slug"/></div>
        <div class="le-field"><label>摘要</label><el-input v-model="form.summary" type="textarea" :rows="2"/></div>
        <div class="le-field"><label>培养说明（Markdown）</label><MarkdownEditor v-model="form.transcriptMarkdown" placeholder="补充讲解正文…"/></div>
        <div class="le-field"><label>时间片段</label>
          <div class="le-segments">
            <el-button size="small" @click="addSegment">+ 新增片段</el-button>
            <div v-for="(seg, i) in segments" :key="seg.id" class="le-segment">
              <div class="le-segment__row">
                <input :value="seg.startMs" @change="seg.startMs = Number(($event.target as HTMLInputElement).value)" placeholder="开始 ms" />
                <span>→</span>
                <input :value="seg.endMs" @change="seg.endMs = Number(($event.target as HTMLInputElement).value)" placeholder="结束 ms" />
                <el-button link :disabled="i===0" @click="moveSegmentLocal(i,-1)">上移</el-button>
                <el-button link :disabled="i===segments.length-1" @click="moveSegmentLocal(i,1)">下移</el-button>
                <el-button link type="danger" @click="deleteSegment(itemId, seg.id).then(reloadSegments).catch(()=>ElMessage.error('删除片段失败。'))">删除</el-button>
              </div>
              <textarea :value="seg.transcriptText" @change="seg.transcriptText = ($event.target as HTMLTextAreaElement).value" placeholder="原文" rows="2"/>
              <textarea :value="seg.translationText ?? ''" @change="seg.translationText = ($event.target as HTMLTextAreaElement).value" placeholder="翻译" rows="2"/>
            </div>
            <div v-if="segments.length" class="le-segments__actions"><el-button size="small" @click="reloadSegments">放弃修改</el-button><el-button size="small" type="primary" @click="saveSegments">保存全部片段</el-button></div>
          </div>
        </div>
      </div>
      <aside class="listening-edit__side">
        <div class="le-field"><label>音频</label>
          <div class="le-media">
            <audio v-if="audioUrl" :src="audioUrl" controls preload="none"/>
            <span v-else>未选择音频</span>
            <div><el-button size="small" @click="mediaOpen = true">选择音频</el-button><el-button v-if="audioUrl" size="small" @click="removeAudio">移除</el-button></div>
          </div>
        </div>
        <div class="le-field"><label>封面</label>
          <div class="le-media"><img v-if="coverUrl" :src="coverUrl" alt=""/><span v-else>无封面</span>
            <div><el-button size="small" @click="coverOpen = true">选择封面</el-button><el-button v-if="coverUrl" size="small" @click="removeCover">移除</el-button></div>
          </div>
        </div>
        <div class="le-field"><label>能力层级</label><el-select v-model="form.listeningLevel" style="width:100%"><el-option :value="1" label="语音识别"/><el-option :value="2" label="信息捕获"/><el-option :value="3" label="逻辑理解"/></el-select></div>
        <div class="le-field"><label>CEFR</label><el-select v-model="form.cefrLevel" style="width:100%"><el-option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :label="lv" :value="lv"/></el-select></div>
        <div class="le-field"><label>时长（秒）</label><el-input-number v-model="form.durationSeconds" :min="0"/></div>
        <div class="le-field"><label>来源</label><el-input v-model="form.sourceName"/></div>
        <div class="le-field"><label>来源 URL</label><el-input v-model="form.sourceUrl"/></div>
        <div class="le-field"><label>版权</label><el-input v-model="form.copyrightNote"/></div>
        <div v-if="itemId" class="le-field"><label>配对精读</label>
          <div class="le-pair-form"><el-select v-model="pairDraft.readingArticleId" filterable placeholder="选择阅读文章"><el-option v-for="article in readingOptions" :key="article.id" :label="article.title" :value="article.id"/></el-select><el-select v-model="pairDraft.relationType"><el-option label="同主题" value="SAME_TOPIC"/><el-option label="同内容" value="SAME_CONTENT"/><el-option label="拓展训练" value="EXTENDED_TRAINING"/></el-select><el-button @click="addPair">关联</el-button></div>
          <div v-for="pair in readingPairs" :key="pair.readingArticleId" class="le-pair"><span>{{pair.readingTitle}}</span><el-button link type="danger" @click="removePair(pair)">解除</el-button></div>
        </div>
      </aside>
    </div>

    <section class="listening-edit__tags">
      <h2>场景标签</h2><SemanticTagPicker v-model="form.sceneTagIds" :terms="tags('SCENE')" dimension="SCENE"/>
      <h2>形式标签</h2><SemanticTagPicker v-model="form.formatTagIds" :terms="tags('FORMAT')" dimension="FORMAT"/>
      <h2>主题标签</h2><SemanticTagPicker v-model="form.topicTagIds" :terms="tags('TOPIC')" dimension="TOPIC"/>
      <h2>能力标签</h2><SemanticTagPicker v-model="form.abilityTagIds" :terms="tags('ABILITY')" dimension="ABILITY"/>
    </section>

    <MediaPicker v-model="mediaOpen" @select="onAudio"/>
    <MediaPicker v-model="coverOpen" @select="onCover"/>
    <PublishChecklistDrawer :open="checkOpen" :checks="checks" @close="checkOpen = false"/>
  </section>
</template>

<style scoped>
.listening-edit__bar{position:sticky;top:0;z-index:5;display:flex;justify-content:space-between;align-items:center;padding:16px 0 12px;background:var(--bg-page)}
.listening-edit__title{display:flex;align-items:center;gap:10px}.listening-edit__title h1{font-size:22px;margin:0}.listening-edit__title span{font-size:12px;color:var(--text-secondary)}
.listening-edit__actions{display:flex;gap:8px}
.listening-edit__layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:24px;align-items:start}.listening-edit__main{min-width:0}
.listening-edit__side{display:flex;flex-direction:column;gap:14px;border-left:1px solid var(--border);padding-left:24px}
.le-field{display:flex;flex-direction:column;gap:6px}.le-field label{font-size:13px;font-weight:600;color:var(--text-primary)}
.le-media{display:flex;flex-direction:column;gap:8px}.le-media img{width:100%;height:120px;object-fit:cover;border-radius:10px}.le-media audio{width:100%}
.le-segments{display:flex;flex-direction:column;gap:12px}.le-segment{border:1px solid var(--border);border-radius:10px;padding:10px;display:flex;flex-direction:column;gap:6px}
.le-segment__row{display:flex;align-items:center;gap:6px}.le-segment__row input{width:80px;padding:6px;border:1px solid var(--border);border-radius:6px;background:var(--bg-surface);color:var(--text-primary)}
.le-segment textarea{padding:8px;border:1px solid var(--border);border-radius:6px;background:var(--bg-surface);color:var(--text-primary);resize:vertical}
.le-pair-form{display:grid;grid-template-columns:1fr 120px auto;gap:6px}.le-pair{display:flex;align-items:center;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--border);font-size:12px}
.listening-edit__tags{margin-top:24px}.listening-edit__tags h2{font-size:15px;margin:16px 0 8px}
@media (max-width:860px){.listening-edit__layout{grid-template-columns:1fr}.listening-edit__side{border-left:none;padding-left:0}}
@media (max-width:720px){
  .listening-edit__bar{align-items:flex-start;gap:12px}.listening-edit__title span{display:none}
  .listening-edit__actions{flex-wrap:wrap;justify-content:flex-end}.le-segment__row{flex-wrap:wrap}
  .le-segment__row input{width:110px}.listening-edit__title h1{font-size:19px}.le-pair-form{grid-template-columns:1fr}
}
@media (prefers-reduced-motion:reduce){*{scroll-behavior:auto!important;transition-duration:.01ms!important}}
</style>
