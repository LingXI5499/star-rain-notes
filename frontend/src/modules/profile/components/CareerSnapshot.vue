<script setup>
/**
 * CareerSnapshot.vue：作者资料模块的视图组件。
 * 职责范围：个人介绍、社交链接、经历技能和精选内容。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 组件输入：headline、focus、featured、status；默认值与类型见 defineProps。
 */


/*
 * 履历速览四格 —— 从 V1 `components/about/CareerSnapshot.vue` 移植。
 *
 * 逐个对齐 V1 的四格顺序与文案：CURRENT（headline，缺省「持续学习与构建中」）、
 * DIRECTION（关注点用「 / 」连接，缺省「全栈软件工程」）、
 * FEATURED（首个代表作品，缺省「星雨笔录」）、STATUS（由调用方传入的阶段说明）。
 *
 * V1 用 TypeScript 声明 props，V2 是纯 JS，改成运行时声明，取值语义不变：
 * 调用方拿不到数据时传 null / 空数组，四格仍然全部渲染（V1 也不隐藏任何一格）。
 */
defineProps({
  headline: { type: String, default: null },
  focus: { type: Array, default: () => [] },
  featured: { type: String, default: null },
  status: { type: String, default: '' },
})
</script>

<template>
  <dl class="career-snapshot">
    <div class="career-snapshot__cell">
      <dt>CURRENT</dt>
      <dd>{{ headline || '持续学习与构建中' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>DIRECTION</dt>
      <dd>{{ focus.length ? focus.join(' / ') : '全栈软件工程' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>FEATURED</dt>
      <dd>{{ featured || '星雨笔录' }}</dd>
    </div>
    <div class="career-snapshot__cell">
      <dt>STATUS</dt>
      <dd>{{ status }}</dd>
    </div>
  </dl>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/* V1：四格等宽、发丝边框、直角到 --radius-lg 的圆角，移动端收成两列（768px 断点一致） */
.career-snapshot {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin: 0;
}

/**
 * 样式范围：.career-snapshot__cell。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.career-snapshot__cell {
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
}

/**
 * 样式范围：.career-snapshot__cell dt。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.career-snapshot__cell dt {
  margin-bottom: var(--space-3);
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.career-snapshot__cell dd。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.career-snapshot__cell dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.7;
}

/**
 * 响应式或设备条件：(max-width: 768px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 768px) {
  /**
   * 样式范围：.career-snapshot。
   */
  .career-snapshot { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
