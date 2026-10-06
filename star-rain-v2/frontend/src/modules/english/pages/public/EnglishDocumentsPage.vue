<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { listEnglishDocuments } from '../../api/englishApi'

const props = defineProps({ domain: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const data = ref({ items: [], total: 0 })
const resources = ref([])
const prompts = ref([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / 20)))
const labels = { reading: { zh: '阅读中心', en: 'READING', desc: '通过分级文章训练理解与表达。', cta: '开始阅读' }, writing: { zh: '写作中心', en: 'WRITING', desc: '从素材、范文和任务中练习清晰表达。', cta: '查看内容' } }
const label = computed(() => labels[props.domain])

function path(item, kind = props.domain) {
  if (kind === 'writing-resources') return `/english/writing/resources/${item.slug}`
  if (kind === 'writing-prompts') return `/english/writing/practice/${item.slug}`
  return `/english/${kind}/${item.slug}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (props.domain === 'writing') {
      const [resourcePage, promptPage] = await Promise.all([
        listEnglishDocuments('writing-resources', { page: 1, size: 50 }),
        listEnglishDocuments('writing-prompts', { page: 1, size: 50 }),
      ])
      resources.value = resourcePage.items
      prompts.value = promptPage.items
    } else {
      data.value = await listEnglishDocuments(props.domain, { page: page.value, size: 20, search: route.query.search || '' })
      search.value = String(route.query.search || '')
    }
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function submitSearch() { router.push({ query: search.value.trim() ? { search: search.value.trim() } : {} }) }
function go(next) { router.push({ query: { ...route.query, page: next === 1 ? undefined : String(next) } }) }
watch(() => [props.domain, route.query.page, route.query.search], load, { immediate: true })
</script>

<template>
  <main class="english-documents" :data-domain="domain">
    <RouterLink to="/english" class="english-documents__back">← 英语</RouterLink>
    <header class="english-documents__hero"><div><p class="public-eyebrow">ENGLISH {{ label.en }} · 分级精选</p><h1>{{ label.zh }}</h1><p>{{ label.desc }}</p></div><div class="english-documents__count"><strong>{{ domain === 'writing' ? resources.length + prompts.length : data.total }}</strong><span>{{ domain === 'writing' ? '项写作内容' : '篇精选文章' }}</span></div></header>
    <p v-if="loading" class="english-documents__state">正在读取内容…</p><p v-else-if="error" class="english-documents__state" role="alert">{{ error }}</p>
    <template v-else-if="domain === 'writing'">
      <section class="english-documents__section"><header><div><p class="public-eyebrow">WRITING MATERIALS</p><h2>写作素材</h2></div><span>{{ resources.length }} 篇</span></header><div class="english-documents__grid"><RouterLink v-for="item in resources" :key="item.id" :to="path(item,'writing-resources')" class="english-documents__card"><div class="english-documents__card-top"><span class="english-documents__level">{{ item.cefrLevel || 'ENGLISH' }}</span><small>{{ item.resourceKind || '素材' }}</small></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><strong>开始学习 →</strong></RouterLink><p v-if="!resources.length" class="english-documents__empty">素材正在建设中。</p></div></section>
      <section class="english-documents__section"><header><div><p class="public-eyebrow">WRITING PRACTICE</p><h2>写作任务</h2></div><span>{{ prompts.length }} 项</span></header><div class="english-documents__grid"><RouterLink v-for="item in prompts" :key="item.id" :to="path(item,'writing-prompts')" class="english-documents__card english-documents__card--prompt"><div class="english-documents__card-top"><span class="english-documents__level">{{ item.cefrLevel || 'ENGLISH' }}</span><small>练习任务</small></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><strong>{{ item.wordMin }}–{{ item.wordMax }} 词 · {{ item.estimatedMinutes }} 分钟 →</strong></RouterLink><p v-if="!prompts.length" class="english-documents__empty">任务正在准备中。</p></div></section>
    </template>
    <template v-else>
      <div class="english-documents__toolbar"><span>{{ data.total }} 项内容</span><form class="english-documents__search" @submit.prevent="submitSearch"><input v-model="search" type="search" placeholder="搜索标题或摘要" aria-label="搜索标题或摘要"><button type="submit">搜索</button></form></div>
      <div class="english-documents__grid"><RouterLink v-for="item in data.items" :key="item.id" :to="path(item)" class="english-documents__card"><div class="english-documents__card-top"><span class="english-documents__level">{{ item.cefrLevel || 'ENGLISH' }}</span><small>{{ item.difficultyLevel ? '难度 ' + item.difficultyLevel : label.en }}</small></div><h2>{{ item.title }}</h2><p>{{ item.summary }}</p><strong>{{ label.cta }} →</strong></RouterLink><p v-if="!data.items.length" class="english-documents__empty">暂无内容。</p></div>
      <nav v-if="totalPages > 1" class="english-documents__pager"><button :disabled="page <= 1" @click="go(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button :disabled="page >= totalPages" @click="go(page + 1)">下一页</button></nav>
    </template>
  </main>
</template>
<style scoped>
.english-documents{max-width:1340px;margin:auto;padding:25px 0 85px}.english-documents__back{color:var(--primary);font-size:13px}.english-documents__hero{display:flex;justify-content:space-between;align-items:end;gap:30px;padding:28px 0 30px}.english-documents__hero h1{margin:8px 0;font-size:clamp(36px,5vw,48px);line-height:1.15}.english-documents__hero p:last-child{margin:0;color:var(--text-secondary);line-height:1.7}.english-documents__count{display:grid;min-width:105px;padding:15px 18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);text-align:center}.english-documents__count strong{color:var(--primary);font-size:30px;line-height:1.2}.english-documents__count span{color:var(--text-muted);font-size:12px}.english-documents__state{padding:70px 0;color:var(--text-secondary)}.english-documents__toolbar{display:flex;justify-content:space-between;align-items:center;gap:15px;margin:0 0 20px;padding-top:20px;border-top:1px solid var(--border)}.english-documents__toolbar>span,.english-documents__section>header>span{color:var(--text-muted);font-size:13px}.english-documents__search{display:flex;width:min(100%,430px);gap:8px}.english-documents__search input{flex:1;min-width:0;padding:10px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.english-documents__search button,.english-documents__pager button{padding:9px 15px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.english-documents__search button{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.english-documents__section{margin:18px 0 54px}.english-documents__section>header{display:flex;justify-content:space-between;align-items:end;margin-bottom:18px;padding-top:20px;border-top:1px solid var(--border)}.english-documents__section h2{margin:5px 0 0;font-size:26px}.english-documents__grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:15px}.english-documents__card{position:relative;display:flex;min-height:305px;flex-direction:column;padding:19px 18px 20px;border:1px solid var(--border);border-radius:17px;color:var(--text-primary);background:var(--bg-surface);box-shadow:0 2px 8px rgb(0 0 0/.025);transition:transform .18s,border-color .18s,box-shadow .18s}.english-documents__card:hover{transform:translateY(-3px);border-color:var(--primary);box-shadow:0 8px 22px rgb(0 0 0/.055)}.english-documents__card--prompt{background:linear-gradient(140deg,var(--bg-surface),var(--primary-soft))}.english-documents__card-top{display:flex;justify-content:space-between;align-items:center;gap:6px}.english-documents__level{display:inline-grid;min-width:34px;min-height:34px;place-items:center;padding:0 5px;border:1px solid var(--border-strong);border-radius:50%;color:var(--accent);font-size:11px;font-weight:700}.english-documents__card small{color:var(--text-muted);font-size:11px}.english-documents__card h2,.english-documents__card h3{margin:17px 0 7px;font-size:20px;line-height:1.25}.english-documents__card p{margin:0;color:var(--text-secondary);font-size:13px;line-height:1.8}.english-documents__card strong{margin-top:auto;padding-top:18px;color:var(--primary);font-size:13px;font-weight:550}.english-documents__empty{grid-column:1/-1;padding:60px;color:var(--text-muted);text-align:center}.english-documents__pager{display:flex;justify-content:center;align-items:center;gap:15px;margin-top:25px}.english-documents__pager button:disabled{opacity:.4;cursor:default}@media(max-width:1050px){.english-documents__grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:780px){.english-documents__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:600px){.english-documents__hero,.english-documents__toolbar{align-items:start;flex-direction:column}.english-documents__search{width:100%}.english-documents__grid{grid-template-columns:1fr}.english-documents__card{min-height:235px}}@media(prefers-reduced-motion:reduce){.english-documents__card{transition:none}.english-documents__card:hover{transform:none}}
</style>
