import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const brandDir = resolve(import.meta.dirname, '../../public/brand')
const viewsDir = resolve(import.meta.dirname, '../views')

const spaShell = resolve(import.meta.dirname, '../../../backend/src/main/resources/seo/spa-shell.html')
const indexHtml = resolve(import.meta.dirname, '../../index.html')
const portfolioDetail = resolve(viewsDir, 'portfolio/PortfolioDetailView.vue')
const siteSettings = resolve(viewsDir, 'admin/SiteSettingsView.vue')
const aboutAdmin = resolve(viewsDir, 'admin/AboutEditView.vue')
const portfolioAdmin = resolve(viewsDir, 'admin/PortfolioEditView.vue')

describe('brand assets visual theme V3', () => {
  it('ships the new raster brand mark', () => {
    expect(statSync(resolve(brandDir, 'mark.png')).size).toBeGreaterThan(500)
    expect(statSync(resolve(brandDir, 'mark.svg')).size).toBeGreaterThan(200)
    expect(readFileSync(resolve(brandDir, 'mark.svg'), 'utf8')).toContain('#E07A4C')
    expect(statSync(resolve(brandDir, 'logo-wordmark.png')).size).toBeGreaterThan(1000)
  })

  it('ships raster search-engine favicon assets from the new brand mark', () => {
    for (const name of ['favicon.png', 'favicon.ico', 'icon-192.png', 'icon-512.png', 'apple-touch-icon.png'] as const) {
      const file = resolve(brandDir, name)
      expect(statSync(file).size, name).toBeGreaterThan(200)
    }
  })

  it('points the SPA and crawler shells at the PNG favicon, not the old SVG', () => {
    const index = readFileSync(indexHtml, 'utf8')
    const shell = readFileSync(spaShell, 'utf8')
    expect(index).toContain('href="/brand/favicon.png"')
    expect(index).not.toContain('favicon.svg')
    expect(shell).toContain('href="/brand/favicon.png"')
    expect(shell).not.toContain('favicon.svg')
  })
})

describe('theme hero page wiring', () => {
  it('Home / English / About use ThemeHero bleed stages with pinned URLs', () => {
    const home = readFileSync(resolve(viewsDir, 'HomeView.vue'), 'utf8')
    const english = readFileSync(resolve(viewsDir, 'english/EnglishView.vue'), 'utf8')
    const about = readFileSync(resolve(viewsDir, 'about/AboutView.vue'), 'utf8')
    expect(home).toContain('/brand/themes/home-hero.webp')
    expect(home).toContain('ThemeHero')
    expect(home).not.toContain('home-hero__visual')
    expect(english).toContain('/brand/themes/english-hero.webp')
    expect(english).toContain('ThemeHero')
    expect(english).not.toContain('StarfallScene')
    expect(english).not.toContain('english-hero__visual')
    expect(about).toContain('/brand/themes/about-hero.webp')
    expect(about).not.toContain('StarfallScene')
    expect(about).not.toContain('compact')
  })
})

describe('case study README outline shell', () => {
  it('keeps the gallery and generic CTA without repeating cover, meta, or stack blocks', () => {
    const source = readFileSync(portfolioDetail, 'utf8')
    expect(source).toContain('ProjectGallery')
    expect(source).toContain('项目仍在持续迭代。')
    expect(source).not.toContain('星雨笔录仍在')
    expect(source).not.toContain('class="case-cover"')
    expect(source).not.toContain('class="case-meta"')
    expect(source).not.toContain('class="case-tech"')
  })

  it('keeps the five-chapter authoring prompt and hides deprecated profile media controls', () => {
    const portfolioSource = readFileSync(portfolioAdmin, 'utf8')
    const aboutTemplate = readFileSync(aboutAdmin, 'utf8').split('<template>')[1] ?? ''
    expect(portfolioSource).toContain('插入 README 大纲')
    expect(portfolioSource).toContain('背景 → 页面导览 → 用户流程 → 功能清单 → 技术架构')
    expect(aboutTemplate).not.toContain('头像')
    expect(aboutTemplate).not.toContain('简历媒体 ID')
  })
})

describe('site brand media guidance', () => {
  it('explains that custom media overrides the built-in logo and favicon', () => {
    const source = readFileSync(siteSettings, 'utf8')
    expect(source).toContain('留空 = 使用默认品牌标记')
    expect(source).toContain('留空 = 使用默认品牌图标')
    expect(source).toContain('移除自定义图片并保存后')
    expect(source).toContain('/brand/favicon.png')
  })
})
