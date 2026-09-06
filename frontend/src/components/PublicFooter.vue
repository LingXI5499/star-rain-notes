<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicSite } from '@/api/site'
import { setSeoTagline } from '@/lib/seo'

const siteName = ref('星雨笔录')
const footerText = ref('Knowledge · Code · Growth')
const githubUrl = ref<string | null>(null)
const domain = ref('yulanlin.cn')

const navItems = [
  { to: '/tutorials', label: '教程' },
  { to: '/blog', label: '博客' },
  { to: '/portfolio', label: '作品' },
  { to: '/english', label: '英语' },
  { to: '/about', label: '关于' },
]

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

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
      <section class="site-footer__lead" aria-labelledby="footer-brand">
        <div class="site-footer__brand">
          <span class="site-footer__mark" aria-hidden="true">✦</span>
          <div>
            <p id="footer-brand" class="site-footer__name">{{ siteName }}</p>
            <p class="site-footer__tagline">{{ footerText }}</p>
          </div>
        </div>
        <p class="site-footer__statement">在知识、代码与成长之间，留下可以回看的坐标。</p>
      </section>

      <section class="site-footer__links">
        <div>
          <p class="site-footer__label">EXPLORE</p>
          <nav class="site-footer__nav" aria-label="页脚导航">
            <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="site-footer__link">
              {{ item.label }}
            </RouterLink>
          </nav>
        </div>
        <div>
          <p class="site-footer__label">CONNECT</p>
          <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener noreferrer" class="site-footer__github">
            GitHub <span aria-hidden="true">↗</span>
          </a>
          <span v-else class="site-footer__muted">公开链接待配置</span>
        </div>
      </section>

      <div class="site-footer__legal">
        <p>© {{ new Date().getFullYear() }} {{ siteName }} · Built with Java &amp; Vue</p>
        <button type="button" aria-label="返回页面顶部" @click="scrollToTop">
          返回顶部 <span aria-hidden="true">↑</span>
        </button>
      </div>

      <div class="site-footer__icp" aria-label="网站备案信息">
        <span>© {{ new Date().getFullYear() }} {{ siteName }} · {{ domain }}</span>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">晋ICP备2026008281号-1</a>
        <a
          href="https://beian.mps.gov.cn/#/query/webSearch?code=14050002001992"
          target="_blank"
          rel="noopener noreferrer"
        >
          晋公网安备14050002001992号
        </a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: 1px solid var(--border);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--primary) 5%, transparent), transparent 48%),
    var(--bg-surface);
}

.site-footer__inner {
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding: clamp(46px, 7vw, 82px) var(--page-padding-x) 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, .7fr);
  gap: 44px clamp(36px, 7vw, 96px);
}

.site-footer__lead { max-width: 580px; }
.site-footer__brand { display: flex; align-items: center; gap: 14px; }
.site-footer__mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--accent) 55%, var(--border));
  border-radius: 50%;
  color: var(--accent);
  background: var(--bg-page);
}

.site-footer__name {
  font-size: 20px;
  font-weight: 760;
  color: var(--text-primary);
}

.site-footer__tagline {
  font-size: 12px;
  letter-spacing: .12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: var(--space-1);
}

.site-footer__statement { margin-top: 22px; color: var(--text-secondary); line-height: 1.8; }
.site-footer__links { display: grid; grid-template-columns: 1fr .7fr; gap: 28px; }
.site-footer__label { margin-bottom: 14px; color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .16em; }

.site-footer__nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 22px;
}

.site-footer__link {
  font-size: 14px;
  color: var(--text-secondary);
  transition: color var(--motion-fast) var(--ease-standard), transform var(--motion-fast) var(--ease-standard);
}

.site-footer__link:hover {
  color: var(--primary);
  transform: translateX(2px);
}

.site-footer__github { color: var(--text-primary); font-weight: 650; }
.site-footer__github span { color: var(--accent); }
.site-footer__muted { color: var(--text-muted); font-size: 13px; }
.site-footer__legal {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 13px;
}
.site-footer__legal button {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.site-footer__icp {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 760px) {
  .site-footer__inner { grid-template-columns: 1fr; gap: 34px; padding-bottom: 86px; }
  .site-footer__links { grid-template-columns: 1fr 1fr; }
  .site-footer__legal { align-items: flex-start; }
  .site-footer__icp { align-items: flex-start; flex-direction: column; gap: 8px; }
}

@media (max-width: 420px) {
  .site-footer__links { grid-template-columns: 1fr; }
}

.site-footer__icp a {
  color: var(--text-muted);
}

.site-footer__icp a:hover {
  color: var(--primary);
}
</style>
