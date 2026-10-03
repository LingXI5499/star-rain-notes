<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listPublicCategories, listPublicTutorials } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const tutorials = ref([])
const selectedSlug = ref('')
const search = ref('')
const loading = ref(true)
const error = ref('')

const selectedCategory = computed(() => categories.value.find((item) => item.slug === selectedSlug.value))
const filtered = computed(() => tutorials.value.filter((item) => {
  if (selectedCategory.value && item.categoryId !== selectedCategory.value.id) return false
  const term = search.value.trim().toLocaleLowerCase()
  return !term || `${item.title} ${item.summary || ''}`.toLocaleLowerCase().includes(term)
}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryRows, first] = await Promise.all([
      listPublicCategories(), listPublicTutorials({ page: 1, pageSize: 100 }),
    ])
    categories.value = categoryRows
    tutorials.value = [...(first.items || [])]
    const pages = Math.ceil((first.total || 0) / 100)
    for (let page = 2; page <= pages; page += 1) {
      const next = await listPublicTutorials({ page, pageSize: 100 })
      tutorials.value.push(...(next.items || []))
    }
    selectedSlug.value = typeof route.query.categorySlug === 'string' ? route.query.categorySlug : ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function selectCategory(slug) {
  selectedSlug.value = slug
  search.value = ''
  router.replace({ query: slug ? { categorySlug: slug } : {} })
}

watch(() => route.query.categorySlug, (value) => { selectedSlug.value = typeof value === 'string' ? value : '' })
onMounted(load)
</script>

<template>
  <section class="page-container tutorial-catalog">
    <header class="tutorial-catalog__hero">
      <div><p class="eyebrow">LEARNING PATHS · 教程中心</p><h1>选择一门教程，开始系统学习</h1><p>左侧按知识体系浏览，右侧从教程卡片进入；课程内提供完整目录、上下篇与页内导航。</p></div>
      <div class="tutorial-catalog__summary"><strong>{{ filtered.length }}</strong><span>{{ selectedCategory ? '当前分类教程' : '公开教程' }}</span></div>
    </header>
    <div class="tutorial-catalog__layout">
      <aside class="tutorial-catalog__sidebar">
        <header><strong>教程目录</strong><small>{{ categories.length }} 类</small></header>
        <nav aria-label="教程分类"><button type="button" :class="{ active: !selectedSlug }" @click="selectCategory('')">全部教程 <span>{{ tutorials.length }}</span></button><button v-for="category in categories" :key="category.id" type="button" :class="{ active: selectedSlug === category.slug }" @click="selectCategory(category.slug)">{{ category.name }} <span>{{ category.tutorialCount }}</span></button></nav>
      </aside>
      <main class="tutorial-catalog__content">
        <header><div><p>教程 / {{ selectedCategory?.name || '全部教程' }}</p><h2>{{ selectedCategory?.name || '全部教程' }}</h2><small>共 {{ filtered.length }} 门可学习教程</small></div><input v-model="search" type="search" placeholder="筛选当前教程" aria-label="筛选当前教程" /></header>
        <p v-if="loading" class="tutorial-catalog__empty">正在加载教程…</p>
        <p v-else-if="error" class="tutorial-catalog__empty" role="alert">{{ error }}</p>
        <p v-else-if="!filtered.length" class="tutorial-catalog__empty">当前分类暂无匹配教程。</p>
        <div v-else class="tutorial-catalog__grid"><RouterLink v-for="tutorial in filtered" :key="tutorial.id" :to="`/tutorials/${tutorial.slug}`" class="tutorial-card"><small>{{ tutorial.categoryName }}</small><h3>{{ tutorial.title }}</h3><p>{{ tutorial.summary }}</p><div><span>{{ tutorial.chapterCount }} 个章节</span><strong>进入课程 →</strong></div></RouterLink></div>
      </main>
    </div>
  </section>
</template>

<style scoped>
.tutorial-catalog__hero{display:flex;align-items:end;justify-content:space-between;gap:28px;padding:32px 0;margin-bottom:32px;border-bottom:1px solid var(--border)}
.tutorial-catalog__hero h1{font-size:clamp(32px,4vw,48px);line-height:1.15;margin:10px 0}
.tutorial-catalog__hero p:last-child{color:var(--text-secondary);line-height:1.8}
.tutorial-catalog__summary{min-width:130px;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);text-align:center}
.tutorial-catalog__summary strong{display:block;color:var(--primary);font-size:28px}.tutorial-catalog__summary span{font-size:12px;color:var(--text-muted)}
.tutorial-catalog__layout{display:grid;grid-template-columns:260px minmax(0,1fr);gap:32px;align-items:start}
.tutorial-catalog__sidebar{position:sticky;top:100px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);overflow:hidden}
.tutorial-catalog__sidebar header,.tutorial-catalog__content header{display:flex;align-items:center;justify-content:space-between;gap:16px}
.tutorial-catalog__sidebar header{padding:18px;border-bottom:1px solid var(--border)}.tutorial-catalog__sidebar small{color:var(--text-muted)}
.tutorial-catalog__sidebar nav{display:grid;gap:4px;padding:10px}.tutorial-catalog__sidebar button{display:flex;justify-content:space-between;text-align:left;border:0;border-radius:10px;padding:12px;background:transparent;color:var(--text-secondary);cursor:pointer}
.tutorial-catalog__sidebar button:hover,.tutorial-catalog__sidebar button.active{color:var(--primary);background:var(--bg-subtle)}
.tutorial-catalog__content{min-width:0}.tutorial-catalog__content header{margin-bottom:24px}.tutorial-catalog__content header p,.tutorial-catalog__content header small{color:var(--text-muted);font-size:13px}.tutorial-catalog__content h2{font-size:26px;margin:5px 0}
.tutorial-catalog__content input{width:min(280px,40%);border:1px solid var(--border);border-radius:12px;padding:12px;background:var(--bg-surface);color:var(--text-primary)}
.tutorial-catalog__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(280px,100%),1fr));gap:20px}.tutorial-card{display:flex;flex-direction:column;min-height:230px;padding:22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);color:var(--text-primary);transition:transform .17s,box-shadow .17s}.tutorial-card:hover{transform:translateY(-3px);box-shadow:0 14px 35px #0001}.tutorial-card small{color:var(--accent)}.tutorial-card h3{font-size:20px;line-height:1.4;margin:12px 0}.tutorial-card p{color:var(--text-secondary);line-height:1.7;flex:1}.tutorial-card div{display:flex;justify-content:space-between;gap:12px;border-top:1px solid var(--border);padding-top:16px;color:var(--text-muted);font-size:12px}.tutorial-card strong{color:var(--primary)}.tutorial-catalog__empty{padding:64px;border:1px dashed var(--border);border-radius:16px;color:var(--text-muted);text-align:center}
@media(max-width:900px){.tutorial-catalog__layout{grid-template-columns:1fr}.tutorial-catalog__sidebar{position:static}.tutorial-catalog__summary{display:none}}
@media(max-width:600px){.tutorial-catalog__content header{align-items:stretch;flex-direction:column}.tutorial-catalog__content input{width:100%}}
</style>
