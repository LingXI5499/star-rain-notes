<script setup>
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import PublicFilterBar from '../../../../shared/ui/PublicFilterBar.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listPublicCategories, listPublicTutorials } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { VIEW_MODE, accountPath, resolveViewMode } from '../../../../shared/viewMode'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const allTutorials = ref([])
const selectedSlug = ref(String(route.query.categorySlug || ''))
const search = ref(String(route.query.q || ''))
const page = computed(() => publicPage(route.query.page))
const pageSize = computed(() => publicPageSize(route.query.pageSize))
const loading = ref(true)
const error = ref('')

const tutorialPath = (slug) => resolveViewMode(route.path) === VIEW_MODE.ACCOUNT
  ? accountPath(`/tutorials/${slug}`) : `/tutorials/${slug}`

const selectedCategory = computed(() => categories.value.find((item) => item.slug === selectedSlug.value))
const catalogStats = computed(() => ({
  tutorials: categoryTutorials.value.length,
  chapters: categoryTutorials.value.reduce((sum, item) => sum + Number(item.chapterCount || 0), 0),
  words: categoryTutorials.value.reduce((sum, item) => sum + Number(item.wordCount || 0), 0),
}))

/*
 * 与 V1 TutorialsView 一致地分成两层：
 *   categoryTutorials —— 只按左侧分类过滤，用于顶部数字与「共 N 门可学习教程」；
 *   displayed         —— 再叠加搜索关键字，只决定网格里显示哪些卡片。
 * 混成一层会让「输入关键字」时顶部总数也跟着变小，与 V1 的观感不同。
 */
const categoryTutorials = computed(() => selectedCategory.value
  ? allTutorials.value.filter((item) => item.categoryId === selectedCategory.value.id)
  : allTutorials.value)

const filtered = computed(() => {
  const keyword = String(route.query.q || '').trim().toLocaleLowerCase()
  return keyword
    ? categoryTutorials.value.filter((item) => `${item.title} ${item.summary || ''}`.toLocaleLowerCase().includes(keyword))
    : categoryTutorials.value
})

const displayed = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
function changePage(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 12) }) }
function searchTutorials(value) { router.push({ query: { ...route.query, q: value || undefined, page: undefined } }) }
function resetFilters() { router.push({ query: { pageSize: route.query.pageSize } }) }

function categoryCount(category) {
  if (typeof category.tutorialCount === 'number') return category.tutorialCount
  return allTutorials.value.filter((item) => item.categoryId === category.id).length
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryRows, first] = await Promise.all([
      listPublicCategories(), listPublicTutorials({ page: 1, pageSize: 100 }),
    ])
    categories.value = categoryRows
    allTutorials.value = [...(first.items || [])]
    const pages = Math.ceil((first.total || 0) / 100)
    for (let page = 2; page <= pages; page += 1) {
      const next = await listPublicTutorials({ page, pageSize: 100 })
      allTutorials.value.push(...(next.items || []))
    }
    selectedSlug.value = typeof route.query.categorySlug === 'string' ? route.query.categorySlug : ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function selectCategory(slug) { router.push({ query: { ...route.query, categorySlug: slug || undefined, page: undefined } }) }
watch(() => route.query, () => { selectedSlug.value = String(route.query.categorySlug || ''); search.value = String(route.query.q || '') }, { immediate: true })
watch([filtered, pageSize], () => { if (!loading.value && page.value > Math.max(1, Math.ceil(filtered.value.length / pageSize.value))) changePage(1) })
onMounted(load)
</script>

