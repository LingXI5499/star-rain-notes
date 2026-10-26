<script setup>
/**
 * PublicShell.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../../modules/site/components/PublicHeader.vue、../../modules/site/components/PublicFooter.vue、../../modules/account/components/AccountEntry.vue、../ui/MobilePublicNav.vue、../viewMode、../../styles/base.css。
 */


import PublicHeader from '../../modules/site/components/PublicHeader.vue'
import { ref } from 'vue'
import GlobalReadingDirectory from '../reading/GlobalReadingDirectory.vue'
import PublicFooter from '../../modules/site/components/PublicFooter.vue'
import AccountEntry from '../../modules/account/components/AccountEntry.vue'
import MobilePublicNav from '../ui/MobilePublicNav.vue'
import '../../styles/base.css'
import '../../styles/public-theme.css'

// 前台统一显示账号入口；后台侧栏仅由 meta.console 选择。
const contentRoot = ref(null)
</script>

<template>
  <div class="public-shell">
    <PublicHeader>
      <!-- 统一账号入口 -->
      <template #account>
        <AccountEntry />
      </template>
    </PublicHeader>
    <main ref="contentRoot" class="public-main layout-shell">
      <!-- 页面由 App.vue 以插槽传入（外壳不自己渲染 RouterView） -->
      <slot />
    </main>
    <GlobalReadingDirectory :root="contentRoot" />
    <PublicFooter />
    <MobilePublicNav />
  </div>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.public-shell。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 */
.public-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

/**
 * 样式范围：.public-main。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.public-main {
  flex: 1;
  min-height: calc(100svh - 64px);
  width: 100%;
  padding-block: var(--space-10);
}
/**
 * 响应式或设备条件：(max-width: 768px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 768px) {
  /**
   * 样式范围：.public-shell。
   */
  .public-shell { padding-bottom: calc(84px + env(safe-area-inset-bottom)); }
  /**
   * 样式范围：.public-main。
   */
  .public-main { padding-block: 24px; }
}
</style>
