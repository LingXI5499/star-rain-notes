import { beforeEach, expect, it, vi } from 'vitest'
const get = vi.hoisted(() => vi.fn())
vi.mock('./http', () => ({ http: { get } }))
beforeEach(() => { vi.resetModules(); get.mockReset() })
it('deduplicates parallel header/footer requests and expires public configuration', async () => {
  vi.useFakeTimers()
  get.mockResolvedValue({ data: { siteName: 'first' } })
  const api = await import('./site')
  await Promise.all([api.fetchPublicSite(), api.fetchPublicSite()])
  expect(get).toHaveBeenCalledTimes(1)
  vi.advanceTimersByTime(60_001)
  await api.fetchPublicSite()
  expect(get).toHaveBeenCalledTimes(2)
  vi.useRealTimers()
})
it('does not let a previous in-flight request overwrite a forced refresh', async () => {
  let resolve!: (value: unknown) => void
  get.mockImplementationOnce(() => new Promise(r => { resolve = r }))
  const api = await import('./site')
  const old = api.fetchPublicSite()
  get.mockResolvedValueOnce({ data: { siteName: 'new' } })
  await api.fetchPublicSite(true)
  resolve({ data: { siteName: 'old' } })
  await old
  expect((await api.fetchPublicSite()).siteName).toBe('new')
})
