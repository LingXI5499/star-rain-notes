<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import type { MediaAsset } from '@/api/media'
import CefrBadge from '@/components/english/CefrBadge.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import {
  addBundleItem,
  createBundle,
  deleteBundle,
  fetchBundleCatalog,
  fetchBundleItems,
  fetchBundleReadiness,
  fetchBundles,
  moveBundleItem,
  publishBundle,
  removeBundleItem,
  updateBundle,
  withdrawBundle,
  type BundleCatalogItem,
  type BundleCatalogPage,
  type BundleItem,
  type BundlePayload,
  type BundleReadiness,
  type LearningBundle,
} from '@/api/englishBundle'

const moduleLabels: Record<BundleItem['contentType'], string> = {
  READING: '阅读', LISTENING: '听力', WRITING: '写作',
}
const issueLabels: Record<string, string> = {
  SUMMARY_REQUIRED: '补充组合摘要',
  CEFR_REQUIRED: '选择主 CEFR 等级',
  MINIMUM_ITEMS_REQUIRED: '至少加入 2 项内容',
  MULTIPLE_MODULES_REQUIRED: '至少覆盖 2 个不同模块',
  UNPUBLISHED_ITEMS_PRESENT: '组合中的所有内容必须已发布',
}

const bundles = ref<LearningBundle[]>([])
const readinessMap = ref<Record<number, BundleReadiness>>({})
const loading = ref(true)
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const mediaPickerOpen = ref(false)
const workspaceOpen = ref(false)
const workspaceLoading = ref(false)
const catalogLoading = ref(false)
const mutating = ref(false)
const activeBundle = ref<LearningBundle | null>(null)
const bundleItems = ref<BundleItem[]>([])
const readiness = ref<BundleReadiness | null>(null)
const catalog = ref<BundleCatalogPage>({ items: [], page: 1, pageSize: 12, total: 0, totalPages: 0 })
const dragging = ref<string | null>(null)
const filters = reactive({ q: '', type: '', status: 'PUBLISHED', cefr: '', page: 1 })

const form = reactive({
  title: '', slug: '', summary: '', primaryCefr: '', coverMediaId: null as number | null,
  coverUrl: '', sortOrder: 10,
})

const workspaceLocked = computed(() => activeBundle.value?.publishStatus === 'PUBLISHED')
const moduleSummary = computed(() => {
  if (!readiness.value) return []
  return (Object.keys(moduleLabels) as BundleItem['contentType'][]).map(type => ({
    type, label: moduleLabels[type], count: readiness.value?.moduleCounts[type] ?? 0,
  }))
})

function detail(error: unknown, fallback: string) {
  return (error as { response?: { data?: ProblemDetail } }).response?.data?.detail ?? fallback
}

