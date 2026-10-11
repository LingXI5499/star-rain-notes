<script setup>
import LearningListFilters from '../../components/LearningListFilters.vue'
import { publicPage, publicPageSize } from '../../../../shared/composables/publicListState'
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LearningNav from '../../components/LearningNav.vue'
import LearningPagination from '../../components/LearningPagination.vue'
import { getMasteryPage, getMasteryOptions } from '../../api/learningApi'
import { masteryLabels } from '../../support/learningLabels'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import '../../styles/learning.css'
const route = useRoute(), router = useRouter()
const rows = ref([]), tutorials = ref([]), total = ref(0), page = ref(1), pageSize = ref(20), error = ref(''), loading = ref(true)
const filters = reactive({ tutorialId: '', status: '', keyword: '', needsRevalidation: '' })
let applied = {}, version = 0
async function load() {
 const request = ++version; loading.value = true; error.value = ''
 try { const result = await getMasteryPage({ ...applied, page: page.value, pageSize: pageSize.value }); if (request === version) { rows.value = result.items; total.value = result.total } }
 catch (e) { if (request === version) error.value = errorMessage(e) } finally { if (request === version) loading.value = false }
}
function search() { router.push({ query: { ...route.query, ...Object.fromEntries(Object.entries(filters).map(([k,v]) => [k, v.trim() || undefined])), page: undefined } }) }
function reset() { Object.keys(filters).forEach(k => { filters[k] = '' }); search() }
function move(n) { router.push({ query: { ...route.query, page: n > 1 ? String(n) : undefined } }) }
function resize(n) { router.push({ query: { ...route.query, pageSize: n === 20 ? undefined : String(n), page: undefined } }) }
watch(() => route.query, () => {
  page.value = publicPage(route.query.page); pageSize.value = publicPageSize(route.query.pageSize, 20, [10, 20, 50])
  Object.keys(filters).forEach(key => { filters[key] = typeof route.query[key] === 'string' ? route.query[key] : '' })
  applied = Object.fromEntries(Object.entries(filters).filter(([,v]) => v !== ''))
  load()
}, { immediate: true })
onMounted(async () => { await Promise.allSettled([getMasteryOptions().then(options => { tutorials.value = options }).catch(e => { error.value = errorMessage(e) })]) })
</script>
<template><main class="learning-page"><header class="learning-page__heading"><div><small>KNOWLEDGE MASTERY</small><h1>知识掌握</h1><p>至少三次有效评价后，根据最近三次回忆判断掌握状态。</p></div></header><LearningNav />
 <LearningListFilters :model-value="filters" :tutorials="tutorials" :statuses="masteryLabels" @update:model-value="Object.assign(filters, $event)" @search="search" @reset="reset" />
 <p v-if="error" class="learning-error" role="alert">{{ error }} <button class="learning-button" @click="load">重试</button></p><p v-if="loading" role="status">正在读取掌握状态…</p>
 <section v-else-if="!error" class="learning-panel"><p v-if="!rows.length" class="learning-empty">暂无符合条件的知识点。</p><ul class="learning-list"><li v-for="c in rows" :key="c.knowledgeCardId"><div><strong>{{ c.frontText }}</strong><small>{{ c.tutorialTitle }} / {{ c.chapterTitle }} · {{ c.evidenceCount }} 次证据 · 最近三次得分 {{ c.recentScore }} / 6</small><span class="learning-badge">{{ masteryLabels[c.masteryStatus] }}</span><span v-if="c.needsRevalidation" class="learning-badge">内容已更新 · 需要重新确认</span></div><RouterLink :to="accountPath(`/learning/chapters/${c.chapterId}`)">查看章节</RouterLink></li></ul></section>
 <LearningPagination :page="page" :page-size="pageSize" :total="total" :loading="loading" @page="move" @page-size="resize" />
</main></template>
