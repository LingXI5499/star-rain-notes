<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, AriaComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, AriaComponent, SVGRenderer])
const props = defineProps({ option: Object, title: String })
const root = ref(null), palette = ref({}), reducedMotion = ref(false)
let observer, motion
function readTheme() {
  const styles = getComputedStyle(root.value)
  palette.value = {
    text: styles.getPropertyValue('--text-secondary').trim() || '#65736d',
    border: styles.getPropertyValue('--border').trim() || '#e0e5e1',
    primary: styles.getPropertyValue('--primary').trim() || '#24766a',
    surface: styles.getPropertyValue('--bg-surface').trim() || '#ffffff',
    dark: document.documentElement.dataset.theme === 'dark',
  }
}
function motionChanged() { reducedMotion.value = Boolean(motion?.matches) }
onMounted(() => {
  readTheme()
  observer = new MutationObserver(readTheme)
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  motion = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionChanged(); motion.addEventListener('change', motionChanged)
})
onBeforeUnmount(() => { observer?.disconnect(); motion?.removeEventListener('change', motionChanged) })
const themedOption = computed(() => {
  const p = palette.value, option = props.option || {}
  const axis = value => value && ({
    ...value, axisLabel: { color: p.text, fontSize: 12, ...value.axisLabel },
    axisLine: { lineStyle: { color: p.border } }, axisTick: { show: false },
    splitLine: { show: value.type === 'value', lineStyle: { color: p.border, type: 'dashed' } },
  })
  return {
    ...option, backgroundColor: 'transparent', animation: !reducedMotion.value, animationDuration: 500,
    color: [p.dark ? '#72c9b3' : p.primary, '#c28a52', '#7296c3', '#a18ab8', '#8a9b68', '#c37f88'],
    textStyle: { color: p.text, fontFamily: 'inherit' }, aria: { enabled: true },
    xAxis: axis(option.xAxis), yAxis: axis(option.yAxis),
    legend: option.legend && { ...option.legend, textStyle: { color: p.text } },
    tooltip: { ...option.tooltip, renderMode: 'richText', backgroundColor: p.surface, borderColor: p.border, textStyle: { color: p.text } },
  }
})
</script>

<template>
  <div ref="root" class="statistics-canvas" role="img" :aria-label="title">
    <VChart :option="themedOption" :init-options="{ renderer: 'svg' }" :update-options="{ notMerge: true }" :autoresize="{ throttle: 100 }" />
  </div>
</template>

<style scoped>
.statistics-canvas{height:320px;min-width:0;width:100%}
</style>
