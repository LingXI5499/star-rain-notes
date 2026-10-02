<script setup>
import PublicHeader from '../../modules/site/components/PublicHeader.vue'
import PublicFooter from '../../modules/site/components/PublicFooter.vue'
import '../../styles/base.css'
import '../../styles/public-theme.css'

/*
 * 公开站外壳：匿名只读。
 *
 * 硬约束：这个外壳以及它渲染的全部公开站页面里不允许出现任何账号入口——
 * 没有链接、没有按钮、没有表单，也不引用 auth store；公开站是匿名只读的。
 * 验收时对公开站源码检索账号入口关键字（命令见 `docs/开发文档/公开站与用户中心验收.md` §4），
 * 命中即视为回归。服务端另有兜底：非用户站的账号开通请求会被 AccountSecurityConfig 直接 403。
 *
 * 样式从 V1 移植：
 *   - `styles/base.css`   —— V1 的公开站基础排版（作用域收敛到 `.public-shell`）
 *   - `styles/public-theme.css` —— V1 的「星轨纸境」主题层（同样收敛到 `.public-shell`）
 * 两份 CSS 在这里静态引入，PublicShell 又被 App.vue 静态引入，因此它们随主包加载，
 * 公开站首屏不会出现样式闪烁。
 *
 * 结构对齐 V1 `layouts/BaseLayout.vue`：Header → main（layout-shell）→ Footer。
 */
</script>

<template>
  <div class="public-shell">
    <PublicHeader />
    <main class="public-main layout-shell">
      <!-- 页面由 App.vue 以插槽传入（与 AdminShell / UserShell 同一约定，外壳不自己渲染 RouterView） -->
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
