<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import GlobalSearch from './search/GlobalSearch.vue'

const theme = useThemeStore()
const route = useRoute()

const navItems = [
  { to: '/tutorials', label: '教程' },
  { to: '/blog', label: '博客' },
  { to: '/portfolio', label: '作品' },
  { to: '/english', label: '英语' },
  { to: '/about', label: '关于' },
]

const themeLabels: Record<ThemeMode, string> = {
  light: '浅色',
  dark: '深色',
  system: '跟随系统',
}

function cycleTheme() {
  const next: ThemeMode = theme.mode === 'light' ? 'dark' : theme.mode === 'dark' ? 'system' : 'light'
  theme.setMode(next)
}

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner">
      <RouterLink to="/" class="site-header__brand">星雨笔录</RouterLink>

      <nav class="site-header__nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="site-header__link"
          :class="{ 'site-header__link--active': isActive(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <GlobalSearch class="site-header__search" />

      <button class="site-header__action" type="button" title="主题" @click="cycleTheme">
        主题 · {{ themeLabels[theme.mode] }}
      </button>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header, 20);
  height: var(--header-height);
  background: color-mix(in srgb, var(--bg-page) 82%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
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
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
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

/* The search widget pushes itself and the theme button to the right. */
.site-header__search {
  margin-left: auto;
}

.site-header__action {
  font-size: 14px;
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  white-space: nowrap;
}

.site-header__action:hover {
  color: var(--primary);
}

@media (max-width: 768px) {
  .site-header {
    height: var(--header-height-mobile);
  }

  .site-header__nav {
    display: none;
  }
}
</style>
