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
const featured = computed(() => page.value.items[0] || null)
const remaining = computed(() => page.value.items.slice(1))
const stageLabels = { DEVELOPING: '开发中', COMPLETED: '已完成', ONLINE: '已上线' }

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
      <div><p class="public-eyebrow">PORTFOLIO · ENGINEERING CASE STUDIES</p>
      <h1 class="public-display">把复杂问题，做成可靠的产品。</h1></div>
      <p>从需求拆解、架构设计到上线复盘，记录每个项目背后的判断与工程过程。</p>
    </header>
    <nav class="portfolio-list__types" aria-label="作品类型">
      <button v-for="item in types" :key="item.value" type="button"
        :class="{ active: selectedType === item.value }" @click="navigate(item.value)">{{ item.label }}</button>
    </nav>
    <p v-if="loading" class="portfolio-list__state">正在加载作品…</p>
    <p v-else-if="error" class="portfolio-list__state" role="alert">{{ error }}</p>
    <p v-else-if="!page.items.length" class="portfolio-list__state">这里还没有已发布的作品。</p>
    <div v-else class="portfolio-list__works">
      <RouterLink v-if="featured" :to="contentPath(`/portfolio/${featured.slug}`)" class="portfolio-feature">
        <div class="portfolio-feature__visual"><img v-if="featured.coverUrl" :src="featured.coverUrl" :alt="featured.title" /><span v-else>CASE STUDY<br><b>{{ featured.title }}</b></span></div>
        <div class="portfolio-feature__body"><small>FEATURED CASE · {{ stageLabels[featured.typeDetail?.projectStage] || '工程实践' }}</small>
          <h2>{{ featured.title }}</h2><strong v-if="featured.typeDetail?.role">{{ featured.typeDetail.role }}</strong>
          <p>{{ featured.summary }}</p><div v-if="featured.typeDetail?.techStack?.length" class="portfolio-stack"><span v-for="tech in featured.typeDetail.techStack.slice(0, 6)" :key="tech">{{ tech }}</span></div>
          <b>查看完整案例 →</b></div>
      </RouterLink>
      <section v-if="remaining.length" class="portfolio-more"><header><div><small>MORE WORK</small><h2>更多项目</h2></div><span>{{ page.total }} 个工程案例</span></header>
        <RouterLink v-for="work in remaining" :key="work.id" :to="contentPath(`/portfolio/${work.slug}`)" class="portfolio-row">
          <div class="portfolio-row__visual"><img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" /><span v-else>{{ work.title.slice(0, 1) }}</span></div>
          <div class="portfolio-row__body"><small>{{ stageLabels[work.typeDetail?.projectStage] || types.find((item) => item.value === work.workType)?.label || '作品' }}</small><h3>{{ work.title }}</h3><p>{{ work.summary }}</p></div><strong>探索案例 →</strong>
        </RouterLink>
      </section>
    </div>
    <nav v-if="pageCount > 1" class="portfolio-list__pages" aria-label="分页">
      <button type="button" :disabled="pageNumber <= 1" @click="navigate(selectedType, pageNumber - 1)">上一页</button>
      <span>{{ pageNumber }} / {{ pageCount }}</span>
      <button type="button" :disabled="pageNumber >= pageCount" @click="navigate(selectedType, pageNumber + 1)">下一页</button>
    </nav>
  </section>
</template>

<style scoped>
.portfolio-list{width:min(1180px,100%);margin:auto;padding:20px 0 80px}.portfolio-list__hero{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(240px,.65fr);gap:48px;align-items:end;padding:36px 0 48px;border-bottom:1px solid var(--border)}.portfolio-list__hero .public-eyebrow{margin:0 0 16px}.portfolio-list__hero h1{max-width:16ch;margin:0;line-height:1.08}.portfolio-list__hero>p{margin:0;color:var(--text-secondary);line-height:1.9}.portfolio-list__types{display:flex;gap:8px;flex-wrap:wrap;padding:26px 0 38px}.portfolio-list__types button,.portfolio-list__pages button{padding:9px 16px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);background:var(--bg-surface);cursor:pointer}.portfolio-list__types button.active,.portfolio-list__types button:hover{color:var(--on-primary);background:var(--primary);border-color:var(--primary)}.portfolio-feature{display:grid;grid-template-columns:1.15fr .85fr;gap:44px;align-items:center;color:var(--text-primary)}.portfolio-feature__visual,.portfolio-row__visual{overflow:hidden;display:grid;place-items:center;border:1px solid var(--border);border-radius:18px;background:radial-gradient(circle at 25% 20%,color-mix(in srgb,var(--primary) 18%,transparent),transparent 50%),linear-gradient(145deg,var(--bg-subtle),color-mix(in srgb,var(--accent) 12%,var(--bg-surface)))}.portfolio-feature__visual{min-height:345px}.portfolio-feature__visual img,.portfolio-row__visual img{width:100%;height:100%;object-fit:cover}.portfolio-feature__visual>span{max-width:80%;color:var(--primary);font:700 13px/1.7 Georgia,serif;letter-spacing:.15em}.portfolio-feature__visual b{display:block;margin-top:12px;color:var(--text-primary);font:700 clamp(23px,3vw,38px)/1.25 sans-serif;letter-spacing:0}.portfolio-feature__body small,.portfolio-more small,.portfolio-row small{color:var(--accent);font-size:11px;letter-spacing:.13em}.portfolio-feature__body h2{margin:14px 0;font-size:clamp(26px,3vw,40px);line-height:1.25}.portfolio-feature__body>strong{color:var(--primary)}.portfolio-feature__body p,.portfolio-row__body p{color:var(--text-secondary);line-height:1.85}.portfolio-stack{display:flex;gap:8px;flex-wrap:wrap;margin:22px 0}.portfolio-stack span{padding:6px 9px;border:1px solid var(--border);border-radius:6px;color:var(--text-muted);font-size:12px}.portfolio-feature__body>b,.portfolio-row>strong{color:var(--primary);font-size:13px}.portfolio-more{margin-top:72px}.portfolio-more>header{display:flex;justify-content:space-between;align-items:end;padding-bottom:18px;border-bottom:1px solid var(--border)}.portfolio-more h2{margin:7px 0 0}.portfolio-more>header>span{color:var(--text-muted);font-size:13px}.portfolio-row{display:grid;grid-template-columns:205px minmax(0,1fr) auto;gap:26px;align-items:center;padding:24px 0;border-bottom:1px solid var(--border);color:var(--text-primary)}.portfolio-row__visual{height:128px}.portfolio-row__visual span{color:var(--primary);font-size:62px;font-weight:700}.portfolio-row h3{margin:8px 0;font-size:22px}.portfolio-row__body p{margin:0;font-size:13px}.portfolio-list__state{padding:80px 20px;border:1px dashed var(--border);border-radius:18px;text-align:center;color:var(--text-muted)}.portfolio-list__pages{display:flex;justify-content:center;align-items:center;gap:16px;margin-top:32px}.portfolio-list__pages button:disabled{opacity:.45;cursor:default}@media(max-width:800px){.portfolio-list__hero,.portfolio-feature{grid-template-columns:1fr;gap:24px}.portfolio-row{grid-template-columns:140px 1fr}.portfolio-row>strong{grid-column:2}.portfolio-feature__visual{min-height:260px}}@media(max-width:550px){.portfolio-row{grid-template-columns:1fr}.portfolio-row__visual{height:170px}.portfolio-row>strong{grid-column:auto}}
</style>
