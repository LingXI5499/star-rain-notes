<script setup>
/**
 * BlogArchiveCalendar.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：year、month、days、months、selectedDay、loading；默认值与类型见 defineProps。
 * 组件输出事件：select-day、select-month；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、counts、leadingBlanks、dayCount、cells、monthKey。
 * 阅读入口：changeMonth（change月份的处理函数）。
 */


import { computed } from 'vue'

/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  days: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  selectedDay: { type: Number, default: null },
  loading: { type: Boolean, default: false },
})
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['select-day', 'select-month'])

/**
 * 数量集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const counts = computed(() => new Map(props.days.map((item) => [Number(item.day), Number(item.postCount) || 0])))
/**
 * leadingBlanks；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const leadingBlanks = computed(() => (new Date(props.year, props.month - 1, 1).getDay() + 6) % 7)
/**
 * 日数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const dayCount = computed(() => new Date(props.year, props.month, 0).getDate())
/**
 * cells；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const cells = computed(() => [
  ...Array.from({ length: leadingBlanks.value }, () => null),
  ...Array.from({ length: dayCount.value }, (_, index) => index + 1),
])
/**
 * 月份键；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const monthKey = computed(() => `${props.year}-${String(props.month).padStart(2, '0')}`)

/**
 * change月份的处理函数。
 * 主要协作：props.months.find、.padStart、emit。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function changeMonth(event) {
  const selected = props.months.find((item) => `${item.year}-${String(item.month).padStart(2, '0')}` === event.target.value)
  if (selected) emit('select-month', selected)
}
</script>

<template>
  <section class="archive-calendar" aria-label="发布日历">
    <header class="archive-calendar__head">
      <div>
        <span>发布日期</span>
        <h2>{{ year }} 年 {{ month }} 月</h2>
      </div>
      <select :value="monthKey" aria-label="选择归档月份" @change="changeMonth">
        <option v-for="item in months" :key="`${item.year}-${item.month}`" :value="`${item.year}-${String(item.month).padStart(2, '0')}`">
          {{ item.year }} 年 {{ item.month }} 月 · {{ item.postCount }} 篇
        </option>
      </select>
    </header>
    <div class="archive-calendar__grid" role="grid" :aria-label="`${year} 年 ${month} 月发布日历`">
      <span v-for="weekday in ['一', '二', '三', '四', '五', '六', '日']" :key="weekday" class="archive-calendar__weekday">{{ weekday }}</span>
      <span v-for="(_, index) in leadingBlanks" :key="`blank-${index}`" aria-hidden="true" />
      <button
        v-for="day in cells.slice(leadingBlanks)"
        :key="day"
        type="button"
        :disabled="loading || !counts.get(day)"
        :class="['archive-calendar__day', selectedDay === day && 'is-selected']"
        :aria-pressed="selectedDay === day"
        :aria-label="`${month} 月 ${day} 日，${counts.get(day) || 0} 篇文章`"
        @click="emit('select-day', day)"
      >
        <strong>{{ day }}</strong>
        <small v-if="counts.get(day)">{{ counts.get(day) }} 篇</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.archive-calendar。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar {
  margin-bottom: var(--space-6);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
}
/**
 * 样式范围：.archive-calendar__head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
/**
 * 样式范围：.archive-calendar__head span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__head span { color: var(--text-muted); font-size: 11px; }
/**
 * 样式范围：.archive-calendar__head h2。
 */
.archive-calendar__head h2 { margin: 3px 0 0; font-size: 19px; }
/**
 * 样式范围：.archive-calendar__head select。
 */
.archive-calendar__head select { width: auto; max-width: 55%; }
/**
 * 样式范围：.archive-calendar__grid。
 * 使用网格布局组织内容；列定义为 repeat(7, minmax(0, 1fr))，间距由 5px 控制。
 */
.archive-calendar__grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 5px; }
/**
 * 样式范围：.archive-calendar__weekday。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__weekday { padding: 5px; color: var(--text-muted); font-size: 11px; text-align: center; }
/**
 * 样式范围：.archive-calendar__day。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 2px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__day {
  display: grid;
  align-content: center;
  gap: 2px;
  min-height: 56px;
  padding: 5px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-page);
  text-align: center;
  cursor: pointer;
}
/**
 * 样式范围：.archive-calendar__day small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__day small { color: var(--primary); font-size: 10px; }
/**
 * 样式范围：.archive-calendar__day:disabled。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__day:disabled { border-color: transparent; color: var(--text-muted); background: transparent; opacity: 0.55; cursor: default; }
/**
 * 样式范围：.archive-calendar__day.is-selected。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.archive-calendar__day.is-selected { border-color: var(--primary); background: color-mix(in srgb, var(--primary) 12%, var(--bg-surface)); }
/**
 * 响应式或设备条件：(max-width: 600px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 600px) {
  /**
   * 样式范围：.archive-calendar。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .archive-calendar { padding: var(--space-3); }
  /**
   * 样式范围：.archive-calendar__head。
   */
  .archive-calendar__head { align-items: stretch; flex-direction: column; }
  /**
   * 样式范围：.archive-calendar__head select。
   */
  .archive-calendar__head select { max-width: 100%; }
  /**
   * 样式范围：.archive-calendar__day。
   */
  .archive-calendar__day { min-height: 44px; font-size: 12px; }
  /**
   * 样式范围：.archive-calendar__day small。
   */
  .archive-calendar__day small { font-size: 9px; }
}
</style>
