<script setup>
/**
 * TutorialCatalogPage.vue：教程与学习模块的页面组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/ui/PublicPagination.vue、../../../../shared/composables/publicListState、vue、vue-router。
 * 主要响应式状态：route、router、categories、allTutorials、selectedSlug、page、pageSize。
 * 阅读入口：tutorialPath（教程路径的处理函数）；changePage（change分页的处理函数）；changeSize（change大小的处理函数）。
 */


import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../../shared/composables/publicListState'

import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listPublicCategories, listPublicTutorials } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * categories；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categories = ref([])
/**
 * 全部Tutorials；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const allTutorials = ref([])
/**
 * 选中固定链接标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selectedSlug = ref(String(route.query.categorySlug || ''))
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
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

/**
 * 教程路径的处理函数。
 * 主要协作：accountPath。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const tutorialPath = (slug) => accountPath(`/tutorials/${slug}`)

/**
 * 选中分类；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedCategory = computed(() => categories.value.find((item) => item.slug === selectedSlug.value))
/**
 * catalog统计字段；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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
/**
 * 分类Tutorials；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const categoryTutorials = computed(() => selectedCategory.value
  ? allTutorials.value.filter((item) => item.categoryId === selectedCategory.value.id)
  : allTutorials.value)

/**
 * filtered；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const filtered = computed(() => {
  const keyword = String(route.query.q || '').trim().toLocaleLowerCase()
  return keyword
    ? categoryTutorials.value.filter((item) => `${item.title} ${item.summary || ''}`.toLocaleLowerCase().includes(keyword))
    : categoryTutorials.value
})

/**
 * displayed；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const displayed = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
/**
 * change分页的处理函数。
 * 主要协作：router.push。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 12) }) }

/**
 * 分类数量的处理函数。
 * 主要协作：allTutorials.value.filter。
 * @param category 分类；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function categoryCount(category) {
  if (typeof category.tutorialCount === 'number') return category.tutorialCount
  return allTutorials.value.filter((item) => item.categoryId === category.id).length
}

/**
 * 加载当前操作。
 * 主要协作：Promise.all、listPublicCategories、listPublicTutorials、allTutorials.value.push、errorMessage。
 * 更新的响应式状态包括 loading、error、categories、allTutorials、selectedSlug；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 查询分类的处理函数。
 * 主要协作：router.push。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 */
function selectCategory(slug) { router.push({ query: { ...route.query, categorySlug: slug || undefined, page: undefined } }) }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query, () => { selectedSlug.value = String(route.query.categorySlug || '') }, { immediate: true })
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch([filtered, pageSize], () => { if (!loading.value && page.value > Math.max(1, Math.ceil(filtered.value.length / pageSize.value))) changePage(1) })
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tutorial-catalog__stats。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__stats { margin-top: 20px; padding: 18px; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-surface); }
/**
 * 样式范围：.tutorial-catalog__stats h3。
 */
.tutorial-catalog__stats h3 { margin: 0 0 12px; font-size: 15px; }
/**
 * 样式范围：.tutorial-catalog__stats dl。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 10px 控制。
 */
