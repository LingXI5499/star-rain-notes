// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from 'vitest'
import { applyBrandAssets, applyPageMeta, canonicalUrl, caseStudySchema } from './seo'

describe('SEO manager', () => {
  beforeEach(() => {
    document.head.innerHTML = ''
    document.title = ''
  })

  it('uses the canonical production host and removes tracking parameters', () => {
    expect(canonicalUrl('/blog/123?utm_source=test&ref=home&keep=yes'))
      .toBe('https://yulanlin.cn/blog/123?keep=yes')
  })

  it('keeps one copy of every managed tag', () => {
    applyPageMeta({ title: '第一篇', description: '第一篇摘要' }, '/blog/1')
    applyPageMeta({ title: '第二篇', description: '第二篇摘要' }, '/blog/2')
    expect(document.querySelectorAll('meta[name="description"]')).toHaveLength(1)
    expect(document.querySelectorAll('link[rel="canonical"]')).toHaveLength(1)
    expect(document.querySelector('meta[property="og:url"]')?.getAttribute('content')).toBe('https://yulanlin.cn/blog/2')
  })

  it('clears article metadata and old structured data between routes', () => {
    applyPageMeta({ title: '正文', type: 'article', publishedAt: '2026-01-01T00:00:00Z', jsonLd: { '@type': 'Article' } }, '/blog/1')
    expect(document.querySelector('meta[property="article:published_time"]')).not.toBeNull()
    expect(document.querySelectorAll('script[data-seo-schema]')).toHaveLength(1)
    applyPageMeta({ title: '列表' }, '/blog')
    expect(document.querySelector('meta[property="article:published_time"]')).toBeNull()
    expect(document.querySelectorAll('script[data-seo-schema]')).toHaveLength(0)
  })

  it('marks non-indexable routes without producing query canonicals', () => {
    applyPageMeta({ title: '搜索', robots: 'noindex,follow' }, '/search?q=java')
    expect(document.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex,follow')
    expect(document.querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe('https://yulanlin.cn/search')
  })

  it('defaults the share image to the platform-friendly brand PNG', () => {
    applyPageMeta({ title: '首页' }, '/')
    expect(document.querySelector('meta[property="og:image"]')?.getAttribute('content'))
      .toBe('https://yulanlin.cn/brand/og-default.png')
    expect(document.querySelector('meta[name="twitter:image"]')?.getAttribute('content'))
      .toBe('https://yulanlin.cn/brand/og-default.png')
  })

  it('builds CreativeWork JSON-LD for portfolio case studies', () => {
    const schema = caseStudySchema({
      title: '星雨笔录',
      description: '全栈知识站',
      path: '/portfolio/demo',
      demoUrl: 'https://yulanlin.cn',
      techStack: ['Vue 3', 'Spring Boot'],
    })
    expect(schema['@type']).toBe('CreativeWork')
    expect(schema.url).toBe('https://yulanlin.cn')
    expect(schema.keywords).toBe('Vue 3, Spring Boot')
  })
})

describe('Brand assets', () => {
  beforeEach(() => {
    document.head.innerHTML = ''
  })

  it('keeps exactly one primary favicon and one manifest, using the static fallback', () => {
    applyBrandAssets()
    applyBrandAssets()
    expect(document.querySelectorAll('link[rel="icon"][data-brand-icon]')).toHaveLength(1)
    expect(document.querySelector('link[data-brand-icon]')?.getAttribute('href')).toBe('/brand/favicon.svg')
    expect(document.querySelectorAll('link[rel="manifest"]')).toHaveLength(1)
    expect(document.querySelector('link[rel="manifest"]')?.getAttribute('href')).toBe('/brand/site.webmanifest')
  })

  it('overrides the primary favicon with the configured media URL and back', () => {
    applyBrandAssets('https://yulanlin.cn/uploads/media/9.png')
    expect(document.querySelector('link[data-brand-icon]')?.getAttribute('href'))
      .toBe('https://yulanlin.cn/uploads/media/9.png')
    applyBrandAssets(null)
    expect(document.querySelector('link[data-brand-icon]')?.getAttribute('href')).toBe('/brand/favicon.svg')
    expect(document.querySelectorAll('link[data-brand-icon]')).toHaveLength(1)
  })
})
