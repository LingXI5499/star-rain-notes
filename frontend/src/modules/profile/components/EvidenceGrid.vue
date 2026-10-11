<script setup>
/**
 * EvidenceGrid.vue：作者资料模块的视图组件。
 * 职责范围：个人介绍、社交链接、经历技能和精选内容。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router。
 * 组件输入：items；默认值与类型见 defineProps。
 */


/*
 * 工程证据卡片网格 —— 从 V1 `components/about/EvidenceGrid.vue` 移植。
 *
 * V1 用 `<a href>`，V2 必须是 RouterLink：账号树下要落在 /useradmin 前缀，
 * 所以 `to` 由调用方经 useViewMode().contentPath() 生成，本组件不做路径加工。
 */
import { RouterLink } from 'vue-router'

defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <div class="evidence-grid" role="list">
    <RouterLink
      v-for="(item, index) in items"
      :key="`${item.to}-${index}`"
      class="evidence-card"
      role="listitem"
      :to="item.to"
    >
      <span class="evidence-card__kind">{{ item.kind }}</span>
      <span class="evidence-card__title">{{ item.title }}</span>
      <span v-if="item.note" class="evidence-card__note">{{ item.note }}</span>
      <span class="evidence-card__arrow" aria-hidden="true">→</span>
    </RouterLink>
  </div>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.evidence-grid。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 var(--space-4) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

/**
 * 样式范围：.evidence-card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  background: var(--bg-surface);
  transition:
    border-color var(--motion-fast) var(--ease-standard),
    box-shadow var(--motion-base) var(--ease-standard),
    transform var(--motion-base) var(--ease-out);
}

/**
 * 样式范围：.evidence-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

/**
 * 样式范围：.evidence-card__kind。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card__kind {
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.evidence-card__title。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card__title {
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 650;
}

/**
 * 样式范围：.evidence-card__note。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card__note {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

/**
 * 样式范围：.evidence-card__arrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.evidence-card__arrow {
  align-self: flex-start;
  margin-top: auto;
  color: var(--primary);
  transition: transform var(--motion-base) var(--ease-out);
}

/**
 * 样式范围：.evidence-card:hover .evidence-card__arrow。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.evidence-card:hover .evidence-card__arrow { transform: translateX(4px); }

/**
 * 响应式或设备条件：(max-width: 640px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 640px) {
  /**
   * 样式范围：.evidence-grid。
   */
  .evidence-grid { grid-template-columns: 1fr; }
}

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.evidence-card,   .evidence-card__arrow。
   */
  .evidence-card,
  .evidence-card__arrow { transition: none; }
}
</style>
