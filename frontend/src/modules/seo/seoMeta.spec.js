/** SEO navigation must clear metadata belonging to the previous page on failure. */
import { beforeEach, expect, it, vi } from 'vitest'
import { installSeoMeta } from './seoMeta'
const api = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../shared/http', () => ({ default: api }))
beforeEach(() => { vi.resetAllMocks(); document.head.innerHTML = '' })
it('removes stale descriptions and canonical on a failed page request', async () => {
  let navigate; installSeoMeta({ afterEach: handler => { navigate = handler } })
  api.get.mockResolvedValueOnce({ data: { data: { title: 'A', description: 'Description A', robots: 'index,follow', canonicalUrl: 'https://example.invalid/a' } } })
  await navigate({ path: '/a' }); api.get.mockRejectedValueOnce(new Error('offline')); await navigate({ path: '/b' })
  expect(document.querySelector('meta[name="description"]').content).toBe('')
  expect(document.querySelector('meta[name="robots"]').content).toBe('noindex,follow')
  expect(document.querySelector('link[rel="canonical"]').hasAttribute('href')).toBe(false)
})
it('ignores old responses even if the fake transport does not honor cancellation', async () => {
  let resolveA, navigate; installSeoMeta({ afterEach: handler => { navigate = handler } })
  api.get.mockReturnValueOnce(new Promise(resolve => { resolveA = resolve }))
  const a = navigate({ path: '/a' })
  api.get.mockResolvedValueOnce({ data: { data: { title: 'B', robots: 'index,follow', canonicalUrl: 'https://example.invalid/b' } } })
  await navigate({ path: '/b' }); resolveA({ data: { data: { title: 'A' } } }); await a
  expect(document.title).toBe('B'); expect(document.querySelectorAll('link[rel="canonical"]')).toHaveLength(1)
})
