import { beforeEach, describe, it, expect, vi } from 'vitest'
import { installSeoMeta } from './seoMeta'
const http = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../shared/http', () => ({ default: http }))
function router() { let navigate; installSeoMeta({ afterEach: hook => { navigate = hook } }); return navigate }
const publicMeta = path => ({ data: { data: { title: path, description: '', robots: 'index,follow', canonicalUrl: 'https://example.org' + path } } })
beforeEach(() => { document.head.innerHTML = ''; http.get.mockReset(); http.get.mockImplementation(async (_, config) => publicMeta(config.params.route)) })
describe('SEO navigation metadata', () => {
  it.each([
    ['/login', { authPage: true }], ['/register', { authPage: true }],
    ['/center', { requiresAuth: true }], ['/blog/manage', { console: true, requiresAuth: true }],
    ['/tutorials/manage', { permission: 'tutorial:read-admin' }], ['/portfolio/manage', { superAdminOnly: true }],
    ['/missing', { noindex: true }],
  ])('excludes %s from indexing without calling public SEO', async (path, meta) => {
    await router()({ path, meta }, {})
    expect(document.querySelector('meta[name="robots"]').content).toBe('noindex,nofollow')
    expect(document.querySelector('link[rel="canonical"]').hasAttribute('href')).toBe(false)
    expect(http.get).not.toHaveBeenCalled()
  })
  it('restores public indexing after returning from account mode', async () => {
    const navigate = router()
    await navigate({ path: '/blog' }, { path: '/' })
    await navigate({ path: '/center', meta: { requiresAuth: true } }, { path: '/blog' })
    expect(document.querySelector('meta[name="robots"]').content).toBe('noindex,nofollow')
    await navigate({ path: '/blog' }, { path: '/center', meta: { requiresAuth: true } })
    expect(document.querySelector('meta[name="robots"]').content).toBe('index,follow')
    expect(http.get).toHaveBeenCalledTimes(2)
  })
  it('does not repeat metadata requests for a query-only filter change', async () => {
    const navigate = router()
    await navigate({ path: '/portfolio' }, { path: '/' })
    await navigate({ path: '/portfolio', query: { category: 'ai' } }, { path: '/portfolio' })
    expect(http.get).toHaveBeenCalledTimes(1)
  })
})