<template>
  <section class="page-container tutorial-catalog">
    <header class="tutorial-catalog__hero">
      <div>
        <p class="tutorial-catalog__eyebrow">LEARNING PATHS · 教程中心</p>
        <h1>选择一门教程，开始系统学习</h1>
        <p>左侧按知识体系浏览，右侧从教程卡片进入；课程内提供完整目录、上下篇与页内导航。</p>
      </div>
      <div class="tutorial-catalog__summary">
        <strong>{{ categoryTutorials.length }}</strong>
        <span>{{ selectedCategory ? '当前分类教程' : '公开教程' }}</span>
      </div>
    </header>

    <div class="tutorial-catalog__layout">
      <aside class="tutorial-catalog__sidebar">
        <div class="tutorial-catalog__sidebar-head">
          <span>教程目录</span>
          <small>{{ categories.length }} 类</small>
        </div>
        <nav aria-label="教程分类">
          <button
            type="button"
            class="tutorial-catalog__all"
            :class="{ 'tutorial-catalog__all--active': !selectedSlug }"
            @click="selectCategory('')"
          >
            <span>全部教程</span>
            <span class="tutorial-catalog__all-count">{{ allTutorials.length }}</span>
            <span class="tutorial-catalog__all-arrow" aria-hidden="true">›</span>
          </button>
          <ul class="tutorial-catalog__category-tree">
            <li v-for="category in categories" :key="category.id" class="tutorial-catalog__category-item">
              <button
                type="button"
                class="tutorial-catalog__category"
                :class="{ 'tutorial-catalog__category--active': selectedSlug === category.slug }"
                @click="selectCategory(category.slug)"
              >
                <span class="tutorial-catalog__category-name">{{ category.name }}</span>
                <span class="tutorial-catalog__category-count">{{ categoryCount(category) }}</span>
                <span class="tutorial-catalog__category-arrow" aria-hidden="true">›</span>
              </button>
            </li>
          </ul>
        </nav>
        <section class="tutorial-catalog__stats" aria-label="教程统计">
          <h3>教程统计</h3>
          <dl>
            <div><dt>教程</dt><dd>{{ catalogStats.tutorials }} 门</dd></div>
            <div><dt>公开章节</dt><dd>{{ catalogStats.chapters }} 章</dd></div>
            <div><dt>正文总字数</dt><dd>{{ catalogStats.words.toLocaleString() }} 字</dd></div>
          </dl>
        </section>
      </aside>

      <main class="tutorial-catalog__content">
        <div class="tutorial-catalog__content-head">
          <div>
            <p class="tutorial-catalog__path">教程 / {{ selectedCategory?.name || '全部教程' }}</p>
            <h2>{{ selectedCategory?.name || '全部教程' }}</h2>
            <p>共 {{ categoryTutorials.length }} 门可学习教程</p>
          </div>

        </div>

        <PublicFilterBar label="教程筛选" :total="filtered.length" unit="门教程" :active="Boolean(selectedSlug || route.query.q)" @reset="resetFilters"><PublicSearch v-model="search" label="筛选当前教程" placeholder="搜索教程标题或简介" @search="searchTutorials" /></PublicFilterBar>
        <p v-if="loading" class="tutorial-catalog__empty">正在加载教程…</p>
        <p v-else-if="error" class="tutorial-catalog__empty" role="alert">{{ error }}</p>
        <p v-else-if="!displayed.length" class="tutorial-catalog__empty">当前分类暂无匹配教程。</p>
        <div v-else class="tutorial-catalog__grid" data-stagger>
          <RouterLink
            v-for="tutorial in displayed"
            :key="tutorial.id"
            :to="tutorialPath(tutorial.slug)"
            class="tutorial-card"
          >
            <div class="tutorial-card__body">
              <p class="tutorial-card__category">{{ tutorial.categoryName }}</p>
              <h3>{{ tutorial.title }}</h3>
              <p class="tutorial-card__summary">{{ tutorial.summary }}</p>
              <div class="tutorial-card__footer">
                <span class="tutorial-card__chapter-count">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z"/><path d="M4 5.5v15M8 7h8M8 11h6"/></svg>
                  {{ tutorial.chapterCount }} 个章节
                </span>
                <strong class="tutorial-card__cta">
                  进入课程
                  <span aria-hidden="true">→</span>
                </strong>
              </div>
            </div>
          </RouterLink>
        </div>
        <PublicPagination :page="page" :page-size="pageSize" :total="filtered.length" :loading="loading" label="教程分页" unit="门" @change="changePage" @page-size="changeSize" />
      </main>
    </div>
  </section>
</template>

