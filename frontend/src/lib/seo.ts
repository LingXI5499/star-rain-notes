/**
 * Basic SEO for the CSR SPA (01 §10, 06 §13): per-route title, meta
 * description, canonical, Open Graph and robots. The router afterEach hook
 * applies static route metadata; detail views call `applyPageMeta` again
 * once their content has loaded so titles/descriptions reflect the content.
 */

const SITE_NAME = '星雨笔录 · Star Rain Notes'

export const DEFAULT_DESCRIPTION =
  '星雨笔录 · Star Rain Notes — 个人知识系统：系统整理技术，记录思考，用真实项目验证学习与成长。'

export interface PageMeta {
  title?: string
  description?: string
  robots?: string
}

function upsertMeta(attr: 'name' | 'property', key: string, content: string): void {
  const selector = `meta[${attr}="${key}"]`
  let el = document.head.querySelector<HTMLMetaElement>(selector)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function upsertLink(rel: string, href: string): void {
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', rel)
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

export function applyPageMeta(meta: PageMeta, path?: string): void {
  const title = meta.title ? `${meta.title} · ${SITE_NAME}` : SITE_NAME
  const description = meta.description ?? DEFAULT_DESCRIPTION
  const url = new URL(path ?? window.location.pathname, window.location.origin)

  document.title = title
  upsertMeta('name', 'description', description)
  upsertMeta('name', 'robots', meta.robots ?? 'index,follow')
  upsertMeta('property', 'og:title', title)
  upsertMeta('property', 'og:description', description)
  upsertMeta('property', 'og:type', 'website')
  upsertMeta('property', 'og:url', url.href)
  upsertMeta('property', 'og:site_name', SITE_NAME)
  upsertLink('canonical', url.href)
}
