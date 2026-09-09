<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import type { ContentReview } from '@/api/account'
import type { ProblemDetail } from '@/api/http'
import {
  addReadingPair,
  batchSegments,
  createListening,
  fetchListening,
  fetchListeningExercises,
  removeReadingPair,
  updateListening,
  type ListeningItem,
  type ListeningSegment,
  type ReadingPairRef,
} from '@/api/listening'
import { fetchReadings, type ReadingArticleSummary } from '@/api/reading'
import { fetchTaxonomy, type TaxonomyTerm } from '@/api/englishMeta'
import type { MediaAsset } from '@/api/media'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import SemanticTagPicker from '@/components/english/SemanticTagPicker.vue'
import CefrBadge from '@/components/english/CefrBadge.vue'
import PublishChecklistDrawer, { type PublishCheck } from '@/components/english/PublishChecklistDrawer.vue'
import { formatTimecode, parseTimecode, validateSegmentDrafts } from '@/lib/listeningSegments'

const route = useRoute()
const router = useRouter()
const itemId = computed(() => Number(route.params.id ?? 0))
const cloneFrom = Number(route.query.clone ?? 0)
const taxonomy = ref<TaxonomyTerm[]>([])
const loading = ref(true)
const saving = ref(false)
const segmentSaving = ref(false)
const mediaOpen = ref(false)
const checkOpen = ref(false)
const audioUrl = ref<string | null>(null)
const audioPlayer = ref<HTMLAudioElement | null>(null)
const audioReady = ref(false)
const audioError = ref('')
const previewEndMs = ref<number | null>(null)
const publishedExerciseCount = ref(0)

const form = reactive({
  title: '',
  summary: '',
  transcriptMarkdown: '',
  cefrLevel: 'B1',
  listeningLevel: 1,
  audioMediaId: null as number | null,
  coverMediaId: null as number | null,
  durationSeconds: 0,
  sourceName: '',
  sourceUrl: '',
  copyrightNote: '',
  sortOrder: null as number | null,
  topicTagIds: [] as number[],
  sceneTagIds: [] as number[],
  formatTagIds: [] as number[],
  abilityTagIds: [] as number[],
  functionTagIds: [] as number[],
})

const segments = ref<ListeningSegment[]>([])
const readingOptions = ref<ReadingArticleSummary[]>([])
const readingPairs = ref<ReadingPairRef[]>([])
const pairDraft = reactive({ readingArticleId: null as number | null, relationType: 'SAME_TOPIC' })
let localSegmentId = -1

const segmentIssues = computed(() => validateSegmentDrafts(segments.value, form.durationSeconds))

const checks = computed<PublishCheck[]>(() => [
  { key: 'title', label: '标题完整', passed: !!form.title.trim() },
  { key: 'slug', label: '内容编号自动生成', passed: true },
  { key: 'summary', label: '摘要完整', passed: !!form.summary.trim() },
  { key: 'level', label: '能力层级合法', passed: [1, 2, 3].includes(form.listeningLevel) },
  { key: 'cefr', label: 'CEFR 已选', passed: !!form.cefrLevel },
  { key: 'audio', label: audioError.value || '完整音频已选择并可加载', passed: !!form.audioMediaId && audioReady.value && !audioError.value },
  { key: 'scene', label: '至少一个场景标签', passed: form.sceneTagIds.length > 0 },
  { key: 'format', label: '至少一个形式标签', passed: form.formatTagIds.length > 0 },
  { key: 'segments', label: segmentIssues.value[0] || '至少一个有效且不重叠的时间片段', passed: segments.value.length > 0 && !segmentIssues.value.length },
  { key: 'exercise', label: '至少一道已发布听力练习', passed: publishedExerciseCount.value > 0 },
])

const tags = (dimension: string) => taxonomy.value.filter((term) => term.dimension === dimension && term.parentId === null)
function isReview(value: unknown): value is ContentReview {
  return typeof value === 'object' && value !== null && 'contentType' in value
}

