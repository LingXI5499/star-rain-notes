<script setup>
import PublicHeader from '../../modules/site/components/PublicHeader.vue'
import PublicFooter from '../../modules/site/components/PublicFooter.vue'
import AccountEntry from '../../modules/account/components/AccountEntry.vue'
import { useViewMode } from '../viewMode'
import '../../styles/base.css'
import '../../styles/public-theme.css'

/*
 * 前台外壳 —— 两棵路径树共用的**唯一**前台外壳（原来的 UserShell 已删除）。
 *
 * 硬约束：前台永远没有左侧栏。账号相关的东西只在右上角，且只在账号模式下存在：
 *   /            公开树：右上角没有任何账号入口（AccountEntry 根本不挂载）
 *   /useradmin   账号树：右上角是账号区（未登录只有一个「登录」按钮，已登录是用户标识菜单）
 *
 * 公开模式不挂载账号区是**渲染策略**，不是 Cookie 隔离：同域名下浏览器必然带着会话 Cookie，
 * 真要按路径隔离 Cookie，`/api/**` 就都拿不到会话了。因此「公开站永远匿名」由两件事一起保证：
 *   1. 公开树不渲染账号 UI（这里）；
 *   2. 公开树也不去取当前会话（router/index.js 的守卫）。
 *
 * 样式从 V1 移植：
 *   - `styles/base.css`        —— V1 的公开站基础排版（作用域收敛到 `.public-shell`）
 *   - `styles/public-theme.css` —— V1 的「星轨纸境」主题层（同样收敛到 `.public-shell`）
 * 两份 CSS 在这里静态引入，PublicShell 又被 App.vue 静态引入，因此它们随主包加载，
 * 前台首屏不会出现样式闪烁。
 *
 * 结构对齐 V1 `layouts/BaseLayout.vue`：Header → main（layout-shell）→ Footer。
 */
const { isAccount } = useViewMode()
</script>

<template>
  <div class="public-shell">
    <PublicHeader>
      <!-- 账号区只在账号模式挂载；公开模式下这里什么都不渲染 -->
      <template #account>
        <AccountEntry v-if="isAccount" />
      </template>
    </PublicHeader>
    <main class="public-main layout-shell">
      <!-- 页面由 App.vue 以插槽传入（外壳不自己渲染 RouterView） -->
      <slot />
    </main>
    <PublicFooter />
  </div>
</template>

<style scoped>
.public-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

.public-main {
  flex: 1;
  width: 100%;
  padding-block: var(--space-10);
}
</style>