async function load() {
  loading.value = true
  try {
    const list = await fetchBundles()
    bundles.value = list
    const pairs = await Promise.all(list.map(async bundle => [bundle.id, await fetchBundleReadiness(bundle.id)] as const))
    readinessMap.value = Object.fromEntries(pairs)
  } catch (error) {
    ElMessage.error(detail(error, '加载学习组合失败。'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { title: '', slug: '', summary: '', primaryCefr: '', coverMediaId: null, coverUrl: '', sortOrder: 10 })
}

function openCreate() {
  resetForm()
  dialogOpen.value = true
}

function openEdit(bundle: LearningBundle) {
  editingId.value = bundle.id
  Object.assign(form, {
    title: bundle.title, slug: bundle.slug, summary: bundle.summary ?? '', primaryCefr: bundle.primaryCefr ?? '',
    coverMediaId: bundle.coverMediaId, coverUrl: bundle.coverUrl ?? '', sortOrder: bundle.sortOrder,
  })
  dialogOpen.value = true
}

function selectCover(asset: MediaAsset) {
  if (asset.assetType !== 'IMAGE') {
    ElMessage.warning('学习组合封面必须使用图片。')
    return
  }
  form.coverMediaId = asset.id
  form.coverUrl = asset.publicUrl
}

async function save() {
  if (!form.title.trim() || !form.slug.trim()) {
    ElMessage.warning('请填写标题与 slug。')
    return
  }
  saving.value = true
  const payload: BundlePayload = {
    title: form.title.trim(), slug: form.slug.trim(), summary: form.summary.trim() || null,
    primaryCefr: form.primaryCefr || null, coverMediaId: form.coverMediaId, sortOrder: form.sortOrder,
  }
  try {
    if (editingId.value) await updateBundle(editingId.value, payload)
    else await createBundle(payload)
    ElMessage.success(editingId.value ? '组合信息已更新。' : '学习组合已创建。')
    dialogOpen.value = false
    await load()
  } catch (error) {
    ElMessage.error(detail(error, '保存失败。'))
  } finally {
    saving.value = false
  }
}

async function setPublished(bundle: LearningBundle, published: boolean) {
  try {
    if (published) {
      const check = await fetchBundleReadiness(bundle.id)
      readinessMap.value[bundle.id] = check
      if (!check.ready) {
        await manageItems(bundle)
        ElMessage.warning('组合尚未满足发布条件，请先完成右侧检查清单。')
        return
      }
      await ElMessageBox.confirm(`确认发布「${bundle.title}」？发布后需先撤回才能调整路径。`, '发布学习路径', { type: 'warning' })
      await publishBundle(bundle.id)
      ElMessage.success('学习组合已发布。')
    } else {
      await withdrawBundle(bundle.id)
      ElMessage.success('学习组合已撤回，可以继续编排。')
    }
    await load()
    if (workspaceOpen.value && activeBundle.value?.id === bundle.id) {
      const latest = bundles.value.find(item => item.id === bundle.id)
      if (latest) activeBundle.value = latest
      await loadWorkspace()
    }
  } catch (error) {
    const message = detail(error, '')
    if (message) ElMessage.error(message)
  }
}

async function remove(bundle: LearningBundle) {
  try {
    await ElMessageBox.confirm(`确定删除「${bundle.title}」？`, '删除确认', { type: 'warning' })
    await deleteBundle(bundle.id)
    ElMessage.success('已删除。')
    await load()
  } catch (error) {
    const message = detail(error, '')
    if (message) ElMessage.error(message)
  }
}

async function loadWorkspace() {
  if (!activeBundle.value) return
  workspaceLoading.value = true
  try {
    const [items, check] = await Promise.all([
      fetchBundleItems(activeBundle.value.id), fetchBundleReadiness(activeBundle.value.id),
    ])
    bundleItems.value = items
    readiness.value = check
    readinessMap.value[activeBundle.value.id] = check
    await loadCatalog()
  } catch (error) {
    ElMessage.error(detail(error, '加载组合编排失败。'))
  } finally {
    workspaceLoading.value = false
  }
}

async function loadCatalog(resetPage = false) {
  if (!activeBundle.value) return
  if (resetPage) filters.page = 1
  catalogLoading.value = true
  try {
    catalog.value = await fetchBundleCatalog(activeBundle.value.id, {
      q: filters.q.trim() || undefined, type: filters.type || undefined,
      status: filters.status || 'ALL', cefr: filters.cefr || undefined,
      page: filters.page, pageSize: 12,
    })
  } catch (error) {
    ElMessage.error(detail(error, '加载内容库失败。'))
  } finally {
    catalogLoading.value = false
  }
}

async function manageItems(bundle: LearningBundle) {
  activeBundle.value = bundle
  workspaceOpen.value = true
  Object.assign(filters, { q: '', type: '', status: 'PUBLISHED', cefr: '', page: 1 })
  await loadWorkspace()
}

async function addItem(item: BundleCatalogItem) {
  if (!activeBundle.value || item.selected || workspaceLocked.value) return
  mutating.value = true
  try {
    await addBundleItem(activeBundle.value.id, item.contentType, item.contentId)
    ElMessage.success(`已加入：${item.title}`)
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(detail(error, '加入组合失败。'))
  } finally {
    mutating.value = false
  }
}

async function moveItem(item: BundleItem, target: number) {
  if (!activeBundle.value || workspaceLocked.value) return
  mutating.value = true
  try {
    await moveBundleItem(activeBundle.value.id, item, target)
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(detail(error, '排序失败。'))
  } finally {
    mutating.value = false
  }
}

function itemKey(item: Pick<BundleItem, 'contentType' | 'contentId'>) {
  return `${item.contentType}:${item.contentId}`
}

async function dropItem(targetIndex: number) {
  const source = bundleItems.value.find(item => itemKey(item) === dragging.value)
  dragging.value = null
  if (source) await moveItem(source, targetIndex)
}

async function deleteItem(item: BundleItem) {
  if (!activeBundle.value || workspaceLocked.value) return
  try {
    await ElMessageBox.confirm(`从当前路径移除「${item.title}」？不会删除原内容。`, '移除内容', { type: 'warning' })
    await removeBundleItem(activeBundle.value.id, item)
    await loadWorkspace()
  } catch (error) {
    const message = detail(error, '')
    if (message) ElMessage.error(message)
  }
}

async function withdrawWorkspace() {
  if (!activeBundle.value) return
  await setPublished(activeBundle.value, false)
  const latest = bundles.value.find(item => item.id === activeBundle.value?.id)
  if (latest) activeBundle.value = latest
  await loadWorkspace()
}

onMounted(load)
</script>

<template>
  <section class="bundle-manager">
    <header class="bundle-manager__header">
      <div>
        <p>LEARNING BUNDLES · 内容运营</p>
        <h1>跨模块学习路径</h1>
        <span>从完整内容库检索阅读、听力与写作任务，按明确顺序编排并通过质量检查后发布。</span>
      </div>
      <el-button type="primary" @click="openCreate">新建组合</el-button>
    </header>

    <div v-loading="loading" class="bundle-grid">
      <div v-if="!loading && !bundles.length" class="bundle-manager__empty">
        <b>还没有学习组合</b><span>先创建一个主题路径，再加入至少两个模块的已发布内容。</span>
      </div>
      <article v-for="bundle in bundles" :key="bundle.id" class="bundle-card">
        <div class="bundle-card__cover" :style="bundle.coverUrl ? { backgroundImage: `url(${bundle.coverUrl})` } : undefined">
          <span>{{ bundle.title.slice(0, 1) }}</span>
          <em :class="bundle.publishStatus.toLowerCase()">{{ bundle.publishStatus === 'PUBLISHED' ? '已发布' : bundle.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</em>
        </div>
        <div class="bundle-card__body">
          <div class="bundle-card__title"><h2>{{ bundle.title }}</h2><CefrBadge :level="bundle.primaryCefr" /></div>
          <code>{{ bundle.slug }}</code>
          <p>{{ bundle.summary || '尚未填写摘要，发布前需要补充学习目标。' }}</p>
          <div v-if="readinessMap[bundle.id]" class="bundle-card__stats">
            <span><b>{{ readinessMap[bundle.id].totalItems }}</b> 项内容</span>
            <span><b>{{ readinessMap[bundle.id].moduleCount }}</b> 个模块</span>
            <span :class="{ ready: readinessMap[bundle.id].ready }">{{ readinessMap[bundle.id].ready ? '可发布' : `${readinessMap[bundle.id].issues.length} 项待完善` }}</span>
          </div>
          <nav>
            <el-button type="primary" @click="manageItems(bundle)">编排路径</el-button>
            <el-button @click="openEdit(bundle)">编辑信息</el-button>
            <el-button v-if="bundle.publishStatus === 'PUBLISHED'" type="warning" @click="setPublished(bundle, false)">撤回</el-button>
            <el-button v-else type="success" @click="setPublished(bundle, true)">发布</el-button>
            <RouterLink v-if="bundle.publishStatus === 'PUBLISHED'" :to="`/english/bundles/${bundle.slug}`" target="_blank">预览 ↗</RouterLink>
            <el-button link type="danger" @click="remove(bundle)">删除</el-button>
          </nav>
        </div>
      </article>
    </div>

    <el-drawer v-model="workspaceOpen" size="92%" :with-header="false" class="bundle-workspace-drawer">
      <div v-if="activeBundle" v-loading="workspaceLoading" class="workspace">
        <header class="workspace__header">
          <div><button type="button" @click="workspaceOpen = false">← 返回</button><p>PATH BUILDER · {{ activeBundle.slug }}</p><h2>{{ activeBundle.title }}</h2></div>
          <div class="workspace__header-actions">
            <span v-if="workspaceLocked" class="locked">已发布 · 路径已锁定</span>
            <el-button v-if="workspaceLocked" type="warning" @click="withdrawWorkspace">撤回后编辑</el-button>
            <el-button v-else :disabled="!readiness?.ready" type="success" @click="setPublished(activeBundle, true)">发布路径</el-button>
          </div>
        </header>

        <section class="readiness" :class="{ ready: readiness?.ready }">
          <div class="readiness__score"><b>{{ readiness?.ready ? '✓' : readiness?.issues.length ?? 0 }}</b><span>{{ readiness?.ready ? '发布检查通过' : '项需要处理' }}</span></div>
          <div class="readiness__modules"><span v-for="item in moduleSummary" :key="item.type"><b>{{ item.count }}</b>{{ item.label }}</span></div>
          <ul v-if="readiness && !readiness.ready"><li v-for="issue in readiness.issues" :key="issue">{{ issueLabels[issue] || issue }}</li></ul>
          <p v-else>摘要、CEFR、模块覆盖和内容发布状态均符合要求。</p>
        </section>

        <div class="workspace__columns">
          <section class="catalog-panel">
            <header><div><p>CONTENT LIBRARY</p><h3>内容库</h3></div><span>共 {{ catalog.total }} 项</span></header>
            <div class="catalog-filters">
              <el-input v-model="filters.q" clearable placeholder="搜索标题、slug 或摘要" @keyup.enter="loadCatalog(true)" />
              <el-select v-model="filters.type" placeholder="全部模块" clearable @change="loadCatalog(true)">
                <el-option label="阅读" value="READING" /><el-option label="听力" value="LISTENING" /><el-option label="写作" value="WRITING" />
              </el-select>
              <el-select v-model="filters.status" @change="loadCatalog(true)">
                <el-option label="已发布" value="PUBLISHED" /><el-option label="草稿" value="DRAFT" /><el-option label="已撤回" value="WITHDRAWN" /><el-option label="全部状态" value="ALL" />
              </el-select>
              <el-select v-model="filters.cefr" placeholder="全部 CEFR" clearable @change="loadCatalog(true)">
                <el-option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level" :label="level" :value="level" />
              </el-select>
              <el-button @click="loadCatalog(true)">搜索</el-button>
            </div>
            <div v-loading="catalogLoading" class="catalog-list">
              <article v-for="item in catalog.items" :key="itemKey(item)" :class="{ selected: item.selected }">
                <div class="catalog-cover" :style="item.coverUrl ? { backgroundImage: `url(${item.coverUrl})` } : undefined">{{ moduleLabels[item.contentType].slice(0, 1) }}</div>
                <div><span>{{ moduleLabels[item.contentType] }} · {{ item.cefrLevel || '—' }} · {{ item.publishStatus }}</span><h4>{{ item.title }}</h4><p>{{ item.summary || item.slug }}</p></div>
                <el-button :disabled="item.selected || workspaceLocked || mutating" :type="item.selected ? 'info' : 'primary'" @click="addItem(item)">{{ item.selected ? '已加入' : '加入' }}</el-button>
              </article>
              <p v-if="!catalogLoading && !catalog.items.length" class="empty">没有符合筛选条件的内容。</p>
            </div>
            <el-pagination v-if="catalog.totalPages > 1" v-model:current-page="filters.page" layout="prev, pager, next" :page-count="catalog.totalPages" @current-change="loadCatalog()" />
          </section>

          <section class="path-panel">
            <header><div><p>LEARNING PATH</p><h3>学习顺序</h3></div><span>{{ bundleItems.length }} 个步骤</span></header>
            <div class="path-list">
              <article v-for="(item,index) in bundleItems" :key="itemKey(item)" :draggable="!workspaceLocked" @dragstart="dragging = itemKey(item)" @dragover.prevent @drop.prevent="dropItem(index)">
                <i>{{ index + 1 }}</i><b class="handle">⠿</b>
                <div><span>{{ moduleLabels[item.contentType] }} · {{ item.cefrLevel || '—' }}</span><h4>{{ item.title }}</h4><small :class="item.publishStatus.toLowerCase()">{{ item.publishStatus === 'PUBLISHED' ? '已发布' : item.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</small></div>
                <nav><button :disabled="workspaceLocked || index === 0" @click="moveItem(item,index-1)">↑</button><button :disabled="workspaceLocked || index === bundleItems.length-1" @click="moveItem(item,index+1)">↓</button><button :disabled="workspaceLocked" class="danger" @click="deleteItem(item)">移除</button></nav>
              </article>
              <div v-if="!bundleItems.length" class="path-empty"><b>路径还是空的</b><span>从左侧内容库加入阅读、听力或写作任务。</span></div>
            </div>
          </section>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑组合信息' : '新建学习组合'" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="稳定 Slug"><el-input v-model="form.slug" placeholder="climate-input-to-output" /></el-form-item>
        <el-form-item label="学习目标摘要"><el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        <div class="form-grid"><el-form-item label="主 CEFR 等级"><el-select v-model="form.primaryCefr" clearable style="width:100%"><el-option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level" :label="level" :value="level" /></el-select></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" :step="10" /></el-form-item></div>
        <el-form-item label="封面">
          <div class="cover-field"><img v-if="form.coverUrl" :src="form.coverUrl" alt="组合封面" /><div v-else>暂无封面</div><span><el-button @click="mediaPickerOpen = true">选择图片</el-button><el-button v-if="form.coverMediaId" @click="form.coverMediaId = null; form.coverUrl = ''">移除</el-button></span></div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
    <MediaPicker v-model="mediaPickerOpen" @select="selectCover" />
  </section>
</template>

<style scoped>
.bundle-manager__header{display:flex;justify-content:space-between;align-items:flex-end;gap:30px;margin-bottom:26px}.bundle-manager__header p,.workspace p,.catalog-panel header p,.path-panel header p{margin:0;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.15em}.bundle-manager__header h1{margin:7px 0;font-size:32px}.bundle-manager__header span{color:var(--text-secondary);font-size:13px}.bundle-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(360px,1fr));gap:18px;min-height:180px}.bundle-manager__empty{grid-column:1/-1;display:grid;min-height:260px;place-items:center;align-content:center;gap:8px;border:1px dashed var(--border);border-radius:20px;color:var(--text-muted)}.bundle-manager__empty b{color:var(--text-primary);font-size:18px}.bundle-card{overflow:hidden;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface);transition:transform .16s ease,border-color .16s ease}.bundle-card:hover{transform:translateY(-2px);border-color:color-mix(in srgb,var(--primary) 45%,var(--border))}.bundle-card__cover{position:relative;display:flex;height:125px;align-items:center;justify-content:center;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-subtle)),color-mix(in srgb,var(--accent) 13%,var(--bg-surface)));background-position:center;background-size:cover}.bundle-card__cover>span{color:var(--primary);font-size:42px;font-weight:900}.bundle-card__cover em{position:absolute;top:14px;right:14px;padding:6px 10px;border-radius:999px;background:var(--bg-surface);font-size:10px;font-style:normal}.bundle-card__cover em.published{color:var(--success)}.bundle-card__cover em.withdrawn{color:var(--warning)}.bundle-card__body{padding:20px}.bundle-card__title{display:flex;align-items:center;justify-content:space-between;gap:12px}.bundle-card h2{margin:0;font-size:20px}.bundle-card code{display:block;margin-top:5px;color:var(--text-muted);font-size:11px}.bundle-card p{min-height:42px;color:var(--text-secondary);font-size:13px;line-height:1.6}.bundle-card__stats{display:flex;gap:8px;margin:16px 0;padding:11px;border-radius:12px;background:var(--bg-subtle);font-size:11px}.bundle-card__stats span{color:var(--text-muted)}.bundle-card__stats b{margin-right:3px;color:var(--text-primary)}.bundle-card__stats span:last-child{margin-left:auto;color:var(--warning)}.bundle-card__stats span.ready{color:var(--success)}.bundle-card nav{display:flex;align-items:center;gap:7px;flex-wrap:wrap}.bundle-card nav>a{margin-left:auto;color:var(--primary);font-size:12px}.workspace{min-height:100vh;padding:26px;background:var(--bg-page)}.workspace__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding-bottom:20px;border-bottom:1px solid var(--border)}.workspace__header button{border:0;background:none;color:var(--text-muted);cursor:pointer}.workspace__header h2{margin:7px 0 0;font-size:28px}.workspace__header-actions{display:flex;align-items:center;gap:12px}.locked{padding:8px 12px;border-radius:999px;background:color-mix(in srgb,var(--warning) 12%,var(--bg-surface));color:var(--warning);font-size:11px}.readiness{display:grid;grid-template-columns:auto auto minmax(240px,1fr);gap:24px;align-items:center;margin:20px 0;padding:18px;border:1px solid color-mix(in srgb,var(--warning) 45%,var(--border));border-radius:18px;background:color-mix(in srgb,var(--warning) 5%,var(--bg-surface))}.readiness.ready{border-color:color-mix(in srgb,var(--success) 45%,var(--border));background:color-mix(in srgb,var(--success) 5%,var(--bg-surface))}.readiness__score{display:flex;align-items:center;gap:10px}.readiness__score b{display:grid;width:40px;height:40px;place-items:center;border-radius:12px;background:var(--warning);color:white;font-size:20px}.ready .readiness__score b{background:var(--success)}.readiness__score span{font-size:12px}.readiness__modules{display:flex;gap:8px}.readiness__modules span{padding:8px 10px;border-radius:10px;background:var(--bg-surface);color:var(--text-muted);font-size:11px}.readiness__modules b{margin-right:4px;color:var(--text-primary)}.readiness ul{display:flex;justify-content:flex-end;gap:6px;flex-wrap:wrap;margin:0;padding:0;list-style:none}.readiness li{padding:5px 8px;border-radius:999px;background:var(--bg-surface);color:var(--warning);font-size:10px}.readiness>p{margin:0;text-align:right;color:var(--success);font-size:12px}.workspace__columns{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(360px,.85fr);gap:18px}.catalog-panel,.path-panel{min-width:0;padding:20px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}.catalog-panel>header,.path-panel>header{display:flex;align-items:end;justify-content:space-between;margin-bottom:16px}.catalog-panel h3,.path-panel h3{margin:5px 0 0;font-size:20px}.catalog-panel header>span,.path-panel header>span{color:var(--text-muted);font-size:11px}.catalog-filters{display:grid;grid-template-columns:minmax(180px,1fr) 110px 110px 105px auto;gap:8px;margin-bottom:14px}.catalog-list{display:flex;min-height:260px;flex-direction:column;gap:9px}.catalog-list article{display:grid;grid-template-columns:64px minmax(0,1fr) auto;gap:13px;align-items:center;padding:10px;border:1px solid var(--border);border-radius:14px;transition:border-color .16s ease,opacity .16s ease}.catalog-list article.selected{opacity:.58}.catalog-cover{display:grid;width:64px;height:56px;place-items:center;border-radius:10px;background:linear-gradient(135deg,var(--bg-subtle),color-mix(in srgb,var(--primary) 10%,var(--bg-subtle)));color:var(--primary);font-weight:900;background-size:cover;background-position:center}.catalog-list span,.path-list span{color:var(--accent);font-size:9px;font-weight:750;letter-spacing:.08em}.catalog-list h4,.path-list h4{overflow:hidden;margin:4px 0;white-space:nowrap;text-overflow:ellipsis}.catalog-list p{overflow:hidden;margin:0;color:var(--text-muted);font-size:11px;white-space:nowrap;text-overflow:ellipsis}.catalog-list .empty{padding:80px 0;text-align:center;color:var(--text-muted)}.catalog-panel :deep(.el-pagination){justify-content:center;margin-top:14px}.path-list{display:flex;flex-direction:column;gap:9px}.path-list article{display:grid;grid-template-columns:32px 20px minmax(0,1fr) auto;gap:8px;align-items:center;padding:13px;border:1px solid var(--border);border-radius:14px;background:var(--bg-subtle)}.path-list article>i{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;background:var(--primary);color:white;font-size:11px;font-style:normal}.handle{color:var(--text-muted);cursor:grab}.path-list small{font-size:9px}.path-list small.published{color:var(--success)}.path-list small.draft{color:var(--text-muted)}.path-list small.withdrawn{color:var(--warning)}.path-list nav{display:flex;gap:3px}.path-list nav button{border:0;border-radius:7px;padding:5px 7px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}.path-list nav button:disabled{opacity:.35;cursor:not-allowed}.path-list nav .danger{color:var(--danger)}.path-empty{display:grid;min-height:260px;place-items:center;align-content:center;gap:8px;color:var(--text-muted)}.path-empty b{color:var(--text-primary)}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.cover-field{display:flex;width:100%;align-items:center;gap:12px}.cover-field>img,.cover-field>div{display:grid;width:120px;aspect-ratio:16/9;place-items:center;border-radius:10px;background:var(--bg-subtle);object-fit:cover;color:var(--text-muted);font-size:11px}.cover-field>span{display:flex;gap:6px}@media(max-width:1080px){.workspace__columns{grid-template-columns:1fr}.catalog-filters{grid-template-columns:1fr 1fr 1fr}.catalog-filters>button{width:max-content}.readiness{grid-template-columns:1fr 1fr}.readiness ul,.readiness>p{grid-column:1/-1;justify-content:flex-start;text-align:left}}@media(max-width:720px){.bundle-manager__header,.workspace__header{align-items:flex-start;flex-direction:column}.bundle-grid{grid-template-columns:1fr}.workspace{padding:16px}.workspace__header-actions{flex-wrap:wrap}.readiness{grid-template-columns:1fr}.readiness ul,.readiness>p{grid-column:auto}.catalog-filters{grid-template-columns:1fr 1fr}.catalog-filters>:first-child{grid-column:1/-1}.catalog-list article{grid-template-columns:48px minmax(0,1fr)}.catalog-cover{width:48px;height:48px}.catalog-list article>button{grid-column:2;justify-self:start}.path-list article{grid-template-columns:30px 16px minmax(0,1fr)}.path-list nav{grid-column:3}.form-grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.bundle-card,.catalog-list article{transition:none}}
</style>
