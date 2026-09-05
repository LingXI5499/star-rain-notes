<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ items: string[] }>()

/** Split the list into 2-3 rows, rotating the start so rows read differently. */
const rows = computed(() => {
  const list = props.items.length ? props.items : ['JAVA', 'SPRING BOOT', 'MYSQL', 'VUE 3', 'TYPESCRIPT', 'LINUX']
  const rowCount = list.length > 8 ? 3 : 2
  const size = Math.ceil(list.length / rowCount)
  return Array.from({ length: rowCount }, (_, i) => {
    const rotated = [...list.slice(i), ...list.slice(0, i)]
    return rotated.slice(0, size)
  }).filter((row) => row.length)
})
</script>

<template>
  <div class="tech-flow" aria-label="技术方向">
    <div
      v-for="(row, index) in rows"
      :key="index"
      class="tech-flow__row"
      :class="{ 'tech-flow__row--reverse': index % 2 === 1 }"
      :style="{ '--speed': `${24 + index * 7}s` }"
    >
      <ul class="tech-flow__track">
        <li v-for="item in row" :key="item">{{ item }}</li>
      </ul>
      <ul class="tech-flow__track" aria-hidden="true">
        <li v-for="item in row" :key="item">{{ item }}</li>
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
  list-style: none;
  padding: 0;
  margin: 0;
}
.tech-flow__track li {
  flex: 0 0 auto;
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font: 600 13px var(--font-mono, monospace);
  letter-spacing: .08em;
  white-space: nowrap;
}
@media (hover: hover) {
  .tech-flow:hover .tech-flow__row,
  .tech-flow__row:focus-within { animation-play-state: paused; }
}
@keyframes tech-flow-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}
@media (prefers-reduced-motion: reduce) {
  .tech-flow__row { animation: none; width: auto; flex-wrap: wrap; overflow: visible; }
  .tech-flow__track[aria-hidden='true'] { display: none; }
}
</style>
