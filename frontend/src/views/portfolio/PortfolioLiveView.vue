<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchPublicProject } from '@/api/portfolio'
import { resolvePortfolioLinks } from '@/lib/portfolioLinks'

const route = useRoute()
const title = ref('在线访问')
const error = ref('')
const prototypeUrl = ref('')
onMounted(async () => {
  try {
    const project = await fetchPublicProject(String(route.params.slug))
    title.value = project.title
    const links = resolvePortfolioLinks(project)
    if (links.demoUrl) {
      window.location.replace(links.demoUrl)
      return
    }
    if (links.prototypeEntryUrl) {
      const separator = links.prototypeEntryUrl.includes('?') ? '&' : '?'
      prototypeUrl.value = `${links.prototypeEntryUrl}${separator}sandbox=v1`
      return
    }
    error.value = '此作品暂未提供在线访问。'
  } catch {
    error.value = '在线访问暂时不可用。'
  }
})
</script>

<template>
  <main class="portfolio-live">
    <header><RouterLink :to="`/portfolio/${route.params.slug}`">← 返回案例</RouterLink><strong>{{ title }}</strong></header>
    <iframe
      v-if="prototypeUrl"
      :src="prototypeUrl"
      :title="`${title} 静态原型`"
      sandbox="allow-scripts"
      referrerpolicy="no-referrer"
    />
    <p v-else class="portfolio-live__state">{{ error || '正在跳转…' }}</p>
  </main>
</template>

<style scoped>
.portfolio-live{display:grid;width:100%;height:100dvh;grid-template-rows:auto 1fr;background:var(--bg-page)}.portfolio-live header{display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:56px;padding:10px 20px;border-bottom:1px solid var(--border);background:var(--bg-surface)}.portfolio-live header a{font-size:13px;font-weight:700}.portfolio-live iframe{width:100%;height:100%;border:0;background:#fff}.portfolio-live__state{padding:64px 20px;text-align:center;color:var(--text-secondary)}
</style>
