<script setup>
/**
 * TechFlow.vue：作者资料模块的视图组件。
 * 职责范围：个人介绍、社交链接、经历技能和精选内容。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：items；默认值与类型见 defineProps。
 * 主要响应式状态：props、rows。
 */


import { computed } from 'vue'

/*
 * 「此刻关注」标签流 —— 从 V1 `components/about/TechFlow.vue` 移植。
 *
 * 把关注点拆成 2~3 行，每行起点逐行错开（第二行反向滚动），做出流动的标签带。
 * 数据来自调用方：V2 用 `skills` 里 category = DIRECTION 的名称（V1 是 currentFocus）。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
})

/* 行数规则照抄 V1：超过 8 项才铺三行，否则最多两行 */
/**
 * 行集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tech-flow。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 var(--space-3) 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tech-flow {
  display: grid;
  gap: var(--space-3);
  overflow: hidden;
  padding: var(--space-3) 0;
  /* 两端渐隐，标签带不会在容器边缘被硬切 */
  mask-image: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
}

/**
 * 样式范围：.tech-flow__row。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tech-flow__row {
  display: flex;
  width: max-content;
  gap: var(--space-4);
  animation: tech-flow-scroll var(--speed, 28s) linear infinite;
}

/**
 * 样式范围：.tech-flow__row--reverse。
 */
.tech-flow__row--reverse { animation-direction: reverse; }

/**
 * 样式范围：.tech-flow__track。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tech-flow__track {
  display: flex;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

/**
 * 样式范围：.tech-flow__track li。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 响应式或设备条件：(hover: hover)；仅在该条件成立时覆盖基础样式。
 */
@media (hover: hover) {
  /**
   * 样式范围：.tech-flow:hover .tech-flow__row,   .tech-flow__row:focus-within。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .tech-flow:hover .tech-flow__row,
  .tech-flow__row:focus-within { animation-play-state: paused; }
}

/**
 * 动画 tech-flow-scroll 的关键帧；帧内仅定义随动画时间改变的属性。
 */
@keyframes tech-flow-scroll {
  /**
   * 样式范围：from。
   */
  from { transform: translateX(0); }
  /**
   * 样式范围：to。
   */
  to { transform: translateX(-50%); }
}

/* 尊重系统的「减少动态效果」：静止换行显示，隐藏用于接缝的复制轨道 */
/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.tech-flow__row。
   * 溢出处理：整体 visible。
   */
  .tech-flow__row { width: auto; flex-wrap: wrap; overflow: visible; animation: none; }
  /**
   * 样式范围：.tech-flow__track[aria-hidden='true']。
   */
  .tech-flow__track[aria-hidden='true'] { display: none; }
}
</style>
