<script setup>
import { computed } from 'vue'
import ChartPanel from './ChartPanel.vue'
import { dailyRows, hasTraffic, trendOption } from './statistics'
const props = defineProps({ rows: Array, range: Object, loading: Boolean, error: String })
defineEmits(['retry'])
const days = computed(() => dailyRows(props.rows, props.range))
const option = computed(() => trendOption(days.value))
</script>

<template>
  <ChartPanel title="访问趋势" :description="`${range.startDate} 至 ${range.endDate} · UTC · 两类访问分别统计`" :option="option" :loading="loading" :error="error" :empty="!hasTraffic(days)" empty-text="所选时段暂无访问记录。可尝试调整日期范围。" @retry="$emit('retry')">
    <template #actions><slot name="actions" /></template>
    <details class="statistics-details"><summary>查看每日明细</summary><div class="statistics-details__scroll"><table><caption>每日页面访问与内容访问次数（UTC）</caption><thead><tr><th scope="col">日期</th><th scope="col">页面访问</th><th scope="col">内容访问</th></tr></thead><tbody><tr v-for="day in days" :key="day.statDate"><th scope="row">{{ day.statDate }}</th><td>{{ day.pageViews }}</td><td>{{ day.contentViews }}</td></tr></tbody></table></div></details>
  </ChartPanel>
</template>