.tutorial-catalog__stats dl { display: grid; gap: 10px; margin: 0; }
/**
 * 样式范围：.tutorial-catalog__stats dl div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__stats dl div { display: flex; justify-content: space-between; gap: 12px; color: var(--text-secondary); font-size: 13px; }
/**
 * 样式范围：.tutorial-catalog__stats dd。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 样式范围：.tutorial-catalog__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__eyebrow { margin-bottom: var(--space-3); color: var(--accent); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
/**
 * 样式范围：.tutorial-catalog__hero h1。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__hero h1 { max-width: 760px; margin-bottom: var(--space-3); font-size: clamp(32px,4vw,48px); line-height: 1.16; letter-spacing: -.03em; }
/**
 * 样式范围：.tutorial-catalog__hero p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__hero p:last-child { max-width: 720px; color: var(--text-secondary); font-size: 15px; line-height: 1.8; }
/**
 * 样式范围：.tutorial-catalog__summary。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__summary { min-width: 130px; padding: var(--space-4) var(--space-5); border: 1px solid var(--border); border-radius: 16px; background: var(--bg-surface); box-shadow: 0 10px 28px rgb(14 35 28/.05); text-align: center; }
/**
 * 样式范围：.tutorial-catalog__summary strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__summary strong { display: block; color: var(--primary); font-size: 28px; }
/**
 * 样式范围：.tutorial-catalog__summary span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__summary span { color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.tutorial-catalog__layout。
 * 使用网格布局组织内容；列定义为 260px minmax(0,1fr)，间距由 var(--space-8) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__layout { display: grid; grid-template-columns: 260px minmax(0,1fr); gap: var(--space-8); align-items: start; }
/**
 * 样式范围：.tutorial-catalog__sidebar。
 * 在正常文档流中保留位置，吸附偏移由 top=calc(var(--header-height) + var(--space-5)) 决定；父级高度限制吸附范围。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__sidebar { position: sticky; top: calc(var(--header-height) + var(--space-5)); max-height: calc(100vh - var(--header-height) - var(--space-10)); overflow: auto; border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); box-shadow: 0 12px 32px rgb(14 35 28/.055); }
/**
 * 样式范围：.tutorial-catalog__sidebar-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4); border-bottom: 1px solid var(--border); font-weight: 700; }
/**
 * 样式范围：.tutorial-catalog__sidebar-head small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__sidebar-head small { color: var(--text-muted); font-weight: 400; }
/**
 * 样式范围：.tutorial-catalog__sidebar nav。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__sidebar nav { padding: var(--space-3); }
/**
 * 样式范围：.tutorial-catalog__all。
 * 使用网格布局组织内容；列定义为 minmax(0,1fr) auto 12px，间距由 8px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all { width: 100%; min-height: 42px; display: grid; grid-template-columns: minmax(0,1fr) auto 12px; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid transparent; border-radius: 12px; color: var(--text-secondary); background: none; font-size: 14px; text-align: left; cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
/**
 * 样式范围：.tutorial-catalog__all:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all:hover { border-color: var(--border); background: var(--bg-subtle); color: var(--text-primary); transform: translateX(3px); }
/**
 * 样式范围：.tutorial-catalog__all--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all--active { border-color: color-mix(in srgb,var(--primary) 22%,var(--border)); color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); box-shadow: 0 6px 17px color-mix(in srgb,var(--primary) 10%,transparent); font-weight: 600; }
/**
 * 样式范围：.tutorial-catalog__all-count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all-count { min-width: 28px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 11px; text-align: center; }
/**
 * 样式范围：.tutorial-catalog__all-arrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all-arrow { color: var(--primary); font-size: 17px; }
/**
 * 样式范围：.tutorial-catalog__all:focus-visible,.tutorial-card:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__all:focus-visible,.tutorial-card:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 28%,transparent); outline-offset: 3px; }
/**
 * 样式范围：.tutorial-catalog__category-tree。
 */
.tutorial-catalog__category-tree { margin: 3px 0 0; padding: 0; list-style: none; }
/**
 * 样式范围：.tutorial-catalog__category-item。
 */
.tutorial-catalog__category-item { list-style: none; }
/**
 * 样式范围：.tutorial-catalog__category。
 * 使用网格布局组织内容；列定义为 minmax(0,1fr) auto 12px，间距由 8px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category { width: 100%; min-height: 38px; display: grid; grid-template-columns: minmax(0,1fr) auto 12px; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid transparent; border-radius: 12px; color: var(--text-secondary); background: none; font-size: 14px; line-height: 1.45; text-align: left; cursor: pointer; transition: transform 170ms ease,background-color 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
/**
 * 样式范围：.tutorial-catalog__category:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category:hover { color: var(--text-primary); background: var(--bg-subtle); border-color: var(--border); transform: translateX(3px); }
/**
 * 样式范围：.tutorial-catalog__category--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category--active { color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); border-color: color-mix(in srgb,var(--primary) 22%,var(--border)); box-shadow: 0 6px 17px color-mix(in srgb,var(--primary) 10%,transparent); font-weight: 600; }
/**
 * 样式范围：.tutorial-catalog__category:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category:focus-visible { outline: 3px solid color-mix(in srgb,var(--primary) 28%,transparent); outline-offset: 3px; }
/**
 * 样式范围：.tutorial-catalog__category-name。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.tutorial-catalog__category-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/**
 * 样式范围：.tutorial-catalog__category-count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category-count { flex-shrink: 0; min-width: 27px; padding: 3px 7px; border-radius: 999px; color: var(--text-muted); background: var(--bg-page); font-size: 11px; text-align: center; }
/**
 * 样式范围：.tutorial-catalog__category-arrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__category-arrow { color: var(--primary); font-size: 17px; }
/**
 * 样式范围：.tutorial-catalog__content。
 */
.tutorial-catalog__content { min-width: 0; }
/**
 * 样式范围：.tutorial-catalog__content-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__content-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-5); margin-bottom: var(--space-6); }
/**
 * 样式范围：.tutorial-catalog__path。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__path { margin-bottom: var(--space-2); color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.tutorial-catalog__content-head h2。
 */
