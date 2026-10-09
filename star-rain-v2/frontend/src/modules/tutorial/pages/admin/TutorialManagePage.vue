<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../account/stores/authStore'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import {
  createCategory, deleteCategory, deleteTutorial, listAdminCategories, listAdminTutorials,
  publishTutorial, reorderCategories, reorderTutorials,
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

function categoryCount(categoryId) {
  return tutorials.value.filter((item) => String(item.categoryId) === String(categoryId)).length
}

function statusLabel(item) {
  if (item.publicationStatus === 'PUBLISHED') return '已发布'
  if (item.publicationStatus === 'WITHDRAWN') return '已撤回'
  return '草稿'
}

/*
 * 状态胶囊只用三种底色（对齐 V1 `.status-pill--published/draft/withdrawn`）：
 * V2 比 V1 多一个「审核中」编辑态，它既不是已发布也不是已撤回，按「进行中」归到草稿的琥珀色。
 * 映射只影响配色，状态文案仍由 statusLabel 决定。
 */
function statusTone(item) {
  if (item.publicationStatus === 'PUBLISHED') return 'published'
  if (item.publicationStatus === 'WITHDRAWN') return 'withdrawn'
  return 'draft'
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
    if (action === 'publish') await publishTutorial(item.id)
    if (action === 'withdraw') await withdrawTutorial(item.id)
    if (action === 'delete') {
        if (!await confirmDialog.value.ask(`确定删除教程「${item.title}」及其全部分组、章节？未结束计划将取消，个人学习证据和答案版本保留。此操作无法撤销。`)) return
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

<!--
  教程工作台：视觉按 V1 `views/admin/TutorialListView.vue` 对齐。
  信息架构、接口、权限判断、拖拽排序全部保持 V2 原样，只重排了呈现层：
  面板阴影 / 面板头 + 提示条 / 知识体系计数胶囊与悬停才出现的操作 /
  课程卡片的状态胶囊 + 编号行 + 悬停上浮 / V1 那套「图标 + 标题 + 说明」的空态。
  V2 独有的「预览」「更换体系」「提交审核」等按钮一个不少。
-->
<template>
  <main class="page-container tutorial-manage">
    <header class="tutorial-manage__hero">
      <div>
        <p class="eyebrow">TUTORIAL WORKSPACE · 内容编排</p>
        <h1>教程工作台</h1>
        <p>先选择知识体系，再管理其中的教程；课程分组与章节进入独立结构页维护。</p>
      </div>
      <button
        class="primary-button tutorial-manage__create"
        type="button"
        :disabled="!activeCategoryId"
        @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })"
      >新建教程</button>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <div class="tutorial-manage__layout">
      <aside class="tutorial-manage__systems">
        <header class="tutorial-manage__panel-head">
          <h2>知识体系</h2>
          <button
            v-if="auth.hasPermission('tutorial:publish')"
            class="tutorial-manage__icon-button"
            type="button"
            title="新建知识体系"
            aria-label="新建知识体系"
            @click="openCategoryDialog()"
          >＋</button>
        </header>
        <p class="tutorial-manage__hint">拖动左侧手柄调整同级顺序</p>
        <div class="tutorial-manage__system-body">
          <p v-if="!loading && !categories.length" class="tutorial-manage__empty-message">暂无知识体系</p>
          <article
            v-for="category in categories"
            :key="category.id"
            class="tutorial-manage__system"
            :class="{ 'is-active': String(category.id) === activeCategoryId }"
            draggable="true"
            @click="selectCategory(category)"
            @dragstart="draggingCategoryId = String(category.id)"
            @dragover.prevent
            @drop.prevent="dropCategory(category.id)"
          >
            <span class="tutorial-manage__grip" aria-hidden="true">⠿</span>
            <strong class="tutorial-manage__system-name">{{ category.name }}</strong>
            <span class="tutorial-manage__count">{{ categoryCount(category.id) }}</span>
            <span class="tutorial-manage__arrow" aria-hidden="true">›</span>
            <div v-if="auth.hasPermission('tutorial:publish')" class="tutorial-manage__system-actions">
              <button type="button" @click.stop="openCategoryDialog(category)">编辑</button>
              <button type="button" class="danger" @click.stop="removeCategory(category)">删除</button>
            </div>
          </article>
        </div>
      </aside>

      <section class="tutorial-manage__courses">
        <header class="tutorial-manage__panel-head tutorial-manage__panel-head--stack">
          <div>
            <p class="tutorial-manage__path">教程工作台 / {{ activeCategory?.name || '请选择知识体系' }}</p>
            <h2>{{ activeCategory?.name || '教程列表' }}</h2>
            <small>{{ currentTutorials.length }} 门教程 · {{ chapterCount }} 个章节</small>
          </div>
          <div class="tutorial-manage__tools">
            <input v-model="search" type="search" placeholder="搜索当前体系教程" />
            <button
              class="primary-button tutorial-manage__create"
              type="button"
              :disabled="!activeCategoryId"
              @click="router.push({ path: '/useradmin/tutorials/editor/new', query: { categoryId: activeCategoryId } })"
            >新建教程</button>
          </div>
        </header>

        <div class="tutorial-manage__body">
          <div v-if="loading" class="loading">正在加载教程…</div>
          <div v-else-if="!visibleTutorials.length" class="tutorial-manage__empty">
            <span aria-hidden="true">⌁</span>
            <h3>{{ search ? '没有匹配的教程' : '这个知识体系还没有教程' }}</h3>
            <p>{{ search ? '换一个关键词试试。' : '新建第一门教程，开始搭建课程结构。' }}</p>
          </div>
          <div v-else class="tutorial-manage__grid">
            <article
              v-for="item in visibleTutorials"
              :key="item.id"
              class="tutorial-manage__card"
              :draggable="!search"
              @dragstart="draggingTutorialId = String(item.id)"
              @dragover.prevent
              @drop.prevent="dropTutorial(item.id)"
            >
              <div class="tutorial-manage__card-head">
                <span class="tutorial-manage__grip" aria-hidden="true">⠿</span>
                <small>{{ activeCategory?.name }}</small>
                <span class="status-pill" :class="`status-pill--${statusTone(item)}`">{{ statusLabel(item) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="tutorial-manage__card-slug">编号 {{ item.slug }}</p>
              <p class="tutorial-manage__card-summary">{{ item.summary }}</p>
              <div class="tutorial-manage__card-meta">
                <span>{{ item.chapterCount || 0 }} 章</span>
                <span>更新 {{ item.updatedAt?.slice(0, 10) }}</span>
              </div>
              <button
                type="button"
                class="tutorial-manage__enter"
                @click="router.push(`/useradmin/tutorials/${item.id}/curriculum`)"
              >管理课程结构 <span aria-hidden="true">→</span></button>
              <div class="tutorial-manage__card-actions">
                <button type="button" @click="router.push(`/useradmin/tutorials/editor/${item.id}`)">编辑</button>
                <button type="button" @click="router.push(`/useradmin/tutorials/${item.id}/preview`)">预览</button>
                <button type="button" @click="openMoveDialog(item)">更换体系</button>
                <button v-if="auth.hasPermission('tutorial:publish') && item.publicationStatus !== 'PUBLISHED'" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'publish')">{{ item.publicationStatus === 'WITHDRAWN' ? '重新公开' : '公开' }}</button>
                <button v-if="auth.hasPermission('tutorial:withdraw') && item.publicationStatus === 'PUBLISHED'" type="button" :disabled="busyId === String(item.id)" @click="act(item, 'withdraw')">撤回</button>
                <button v-if="auth.hasPermission('tutorial:publish')" type="button" class="danger" :disabled="busyId === String(item.id)" @click="act(item, 'delete')">删除</button>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>

    <dialog ref="categoryDialog" class="tutorial-manage__dialog" @cancel.prevent="categoryDialog.close()">
      <h2>{{ categoryForm.id ? '编辑知识体系' : '新建知识体系' }}</h2>
      <form @submit.prevent="saveCategory">
        <label>名称<input v-model="categoryForm.name" maxlength="100" autofocus required /></label>
        <p class="tutorial-manage__dialog-tip">内容编号由系统自动生成，修改名称不会改变原编号。</p>
        <p class="tutorial-manage__dialog-tip">知识体系固定为一级，不再设置父分类。</p>
        <div class="tutorial-manage__dialog-actions">
          <button type="button" @click="categoryDialog.close()">取消</button>
          <button class="primary-button" type="submit">保存</button>
        </div>
      </form>
    </dialog>

    <dialog ref="moveDialog" class="tutorial-manage__dialog" @cancel.prevent="moveDialog.close()">
      <h2>更换知识体系</h2>
      <p class="tutorial-manage__dialog-lead">{{ moveTarget?.title }}</p>
      <label>目标知识体系
        <select v-model="moveCategoryId">
          <option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
        </select>
      </label>
      <p class="tutorial-manage__dialog-tip">确认后教程会追加到目标知识体系末尾，分组、章节和正文不会改变。</p>
      <div class="tutorial-manage__dialog-actions">
        <button type="button" @click="moveDialog.close()">取消</button>
        <button class="primary-button" type="button" :disabled="moveCategoryId === String(moveTarget?.categoryId)" @click="moveTutorial">确认移动</button>
      </div>
    </dialog>

    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
.tutorial-manage__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.tutorial-manage__hero h1 {
  margin: 4px 0 8px;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.tutorial-manage__hero > div > p:last-child {
  color: var(--text-secondary);
  font-size: 14px;
}

/* 主按钮不换行：窄一点的头部里「新建教程」曾被压成两行 */
.tutorial-manage__create { white-space: nowrap; }

.tutorial-manage__layout {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 190px);
}

.tutorial-manage__systems,
.tutorial-manage__courses {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  box-shadow: 0 12px 36px rgb(17 35 29 / 0.055);
  overflow: hidden;
}

.tutorial-manage__panel-head {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-subtle) 52%, transparent);
}

