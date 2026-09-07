import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const brandDir = resolve(import.meta.dirname, '../../public/brand')
const viewsDir = resolve(import.meta.dirname, '../views')

function assertThemeColors(svg: string, label: string) {
  const lower = svg.toLowerCase()
  expect(lower, label).toContain('#0f3d36')
  expect(lower, label).toContain('#c46f4e')
  // Old accent must not remain as paint values
  expect(lower).not.toMatch(/(?:fill|stroke|stop-color)\s*=\s*["']#b86f47["']/i)
  expect(lower).not.toMatch(/stop-color\s*:\s*#b86f47/i)
}

describe('brand SVG visual theme V3', () => {
  it('mark.svg uses Starfield Green and Star Orange, not legacy copper paints', () => {
    const svg = readFileSync(resolve(brandDir, 'mark.svg'), 'utf8')
    assertThemeColors(svg, 'mark.svg')
  })

  it('favicon.svg uses Starfield Green and Star Orange, not legacy copper paints', () => {
    const svg = readFileSync(resolve(brandDir, 'favicon.svg'), 'utf8')
    assertThemeColors(svg, 'favicon.svg')
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
