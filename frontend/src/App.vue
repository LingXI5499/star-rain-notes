<script setup>
/**
 * App.vue：前端应用模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、./shared/shells/PublicShell.vue、./shared/shells/AdminShell.vue、./shared/routeLoadFailure。
 * 主要响应式状态：route、bare、shell。
 */


import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import PublicShell from './shared/shells/PublicShell.vue'
import AdminShell from './shared/shells/AdminShell.vue'
import AccountPrivacyNotice from './modules/account/components/AccountPrivacyNotice.vue'
import { routeLoadFailed, reloadRoute } from './shared/routeLoadFailure'

// 外壳只由用途决定：认证页无外壳，console 页带侧栏，其余页面显示公开导航和账号入口。
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()

/**
 * bare；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const bare = computed(() => Boolean(route.meta.authPage))
/**
 * shell；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const shell = computed(() => {
  if (bare.value) return null
  return route.meta.console ? AdminShell : PublicShell
})
</script>

<template>
  <AccountPrivacyNotice />
  <div v-if="routeLoadFailed" class="route-load-error" role="alert">
    页面资源未能加载，请检查网络后重新加载。未保存的内容请先复制保留。
    <button type="button" @click="reloadRoute">重新加载</button>
  </div>
  <component :is="shell" v-if="shell"><RouterView /></component>
  <RouterView v-else />
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.route-load-error。
 * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.route-load-error { position: fixed; inset: 0 0 auto; z-index: 1000; padding: 16px; background: var(--bg-surface); color: var(--text-primary); border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.route-load-error button。
 */
.route-load-error button { margin-left: 12px; min-height: 44px; }
</style>