.tutorial-catalog__content-head h2 { margin-bottom: 4px; font-size: 26px; line-height: 1.3; }
/**
 * 样式范围：.tutorial-catalog__content-head > div > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__content-head > div > p:last-child { color: var(--text-muted); font-size: 13px; }
/**
 * 样式范围：.tutorial-catalog__search。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__search { width: min(280px,38vw); height: 42px; display: flex; align-items: center; gap: var(--space-2); padding: 0 var(--space-3); border: 1px solid var(--border-strong); border-radius: 12px; color: var(--text-muted); background: var(--bg-surface); transition: border-color 170ms ease,box-shadow 170ms ease; }
/**
 * 样式范围：.tutorial-catalog__search:focus-within。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__search:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px color-mix(in srgb,var(--primary) 12%,transparent); }
/**
 * 样式范围：.tutorial-catalog__search input。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__search input { min-width: 0; flex: 1; border: 0; outline: 0; color: var(--text-primary); background: none; }
/**
 * 样式范围：.tutorial-catalog__grid。
 * 使用网格布局组织内容；列定义为 repeat(auto-fill,minmax(min(280px,100%),1fr))，间距由 var(--space-5) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(min(280px,100%),1fr)); gap: var(--space-5); }
/**
 * 样式范围：.tutorial-card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card { min-width: 0; overflow: hidden; display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 16px; color: var(--text-primary); background: var(--bg-surface); transition: transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease; }
/**
 * 样式范围：.tutorial-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card:hover { transform: translateY(-3px); border-color: color-mix(in srgb,var(--primary) 55%,var(--border)); box-shadow: 0 14px 35px rgb(0 0 0/.09); }
/**
 * 样式范围：.tutorial-card::before。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card::before { content: ''; display: block; height: 3px; background: linear-gradient(90deg, color-mix(in srgb,var(--primary) 55%,transparent), color-mix(in srgb,var(--accent) 35%,transparent) 65%, transparent); opacity: .55; }
/**
 * 样式范围：.tutorial-card:hover::before。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.tutorial-card:hover::before { opacity: 1; }
/**
 * 样式范围：.tutorial-card__body。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__body { flex: 1; display: flex; flex-direction: column; padding: var(--space-5); }
/**
 * 样式范围：.tutorial-card__category。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__category { margin-bottom: var(--space-2); color: var(--accent); font-size: 12px; }
/**
 * 样式范围：.tutorial-card h3。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card h3 { margin-bottom: var(--space-3); font-size: 19px; line-height: 1.45; }
/**
 * 样式范围：.tutorial-card__summary。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__summary { display: -webkit-box; overflow: hidden; margin-bottom: var(--space-5); color: var(--text-secondary); font-size: 14px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
/**
 * 样式范围：.tutorial-card__footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-top: auto; padding-top: var(--space-4); border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.tutorial-card__chapter-count。
 */
.tutorial-card__chapter-count { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
/**
 * 样式范围：.tutorial-card__cta。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__cta { display: inline-flex; align-items: center; gap: 8px; padding: 8px 11px; border: 1px solid color-mix(in srgb,var(--primary) 22%,transparent); border-radius: 999px; color: var(--primary); background: color-mix(in srgb,var(--primary) 9%,transparent); font-size: 12px; font-weight: 700; white-space: nowrap; transition: color 170ms ease,background-color 170ms ease,transform 170ms ease,box-shadow 170ms ease; }
/**
 * 样式范围：.tutorial-card__cta span。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card__cta span { display: grid; width: 20px; height: 20px; place-items: center; border-radius: 50%; color: var(--on-primary); background: var(--primary); transition: transform 170ms ease; }
/**
 * 样式范围：.tutorial-card:hover .tutorial-card__cta。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card:hover .tutorial-card__cta { color: var(--on-primary); background: var(--primary); box-shadow: 0 8px 20px color-mix(in srgb,var(--primary) 22%,transparent); }
/**
 * 样式范围：.tutorial-card:hover .tutorial-card__cta span。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-card:hover .tutorial-card__cta span { color: var(--primary); background: var(--on-primary); transform: translateX(2px); }
/**
 * 样式范围：.tutorial-catalog__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tutorial-catalog__empty { padding: var(--space-10); border: 1px dashed var(--border-strong); border-radius: var(--radius-md); color: var(--text-muted); text-align: center; }
/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.tutorial-catalog__all,.tutorial-catalog__category,.tutorial-catalog__search,.tutorial-card,.tutorial-card__cta,.tutorial-card__cta span。
   */
  .tutorial-catalog__all,.tutorial-catalog__category,.tutorial-catalog__search,.tutorial-card,.tutorial-card__cta,.tutorial-card__cta span { transition: none; } }
/**
 * 响应式或设备条件：(max-width: 900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 900px) {
  /**
   * 样式范围：.tutorial-catalog__hero。
   */
  .tutorial-catalog__hero { align-items: flex-start; }
  /**
   * 样式范围：.tutorial-catalog__summary。
   */
  .tutorial-catalog__summary { display: none; }
  /**
   * 样式范围：.tutorial-catalog__layout。
   */
  .tutorial-catalog__layout { grid-template-columns: 1fr; }
  /**
   * 样式范围：.tutorial-catalog__sidebar。
   */
  .tutorial-catalog__sidebar { position: static; max-height: none; } }
/**
 * 响应式或设备条件：(max-width: 620px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 620px) {
  /**
   * 样式范围：.tutorial-catalog__content-head。
   */
  .tutorial-catalog__content-head { align-items: stretch; flex-direction: column; }
  /**
   * 样式范围：.tutorial-catalog__search。
   */
  .tutorial-catalog__search { width: 100%; }
  /**
   * 样式范围：.tutorial-catalog__grid。
   */
  .tutorial-catalog__grid { grid-template-columns: 1fr; } }
</style>
