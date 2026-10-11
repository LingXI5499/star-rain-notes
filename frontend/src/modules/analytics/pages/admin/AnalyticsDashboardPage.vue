<script setup>
/**
 * AnalyticsDashboardPage.vue：访问统计模块的页面组件。
 * 职责范围：前台访问事件、按日汇总和统计查询。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、../../../../shared/http、../../api/analyticsApi。
 * 主要响应式状态：startDate、endDate、summary、trend、hot、referrers、loading、error。
 * 阅读入口：openDate（打开日期）；value（值的处理函数）；barWidth（bar宽度的处理函数）；load（加载当前操作）。
 */


import { onMounted, ref } from 'vue'
import { errorMessage } from '../../../../shared/http'
import { getTrafficSummary, getTrafficTrend, getHotContent, getReferrers } from '../../api/analyticsApi'

const today = new Date().toISOString().slice(0, 10)
const prior = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10)
/**
 * 开始日期；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const startDate = ref(prior)
/**
 * 结束日期；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const endDate = ref(today)
/**
 * 摘要；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const summary = ref(null)
/**
 * trend；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const trend = ref([])
/**
 * hot；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const hot = ref([])
/**
 * referrers；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const referrers = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

/**
 * 打开日期。
 * 主要协作：event.target.showPicker。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function openDate(event) { event.target.showPicker?.() }
/**
 * 值的处理函数。
 * @param row 行；默认值、解构字段与可选参数见函数签名。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function value(row, key) { return Number(row?.[key] || 0) }
/**
 * bar宽度的处理函数。
 * 主要协作：trend.value.map、value。
 * @param row 行；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function barWidth(row) {
  const max = Math.max(1, ...trend.value.map((item) => value(item, 'pageViews') + value(item, 'contentViews')))
  return `${Math.max(2, (value(row, 'pageViews') + value(row, 'contentViews')) / max * 100)}%`
}
/**
 * 加载当前操作。
 * 主要协作：Promise.all、getTrafficSummary、getTrafficTrend、getHotContent、getReferrers。
 * 更新的响应式状态包括 loading、error、summary、trend、hot、referrers；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const range = { startDate: startDate.value, endDate: endDate.value }
    const [nextSummary, nextTrend, nextHot, nextReferrers] = await Promise.all([
      getTrafficSummary(), getTrafficTrend(range), getHotContent({ ...range, limit: 10 }), getReferrers(range),
    ])
    summary.value = nextSummary
    trend.value = nextTrend || []
    hot.value = nextHot || []
    referrers.value = nextReferrers || []
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <main class="analytics-page">
    <p class="eyebrow">SITE ANALYTICS · 访问统计</p>
    <h1>访问统计</h1>
    <p>按 UTC 日期统计公开页面和内容访问。最近数据会直接读取当日事件。</p>
    <form class="filters" @submit.prevent="load">
      <label>开始日期 <input v-model="startDate" type="date" @click="openDate" /></label>
      <label>结束日期 <input v-model="endDate" type="date" @click="openDate" /></label>
      <button type="submit" :disabled="loading">{{ loading ? '查询中…' : '查询' }}</button>
    </form>
    <p v-if="error" role="alert" class="error">{{ error }}</p>
    <div v-if="summary" class="cards">
      <article><strong>{{ summary.totalPageViews }}</strong><span>页面访问</span></article>
      <article><strong>{{ summary.totalContentViews }}</strong><span>内容访问</span></article>
      <article><strong>{{ summary.todayPageViews }}</strong><span>今日页面访问</span></article>
      <article><strong>{{ summary.todayContentViews }}</strong><span>今日内容访问</span></article>
    </div>
    <div class="panels">
      <section>
        <h2>访问趋势</h2>
        <p v-if="!trend.length">所选时段暂无访问。</p>
        <div v-for="day in trend" :key="day.statDate" class="trend-row">
          <time>{{ String(day.statDate).slice(0, 10) }}</time>
          <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(day) }"></div></div>
          <span>{{ value(day, 'pageViews') }} / {{ value(day, 'contentViews') }}</span>
        </div>
        <small>数字依次为页面访问 / 内容访问</small>
      </section>
      <section>
        <h2>内容热度</h2>
        <p v-if="!hot.length">所选时段暂无内容访问。</p>
        <ol v-else><li v-for="item in hot" :key="`${item.contentType}-${item.contentId}`">
          <span>{{ item.contentType }} #{{ item.contentId }}</span><strong>{{ item.viewCount }}</strong>
        </li></ol>
      </section>
      <section>
        <h2>访问来源</h2>
        <p v-if="!referrers.length">所选时段暂无来源数据。</p>
        <ol v-else><li v-for="item in referrers" :key="item.category">
          <span>{{ item.category }}</span><strong>{{ item.viewCount }}</strong>
        </li></ol>
      </section>
    </div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.analytics-page。
 */