<style scoped>
.tutorial-catalog__stats { margin-top: 20px; padding: 18px; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-surface); }
.tutorial-catalog__stats h3 { margin: 0 0 12px; font-size: 15px; }
.tutorial-catalog__stats dl { display: grid; gap: 10px; margin: 0; }
.tutorial-catalog__stats dl div { display: flex; justify-content: space-between; gap: 12px; color: var(--text-secondary); font-size: 13px; }
.tutorial-catalog__stats dd { margin: 0; color: var(--primary); font-weight: 700; }
/* 以下样式逐条照抄 V1 `frontend/src/views/tutorials/TutorialsView.vue`，
   只把侧栏的树组件展开成平铺按钮（V2 的公开分类接口本来就是平铺的）。 */
.tutorial-catalog__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-8);
  margin-bottom: var(--space-8);
  padding: var(--space-7) 0;
  border-bottom: 1px solid var(--border);
}
.tutorial-catalog__eyebrow { margin-bottom: var(--space-3); color: var(--accent); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
.tutorial-catalog__hero h1 { max-width: 760px; margin-bottom: var(--space-3); font-size: clamp(32px,4vw,48px); line-height: 1.16; letter-spacing: -.03em; }
.tutorial-catalog__hero p:last-child { max-width: 720px; color: var(--text-secondary); font-size: 15px; line-height: 1.8; }
.tutorial-catalog__summary { min-width: 130px; padding: var(--space-4) var(--space-5); border: 1px solid var(--border); border-radius: 16px; background: var(--bg-surface); box-shadow: 0 10px 28px rgb(14 35 28/.05); text-align: center; }
.tutorial-catalog__summary strong { display: block; color: var(--primary); font-size: 28px; }
.tutorial-catalog__summary span { color: var(--text-muted); font-size: 12px; }
.tutorial-catalog__layout { display: grid; grid-template-columns: 260px minmax(0,1fr); gap: var(--space-8); align-items: start; }
.tutorial-catalog__sidebar { position: sticky; top: calc(var(--header-height) + var(--space-5)); max-height: calc(100vh - var(--header-height) - var(--space-10)); overflow: auto; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); box-shadow: 0 12px 32px rgb(14 35 28/.055); }
.tutorial-catalog__sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4); border-bottom: 1px solid var(--border); font-weight: 700; }
.tutorial-catalog__sidebar-head small { color: var(--text-muted); font-weight: 400; }
.tutorial-catalog__sidebar nav { padding: var(--space-3); }
.tutorial-catalog__all { width: 100%; min-height: 42px; display: grid; grid-template-columns: minmax(0,1fr) auto 12px; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid transparent; border-radius: 12px; color: var(--text-secondary); background: none; font-size: 14px; text-align: left; cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.tutorial-catalog__all:hover { border-color: var(--border); background: var(--bg-subtle); color: var(--text-primary); transform: translateX(3px); }
.tutorial-catalog__all--active { border-color: color-mix(in srgb,var(--primary) 22%,var(--border)); color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); box-shadow: 0 6px 17px color-mix(in srgb,var(--primary) 10%,transparent); font-weight: 600; }
.tutorial-catalog__all-count { min-width: 28px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 11px; text-align: center; }
.tutorial-catalog__all-arrow { color: var(--primary); font-size: 17px; }
.tutorial-catalog__all:focus-visible,.tutorial-card:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 28%,transparent); outline-offset: 3px; }
.tutorial-catalog__category-tree { margin: 3px 0 0; padding: 0; list-style: none; }
.tutorial-catalog__category-item { list-style: none; }
.tutorial-catalog__category { width: 100%; min-height: 38px; display: grid; grid-template-columns: minmax(0,1fr) auto 12px; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid transparent; border-radius: 12px; color: var(--text-secondary); background: none; font-size: 14px; line-height: 1.45; text-align: left; cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.tutorial-catalog__category:hover { color: var(--text-primary); background: var(--bg-subtle); border-color: var(--border); transform: translateX(3px); }
.tutorial-catalog__category--active { color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); border-color: color-mix(in srgb,var(--primary) 22%,var(--border)); box-shadow: 0 6px 17px color-mix(in srgb,var(--primary) 10%,transparent); font-weight: 600; }
.tutorial-catalog__category:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 28%,transparent); outline-offset: 3px; }
.tutorial-catalog__category-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tutorial-catalog__category-count { flex-shrink: 0; min-width: 27px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 11px; text-align: center; }
.tutorial-catalog__category-arrow { color: var(--primary); font-size: 17px; }
.tutorial-catalog__content { min-width: 0; }
.tutorial-catalog__content-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-5); margin-bottom: var(--space-6); }
.tutorial-catalog__path { margin-bottom: var(--space-2); color: var(--text-muted); font-size: 12px; }
.tutorial-catalog__content-head h2 { margin-bottom: 4px; font-size: 26px; line-height: 1.3; }
.tutorial-catalog__content-head > div > p:last-child { color: var(--text-muted); font-size: 13px; }
.tutorial-catalog__search { width: min(280px,38vw); height: 42px; display: flex; align-items: center; gap: var(--space-2); padding: 0 var(--space-3); border: 1px solid var(--border-strong); border-radius: 12px; color: var(--text-muted); background: var(--bg-surface); transition: border-color 170ms ease,box-shadow 170ms ease; }
.tutorial-catalog__search:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px color-mix(in srgb,var(--primary) 12%,transparent); }
.tutorial-catalog__search input { min-width: 0; flex: 1; border: 0; outline: 0; color: var(--text-primary); background: none; }
.tutorial-catalog__grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(min(280px,100%),1fr)); gap: var(--space-5); }
.tutorial-card { min-width: 0; overflow: hidden; display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 16px; color: var(--text-primary); background: var(--bg-surface); transition: transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
.tutorial-card:hover { transform: translateY(-3px); border-color: color-mix(in srgb,var(--primary) 55%,var(--border)); box-shadow: 0 14px 35px rgb(0 0 0/.09); }
.tutorial-card::before { content: ''; display: block; height: 3px; background: linear-gradient(90deg, color-mix(in srgb,var(--primary) 55%,transparent), color-mix(in srgb,var(--accent) 35%,transparent) 65%, transparent); opacity: .55; }
.tutorial-card:hover::before { opacity: 1; }
.tutorial-card__body { flex: 1; display: flex; flex-direction: column; padding: var(--space-5); }
.tutorial-card__category { margin-bottom: var(--space-2); color: var(--accent); font-size: 12px; }
.tutorial-card h3 { margin-bottom: var(--space-3); font-size: 19px; line-height: 1.45; }
.tutorial-card__summary { display: -webkit-box; overflow: hidden; margin-bottom: var(--space-5); color: var(--text-secondary); font-size: 14px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.tutorial-card__footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-top: auto; padding-top: var(--space-4); border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
.tutorial-card__chapter-count { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
.tutorial-card__cta { display: inline-flex; align-items: center; gap: 8px; padding: 8px 11px; border: 1px solid color-mix(in srgb,var(--primary) 22%,transparent); border-radius: 999px; color: var(--primary); background: color-mix(in srgb,var(--primary) 9%,transparent); font-size: 12px; font-weight: 700; white-space: nowrap; transition: color 170ms ease,background-color 170ms ease,transform 170ms ease,box-shadow 170ms ease; }
.tutorial-card__cta span { display: grid; width: 20px; height: 20px; place-items: center; border-radius: 50%; color: var(--on-primary); background: var(--primary); transition: transform 170ms ease; }
.tutorial-card:hover .tutorial-card__cta { color: var(--on-primary); background: var(--primary); box-shadow: 0 8px 20px color-mix(in srgb,var(--primary) 22%,transparent); }
.tutorial-card:hover .tutorial-card__cta span { color: var(--primary); background: var(--on-primary); transform: translateX(2px); }
.tutorial-catalog__empty { padding: var(--space-10); border: 1px dashed var(--border-strong); border-radius: var(--radius-md); color: var(--text-muted); text-align: center; }
@media (prefers-reduced-motion: reduce) { .tutorial-catalog__all,.tutorial-catalog__category,.tutorial-catalog__search,.tutorial-card,.tutorial-card__cta,.tutorial-card__cta span { transition: none; } }
@media (max-width: 900px) { .tutorial-catalog__hero { align-items: flex-start; } .tutorial-catalog__summary { display: none; } .tutorial-catalog__layout { grid-template-columns: 1fr; } .tutorial-catalog__sidebar { position: static; max-height: none; } }
@media (max-width: 620px) { .tutorial-catalog__content-head { align-items: stretch; flex-direction: column; } .tutorial-catalog__search { width: 100%; } .tutorial-catalog__grid { grid-template-columns: 1fr; } }
</style>
