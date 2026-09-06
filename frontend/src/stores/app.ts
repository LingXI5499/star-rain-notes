import { defineStore } from 'pinia'
import { fetchPublicSite } from '@/api/site'
import { applyBrandAssets } from '@/lib/seo'

/**
 * Application-level store: site identity + runtime brand assets.
 * `/public/site` configuration (logo/favicon media) may override the static
 * brand fallbacks; loading failures keep the static `/brand/*` assets.
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    name: '星雨笔录',
    englishName: 'Star Rain Notes',
    tagline: '建立自己的知识世界',
    logoUrl: null as string | null,
    faviconUrl: null as string | null,
    brandingLoaded: false,
  }),
  actions: {
    async loadBranding() {
      if (this.brandingLoaded) return
      this.brandingLoaded = true
      try {
        const site = await fetchPublicSite()
        this.logoUrl = site.logoUrl
        this.faviconUrl = site.faviconUrl
        if (site.siteName) this.name = site.siteName
        applyBrandAssets(site.faviconUrl)
      } catch {
        // Keep the static fallback brand when the public config is unavailable.
        applyBrandAssets(null)
      }
    },
  },
})
