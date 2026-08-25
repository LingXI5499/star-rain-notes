<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchLearningInsights, type LearningInsights } from '@/api/englishLearning'

const insights = ref<LearningInsights | null>(null)
const loading = ref(true)
const failed = ref(false)

const moduleLabels: Record<string, string> = { GRAMMAR: '语法', READING: '阅读', LISTENING: '听力', WRITING: '写作' }
const recommendationMeta: Record<string, { label: string; tone: string }> = {
  REVIEW: { label: '今日复习', tone: 'urgent' },
  CONTINUE: { label: '继续学习', tone: 'continue' },
  BUNDLE_NEXT: { label: '路径下一步', tone: 'path' },
  PAIRED: { label: '读听联动', tone: 'paired' },
  TAG_MATCH: { label: '同主题拓展', tone: 'topic' },
  STARTER: { label: '推荐起点', tone: 'starter' },
}
const maxAttempts = computed(() => Math.max(1, ...(insights.value?.activity.map((item) => item.attempts) ?? [1])))

function percent(value: number | null) {
  return value == null ? 0 : Math.round(Math.max(0, Math.min(1, value)) * 100)
}

function duration(seconds: number) {
  if (seconds < 60) return `${seconds} 秒`
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours ? `${hours} 小时 ${minutes} 分` : `${minutes} 分钟`
}

onMounted(async () => {
  try { insights.value = await fetchLearningInsights() }
  catch { failed.value = true }
  finally { loading.value = false }
})
</script>