function applyItem(item: ListeningItem, cloned: boolean) {
  Object.assign(form, {
    title: item.title,
    summary: item.summary,
    transcriptMarkdown: item.transcriptMarkdown ?? '',
    cefrLevel: item.cefrLevel,
    listeningLevel: item.listeningLevel,
    audioMediaId: item.audioMediaId,
    coverMediaId: item.coverMediaId,
    durationSeconds: item.durationSeconds,
    sourceName: item.sourceName ?? '',
    sourceUrl: item.sourceUrl ?? '',
    copyrightNote: item.copyrightNote ?? '',
    sortOrder: cloned ? null : item.sortOrder,
    topicTagIds: item.tags.filter((tag) => tag.dimension === 'TOPIC').map((tag) => tag.id),
    sceneTagIds: item.tags.filter((tag) => tag.dimension === 'SCENE').map((tag) => tag.id),
    formatTagIds: item.tags.filter((tag) => tag.dimension === 'FORMAT').map((tag) => tag.id),
    abilityTagIds: item.tags.filter((tag) => tag.dimension === 'ABILITY').map((tag) => tag.id),
    functionTagIds: item.tags.filter((tag) => tag.dimension === 'FUNCTION').map((tag) => tag.id),
  })
  audioUrl.value = item.audioUrl
  segments.value = cloned ? [] : item.segments.map((segment) => ({ ...segment }))
  readingPairs.value = cloned ? [] : item.readingPairs
}

async function load() {
  loading.value = true
  audioReady.value = false
  audioError.value = ''
  try {
    const [terms, readingPage] = await Promise.all([
      fetchTaxonomy('tree'),
      fetchReadings({ page: 1, pageSize: 100, status: 'PUBLISHED' }),
    ])
    taxonomy.value = terms
    readingOptions.value = readingPage.items
    if (itemId.value || cloneFrom) {
      const item = await fetchListening(cloneFrom || itemId.value)
      applyItem(item, !!cloneFrom)
      if (!cloneFrom && itemId.value) {
        const exercises = await fetchListeningExercises(itemId.value)
        publishedExerciseCount.value = exercises.filter((exercise) => exercise.publishStatus === 'PUBLISHED').length
      }
    }
    await nextTick()
    audioPlayer.value?.load()
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '加载材料失败。')
  } finally {
    loading.value = false
  }
}

function payload() {
  return {
    title: form.title.trim(),
    summary: form.summary.trim(),
    transcriptMarkdown: form.transcriptMarkdown,
    cefrLevel: form.cefrLevel,
    listeningLevel: form.listeningLevel,
    audioMediaId: form.audioMediaId,
    coverMediaId: form.coverMediaId,
    durationSeconds: form.durationSeconds,
    sourceName: form.sourceName.trim() || null,
    sourceUrl: form.sourceUrl.trim() || null,
    copyrightNote: form.copyrightNote.trim() || null,
    sortOrder: form.sortOrder,
    topicTagIds: form.topicTagIds,
    sceneTagIds: form.sceneTagIds,
    formatTagIds: form.formatTagIds,
    abilityTagIds: form.abilityTagIds,
    functionTagIds: form.functionTagIds,
  }
}

