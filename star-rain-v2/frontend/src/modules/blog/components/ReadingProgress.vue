<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/*
 * 阅读进度条 —— 从 V1 `components/ui/ReadingProgress.vue` 移植成 JS。
 * 固定在页面顶部 2px，只做视觉提示，不参与交互（aria-hidden）。
 * 滚动监听用 requestAnimationFrame 节流，窗口尺寸变化也要重算。
 */
const progress = ref(0)
let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const scrollTop = window.scrollY
    const height = document.documentElement.scrollHeight - window.innerHeight
    progress.value = height > 0 ? Math.min(1, scrollTop / height) : 0
    ticking = false
  })
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
})
</script>

<template>
  <div class="reading-progress" aria-hidden="true" role="presentation">
    <span :style="{ transform: `scaleX(${progress})` }" />
  </div>
</template>

<style scoped>
.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  z-index: calc(var(--z-header, 20) + 1);
  pointer-events: none;
}

.reading-progress span {
  display: block;
  height: 100%;
  width: 100%;
  transform-origin: 0 50%;
  transform: scaleX(0);
  background: var(--primary);
}
</style>
