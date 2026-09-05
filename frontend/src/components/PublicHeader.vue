<script setup lang="ts">
import { onBeforeUnmount, onMounted, onBeforeMount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import GlobalSearch from './search/GlobalSearch.vue'
import ThemeControl from './ui/ThemeControl.vue'

const route = useRoute()
const mobileMenuOpen = ref(false)
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

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMobileMenu()
}

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    scrolled.value = window.scrollY > 16
    ticking = false
  })
}

watch(() => route.fullPath, closeMobileMenu)
onBeforeMount(onScroll)
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <header class="site-header" :class="{ 'site-header--scrolled': scrolled }">
    <div class="site-header__inner">
      <RouterLink to="/" class="site-header__brand">星雨笔录</RouterLink>

      <button
        class="site-header__menu-toggle"
        type="button"
        :aria-expanded="mobileMenuOpen"
        aria-controls="site-mobile-menu"
        :aria-label="mobileMenuOpen ? '关闭主导航' : '打开主导航'"
        @click="mobileMenuOpen = !mobileMenuOpen"
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
          <path v-if="mobileMenuOpen" d="M18 6 6 18M6 6l12 12" />
          <path v-else d="M4 7h16M4 12h16M4 17h16" />
        </svg>
      </button>

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

    <Transition name="mobile-menu">
      <nav v-if="mobileMenuOpen" id="site-mobile-menu" class="site-header__mobile-menu" aria-label="移动端主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="site-header__mobile-link"
          :class="{ 'site-header__mobile-link--active': isActive(item.to) }"
          @click="closeMobileMenu"
        >
          {{ item.label }}
        </RouterLink>
        <ThemeControl class="site-header__mobile-theme-control" />
      </nav>
    </Transition>
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
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.site-header__menu-toggle,
.site-header__mobile-menu {
  display: none;
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

  .site-header__menu-toggle {
    order: 4;
    display: inline-grid;
    place-items: center;
    width: 34px;
    height: 34px;
    color: var(--text-secondary);
    background: none;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    cursor: pointer;
  }

  .site-header__menu-toggle:hover,
  .site-header__menu-toggle[aria-expanded='true'] {
    color: var(--primary);
    border-color: color-mix(in srgb, var(--primary) 45%, var(--border));
  }

  .site-header__nav {
    display: none;
  }

  .site-header__search {
    margin-left: auto;
  }

  .site-header__mobile-theme-control {
    grid-column: 1 / -1;
    display: flex;
    justify-content: center;
    padding: var(--space-2);
  }

  .site-header__mobile-menu {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-2);
    padding: var(--space-4) var(--page-padding-x) var(--space-5);
    background: color-mix(in srgb, var(--bg-page) 96%, transparent);
    backdrop-filter: blur(16px);
    border-bottom: 1px solid var(--border);
    box-shadow: var(--shadow-sm);
  }

  .site-header__mobile-link {
    min-height: 42px;
    display: flex;
    align-items: center;
    padding-inline: var(--space-4);
    color: var(--text-secondary);
    background: var(--bg-elevated);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
  }

  .site-header__mobile-link--active {
    color: var(--primary);
    border-color: color-mix(in srgb, var(--primary) 45%, var(--border));
  }

  .mobile-menu-enter-active,
  .mobile-menu-leave-active {
    transition: opacity 160ms ease, transform 160ms ease;
  }

  .mobile-menu-enter-from,
  .mobile-menu-leave-to {
    opacity: 0;
    transform: translateY(-8px);
  }
}
</style>