.tutorial-manage__panel-head--stack { align-items: flex-end; }

.tutorial-manage__panel-head h2 {
  margin-top: 5px;
  font-size: 18px;
  line-height: 1.3;
}

.tutorial-manage__icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--border-strong);
  border-radius: 11px;
  color: var(--primary);
  background: var(--bg-surface);
  font-size: 21px;
  line-height: 1;
  transition: transform 160ms ease, border-color 160ms ease;
}

.tutorial-manage__icon-button:hover { border-color: var(--primary); transform: translateY(-1px); }

.tutorial-manage__hint {
  margin: 0;
  padding: 9px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

.tutorial-manage__system-body {
  display: flex;
  min-height: 260px;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  overflow: auto;
}

.tutorial-manage__system {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto 12px;
  align-items: center;
  gap: 9px;
  padding: 11px;
  border: 1px solid transparent;
  border-radius: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: transform 170ms ease, background-color 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

.tutorial-manage__system:hover {
  border-color: var(--border);
  background: var(--bg-subtle);
  transform: translateX(3px);
}

.tutorial-manage__system.is-active {
  border-color: color-mix(in srgb, var(--primary) 28%, var(--border));
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, var(--bg-surface));
  box-shadow: 0 7px 20px color-mix(in srgb, var(--primary) 12%, transparent);
}

.tutorial-manage__grip {
  color: var(--text-muted);
  letter-spacing: -4px;
  cursor: grab;
}

.tutorial-manage__system-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tutorial-manage__count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 10px;
  text-align: center;
}

