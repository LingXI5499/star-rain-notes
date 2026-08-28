<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/index.mjs'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import SemanticTagPicker from '@/components/english/SemanticTagPicker.vue'
import PublishChecklistDrawer, { type PublishCheck } from '@/components/english/PublishChecklistDrawer.vue'
import type { ContentReview } from '@/api/account'
import type { ProblemDetail } from '@/api/http'
import type { MediaAsset } from '@/api/media'
import { fetchTaxonomy, type TaxonomyDimension, type TaxonomyTerm } from '@/api/englishMeta'
import {
  createWritingPrompt,
  createWritingResource,
  fetchWritingResources,
  getWritingPrompt,
  getWritingResource,
  updateWritingPrompt,
  updateWritingResource,
  type WritingResourceSummary,
} from '@/api/writing'

interface TemplateBlock extends Record<string, unknown> {
  id: string
  type: 'text' | 'textarea'
  label?: string
  placeholder?: string
  required?: boolean
}
interface RubricItem extends Record<string, unknown> {
  name: string
  maxScore: number
}

const route = useRoute()
const router = useRouter()
const isPrompt = computed(() => String(route.meta.kind) === 'prompt')
const id = computed(() => Number(route.params.id || 0))
const saving = ref(false)
const loading = ref(true)
const coverOpen = ref(false)
const checklistOpen = ref(false)
const coverUrl = ref<string | null>(null)
const taxonomy = ref<TaxonomyTerm[]>([])
const templateOptions = ref<WritingResourceSummary[]>([])
const modelOptions = ref<WritingResourceSummary[]>([])
const templateBlocks = ref<TemplateBlock[]>([])
const rubricItems = ref<RubricItem[]>([])
const checklistItems = ref<string[]>([])

const form = reactive({
  title: '',
  summary: '',
  bodyMarkdown: '',
  backgroundMarkdown: '',
  requirementsMarkdown: '',
  cefrLevel: 'B1',
  resourceKind: 'EXPRESSION_LESSON',
  expressionLevel: 'SENTENCE',
  wordMin: 0,
  wordMax: 300,
  estimatedMinutes: 15,
  templateResourceId: null as number | null,
  modelResourceId: null as number | null,
  coverMediaId: null as number | null,
  sortOrder: null as number | null,
})
const tagIds = reactive<Record<'TOPIC' | 'GENRE' | 'FUNCTION' | 'ABILITY', number[]>>({
  TOPIC: [],
  GENRE: [],
  FUNCTION: [],
  ABILITY: [],
})

const allTagIds = computed(() => [...new Set(Object.values(tagIds).flat())])
const templateJson = computed(() => form.resourceKind === 'TEMPLATE'
  ? JSON.stringify({ version: 1, blocks: templateBlocks.value }, null, 2)
  : null)
const rubricJson = computed(() => rubricItems.value.length ? JSON.stringify(rubricItems.value, null, 2) : null)
const checklistJson = computed(() => {
  const values = checklistItems.value.map((item) => item.trim()).filter(Boolean)
  return values.length ? JSON.stringify([...new Set(values)], null, 2) : null
})
const rubricTotal = computed(() => rubricItems.value.reduce((sum, item) => sum + Number(item.maxScore || 0), 0))
const hasRequiredTag = computed(() => tagIds.TOPIC.length > 0 || tagIds.GENRE.length > 0)
const checks = computed<PublishCheck[]>(() => {
  const common: PublishCheck[] = [
    { key: 'title', label: '标题完整', passed: !!form.title.trim() },
    { key: 'summary', label: '摘要完整', passed: !!form.summary.trim() },
    { key: 'cefr', label: 'CEFR 已选择', passed: !!form.cefrLevel },
    { key: 'tags', label: '至少选择一个主题或文体标签', passed: hasRequiredTag.value },
    { key: 'range', label: '词数和预计时间有效', passed: form.wordMin >= 0 && form.wordMax >= form.wordMin && form.estimatedMinutes >= 0 },
  ]
  if (isPrompt.value) {
    return [
      ...common,
      { key: 'background', label: '写作背景完整', passed: !!form.backgroundMarkdown.trim() },
      { key: 'requirements', label: '写作要求完整', passed: !!form.requirementsMarkdown.trim() },
      { key: 'rubric', label: '评分量表总分不超过 100', passed: rubricTotal.value <= 100 },
      { key: 'template', label: '所选模板已经发布', passed: !form.templateResourceId || templateOptions.value.some((item) => item.id === form.templateResourceId) },
      { key: 'model', label: '所选范文已经发布', passed: !form.modelResourceId || modelOptions.value.some((item) => item.id === form.modelResourceId) },
    ]
  }
  return [
    ...common,
    { key: 'body', label: '正文完整', passed: !!form.bodyMarkdown.trim() },
    { key: 'template', label: '模板至少包含一个有效区块', passed: form.resourceKind !== 'TEMPLATE' || (templateBlocks.value.length > 0 && templateBlocks.value.every((block) => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(block.id))) },
  ]
})

