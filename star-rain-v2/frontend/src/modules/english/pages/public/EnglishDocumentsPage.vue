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
const labels = { reading: { zh: '阅读中心', en: 'READING', desc: '通过分级文章训练理解与表达。', cta: '开始阅读' }, listening: { zh: '听力中心', en: 'LISTENING', desc: '围绕音频与逐句文本进行精听。', cta: '开始训练' }, writing: { zh: '写作中心', en: 'WRITING', desc: '从素材、范文和任务中练习清晰表达。', cta: '查看内容' } }
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
  <main class="english-documents"><RouterLink to="/english" class="english-documents__back">← 英语</RouterLink><header><p class="public-eyebrow">ENGLISH {{ label.en }}</p><h1>{{ label.zh }}</h1><p>{{ label.desc }}</p></header><p v-if="loading" class="english-documents__state">正在读取内容…</p><p v-else-if="error" class="english-documents__state" role="alert">{{ error }}</p>
    <template v-else-if="domain === 'writing'"><section><h2>写作素材</h2><div class="english-documents__grid"><RouterLink v-for="item in resources" :key="item.id" :to="path(item,'writing-resources')" class="english-documents__card"><small>{{ item.resourceKind }} · {{ item.cefrLevel }}</small><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><strong>开始学习 →</strong></RouterLink><p v-if="!resources.length">素材正在建设中。</p></div></section><section><h2>写作任务</h2><div class="english-documents__grid"><RouterLink v-for="item in prompts" :key="item.id" :to="path(item,'writing-prompts')" class="english-documents__card english-documents__card--prompt"><small>WRITING PRACTICE · {{ item.cefrLevel }}</small><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><strong>{{ item.wordMin }}–{{ item.wordMax }} 词 · {{ item.estimatedMinutes }} 分钟 →</strong></RouterLink><p v-if="!prompts.length">任务正在准备中。</p></div></section></template>
    <template v-else><form class="english-documents__search" @submit.prevent="submitSearch"><input v-model="search" type="search" placeholder="搜索标题" aria-label="搜索标题"><button type="submit">搜索</button></form><div class="english-documents__grid"><RouterLink v-for="item in data.items" :key="item.id" :to="path(item)" class="english-documents__card"><small>{{ item.cefrLevel }} · {{ item.difficultyLevel }} 级</small><h2>{{ item.title }}</h2><p>{{ item.summary }}</p><strong>{{ domain === 'listening' ? `${Math.round((item.durationSeconds || 0) / 60)} 分钟 · ` : '' }}{{ label.cta }} →</strong></RouterLink><p v-if="!data.items.length">暂无内容。</p></div><nav v-if="totalPages > 1" class="english-documents__pager"><button :disabled="page <= 1" @click="go(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button :disabled="page >= totalPages" @click="go(page + 1)">下一页</button></nav></template>
  </main>
</template>

<style scoped>
.english-documents{max-width:1240px;margin:auto;padding-bottom:80px}.english-documents__back{color:var(--primary);font-size:13px}.english-documents>header{padding:28px 0}.english-documents h1{margin:8px 0;font-size:clamp(36px,5vw,50px)}.english-documents>header>p:last-child,.english-documents__state{color:var(--text-secondary)}.english-documents__state{padding:60px 0}.english-documents section{margin:30px 0 42px}.english-documents section h2{margin-bottom:18px;font-size:23px}.english-documents__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:15px}.english-documents__card{display:flex;min-height:215px;flex-direction:column;padding:20px;border:1px solid var(--border);border-radius:18px;color:var(--text-primary);background:var(--bg-surface);transition:transform .18s,border-color .18s}.english-documents__card:hover{transform:translateY(-3px);border-color:var(--primary)}.english-documents__card--prompt{background:linear-gradient(135deg,var(--primary-soft),var(--bg-surface))}.english-documents__card small{color:var(--accent);font-size:10px;letter-spacing:.12em}.english-documents__card h2,.english-documents__card h3{margin:13px 0 8px;font-size:21px}.english-documents__card p{color:var(--text-secondary);font-size:13px;line-height:1.65}.english-documents__card strong{margin-top:auto;color:var(--primary);font-size:13px}.english-documents__search{display:flex;max-width:450px;gap:8px;margin:0 0 22px}.english-documents__search input{flex:1;padding:10px 13px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.english-documents__search button,.english-documents__pager button{padding:9px 15px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.english-documents__search button{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.english-documents__pager{display:flex;justify-content:center;align-items:center;gap:16px;margin-top:26px}.english-documents__pager button:disabled{opacity:.4;cursor:default}@media(max-width:600px){.english-documents__grid{grid-template-columns:1fr}}
</style>
