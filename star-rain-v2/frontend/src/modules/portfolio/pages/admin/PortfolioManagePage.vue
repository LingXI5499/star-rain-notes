<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { deleteWork, listAdminWorks, publishWork, restoreWork, withdrawWork } from '../../api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import { accountPath } from '../../../../shared/viewMode'

const route = useRoute()
const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { type: '', status: '', q: '' },
})
const result = ref({ items: [], total: 0, pageSize: 20 })
const error = ref('')
const notice = ref('')
const loading = ref(false)
const busyId = ref('')
const confirmDialog = ref(null)
const pageCount = computed(() => Math.max(1, Math.ceil(result.value.total / pageSize.value)))
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }
const statusLabels = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    result.value = await listAdminWorks({ page: page.value, pageSize: pageSize.value,
      type: filters.type || undefined, status: filters.status || undefined, q: filters.q.trim() || undefined })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
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

<template>
  <section class="portfolio-admin">
    <header class="portfolio-admin__hero">
      <div><p>CASE STUDY LIBRARY · 项目案例</p><h1>作品管理</h1><span>维护作品内容、类型详情与发布状态。</span></div>
      <RouterLink :to="accountPath('/portfolio/editor/new')">＋ 新建作品</RouterLink>
    </header>
    <form class="portfolio-admin__filters" @submit.prevent="search">
      <input v-model="filters.q" type="search" placeholder="搜索作品标题" aria-label="搜索作品标题" />
      <select v-model="filters.type" aria-label="作品类型"><option value="">全部类型</option><option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option></select>
      <select v-model="filters.status" aria-label="发布状态"><option value="">全部状态</option><option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option></select>
      <button type="submit">筛选</button><span>共 {{ result.total }} 件</span>
    </form>
    <p v-if="error" class="portfolio-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="portfolio-admin__notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="portfolio-admin__empty">正在加载…</p>
    <p v-else-if="!result.items.length" class="portfolio-admin__empty">
      <template v-if="filters.type || filters.status || filters.q">没有符合条件的作品。<button type="button" @click="resetQuery">清除筛选</button></template>
      <template v-else>还没有作品。点击“新建作品”开始。</template>
    </p>
    <div v-else class="portfolio-admin__list">
      <article v-for="work in result.items" :key="work.id" class="portfolio-admin__card">
        <div class="portfolio-admin__visual"><img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" /><span v-else>{{ work.title.slice(0, 1) }}</span></div>
        <div class="portfolio-admin__body">
          <small>{{ typeLabels[work.workType] }} · {{ statusLabels[work.status] }} · {{ work.slug }}</small>
          <h2>{{ work.title }}</h2><p>{{ work.summary || '摘要待填写' }}</p>
          <div class="portfolio-admin__actions">
            <RouterLink :to="accountPath(`/portfolio/editor/${work.id}`)">编辑</RouterLink>
            <RouterLink v-if="work.status === 'PUBLISHED'" :to="`/portfolio/${work.slug}`" target="_blank">查看前台 ↗</RouterLink>
            <button type="button" :disabled="busyId === work.id" @click="changeStatus(work)">{{ work.status === 'PUBLISHED' ? '撤回' : work.status === 'WITHDRAWN' ? '恢复' : '发布' }}</button>
            <button v-if="work.status !== 'PUBLISHED'" type="button" :disabled="busyId === work.id" @click="remove(work)">删除</button>
          </div>
        </div>
      </article>
    </div>
    <nav v-if="pageCount > 1" class="portfolio-admin__pages"><button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pageCount }}</span><button type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button></nav>
    <AppConfirmDialog ref="confirmDialog" />
  </section>
</template>

<style scoped>
.portfolio-admin{max-width:1160px;margin:auto}.portfolio-admin__hero{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:32px}.portfolio-admin__hero p{margin:0 0 8px;color:var(--accent);font-size:11px;letter-spacing:.14em}.portfolio-admin__hero h1{margin:0 0 8px;font-size:36px}.portfolio-admin__hero span{color:var(--text-muted);font-size:14px}.portfolio-admin__hero>a,.portfolio-admin__filters button{padding:11px 18px;border:0;border-radius:10px;color:var(--on-primary);background:var(--primary);text-decoration:none;cursor:pointer;white-space:nowrap}.portfolio-admin__filters{display:grid;grid-template-columns:minmax(200px,1fr) 160px 160px auto auto;gap:10px;align-items:center;margin-bottom:25px;padding:16px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.portfolio-admin__filters input,.portfolio-admin__filters select{min-width:0;padding:10px;border:1px solid var(--border);border-radius:9px;color:var(--text-primary);background:var(--bg-page)}.portfolio-admin__filters span{color:var(--text-muted);font-size:12px}.portfolio-admin__list{display:grid;gap:16px}.portfolio-admin__card{display:grid;grid-template-columns:180px 1fr;overflow:hidden;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.portfolio-admin__visual{display:grid;place-items:center;min-height:170px;background:var(--bg-subtle)}.portfolio-admin__visual img{width:100%;height:100%;object-fit:cover}.portfolio-admin__visual span{font:700 55px Georgia,serif;color:var(--primary)}.portfolio-admin__body{padding:20px}.portfolio-admin__body small{color:var(--accent)}.portfolio-admin__body h2{margin:8px 0;font-size:21px}.portfolio-admin__body p{margin:0;color:var(--text-secondary);line-height:1.6}.portfolio-admin__actions{display:flex;gap:8px;margin-top:19px}.portfolio-admin__actions a,.portfolio-admin__actions button,.portfolio-admin__pages button{padding:7px 12px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-page);text-decoration:none;cursor:pointer}.portfolio-admin__empty{padding:60px;border:1px dashed var(--border);border-radius:16px;color:var(--text-muted);text-align:center}.portfolio-admin__error{color:var(--danger)}.portfolio-admin__notice{color:var(--primary)}.portfolio-admin__pages{display:flex;justify-content:center;gap:14px;align-items:center;margin-top:24px}@media(max-width:750px){.portfolio-admin__filters{grid-template-columns:1fr 1fr}.portfolio-admin__card{grid-template-columns:1fr}.portfolio-admin__visual{max-height:180px}.portfolio-admin__hero{align-items:start;flex-direction:column}}@media(max-width:480px){.portfolio-admin__filters{grid-template-columns:1fr}}
</style>
