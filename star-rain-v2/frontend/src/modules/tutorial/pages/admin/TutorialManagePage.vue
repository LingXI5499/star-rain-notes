<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../account/stores/authStore'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import {
  createCategory, deleteCategory, deleteTutorial, listAdminCategories, listAdminTutorials,
  publishTutorial, reorderCategories, reorderTutorials, restoreTutorial, submitTutorialReview,
  updateCategory, updateTutorial, withdrawTutorial,
} from '../../api/tutorialApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = ref([])
const tutorials = ref([])
const activeCategoryId = ref('')
const search = ref('')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const busyId = ref('')
const confirmDialog = ref(null)
const categoryDialog = ref(null)
const moveDialog = ref(null)
const moveTarget = ref(null)
const moveCategoryId = ref('')
const categoryForm = reactive({ id: '', name: '' })
let draggingCategoryId = ''
let draggingTutorialId = ''

const activeCategory = computed(() => categories.value.find((item) => String(item.id) === activeCategoryId.value))
const currentTutorials = computed(() => tutorials.value
  .filter((item) => String(item.categoryId) === activeCategoryId.value)
  .sort((a, b) => a.sortOrder - b.sortOrder || Number(a.id) - Number(b.id)))
const visibleTutorials = computed(() => {
  const term = search.value.trim().toLocaleLowerCase()
  return term ? currentTutorials.value.filter((item) => item.title.toLocaleLowerCase().includes(term)) : currentTutorials.value
})
const chapterCount = computed(() => currentTutorials.value.reduce((total, item) => total + (item.chapterCount || 0), 0))

function statusLabel(item) {
  if (item.editingStatus === 'IN_REVIEW') return '审核中'
  if (item.publicationStatus === 'PUBLISHED') return '已发布'
  if (item.publicationStatus === 'WITHDRAWN') return '已撤回'
  return '草稿'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryRows, firstPage] = await Promise.all([
      listAdminCategories(), listAdminTutorials({ page: 1, pageSize: 100 }),
    ])
    categories.value = categoryRows
    tutorials.value = firstPage.items || []
    const pages = Math.ceil((firstPage.total || 0) / 100)
    for (let page = 2; page <= pages; page += 1) {
      const next = await listAdminTutorials({ page, pageSize: 100 })
      tutorials.value.push(...next.items)
    }
    const requested = String(route.query.category || activeCategoryId.value || '')
    activeCategoryId.value = categories.value.some((item) => String(item.id) === requested)
      ? requested : String(categories.value[0]?.id || '')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function selectCategory(category) {
  activeCategoryId.value = String(category.id)
  search.value = ''
  router.replace({ query: { category: activeCategoryId.value } })
}

function openCategoryDialog(category = null) {
  categoryForm.id = category ? String(category.id) : ''
  categoryForm.name = category?.name || ''
  categoryDialog.value.showModal()
}