async function save() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('请填写标题和摘要。')
    return
  }
  saving.value = true
  try {
    if (itemId.value) {
      const result = await updateListening(itemId.value, payload())
      ElMessage.success(isReview(result) ? '已提交审核，超级管理员批准后会应用到线上听力材料。' : '基础信息已保存。')
    } else {
      const created = await createListening(payload())
      ElMessage.success('材料已创建，请继续录入时间片段、配对阅读和练习。')
      await router.replace({ name: 'admin-listening-edit', params: { id: created.id }, query: listQuery() })
      await load()
    }
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

function addSegment() {
  if (!itemId.value) {
    ElMessage.warning('请先保存材料基础信息。')
    return
  }
  const lastEnd = segments.value.at(-1)?.endMs ?? Math.round((audioPlayer.value?.currentTime ?? 0) * 1000)
  const maxEnd = form.durationSeconds > 0 ? form.durationSeconds * 1000 : lastEnd + 3000
  const endMs = Math.min(lastEnd + 3000, maxEnd)
  if (endMs <= lastEnd) {
    ElMessage.warning('完整音频已经没有可用时间，请调整时长或前一片段。')
    return
  }
  segments.value.push({
    id: localSegmentId--,
    itemId: itemId.value,
    startMs: lastEnd,
    endMs,
    transcriptText: '',
    translationText: null,
    sortOrder: segments.value.length * 10 + 10,
    updatedAt: '',
  })
}

async function reloadSegments() {
  if (!itemId.value) return
  const { fetchSegments } = await import('@/api/listening')
  segments.value = (await fetchSegments(itemId.value)).map((segment) => ({ ...segment }))
}

async function saveSegments() {
  if (!itemId.value) return
  if (segmentIssues.value.length) {
    ElMessage.warning(segmentIssues.value[0])
    return
  }
  segmentSaving.value = true
  try {
    segments.value = await batchSegments(itemId.value, segments.value.map((segment) => ({
      startMs: segment.startMs,
      endMs: segment.endMs,
      transcriptText: segment.transcriptText.trim(),
      translationText: segment.translationText?.trim() || null,
    })))
    ElMessage.success(segments.value.length ? '全部时间片段已保存。' : '时间片段已清空。')
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '片段保存失败。')
  } finally {
    segmentSaving.value = false
  }
}

function removeSegment(index: number) {
  segments.value.splice(index, 1)
}

function moveSegmentLocal(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= segments.value.length) return
  const next = [...segments.value]
  ;[next[index], next[target]] = [next[target], next[index]]
  segments.value = next
}

function updateTime(segment: ListeningSegment, field: 'startMs' | 'endMs', value: string) {
  const parsed = parseTimecode(value)
  if (parsed === null) {
    ElMessage.warning('时间格式应为 时:分:秒.毫秒，例如 00:01:12.500。')
    return
  }
  segment[field] = parsed
}

function useCurrentTime(segment: ListeningSegment, field: 'startMs' | 'endMs') {
  if (!audioPlayer.value) return
  segment[field] = Math.round(audioPlayer.value.currentTime * 1000)
}

async function previewSegment(segment: ListeningSegment) {
  const player = audioPlayer.value
  if (!player || !audioReady.value) {
    ElMessage.warning(audioError.value || '请先选择并加载完整音频。')
    return
  }
  player.currentTime = segment.startMs / 1000
  previewEndMs.value = segment.endMs
  try {
    await player.play()
  } catch {
    ElMessage.warning('浏览器未能开始播放，请手动点击播放器后重试。')
  }
}

function handleTimeUpdate() {
  const player = audioPlayer.value
  if (player && previewEndMs.value !== null && player.currentTime * 1000 >= previewEndMs.value) {
    player.pause()
    previewEndMs.value = null
  }
}

function handleAudioLoaded() {
  const player = audioPlayer.value
  audioReady.value = true
  audioError.value = ''
  if (player && Number.isFinite(player.duration) && player.duration > 0) {
    form.durationSeconds = Math.ceil(player.duration)
  }
}

function handleAudioError() {
  audioReady.value = false
  audioError.value = '音频无法加载：请检查文件是否存在、/uploads/ 映射、权限与音频格式'
}

async function addPair() {
  if (!itemId.value || !pairDraft.readingArticleId) {
    ElMessage.warning('请先选择阅读文章。')
    return
  }
  try {
    await addReadingPair(itemId.value, pairDraft.readingArticleId, pairDraft.relationType)
    const selected = readingOptions.value.find((article) => article.id === pairDraft.readingArticleId)
    if (selected) {
      readingPairs.value.push({
        readingArticleId: selected.id,
        readingTitle: selected.title,
        readingSlug: selected.slug,
        relationType: pairDraft.relationType,
      })
    }
    pairDraft.readingArticleId = null
    ElMessage.success('已关联精读文章。')
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '关联失败。')
  }
}

