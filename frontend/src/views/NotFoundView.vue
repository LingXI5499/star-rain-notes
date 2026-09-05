<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found">
    <div class="not-found__ambient" aria-hidden="true"></div>
    <p class="not-found__eyebrow">LOST IN THE STARS</p>
    <h1 class="not-found__code">404</h1>
    <p class="not-found__body">这颗知识星还没有被记录。</p>
    <RouterLink class="not-found__link" to="/">返回首页 <span aria-hidden="true">→</span></RouterLink>
  </section>
</template>

<style scoped>
.not-found {
  position: relative;
  padding-block: var(--space-13);
  text-align: center;
  overflow: hidden;
}
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
  color: var(--text-secondary);
  margin-bottom: var(--space-6);
}
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
/* 低强度星点 ambient（§22），透明度低、Day/Night 自适应 */
.not-found__ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: .5;
  background-image:
    radial-gradient(1.5px 1.5px at 12% 28%, color-mix(in srgb, var(--text-primary) 55%, transparent), transparent),
    radial-gradient(1px 1px at 24% 64%, color-mix(in srgb, var(--accent) 55%, transparent), transparent),
    radial-gradient(2px 2px at 40% 35%, color-mix(in srgb, var(--text-primary) 45%, transparent), transparent),
    radial-gradient(1px 1px at 58% 72%, color-mix(in srgb, var(--text-primary) 40%, transparent), transparent),
    radial-gradient(1.5px 1.5px at 70% 30%, color-mix(in srgb, var(--accent) 45%, transparent), transparent),
    radial-gradient(1px 1px at 84% 58%, color-mix(in srgb, var(--text-primary) 50%, transparent), transparent),
    radial-gradient(2px 2px at 92% 84%, color-mix(in srgb, var(--primary) 42%, transparent), transparent);
}
@media (prefers-reduced-motion: no-preference) {
  .not-found__ambient {
    animation: not-found-drift 26s linear infinite alternate;
  }
}
@keyframes not-found-drift {
  from { transform: translateY(-6px); }
  to { transform: translateY(10px); }
}
@media (prefers-reduced-motion: reduce) {
  .not-found__ambient { animation: none; }
}
</style>
