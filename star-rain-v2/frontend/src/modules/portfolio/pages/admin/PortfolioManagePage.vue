<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { deleteWork, getWorkTaxonomy, listAdminWorks, publishWork, restoreWork, withdrawWork } from '../../api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import { accountPath } from '../../../../shared/viewMode'
import { stageLabels } from '../../support/workBlocks'

const route = useRoute()
const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { type: '', categoryId: '', status: '', q: '' },
})
const categories = ref([])
getWorkTaxonomy().then(data => { categories.value = data.categories }).catch(cause => { error.value = errorMessage(cause) })
const result = ref({ items: [], total: 0, pageSize: 20 })
const error = ref('')
const notice = ref('')
const loading = ref(false)
const busyId = ref('')
const confirmDialog = ref(null)
let loadVersion = 0
const pageCount = computed(() => Math.max(1, Math.ceil(result.value.total / pageSize.value)))
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }
const statusLabels = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }
function techStackOf(work) {
  const stack = work.techStack ? work.techStack.split(/[,，、]/).map(item => item.trim()).filter(Boolean) : work.typeDetail?.techStack
  return Array.isArray(stack) ? stack : []
}

function updatedLabel(work) {
  return work.updatedAt ? work.updatedAt.slice(0, 16).replace('T', ' ') : '—'
}

