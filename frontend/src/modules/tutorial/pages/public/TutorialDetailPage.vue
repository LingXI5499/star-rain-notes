<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicTutorial } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { VIEW_MODE, accountPath, resolveViewMode } from '../../../../shared/viewMode'

const route = useRoute()
const tutorial = ref(null)
const error = ref('')
const loading = ref(true)
const tutorialPath = (suffix = '') => resolveViewMode(route.path) === VIEW_MODE.ACCOUNT
  ? accountPath(`/tutorials${suffix}`) : `/tutorials${suffix}`
const chapters = computed(() => tutorial.value?.groups?.flatMap((group) => group.chapters || []) || [])
function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('zh-CN')
}
async function load() {
  loading.value = true
  error.value = ''
  try { tutorial.value = await getPublicTutorial(route.params.tutorialSlug) }
  catch (cause) { tutorial.value = null; error.value = errorMessage(cause) }
  finally { loading.value = false }
}
watch(() => route.params.tutorialSlug, load, { immediate: true })
</script>

<template>
  <section class="page-container tutorial-detail">
    <p v-if="loading">加载中…</p>
    <div v-else-if="error" role="alert"><p>{{ error }}</p><RouterLink :to="tutorialPath()">返回教程中心</RouterLink></div>
    <template v-else-if="tutorial">
      <main class="tutorial-detail__main">
        <nav class="tutorial-detail__breadcrumb" aria-label="面包屑"><RouterLink :to="tutorialPath()">教程</RouterLink> / <RouterLink :to="{ path: tutorialPath(), query: { categorySlug: tutorial.categorySlug } }">{{ tutorial.categoryName }}</RouterLink></nav>
        <h1>{{ tutorial.title }}</h1><p class="tutorial-detail__summary">{{ tutorial.summary }}</p><p class="tutorial-detail__meta">发布于 {{ formatDate(tutorial.publishedAt) }} · {{ chapters.length }} 个公开章节</p>
        <RouterLink v-if="chapters.length" :to="tutorialPath(`/${tutorial.slug}/${chapters[0].slug}`)" class="tutorial-detail__start">开始学习 →</RouterLink>
        <h2>课程目录</h2><div v-for="group in tutorial.groups" :key="group.id" class="tutorial-detail__group"><h3>{{ group.title }}</h3><RouterLink v-for="(chapter, index) in group.chapters" :key="chapter.id" :to="tutorialPath(`/${tutorial.slug}/${chapter.slug}`)"><span>{{ index + 1 }}</span>{{ chapter.title }}<span>→</span></RouterLink></div>
      </main>
      <aside class="tutorial-detail__aside"><h2>教程信息</h2><dl><dt>分类</dt><dd>{{ tutorial.categoryName }}</dd><dt>公开章节</dt><dd>{{ chapters.length }}</dd><dt>发布于</dt><dd>{{ formatDate(tutorial.publishedAt) }}</dd></dl><RouterLink v-if="chapters.length" :to="tutorialPath(`/${tutorial.slug}/${chapters[0].slug}`)">开始学习 →</RouterLink></aside>
    </template>
  </section>
</template>

<style scoped>
.tutorial-detail{display:grid;grid-template-columns:minmax(0,820px) minmax(210px,260px);justify-content:center;gap:48px;align-items:start;padding-block:36px}.tutorial-detail__main{min-width:0}.tutorial-detail__breadcrumb{color:var(--text-muted);margin-bottom:28px}.tutorial-detail__breadcrumb a{color:var(--primary)}.tutorial-detail h1{font-size:clamp(32px,4vw,44px);line-height:1.2}.tutorial-detail__summary{font-size:17px;line-height:1.8;color:var(--text-secondary);margin:20px 0 10px}.tutorial-detail__meta{color:var(--text-muted);margin-bottom:30px}.tutorial-detail__start,.tutorial-detail__aside>a{display:inline-block;padding:13px 20px;border-radius:999px;background:var(--primary);color:var(--on-primary);font-weight:700;margin-bottom:36px}.tutorial-detail__main h2{font-size:24px;margin-bottom:16px}.tutorial-detail__group{border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);overflow:hidden;margin-bottom:18px}.tutorial-detail__group h3{padding:16px 20px;background:var(--bg-subtle);font-size:17px}.tutorial-detail__group a{display:flex;align-items:center;gap:16px;padding:14px 20px;border-top:1px solid var(--border);color:var(--text-primary)}.tutorial-detail__group a:hover{color:var(--primary);background:var(--bg-subtle)}.tutorial-detail__group a span:last-child{margin-left:auto}.tutorial-detail__group a span:first-child{color:var(--text-muted)}.tutorial-detail__aside{position:sticky;top:100px;padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.tutorial-detail__aside h2{font-size:18px}.tutorial-detail__aside dl{display:grid;grid-template-columns:1fr auto;gap:12px;margin:20px 0;color:var(--text-secondary)}.tutorial-detail__aside dd{text-align:right}.tutorial-detail__aside>a{margin:0}
@media(max-width:850px){.tutorial-detail{grid-template-columns:1fr}.tutorial-detail__aside{position:static}}
</style>
