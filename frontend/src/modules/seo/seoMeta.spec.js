import { beforeEach, describe, it, expect, vi } from 'vitest'
import { installSeoMeta } from './seoMeta'
const http = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../shared/http', () => ({ default: http }))
function router() { let navigate; installSeoMeta({ afterEach: hook => { navigate = hook } }); return navigate }
const publicMeta = path => ({ data: { data: { title: path, description: '', robots: 'index,follow', canonicalUrl: 'https://example.org' + path } } })
beforeEach(() => { document.head.innerHTML = ''; http.get.mockReset(); http.get.mockImplementation(async (_, config) => publicMeta(config.params.route)) })
describe('SEO navigation metadata', () => {
  it('restores public indexing after returning from account mode', async () => {
    const navigate = router()
    await navigate({ path: '/blog' }, { path: '/' })
    await navigate({ path: '/useradmin/center' }, { path: '/blog' })
    expect(document.querySelector('meta[name="robots"]').content).toBe('noindex,nofollow')
    await navigate({ path: '/blog' }, { path: '/useradmin/center' })
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
