<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicWork, getAdminWork } from '../api/portfolioApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogProse from '../../blog/components/BlogProse.vue'
import ReadingAside from '../../blog/components/ReadingAside.vue'
import { estimateReadingStats } from '../../blog/support/display'
import WorkSectionRenderer from '../components/WorkSectionRenderer.vue'
import { stageLabels, safeWorkUrl } from '../support/workBlocks'
import '../../../styles/reading-layout.css'

const route = useRoute()
const { contentPath } = useViewMode()
const work = ref(null)
const outline = ref([])
const error = ref('')
const loading = ref(false)
const links = computed(() => work.value?.links?.filter(item => item.enabled && item.linkType === 'GITHUB' && safeWorkUrl(item.url)) || [])
const navigation = computed(() => work.value?.hasSections
  ? (work.value.sections || []).filter(item => item.visible !== false && item.title).map(item => ({ id: `work-section-${item.id}`, text: item.title, level: 2 }))
  : outline.value)
const stats = computed(() => estimateReadingStats(work.value?.hasSections
  ? (work.value.sections || []).filter(item => item.visible !== false).map(item => `${item.title || ''}\n${item.content || ''}`).join('\n')
  : work.value?.bodyMarkdown))
const preview = computed(() => !!route.params.workId)
const typeLabel = computed(() => ({ SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '作品' }[work.value?.workType] || '作品'))

