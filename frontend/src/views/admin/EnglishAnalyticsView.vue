<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import {
  fetchEnglishAnalytics,
  type AdminLearningAnalytics,
  type EnglishAnalyticsType,
} from '@/api/englishAnalytics'

const route = useRoute()
const router = useRouter()
const analytics = ref<AdminLearningAnalytics>()
const loading = ref(false)
const errorMessage = ref('')
const days = ref<7 | 30 | 90>(30)
const contentType = ref<EnglishAnalyticsType>('ALL')

const ranges = [7, 30, 90] as const
const modules: Array<{ value: EnglishAnalyticsType; label: string; glyph: string }> = [
  { value: 'ALL', label: '全部模块', glyph: '全' },
  { value: 'GRAMMAR', label: '语法', glyph: '语' },
  { value: 'READING', label: '阅读', glyph: '读' },
  { value: 'LISTENING', label: '听力', glyph: '听' },
  { value: 'WRITING', label: '写作', glyph: '写' },
]

const maxAttempts = computed(() => Math.max(1, ...(analytics.value?.trend.map((item) => item.attempts) ?? [1])))
const hasActivity = computed(() => (analytics.value?.overview.totalAttempts ?? 0) > 0)

function syncRoute() {
  const value = Number(route.query.days)
  days.value = value === 7 || value === 90 ? value : 30
  const type = String(route.query.type ?? 'ALL').toUpperCase() as EnglishAnalyticsType
  contentType.value = modules.some((item) => item.value === type) ? type : 'ALL'
}

async function change(nextDays = days.value, nextType = contentType.value) {
  await router.push({
    query: {
      days: nextDays === 30 ? undefined : String(nextDays),
      type: nextType === 'ALL' ? undefined : nextType.toLowerCase(),
    },
  })
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    analytics.value = await fetchEnglishAnalytics(days.value, contentType.value)
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.detail || error?.message || '学习统计接口暂时不可用。'
    ElMessage.error('加载学习统计失败。')
  } finally {
    loading.value = false
  }
}

function moduleMeta(type: string) {
  return modules.find((item) => item.value === type) ?? { label: type, glyph: '英' }
}

function rate(value: number) {
  return `${Number(value.toFixed(1))}%`
}

function duration(seconds: number) {
  if (seconds < 60) return `${seconds} 秒`
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours ? `${hours} 小时 ${minutes} 分` : `${minutes} 分钟`
}

function dateLabel(date: string, index: number) {
  const total = analytics.value?.trend.length ?? 0
  const interval = days.value === 7 ? 1 : days.value === 30 ? 5 : 15
  return index === 0 || index === total - 1 || index % interval === 0 ? date.slice(5).replace('-', '/') : ''
}

onMounted(() => {
  syncRoute()
  void load()
})
watch(() => route.query, () => {
  syncRoute()
  void load()
})
</script>

