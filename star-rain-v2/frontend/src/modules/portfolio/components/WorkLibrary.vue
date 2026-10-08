<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPublicWorks, getWorkTaxonomy } from '../api/portfolioApi'
import WorkCard from './WorkCard.vue'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
const route = useRoute(), router = useRouter()
const { contentPath } = useViewMode()
const taxonomy = ref({ categories: [], formats: [], tags: [] })
const result = ref({ items: [], total: 0 }), loading = ref(false), error = ref('')
const featuredWorks = ref([]), featuredLoading = ref(true), featuredError = ref('')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const pages = computed(() => Math.max(1, Math.ceil(result.value.total / 12)))
let version = 0
async function load() {
 const current = ++version; loading.value = true; error.value = ''
 try { const data = await listPublicWorks({ page: page.value, pageSize: 12, categoryId: route.query.categoryId || undefined, formatId: route.query.formatId || undefined, tagId: route.query.tagId || undefined, featured: route.query.featured === 'true' ? true : undefined }); if (current === version) result.value = data }
 catch (cause) { if (current === version) error.value = errorMessage(cause) }
 finally { if (current === version) loading.value = false }
}
async function loadFeatured() {
 featuredLoading.value = true; featuredError.value = ''
 try { const data = await listPublicWorks({ page: 1, pageSize: 3, featured: true }); featuredWorks.value = data.items.slice(0, 3) }
 catch (cause) { featuredError.value = errorMessage(cause) }
 finally { featuredLoading.value = false }
}
function filter(key, value) { router.push({ path: contentPath('/portfolio'), query: { ...route.query, [key]: value || undefined, page: undefined } }) }
function changePage(value) { router.push({ path: contentPath('/portfolio'), query: { ...route.query, page: value > 1 ? value : undefined } }) }
watch(() => route.fullPath, load, { immediate: true })
onMounted(async () => { try { taxonomy.value = await getWorkTaxonomy() } catch (cause) { error.value = errorMessage(cause) } })
onMounted(loadFeatured)
</script>
<template>
 <section class="works-library">
  <header class="works-library__hero"><div><p class="public-eyebrow">PORTFOLIO · SELECTED WORKS</p><h1 class="public-display">精选作品</h1></div><p>从软件、工具、实验与创作中，挑选值得展开讲述的作品。</p></header>
  <section class="works-featured" aria-label="精选作品">
   <p v-if="featuredLoading" class="works-state">正在加载精选作品…</p>
   <div v-else-if="featuredError" class="works-state" role="alert">{{ featuredError }} <button @click="loadFeatured">重新加载</button></div>
   <p v-else-if="!featuredWorks.length" class="works-featured__empty">精选作品正在整理中。</p>
   <template v-else><WorkCard :work="featuredWorks[0]" prominent heading-level="h2" /><div v-if="featuredWorks.length > 1" class="works-grid works-featured__more"><WorkCard v-for="work in featuredWorks.slice(1)" :key="work.id" :work="work" heading-level="h2" /></div></template>
  </section>
  <section class="works-archive" aria-labelledby="works-archive-title">
  <header class="works-archive__heading"><div><p class="public-eyebrow">EXPLORE THE ARCHIVE</p><h2 id="works-archive-title">全部作品</h2></div><p>按分类、形态和标签，找到感兴趣的作品。</p></header>
  <nav class="works-categories" aria-label="作品分类"><button :class="{ active: !route.query.categoryId }" @click="filter('categoryId','')">全部作品</button><button v-for="item in taxonomy.categories" :key="item.id" :class="{ active: route.query.categoryId === item.id }" @click="filter('categoryId',item.id)">{{ item.name }}</button></nav>
  <div class="works-filters"><label>作品形态<select :value="route.query.formatId || ''" @change="filter('formatId',$event.target.value)"><option value="">全部形态</option><option v-for="item in taxonomy.formats" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>标签<select :value="route.query.tagId || ''" @change="filter('tagId',$event.target.value)"><option value="">全部标签</option><option v-for="item in taxonomy.tags" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label><input type="checkbox" :checked="route.query.featured === 'true'" @change="filter('featured',$event.target.checked ? 'true' : '')" />只看精选</label><span>{{ result.total }} 件作品</span></div>
  <p v-if="loading" class="works-state">正在加载作品…</p><p v-else-if="error" class="works-state" role="alert">{{ error }}</p><p v-else-if="!result.items.length" class="works-state">暂无符合条件的公开作品。</p>
  <div v-else class="works-grid"><WorkCard v-for="work in result.items" :key="work.id" :work="work" /></div>
  <nav v-if="pages > 1" class="works-pages" aria-label="作品分页"><button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page >= pages" @click="changePage(page + 1)">下一页</button></nav>
  </section>
 </section>
</template>
<style scoped>
.works-library{width:min(1180px,100%);margin:auto;padding:40px 0 80px}.works-library__hero{display:grid;grid-template-columns:1.4fr 1fr;gap:50px;align-items:end;padding:20px 0 0}.works-library__hero h1{margin:12px 0 0;font-size:clamp(42px,5vw,64px);line-height:1.2}.works-library__hero>p,.works-archive__heading>p{color:var(--text-secondary);line-height:1.9;margin:0}.works-featured{margin-top:34px}.works-featured__more{margin-top:26px}.works-featured__empty{padding:30px;border:1px dashed var(--border);border-radius:18px;color:var(--text-muted);line-height:1.8}.works-archive{margin-top:72px;padding-top:42px;border-top:1px solid var(--border)}.works-archive__heading{display:flex;justify-content:space-between;align-items:end;gap:24px}.works-archive__heading h2{font-size:32px;margin:8px 0 0;line-height:1.3}.works-archive__heading .public-eyebrow{font-size:11px}.works-archive__heading>p{font-size:13px}.works-categories{display:flex;gap:10px;flex-wrap:wrap;padding:26px 0}.works-categories button,.works-pages button,.works-state button{padding:10px 18px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}.works-categories button.active{background:var(--primary);color:var(--on-primary);border-color:var(--primary)}.works-filters{display:flex;gap:20px;align-items:center;flex-wrap:wrap;margin-bottom:30px;font-size:13px;color:var(--text-secondary)}.works-filters label{display:flex;gap:10px;align-items:center;white-space:nowrap}.works-filters select{padding:8px 12px;max-width:180px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--text-primary)}.works-filters input[type=checkbox]{width:16px;height:16px;min-height:0;margin:0;accent-color:var(--primary)}.works-filters>span{margin-left:auto;color:var(--text-muted)}.works-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:26px}.works-state{padding:60px 20px;border:1px dashed var(--border);border-radius:18px;text-align:center;color:var(--text-muted)}.works-state button{margin-left:12px}.works-pages{display:flex;gap:20px;align-items:center;justify-content:center;margin-top:35px}.works-pages button:disabled{opacity:.4;cursor:default}
@media(max-width:700px){.works-library__hero,.works-grid{grid-template-columns:1fr;gap:22px}.works-archive{margin-top:48px;padding-top:30px}.works-archive__heading{display:block}.works-archive__heading>p{margin-top:12px}.works-filters{gap:12px}.works-filters>span{margin-left:0}}
</style>
