<script setup>
import { computed } from 'vue'

/*
 * 「此刻关注」标签流 —— 从 V1 `components/about/TechFlow.vue` 移植。
 *
 * 把关注点拆成 2~3 行，每行起点逐行错开（第二行反向滚动），做出流动的标签带。
 * 数据来自调用方：V2 用 `skills` 里 category = DIRECTION 的名称（V1 是 currentFocus）。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
})

/* 行数规则照抄 V1：超过 8 项才铺三行，否则最多两行 */
const rows = computed(() => {
  const list = props.items.filter(Boolean)
  if (!list.length) return []
  const rowCount = list.length > 8 ? 3 : Math.min(2, list.length)
  return Array.from({ length: rowCount }, (_, index) => [
    ...list.slice(index),
    ...list.slice(0, index),
  ])
})
</script>

<template>
  <div class="tech-flow" aria-label="当前关注点">
    <div
      v-for="(row, index) in rows"
      :key="index"
      class="tech-flow__row"
      :class="{ 'tech-flow__row--reverse': index % 2 === 1 }"
      :style="{ '--speed': `${24 + index * 7}s` }"
    >
      <!-- 两段相同的轨道首尾相接，滚动到 -50% 时看不出接缝 -->
      <ul class="tech-flow__track">
        <li v-for="(item, itemIndex) in row" :key="`${item}-${itemIndex}`">{{ item }}</li>
      </ul>
      <ul class="tech-flow__track" aria-hidden="true">
        <li v-for="(item, itemIndex) in row" :key="`dup-${item}-${itemIndex}`">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.tech-flow {
  display: grid;
  gap: var(--space-3);
  overflow: hidden;
  padding: var(--space-3) 0;
  /* 两端渐隐，标签带不会在容器边缘被硬切 */
  mask-image: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
}

.tech-flow__row {
  display: flex;
  width: max-content;
  gap: var(--space-4);
  animation: tech-flow-scroll var(--speed, 28s) linear infinite;
}

.tech-flow__row--reverse { animation-direction: reverse; }

.tech-flow__track {
  display: flex;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

.tech-flow__track li {
  flex: 0 0 auto;
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: var(--bg-surface);
  font: 600 13px var(--font-mono);
  letter-spacing: 0.08em;
  white-space: nowrap;
}

/* 悬停暂停，方便读清标签；触屏没有 hover 时不触发 */
@media (hover: hover) {
  .tech-flow:hover .tech-flow__row,
  .tech-flow__row:focus-within { animation-play-state: paused; }
}

@keyframes tech-flow-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

/* 尊重系统的「减少动态效果」：静止换行显示，隐藏用于接缝的复制轨道 */
@media (prefers-reduced-motion: reduce) {
  .tech-flow__row { width: auto; flex-wrap: wrap; overflow: visible; animation: none; }
  .tech-flow__track[aria-hidden='true'] { display: none; }
}
</style>