async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const response = await listAdminWorks({ page: page.value, pageSize: pageSize.value,
      categoryId: filters.categoryId || undefined, type: filters.type || undefined, status: filters.status || undefined, q: filters.q.trim() || undefined })
    if (version === loadVersion) result.value = response
  } catch (cause) {
    if (version === loadVersion) error.value = errorMessage(cause)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function search() {
  page.value = 1
  const previous = route.fullPath
  await writeQuery()
  if (route.fullPath === previous) await load()
}

function changePage(next) {
  if (next < 1 || next > pageCount.value || next === page.value) return
  page.value = next
  writeQuery()
}

async function changeStatus(work) {
  busyId.value = work.id
  error.value = ''
  notice.value = ''
  try {
    if (work.status === 'PUBLISHED') await withdrawWork(work.id)
    else if (work.status === 'WITHDRAWN') await restoreWork(work.id)
    else await publishWork(work.id)
    notice.value = '作品状态已更新。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = ''
  }
}

async function remove(work) {
  if (!await confirmDialog.value.ask({
    title: '删除作品', message: `删除「${work.title}」后无法恢复。`,
    confirmText: '删除', danger: true, requireName: work.title,
  })) return
  busyId.value = work.id
  error.value = ''
  try { await deleteWork(work.id); notice.value = '作品已删除。'; await load() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busyId.value = '' }
}

watch(() => route.fullPath, () => {
  readQuery()
  load()
}, { immediate: true })
</script>

<!--
  作品后台：视觉按 V1 `views/admin/PortfolioListView.vue` 对齐。
  接口、筛选写回 URL、分页、发布状态流转、删除确认全部保持 V2 原样，只重排呈现层：
  筛选栏（筛选改为次要按钮、计数右对齐）/ 卡片（编号等宽小字 + 类型·状态·阶段三枚胶囊 /
  摘要两行截断 / 角色 + 技术栈胶囊 / 底部分隔线上的更新时间与圆角操作按钮）/
  封面位改成 V1 的主色到强调色渐变 + 衬线首字 / 卡片悬停上浮 / V1 那套虚线空态。
  数据全部取自列表接口已有的字段（typeDetail.role、typeDetail.techStack、typeDetail.projectStage）。
-->
<template>
  <section class="portfolio-admin">
    <header class="portfolio-admin__hero">
      <div>
        <p>CASE STUDY LIBRARY · 项目案例</p>
        <h1>作品管理</h1>
        <span>通过模板与内容区块管理作品、分类和发布状态。</span>
      </div>
      <RouterLink :to="accountPath('/portfolio/editor/new')">＋ 新建作品</RouterLink>
    </header>

    <form class="portfolio-admin__filters" @submit.prevent="search">
      <input v-model="filters.q" type="search" placeholder="搜索作品标题" aria-label="搜索作品标题" />
      <select v-model="filters.categoryId" aria-label="作品分类"><option value="">全部分类</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select>
      <select v-model="filters.type" aria-label="作品类型">
        <option value="">全部类型</option>
        <option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option>
      </select>
      <select v-model="filters.status" aria-label="发布状态">
        <option value="">全部状态</option>
        <option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option>
      </select>
      <button type="submit">筛选</button>
      <span>共 {{ result.total }} 件</span>
    </form>

    <p v-if="error" class="portfolio-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="portfolio-admin__notice" role="status">{{ notice }}</p>

    <p v-if="loading" class="portfolio-admin__empty">正在加载…</p>
    <div v-else-if="!result.items.length" class="portfolio-admin__empty">
      <template v-if="filters.categoryId || filters.type || filters.status || filters.q">
        <strong>没有符合条件的作品</strong>
        <span>换一组筛选条件，或清除筛选查看全部作品。</span>
        <button type="button" @click="resetQuery">清除筛选</button>
      </template>
      <template v-else>
        <strong>暂无作品</strong>
        <span>创建项目案例，展示完整的工程过程。</span>
      </template>
    </div>

    <div v-else class="portfolio-admin__list">
      <article v-for="work in result.items" :key="work.id" class="portfolio-card">
        <div class="portfolio-card__visual">
          <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" />
          <span v-else aria-hidden="true">{{ work.title.slice(0, 1) }}</span>
        </div>
        <div class="portfolio-card__body">
          <div class="portfolio-card__top">
            <div>
              <p>编号 {{ work.slug }}</p>
              <h2>{{ work.title }}</h2>
            </div>
            <div class="portfolio-card__pills">
              <span>{{ typeLabels[work.workType] || work.workType }}</span>
              <span>{{ statusLabels[work.status] || work.status }}</span>
              <span v-if="stageLabels[work.projectStatus || work.typeDetail?.projectStage]">{{ stageLabels[work.projectStatus || work.typeDetail?.projectStage] }}</span>
            </div>
          </div>

          <p class="portfolio-card__summary">{{ work.summary || '摘要待填写' }}</p>
          <p v-if="(work.role || work.typeDetail?.role)" class="portfolio-card__role">角色 · {{ work.role || work.typeDetail?.role }}</p>

          <div class="portfolio-card__stack">
            <span v-for="tech in techStackOf(work)" :key="tech">{{ tech }}</span>
            <small v-if="!techStackOf(work).length">技术栈待补充</small>
          </div>

          <footer>
            <time :datetime="work.updatedAt || undefined">更新 {{ updatedLabel(work) }}</time>
            <div>
              <RouterLink :to="accountPath(`/portfolio/editor/${work.id}`)">编辑</RouterLink>
              <RouterLink v-if="work.status === 'PUBLISHED'" :to="`/portfolio/${work.slug}`" target="_blank">查看前台 ↗</RouterLink>
              <button type="button" :disabled="busyId === work.id" @click="changeStatus(work)">{{ work.status === 'PUBLISHED' ? '撤回' : work.status === 'WITHDRAWN' ? '恢复' : '发布' }}</button>
              <button v-if="work.status !== 'PUBLISHED'" type="button" class="danger" :disabled="busyId === work.id" @click="remove(work)">删除</button>
            </div>
          </footer>
        </div>
      </article>
    </div>

    <nav v-if="pageCount > 1" class="portfolio-admin__pages">
      <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pageCount }}</span>
      <button type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button>
    </nav>

    <AppConfirmDialog ref="confirmDialog" />
  </section>
</template>

<style scoped>
.portfolio-admin { max-width: 1160px; margin: auto; }

.portfolio-admin__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}

.portfolio-admin__hero p {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.portfolio-admin__hero h1 { margin-bottom: 5px; font-size: 34px; letter-spacing: -0.03em; }

.portfolio-admin__hero span { color: var(--text-muted); font-size: 13px; }

.portfolio-admin__hero > a {
  padding: 11px 18px;
  border-radius: 10px;
  color: var(--on-primary);
  background: var(--primary);
  font-weight: 650;
  text-decoration: none;
  white-space: nowrap;
}

.portfolio-admin__hero > a:hover { background: var(--primary-hover); }

.portfolio-admin__filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 150px 150px auto auto;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
}

.portfolio-admin__filters input,
.portfolio-admin__filters select {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--text-primary);
  background: var(--bg-page);
}

