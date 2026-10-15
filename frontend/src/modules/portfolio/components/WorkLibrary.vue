<script setup>
/**
 * WorkLibrary.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../../shared/ui/PublicSelect.vue、../../../shared/ui/PublicToggleFilter.vue、../../../shared/ui/PublicFilterBar.vue、../../../shared/ui/PublicFilterTabs.vue、../../../shared/ui/PublicPagination.vue、../../../shared/composables/publicListState。
 * 主要响应式状态：route、router、taxonomy、result、loading、error、featuredWorks、featuredLoading。
 * 阅读入口：changeSize（change大小的处理函数）；resetFilters（重置筛选条件）；load（加载当前操作）；loadFeatured（加载精选）；filter（筛选当前操作）。
 */


import PublicSelect from '../../../shared/ui/PublicSelect.vue'
import PublicToggleFilter from '../../../shared/ui/PublicToggleFilter.vue'
import PublicFilterBar from '../../../shared/ui/PublicFilterBar.vue'
import PublicFilterTabs from '../../../shared/ui/PublicFilterTabs.vue'
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPublicWorks, getWorkTaxonomy } from '../api/portfolioApi'
import WorkCard from './WorkCard.vue'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter()
const { contentPath } = useViewMode()
/**
 * taxonomy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const taxonomy = ref({ categories: [], formats: [], tags: [] })
/**
 * 结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const result = ref({ items: [], total: 0 }), loading = ref(false), error = ref('')
/**
 * 精选作品列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const featuredWorks = ref([]), featuredLoading = ref(true), featuredError = ref('')
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page = computed(() => publicPage(route.query.page))
/**
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize))
/**
 * 有效筛选条件；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeFilters = computed(() => ['categoryId', 'formatId', 'tagId', 'featured'].some(key => route.query[key]))
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 12) }) }
/**
 * 重置筛选条件。
 * 主要协作：router.push。
 */
