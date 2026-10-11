<script setup>
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
/* V1：四格等宽、发丝边框、直角到 --radius-lg 的圆角，移动端收成两列（768px 断点一致） */
.career-snapshot {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin: 0;
}

.career-snapshot__cell {
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
}

.career-snapshot__cell dt {
  margin-bottom: var(--space-3);
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

.career-snapshot__cell dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .career-snapshot { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