async function load() {
  loading.value = true
  error.value = ''
  work.value = null
  outline.value = []
  try {
    work.value = await (preview.value ? getAdminWork(route.params.workId) : getPublicWork(route.params.slug))
  } catch (cause) {
    error.value = cause?.response?.status === 404 ? '作品不存在或尚未公开。' : errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => [route.params.slug, route.params.workId], load)
</script>

<template>
  <article class="work-detail page-container">
    <p v-if="loading" class="work-detail__state">正在加载作品…</p>
    <p v-else-if="error" class="work-detail__state" role="alert">{{ error }} <RouterLink :to="contentPath('/portfolio')">返回作品列表</RouterLink></p>
    <template v-else-if="work">
      <p v-if="preview" class="work-preview">作品预览 · 仅展示可见区块，不改变发布状态</p>
      <div class="work-detail__reading reading-layout">
        <ReadingAside class="work-detail__navigation reading-layout__sidebar" navigation-style="curriculum" :items="navigation" :char-count="stats.charCount" :read-minutes="stats.readMinutes" />
        <main>
      <header class="work-detail__hero">
        <RouterLink :to="contentPath('/portfolio')">← 全部作品</RouterLink>
        <p class="public-eyebrow">SELECTED WORK · {{ typeLabel }}</p>
        <h1 class="public-display">{{ work.title }}</h1>
        <p v-if="work.subtitle" class="work-detail__subtitle">{{ work.subtitle }}</p><div class="work-detail__meta"><span v-if="work.category">{{ work.category.name }}</span><span v-if="work.format">{{ work.format.name }}</span><span>{{ stageLabels[work.projectStatus] }}</span><span v-if="work.role">{{ work.role }}</span></div><div class="work-detail__tags"><span v-for="tag in work.tags || []" :key="tag.id">{{ tag.name }}</span></div><p class="work-detail__summary">{{ work.summary }}</p>
        <div v-if="links.length" class="work-detail__links">
          <a v-for="link in links" :key="link.id" :href="link.url" target="_blank" rel="noopener noreferrer">代码库{{ links.length > 1 && link.label ? ` · ${link.label}` : '' }} ↗</a>
        </div>
      </header>
      <details v-if="navigation.length" class="work-detail__mobile-navigation"><summary>本页导航</summary><ReadingAside navigation-style="curriculum" :items="navigation" :char-count="stats.charCount" :read-minutes="stats.readMinutes" /></details>
          <WorkSectionRenderer v-if="work.hasSections" :sections="work.sections" /><template v-else><BlogProse :markdown="work.bodyMarkdown || ''" @outline="outline = $event" /><div class="work-detail__gallery"><figure v-for="item in work.media?.filter(m => m.usageType === 'SCREENSHOT') || []" :key="item.id"><img :src="item.url" :alt="item.caption || work.title" /><figcaption>{{ item.caption }}</figcaption></figure></div><audio v-for="item in work.media?.filter(m => m.usageType === 'AUDIO') || []" :key="item.id" :src="item.url" controls /></template>
      <nav v-if="!preview" class="work-neighbors" aria-label="前后作品"><RouterLink v-if="work.previousWork" :to="contentPath(`/portfolio/${work.previousWork.slug}`)">← {{ work.previousWork.title }}</RouterLink><RouterLink v-if="work.nextWork" :to="contentPath(`/portfolio/${work.nextWork.slug}`)">{{ work.nextWork.title }} →</RouterLink></nav>
        </main>
      </div>
    </template>
  </article>
</template>

<style scoped>
.work-detail__mobile-navigation{display:none}.work-detail__mobile-navigation summary{cursor:pointer;color:var(--primary);font-weight:600}
.work-detail{width:100%;padding-block:34px 90px}.work-detail__hero{padding:20px 0 40px;text-align:center}.work-detail__hero>a{display:inline-block;margin-bottom:45px;color:var(--text-muted);text-decoration:none}.work-detail__hero .public-eyebrow{margin-bottom:15px}.work-detail__hero h1{margin:0}.work-detail__summary{margin:20px auto 0;max-width:680px;color:var(--text-secondary);font-size:18px;line-height:1.8}.work-detail__links{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin-top:30px}.work-detail__links a{padding:10px 17px;border:1px solid var(--border);border-radius:999px;color:var(--primary);text-decoration:none}.work-detail__gallery{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:24px}.work-detail__gallery figure{margin:0}.work-detail__gallery img{width:100%;border-radius:14px}.work-detail__gallery figcaption{padding:7px;color:var(--text-muted);font-size:12px}.work-detail__reading main{min-width:0}.work-detail__reading aside{position:sticky;top:100px;align-self:start}.work-detail__state{padding:100px 20px;color:var(--text-muted);text-align:center}.work-detail__state a{margin-left:8px;color:var(--primary)}@media(max-width:720px){.work-detail__mobile-navigation{display:block;margin-top:20px;padding:14px;border:1px solid var(--border);border-radius:12px;background:var(--bg-surface)}.work-detail__mobile-navigation :deep(.reading-aside){position:static;max-height:none;margin-top:16px;padding-right:0;border-right:0}}@media(max-width:620px){.work-detail__gallery{grid-template-columns:1fr}}
.work-detail__meta{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin:20px auto 0;color:var(--text-muted);font-size:13px}.work-detail__meta span+span::before{content:'·';margin-right:12px;color:var(--border-strong)}
.work-section-outline{display:grid;gap:16px;border-left:1px solid var(--border);padding-left:18px}.work-section-outline a{color:var(--text-secondary);font-size:13px;line-height:1.7}.work-detail__subtitle{font-size:22px;color:var(--text-secondary)}.work-detail__tags{display:flex;gap:8px;justify-content:center;flex-wrap:wrap;margin-top:18px}.work-detail__tags span{font-size:12px;border:1px solid var(--border);border-radius:999px;padding:5px 12px;color:var(--primary)}.work-preview{padding:14px 18px;border:1px solid var(--border);background:var(--bg-subtle);border-radius:12px;color:var(--primary)}
.work-neighbors{display:flex;justify-content:space-between;flex-wrap:wrap;gap:20px;border-top:1px solid var(--border);margin-top:56px;padding-top:24px}.work-neighbors a{color:var(--primary);font-size:14px;line-height:1.8;max-width:45%;text-decoration:none}
</style>