function resetFilters() { router.push({ query: { pageSize: route.query.pageSize } }) }
let version = 0
/**
 * 加载当前操作。
 * 主要协作：listPublicWorks、errorMessage。
 * 更新的响应式状态包括 loading、error、result；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
 const current = ++version; loading.value = true; error.value = ''
 try { const data = await listPublicWorks({ page: page.value, pageSize: pageSize.value, categoryId: route.query.categoryId || undefined, formatId: route.query.formatId || undefined, tagId: route.query.tagId || undefined, featured: route.query.featured === 'true' ? true : undefined }); if (current === version) result.value = data }
 catch (cause) { if (current === version) error.value = errorMessage(cause) }
 finally { if (current === version) loading.value = false }
}
/**
 * 加载精选。
 * 主要协作：listPublicWorks、data.items.slice、errorMessage。
 * 更新的响应式状态包括 featuredLoading、featuredError、featuredWorks；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadFeatured() {
 featuredLoading.value = true; featuredError.value = ''
 try { const data = await listPublicWorks({ page: 1, pageSize: 3, featured: true }); featuredWorks.value = data.items.slice(0, 3) }
 catch (cause) { featuredError.value = errorMessage(cause) }
 finally { featuredLoading.value = false }
}
/**
 * 筛选当前操作。
 * 主要协作：router.push、contentPath。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function filter(key, value) { router.push({ path: contentPath('/portfolio'), query: { ...route.query, [key]: value || undefined, page: undefined } }) }
/**
 * change分页的处理函数。
 * 主要协作：router.push、contentPath。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function changePage(value) { router.push({ path: contentPath('/portfolio'), query: { ...route.query, page: value > 1 ? value : undefined } }) }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, load, { immediate: true })
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => { try { taxonomy.value = await getWorkTaxonomy() } catch (cause) { error.value = errorMessage(cause) } })
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(loadFeatured)
</script>
<template>
 <section class="works-library">
  <header class="works-library__hero"><div><p class="public-eyebrow">PORTFOLIO · SELECTED WORKS</p><h1 class="public-display">精选作品</h1></div><p>从软件、工具、实验与创作中，挑选值得展开讲述的作品。</p></header>
  <section class="works-featured" aria-label="精选作品">
   <p v-if="featuredLoading" class="works-state">正在加载精选作品…</p>
   <div v-else-if="featuredError" class="works-state" role="alert">{{ featuredError }} <button @click="loadFeatured">重新加载</button></div>
   <p v-else-if="!featuredWorks.length" class="works-featured__empty">精选作品正在整理中。</p>
   <div v-else class="works-featured__list"><WorkCard v-for="(work, index) in featuredWorks" :key="work.id" :work="work" prominent :reverse="index % 2 === 1" heading-level="h2" /></div>
  </section>
  <section class="works-archive" aria-labelledby="works-archive-title">
  <header class="works-archive__heading"><div><p class="public-eyebrow">EXPLORE THE ARCHIVE</p><h2 id="works-archive-title">全部作品</h2></div><p>按分类、形态和标签，找到感兴趣的作品。</p></header>
  <PublicFilterBar label="作品筛选" :active="activeFilters" :total="result.total" unit="件作品" @reset="resetFilters">
    <template #tabs><PublicFilterTabs label="作品分类" :model-value="route.query.categoryId || ''" :options="[{ value: '', label: '全部作品' }, ...taxonomy.categories.map(item => ({ value: item.id, label: item.name }))]" @update:model-value="filter('categoryId', $event)" /></template>
    <details class="works-secondary-filters" :open="!!(route.query.formatId || route.query.tagId || route.query.featured)"><summary>更多筛选</summary><div>
    <PublicSelect label="作品形态" :model-value="route.query.formatId || ''" :options="[{ value: '', label: '全部形态' }, ...taxonomy.formats.map(item => ({ value: item.id, label: item.name }))]" @update:model-value="filter('formatId', $event)" />
    <PublicSelect label="标签" :model-value="route.query.tagId || ''" :options="[{ value: '', label: '全部标签' }, ...taxonomy.tags.map(item => ({ value: item.id, label: item.name }))]" @update:model-value="filter('tagId', $event)" />
    <PublicToggleFilter label="只看精选" :model-value="route.query.featured === 'true'" @update:model-value="filter('featured', $event ? 'true' : '')" />
    </div></details>
  </PublicFilterBar>
  <div class="works-archive__results" :aria-busy="loading">
    <p v-if="loading" class="works-state" role="status">正在更新作品…</p>
    <p v-else-if="error" class="works-state" role="alert">{{ error }}</p>
    <p v-else-if="!result.items.length" class="works-state">暂无符合条件的公开作品。</p>
    <div v-if="result.items.length" class="works-grid"><WorkCard v-for="work in result.items" :key="work.id" :work="work" /></div>
  </div>
  <PublicPagination :page="page" :page-size="pageSize" :total="result.total" :loading="loading || featuredLoading" label="作品分页" unit="件" @change="changePage" @page-size="changeSize" />
  </section>
 </section>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.works-library。
 */
.works-library{width:min(1180px,100%);margin:auto;padding:40px 0 80px}
/* Leave a viewport of result space even for an empty filter; avoid scroll clamping on refresh. */
.works-archive__results{min-height:calc(100dvh - 160px);overflow-anchor:none}
  /**
   * 样式范围：.works-library__hero。
   * 使用网格布局组织内容；列定义为 1.4fr 1fr，间距由 50px 控制。
   */
  .works-library__hero{display:grid;grid-template-columns:1.4fr 1fr;gap:50px;align-items:end;padding:20px 0 0}
  /**
   * 样式范围：.works-library__hero h1。
   */
  .works-library__hero h1{margin:12px 0 0;font-size:clamp(42px,5vw,64px);line-height:1.2}
  /**
   * 样式范围：.works-library__hero>p,.works-archive__heading>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-library__hero>p,.works-archive__heading>p{color:var(--text-secondary);line-height:1.9;margin:0}
  /**
   * 样式范围：.works-featured。
   */
  .works-featured{margin-top:34px}