/* 筛选是次要动作，主按钮留给「新建作品」——V1 这里也是默认按钮 */
.portfolio-admin__filters button {
  padding: 11px 18px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  color: var(--text-primary);
  background: var(--bg-surface);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.portfolio-admin__filters button:hover { border-color: var(--primary); color: var(--primary); }

.portfolio-admin__filters > span { justify-self: end; color: var(--text-muted); font-size: 12px; }

.portfolio-admin__list { min-height: 180px; display: grid; gap: var(--space-4); }

.portfolio-card {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  transition: transform 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

.portfolio-card:hover {
  border-color: color-mix(in srgb, var(--primary) 38%, var(--border));
  transform: translateY(-2px);
  box-shadow: 0 15px 34px rgb(0 0 0 / 0.065);
}

.portfolio-card__visual {
  position: relative;
  display: grid;
  min-height: 205px;
  overflow: hidden;
  place-items: center;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--primary) 18%, var(--bg-subtle)),
    color-mix(in srgb, var(--accent) 13%, var(--bg-surface))
  );
}

.portfolio-card__visual img { width: 100%; height: 100%; object-fit: cover; }

.portfolio-card__visual > span {
  color: color-mix(in srgb, var(--primary) 58%, var(--text-muted));
  font: 700 62px/1 Georgia, 'Songti SC', serif;
}

.portfolio-card__body { min-width: 0; padding: var(--space-5); }

.portfolio-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.portfolio-card__top p {
  margin-bottom: 4px;
  color: var(--accent);
  font: 600 10px/1.4 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

.portfolio-card__top h2 { font-size: 21px; line-height: 1.4; }

.portfolio-card__pills { display: flex; flex-wrap: wrap; gap: 5px; }

.portfolio-card__pills span {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
  font-size: 10px;
  white-space: nowrap;
}

.portfolio-card__summary {
  display: -webkit-box;
  overflow: hidden;
  margin: 10px 0 5px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.portfolio-card__role { color: var(--text-muted); font-size: 11px; }

.portfolio-card__stack {
  display: flex;
  min-height: 26px;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.portfolio-card__stack span {
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 9%, transparent);
  font-size: 11px;
}

.portfolio-card__stack small { color: var(--text-muted); }

.portfolio-card footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.portfolio-card time { color: var(--text-muted); font-size: 11px; }

.portfolio-card footer > div { display: flex; flex-wrap: wrap; gap: 5px; }

.portfolio-card footer a,
.portfolio-card footer button {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-page);
  font-size: 12px;
  text-decoration: none;
  cursor: pointer;
}

.portfolio-card footer a:hover,
.portfolio-card footer button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }

.portfolio-card footer .danger:hover:not(:disabled) { border-color: var(--danger); color: var(--danger); }

.portfolio-admin__empty {
  display: grid;
  min-height: 220px;
  place-content: center;
  gap: 6px;
  padding: 60px 24px;
  border: 1px dashed var(--border);
  border-radius: 16px;
  color: var(--text-muted);
  text-align: center;
}

.portfolio-admin__empty strong { color: var(--text-primary); font-size: 15px; }

.portfolio-admin__empty span { font-size: 13px; }

.portfolio-admin__empty button {
  justify-self: center;
  margin-top: 6px;
  padding: 7px 14px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--text-primary);
  background: var(--bg-surface);
  font-size: 12px;
  cursor: pointer;
}

.portfolio-admin__empty button:hover { border-color: var(--primary); color: var(--primary); }

.portfolio-admin__error { color: var(--danger); }
.portfolio-admin__notice { color: var(--primary); }

.portfolio-admin__pages {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: var(--space-6);
}

.portfolio-admin__pages button {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--primary);
  background: var(--bg-page);
  cursor: pointer;
}

.portfolio-admin__pages span { color: var(--text-muted); font-size: 12px; }

@media (prefers-reduced-motion: reduce) {
  .portfolio-card { transition: none; }
  .portfolio-card:hover { transform: none; }
}

@media (max-width: 1000px) {
  .portfolio-admin__filters { grid-template-columns: 1fr 1fr; }
  .portfolio-card { grid-template-columns: 160px minmax(0, 1fr); }
  .portfolio-card footer { align-items: flex-start; flex-direction: column; }
}

@media (max-width: 750px) {
  .portfolio-admin__filters { grid-template-columns: 1fr 1fr; }
  .portfolio-admin__hero { align-items: flex-start; flex-direction: column; }
  .portfolio-card { grid-template-columns: 1fr; }
  .portfolio-card__visual { min-height: 150px; aspect-ratio: 16 / 7; }
  .portfolio-card__top { flex-direction: column; }
}

@media (max-width: 480px) {
  .portfolio-admin__filters { grid-template-columns: 1fr; }
}
</style>
