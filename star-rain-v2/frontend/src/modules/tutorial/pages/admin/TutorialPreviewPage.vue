<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ArticleOutline from '../../../blog/components/ArticleOutline.vue'
import { previewTutorial } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'

const route = useRoute()
const router = useRouter()
const tutorial = ref(null)
const selectedSlug = ref('')
const outline = ref([])
const error = ref('')
const loading = ref(true)
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
const selected = computed(() => chapters.value.find((item) => item.slug === selectedSlug.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    tutorial.value = await previewTutorial(route.params.tutorialId)
    selectedSlug.value = typeof route.query.chapter === 'string' && chapters.value.some((item) => item.slug === route.query.chapter)
      ? route.query.chapter : (chapters.value[0]?.slug || '')
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}
function selectChapter(slug) {
  selectedSlug.value = slug
  outline.value = []
  router.replace({ query: { chapter: slug } })
}
watch(() => route.params.tutorialId, load, { immediate: true })
</script>

<template>
  <main class="page-container tutorial-preview">
    <header class="tutorial-preview__header"><div><p class="eyebrow">DRAFT PREVIEW · 工作区预览</p><h1>{{ tutorial?.title || '教程预览' }}</h1><p>这里展示当前草稿，读者页面仍以最近一次公开版本为准。</p></div><button type="button" @click="router.push(`/useradmin/tutorials/${route.params.tutorialId}/curriculum`)">返回课程结构</button></header>
    <p v-if="loading">正在生成预览…</p><p v-else-if="error" class="error" role="alert">{{ error }}</p>
    <div v-else-if="tutorial" class="tutorial-preview__layout">
      <aside class="tutorial-preview__curriculum"><h2>课程目录</h2><section v-for="group in tutorial.groups" :key="group.id"><h3>{{ group.title }}</h3><button v-for="chapter in group.chapters" :key="chapter.id" type="button" :class="{ active: chapter.slug === selectedSlug }" @click="selectChapter(chapter.slug)">{{ chapter.title }}</button></section></aside>
      <article v-if="selected" class="tutorial-preview__article"><h2>{{ selected.title }}</h2><p v-if="selected.summary">{{ selected.summary }}</p><BlogProse :key="selected.id" :markdown="selected.bodyMarkdown" @outline="outline = $event" /><section v-if="selected.cards?.length" class="tutorial-preview__extra"><h3>知识卡片</h3><details v-for="card in selected.cards" :key="card.id"><summary>{{ card.frontText }}</summary><BlogProse :markdown="card.backMarkdown" /></details></section><section v-if="selected.questions?.length" class="tutorial-preview__extra"><h3>章节问题</h3><details v-for="question in selected.questions" :key="question.id"><summary>{{ question.questionText }}</summary><BlogProse :markdown="question.referenceAnswer" /></details></section></article>
      <aside class="tutorial-preview__outline"><h2>本页导航</h2><ArticleOutline :items="outline" hide-title embedded /></aside>
    </div>
  </main>
</template>

<style scoped>
.tutorial-preview__header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin:24px 0 32px}.tutorial-preview__header h1{font-size:32px;margin:8px 0}.tutorial-preview__header p:last-child{color:var(--text-secondary)}.tutorial-preview__layout{display:grid;grid-template-columns:240px minmax(0,760px) 200px;justify-content:center;gap:32px;align-items:start}.tutorial-preview__curriculum,.tutorial-preview__outline{position:sticky;top:100px}.tutorial-preview__curriculum h2,.tutorial-preview__outline h2{font-size:17px;margin-bottom:16px}.tutorial-preview__curriculum section{border-top:1px solid var(--border);padding:14px 0}.tutorial-preview__curriculum h3{font-size:14px;margin-bottom:8px}.tutorial-preview__curriculum button{display:block;width:100%;text-align:left;padding:9px 10px;border:0;border-radius:8px;background:transparent;color:var(--text-secondary);cursor:pointer}.tutorial-preview__curriculum button.active,.tutorial-preview__curriculum button:hover{background:var(--bg-subtle);color:var(--primary)}.tutorial-preview__article{min-width:0}.tutorial-preview__article h2{font-size:32px;margin-bottom:20px}.tutorial-preview__article>p{color:var(--text-secondary);margin-bottom:24px}.tutorial-preview__extra{margin-top:38px;border-top:1px solid var(--border);padding-top:20px}.tutorial-preview__extra h3{margin-bottom:16px}.tutorial-preview__extra details{padding:14px;border:1px solid var(--border);border-radius:10px;margin-bottom:10px}.tutorial-preview__extra summary{cursor:pointer}.tutorial-preview__extra details>div{margin-top:18px}.tutorial-preview__outline{border-left:1px solid var(--border);padding-left:16px}
@media(max-width:1150px){.tutorial-preview__layout{grid-template-columns:200px minmax(0,1fr)}.tutorial-preview__outline{display:none}}@media(max-width:760px){.tutorial-preview__layout{display:block}.tutorial-preview__curriculum{position:static;margin-bottom:30px}}
</style>
