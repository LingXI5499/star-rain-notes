<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import {
  createTaxonomy,
  deleteTaxonomy,
  fetchTaxonomy,
  updateTaxonomy,
  type TaxonomyDimension,
  type TaxonomyPayload,
  type TaxonomyTerm,
} from '@/api/englishMeta'

const terms = ref<TaxonomyTerm[]>([])
const loading = ref(true)
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const dimensions: TaxonomyDimension[] = ['TOPIC', 'SCENE', 'FUNCTION', 'ABILITY', 'GENRE', 'FORMAT']

const form = reactive({
  dimension: 'TOPIC' as TaxonomyDimension,
  parentId: null as number | null,
  name: '',
  slug: '',
  description: '',
  sortOrder: 10,
  enabled: true,
})

const dimensionOptions = computed(() =>
  terms.value
    .filter((t) => t.parentId === null && t.dimension === form.dimension)
    .map((t) => ({ value: t.id, label: t.name })),
)

const roots = computed(() => terms.value.filter((t) => t.parentId === null))
const childrenOf = (id: number) => terms.value.filter((t) => t.parentId === id)

async function load() {
  loading.value = true
  try {
    terms.value = await fetchTaxonomy('tree')
  } catch {
    ElMessage.error('加载语义标签失败。')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    dimension: 'TOPIC',
    parentId: null,
    name: '',
    slug: '',
    description: '',
    sortOrder: 10,
    enabled: true,
  })
}

function openCreate(dimension: TaxonomyDimension = 'TOPIC', parentId: number | null = null) {
  resetForm()
  form.dimension = dimension
  form.parentId = parentId
  dialogOpen.value = true
}

function openEdit(term: TaxonomyTerm) {
  editingId.value = term.id
  Object.assign(form, {
    dimension: term.dimension,
    parentId: term.parentId,
    name: term.name,
    slug: term.slug,
    description: term.description ?? '',
    sortOrder: term.sortOrder,
    enabled: term.enabled,
  })
  dialogOpen.value = true
}

async function save() {
  if (!form.name.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写名称与稳定 slug。')
    return
  }
  saving.value = true
  const payload: TaxonomyPayload = {
    dimension: form.dimension,
    parentId: form.parentId,
    name: form.name.trim(),
    slug: form.slug.trim(),
    description: form.description.trim() || null,
    sortOrder: form.sortOrder,
    enabled: form.enabled,
  }
  try {
    if (editingId.value) {
      await updateTaxonomy(editingId.value, payload)
      ElMessage.success('已更新。')
    } else {
      await createTaxonomy(payload)
      ElMessage.success('已创建。')
    }
    dialogOpen.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

async function remove(term: TaxonomyTerm) {
  try {
    await ElMessageBox.confirm(`确定删除「${term.name}」？`, '删除确认', { type: 'warning' })
    await deleteTaxonomy(term.id)
    ElMessage.success('已删除。')
    await load()
  } catch (error) {
    const detail = (error as { response?: { data?: ProblemDetail } }).response?.data?.detail
    if (detail) ElMessage.error(detail)
  }
}

onMounted(load)
</script>

<template>
  <section class="taxonomy-manager">
    <header class="taxonomy-manager__header">
      <div>
        <p>SHARED TAXONOMY · 共享语义标签</p>
        <h1>标签管理</h1>
        <span>统一主题、场景、功能、能力、文体与形式六类标签，最多两级且同维父子。</span>
      </div>
      <el-button type="primary" @click="openCreate()">新建标签</el-button>
    </header>

    <div v-loading="loading" class="taxonomy-manager__body">
      <div v-for="dimension in dimensions" :key="dimension" class="taxonomy-panel">
        <h2 class="taxonomy-panel__title">{{ dimension }}</h2>
        <div class="taxonomy-panel__list">
          <div v-for="root in roots.filter((t) => t.dimension === dimension)" :key="root.id" class="taxonomy-row">
            <div class="taxonomy-row__main">
              <span class="taxonomy-row__name">{{ root.name }}</span>
              <span class="taxonomy-row__slug">{{ root.slug }}</span>
              <span v-if="root.description" class="taxonomy-row__desc">{{ root.description }}</span>
            </div>
            <div class="taxonomy-row__actions">
              <el-button link type="primary" @click="openEdit(root)">编辑</el-button>
              <el-button link type="danger" @click="remove(root)">删除</el-button>
              <el-button link @click="openCreate(dimension, root.id)">+ 子级</el-button>
            </div>
            <div v-if="root.children?.length" class="taxonomy-row__children">
              <div v-for="child in childrenOf(root.id)" :key="child.id" class="taxonomy-row taxonomy-row--child">
                <div class="taxonomy-row__main">
                  <span class="taxonomy-row__name">{{ child.name }}</span>
                  <span class="taxonomy-row__slug">{{ child.slug }}</span>
                </div>
                <div class="taxonomy-row__actions">
                  <el-button link type="primary" @click="openEdit(child)">编辑</el-button>
                  <el-button link type="danger" @click="remove(child)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑标签' : '新建标签'" width="520px">
      <el-form label-position="top">
        <el-form-item label="维度">
          <el-select v-model="form.dimension" style="width: 100%">
            <el-option v-for="d in dimensions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级（留空为顶级）">
          <el-select v-model="form.parentId" clearable placeholder="顶级" style="width: 100%">
            <el-option v-for="option in dimensionOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.taxonomy-manager__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}
.taxonomy-manager__header p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: 0.14em; margin: 0; }
.taxonomy-manager__header h1 { font-size: 28px; margin: 6px 0; }
.taxonomy-manager__header span { color: var(--text-secondary); font-size: 13px; }
.taxonomy-manager__body { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 380px), 1fr)); gap: var(--space-4); }
.taxonomy-panel { min-width: 0; border: 1px solid var(--border); border-radius: 16px; padding: 18px; background: var(--bg-surface); }
.taxonomy-panel__title { font-size: 15px; margin: 0 0 12px; color: var(--primary); }
.taxonomy-panel__list { display: flex; flex-direction: column; gap: 8px; }
.taxonomy-row { display: grid; grid-template-columns: minmax(0, 1fr) max-content; align-items: flex-start; gap: 8px; padding: 8px; border-radius: 8px; }
.taxonomy-row:hover { background: var(--bg-subtle); }
.taxonomy-row--child { margin-left: 18px; }
.taxonomy-row__main { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.taxonomy-row__name { font-weight: 600; font-size: 14px; }
.taxonomy-row__slug { font-size: 11px; color: var(--text-muted); overflow-wrap: anywhere; }
.taxonomy-row__desc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; overflow-wrap: anywhere; }
.taxonomy-row__actions { display: flex; gap: 4px; align-items: center; white-space: nowrap; }
.taxonomy-row__children { grid-column: 1 / -1; display: flex; flex-direction: column; gap: 4px; width: 100%; }
@media (max-width: 720px) {
  .taxonomy-manager__header { align-items: flex-start; flex-direction: column; gap: 12px; }
  .taxonomy-manager__header span { display: block; line-height: 1.6; }
  .taxonomy-panel { padding: 14px; }
  .taxonomy-row { grid-template-columns: minmax(0, 1fr); }
  .taxonomy-row__actions, .taxonomy-row__children { grid-column: 1; }
  .taxonomy-row__actions { justify-content: flex-start; }
  .taxonomy-row--child { margin-left: 10px; }
}
</style>