async function saveCategory() {
  if (!categoryForm.name.trim()) return
  try {
    const saved = categoryForm.id
      ? await updateCategory(categoryForm.id, categoryForm.name.trim())
      : await createCategory(categoryForm.name.trim())
    categoryDialog.value.close()
    activeCategoryId.value = String(saved.id)
    notice.value = '知识体系已保存。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function removeCategory(category) {
  if (!await confirmDialog.value.ask(`确定删除知识体系「${category.name}」？仅空体系可以删除。`)) return
  try {
    await deleteCategory(category.id)
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function dropCategory(targetId) {
  if (!draggingCategoryId || draggingCategoryId === String(targetId)) return
  const ids = categories.value.map((item) => String(item.id))
  ids.splice(ids.indexOf(draggingCategoryId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingCategoryId)
  draggingCategoryId = ''
  try { await reorderCategories(ids); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

async function dropTutorial(targetId) {
  if (!draggingTutorialId || draggingTutorialId === String(targetId)) return
  const ids = currentTutorials.value.map((item) => String(item.id))
  ids.splice(ids.indexOf(draggingTutorialId), 1)
  ids.splice(ids.indexOf(String(targetId)), 0, draggingTutorialId)
  draggingTutorialId = ''
  try { await reorderTutorials(activeCategoryId.value, ids); await load() }
  catch (cause) { error.value = errorMessage(cause) }
}

function openMoveDialog(item) {
  moveTarget.value = item
  moveCategoryId.value = String(item.categoryId)
  moveDialog.value.showModal()
}

async function moveTutorial() {
  if (!moveTarget.value || moveCategoryId.value === String(moveTarget.value.categoryId)) return
  try {
    await updateTutorial(moveTarget.value.id, { categoryId: moveCategoryId.value })
    moveDialog.value.close()
    activeCategoryId.value = moveCategoryId.value
    notice.value = '教程已移动到目标知识体系末尾。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function act(item, action) {
  busyId.value = String(item.id)
  error.value = ''
  notice.value = ''
  try {
    if (action === 'submit') await submitTutorialReview(item.id)
    if (action === 'publish') await publishTutorial(item.id)
    if (action === 'withdraw') await withdrawTutorial(item.id)
    if (action === 'restore') await restoreTutorial(item.id)
    if (action === 'delete') {
      if (!await confirmDialog.value.ask(`确定删除教程「${item.title}」？仅空教程可以删除。`)) return
      await deleteTutorial(item.id)
    }
    notice.value = '操作成功。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="page-container tutorial-manage">
    <header class="tutorial-manage__hero">
      <div><p class="eyebrow">TUTORIAL WORKSPACE · 内容编排</p><h1>教程工作台</h1><p>先选择知识体系，再管理其中的教程；课程分组与章节进入独立结构页维护。</p></div>
      <button class="primary-button" type="button" :disabled="!activeCategoryId" @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })">新建教程</button>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <div class="tutorial-manage__layout">
      <aside class="tutorial-manage__systems">
        <header><h2>知识体系</h2><button v-if="auth.hasPermission('tutorial:publish')" type="button" aria-label="新建知识体系" @click="openCategoryDialog()">＋</button></header>
        <p class="muted">拖动左侧手柄调整同级顺序</p>
        <div v-if="!loading && !categories.length" class="empty-state">暂无知识体系</div>
        <div v-for="category in categories" :key="category.id" class="tutorial-manage__system" :class="{ 'is-active': String(category.id) === activeCategoryId }" draggable="true" @click="selectCategory(category)" @dragstart="draggingCategoryId = String(category.id)" @dragover.prevent @drop.prevent="dropCategory(category.id)">
          <span class="tutorial-manage__grip">⠿</span><strong>{{ category.name }}</strong><small>{{ tutorials.filter((item) => String(item.categoryId) === String(category.id)).length }}</small><span>›</span>
          <div v-if="auth.hasPermission('tutorial:publish')" class="tutorial-manage__system-actions"><button type="button" @click.stop="openCategoryDialog(category)">编辑</button><button type="button" @click.stop="removeCategory(category)">删除</button></div>
        </div>
      </aside>
      <section class="tutorial-manage__courses">
        <header>
          <div><p class="muted">教程工作台 / {{ activeCategory?.name || '请选择知识体系' }}</p><h2>{{ activeCategory?.name || '教程列表' }}</h2><small>{{ currentTutorials.length }} 门教程 · {{ chapterCount }} 个章节</small></div>
          <div class="tutorial-manage__tools"><input v-model="search" type="search" placeholder="搜索当前体系教程" /><button class="primary-button" type="button" :disabled="!activeCategoryId" @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })">新建教程</button></div>
        </header>
        <div v-if="loading" class="loading">正在加载教程…</div>
        <div v-else-if="!visibleTutorials.length" class="empty-state">{{ search ? '没有匹配的教程' : '这个知识体系还没有教程' }}</div>
        <div v-else class="tutorial-manage__grid">
          <article v-for="item in visibleTutorials" :key="item.id" class="tutorial-manage__card" :draggable="!search" @dragstart="draggingTutorialId = String(item.id)" @dragover.prevent @drop.prevent="dropTutorial(item.id)">
            <div class="tutorial-manage__card-head"><span class="tutorial-manage__grip">⠿</span><small>{{ activeCategory?.name }}</small><em>{{ statusLabel(item) }}</em></div>
            <h3>{{ item.title }}</h3><p>{{ item.summary }}</p>
            <div class="tutorial-manage__card-meta"><span>{{ item.chapterCount || 0 }} 章</span><span>更新 {{ item.updatedAt?.slice(0, 10) }}</span></div>
            <button type="button" class="tutorial-manage__enter" @click="router.push(`/useradmin/tutorials/${item.id}/curriculum`)">管理课程结构 →</button>
            <div class="tutorial-manage__card-actions">
              <button type="button" @click="router.push(`/useradmin/tutorials/editor/${item.id}`)">编辑</button>
              <button type="button" @click="router.push(`/useradmin/tutorials/${item.id}/preview`)">预览</button>
              <button type="button" @click="openMoveDialog(item)">更换体系</button>
              <button v-if="item.editingStatus === 'DRAFT' && auth.hasPermission('tutorial:submit') && !auth.hasPermission('tutorial:publish')" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'submit')">提交审核</button>
              <button v-if="auth.hasPermission('tutorial:publish') && item.publicationStatus !== 'WITHDRAWN' && item.editingStatus !== 'IN_REVIEW'" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'publish')">{{ item.publicationStatus === 'PUBLISHED' ? '更新公开版本' : '发布' }}</button>
              <button v-if="auth.hasPermission('tutorial:withdraw') && item.publicationStatus === 'PUBLISHED'" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'withdraw')">撤回</button>
              <button v-if="auth.hasPermission('tutorial:withdraw') && item.publicationStatus === 'WITHDRAWN'" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'restore')">重新公开</button>
              <button v-if="auth.hasPermission('tutorial:publish')" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'delete')">删除</button>
            </div>
          </article>
        </div>
      </section>
    </div>
    <dialog ref="categoryDialog" class="tutorial-manage__dialog" @cancel.prevent="categoryDialog.close()"><h2>{{ categoryForm.id ? '编辑知识体系' : '新建知识体系' }}</h2><form @submit.prevent="saveCategory"><label>名称<input v-model="categoryForm.name" maxlength="100" autofocus required /></label><div class="tutorial-manage__dialog-actions"><button type="button" @click="categoryDialog.close()">取消</button><button class="primary-button" type="submit">保存</button></div></form></dialog>
    <dialog ref="moveDialog" class="tutorial-manage__dialog" @cancel.prevent="moveDialog.close()"><h2>更换知识体系</h2><p>{{ moveTarget?.title }}</p><label>目标知识体系<select v-model="moveCategoryId"><option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select></label><p class="muted">确认后教程会追加到目标知识体系末尾，分组、章节和正文不会改变。</p><div class="tutorial-manage__dialog-actions"><button type="button" @click="moveDialog.close()">取消</button><button class="primary-button" type="button" :disabled="moveCategoryId === String(moveTarget?.categoryId)" @click="moveTutorial">确认移动</button></div></dialog>
    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
