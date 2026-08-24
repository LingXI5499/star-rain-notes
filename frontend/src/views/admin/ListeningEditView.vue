<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { batchSegments, createListening, createSegment, deleteSegment, fetchListening, updateListening, type ListeningItem, type ListeningSegment } from '@/api/listening'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import SemanticTagPicker from '@/components/english/SemanticTagPicker.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import PublishChecklistDrawer, { type PublishCheck } from '@/components/english/PublishChecklistDrawer.vue'

const route = useRoute(); const router = useRouter()
const itemId = computed(() => Number(route.params.id ?? 0))
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
    if (itemId.value) {
      const item: ListeningItem = await fetchListening(itemId.value)
      Object.assign(form, {
        title: item.title, slug: item.slug, summary: item.summary, transcriptMarkdown: item.transcriptMarkdown ?? '',
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
      segments.value = item.segments
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
    else { const created = await createListening(payload); router.replace({ name: 'admin-listening-edit', params: { id: created.id } }); ElMessage.success('已创建。') }
    await router.push({ name: 'admin-listening' })
  } catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。') }
  finally { saving.value = false }
}

async function addSegment() {
  if (!itemId.value) { ElMessage.warning('请先保存材料。'); return }
  try { const seg = await createSegment(itemId.value, { startMs: 0, endMs: 3000, transcriptText: '' }); await reloadSegments() }
  catch (e) { ElMessage.error((e as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '添加片段失败。') }
}
async function reloadSegments() { const r = await import('@/api/listening'); segments.value = await r.fetchSegments(itemId.value) }

const segDrafts = reactive({ }) as Record<number, { startMs: number; endMs: number; transcriptText: string; translationText: string }>

function onCover(asset: { id: number; publicUrl: string }) { form.coverMediaId = asset.id; coverUrl.value = asset.publicUrl; coverOpen.value = false }
function onAudio(asset: { id: number; publicUrl: string }) { form.audioMediaId = asset.id; audioUrl.value = asset.publicUrl; mediaOpen.value = false }
function removeAudio() { form.audioMediaId = null; audioUrl.value = null }
function removeCover() { form.coverMediaId = null; coverUrl.value = null }

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="listening-edit">
    <header class="listening-edit__bar">
      <div class="listening-edit__title"><CefrBadge :level="form.cefrLevel"/><h1>{{ itemId ? '编辑材料' : '新建材料' }}</h1><span>正文目录收录 H2–H4，页面唯一 H1 为标题。</span></div>
      <div class="listening-edit__actions"><el-button @click="checkOpen = true">发布检查</el-button><el-button @click="router.push({ name: 'admin-listening' })">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></div>
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
                <el-button link type="danger" @click="deleteSegment(itemId, seg.id).then(reloadSegments).catch(()=>{})">删除</el-button>
              </div>
              <textarea :value="seg.transcriptText" @change="seg.transcriptText = ($event.target as HTMLTextAreaElement).value" placeholder="原文" rows="2"/>
              <textarea :value="seg.translationText ?? ''" @change="seg.translationText = ($event.target as HTMLTextAreaElement).value" placeholder="翻译" rows="2"/>
            </div>
            <div v-if="segments.length"><el-button size="small" type="primary" @click="reloadSegments">刷新</el-button></div>
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
.listening-edit__tags{margin-top:24px}.listening-edit__tags h2{font-size:15px;margin:16px 0 8px}
@media (max-width:860px){.listening-edit__layout{grid-template-columns:1fr}.listening-edit__side{border-left:none;padding-left:0}}
</style>
