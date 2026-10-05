<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { getVocabularyThemes } from '../../api/englishApi'

const themes = ref([])
const activeLayer = ref('')
const loading = ref(true)
const error = ref('')
const groups = computed(() => {
  const grouped = new Map()
  for (const theme of themes.value) {
    if (!grouped.has(theme.layer)) grouped.set(theme.layer, [])
    grouped.get(theme.layer).push(theme)
  }
  return [...grouped].map(([name, items]) => ({ name, items }))
})
const visibleGroups = computed(() => activeLayer.value ? groups.value.filter((group) => group.name === activeLayer.value) : groups.value)
const total = computed(() => themes.value.reduce((sum, theme) => sum + theme.wordCount, 0))

onMounted(async () => {
  try { themes.value = await getVocabularyThemes() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
})
</script>

<template>
  <main class="vocabulary-page">
    <header><p class="public-eyebrow">VOCABULARY · THEMED</p><h1>词汇</h1><p>按主题分类的英语词汇库 · {{ groups.length }} 个分类组 / {{ total.toLocaleString() }} 个单词</p></header>
    <p v-if="loading" class="vocabulary-page__state">正在读取词库…</p><p v-else-if="error" class="vocabulary-page__state" role="alert">{{ error }}</p>
    <template v-else><div class="vocabulary-page__filters" role="group" aria-label="分类组筛选"><button :class="{ active: !activeLayer }" @click="activeLayer = ''">全部</button><button v-for="group in groups" :key="group.name" :class="{ active: activeLayer === group.name }" @click="activeLayer = group.name">{{ group.name }}</button></div><section v-for="group in visibleGroups" :key="group.name" class="vocabulary-page__group"><h2>{{ group.name }}</h2><div><RouterLink v-for="theme in group.items" :key="theme.id" :to="`/english/vocabulary/${theme.id}`"><strong>{{ theme.name }}</strong><span>{{ theme.wordCount }} 词</span></RouterLink></div></section><p v-if="!themes.length" class="vocabulary-page__state">词库暂无内容。</p></template>
  </main>
</template>

<style scoped>
.vocabulary-page{max-width:1200px;margin:auto;padding-bottom:70px}.vocabulary-page header{margin-bottom:32px}.vocabulary-page h1{margin:8px 0;font-size:clamp(38px,5vw,54px)}.vocabulary-page header>p:last-child,.vocabulary-page__state{color:var(--text-secondary)}.vocabulary-page__state{padding:50px 0}.vocabulary-page__filters{display:flex;flex-wrap:wrap;gap:9px;margin-bottom:36px}.vocabulary-page__filters button{padding:8px 15px;border:1px solid var(--border-strong);border-radius:999px;color:var(--text-secondary);background:var(--bg-surface);cursor:pointer}.vocabulary-page__filters button.active{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}.vocabulary-page__group{margin-bottom:35px}.vocabulary-page__group h2{margin:0 0 15px;font-size:22px}.vocabulary-page__group>div{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:13px}.vocabulary-page__group a{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:95px;padding:18px;border:1px solid var(--border);border-radius:15px;color:var(--text-primary);background:var(--bg-surface)}.vocabulary-page__group a:hover{border-color:var(--primary);transform:translateY(-2px)}.vocabulary-page__group strong{font-size:17px}.vocabulary-page__group span{color:var(--text-muted);font-size:12px;white-space:nowrap}
</style>
