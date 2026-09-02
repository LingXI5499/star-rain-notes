// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from 'vitest'
import { applyPageMeta, canonicalUrl } from './seo'

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
})