<template>
  <section class="learning-stats" v-loading="loading">
    <header class="learning-stats__header">
      <div>
        <p>LEARNING STATS · 简洁学习统计</p>
        <h1>英语学习统计</h1>
        <span>只记录真实学习数量和内容触达情况，帮助你自主判断下一步需要补充什么。</span>
      </div>
      <div class="learning-stats__filters" aria-label="统计筛选">
        <div class="range-switch" aria-label="时间范围">
          <button v-for="value in ranges" :key="value" type="button" :class="{ active: days === value }" @click="change(value, contentType)">近 {{ value }} 天</button>
        </div>
        <el-select v-model="contentType" aria-label="英语模块" @change="change(days, contentType)">
          <el-option v-for="item in modules" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </header>

    <div v-if="errorMessage" class="error-state" role="alert">
      <div><strong>暂时无法加载统计</strong><span>{{ errorMessage }}</span></div>
      <button type="button" @click="load">重新加载</button>
    </div>

    <template v-if="analytics && !errorMessage">
      <section class="summary-grid" aria-label="学习摘要">
        <article><span>活跃学习者</span><strong>{{ analytics.overview.activeLearners }}</strong><small>当前周期匿名去重</small></article>
        <article><span>学习尝试</span><strong>{{ analytics.overview.totalAttempts }}</strong><small>{{ analytics.overview.completions }} 次完成</small></article>
        <article><span>完成率</span><strong>{{ rate(analytics.overview.completionRate) }}</strong><small>完成次数 / 学习尝试</small></article>
        <article><span>学习时长</span><strong class="duration">{{ duration(analytics.overview.totalTimeSeconds) }}</strong><small>当前周期累计</small></article>
      </section>

      <section class="stats-panel trend-panel">
        <header><div><p>ACTIVITY</p><h2>每日学习趋势</h2></div><span><i />尝试 <i class="complete" />完成</span></header>
        <div v-if="hasActivity" class="trend-chart" role="img" :aria-label="`近 ${days} 天学习趋势`">
          <div v-for="(item, index) in analytics.trend" :key="item.date" class="trend-day" :title="`${item.date}：${item.attempts} 次尝试，${item.completions} 次完成`">
            <div class="trend-day__bars">
              <i :style="{ height: `${Math.max(item.attempts ? 8 : 0, item.attempts / maxAttempts * 100)}%` }" />
              <i class="complete" :style="{ height: `${Math.max(item.completions ? 6 : 0, item.completions / maxAttempts * 100)}%` }" />
            </div>
            <time>{{ dateLabel(item.date, index) }}</time>
          </div>
        </div>
        <div v-else class="empty-state"><b>还没有学习记录</b><span>开始阅读或练习后，这里会自动出现简单趋势。</span></div>
      </section>

      <section class="stats-panel">
        <header><div><p>MODULES</p><h2>模块内容与触达</h2></div><span>仅展示内容数量和学习情况</span></header>
        <div class="module-grid">
          <article v-for="item in analytics.modules" :key="item.contentType" class="module-card">
            <div class="module-card__title"><i>{{ moduleMeta(item.contentType).glyph }}</i><div><small>{{ item.contentType }}</small><h3>{{ moduleMeta(item.contentType).label }}</h3></div></div>
            <dl>
              <div><dt>已发布内容</dt><dd>{{ item.publishedContent }}</dd></div>
              <div><dt>已触达内容</dt><dd>{{ item.engagedContent }}</dd></div>
              <div><dt>学习尝试</dt><dd>{{ item.attempts }}</dd></div>
              <div><dt>完成率</dt><dd>{{ rate(item.completionRate) }}</dd></div>
            </dl>
            <footer><span>完成 {{ item.completions }} 次</span><span>学习 {{ duration(item.timeSpentSeconds) }}</span></footer>
          </article>
        </div>
      </section>

      <footer class="privacy-note">本页面仅使用聚合统计，不展示学习者身份、写作正文或单次作答内容。</footer>
    </template>
  </section>
</template>

