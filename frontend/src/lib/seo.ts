/** Central SEO manager for both static route metadata and async content. */
const SITE_NAME = '星雨笔录'
const SITE_ORIGIN = 'https://yulanlin.cn'
const DEFAULT_IMAGE = `${SITE_ORIGIN}/og-default.svg`

export const DEFAULT_DESCRIPTION = '建立自己的知识世界'
let siteTagline = DEFAULT_DESCRIPTION
let pageUsesFallbackDescription = true

export type JsonLd = Record<string, unknown> | Array<Record<string, unknown>>
export interface PageMeta {
  title?: string
  description?: string
  robots?: string
  canonicalPath?: string
  type?: 'website' | 'article'
  image?: string | null
  publishedAt?: string | null
  modifiedAt?: string | null
  jsonLd?: JsonLd | null
}

export function setSeoTagline(tagline?: string | null): void {
  siteTagline = tagline?.trim() || DEFAULT_DESCRIPTION
  if (pageUsesFallbackDescription) {
    upsertMeta('name', 'description', siteTagline)
    upsertMeta('property', 'og:description', siteTagline)
    upsertMeta('name', 'twitter:description', siteTagline)
  }
}

function upsertMeta(attr: 'name' | 'property', key: string, content: string): void {
  const selector = `meta[${attr}="${key}"]`
  const matches = Array.from(document.head.querySelectorAll<HTMLMetaElement>(selector))
  const element = matches.shift() ?? document.createElement('meta')
  if (!element.parentNode) {
    element.setAttribute(attr, key)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
  matches.forEach((duplicate) => duplicate.remove())
}

function removeMeta(attr: 'name' | 'property', key: string): void {
  document.head.querySelectorAll(`meta[${attr}="${key}"]`).forEach((element) => element.remove())
}

function upsertCanonical(href: string): void {
  const matches = Array.from(document.head.querySelectorAll<HTMLLinkElement>('link[rel="canonical"]'))
  const element = matches.shift() ?? document.createElement('link')
  if (!element.parentNode) {
    element.rel = 'canonical'
    document.head.appendChild(element)
  }
  element.href = href
  matches.forEach((duplicate) => duplicate.remove())
}

function absoluteUrl(value?: string | null): string {
  if (!value) return DEFAULT_IMAGE
  try { return new URL(value, SITE_ORIGIN).href } catch { return DEFAULT_IMAGE }
}

export function canonicalUrl(path?: string): string {
  const source = path ?? `${window.location.pathname}${window.location.search}`
  const url = new URL(source, SITE_ORIGIN)
  url.protocol = 'https:'
  url.host = 'yulanlin.cn'
  url.hash = ''
  ;['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'ref', 'from'].forEach((key) => url.searchParams.delete(key))
  if (url.pathname === '/search') url.search = ''
  return url.href
}

function applyJsonLd(value?: JsonLd | null): void {
  document.head.querySelectorAll('script[data-seo-schema]').forEach((element) => element.remove())
  if (!value) return
  const script = document.createElement('script')
  script.type = 'application/ld+json'
  script.dataset.seoSchema = 'true'
  script.textContent = JSON.stringify(value).replace(/</g, '\\u003c')
  document.head.appendChild(script)
}

export function applyPageMeta(meta: PageMeta, path?: string): void {
  const rawTitle = meta.title?.trim()
  const title = !rawTitle ? SITE_NAME : rawTitle.includes(SITE_NAME) ? rawTitle : `${rawTitle} | ${SITE_NAME}`
  const explicitDescription = meta.description?.trim()
  pageUsesFallbackDescription = !explicitDescription
  const description = explicitDescription || siteTagline
  const canonical = canonicalUrl(meta.canonicalPath ?? path)
  const image = absoluteUrl(meta.image)
  const type = meta.type ?? 'website'

  document.title = title
  upsertMeta('name', 'description', description)
  upsertMeta('name', 'robots', meta.robots ?? 'index,follow')
  upsertMeta('property', 'og:title', title)
  upsertMeta('property', 'og:description', description)
  upsertMeta('property', 'og:type', type)
  upsertMeta('property', 'og:url', canonical)
  upsertMeta('property', 'og:site_name', SITE_NAME)
  upsertMeta('property', 'og:image', image)
  upsertMeta('name', 'twitter:card', 'summary_large_image')
  upsertMeta('name', 'twitter:title', title)
  upsertMeta('name', 'twitter:description', description)
  upsertMeta('name', 'twitter:image', image)
  if (type === 'article' && meta.publishedAt) upsertMeta('property', 'article:published_time', meta.publishedAt)
  else removeMeta('property', 'article:published_time')
  if (type === 'article' && meta.modifiedAt) upsertMeta('property', 'article:modified_time', meta.modifiedAt)
  else removeMeta('property', 'article:modified_time')
  upsertCanonical(canonical)
  applyJsonLd(meta.jsonLd)
}

export function articleSchema(input: { title: string; description: string; path: string; image?: string | null; publishedAt?: string | null; modifiedAt?: string | null }): Record<string, unknown> {
  const schema: Record<string, unknown> = {
    '@context': 'https://schema.org', '@type': 'Article', headline: input.title,
    description: input.description, mainEntityOfPage: canonicalUrl(input.path), image: absoluteUrl(input.image),
  }
  if (input.publishedAt) schema.datePublished = input.publishedAt
  if (input.modifiedAt) schema.dateModified = input.modifiedAt
  return schema
}