.tutorial-manage__arrow { color: var(--primary); font-size: 18px; }

/* 编辑 / 删除只在悬停或键盘聚焦时出现，静态列表保持干净（与 V1 一致） */
.tutorial-manage__system-actions {
  display: none;
  grid-column: 2 / 5;
  gap: 12px;
  padding-top: 5px;
}

.tutorial-manage__system:hover .tutorial-manage__system-actions,
.tutorial-manage__system:focus-within .tutorial-manage__system-actions { display: flex; }

.tutorial-manage__system-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

.tutorial-manage__system-actions button:hover { background: none; text-decoration: underline; }

.tutorial-manage__system-actions .danger,
.tutorial-manage__card-actions .danger { color: var(--danger); }

.tutorial-manage__path {
  margin-bottom: 7px;
  color: var(--text-muted);
  font-size: 11px;
}

.tutorial-manage__courses .tutorial-manage__panel-head h2 { font-size: 24px; }

.tutorial-manage__courses .tutorial-manage__panel-head small {
  color: var(--text-muted);
  font-size: 12px;
}

.tutorial-manage__tools { display: flex; width: min(420px, 48%); gap: 10px; }

.tutorial-manage__tools input { min-width: 160px; }

.tutorial-manage__body {
  min-height: 360px;
  flex: 1;
  padding: 20px;
}

