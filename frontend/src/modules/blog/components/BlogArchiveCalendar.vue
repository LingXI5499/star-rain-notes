<script setup>
import { computed } from 'vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  days: { type: Array, default: () => [] },
  months: { type: Array, default: () => [] },
  selectedDay: { type: Number, default: null },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['select-day', 'select-month'])

const counts = computed(() => new Map(props.days.map((item) => [Number(item.day), Number(item.postCount) || 0])))
const leadingBlanks = computed(() => (new Date(props.year, props.month - 1, 1).getDay() + 6) % 7)
const dayCount = computed(() => new Date(props.year, props.month, 0).getDate())
const cells = computed(() => [
  ...Array.from({ length: leadingBlanks.value }, () => null),
  ...Array.from({ length: dayCount.value }, (_, index) => index + 1),
])
const monthKey = computed(() => `${props.year}-${String(props.month).padStart(2, '0')}`)

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
.archive-calendar {
  margin-bottom: var(--space-6);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
}
.archive-calendar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.archive-calendar__head span { color: var(--text-muted); font-size: 11px; }
.archive-calendar__head h2 { margin: 3px 0 0; font-size: 19px; }
.archive-calendar__head select { width: auto; max-width: 55%; }
.archive-calendar__grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 5px; }
.archive-calendar__weekday { padding: 5px; color: var(--text-muted); font-size: 11px; text-align: center; }
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
.archive-calendar__day small { color: var(--primary); font-size: 10px; }
.archive-calendar__day:disabled { border-color: transparent; color: var(--text-muted); background: transparent; opacity: 0.55; cursor: default; }
.archive-calendar__day.is-selected { border-color: var(--primary); background: color-mix(in srgb, var(--primary) 12%, var(--bg-surface)); }
@media (max-width: 600px) {
  .archive-calendar { padding: var(--space-3); }
  .archive-calendar__head { align-items: stretch; flex-direction: column; }
  .archive-calendar__head select { max-width: 100%; }
  .archive-calendar__day { min-height: 44px; font-size: 12px; }
  .archive-calendar__day small { font-size: 9px; }
}
</style>
