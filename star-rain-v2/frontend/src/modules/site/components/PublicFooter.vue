<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { useSiteBranding } from '../support/siteBranding'
import { usePublicSocialLinks } from '../support/publicSocialLinks'

/*
 * 公开站页脚 —— 对齐 V1 `components/PublicFooter.vue` 的信息架构：
 * 品牌区（名称 + 标语 + 一句话）→ EXPLORE 导航 / CONNECT → 版权与返回顶部 → 备案信息。
 *
 * 与 V1 的对应关系：
 *   - 品牌名下的英文标语来自站点配置 `footerText`（V1 同一字段），品牌区那句话是固定文案；
 *     两者在早期版本里被写反过，这里按住 V1 的位置摆回来。
 *   - CONNECT 显示作者公开社交链接（V1 取的是公开站点配置里的 GitHub），
 *     数据来自公开作者资料，整会话只请求一次。
 *   - 备案信息是真实备案号，不是占位文案。
 *
 * 导航目标与顶栏同一口径，按当前路径树生成（公开树 /blog、账号树 /useradmin/blog）：
 * 页脚是同一套内容在两条树上复用，链接必须跟着树走，否则页脚会把账号外壳点掉。
 *
 * 硬约束：本文件不出现任何账号入口字样。
 */
const { contentPath } = useViewMode()
const branding = useSiteBranding()
const social = usePublicSocialLinks()
const year = new Date().getFullYear()

/* 生产公开域名。V2 站点配置尚未承载该字段，先作为部署身份常量放在这里。 */
const publicDomain = 'yulanlin.cn'

/* 与顶栏同一顺序：教程 → 博客 → 作品 → 英语 → 留言 → 关于 */
const navItems = computed(() => [
  { label: '教程', to: contentPath('/tutorials') },
  { label: '博客', to: contentPath('/blog') },
  { label: '作品', to: contentPath('/portfolio') },
  { label: '英语', to: contentPath('/english') },
  { label: '留言', to: contentPath('/messages') },
  { label: '关于', to: contentPath('/about') },
])

function scrollToTop() {
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}
</script>

<template>
  <footer class="site-footer">
    <div class="site-footer__inner">
      <section class="site-footer__lead" aria-labelledby="footer-brand">
        <div class="site-footer__brand">
          <img class="site-footer__mark" :src="branding.logoUrl || '/brand/mark.svg'" alt="" aria-hidden="true" width="42" height="42" />
          <div>
            <p id="footer-brand" class="site-footer__name">{{ branding.siteName }}</p>
            <p class="site-footer__tagline">{{ branding.footerText || 'Knowledge · Code · Growth' }}</p>
          </div>
        </div>
        <p class="site-footer__statement">在知识、代码与成长之间，留下可以回看的坐标。</p>
      </section>

      <section class="site-footer__links">
        <div>
          <p class="site-footer__label">EXPLORE</p>
          <nav class="site-footer__nav" aria-label="页脚导航">
            <template v-for="item in navItems" :key="item.label">
              <RouterLink v-if="item.to" :to="item.to" class="site-footer__link">{{ item.label }}</RouterLink>
              <span v-else class="site-footer__link site-footer__link--pending" :title="item.pending" aria-disabled="true">{{ item.label }}</span>
            </template>
          </nav>
        </div>
        <div>
          <p class="site-footer__label">CONNECT</p>
          <nav v-if="social.links.length" class="site-footer__connect" aria-label="作者公开链接">
            <a v-for="link in social.links" :key="link.id" class="site-footer__link"
               :href="link.url" :target="link.url.startsWith('mailto:') ? undefined : '_blank'"
               rel="noopener noreferrer">{{ link.label }} <span aria-hidden="true">↗</span></a>
          </nav>
          <span v-else class="site-footer__muted">公开链接待配置</span>
        </div>
      </section>

      <div class="site-footer__legal">
        <p>© {{ year }} {{ branding.siteName }} · Built with Java &amp; Vue</p>
        <nav class="site-footer__policies" aria-label="协议与政策">
          <RouterLink :to="contentPath('/terms')">用户协议</RouterLink>
          <RouterLink :to="contentPath('/privacy')">隐私政策</RouterLink>
        </nav>
        <button type="button" aria-label="返回页面顶部" @click="scrollToTop">返回顶部 <span aria-hidden="true">↑</span></button>
      </div>

      <div class="site-footer__icp" aria-label="网站备案信息">
        <span>© {{ year }} {{ branding.siteName }} · {{ publicDomain }}</span>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">晋ICP备2026008281号-1</a>
        <a href="https://beian.mps.gov.cn/#/query/webSearch?code=14050002001992" target="_blank" rel="noopener noreferrer">晋公网安备14050002001992号</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
/*
 * 上一版外壳骨架里那组 `.site-footer { padding / font-size }` 与 `.site-footer p { margin }`
 * 已随域名方案（styles/entry.css）一并删除，这里不需要再写覆盖规则把排版拉回来。
 * `.public-shell *` 的 margin/padding 归零仍在 styles/base.css 里，页脚排版由下面自己声明。
 */
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
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  gap: 44px clamp(36px, 7vw, 96px);
}

.site-footer__lead { max-width: 580px; }
.site-footer__brand { display: flex; align-items: center; gap: 14px; }
.site-footer__mark { width: 42px; height: 42px; object-fit: contain; }

.site-footer__name {
  font-size: 20px;
  font-weight: 760;
  color: var(--text-primary);
}

.site-footer__tagline {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: var(--space-1);
}

.site-footer__statement { margin-top: 22px; color: var(--text-secondary); line-height: 1.8; }
.site-footer__links { display: grid; grid-template-columns: 1fr 0.7fr; gap: 28px; }
.site-footer__label { margin-bottom: 14px; color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: 0.16em; }

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

.site-footer__link--pending,
.site-footer__link--pending:hover {
  color: var(--text-muted);
  cursor: not-allowed;
  transform: none;
}

.site-footer__connect { display: grid; gap: 10px; justify-items: start; }
.site-footer__connect .site-footer__link span { color: var(--accent); }

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

.site-footer__legal button:hover { color: var(--primary); }
.site-footer__legal { flex-wrap: wrap; }
.site-footer__policies { display: flex; flex-wrap: wrap; gap: 16px; color: var(--text-secondary); }
.site-footer__policies a:hover { color: var(--primary); }

.site-footer__icp {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  font-size: 12px;
  color: var(--text-muted);
}

.site-footer__icp a { color: var(--text-muted); }
.site-footer__icp a:hover { color: var(--primary); }

@media (max-width: 760px) {
  .site-footer__inner { grid-template-columns: 1fr; gap: 34px; }
  .site-footer__links { grid-template-columns: 1fr 1fr; }
  .site-footer__legal { align-items: flex-start; }
  .site-footer__icp { align-items: flex-start; flex-direction: column; gap: 8px; }
}

@media (max-width: 420px) {
  .site-footer__links { grid-template-columns: 1fr; }
}
</style>
