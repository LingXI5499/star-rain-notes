<script setup lang="ts">
import { onBeforeUnmount, onMounted, onBeforeMount, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/app'
import BrandMark from './brand/BrandMark.vue'
import GlobalSearch from './search/GlobalSearch.vue'
import ThemeControl from './ui/ThemeControl.vue'

const route = useRoute()
const appStore = useAppStore()
const { name, logoUrl } = storeToRefs(appStore)
const scrolled = ref(false)
let ticking = false

const navItems = [
  { to: '/tutorials', label: '教程' },
  { to: '/blog', label: '博客' },
  { to: '/portfolio', label: '作品' },
  { to: '/english', label: '英语' },
  { to: '/about', label: '关于' },
]

function isActive(path: string): boolean {
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

onBeforeMount(onScroll)
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  void appStore.loadBranding()
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <header class="site-header" :class="{ 'site-header--scrolled': scrolled }">
    <div class="site-header__inner">
      <RouterLink to="/" class="site-header__brand">
        <img v-if="logoUrl" class="site-header__logo" :src="logoUrl" alt="" aria-hidden="true" />
        <BrandMark v-else :size="26" decorative />
        <span>{{ name }}</span>
      </RouterLink>

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

      <ThemeControl class="site-header__theme" />
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
  transition: height var(--motion-base) var(--ease-standard), background-color var(--motion-base) var(--ease-standard), border-color var(--motion-base) var(--ease-standard), backdrop-filter var(--motion-base) var(--ease-standard);
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

  .site-header__inner {
    gap: var(--space-3);
  }

  .site-header__nav {
    display: none;
  }

  .site-header__search {
    margin-left: auto;
  }

  .site-header__brand { font-size: 17px; }
  .site-header__theme { display: inline-flex; }
}
</style>
