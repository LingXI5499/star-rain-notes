<script setup>
import { useViewMode } from '../../../shared/viewMode'

/*
 * 未实现模块的占位页（教程 / 作品 / 关于 / 搜索）。
 *
 * 为什么给它们真路由，而不是继续留成不可点的导航项：
 * 所有者给出的目标形态把 /tutorials、/portfolio、/about、/search 列进了公开树的路由清单，
 * 地址栏直接输入时应该落在一个说明「模块建设中」的页面上，
 * 而不是被兜底重定向弹回首页（那会让人以为地址写错了）。
 * 导航里这三项仍然渲染成不可点的占位，不改既有行为。
 *
 * 页面刻意不引用任何接口：后端模块不存在，这里能说的只有事实。
 * 页内链接按当前路径树生成，账号模式下不会把外壳点掉。
 */
const { contentPath } = useViewMode()
defineProps({ title: { type: String, required: true } })
</script>

<template>
  <div class="pending-page">
    <p class="public-eyebrow">COMING SOON</p>
    <h1 class="public-display">{{ title }}模块建设中</h1>
    <p class="pending-page__text">
      「{{ title }}」所在的模块还没有后端实现，这一页只是路由占位。
      现在可以先看博客：已发布的文章都会出现在博客时间线里。
    </p>
    <RouterLink class="public-button primary" :to="contentPath('/blog')">进入博客时间线 <span aria-hidden="true">↗</span></RouterLink>
  </div>
</template>

<style scoped>
.pending-page {
  max-width: 640px;
  padding-block: var(--space-10);
}

.pending-page h1 {
  margin-top: 14px;
  font-size: clamp(30px, 3.6vw, 46px);
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.pending-page__text {
  margin-top: var(--space-6);
  color: var(--text-secondary);
  line-height: 1.85;
}

.pending-page .public-button {
  margin-top: var(--space-7);
}
</style>