<template>
  <section class="insights-page">
    <header class="insights-hero">
      <div>
        <p>LEARNING INSIGHTS · 学习洞察</p>
        <h1>把每一次练习，变成下一步方向</h1>
        <span>根据真实学习记录生成趋势、掌握度与复习建议，通过当前浏览器生成的匿名身份关联，不保存账号信息。</span>
      </div>
      <RouterLink to="/english" class="back-link">返回英语中心</RouterLink>
    </header>

    <div v-if="loading" class="state">正在整理学习数据…</div>
    <div v-else-if="failed" class="state">暂时无法加载学习洞察，请稍后重试。</div>
    <template v-else-if="insights">
      <section class="metrics" aria-label="学习概览">
        <article><small>累计学习</small><strong>{{ duration(insights.totalTimeSeconds) }}</strong><span>{{ insights.totalAttempts }} 次记录</span></article>
        <article><small>连续学习</small><strong>{{ insights.currentStreak }} 天</strong><span>近 14 天活跃 {{ insights.activeDays14 }} 天</span></article>
        <article><small>平均得分</small><strong>{{ insights.averageScore == null ? '—' : Math.round(insights.averageScore) }}</strong><span>由已评分练习生成</span></article>
        <article><small>综合掌握度</small><strong>{{ percent(insights.averageMastery) }}%</strong><span>随每次学习动态更新</span></article>
      </section>

      <div class="insights-grid">
        <section class="panel activity-panel">
          <header><div><small>ACTIVITY</small><h2>14 日学习节奏</h2></div><span>保持小步、连续输入</span></header>
          <div class="activity-chart" role="img" aria-label="最近十四天学习次数柱状图">
            <div v-for="day in insights.activity" :key="day.date" class="activity-day">
              <span class="activity-value">{{ day.attempts || '' }}</span>
              <div class="activity-track"><i :style="{ height: `${Math.max(day.attempts ? 12 : 3, day.attempts / maxAttempts * 100)}%` }" /></div>
              <small>{{ day.date.slice(5).replace('-', '/') }}</small>
            </div>
          </div>
        </section>

        <section class="panel recommendation-panel">
          <header><div><small>NEXT ACTION</small><h2>下一步建议</h2></div></header>
          <div v-if="insights.recommendations.length" class="recommendations">
            <RouterLink v-for="item in insights.recommendations" :key="`${item.contentType}-${item.contentId}`" :to="item.route" :class="`is-${recommendationMeta[item.recommendationType]?.tone || 'starter'}`">
              <i>{{ recommendationMeta[item.recommendationType]?.label || item.reason }}</i>
              <div><span>{{ moduleLabels[item.contentType] }}<template v-if="item.cefrLevel"> · {{ item.cefrLevel }}</template><template v-if="item.sourceTitle"> · 来自「{{ item.sourceTitle }}」</template></span><strong>{{ item.title }}</strong></div>
              <em>继续 →</em>
            </RouterLink>
          </div>
          <p v-else class="empty">暂无待办，选择一个模块开始新的学习。</p>
        </section>
      </div>

      <section class="panel module-panel">
        <header><div><small>MODULE MASTERY</small><h2>模块掌握情况</h2></div><RouterLink to="/english/bundles">查看学习组合 →</RouterLink></header>
        <div class="modules">
          <article v-for="item in insights.modules" :key="item.contentType">
            <div><strong>{{ moduleLabels[item.contentType] }}</strong><span>{{ item.completed }} / {{ item.total }} 已完成</span></div>
            <div class="mastery"><i :style="{ width: `${percent(item.averageMastery)}%` }" /></div>
            <small>掌握度 {{ percent(item.averageMastery) }}% · {{ duration(item.timeSpentSeconds) }}</small>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.insights-page{max-width:1180px;min-width:0;margin:auto}.insights-hero{display:flex;justify-content:space-between;align-items:end;gap:30px;padding:22px 0 36px;border-bottom:1px solid var(--border)}.insights-hero p,.panel header small{color:var(--accent);font-size:11px;font-weight:800;letter-spacing:.15em}.insights-hero h1{max-width:760px;margin:9px 0 12px;font-size:clamp(32px,5vw,54px);line-height:1.12}.insights-hero span{display:block;max-width:720px;color:var(--text-secondary);line-height:1.7}.back-link{flex:none;border:1px solid var(--border);border-radius:999px;padding:10px 16px;color:var(--text-secondary)}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:28px 0}.metrics article,.panel{border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}.metrics article{display:flex;flex-direction:column;padding:20px;background:linear-gradient(145deg,var(--bg-surface),color-mix(in srgb,var(--primary) 5%,var(--bg-surface)))}.metrics small{color:var(--text-muted)}.metrics strong{margin:12px 0 8px;font-size:26px}.metrics span{color:var(--text-secondary);font-size:12px}.insights-grid{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(320px,.65fr);gap:18px;min-width:0}.panel{min-width:0;padding:22px}.panel>header{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:22px}.panel h2{margin:5px 0 0;font-size:21px}.panel header>span,.panel header>a{color:var(--text-muted);font-size:12px}.activity-chart{display:grid;grid-template-columns:repeat(14,minmax(24px,1fr));gap:8px;height:210px}.activity-day{display:grid;grid-template-rows:20px 1fr 22px;gap:5px;text-align:center}.activity-value{font-size:10px;color:var(--primary)}.activity-track{display:flex;align-items:end;justify-content:center;border-radius:10px;background:var(--bg-subtle);overflow:hidden}.activity-track i{display:block;width:100%;min-height:3px;border-radius:9px;background:linear-gradient(180deg,var(--accent),var(--primary));transition:height .16s ease}.activity-day small{font-size:9px;color:var(--text-muted);writing-mode:vertical-rl}.recommendations{display:flex;flex-direction:column;gap:9px}.recommendations a{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:13px;border-radius:13px;background:var(--bg-subtle);color:inherit;transition:transform .16s ease,background .16s ease}.recommendations a:hover{transform:translateX(3px);background:color-mix(in srgb,var(--primary) 8%,var(--bg-subtle))}.recommendations span{display:block;color:var(--accent);font-size:10px}.recommendations strong{display:block;margin-top:4px;font-size:14px}.recommendations em{flex:none;color:var(--primary);font-size:12px;font-style:normal}.module-panel{margin-top:18px}.modules{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.modules article{padding:15px;border-radius:14px;background:var(--bg-subtle)}.modules article>div:first-child{display:flex;justify-content:space-between;gap:10px}.modules article span,.modules article small{color:var(--text-muted);font-size:11px}.mastery{height:6px;margin:16px 0 10px;border-radius:999px;background:var(--border);overflow:hidden}.mastery i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,var(--primary),var(--accent))}.state,.empty{padding:80px 0;text-align:center;color:var(--text-muted)}@media(max-width:900px){.metrics,.modules{grid-template-columns:repeat(2,1fr)}.insights-grid{grid-template-columns:1fr}}@media(max-width:600px){.insights-hero{align-items:flex-start;flex-direction:column}.metrics,.modules{grid-template-columns:1fr}.activity-chart{grid-template-columns:repeat(14,minmax(0,1fr));gap:4px}.panel{padding:16px}.activity-day small{font-size:8px}}@media(prefers-reduced-motion:reduce){.activity-track i,.recommendations a{transition:none}}
.recommendations a{display:grid;grid-template-columns:auto minmax(0,1fr) auto;justify-content:initial;gap:11px}.recommendations a>i{padding:5px 7px;border-radius:7px;background:color-mix(in srgb,var(--primary) 10%,var(--bg-surface));color:var(--primary);font-size:9px;font-style:normal;font-weight:800;white-space:nowrap}.recommendations .is-urgent>i{background:color-mix(in srgb,#ef8354 13%,var(--bg-surface));color:#d96432}.recommendations .is-path>i{background:color-mix(in srgb,var(--accent) 13%,var(--bg-surface));color:var(--accent)}.recommendations span,.recommendations strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.recommendations span{color:var(--text-muted)}
@media(max-width:600px){.recommendations a{grid-template-columns:1fr auto}.recommendations a>i{grid-column:1/-1;width:max-content}}
</style>
