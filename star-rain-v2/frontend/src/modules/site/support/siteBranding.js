import { reactive } from 'vue'
import { getPublicSiteConfig } from '../api/siteApi'

const branding = reactive({ siteName: '星雨笔录', footerText: '', logoUrl: '', faviconUrl: '' })
let pending

export function applySiteBranding(config) {
  branding.siteName = config.siteName || '星雨笔录'
  branding.footerText = config.footerText || ''
  branding.logoUrl = config.logoUrl || ''
  branding.faviconUrl = config.faviconUrl || ''
  let icon = document.querySelector('link[rel="icon"]')
  if (!icon) {
    icon = document.createElement('link')
    icon.rel = 'icon'
    document.head.appendChild(icon)
  }
  icon.href = branding.faviconUrl || '/brand/mark.svg'
}

export function useSiteBranding() {
  if (!pending) pending = getPublicSiteConfig().then(applySiteBranding).catch(() => { pending = null })
  return branding
}
