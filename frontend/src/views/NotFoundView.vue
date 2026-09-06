<script setup lang="ts">
import { RouterLink } from 'vue-router'
import StarfallScene from '@/components/visual/StarfallScene.vue'
</script>

<template>
  <section class="not-found">
    <div class="not-found__scene"><StarfallScene label="未找到页面的星雨远山场景" /></div>
    <div class="not-found__copy">
      <p class="not-found__eyebrow">LOST IN THE STARS</p>
      <h1 class="not-found__code">404</h1>
      <p class="not-found__body">这颗知识星还没有被记录。</p>
      <p class="not-found__hint">地址可能已经改变，也可能只是暂时偏离了知识轨道。</p>
      <RouterLink class="not-found__link" to="/">返回首页 <span aria-hidden="true">→</span></RouterLink>
    </div>
  </section>
</template>

<style scoped>
.not-found {
  position: relative;
  min-height: min(700px, calc(100vh - var(--header-height) - 120px));
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, .85fr);
  align-items: center;
  gap: clamp(28px, 6vw, 88px);
  padding-block: clamp(28px, 7vw, 80px);
  overflow: hidden;
}
.not-found__scene { min-width: 0; }
.not-found__copy { position: relative; z-index: 1; }
.not-found__eyebrow {
  margin: 0 0 var(--space-3);
  color: var(--accent);
  font: 700 11px var(--font-mono, monospace);
  letter-spacing: .2em;
}
.not-found__code {
  font-size: clamp(80px, 14vw, 128px);
  line-height: .95;
  letter-spacing: -.05em;
  color: var(--primary);
  margin-bottom: var(--space-4);
}
.not-found__body {
  font-size: clamp(20px, 2.4vw, 30px);
  font-weight: 680;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.not-found__hint { margin: 0 0 28px; color: var(--text-muted); line-height: 1.7; }
.not-found__link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  border: 1px solid var(--primary);
  border-radius: var(--radius-md);
  color: var(--primary);
  font-size: 14px;
  font-weight: 650;
}
.not-found__link:hover {
  background: var(--primary);
  color: var(--bg-page);
}
@media (max-width: 720px) {
  .not-found { grid-template-columns: 1fr; text-align: center; }
  .not-found__scene { order: 2; }
}
</style>