.analytics-page{padding:clamp(1.25rem,3vw,3rem);max-width:1300px;margin:auto}
  /**
   * 样式范围：.eyebrow。
   */
  .eyebrow{color:#bb6847;letter-spacing:.12em;font-size:.75rem;font-weight:700}
  /**
   * 样式范围：.analytics-page h1。
   */
  .analytics-page h1{margin:.3rem 0}
  /**
   * 样式范围：.filters。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .filters{display:flex;gap:1rem;align-items:end;flex-wrap:wrap;margin:2rem 0}
  /**
   * 样式范围：.filters label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 .4rem 控制。
   */
  .filters label{display:grid;gap:.4rem}
  /**
   * 样式范围：.filters input,.filters button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .filters input,.filters button{padding:.7rem;border:1px solid #ccd2ce;border-radius:.65rem;background:var(--surface,#fff);color:inherit}
  /**
   * 样式范围：.filters button。
   */
  .filters button{cursor:pointer;background:#103f35;color:#fff}
  /**
   * 样式范围：.error。
   */
  .error{color:#a43d2e}
  /**
   * 样式范围：.cards。
   * 使用网格布局组织内容；列定义为 repeat(auto-fit,minmax(160px,1fr))，间距由 1rem 控制。
   */
  .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:1rem;margin-bottom:2rem}
  /**
   * 样式范围：.cards article,.panels section。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .cards article,.panels section{border:1px solid #dce1dc;border-radius:1rem;padding:1.2rem;background:var(--surface,#fff)}
  /**
   * 样式范围：.cards strong。
   */
  .cards strong{display:block;font-size:2rem}
  /**
   * 样式范围：.cards span。
   */
  .cards span{color:#68736e}
  /**
   * 样式范围：.panels。
   * 使用网格布局组织内容；列定义为 repeat(auto-fit,minmax(300px,1fr))，间距由 1rem 控制。
   */
  .panels{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:1rem}
  /**
   * 样式范围：.panels h2。
   */
  .panels h2{margin-top:0}
  /**
   * 样式范围：.trend-row。
   * 使用网格布局组织内容；列定义为 6rem 1fr 5rem，间距由 .5rem 控制。
   */
  .trend-row{display:grid;grid-template-columns:6rem 1fr 5rem;gap:.5rem;align-items:center;margin:.5rem 0;font-size:.85rem}
  /**
   * 样式范围：.bar-track。
   */
  .bar-track{height:.7rem;background:#e5ebe6;border-radius:99px}
  /**
   * 样式范围：.bar-fill。
   */
  .bar-fill{height:100%;background:#3e8173;border-radius:99px}
  /**
   * 样式范围：ol。
   */
  ol{padding:0;list-style:none}
  /**
   * 样式范围：li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  li{display:flex;justify-content:space-between;border-bottom:1px solid #e5e8e5;padding:.65rem 0}
  /**
   * 样式范围：small。
   */
  small{color:#6c7670}
</style>