function isReview(value: unknown): value is ContentReview {
  return typeof value === 'object' && value !== null && 'contentType' in value
}
function terms(dimension: TaxonomyDimension) {
  return taxonomy.value.filter((term) => term.dimension === dimension && term.parentId === null)
}
function parseTemplate(raw?: string | null) {
  if (!raw) return
  try {
    const parsed = JSON.parse(raw) as { blocks?: TemplateBlock[] }
    templateBlocks.value = Array.isArray(parsed.blocks) ? parsed.blocks.map((block) => ({ ...block })) : []
  } catch {
    ElMessage.warning('历史模板结构无法解析，请在保存前重新配置。')
  }
}
function parseRubric(raw?: string | null) {
  if (!raw) return
  try {
    const parsed = JSON.parse(raw) as RubricItem[]
    rubricItems.value = Array.isArray(parsed) ? parsed.map((item) => ({ ...item })) : []
  } catch {
    ElMessage.warning('历史评分量表无法解析，请在保存前重新配置。')
  }
}
function parseChecklist(raw?: string | null) {
  if (!raw) return
  try {
    const parsed = JSON.parse(raw) as string[]
    checklistItems.value = Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    ElMessage.warning('历史自检清单无法解析，请在保存前重新配置。')
  }
}
function assignTags(tags: Array<{ id: number; dimension: string }>) {
  for (const dimension of Object.keys(tagIds) as Array<keyof typeof tagIds>) {
    tagIds[dimension] = tags.filter((tag) => tag.dimension === dimension).map((tag) => tag.id)
  }
}

async function load() {
  loading.value = true
  try {
    const [tree, templates, models] = await Promise.all([
      fetchTaxonomy('tree'),
      fetchWritingResources({ page: 1, pageSize: 50, status: 'PUBLISHED', kind: 'TEMPLATE' }),
      fetchWritingResources({ page: 1, pageSize: 50, status: 'PUBLISHED', kind: 'MODEL_ESSAY' }),
    ])
    taxonomy.value = tree
    templateOptions.value = templates.items
    modelOptions.value = models.items
    if (id.value) {
      if (isPrompt.value) {
        const item = await getWritingPrompt(id.value)
        Object.assign(form, {
          title: item.title,
          summary: item.summary,
          backgroundMarkdown: item.backgroundMarkdown,
          requirementsMarkdown: item.requirementsMarkdown,
          cefrLevel: item.cefrLevel,
          wordMin: item.wordMin,
          wordMax: item.wordMax,
          estimatedMinutes: item.estimatedMinutes,
          templateResourceId: item.templateResourceId ?? null,
          modelResourceId: item.modelResourceId ?? null,
          coverMediaId: item.coverMediaId ?? null,
          sortOrder: item.sortOrder,
        })
        coverUrl.value = item.coverUrl ?? null
        assignTags(item.tags)
        parseRubric(item.rubricJson)
        parseChecklist(item.checklistJson)
      } else {
        const item = await getWritingResource(id.value)
        Object.assign(form, {
          title: item.title,
          summary: item.summary,
          bodyMarkdown: item.bodyMarkdown,
          cefrLevel: item.cefrLevel,
          resourceKind: item.resourceKind,
          expressionLevel: item.expressionLevel ?? '',
          wordMin: item.wordMin ?? 0,
          wordMax: item.wordMax ?? 0,
          estimatedMinutes: item.estimatedMinutes,
          coverMediaId: item.coverMediaId ?? null,
          sortOrder: item.sortOrder,
        })
        coverUrl.value = item.coverUrl ?? null
        assignTags(item.tags)
        parseTemplate(item.templateSchemaJson)
      }
    }
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '加载写作内容失败。')
  } finally {
    loading.value = false
  }
}

