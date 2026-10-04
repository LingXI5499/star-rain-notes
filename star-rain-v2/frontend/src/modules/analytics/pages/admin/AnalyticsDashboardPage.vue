<script setup>
import { onMounted, ref } from 'vue'
import { errorMessage } from '../../../../shared/http'
import { getTrafficSummary, getTrafficTrend, getHotContent, getReferrers } from '../../api/analyticsApi'

const today = new Date().toISOString().slice(0, 10)
const prior = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10)
const startDate = ref(prior)
const endDate = ref(today)
const summary = ref(null)
const trend = ref([])
const hot = ref([])
const referrers = ref([])
const loading = ref(false)
const error = ref('')

function openDate(event) { event.target.showPicker?.() }
function value(row, key) { return Number(row?.[key] || 0) }
function barWidth(row) {
  const max = Math.max(1, ...trend.value.map((item) => value(item, 'pageViews') + value(item, 'contentViews')))
  return `${Math.max(2, (value(row, 'pageViews') + value(row, 'contentViews')) / max * 100)}%`
}
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
.analytics-page{padding:clamp(1.25rem,3vw,3rem);max-width:1300px;margin:auto}.eyebrow{color:#bb6847;letter-spacing:.12em;font-size:.75rem;font-weight:700}.analytics-page h1{margin:.3rem 0}.filters{display:flex;gap:1rem;align-items:end;flex-wrap:wrap;margin:2rem 0}.filters label{display:grid;gap:.4rem}.filters input,.filters button{padding:.7rem;border:1px solid #ccd2ce;border-radius:.65rem;background:var(--surface,#fff);color:inherit}.filters button{cursor:pointer;background:#103f35;color:#fff}.error{color:#a43d2e}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:1rem;margin-bottom:2rem}.cards article,.panels section{border:1px solid #dce1dc;border-radius:1rem;padding:1.2rem;background:var(--surface,#fff)}.cards strong{display:block;font-size:2rem}.cards span{color:#68736e}.panels{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:1rem}.panels h2{margin-top:0}.trend-row{display:grid;grid-template-columns:6rem 1fr 5rem;gap:.5rem;align-items:center;margin:.5rem 0;font-size:.85rem}.bar-track{height:.7rem;background:#e5ebe6;border-radius:99px}.bar-fill{height:100%;background:#3e8173;border-radius:99px}ol{padding:0;list-style:none}li{display:flex;justify-content:space-between;border-bottom:1px solid #e5e8e5;padding:.65rem 0}small{color:#6c7670}
</style>
