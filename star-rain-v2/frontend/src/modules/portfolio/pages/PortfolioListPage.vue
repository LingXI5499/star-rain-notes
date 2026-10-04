<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listPublicWorks } from '../api/portfolioApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'

const route = useRoute()
const router = useRouter()
const { contentPath } = useViewMode()
const types = [
  { value: '', label: '全部作品' },
  { value: 'SOFTWARE', label: '软件' },
  { value: 'VIDEO', label: '视频' },
  { value: 'MUSIC', label: '音乐' },
  { value: 'WRITING', label: '写作' },
  { value: 'OTHER', label: '其他' },
]
const page = ref({ items: [], total: 0, page: 1, pageSize: 12 })
const loading = ref(false)
const error = ref('')
const selectedType = computed(() => types.some((item) => item.value === route.query.type) ? route.query.type : '')
const pageNumber = computed(() => Math.max(1, Number(route.query.page) || 1))
const pageCount = computed(() => Math.max(1, Math.ceil(page.value.total / 12)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await listPublicWorks({ page: pageNumber.value, pageSize: 12, type: selectedType.value || undefined })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function navigate(type, number = 1) {
  router.push({ path: contentPath('/portfolio'), query: { ...(type ? { type } : {}), ...(number > 1 ? { page: number } : {}) } })
}

onMounted(load)
watch(() => [route.query.type, route.query.page], load)
</script>

<template>
  <section class="portfolio-list">
    <header class="portfolio-list__hero">
      <p class="public-eyebrow">PORTFOLIO · CASE STUDIES</p>
      <h1 class="public-display">把想法做成作品。</h1>
      <p>从设计、构建到上线，记录每件作品背后的过程与判断。</p>
    </header>
    <nav class="portfolio-list__types" aria-label="作品类型">
      <button v-for="item in types" :key="item.value" type="button"
        :class="{ active: selectedType === item.value }" @click="navigate(item.value)">{{ item.label }}</button>
    </nav>
    <p v-if="loading" class="portfolio-list__state">正在加载作品…</p>
    <p v-else-if="error" class="portfolio-list__state" role="alert">{{ error }}</p>
    <p v-else-if="!page.items.length" class="portfolio-list__state">这里还没有已发布的作品。</p>
    <div v-else class="portfolio-list__grid">
      <RouterLink v-for="work in page.items" :key="work.id" :to="contentPath(`/portfolio/${work.slug}`)" class="portfolio-card">
        <div class="portfolio-card__visual">
          <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" />
          <span v-else>{{ work.title.slice(0, 1) }}</span>
        </div>
        <div class="portfolio-card__content">
          <small>{{ types.find((item) => item.value === work.workType)?.label || '作品' }}</small>
          <h2>{{ work.title }}</h2>
          <p>{{ work.summary }}</p>
          <b>查看作品 <span aria-hidden="true">↗</span></b>
        </div>
      </RouterLink>
    </div>
    <nav v-if="pageCount > 1" class="portfolio-list__pages" aria-label="分页">
      <button type="button" :disabled="pageNumber <= 1" @click="navigate(selectedType, pageNumber - 1)">上一页</button>
      <span>{{ pageNumber }} / {{ pageCount }}</span>
      <button type="button" :disabled="pageNumber >= pageCount" @click="navigate(selectedType, pageNumber + 1)">下一页</button>
    </nav>
  </section>
</template>

<style scoped>
.portfolio-list{width:min(1160px,100%);margin:auto;padding:32px 0 80px}.portfolio-list__hero{display:grid;grid-template-columns:1fr 320px;gap:16px;align-items:end;padding:20px 0 42px;border-bottom:1px solid var(--border)}.portfolio-list__hero .public-eyebrow{grid-column:1/-1;margin:0}.portfolio-list__hero h1{margin:0}.portfolio-list__hero>p:last-child{margin:0;color:var(--text-secondary);line-height:1.8}.portfolio-list__types{display:flex;gap:8px;flex-wrap:wrap;padding:25px 0 30px}.portfolio-list__types button,.portfolio-list__pages button{padding:9px 16px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);background:var(--bg-surface);cursor:pointer}.portfolio-list__types button.active,.portfolio-list__types button:hover{color:var(--on-primary);background:var(--primary);border-color:var(--primary)}.portfolio-list__grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}.portfolio-card{overflow:hidden;border:1px solid var(--border);border-radius:18px;color:var(--text-primary);background:var(--bg-surface);text-decoration:none;transition:transform .2s,border-color .2s}.portfolio-card:hover{transform:translateY(-3px);border-color:var(--primary)}.portfolio-card__visual{display:grid;place-items:center;aspect-ratio:16/10;background:linear-gradient(135deg,var(--bg-subtle),color-mix(in srgb,var(--primary) 15%,var(--bg-surface)))}.portfolio-card__visual img{width:100%;height:100%;object-fit:cover}.portfolio-card__visual span{font:700 76px Georgia,serif;color:var(--primary);opacity:.5}.portfolio-card__content{padding:22px}.portfolio-card small{color:var(--accent);font-size:11px;letter-spacing:.1em}.portfolio-card h2{margin:9px 0;font-size:22px}.portfolio-card p{min-height:3em;margin:0;color:var(--text-secondary);font-size:14px;line-height:1.7}.portfolio-card b{display:block;margin-top:24px;color:var(--primary);font-size:13px}.portfolio-list__state{padding:80px 20px;border:1px dashed var(--border);border-radius:18px;text-align:center;color:var(--text-muted)}.portfolio-list__pages{display:flex;justify-content:center;align-items:center;gap:16px;margin-top:32px}.portfolio-list__pages button:disabled{opacity:.45;cursor:default}@media(max-width:900px){.portfolio-list__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:640px){.portfolio-list__hero{grid-template-columns:1fr}.portfolio-list__grid{grid-template-columns:1fr}}
</style>