function validateSave() {
  const saveChecks: PublishCheck[] = [
    { key: 'title', label: '请填写标题', passed: !!form.title.trim() },
    { key: 'summary', label: '请填写摘要', passed: !!form.summary.trim() },
    { key: 'cefr', label: '请选择 CEFR 等级', passed: !!form.cefrLevel },
    { key: 'range', label: '请检查词数范围和预计时间', passed: form.wordMin >= 0 && form.wordMax >= form.wordMin && form.estimatedMinutes >= 0 },
  ]
  if (isPrompt.value) {
    saveChecks.push(
      { key: 'background', label: '请填写写作背景', passed: !!form.backgroundMarkdown.trim() },
      { key: 'requirements', label: '请填写写作要求', passed: !!form.requirementsMarkdown.trim() },
      { key: 'rubric', label: '评分量表总分不能超过 100', passed: rubricTotal.value <= 100 },
    )
  } else {
    saveChecks.push(
      { key: 'body', label: '请填写正文', passed: !!form.bodyMarkdown.trim() },
      { key: 'template', label: '模板至少需要一个编号有效且不重复的区块', passed: form.resourceKind !== 'TEMPLATE' || (templateBlocks.value.length > 0 && templateBlocks.value.every((block, index, blocks) => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(block.id) && blocks.findIndex((item) => item.id === block.id) === index)) },
    )
  }
  const failed = saveChecks.find((check) => !check.passed)
  if (failed) {
    ElMessage.warning(failed.label)
    return false
  }
  return true
}

