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
const heading = computed(() => ({ reading: '阅读管理', writing: '写作管理' })[props.domain])
const introduction = computed(() => ({
  reading: '管理分级阅读文章，检查内容后发布到阅读中心。',
  writing: '管理写作素材和练习任务，维护公开学习页面的内容。',
})[props.domain])
const englishLabel = computed(() => ({ reading: 'READING LIBRARY', writing: 'WRITING LIBRARY' })[props.domain])
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
  <main class="english-docs-admin" :data-domain="domain">
    <nav class="english-docs-admin__crumb"><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink> / {{ heading }}</nav>
    <header class="english-docs-admin__hero"><div><p class="public-eyebrow">{{ englishLabel }} · CONTENT MANAGEMENT</p><h1>{{ heading }}</h1><p>{{ introduction }}</p></div><div class="english-docs-admin__hero-side"><div class="english-docs-admin__count"><strong>{{ page.total }}</strong><span>项内容</span></div><RouterLink :to="accountPath('/english/manage/editor/' + kind + '/new')" class="english-docs-admin__create">＋ 新建内容</RouterLink></div></header>
    <nav v-if="domain === 'writing'" class="english-docs-admin__tabs" aria-label="写作内容类型"><RouterLink v-for="option in options" :key="option.value" :to="{ path: accountPath('/english/manage/writing'), query: { kind: option.value } }" :class="{ active: kind === option.value }">{{ option.label }}</RouterLink></nav>
    <section class="english-docs-admin__catalog"><div class="english-docs-admin__toolbar"><div><p class="public-eyebrow">CONTENT CATALOG</p><h2>{{ domain === 'writing' ? kind === 'writing-prompts' ? '写作任务' : '写作素材' : heading }}</h2></div><form class="english-docs-admin__search" @submit.prevent="currentPage = 1; load()"><input v-model="search" type="search" placeholder="搜索标题" aria-label="搜索标题"><button type="submit">查询</button></form></div>
      <p v-if="error" class="english-docs-admin__error" role="alert">{{ error }}</p><p v-if="notice" class="english-docs-admin__notice" role="status">{{ notice }}</p><p v-if="loading" class="english-docs-admin__empty">正在读取内容…</p>
      <div v-else class="english-docs-admin__grid"><article v-for="item in page.items" :key="item.id" class="english-docs-admin__card"><div class="english-docs-admin__card-top"><span class="english-docs-admin__level">{{ item.cefrLevel || 'ENGLISH' }}</span><span class="english-docs-admin__status" :class="{ published: item.publishStatus === 'PUBLISHED' }">{{ item.publishStatus === 'PUBLISHED' ? '已发布' : item.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</span></div><h3>{{ item.title }}</h3><p>{{ item.summary || '暂无摘要' }}</p><div class="english-docs-admin__meta"><span v-if="item.difficultyLevel">难度 {{ item.difficultyLevel }}</span><span v-if="domain === 'writing' && item.resourceKind">{{ item.resourceKind }}</span></div><div class="english-docs-admin__actions"><RouterLink :to="accountPath('/english/manage/editor/' + kind + '/' + item.id)">编辑</RouterLink><button type="button" :disabled="busy" @click="changeStatus(item)">{{ item.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button type="button" :disabled="busy" class="english-docs-admin__delete" @click="remove(item)">删除</button></div></article><p v-if="!page.items.length" class="english-docs-admin__empty">暂无内容。</p></div>
      <nav v-if="page.total > 20" class="english-docs-admin__pager" aria-label="分页"><button type="button" :disabled="currentPage <= 1" @click="currentPage--; load()">上一页</button><span>{{ currentPage }} / {{ Math.ceil(page.total / 20) }}</span><button type="button" :disabled="currentPage >= Math.ceil(page.total / 20)" @click="currentPage++; load()">下一页</button></nav>
    </section>
  </main>
</template>
<style scoped>
.english-docs-admin{max-width:1450px;margin:auto;padding:25px 0 80px}.english-docs-admin__crumb{margin-bottom:22px;color:var(--text-muted);font-size:13px}.english-docs-admin__crumb a{color:var(--primary)}.english-docs-admin__hero{display:flex;justify-content:space-between;align-items:end;gap:30px;margin-bottom:30px}.english-docs-admin__hero h1{margin:8px 0;font-size:clamp(34px,4vw,46px)}.english-docs-admin__hero p:last-child{margin:0;color:var(--text-secondary);line-height:1.7}.english-docs-admin__hero-side{display:flex;align-items:end;gap:22px;flex-shrink:0}.english-docs-admin__count{display:grid;min-width:95px;padding:12px 19px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);text-align:center}.english-docs-admin__count strong{color:var(--primary);font-size:30px;line-height:1.2}.english-docs-admin__count span{color:var(--text-muted);font-size:12px}.english-docs-admin__create{padding:11px 18px;border-radius:9px;background:var(--primary);color:var(--on-primary);white-space:nowrap}.english-docs-admin__tabs{display:flex;gap:8px;margin-bottom:28px;border-bottom:1px solid var(--border)}.english-docs-admin__tabs a{padding:12px 17px;border-bottom:2px solid transparent;color:var(--text-secondary)}.english-docs-admin__tabs a.active{border-bottom-color:var(--primary);color:var(--primary);font-weight:650}.english-docs-admin__catalog{padding-top:24px;border-top:1px solid var(--border)}.english-docs-admin__tabs+.english-docs-admin__catalog{border-top:0;padding-top:0}.english-docs-admin__toolbar{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:19px}.english-docs-admin__toolbar h2{margin:5px 0 0;font-size:23px}.english-docs-admin__search{display:flex;width:min(100%,390px);gap:8px}.english-docs-admin__search input{flex:1;min-width:0;padding:10px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.english-docs-admin button{padding:9px 13px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}.english-docs-admin button:disabled{opacity:.45;cursor:default}.english-docs-admin__grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.english-docs-admin__card{display:flex;min-height:265px;flex-direction:column;padding:21px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);box-shadow:0 2px 8px rgb(0 0 0/.025)}.english-docs-admin__card-top{display:flex;justify-content:space-between;align-items:center;gap:8px}.english-docs-admin__level{padding:5px 9px;border:1px solid var(--border-strong);border-radius:999px;color:var(--accent);font-size:11px;font-weight:700}.english-docs-admin__status{color:var(--text-muted);font-size:12px}.english-docs-admin__status.published{color:var(--primary)}.english-docs-admin__card h3{margin:17px 0 8px;font-size:19px;line-height:1.35}.english-docs-admin__card>p{margin:0;color:var(--text-secondary);font-size:13px;line-height:1.7}.english-docs-admin__meta{display:flex;gap:10px;margin:13px 0;color:var(--text-muted);font-size:12px}.english-docs-admin__actions{display:flex;gap:7px;margin-top:auto;padding-top:15px;border-top:1px solid var(--border)}.english-docs-admin__actions a{padding:9px 13px;border:1px solid var(--border-strong);border-radius:8px;color:var(--primary);font-size:13px}.english-docs-admin__actions button{font-size:13px}.english-docs-admin__actions .english-docs-admin__delete{margin-left:auto;color:var(--accent)}.english-docs-admin__empty{grid-column:1/-1;padding:55px 10px;color:var(--text-muted);text-align:center}.english-docs-admin__error,.english-docs-admin__notice{padding:12px;border-radius:9px}.english-docs-admin__error{color:#a13b2b;background:#fff0e8}.english-docs-admin__notice{color:var(--primary);background:var(--primary-soft)}.english-docs-admin__pager{display:flex;justify-content:center;align-items:center;gap:15px;margin-top:25px}@media(max-width:1050px){.english-docs-admin__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:700px){.english-docs-admin__hero,.english-docs-admin__toolbar{align-items:start;flex-direction:column}.english-docs-admin__hero-side{width:100%;justify-content:space-between}.english-docs-admin__search{width:100%}.english-docs-admin__grid{grid-template-columns:1fr}}
</style>
