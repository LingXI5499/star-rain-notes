<script setup>
import { computed, onMounted, ref } from 'vue'
import { getTrafficSummary, getTrafficTrend, getHotContent, getReferrers } from '../../api/analyticsApi'
import ChartPanel from '../../../../shared/charts/ChartPanel.vue'
import TrafficTrendPanel from '../../../../shared/charts/TrafficTrendPanel.vue'
import { useStatisticsResource } from '../../../../shared/charts/useStatisticsResource'
import { count, utcToday, recentRange, rangeError, hotRows, sourceRows, rankingOption, sourceOption } from '../../../../shared/charts/statistics'
import '../../../../shared/charts/statistics.css'

const initial = recentRange(30)
const startDate = ref(initial.startDate), endDate = ref(initial.endDate), appliedRange = ref(initial)
const period = ref(30), validation = ref('')
const summary = useStatisticsResource(getTrafficSummary), trend = useStatisticsResource(getTrafficTrend)
const hot = useStatisticsResource(getHotContent), referrers = useStatisticsResource(getReferrers)
const loading = computed(() => [summary, trend, hot, referrers].some(resource => resource.loading.value))
const hotData = computed(() => hotRows(hot.data.value)), sourceData = computed(() => sourceRows(referrers.data.value))
const hotOption = computed(() => rankingOption(hotData.value, '内容访问'))
const referrerOption = computed(() => sourceOption(sourceData.value))
const rangeLabel = computed(() => `${appliedRange.value.startDate} 至 ${appliedRange.value.endDate} · UTC`)
const cards = computed(() => [
  { label: '累计页面访问', value: summary.data.value?.totalPageViews },
  { label: '累计内容访问', value: summary.data.value?.totalContentViews },
  { label: '今日页面访问', value: summary.data.value?.todayPageViews },
  { label: '今日内容访问', value: summary.data.value?.todayContentViews },
])
function loadHot() { return hot.load({ ...appliedRange.value, limit: 10 }) }
function loadReferrers() { return referrers.load({ ...appliedRange.value }) }
function loadTrend() { return trend.load({ ...appliedRange.value }) }
function load() {
  const range = { startDate: startDate.value, endDate: endDate.value }
  validation.value = rangeError(range)
  if (validation.value) return
  appliedRange.value = range
  void summary.load(); void loadTrend(); void loadHot(); void loadReferrers()
}
function choosePeriod(days) {
  const range = recentRange(days)
  period.value = days; startDate.value = range.startDate; endDate.value = range.endDate
  load()
}
onMounted(load)
</script>

<template>
  <main class="analytics-page">
    <header class="analytics-page__heading"><div><p class="analytics-page__eyebrow">站点数据</p><h1>访问统计</h1><p>了解访问变化、热门内容与访问来源。所有日期按 UTC 统计。</p></div><span class="analytics-page__timezone">UTC</span></header>
    <section class="analytics-summary" aria-label="访问概览" :aria-busy="summary.loading.value">
      <header><h2>访问概览</h2><p>累计及今日数据不随下方日期筛选变化；次数不代表独立访客。</p></header>
      <p v-if="summary.error.value" role="alert">{{summary.error.value}} <button type="button" @click="summary.load()">重试概览</button></p>
      <div class="analytics-cards"><article v-for="card in cards" :key="card.label"><span>{{card.label}}</span><strong :class="{'analytics-cards__placeholder':summary.loading.value}">{{ summary.loading.value ? '…' : card.value == null ? '—' : count(card.value).toLocaleString('zh-CN') }}</strong><small>{{card.label.startsWith('今日') ? 'UTC 今日' : '自开始记录以来'}}</small></article></div>
    </section>
    <form class="analytics-filters" @submit.prevent="load">
      <div class="statistics-periods" aria-label="统计时间范围"><button v-for="days in [7,30]" :key="days" type="button" :aria-pressed="period===days" :disabled="loading" @click="choosePeriod(days)">近 {{days}} 天</button></div>
      <label>开始日期<input v-model="startDate" type="date" required :max="utcToday()" @input="period=0" /></label>
      <label>结束日期<input v-model="endDate" type="date" required :max="utcToday()" @input="period=0" /></label>
      <button class="analytics-filters__submit" type="submit" :disabled="loading">{{loading ? '查询中…' : '查询'}}</button>
      <p v-if="validation" class="analytics-filters__error" role="alert">{{validation}}</p>
    </form>
    <TrafficTrendPanel :rows="trend.data.value || []" :range="appliedRange" :loading="trend.loading.value" :error="trend.error.value" @retry="loadTrend" />
    <div class="analytics-panels">
      <ChartPanel title="热门内容 TOP 10" :description="`${rangeLabel} · 按内容访问次数排名`" :option="hotOption" :loading="hot.loading.value" :error="hot.error.value" :empty="!hotData.some(row=>row.value>0)" empty-text="所选时段暂无内容访问。" @retry="loadHot">
        <details class="statistics-details"><summary>查看排名明细</summary><div class="statistics-details__scroll"><table><caption>内容类型及编号与访问次数</caption><thead><tr><th scope="col">内容</th><th scope="col">访问次数</th></tr></thead><tbody><tr v-for="row in hotData" :key="row.name"><th scope="row">{{row.name}}</th><td>{{row.value}}</td></tr></tbody></table></div></details>
      </ChartPanel>
      <ChartPanel title="访问来源" :description="`${rangeLabel} · 页面访问来源分布`" :option="referrerOption" :loading="referrers.loading.value" :error="referrers.error.value" :empty="!sourceData.some(row=>row.value>0)" empty-text="所选时段暂无来源数据。" @retry="loadReferrers">
        <details class="statistics-details"><summary>查看来源明细</summary><div class="statistics-details__scroll"><table><caption>页面访问来源与次数</caption><thead><tr><th scope="col">来源</th><th scope="col">访问次数</th></tr></thead><tbody><tr v-for="row in sourceData" :key="row.name"><th scope="row">{{row.name}}</th><td>{{row.value}}</td></tr></tbody></table></div></details>
      </ChartPanel>
    </div>
    <p class="analytics-page__note">页面访问与内容访问分别记录，不合并为总访客数。近期数据包含当日事件；无事件的日期在趋势中补零。</p>
  </main>
