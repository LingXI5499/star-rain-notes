<script setup>
/**
 * WorkLivePage.vue：作品模块的页面组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../api/portfolioApi、../../../shared/viewMode、../../../shared/http。
 * 主要响应式状态：route、work、error。
 */


import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicWork } from '../api/portfolioApi'
import { useViewMode } from '../../../shared/viewMode'
import { errorMessage } from '../../../shared/http'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), work = ref(null), error = ref('')
const { contentPath } = useViewMode()
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => { try { work.value = await getPublicWork(route.params.slug); if (!work.value.prototypeUrl) error.value = '这个作品暂无可体验的静态原型。' } catch (cause) { error.value = errorMessage(cause) } })
</script>
<template><section class="work-live"><header><RouterLink :to="contentPath(`/portfolio/${route.params.slug}`)">← 返回作品</RouterLink><h1>{{ work?.title || '作品原型' }}</h1><span>静态原型体验</span></header><p v-if="error" role="alert">{{ error }}</p><iframe v-else-if="work?.prototypeUrl" :src="work.prototypeUrl" :title="`${work.title}静态原型`" sandbox="allow-scripts" referrerpolicy="no-referrer" /><p v-else>正在加载原型…</p></section></template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */
/**
 * 样式范围：.work-live。
 */
.work-live{padding:28px 0 50px}
  /**
   * 样式范围：.work-live header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-live header{display:flex;align-items:center;gap:24px;flex-wrap:wrap;margin-bottom:22px}
  /**
   * 样式范围：.work-live h1。
   */
  .work-live h1{margin:0;font-size:24px}
  /**
   * 样式范围：.work-live a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-live a{color:var(--primary)}
  /**
   * 样式范围：.work-live span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-live span{margin-left:auto;font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.work-live iframe。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-live iframe{width:100%;height:calc(100vh - 230px);min-height:500px;border:1px solid var(--border);border-radius:16px;background:white}
  /**
   * 样式范围：.work-live p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-live p{padding:50px;text-align:center;color:var(--text-muted)}</style>