.tutorial-manage__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.tutorial-manage__card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 17px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
  transition: transform 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

.tutorial-manage__card:hover {
  border-color: color-mix(in srgb, var(--primary) 35%, var(--border));
  box-shadow: 0 16px 34px rgb(14 35 28 / 0.09);
  transform: translateY(-3px);
}

.tutorial-manage__card-head { display: flex; align-items: center; gap: 8px; }

.tutorial-manage__card-head small {
  min-width: 0;
  overflow: hidden;
  color: var(--accent);
  font-size: 10px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tutorial-manage__card-head .status-pill { margin-left: auto; }

.tutorial-manage__card h3 {
  margin: 7px 0 4px;
  font-size: 17px;
  line-height: 1.4;
}

.tutorial-manage__card-slug {
  overflow: hidden;
  color: var(--text-muted);
  font: 10px/1.5 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tutorial-manage__card-summary {
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.tutorial-manage__card-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  color: var(--text-muted);
  font-size: 10px;
}

/* 「管理课程结构」与卡片里的次要操作按 V1 的 §course-card 尺寸：36px 高、浅描边按钮 */
.tutorial-manage__enter,
.tutorial-manage__card-actions button {
  min-height: 36px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--primary);
  background: var(--bg-surface);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}

.tutorial-manage__enter {
  align-self: flex-start;
  margin-top: 14px;
  white-space: nowrap;
}

.tutorial-manage__enter span {
  display: inline-block;
  transition: transform 160ms ease;
}

.tutorial-manage__card:hover .tutorial-manage__enter span { transform: translateX(3px); }

.tutorial-manage__enter:hover,
.tutorial-manage__card-actions button:hover:not(:disabled) {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, var(--bg-surface));
}

.tutorial-manage__card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

.tutorial-manage__empty,
.tutorial-manage__empty-message { color: var(--text-muted); text-align: center; }

.tutorial-manage__empty {
  display: grid;
  min-height: 360px;
  place-content: center;
  gap: 7px;
}

.tutorial-manage__empty span { color: var(--primary); font-size: 32px; }

.tutorial-manage__empty h3 { color: var(--text-secondary); font-size: 16px; }

.tutorial-manage__empty p,
.tutorial-manage__empty-message { font-size: 12px; }

.tutorial-manage__empty-message { padding: 38px 12px; }

.tutorial-manage__dialog {
  width: min(440px, 92vw);
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 16px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

.tutorial-manage__dialog h2 { margin-bottom: 16px; }

.tutorial-manage__dialog form { display: grid; gap: 16px; }

.tutorial-manage__dialog-tip {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.tutorial-manage__dialog-lead { margin: 0 0 14px; color: var(--text-primary); font-weight: 650; }

.tutorial-manage__dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

@media (prefers-reduced-motion: reduce) {
  .tutorial-manage__system,
  .tutorial-manage__card,
  .tutorial-manage__enter span,
  .tutorial-manage__icon-button { transition: none; }
}

@media (max-width: 1024px) {
  .tutorial-manage__layout { grid-template-columns: 240px minmax(0, 1fr); }
  .tutorial-manage__grid { grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); }
  .tutorial-manage__panel-head--stack { align-items: flex-start; flex-direction: column; }
  .tutorial-manage__tools { width: 100%; }
}

@media (max-width: 720px) {
  .tutorial-manage__hero { align-items: flex-start; flex-direction: column; }
  .tutorial-manage__layout { grid-template-columns: 1fr; }
  .tutorial-manage__system-body { max-height: 300px; }
  .tutorial-manage__tools { align-items: stretch; flex-direction: column; }
  .tutorial-manage__grid { grid-template-columns: 1fr; }
}
</style>
