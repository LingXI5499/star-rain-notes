<script setup>
/**
 * ReadingProgress.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 主要响应式状态：progress。
 * 阅读入口：onScroll（响应滚动）。
 */


import { onBeforeUnmount, onMounted, ref } from 'vue'

/*
 * 阅读进度条 —— 从 V1 `components/ui/ReadingProgress.vue` 移植成 JS。
 * 固定在页面顶部 2px，只做视觉提示，不参与交互（aria-hidden）。
 * 滚动监听用 requestAnimationFrame 节流，窗口尺寸变化也要重算。
 */
/**
 * 进度；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const progress = ref(0)
let ticking = false

/**
 * 响应滚动。
 * 主要协作：requestAnimationFrame。
 * 更新的响应式状态包括 progress；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.reading-progress。
 * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  z-index: calc(var(--z-header, 20) + 1);
  pointer-events: none;
}

/**
 * 样式范围：.reading-progress span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.reading-progress span {
  display: block;
  height: 100%;
  width: 100%;
  transform-origin: 0 50%;
  transform: scaleX(0);
  background: var(--primary);
}
</style>
