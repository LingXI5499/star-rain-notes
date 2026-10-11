<script setup>
/**
 * PendingPage.vue：站点模块的页面组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/viewMode。
 * 组件输入：title；默认值与类型见 defineProps。
 */


import { useViewMode } from '../../../shared/viewMode'

/*
 * 未实现模块的占位页（作品 / 关于 / 搜索）。
 *
 * 为什么给它们真路由，而不是继续留成不可点的导航项：
 * 所有者给出的目标形态把 /portfolio、/about、/search 列进了公开树的路由清单，
 * 地址栏直接输入时应该落在一个说明「模块建设中」的页面上，
 * 而不是被兜底重定向弹回首页（那会让人以为地址写错了）。
 * 未实现的导航项仍然渲染成不可点的占位。
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.pending-page。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.pending-page {
  max-width: 640px;
  padding-block: var(--space-10);
}

/**
 * 样式范围：.pending-page h1。
 */
.pending-page h1 {
  margin-top: 14px;
  font-size: clamp(30px, 3.6vw, 46px);
  line-height: 1.15;
  letter-spacing: -0.04em;
}

/**
 * 样式范围：.pending-page__text。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.pending-page__text {
  margin-top: var(--space-6);
  color: var(--text-secondary);
  line-height: 1.85;
}

/**
 * 样式范围：.pending-page .public-button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.pending-page .public-button {
  margin-top: var(--space-7);
}
</style>
