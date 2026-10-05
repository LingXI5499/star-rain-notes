<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import { deleteEnglishDocument, listEnglishDocuments, setEnglishDocumentPublished } from '../../api/englishApi'

const props = defineProps({ domain: { type: String, required: true } })
const route = useRoute()
const kind = computed(() => props.domain === 'writing' ? String(route.query.kind || 'writing-resources') : props.domain)
const options = [{ value: 'writing-resources', label: '写作素材' }, { value: 'writing-prompts', label: '写作任务' }]
const heading = computed(() => ({ reading: '阅读管理', listening: '听力管理', writing: '写作管理' })[props.domain])
const page = ref({ items: [], total: 0 })
const search = ref('')
const currentPage = ref(1)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try { page.value = await listEnglishDocuments(kind.value, { page: currentPage.value, size: 20, search: search.value }, true) }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function changeStatus(item) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await setEnglishDocumentPublished(kind.value, item.id, item.publishStatus !== 'PUBLISHED')
    notice.value = item.publishStatus === 'PUBLISHED' ? '内容已撤回。' : '内容已发布。'
    await load()
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

async function remove(item) {
  if (!window.confirm(`确定删除「${item.title}」？此操作无法撤销。`)) return
  busy.value = true
  error.value = ''
  try { await deleteEnglishDocument(kind.value, item.id); notice.value = '内容已删除。'; await load() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

watch(() => [props.domain, route.query.kind], () => { currentPage.value = 1; load() }, { immediate: true })
</script>

<template>
  <main class="english-docs-admin"><nav class="english-docs-admin__crumb"><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink><span>›</span>{{ heading }}</nav><header><div><p class="public-eyebrow">ENGLISH CONTENT · 内容管理</p><h1>{{ heading }}</h1><p>编辑内容后按需发布，公开页面只展示已发布内容。</p></div><RouterLink :to="accountPath(`/english/manage/editor/${kind}/new`)">＋ 新建内容</RouterLink></header>
    <div v-if="domain === 'writing'" class="english-docs-admin__tabs"><RouterLink v-for="option in options" :key="option.value" :to="{ path: accountPath('/english/manage/writing'), query: { kind: option.value } }" :class="{ active: kind === option.value }">{{ option.label }}</RouterLink></div>
    <form class="english-docs-admin__search" @submit.prevent="currentPage = 1; load()"><input v-model="search" type="search" placeholder="搜索标题" aria-label="搜索标题"><button type="submit">查询</button></form>
    <p v-if="error" class="english-docs-admin__error" role="alert">{{ error }}</p><p v-if="notice" class="english-docs-admin__notice" role="status">{{ notice }}</p><p v-if="loading">正在读取内容…</p><div v-else class="english-docs-admin__list"><article v-for="item in page.items" :key="item.id"><div><small>{{ item.cefrLevel }} · {{ item.publishStatus === 'PUBLISHED' ? '已发布' : item.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</small><h2>{{ item.title }}</h2><p>{{ item.summary }}</p></div><div class="english-docs-admin__actions"><RouterLink :to="accountPath(`/english/manage/editor/${kind}/${item.id}`)">编辑</RouterLink><button type="button" :disabled="busy" @click="changeStatus(item)">{{ item.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button type="button" :disabled="busy" @click="remove(item)">删除</button></div></article><p v-if="!page.items.length">暂无内容。</p></div><nav v-if="page.total > 20" class="english-docs-admin__pager"><button :disabled="currentPage <= 1" @click="currentPage--; load()">上一页</button><span>{{ currentPage }} / {{ Math.ceil(page.total / 20) }}</span><button :disabled="currentPage >= Math.ceil(page.total / 20)" @click="currentPage++; load()">下一页</button></nav>
  </main>
</template>

<style scoped>
.english-docs-admin{max-width:1250px;margin:auto;padding:25px 0 70px}.english-docs-admin__crumb{display:flex;gap:10px;margin-bottom:18px;color:var(--text-muted);font-size:13px}.english-docs-admin__crumb a{color:var(--primary)}.english-docs-admin>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:24px}.english-docs-admin h1{margin:7px 0;font-size:34px}.english-docs-admin>header p:last-child{color:var(--text-secondary)}.english-docs-admin>header>a{padding:11px 17px;border-radius:10px;background:var(--primary);color:var(--on-primary);white-space:nowrap}.english-docs-admin__tabs{display:flex;gap:8px;margin-bottom:22px}.english-docs-admin__tabs a{padding:9px 14px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-secondary)}.english-docs-admin__tabs a.active{border-color:var(--primary);color:var(--primary);background:var(--primary-soft)}.english-docs-admin__search{display:flex;max-width:450px;gap:8px;margin-bottom:18px}.english-docs-admin__search input{flex:1;min-width:0;padding:10px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.english-docs-admin__search button,.english-docs-admin__actions button,.english-docs-admin__pager button{padding:8px 13px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.english-docs-admin__list{display:grid;gap:10px}.english-docs-admin__list article{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:20px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}.english-docs-admin__list small{color:var(--accent)}.english-docs-admin__list h2{margin:5px 0;font-size:19px}.english-docs-admin__list p{max-width:700px;margin:0;color:var(--text-secondary);font-size:13px}.english-docs-admin__actions{display:flex;gap:8px;white-space:nowrap}.english-docs-admin__actions a{padding:8px 13px;border:1px solid var(--border-strong);border-radius:8px;color:var(--primary)}.english-docs-admin__error,.english-docs-admin__notice{padding:12px;border-radius:9px}.english-docs-admin__error{color:#9a3022;background:#fff1ed}.english-docs-admin__notice{color:var(--primary);background:var(--primary-soft)}.english-docs-admin__pager{display:flex;justify-content:center;align-items:center;gap:16px;margin-top:25px}@media(max-width:700px){.english-docs-admin>header,.english-docs-admin__list article{align-items:start;flex-direction:column}.english-docs-admin__actions{flex-wrap:wrap}}
</style>
