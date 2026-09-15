<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchPublicProject } from '@/api/portfolio'
import { resolvePortfolioLinks } from '@/lib/portfolioLinks'

const route = useRoute()
const title = ref('在线访问')
const error = ref('')
onMounted(async () => {
  try {
    const project = await fetchPublicProject(String(route.params.slug))
    title.value = project.title
    const links = resolvePortfolioLinks(project)
    if (links.onlineAccessUrl) {
      window.location.replace(links.onlineAccessUrl)
      return
    }
    error.value = '此作品暂未提供在线访问地址或代码仓库。'
  } catch {
    error.value = '在线访问暂时不可用。'
  }
})
</script>

<template>
  <main class="portfolio-live">
    <header><RouterLink :to="`/portfolio/${route.params.slug}`">← 返回案例</RouterLink><strong>{{ title }}</strong></header>
    <p class="portfolio-live__state">{{ error || '正在跳转…' }}</p>
  </main>
</template>

<style scoped>
.portfolio-live{width:100%;min-height:100dvh;background:var(--bg-page)}.portfolio-live header{display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:56px;padding:10px 20px;border-bottom:1px solid var(--border);background:var(--bg-surface)}.portfolio-live header a{font-size:13px;font-weight:700}.portfolio-live__state{padding:64px 20px;text-align:center;color:var(--text-secondary)}
</style>