<style scoped>
.learning-stats{max-width:1320px;min-height:520px;margin:auto}.learning-stats__header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:18px;padding:24px 26px;border:1px solid var(--border);border-radius:20px;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 10%,var(--bg-surface)),var(--bg-surface))}.learning-stats__header p,.stats-panel header p{margin:0;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.15em}.learning-stats__header h1{margin:7px 0 5px;font-size:32px}.learning-stats__header>div>span,.stats-panel header>span{color:var(--text-secondary);font-size:12px}.learning-stats__filters{display:flex;align-items:center;gap:10px}.learning-stats__filters :deep(.el-select){width:138px}.range-switch{display:flex;padding:4px;border:1px solid var(--border);border-radius:11px;background:var(--bg-surface)}.range-switch button{border:0;border-radius:7px;padding:8px 11px;background:transparent;color:var(--text-secondary);font-size:11px;cursor:pointer;transition:background .16s ease,color .16s ease}.range-switch button.active{background:var(--primary);color:var(--on-primary,#fff)}.error-state{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:16px;padding:13px 16px;border:1px solid color-mix(in srgb,#d9534f 45%,var(--border));border-radius:12px;background:color-mix(in srgb,#d9534f 7%,var(--bg-surface))}.error-state strong,.error-state span{display:block}.error-state span{margin-top:3px;color:var(--text-secondary);font-size:11px}.error-state button{border:1px solid var(--border-strong);border-radius:8px;padding:7px 11px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.summary-grid article{display:flex;min-height:126px;flex-direction:column;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.summary-grid span{color:var(--text-secondary);font-size:12px}.summary-grid strong{margin:auto 0 3px;font-size:29px}.summary-grid strong.duration{font-size:20px}.summary-grid small{color:var(--text-muted);font-size:10px}.stats-panel{margin-top:14px;padding:20px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}.stats-panel>header{display:flex;align-items:flex-end;justify-content:space-between;gap:12px;margin-bottom:18px}.stats-panel h2{margin:5px 0 0;font-size:19px}.stats-panel header>span{display:flex;align-items:center;gap:5px}.stats-panel header>span i{width:8px;height:8px;border-radius:2px;background:var(--primary)}.stats-panel header>span i.complete{margin-left:6px;background:var(--accent)}.trend-chart{display:flex;align-items:stretch;gap:3px;height:220px;border-bottom:1px solid var(--border)}.trend-day{display:flex;min-width:3px;flex:1;flex-direction:column;align-items:center;justify-content:flex-end}.trend-day__bars{display:flex;width:100%;height:182px;align-items:flex-end;justify-content:center;gap:1px}.trend-day__bars i{display:block;width:min(7px,44%);min-width:2px;border-radius:4px 4px 1px 1px;background:var(--primary);transition:height .16s ease}.trend-day__bars i.complete{background:var(--accent)}.trend-day time{height:20px;margin-top:7px;color:var(--text-muted);font-size:8px;white-space:nowrap}.module-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.module-card{padding:16px;border:1px solid var(--border);border-radius:15px;background:var(--bg-subtle)}.module-card__title{display:flex;align-items:center;gap:10px}.module-card__title>i{display:grid;width:40px;height:40px;place-items:center;border-radius:12px;background:color-mix(in srgb,var(--primary) 12%,var(--bg-surface));color:var(--primary);font-style:normal;font-weight:800}.module-card__title small{color:var(--accent);font-size:8px}.module-card h3{margin:2px 0 0;font-size:16px}.module-card dl{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:15px 0}.module-card dl>div{padding:9px;border-radius:9px;background:var(--bg-surface)}.module-card dt{color:var(--text-muted);font-size:9px}.module-card dd{margin:3px 0 0;font-size:15px;font-weight:700}.module-card footer{display:flex;justify-content:space-between;color:var(--text-muted);font-size:9px}.empty-state{display:flex;min-height:180px;align-items:center;justify-content:center;flex-direction:column;text-align:center}.empty-state b{margin-bottom:7px}.empty-state span{color:var(--text-secondary);font-size:12px}.privacy-note{margin-top:14px;padding:13px 16px;border:1px dashed var(--border-strong);border-radius:13px;color:var(--text-muted);font-size:10px;text-align:center}
@media(max-width:1050px){.summary-grid,.module-grid{grid-template-columns:repeat(2,1fr)}.learning-stats__header{align-items:flex-start;flex-direction:column}.learning-stats__filters{width:100%}}
@media(max-width:650px){.learning-stats__header{padding:20px}.learning-stats__header h1{font-size:28px}.learning-stats__filters{align-items:stretch;flex-direction:column}.learning-stats__filters :deep(.el-select){width:100%}.range-switch button{flex:1}.summary-grid{grid-template-columns:repeat(2,1fr);gap:8px}.summary-grid article{min-height:112px;padding:14px}.module-grid{grid-template-columns:1fr}.stats-panel{padding:15px}.trend-chart{gap:1px;overflow-x:auto}.trend-day{min-width:4px}}
@media(max-width:390px){.range-switch button{padding-inline:7px}.summary-grid strong{font-size:24px}.summary-grid strong.duration{font-size:16px}.stats-panel>header{align-items:flex-start;flex-direction:column}}
@media(prefers-reduced-motion:reduce){.range-switch button,.trend-day__bars i{transition:none}}
</style>
