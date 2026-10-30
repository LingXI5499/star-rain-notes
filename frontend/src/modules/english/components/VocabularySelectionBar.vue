<script setup>
import { RouterLink } from 'vue-router'
import { masteryLabels } from '../api/vocabularyLearningApi'
defineProps({ count: Number, matched: Number, loading: Boolean, selectedAll: Boolean, account: Boolean, loginTarget: String, filters: Object, plan: Object })
defineEmits(['select-all', 'invert', 'clear', 'preview', 'filter'])
const fields = [
  { key: 'learned', label: '学习状态', options: [['ANY','全部状态'],['YES','已学'],['NO','未学']] },
  { key: 'mastery', label: '熟练度', options: [['ANY','全部熟练度'],['UNRATED','未评价'],...Object.entries(masteryLabels)] },
  { key: 'inPlan', label: '计划范围', options: [['ANY','全部词汇'],['YES','当前计划内'],['NO','当前计划外']] },
  { key: 'direction', label: '评价方向', options: [['ANY','任一方向'],['EN_TO_ZH','英译中'],['ZH_TO_EN','中译英'],['AUDIO_TO_BOTH','听音辨词']] },
  { key: 'lastRating', label: '最近评价', options: [['ANY','全部评价'],['FORGOT','忘记'],['UNCERTAIN','模糊'],['KNOW','掌握']] },
]
</script>
<template>
  <section class="selection-bar" aria-label="单词记忆学习">
    <header><div><h2>单词记忆</h2><p>选择词汇 → 核对词数与分组 → 开始三轮训练</p></div><nav aria-label="记忆学习导航"><RouterLink to="/english/vocabulary/plan">学习计划</RouterLink><RouterLink to="/english/vocabulary/study">今日背单词</RouterLink><RouterLink to="/english/vocabulary/progress">学习进度</RouterLink></nav></header>
    <p v-if="account && plan && plan.status!=='NONE'" class="selection-bar__plan">当前计划：{{plan.name}} · {{plan.totalWords}} 词 · 已完成 {{plan.completedRatings}} / {{plan.totalWords*3}} 次训练。待选不会立即替换当前计划。</p>
    <div class="selection-bar__actions"><div><strong role="status">{{selectedAll?'筛选全选':'已选'}} {{count}} 词</strong><span v-if="selectedAll">包含后续分页，取消单词可排除</span><span v-else>支持跨页、跨主题选择</span></div><div><button type="button" :disabled="!account || loading || !matched" @click="$emit('select-all')">全选当前筛选结果</button><button type="button" :disabled="loading || !matched" @click="$emit('invert')">反选当前页</button><button type="button" :disabled="loading || !count" @click="$emit('clear')">清空待选</button><button type="button" class="selection-bar__primary" :disabled="loading || !count" @click="$emit('preview')">查看待选并设置计划</button></div></div>
    <details v-if="account"><summary>按个人学习记录筛选</summary><div class="selection-bar__filters"><label v-for="field in fields" :key="field.key">{{field.label}}<select :aria-label="field.label" :value="filters[field.key]" :disabled="loading" @change="$emit('filter',field.key,$event.target.value)"><option v-for="[value,label] in field.options" :key="value" :value="value">{{label}}</option></select></label></div></details>
    <p v-else class="selection-bar__guest">可先浏览和手选单词；登录后可创建持久学习计划、筛选全选与保存评分。<RouterLink :to="{path:'/login',query:{redirect:loginTarget}}">登录后继续</RouterLink></p>
  </section>
</template>
<style scoped>
.selection-bar{margin:24px 0;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);color:var(--text-primary)}header,.selection-bar__actions{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:16px}h2{font-size:18px;line-height:28px;margin:0}p{font-size:12px;line-height:16px;color:var(--text-secondary);margin:4px 0 0}nav,.selection-bar__actions>div:last-child{display:flex;gap:8px;flex-wrap:wrap;align-items:center}a,button{display:inline-block;padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--primary);background:var(--bg-surface);font:inherit;font-size:14px;line-height:20px;cursor:pointer;text-decoration:none}a:hover,button:hover{background:var(--primary-soft);border-color:var(--primary)}button:active{transform:scale(.98)}button:disabled{opacity:.5;cursor:not-allowed}.selection-bar__primary{background:var(--primary);color:var(--text-inverse,#fff)}.selection-bar__actions{margin-top:24px}.selection-bar__actions strong{font-size:16px;line-height:24px;font-variant-numeric:tabular-nums}.selection-bar__actions span{display:block;font-size:12px;line-height:16px;color:var(--text-muted);margin-top:4px}.selection-bar__plan,.selection-bar__guest{margin-top:16px}.selection-bar__guest a{padding:0;border:0;font-size:12px}summary{color:var(--primary);cursor:pointer;font-size:14px;line-height:20px;padding-top:16px}.selection-bar__filters{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:16px;margin-top:16px}label{display:grid;gap:8px;font-size:12px;line-height:16px}select{box-sizing:border-box;min-width:0;width:100%;padding:8px 12px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);font:inherit;font-size:14px;line-height:20px}:is(a,button,summary,select):focus-visible{outline:2px solid var(--primary);outline-offset:2px}@media(max-width:640px){.selection-bar{padding:16px}.selection-bar__actions>div:last-child{width:100%}.selection-bar__actions button{flex:1 1 auto}}
</style>