async function removePair(pair: ReadingPairRef) {
  try {
    await removeReadingPair(itemId.value, pair.readingArticleId)
    readingPairs.value = readingPairs.value.filter((item) => item.readingArticleId !== pair.readingArticleId)
  } catch {
    ElMessage.error('解除关联失败。')
  }
}

function listQuery() {
  const { clone: _clone, ...query } = route.query
  return query
}
function cancel() {
  void router.push({ name: 'admin-listening', query: listQuery() })
}
function onAudio(asset: MediaAsset) {
  form.audioMediaId = asset.id
  audioUrl.value = asset.publicUrl
  audioReady.value = false
  audioError.value = ''
  void nextTick(() => audioPlayer.value?.load())
}
function removeAudio() {
  form.audioMediaId = null
  audioUrl.value = null
  audioReady.value = false
  audioError.value = ''
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="listening-edit">
    <header class="listening-edit__bar">
      <div class="listening-edit__title">
        <CefrBadge :level="form.cefrLevel" />
        <div><h1>{{ itemId ? '编辑听力材料' : '新建听力材料' }}</h1><span>先保存完整音频和基础信息，再制作逐句时间片段。</span></div>
      </div>
      <div class="listening-edit__actions">
        <el-button @click="checkOpen = true">发布检查</el-button>
        <el-button @click="cancel">返回列表</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存基础信息</el-button>
      </div>
    </header>

    <el-alert
      title="听力内容关系"
      description="完整音频是一整段对话或独白；完整听力稿用于展示全文与学习说明；时间片段把音频起止时间映射到逐句英文和中文翻译，前台会据此高亮并支持点击定位。"
      type="info"
      :closable="false"
      show-icon
      class="listening-edit__explain"
    />

    <div class="listening-edit__layout">
      <main class="listening-edit__main">
        <div class="le-field"><label>标题</label><el-input v-model="form.title" /></div>
        <div class="le-field"><label>摘要</label><el-input v-model="form.summary" type="textarea" :rows="2" /></div>
        <div class="le-field">
          <label>完整听力稿与学习说明（Markdown）</label>
          <MarkdownEditor v-model="form.transcriptMarkdown" placeholder="填写完整英文听力稿、重点表达和学习建议…" />
        </div>

        <section class="le-field">
          <div class="le-section-title">
            <div><label>逐句时间片段</label><small>使用 00:00:00.000 格式；片段不可重叠并按播放顺序排列。</small></div>
            <el-button size="small" :disabled="!itemId" @click="addSegment">新增片段</el-button>
          </div>
          <div class="le-segments">
            <article v-for="(segment, index) in segments" :key="segment.id" class="le-segment">
              <header>
                <b>片段 {{ index + 1 }}</b>
                <nav>
                  <el-button link :disabled="index === 0" @click="moveSegmentLocal(index, -1)">上移</el-button>
                  <el-button link :disabled="index === segments.length - 1" @click="moveSegmentLocal(index, 1)">下移</el-button>
                  <el-button link type="primary" @click="previewSegment(segment)">试听</el-button>
                  <el-button link type="danger" @click="removeSegment(index)">删除</el-button>
                </nav>
              </header>
              <div class="le-segment__times">
                <label>开始
                  <el-input :model-value="formatTimecode(segment.startMs)" @change="updateTime(segment, 'startMs', String($event))" />
                </label>
                <el-button size="small" @click="useCurrentTime(segment, 'startMs')">取播放器时间</el-button>
                <span>→</span>
                <label>结束
                  <el-input :model-value="formatTimecode(segment.endMs)" @change="updateTime(segment, 'endMs', String($event))" />
                </label>
                <el-button size="small" @click="useCurrentTime(segment, 'endMs')">取播放器时间</el-button>
              </div>
              <el-input v-model="segment.transcriptText" type="textarea" :rows="2" placeholder="本片段英文原文（必填）" />
              <el-input v-model="segment.translationText" type="textarea" :rows="2" placeholder="本片段中文翻译（可选）" />
            </article>
            <p v-if="!segments.length" class="le-empty">尚未添加时间片段。保存基础信息后，从完整音频开头依次标记每句话。</p>
            <el-alert v-if="segmentIssues.length" :title="segmentIssues[0]" type="warning" :closable="false" show-icon />
            <div v-if="itemId" class="le-segments__actions">
              <el-button size="small" @click="reloadSegments">放弃本地修改</el-button>
              <el-button size="small" type="primary" :loading="segmentSaving" @click="saveSegments">保存全部片段</el-button>
            </div>
          </div>
        </section>
      </main>

      <aside class="listening-edit__side">
        <div class="le-field"><label>完整音频</label>
          <div class="le-media">
            <audio
              v-if="audioUrl"
              ref="audioPlayer"
              :src="audioUrl"
              controls
              preload="metadata"
              @loadedmetadata="handleAudioLoaded"
              @error="handleAudioError"
              @timeupdate="handleTimeUpdate"
            />
            <span v-else>未选择音频</span>
            <el-alert v-if="audioError" :title="audioError" type="error" :closable="false" />
            <div>
              <el-button size="small" @click="mediaOpen = true">{{ audioUrl ? '重新选择' : '上传或选择音频' }}</el-button>
              <el-button v-if="audioUrl" size="small" @click="removeAudio">移除</el-button>
            </div>
          </div>
        </div>
        <div class="le-field"><label>能力层级</label><el-select v-model="form.listeningLevel" style="width:100%"><el-option :value="1" label="语音识别" /><el-option :value="2" label="信息捕获" /><el-option :value="3" label="逻辑理解" /></el-select></div>
        <div class="le-field"><label>CEFR</label><el-select v-model="form.cefrLevel" style="width:100%"><el-option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level" :label="level" :value="level" /></el-select></div>
        <div class="le-field"><label>完整音频时长（秒，加载音频后自动填写）</label><el-input-number v-model="form.durationSeconds" :min="0" /></div>
        <div class="le-field"><label>排序</label><el-input-number v-model="form.sortOrder" :min="0" :step="10" placeholder="留空自动追加" /></div>
        <div class="le-field"><label>来源</label><el-input v-model="form.sourceName" /></div>
        <div class="le-field"><label>来源 URL</label><el-input v-model="form.sourceUrl" /></div>
        <div class="le-field"><label>版权说明</label><el-input v-model="form.copyrightNote" type="textarea" :rows="2" /></div>
        <div v-if="itemId" class="le-field"><label>配对精读</label>
          <div class="le-pair-form">
            <el-select v-model="pairDraft.readingArticleId" filterable placeholder="选择已发布阅读文章"><el-option v-for="article in readingOptions" :key="article.id" :label="article.title" :value="article.id" /></el-select>
            <el-select v-model="pairDraft.relationType"><el-option label="同主题" value="SAME_TOPIC" /><el-option label="同内容" value="SAME_CONTENT" /><el-option label="拓展训练" value="EXTENDED_TRAINING" /></el-select>
            <el-button @click="addPair">关联</el-button>
          </div>
          <div v-for="pair in readingPairs" :key="pair.readingArticleId" class="le-pair"><span>{{ pair.readingTitle }}</span><el-button link type="danger" @click="removePair(pair)">解除</el-button></div>
        </div>
      </aside>
    </div>

    <section class="listening-edit__tags">
      <h2>场景标签（发布必填）</h2><SemanticTagPicker v-model="form.sceneTagIds" :terms="tags('SCENE')" dimension="SCENE" />
      <h2>形式标签（发布必填）</h2><SemanticTagPicker v-model="form.formatTagIds" :terms="tags('FORMAT')" dimension="FORMAT" />
      <h2>主题标签</h2><SemanticTagPicker v-model="form.topicTagIds" :terms="tags('TOPIC')" dimension="TOPIC" />
      <h2>能力标签</h2><SemanticTagPicker v-model="form.abilityTagIds" :terms="tags('ABILITY')" dimension="ABILITY" />
      <h2>功能标签</h2><SemanticTagPicker v-model="form.functionTagIds" :terms="tags('FUNCTION')" dimension="FUNCTION" />
    </section>

    <MediaPicker v-model="mediaOpen" asset-type="AUDIO" allow-upload title="上传或选择完整听力音频" @select="onAudio" />
    <PublishChecklistDrawer :open="checkOpen" :checks="checks" @close="checkOpen = false" />
  </section>
</template>

<style scoped>
.listening-edit__bar{position:sticky;top:0;z-index:5;display:flex;justify-content:space-between;align-items:center;padding:16px 0 12px;background:var(--bg-page)}
.listening-edit__title{display:flex;align-items:center;gap:10px}.listening-edit__title h1{margin:0;font-size:22px}.listening-edit__title span{font-size:12px;color:var(--text-secondary)}
.listening-edit__actions{display:flex;gap:8px}.listening-edit__explain{margin-bottom:18px}
.listening-edit__layout{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:24px;align-items:start}.listening-edit__main{display:flex;min-width:0;flex-direction:column;gap:18px}
.listening-edit__side{display:flex;flex-direction:column;gap:14px;border-left:1px solid var(--border);padding-left:24px}
.le-field{display:flex;flex-direction:column;gap:7px}.le-field label{font-size:13px;font-weight:600;color:var(--text-primary)}
.le-section-title{display:flex;align-items:flex-end;justify-content:space-between;gap:12px}.le-section-title>div{display:flex;flex-direction:column;gap:4px}.le-section-title small{color:var(--text-muted)}
.le-media{display:flex;flex-direction:column;gap:8px}.le-media img{width:100%;height:120px;object-fit:cover;border-radius:10px}.le-media audio{width:100%}
.le-segments{display:flex;flex-direction:column;gap:12px}.le-segment{display:flex;flex-direction:column;gap:9px;padding:12px;border:1px solid var(--border);border-radius:12px;background:var(--bg-surface)}
.le-segment header{display:flex;align-items:center;justify-content:space-between}.le-segment nav{display:flex;gap:3px}.le-segment__times{display:grid;grid-template-columns:minmax(150px,1fr) auto auto minmax(150px,1fr) auto;align-items:end;gap:7px}.le-segment__times label{display:flex;flex-direction:column;gap:4px}
.le-empty{margin:0;padding:18px;border:1px dashed var(--border);border-radius:10px;color:var(--text-muted);text-align:center}.le-segments__actions{display:flex;justify-content:flex-end;gap:8px}
.le-pair-form{display:grid;grid-template-columns:1fr 120px auto;gap:6px}.le-pair{display:flex;align-items:center;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--border);font-size:12px}
.listening-edit__tags{margin-top:24px}.listening-edit__tags h2{margin:20px 0 8px;font-size:15px}
@media (max-width:980px){.listening-edit__layout{grid-template-columns:1fr}.listening-edit__side{border-left:0;padding-left:0}.le-segment__times{grid-template-columns:1fr auto}.le-segment__times>span{display:none}}
@media (max-width:720px){.listening-edit__bar{align-items:flex-start;flex-direction:column;gap:12px}.listening-edit__actions{flex-wrap:wrap}.le-segment header{align-items:flex-start;flex-direction:column}.le-segment__times{grid-template-columns:1fr}.le-pair-form{grid-template-columns:1fr}}
@media (prefers-reduced-motion:reduce){*{scroll-behavior:auto!important;transition-duration:.01ms!important}}
</style>