.works-featured__list{display:grid;gap:30px}
.works-secondary-filters summary{cursor:pointer;color:var(--primary);font-size:13px;padding:10px 0}
.works-secondary-filters>div{display:flex;flex-wrap:wrap;align-items:end;gap:16px;padding:12px 0}
.works-secondary-filters summary:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.works-featured__more。
   */
  .works-featured__more{margin-top:26px}
  /**
   * 样式范围：.works-featured__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-featured__empty{padding:30px;border:1px dashed var(--border);border-radius:18px;color:var(--text-muted);line-height:1.8}
  /**
   * 样式范围：.works-archive。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-archive{margin-top:72px;padding-top:42px;border-top:1px solid var(--border)}
  /**
   * 样式范围：.works-archive__heading。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .works-archive__heading{display:flex;justify-content:space-between;align-items:end;gap:24px}
  /**
   * 样式范围：.works-archive__heading h2。
   */
  .works-archive__heading h2{font-size:32px;margin:8px 0 0;line-height:1.3}
  /**
   * 样式范围：.works-archive__heading .public-eyebrow。
   */
  .works-archive__heading .public-eyebrow{font-size:11px}
  /**
   * 样式范围：.works-archive__heading>p。
   */
  .works-archive__heading>p{font-size:13px}
  /**
   * 样式范围：.works-categories。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .works-categories{display:flex;gap:10px;flex-wrap:wrap;padding:26px 0}
  /**
   * 样式范围：.works-categories button,.works-pages button,.works-state button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-categories button,.works-pages button,.works-state button{padding:10px 18px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}
  /**
   * 样式范围：.works-categories button.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-categories button.active{background:var(--primary);color:var(--on-primary);border-color:var(--primary)}
  /**
   * 样式范围：.works-filters。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-filters{display:flex;gap:20px;align-items:center;flex-wrap:wrap;margin-bottom:30px;font-size:13px;color:var(--text-secondary)}
  /**
   * 样式范围：.works-filters label。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .works-filters label{display:flex;gap:10px;align-items:center;white-space:nowrap}
  /**
   * 样式范围：.works-filters select。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-filters select{padding:8px 12px;max-width:180px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--text-primary)}
  /**
   * 样式范围：.works-filters input[type=checkbox]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-filters input[type=checkbox]{width:16px;height:16px;min-height:0;margin:0;accent-color:var(--primary)}
  /**
   * 样式范围：.works-filters>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-filters>span{margin-left:auto;color:var(--text-muted)}
  /**
   * 样式范围：.works-grid。
   * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 26px 控制。
   */
  .works-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:26px}
  /**
   * 样式范围：.works-state。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-state{padding:60px 20px;border:1px dashed var(--border);border-radius:18px;text-align:center;color:var(--text-muted)}
  /**
   * 样式范围：.works-state button。
   */
  .works-state button{margin-left:12px}
  /**
   * 样式范围：.works-pages。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .works-pages{display:flex;gap:20px;align-items:center;justify-content:center;margin-top:35px}
  /**
   * 样式范围：.works-pages button:disabled。
   */
  .works-pages button:disabled{opacity:.4;cursor:default}
/**
 * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:700px){
  /**
   * 样式范围：.works-library__hero,.works-grid。
   */
  .works-library__hero,.works-grid{grid-template-columns:1fr;gap:22px}
  /**
   * 样式范围：.works-archive。
   */
  .works-archive{margin-top:48px;padding-top:30px}
  /**
   * 样式范围：.works-archive__heading。
   */
  .works-archive__heading{display:block}
  /**
   * 样式范围：.works-archive__heading>p。
   */
  .works-archive__heading>p{margin-top:12px}
  /**
   * 样式范围：.works-filters。
   */
  .works-filters{gap:12px}
  /**
   * 样式范围：.works-filters>span。
   */
  .works-filters>span{margin-left:0}}
</style>
