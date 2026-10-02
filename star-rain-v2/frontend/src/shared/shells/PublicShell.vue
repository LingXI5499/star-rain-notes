<script setup>
import { RouterLink } from 'vue-router'

/*
 * 公开站外壳：匿名只读。
 *
 * 硬约束：这个文件里不允许出现任何账号入口——没有链接、没有按钮、没有表单，
 * 也没有指向认证页的路由目标。验收时直接对本文件检索账号入口关键字，命中即视为回归。
 * 服务端另有兜底：AccountSecurityConfig 会让非用户站的账号开通请求直接 403。
 *
 * 顶部只保留内容导航、搜索占位与主题占位；视觉细节等对齐 V1 时再补，
 * 本阶段只要求骨架与「不暴露账号入口」这条硬线。
 */
</script>

<template>
  <div class="entry-shell">
    <header class="site-header public-header">
      <RouterLink to="/blog" class="site-brand">
        <img src="/brand/mark.svg" alt="" width="30" height="30" />
        <span>星雨笔录 <small>公开站</small></span>
      </RouterLink>
      <nav class="public-nav" aria-label="公开站导航">
        <!-- 教程 / 作品 / 关于的页面尚未实现，先占位，避免死链 -->
        <span class="nav-pending" title="教程模块建设中">教程</span>
        <RouterLink to="/blog">博客</RouterLink>
        <span class="nav-pending" title="作品模块建设中">作品</span>
        <span class="nav-pending" title="关于页面建设中">关于</span>
      </nav>
      <div class="public-actions">
        <input class="public-search" type="search" placeholder="搜索（待接入）" aria-label="站内搜索" disabled />
        <button class="text-button" type="button" disabled title="主题切换待接入">主题</button>
      </div>
    </header>
    <main class="entry-main">
      <slot />
    </main>
    <footer class="site-footer">
      <p>星雨笔录 · 公开站</p>
      <p class="muted">备案号占位：待备案完成后填写（例如 粤ICP备00000000号-1）</p>
    </footer>
  </div>
</template>
