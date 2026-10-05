<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { getVocabularyThemes, getVocabularyWords } from '../../api/englishApi'

const route = useRoute()
const router = useRouter()
const theme = ref(null)
const words = ref([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const search = ref('')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / 24)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [themes, result] = await Promise.all([
      getVocabularyThemes(), getVocabularyWords({ themeId: String(route.params.themeId), page: page.value, size: 24, search: route.query.search || '' }),
    ])
    theme.value = themes.find((item) => item.id === String(route.params.themeId)) || null
    words.value = result.items
    total.value = result.total
    search.value = String(route.query.search || '')
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function submitSearch() { router.push({ query: search.value.trim() ? { search: search.value.trim() } : {} }) }
function go(next) { router.push({ query: { ...route.query, page: next === 1 ? undefined : String(next) } }) }
function pronounce(word) {
  if (!window.speechSynthesis) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = 'en-US'
  window.speechSynthesis.speak(utterance)
}

watch(() => [route.params.themeId, route.query.page, route.query.search], load, { immediate: true })
</script>

<template>
  <main class="vocabulary-theme">
    <header><RouterLink to="/english/vocabulary">← 词汇总览</RouterLink><p class="public-eyebrow">{{ theme?.layer || 'VOCABULARY' }}</p><h1>{{ theme?.name || '主题词汇' }}</h1><p>{{ total }} 词 · 第 {{ page }} / {{ totalPages }} 页</p></header>
    <form class="vocabulary-theme__search" @submit.prevent="submitSearch"><input v-model="search" type="search" placeholder="搜索单词或释义" aria-label="搜索单词或释义"><button type="submit">搜索</button></form>
    <p v-if="loading" class="vocabulary-theme__state">正在读取单词…</p><p v-else-if="error" class="vocabulary-theme__state" role="alert">{{ error }}</p>
    <template v-else><div class="vocabulary-theme__grid"><article v-for="item in words" :key="item.id" class="vocabulary-theme__card"><div><h2>{{ item.word }}</h2><button type="button" :aria-label="`朗读 ${item.word}`" @click="pronounce(item.word)">▶</button></div><p class="vocabulary-theme__phonetic">{{ item.phoneticUs ? `美 ${item.phoneticUs}` : '' }} {{ item.phoneticUk ? `英 ${item.phoneticUk}` : '' }}</p><p><small v-if="item.partOfSpeech">{{ item.partOfSpeech }}</small> {{ item.translation }}</p></article></div><p v-if="!words.length" class="vocabulary-theme__state">没有符合条件的单词。</p><nav v-if="totalPages > 1" class="vocabulary-theme__pager"><button :disabled="page <= 1" @click="go(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button :disabled="page >= totalPages" @click="go(page + 1)">下一页</button></nav></template>
  </main>
</template>

<style scoped>
.vocabulary-theme{max-width:1200px;margin:auto;padding-bottom:70px}.vocabulary-theme header>a{color:var(--text-secondary);font-size:13px}.vocabulary-theme header>.public-eyebrow{margin-top:22px}.vocabulary-theme h1{margin:6px 0;font-size:clamp(34px,5vw,50px)}.vocabulary-theme header>p:last-child,.vocabulary-theme__state{color:var(--text-secondary)}.vocabulary-theme__search{display:flex;max-width:550px;gap:8px;margin:28px 0}.vocabulary-theme__search input{flex:1;min-width:0;padding:12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-surface);color:var(--text-primary);font:inherit}.vocabulary-theme__search button{padding:10px 18px;border:0;border-radius:10px;color:var(--on-primary);background:var(--primary);cursor:pointer}.vocabulary-theme__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}.vocabulary-theme__card{min-height:170px;padding:20px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.vocabulary-theme__card>div{display:flex;justify-content:space-between;gap:10px;align-items:center}.vocabulary-theme__card h2{margin:0;font-size:24px}.vocabulary-theme__card button{width:36px;height:36px;border:1px solid var(--border-strong);border-radius:50%;color:var(--primary);background:transparent;cursor:pointer}.vocabulary-theme__phonetic{min-height:22px;color:var(--text-muted);font-size:13px}.vocabulary-theme__card>p:last-child{color:var(--text-secondary);line-height:1.7}.vocabulary-theme__card small{color:var(--accent)}.vocabulary-theme__pager{display:flex;align-items:center;justify-content:center;gap:18px;margin-top:30px}.vocabulary-theme__pager button{padding:8px 15px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.vocabulary-theme__pager button:disabled{opacity:.4;cursor:default}
</style>
