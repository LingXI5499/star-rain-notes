<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import CefrBadge from '@/components/english/CefrBadge.vue'
import {
  createBundle,
  deleteBundle,
  fetchBundles,
  publishBundle,
  updateBundle,
  withdrawBundle,
  type BundlePayload,
  type LearningBundle,
} from '@/api/englishBundle'

const bundles = ref<LearningBundle[]>([])
const loading = ref(true)
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  primaryCefr: '' as string,
  sortOrder: 10,
})

async function load() {
  loading.value = true
  try {
    bundles.value = await fetchBundles()
  } catch {
    ElMessage.error('加载学习组合失败。')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { title: '', slug: '', summary: '', primaryCefr: '', sortOrder: 10 })
}

function openCreate() {
  resetForm()
  dialogOpen.value = true
}

function openEdit(bundle: LearningBundle) {
  editingId.value = bundle.id
  Object.assign(form, {
    title: bundle.title,
    slug: bundle.slug,
    summary: bundle.summary ?? '',
    primaryCefr: bundle.primaryCefr ?? '',
    sortOrder: bundle.sortOrder,
  })
  dialogOpen.value = true
}

async function save() {
  if (!form.title.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写标题与 slug。')
    return
  }
  saving.value = true
  const payload: BundlePayload = {
    title: form.title.trim(),
    slug: form.slug.trim(),
    summary: form.summary.trim() || null,
    primaryCefr: form.primaryCefr || null,
    sortOrder: form.sortOrder,
  }
  try {
    if (editingId.value) {
      await updateBundle(editingId.value, payload)
      ElMessage.success('已更新。')
    } else {
      await createBundle(payload)
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

async function setPublished(bundle: LearningBundle, published: boolean) {
  try {
    if (published) {
      await publishBundle(bundle.id)
      ElMessage.success('已发布。')
    } else {
      await withdrawBundle(bundle.id)
      ElMessage.success('已撤回。')
    }
    await load()
  } catch (error) {
    ElMessage.error((error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? '操作失败。')
  }
}

async function remove(bundle: LearningBundle) {
  try {
    await ElMessageBox.confirm(`确定删除「${bundle.title}」？`, '删除确认', { type: 'warning' })
    await deleteBundle(bundle.id)
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
  <section class="bundle-manager">
    <header class="bundle-manager__header">
      <div>
        <p>LEARNING BUNDLES · 学习组合</p>
        <h1>学习组合</h1>
        <span>将阅读、听力与写作内容组织为显式的跨模块学习路径，按主题与 CEFR 关联。</span>
      </div>
      <el-button type="primary" @click="openCreate">新建组合</el-button>
    </header>

    <div v-loading="loading" class="bundle-manager__list">
      <p v-if="!loading && !bundles.length" class="bundle-manager__empty">暂无学习组合。</p>
      <div v-for="bundle in bundles" :key="bundle.id" class="bundle-row">
        <div class="bundle-row__main">
          <div class="bundle-row__top">
            <h3>{{ bundle.title }}</h3>
            <CefrBadge :level="bundle.primaryCefr" />
          </div>
          <p class="bundle-row__slug">{{ bundle.slug }} · {{ bundle.publishStatus }}</p>
          <p v-if="bundle.summary" class="bundle-row__summary">{{ bundle.summary }}</p>
        </div>
        <div class="bundle-row__actions">
          <el-button link type="primary" @click="openEdit(bundle)">编辑</el-button>
          <el-button v-if="bundle.publishStatus === 'PUBLISHED'" link type="warning" @click="setPublished(bundle, false)">撤回</el-button>
          <el-button v-else link type="success" @click="setPublished(bundle, true)">发布</el-button>
          <el-button link type="danger" @click="remove(bundle)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑组合' : '新建组合'" width="520px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="主 CEFR 等级">
          <el-select v-model="form.primaryCefr" clearable placeholder="选择等级" style="width: 100%">
            <el-option v-for="lvl in ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']" :key="lvl" :label="lvl" :value="lvl" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.bundle-manager__header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.bundle-manager__header p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: 0.14em; margin: 0; }
.bundle-manager__header h1 { font-size: 28px; margin: 6px 0; }
.bundle-manager__header span { color: var(--text-secondary); font-size: 13px; }
.bundle-manager__list { display: flex; flex-direction: column; gap: var(--space-3); }
.bundle-row { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-4); padding: 18px; border: 1px solid var(--border); border-radius: 16px; background: var(--bg-surface); }
.bundle-row__top { display: flex; align-items: center; gap: 10px; }
.bundle-row__top h3 { margin: 0; font-size: 17px; }
.bundle-row__slug { font-size: 12px; color: var(--text-muted); margin: 4px 0 0; }
.bundle-row__summary { font-size: 13px; color: var(--text-secondary); margin: 8px 0 0; }
.bundle-row__actions { display: flex; gap: 4px; flex-shrink: 0; }
.bundle-manager__empty { color: var(--text-muted); padding: var(--space-6) 0; }
</style>
