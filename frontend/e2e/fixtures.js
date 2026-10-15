/** In-memory fake services reset with each browser context; no request reaches a real API. */
import { test as base, expect } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import { resolve, sep } from 'node:path'

export const post = (slug = 'a') => ({ id: slug === 'a' ? 1 : 2, slug, title: `Article ${slug.toUpperCase()}`,
  summary: '', status: 'DRAFT', tags: [], bodyMarkdown: '# Heading\n\nOriginal body\n', contentMediaAssetIds: [] })
export const work = slug => ({ id: slug.charCodeAt(0), slug, title: `Work ${slug.toUpperCase()}`,
  summary: 'Controlled fixture', featured: true, hasSections: true, tags: [], links: [],
  sections: [{ id: 1, title: 'Introduction', visible: true, sectionType: 'MARKDOWN', content: 'Fixture '.repeat(1000) },
    { id: 2, title: 'Details', visible: true, sectionType: 'MARKDOWN', content: 'Details' }] })
const user = { id: 1, username: 'fixture_admin', nickname: 'Fixture', roles: ['SUPER_ADMIN'],
  permissions: ['blog:read-admin', 'blog:post-create', 'blog:post-edit', 'blog:taxonomy-manage',
    'blog:create', 'blog:edit', 'portfolio:read-admin', 'portfolio:edit', 'media:upload', 'tutorial:read-admin'] }

export const test = base.extend({
  fixture: [async ({ page }, use) => {
    const browserErrors = []
    page.on('pageerror', error => { browserErrors.push(error.message); console.error('Browser application error:', error.message) })
    const state = { signedIn: true, user, article: post(), savedBodies: [], delayBody: null, blogDelays: {}, workDelays: {},
      tags: [{ id: 1, name: 'Unused', slug: 'unused', status: 'ENABLED', postCount: 0 }], deletedTags: [], bodyFailure: false,
      writing:{id:2,title:'Writing',bodyMarkdown:'English body\n',translationZhMarkdown:'中文正文\n',rowVersion:1,state:'DRAFT',visibility:'PRIVATE',keywords:[]},writingSaves:[] }
    await page.route('https://**/*', async route => {
      const url = new URL(route.request().url()), start = url.pathname.indexOf('/dist/')
      if (!url.pathname.includes('vditor') || start < 0) return route.abort()
      const root = resolve('node_modules/vditor/dist'), path = resolve(root, decodeURIComponent(url.pathname.slice(start + 6)))
      if (!path.startsWith(root + sep)) return route.abort()
      try {
        await route.fulfill({ body: await readFile(path), contentType: path.endsWith('.js') ? 'application/javascript'
          : path.endsWith('.css') ? 'text/css' : path.endsWith('.wasm') ? 'application/wasm' : 'application/octet-stream' })
      } catch { await route.abort() }
    })
    await page.route(/^http:\/\/127\.0\.0\.1:5189\/api\//, async route => {
      const request = route.request(), url = new URL(request.url()), path = url.pathname.replace(/^\/api/, ''), method = request.method()
      const ok = data => route.fulfill({ json: { code: 'OK', data } })
      const fail = (status, code) => route.fulfill({ status, json: { code, message: code, data: null } })
      if (path === '/auth/csrf') return ok({ token: 'isolated-csrf-fixture' })
      if (path === '/account/me') return state.signedIn ? ok(state.user) : fail(401, 'UNAUTHORIZED')
      if (path === '/auth/login') { state.signedIn = true; return ok(state.user) }
      if (path === '/auth/logout') { state.signedIn = false; return ok(null) }
      if(path==='/public/english/taxonomy')return ok([])
      if(path==='/account/english/writing/articles/2') {
        if(method==='PUT'){const submitted=request.postDataJSON();state.writingSaves.push(submitted);state.writing={...state.writing,...submitted,rowVersion:state.writing.rowVersion+1}}
        return ok(state.writing)
      }
      if (path === '/public/site/config') return ok({ siteName: '星雨笔录', modules: [] })
      if (path === '/public/site/home') return ok({ config: { siteName: '星雨笔录' }, sections: [{ code: 'HERO' }] })
      if (path === '/public/seo/meta') {
        const target = url.searchParams.get('route')
        if (target?.includes('missing')) return fail(404, 'SEO_NOT_FOUND')
        return ok({ title: target, description: 'Fixture description', robots: 'index,follow', canonicalUrl: `https://example.invalid${target}` })
      }
      if (path === '/admin/blog/tags') return ok({ items: state.tags, total: state.tags.length })
      if (/^\/admin\/blog\/tags\/\d+$/.test(path) && method === 'DELETE') {
        const id = Number(path.split('/').at(-1)); state.deletedTags.push(id); state.tags = state.tags.filter(tag => tag.id !== id); return ok(null)
      }
      if (path === '/admin/blog/posts') return ok({ items: [state.article], total: 1 })
      if (/^\/admin\/blog\/posts\/\d+\/body$/.test(path)) {
        if(state.bodyFailure)return fail(409,'CONTENT_VERSION_CONFLICT')
        const payload = request.postDataJSON(); state.savedBodies.push(payload.bodyMarkdown)
        if (state.delayBody) await state.delayBody
        state.article = { ...state.article, bodyMarkdown: payload.bodyMarkdown }; return ok(state.article)
      }
      if (/^\/admin\/blog\/posts\/\d+$/.test(path)) {
        if (method === 'PATCH') state.article = { ...state.article, ...request.postDataJSON() }
        return ok(state.article)
      }
      if (/^\/public\/blog\/posts\//.test(path)) {
        const slug = path.split('/').at(-1); if (state.blogDelays[slug]) await state.blogDelays[slug]
        return slug === 'missing' ? fail(404, 'BLOG_POST_NOT_FOUND') : ok(post(slug))
      }
      if (['/public/blog/tags', '/public/blog/topics', '/public/blog/archive/months'].includes(path)) return ok([])
      if (path === '/public/portfolio/taxonomy') return ok({ categories: [{ id: '1', name: '工具' }],
        formats: [{ id: '2', name: '移动应用' }], tags: [{ id: '3', name: '人工智能' }] })
      if (path === '/public/portfolio/works') return ok({ items: [work('a'), work('b'), work('c')], total: 3 })
      if (/^\/public\/portfolio\/works\//.test(path)) {
        const slug = path.split('/').at(-1); if (state.workDelays[slug]) await state.workDelays[slug]; return ok(work(slug))
      }
      if (path.startsWith('/public/blog/posts')) return ok({ items: [post('a'), post('b')], total: 2 })
      return ok({ items: [], total: 0, categories: [], tags: [], topics: [], permissions: [] })
    })
    await use(state)
    expect(browserErrors).toEqual([])
  }, { auto: true }],
})
export { expect }
