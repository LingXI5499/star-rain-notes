<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicWork } from '../api/portfolioApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogProse from '../../blog/components/BlogProse.vue'
import ArticleOutline from '../../blog/components/ArticleOutline.vue'

const route = useRoute()
const { contentPath } = useViewMode()
const work = ref(null)
const outline = ref([])
const error = ref('')
const loading = ref(false)
const cover = computed(() => work.value?.media?.find((item) => item.usageType === 'COVER'))
const gallery = computed(() => work.value?.media?.filter((item) => item.usageType === 'SCREENSHOT') || [])
const links = computed(() => work.value?.links?.filter((item) => item.enabled) || [])
const projectStage = computed(() => ({ DEVELOPING: '开发中', COMPLETED: '已完成', ONLINE: '已上线' }[work.value?.typeDetail?.projectStage] || '工程实践'))

async function load() {
  loading.value = true
  error.value = ''
  work.value = null
  outline.value = []
  try {
    work.value = await getPublicWork(route.params.slug)
  } catch (cause) {
    error.value = cause?.response?.status === 404 ? '作品不存在或尚未公开。' : errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <article class="work-detail">
    <p v-if="loading" class="work-detail__state">正在加载作品…</p>
    <p v-else-if="error" class="work-detail__state" role="alert">{{ error }} <RouterLink :to="contentPath('/portfolio')">返回作品列表</RouterLink></p>
    <template v-else-if="work">
      <header class="work-detail__hero">
        <RouterLink :to="contentPath('/portfolio')">← 全部作品</RouterLink>
        <p class="public-eyebrow">CASE STUDY · {{ projectStage }}</p>
        <h1 class="public-display">{{ work.title }}</h1>
        <p class="work-detail__summary">{{ work.summary }}</p>
        <p v-if="work.typeDetail" class="work-detail__meta"><span v-if="work.typeDetail.role">{{ work.typeDetail.role }}</span><span v-for="tech in work.typeDetail.techStack?.slice(0, 4)" :key="tech">{{ tech }}</span></p>
        <div class="work-detail__links">
          <a v-for="link in links" :key="link.id" :href="link.url" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a>
        </div>
      </header>
      <img v-if="cover?.url" class="work-detail__cover" :src="cover.url" :alt="cover.caption || work.title" />
      <div v-if="gallery.length" class="work-detail__gallery">
        <figure v-for="item in gallery" :key="item.id"><img :src="item.url" :alt="item.caption || work.title" /><figcaption>{{ item.caption }}</figcaption></figure>
      </div>
      <div class="work-detail__reading">
        <main>
          <BlogProse :markdown="work.bodyMarkdown || ''" @outline="outline = $event" />
        </main>
        <aside v-if="outline.length"><ArticleOutline :items="outline" /></aside>
      </div>
    </template>
  </article>
</template>

<style scoped>
.work-detail{width:min(1160px,100%);margin:auto;padding:30px 0 90px}.work-detail__hero{max-width:850px;margin:auto;padding:20px 0 40px;text-align:center}.work-detail__hero>a{display:inline-block;margin-bottom:45px;color:var(--text-muted);text-decoration:none}.work-detail__hero .public-eyebrow{margin-bottom:15px}.work-detail__hero h1{margin:0}.work-detail__summary{margin:20px auto 0;max-width:680px;color:var(--text-secondary);font-size:18px;line-height:1.8}.work-detail__links{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin-top:30px}.work-detail__links a{padding:10px 17px;border:1px solid var(--border);border-radius:999px;color:var(--primary);text-decoration:none}.work-detail__cover{display:block;width:100%;max-height:560px;object-fit:cover;border-radius:20px}.work-detail__gallery{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:24px}.work-detail__gallery figure{margin:0}.work-detail__gallery img{width:100%;border-radius:14px}.work-detail__gallery figcaption{padding:7px;color:var(--text-muted);font-size:12px}.work-detail__reading{display:grid;grid-template-columns:minmax(0,760px) 230px;justify-content:center;gap:56px;margin-top:55px}.work-detail__reading main{min-width:0}.work-detail__reading aside{position:sticky;top:100px;align-self:start}.work-detail__state{padding:100px 20px;color:var(--text-muted);text-align:center}.work-detail__state a{margin-left:8px;color:var(--primary)}@media(max-width:900px){.work-detail__reading{grid-template-columns:1fr}.work-detail__reading aside{display:none}}@media(max-width:620px){.work-detail__gallery{grid-template-columns:1fr}}
.work-detail__meta{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin:20px auto 0;color:var(--text-muted);font-size:13px}.work-detail__meta span+span::before{content:'·';margin-right:12px;color:var(--border-strong)}
</style>
