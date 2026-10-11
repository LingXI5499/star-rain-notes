/**
 * siteBranding.js：站点模块的业务支持工具。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：vue、../api/siteApi。
 * 主要响应式状态：branding。
 * 阅读入口：applySiteBranding（应用站点Branding）；useSiteBranding（use站点Branding的处理函数）。
 */

import { reactive } from 'vue'
import { getPublicSiteConfig } from '../api/siteApi'

/**
 * branding；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const branding = reactive({ siteName: '星雨笔录', tagline: '', homeIntro: '', footerText: '', logoUrl: '', faviconUrl: '' })
let pending

export
  /**
   * 应用站点Branding。
   * 主要协作：document.querySelector、document.createElement、document.head.appendChild。
   * @param config 配置；默认值、解构字段与可选参数见函数签名。
   */
  function applySiteBranding(config) {
  branding.siteName = config.siteName || '星雨笔录'
  branding.tagline = config.tagline || ''
  branding.homeIntro = config.homeIntro || ''
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

export
  /**
   * use站点Branding的处理函数。
   * 主要协作：.catch、.then、getPublicSiteConfig。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useSiteBranding() {
  if (!pending) pending = getPublicSiteConfig().then(applySiteBranding).catch(() => { pending = null })
  return branding
}