</template>

<style scoped>
.analytics-page{min-width:0;max-width:1440px;margin:auto;color:var(--text-primary)}
.analytics-page__heading{display:flex;align-items:center;justify-content:space-between;gap:24px;margin-bottom:32px}.analytics-page__eyebrow{color:var(--primary);font-size:12px;line-height:16px;margin:0 0 8px}
h1{font-size:30px;line-height:36px;margin:0 0 8px;font-weight:600}.analytics-page__heading p:last-child{color:var(--text-secondary);font-size:14px;line-height:20px;margin:0}.analytics-page__timezone{padding:8px 12px;background:var(--bg-subtle);border:1px solid var(--border);border-radius:8px;font-size:12px;line-height:16px}
.analytics-summary{margin-bottom:24px}.analytics-summary header{display:flex;align-items:baseline;flex-wrap:wrap;gap:8px 16px;margin-bottom:16px}.analytics-summary h2{font-size:18px;line-height:28px;margin:0;font-weight:600}.analytics-summary header p,.analytics-page__note{font-size:12px;line-height:16px;color:var(--text-secondary);margin:0}
.analytics-cards{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px}.analytics-cards article{display:flex;flex-direction:column;gap:12px;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);min-width:0}.analytics-cards span{font-size:14px;line-height:20px;color:var(--text-secondary)}.analytics-cards strong{font-size:30px;line-height:36px;font-weight:600;overflow-wrap:anywhere;font-variant-numeric:tabular-nums}.analytics-cards small{font-size:12px;line-height:16px;color:var(--text-muted)}.analytics-cards__placeholder{background:var(--bg-subtle);border-radius:4px}
.analytics-filters{display:flex;align-items:end;gap:16px;flex-wrap:wrap;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);margin-bottom:24px}.analytics-filters label{display:grid;gap:8px;color:var(--text-secondary);font-size:12px;line-height:16px}.analytics-filters input{box-sizing:border-box;width:100%;min-width:0;border:1px solid var(--border);border-radius:8px;padding:8px 12px;font:inherit;font-size:14px;line-height:20px;background:var(--bg-surface);color:var(--text-primary)}
.analytics-page button{font-family:inherit}.analytics-filters__submit,.analytics-summary button{padding:8px 12px;background:var(--primary);color:var(--text-inverse,#fff);border:1px solid var(--primary);border-radius:8px;font-size:14px;line-height:20px;font-weight:600;cursor:pointer}.analytics-filters__submit:hover,.analytics-summary button:hover{background:var(--primary-hover,var(--primary))}.analytics-page button:active{transform:scale(.98)}.analytics-page :is(button,input):focus-visible{outline:2px solid var(--primary);outline-offset:2px}.analytics-page button:disabled{opacity:.5;cursor:wait}
.analytics-filters__error{flex-basis:100%;margin:0;color:var(--danger,#ad493e);font-size:14px;line-height:20px}.analytics-panels{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:24px;margin:24px 0}.analytics-page__note{margin-top:24px}
@media(max-width:1000px){.analytics-panels{grid-template-columns:minmax(0,1fr)}.analytics-cards{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:640px){.analytics-page__heading{margin-bottom:24px}.analytics-cards article{padding:16px}.analytics-cards strong{font-size:24px;line-height:32px}.analytics-filters{padding:16px}.analytics-filters label{flex:1 1 100%}}
</style>