async function save() {
  if (!validateSave()) return
  saving.value = true
  try {
    let result
    if (isPrompt.value) {
      const payload = {
        title: form.title.trim(),
        summary: form.summary.trim(),
        backgroundMarkdown: form.backgroundMarkdown,
        requirementsMarkdown: form.requirementsMarkdown,
        cefrLevel: form.cefrLevel,
        wordMin: form.wordMin,
        wordMax: form.wordMax,
        estimatedMinutes: form.estimatedMinutes,
        rubricJson: rubricJson.value,
        checklistJson: checklistJson.value,
        templateResourceId: form.templateResourceId,
        modelResourceId: form.modelResourceId,
        coverMediaId: form.coverMediaId,
        sortOrder: form.sortOrder ?? undefined,
        tagIds: allTagIds.value,
      }
      result = id.value ? await updateWritingPrompt(id.value, payload) : await createWritingPrompt(payload)
    } else {
      const payload = {
        resourceKind: form.resourceKind,
        expressionLevel: form.resourceKind === 'EXPRESSION_LESSON' ? form.expressionLevel : null,
        title: form.title.trim(),
        summary: form.summary.trim(),
        bodyMarkdown: form.bodyMarkdown,
        coverMediaId: form.coverMediaId,
        cefrLevel: form.cefrLevel,
        wordMin: form.wordMin,
        wordMax: form.wordMax,
        estimatedMinutes: form.estimatedMinutes,
        templateSchemaJson: templateJson.value,
        sortOrder: form.sortOrder ?? undefined,
        tagIds: allTagIds.value,
      }
      result = id.value ? await updateWritingResource(id.value, payload) : await createWritingResource(payload)
    }
    ElMessage.success(isReview(result) ? '已提交审核，超级管理员批准后会应用到线上写作内容。' : '已保存。')
    await router.push(String(route.query.return || '/admin/english/writing'))
  } catch (error: unknown) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

function addTemplateBlock() {
  templateBlocks.value.push({ id: `block-${templateBlocks.value.length + 1}`, type: 'textarea', label: '' })
}
function addRubricItem() {
  rubricItems.value.push({ name: '', maxScore: 10 })
}
function onCover(asset: MediaAsset) {
  form.coverMediaId = asset.id
  coverUrl.value = asset.publicUrl
}
function removeCover() {
  form.coverMediaId = null
  coverUrl.value = null
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="writing-edit">
    <header>
      <div><p>ENGLISH WRITING · {{ isPrompt ? 'PRACTICE' : 'RESOURCE' }}</p><h1>{{ id ? '编辑' : '新建' }}{{ isPrompt ? '写作任务' : '写作资源' }}</h1><span>编号由系统自动生成；发布前至少选择主题或文体标签。</span></div>
      <nav><el-button @click="checklistOpen = true">发布检查</el-button><el-button @click="router.push(String(route.query.return || '/admin/english/writing'))">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></nav>
    </header>

    <div class="layout">
      <main>
        <label>标题<el-input v-model="form.title" /></label>
        <label>摘要<el-input v-model="form.summary" type="textarea" :rows="3" /></label>
        <template v-if="isPrompt">
          <label>写作背景<MarkdownEditor v-model="form.backgroundMarkdown" placeholder="交代真实写作情境、身份、对象与目的…" /></label>
          <label>写作要求<MarkdownEditor v-model="form.requirementsMarkdown" placeholder="列出必须覆盖的信息、文体、语气和结构要求…" /></label>
        </template>
        <label v-else>正文（Markdown）<MarkdownEditor v-model="form.bodyMarkdown" /></label>

        <section v-if="!isPrompt && form.resourceKind === 'TEMPLATE'" class="editor-card">
          <div class="editor-card__title"><div><h2>模板填写区块</h2><p>区块编号只能使用小写字母、数字和连字符。</p></div><el-button size="small" @click="addTemplateBlock">新增区块</el-button></div>
          <article v-for="(block, index) in templateBlocks" :key="index" class="row-editor">
            <el-input v-model="block.id" placeholder="区块编号，如 opening" />
            <el-input v-model="block.label" placeholder="显示名称" />
            <el-select v-model="block.type"><el-option label="单行文本" value="text" /><el-option label="多行文本" value="textarea" /></el-select>
            <el-checkbox v-model="block.required">必填</el-checkbox>
            <el-button link type="danger" @click="templateBlocks.splice(index, 1)">删除</el-button>
          </article>
          <details><summary>高级 JSON 预览</summary><pre>{{ templateJson }}</pre></details>
        </section>

        <template v-if="isPrompt">
          <section class="editor-card">
            <div class="editor-card__title"><div><h2>评分标准</h2><p>各项分值总和不得超过 100；当前总分 {{ rubricTotal }}。</p></div><el-button size="small" @click="addRubricItem">新增评分项</el-button></div>
            <article v-for="(item, index) in rubricItems" :key="index" class="row-editor row-editor--rubric">
              <el-input v-model="item.name" placeholder="评分项名称" />
              <el-input-number v-model="item.maxScore" :min="1" :max="100" />
              <el-button link type="danger" @click="rubricItems.splice(index, 1)">删除</el-button>
            </article>
            <details><summary>高级 JSON 预览</summary><pre>{{ rubricJson || '[]' }}</pre></details>
          </section>
          <section class="editor-card">
            <div class="editor-card__title"><div><h2>提交前自检清单</h2><p>学习者提交作文前逐项确认。</p></div><el-button size="small" @click="checklistItems.push('')">新增检查项</el-button></div>
            <article v-for="(_item, index) in checklistItems" :key="index" class="row-editor row-editor--checklist">
              <el-input v-model="checklistItems[index]" placeholder="例如：我已经检查主谓一致。" />
              <el-button link type="danger" @click="checklistItems.splice(index, 1)">删除</el-button>
            </article>
            <details><summary>高级 JSON 预览</summary><pre>{{ checklistJson || '[]' }}</pre></details>
          </section>
        </template>

        <section class="tag-section">
          <h2>主题标签（与文体标签至少选择一项）</h2><SemanticTagPicker v-model="tagIds.TOPIC" :terms="terms('TOPIC')" dimension="TOPIC" />
          <h2>文体标签（与主题标签至少选择一项）</h2><SemanticTagPicker v-model="tagIds.GENRE" :terms="terms('GENRE')" dimension="GENRE" />
          <h2>功能标签</h2><SemanticTagPicker v-model="tagIds.FUNCTION" :terms="terms('FUNCTION')" dimension="FUNCTION" />
          <h2>能力标签</h2><SemanticTagPicker v-model="tagIds.ABILITY" :terms="terms('ABILITY')" dimension="ABILITY" />
        </section>
      </main>

      <aside>
        <label>CEFR<el-select v-model="form.cefrLevel"><el-option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level" :label="level" :value="level" /></el-select></label>
        <template v-if="!isPrompt">
          <label>资源类型<el-select v-model="form.resourceKind"><el-option label="表达训练" value="EXPRESSION_LESSON" /><el-option label="文体课程" value="GENRE_LESSON" /><el-option label="范文" value="MODEL_ESSAY" /><el-option label="模板" value="TEMPLATE" /></el-select></label>
          <label v-if="form.resourceKind === 'EXPRESSION_LESSON'">表达层级<el-select v-model="form.expressionLevel"><el-option label="句子" value="SENTENCE" /><el-option label="段落" value="PARAGRAPH" /><el-option label="衔接" value="COHESION" /><el-option label="风格" value="STYLE" /></el-select></label>
        </template>
        <template v-else>
          <label>关联写作模板<el-select v-model="form.templateResourceId" clearable filterable placeholder="可选，仅显示已发布模板"><el-option v-for="item in templateOptions" :key="item.id" :label="item.title" :value="item.id" /></el-select></label>
          <label>关联范文<el-select v-model="form.modelResourceId" clearable filterable placeholder="可选，仅显示已发布范文"><el-option v-for="item in modelOptions" :key="item.id" :label="item.title" :value="item.id" /></el-select></label>
        </template>
        <label>最少词数<el-input-number v-model="form.wordMin" :min="0" /></label>
        <label>最多词数<el-input-number v-model="form.wordMax" :min="0" /></label>
        <label>预计分钟<el-input-number v-model="form.estimatedMinutes" :min="0" /></label>
        <label>排序<el-input-number v-model="form.sortOrder" :min="0" :step="10" placeholder="留空自动追加" /></label>
        <label>封面
          <span class="cover-field"><img v-if="coverUrl" :src="coverUrl" alt="" /><i v-else>未选择封面</i><span><el-button size="small" @click="coverOpen = true">{{ coverUrl ? '重新选择' : '上传或选择' }}</el-button><el-button v-if="coverUrl" size="small" @click="removeCover">移除</el-button></span></span>
        </label>
      </aside>
    </div>

    <MediaPicker v-model="coverOpen" asset-type="IMAGE" allow-upload title="上传或选择写作封面" @select="onCover" />
    <PublishChecklistDrawer :open="checklistOpen" :checks="checks" @close="checklistOpen = false" />
  </section>
</template>

<style scoped>
.writing-edit{max-width:1380px;margin:auto}header{position:sticky;z-index:4;top:0;display:flex;align-items:end;justify-content:space-between;gap:16px;padding:18px 0;background:var(--bg-page)}header p{margin:0;color:var(--accent);font-size:11px;font-weight:800;letter-spacing:.14em}h1{margin:6px 0;font-size:28px}header span{color:var(--text-secondary);font-size:13px}header nav{display:flex;gap:8px}.layout{display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:24px}.layout main,.layout aside{display:flex;min-width:0;flex-direction:column;gap:18px}.layout aside{height:max-content;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}label{display:flex;flex-direction:column;gap:7px;color:var(--text-secondary);font-size:13px;font-weight:700}.editor-card,.tag-section{padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.editor-card__title{display:flex;align-items:center;justify-content:space-between;gap:12px}.editor-card h2,.tag-section h2{margin:0 0 7px;font-size:16px}.editor-card p{margin:0 0 12px;color:var(--text-muted);font-size:12px}.row-editor{display:grid;grid-template-columns:1fr 1fr 150px auto auto;gap:8px;align-items:center;margin-top:9px}.row-editor--rubric{grid-template-columns:1fr 160px auto}.row-editor--checklist{grid-template-columns:1fr auto}.editor-card details{margin-top:14px;color:var(--text-muted);font-size:12px}.editor-card pre{max-height:240px;overflow:auto;padding:12px;border-radius:10px;background:var(--bg-subtle);white-space:pre-wrap}.tag-section{display:flex;flex-direction:column;gap:10px}.tag-section h2:not(:first-child){margin-top:12px}.cover-field{display:flex;flex-direction:column;gap:8px}.cover-field img,.cover-field i{display:grid;width:100%;height:130px;place-items:center;border-radius:10px;background:var(--bg-subtle);object-fit:cover;color:var(--text-muted);font-style:normal}.cover-field>span{display:flex;gap:6px}@media(max-width:980px){.layout{grid-template-columns:1fr}.layout aside{order:-1}.row-editor{grid-template-columns:1fr 1fr}.row-editor>*:last-child{justify-self:start}}@media(max-width:620px){header{align-items:flex-start;flex-direction:column}header span{display:none}.row-editor,.row-editor--rubric,.row-editor--checklist{grid-template-columns:1fr}}
</style>