.tutorial-manage__hero { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.tutorial-manage__hero p:last-child { color: var(--text-secondary); }
.tutorial-manage__layout { display: grid; grid-template-columns: 288px minmax(0, 1fr); gap: 20px; align-items: stretch; min-height: 600px; }
.tutorial-manage__systems,.tutorial-manage__courses { min-width: 0; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); overflow: hidden; }
.tutorial-manage__systems > header,.tutorial-manage__courses > header { display: flex; align-items: end; justify-content: space-between; gap: 14px; padding: 20px; border-bottom: 1px solid var(--border); background: var(--bg-subtle); }
.tutorial-manage__systems > header h2 { font-size: 18px; }
.tutorial-manage__systems > header button { font-size: 21px; }
.tutorial-manage__systems > p { padding: 10px 20px; font-size: 11px; }
.tutorial-manage__system { display: grid; grid-template-columns: 16px minmax(0, 1fr) auto 12px; align-items: center; gap: 8px; margin: 8px 12px; padding: 10px; border-radius: 12px; cursor: pointer; }
.tutorial-manage__system:hover,.tutorial-manage__system.is-active { color: var(--primary); background: var(--bg-subtle); }
.tutorial-manage__system strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tutorial-manage__grip { color: var(--text-muted); cursor: grab; }
.tutorial-manage__system-actions { grid-column: 2 / -1; display: flex; gap: 8px; }
.tutorial-manage__system-actions button { padding: 0; border: 0; color: var(--primary); background: none; }
.tutorial-manage__courses > header h2 { margin-bottom: 4px; font-size: 24px; }
.tutorial-manage__courses > header small { color: var(--text-muted); }
.tutorial-manage__tools { display: flex; gap: 10px; }
.tutorial-manage__tools input { min-width: 180px; }
.tutorial-manage__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; padding: 20px; }
.tutorial-manage__card { min-width: 0; padding: 17px; border: 1px solid var(--border); border-radius: 16px; }
.tutorial-manage__card:hover { border-color: var(--primary); }
.tutorial-manage__card-head { display: flex; align-items: center; gap: 8px; }
.tutorial-manage__card-head small { color: var(--accent); }
.tutorial-manage__card-head em { margin-left: auto; color: var(--primary); font-size: 11px; font-style: normal; }
.tutorial-manage__card h3 { margin: 8px 0; font-size: 17px; }
.tutorial-manage__card > p { color: var(--text-secondary); }
.tutorial-manage__card-meta { display: flex; justify-content: space-between; margin: 14px 0; padding-top: 12px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 11px; }
.tutorial-manage__enter { margin-bottom: 10px; color: var(--primary); }
.tutorial-manage__card-actions { display: flex; flex-wrap: wrap; gap: 7px; }
.tutorial-manage__card-actions button { font-size: 12px; }
.tutorial-manage__dialog { width: min(440px, 92vw); padding: 24px; border: 1px solid var(--border); border-radius: 16px; color: var(--text-primary); background: var(--bg-surface); }
.tutorial-manage__dialog form { display: grid; gap: 16px; }
.tutorial-manage__dialog-actions { display: flex; justify-content: end; gap: 8px; }
@media (max-width: 900px) { .tutorial-manage__layout { grid-template-columns: 1fr; } .tutorial-manage__hero,.tutorial-manage__courses > header { align-items: start; flex-direction: column; } }
</style>
