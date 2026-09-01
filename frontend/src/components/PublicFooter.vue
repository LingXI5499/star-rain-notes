<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicSite } from '@/api/site'
import { setSeoTagline } from '@/lib/seo'

const siteName = ref('星雨笔录')
const footerText = ref('Personal Knowledge System')
const githubUrl = ref<string | null>(null)
const domain = ref(window.location.host || 'localhost:5173')

const navItems = [
  { to: '/tutorials', label: '教程' },
  { to: '/blog', label: '博客' },
  { to: '/portfolio', label: '作品' },
  { to: '/english', label: '英语' },
  { to: '/about', label: '关于' },
]

onMounted(async () => {
  try {
    const site = await fetchPublicSite()
    setSeoTagline(site.tagline)
    siteName.value = site.siteName
    footerText.value = site.footerText ?? 'Personal Knowledge System'
    githubUrl.value = site.githubUrl
    if (site.siteUrl) {
      domain.value = site.siteUrl.replace(/^https?:\/\//, '').replace(/\/+$/, '')
    }
  } catch {
    // fall back to defaults
  }
})
</script>

<template>
  <footer class="site-footer">
    <div class="site-footer__inner">
      <div class="site-footer__brand">
        <p class="site-footer__name">{{ siteName }}</p>
        <p class="site-footer__tagline">{{ footerText }}</p>
      </div>

      <nav class="site-footer__nav" aria-label="页脚导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="site-footer__link">
          {{ item.label }}
        </RouterLink>
        <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener noreferrer" class="site-footer__link">
          GitHub
        </a>
      </nav>

      <p class="site-footer__meta">© {{ new Date().getFullYear() }} · Built with Java &amp; Vue</p>

      <div class="site-footer__identity">
        <span>© {{ new Date().getFullYear() }} {{ siteName }} · {{ domain }}</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: 1px solid var(--border);
  background: var(--bg-surface);
}

.site-footer__inner {
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding: var(--space-10) var(--page-padding-x);
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.site-footer__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.site-footer__tagline {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: var(--space-1);
}

.site-footer__nav {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
}

.site-footer__link {
  font-size: 14px;
  color: var(--text-secondary);
}

.site-footer__link:hover {
  color: var(--primary);
}

.site-footer__meta {
  font-size: 13px;
  color: var(--text-muted);
}

.site-footer__identity {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  font-size: 12px;
  color: var(--text-muted);
}

</style>
