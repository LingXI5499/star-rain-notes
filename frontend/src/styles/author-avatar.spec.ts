import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const brandDir = resolve(import.meta.dirname, '../../public/brand')
const aboutView = resolve(import.meta.dirname, '../views/about/AboutView.vue')

describe('about author avatar', () => {
  it('ships a non-empty brand author avatar asset', () => {
    const webp = resolve(brandDir, 'author-avatar.webp')
    const jpg = resolve(brandDir, 'author-avatar.jpg')
    let path = webp
    try {
      statSync(webp)
    } catch {
      path = jpg
      statSync(jpg)
    }
    expect(statSync(path).size).toBeGreaterThan(8_000)
  })

  it('AboutView uses the soft-blend brand avatar, not BrandMark tombstone fallback', () => {
    const source = readFileSync(aboutView, 'utf8')
    expect(source).toContain('/brand/author-avatar.webp')
    expect(source).toContain('about-hero__portrait--soft')
    expect(source).not.toMatch(/about-hero__portrait[^>]*>[\s\S]*BrandMark/)
  })
})
