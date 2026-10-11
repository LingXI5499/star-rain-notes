<script setup>
/**
 * PublicFooter.vue：站点模块的视图组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../../../shared/viewMode、../support/siteBranding、../support/publicSocialLinks。
 * 主要响应式状态：navItems。
 * 阅读入口：scrollToTop（滚动目标Top）。
 */


import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { useSiteBranding } from '../support/siteBranding'
import { usePublicSocialLinks } from '../support/publicSocialLinks'

/*
 * 公开站页脚：
 * 品牌区（名称 + 标语 + 一句话）→ EXPLORE 导航 / CONNECT → 版权与返回顶部 → 备案信息。
 *
 *   - 品牌名下的英文标语来自站点配置 footerText，品牌区那句话是固定文案。
 *   - CONNECT 显示作者公开社交链接，
 *     数据来自公开作者资料，整会话只请求一次。
 *   - 域名与备案信息由部署者通过构建环境配置。
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

const publicDomain = import.meta.env.VITE_SITE_DOMAIN || ''
const icpRecord = import.meta.env.VITE_ICP_RECORD || ''
const securityRecord = import.meta.env.VITE_PUBLIC_SECURITY_RECORD || ''
const securityCode = import.meta.env.VITE_PUBLIC_SECURITY_CODE || ''

/* 与顶栏同一顺序：教程 → 博客 → 作品 → 英语 → 留言 → 关于 */
/**
 * nav条目列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const navItems = computed(() => [
  { label: '教程', to: contentPath('/tutorials') },
  { label: '博客', to: contentPath('/blog') },
  { label: '作品', to: contentPath('/portfolio') },
  { label: '英语', to: contentPath('/english') },
  { label: '留言', to: contentPath('/messages') },
  { label: '关于', to: contentPath('/about') },
])

/**
 * 滚动目标Top。
 * 主要协作：window.matchMedia、window.scrollTo。
 */
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

      <div v-if="publicDomain || icpRecord || securityRecord" class="site-footer__icp" aria-label="网站备案信息">
        <span>© {{ year }} {{ branding.siteName }} · {{ publicDomain }}</span>
        <a v-if="icpRecord" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ icpRecord }}</a>
        <a v-if="securityRecord" :href="`https://beian.mps.gov.cn/#/query/webSearch?code=${encodeURIComponent(securityCode)}`" target="_blank" rel="noopener noreferrer">{{ securityRecord }}</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

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

/**
 * 样式范围：.site-footer__inner。
 * 使用网格布局组织内容；列定义为 minmax(0, 1.3fr) minmax(320px, 0.7fr)，间距由 44px clamp(36px, 7vw, 96px) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__inner {
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding: clamp(46px, 7vw, 82px) var(--page-padding-x) 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  gap: 44px clamp(36px, 7vw, 96px);
}

/**
 * 样式范围：.site-footer__lead。
 */
.site-footer__lead { max-width: 580px; }
/**
 * 样式范围：.site-footer__brand。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.site-footer__brand { display: flex; align-items: center; gap: 14px; }
/**
 * 样式范围：.site-footer__mark。
 */
.site-footer__mark { width: 42px; height: 42px; object-fit: contain; }

/**
 * 样式范围：.site-footer__name。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__name {
  font-size: 20px;
  font-weight: 760;
  color: var(--text-primary);
}

/**
 * 样式范围：.site-footer__tagline。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__tagline {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: var(--space-1);
}

/**
 * 样式范围：.site-footer__statement。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__statement { margin-top: 22px; color: var(--text-secondary); line-height: 1.8; }
/**
 * 样式范围：.site-footer__links。
 * 使用网格布局组织内容；列定义为 1fr 0.7fr，间距由 28px 控制。
 */
.site-footer__links { display: grid; grid-template-columns: 1fr 0.7fr; gap: 28px; }
/**
 * 样式范围：.site-footer__label。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__label { margin-bottom: 14px; color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: 0.16em; }

/**
 * 样式范围：.site-footer__nav。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 10px 22px 控制。
 */
.site-footer__nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 22px;
}

/**
 * 样式范围：.site-footer__link。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__link {
  font-size: 14px;
  color: var(--text-secondary);
  transition: color var(--motion-fast) var(--ease-standard), transform var(--motion-fast) var(--ease-standard);
}

/**
 * 样式范围：.site-footer__link:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__link:hover {
  color: var(--primary);
  transform: translateX(2px);
}

/**
 * 样式范围：.site-footer__link--pending, .site-footer__link--pending:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__link--pending,
.site-footer__link--pending:hover {
  color: var(--text-muted);
  cursor: not-allowed;
  transform: none;
}

/**
 * 样式范围：.site-footer__connect。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 10px 控制。
 */
.site-footer__connect { display: grid; gap: 10px; justify-items: start; }
/**
 * 样式范围：.site-footer__connect .site-footer__link span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__connect .site-footer__link span { color: var(--accent); }

/**
 * 样式范围：.site-footer__muted。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__muted { color: var(--text-muted); font-size: 13px; }

/**
 * 样式范围：.site-footer__legal。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.site-footer__legal button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__legal button {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

/**
 * 样式范围：.site-footer__legal button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__legal button:hover { color: var(--primary); }
/**
 * 样式范围：.site-footer__legal。
 */
.site-footer__legal { flex-wrap: wrap; }
/**
 * 样式范围：.site-footer__policies。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__policies { display: flex; flex-wrap: wrap; gap: 16px; color: var(--text-secondary); }
/**
 * 样式范围：.site-footer__policies a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__policies a:hover { color: var(--primary); }

/**
 * 样式范围：.site-footer__icp。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__icp {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  font-size: 12px;
  color: var(--text-muted);
}

/**
 * 样式范围：.site-footer__icp a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__icp a { color: var(--text-muted); }
/**
 * 样式范围：.site-footer__icp a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-footer__icp a:hover { color: var(--primary); }

/**
 * 响应式或设备条件：(max-width: 760px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 760px) {
  /**
   * 样式范围：.site-footer__inner。
   */
  .site-footer__inner { grid-template-columns: 1fr; gap: 34px; }
  /**
   * 样式范围：.site-footer__links。
   */
  .site-footer__links { grid-template-columns: 1fr 1fr; }
  /**
   * 样式范围：.site-footer__legal。
   */
  .site-footer__legal { align-items: flex-start; }
  /**
   * 样式范围：.site-footer__icp。
   */
  .site-footer__icp { align-items: flex-start; flex-direction: column; gap: 8px; }
}

/**
 * 响应式或设备条件：(max-width: 420px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 420px) {
  /**
   * 样式范围：.site-footer__links。
   */
  .site-footer__links { grid-template-columns: 1fr; }
}
</style>
