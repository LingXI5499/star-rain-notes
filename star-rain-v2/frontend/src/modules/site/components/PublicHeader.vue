<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SearchLauncher from './SearchLauncher.vue'
import ThemeControl from './ThemeControl.vue'

/*
 * 公开站顶部导航 —— 对齐 V1 `components/PublicHeader.vue`：
 * 粘性、滚动后收窄并加下边框，品牌 + 内容导航 + 搜索 + 主题。
 *
 * 两处刻意的差别：
 *   1. V1 导航比 V2 多一项语言学习模块；V2 本轮只保留「教程 / 博客 / 作品 / 关于」四项，
 *      并且教程 / 作品 / 关于在对应页面实现前是**不可点的占位**（span + title 提示），不放死链；
 *   2. V1 的品牌名与 logo 来自站点设置接口（appStore.loadBranding），
 *      `star-rain-site` 后端在 V2 还是空模块，因此这里用本地常量与 `/brand/mark.svg`。
 *      接入站点设置后改这里一处即可。
 *
 * 硬约束：本文件不出现任何账号入口字样，也不引用 auth store。
 */
const route = useRoute()
const scrolled = ref(false)
let ticking = false

const siteName = '星雨笔录'
const navItems = [
  { label: '教程', to: null, pending: '教程模块建设中' },
  { label: '博客', to: '/blog' },
  { label: '作品', to: null, pending: '作品模块建设中' },
  { label: '关于', to: null, pending: '关于页面建设中' },
]

function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    scrolled.value = window.scrollY > 16
    ticking = false
  })
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="site-header" :class="{ 'site-header--scrolled': scrolled }">
    <div class="site-header__inner">
      <RouterLink to="/" class="site-header__brand">
        <img class="site-header__logo" src="/brand/mark.svg" alt="" aria-hidden="true" width="26" height="26" />
        <span>{{ siteName }}</span>
      </RouterLink>

      <nav class="site-header__nav" aria-label="主导航">
        <template v-for="item in navItems" :key="item.label">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="site-header__link"
            :class="{ 'site-header__link--active': isActive(item.to) }"
          >{{ item.label }}</RouterLink>
          <span v-else class="site-header__link site-header__link--pending" :title="item.pending" aria-disabled="true">{{ item.label }}</span>
        </template>
      </nav>

      <div class="site-header__actions">
        <SearchLauncher class="site-header__search" />
        <ThemeControl class="site-header__theme" />
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header, 20);
  height: var(--header-height);
  background: color-mix(in srgb, var(--bg-page) 72%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid transparent;
  transition:
    height var(--motion-base) var(--ease-standard),
    background-color var(--motion-base) var(--ease-standard),
    border-color var(--motion-base) var(--ease-standard),
    backdrop-filter var(--motion-base) var(--ease-standard);
}

.site-header--scrolled {
  height: 56px;
  background: color-mix(in srgb, var(--bg-page) 88%, transparent);
  backdrop-filter: blur(16px);
  border-bottom-color: var(--border);
}

.site-header__inner {
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding-inline: var(--page-padding-x);
  height: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.site-header__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.site-header__logo {
  width: 26px;
  height: 26px;
  display: block;
  object-fit: contain;
}

.site-header__nav {
  display: flex;
  gap: var(--space-6);
}

.site-header__link {
  font-size: 15px;
  color: var(--text-secondary);
  padding-block: var(--space-1);
  border-bottom: 2px solid transparent;
}

.site-header__link:hover {
  color: var(--primary);
}

.site-header__link--active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

/* 尚未实现的模块：明确不可点，避免产生死链，也避免“点了没反应”的错觉 */
.site-header__link--pending {
  color: var(--text-muted);
  cursor: not-allowed;
}

.site-header__link--pending:hover {
  color: var(--text-muted);
}

.site-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

@media (max-width: 768px) {
  .site-header {
    height: auto;
    padding-block: 8px;
  }

  .site-header--scrolled {
    height: auto;
  }

  .site-header__inner {
    flex-wrap: wrap;
    gap: var(--space-3);
    row-gap: 6px;
  }

  .site-header__brand {
    font-size: 17px;
  }

  /* 导航换行排在第二行，横向可滚动，不藏入口也不挤爆头部 */
  .site-header__nav {
    order: 3;
    width: 100%;
    gap: var(--space-5);
    overflow-x: auto;
    white-space: nowrap;
    scrollbar-width: none;
  }

  .site-header__nav::-webkit-scrollbar {
    display: none;
  }

  .site-header__link {
    font-size: 14px;
  }
}
</style>
