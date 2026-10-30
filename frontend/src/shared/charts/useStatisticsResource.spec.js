import { effectScope } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { useStatisticsResource } from './useStatisticsResource'

const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
describe('independent statistics requests', () => {
  it('ignores late success and failure after a newer query', async () => {
    const old = deferred(), latest = deferred(), scope = effectScope()
    let resource
    scope.run(() => { resource = useStatisticsResource(vi.fn().mockReturnValueOnce(old.promise).mockReturnValueOnce(latest.promise)) })
    const a = resource.load('old'), b = resource.load('new')
    latest.resolve([2]); await b
    old.reject(new Error('late failure')); await a
    expect(resource.data.value).toEqual([2]); expect(resource.error.value).toBe(''); expect(resource.loading.value).toBe(false)
    scope.stop()
  })
  it('clears stale values on reload and recovers after an API failure', async () => {
    const scope = effectScope(), fetcher = vi.fn().mockResolvedValueOnce([4]).mockRejectedValueOnce(new Error('failed')).mockResolvedValueOnce([8])
    let resource; scope.run(() => { resource = useStatisticsResource(fetcher) })
    await resource.load(); const reload = resource.load()
    expect(resource.data.value).toBe(null); await reload
    expect(resource.error.value).toBeTruthy(); expect(resource.loading.value).toBe(false)
    await resource.load(); expect(resource.data.value).toEqual([8]); expect(resource.error.value).toBe('')
    scope.stop()
  })
  it('does not write responses after its page scope is disposed', async () => {
    const pending = deferred(), scope = effectScope()
    let resource; scope.run(() => { resource = useStatisticsResource(() => pending.promise) })
    const request = resource.load(); scope.stop(); pending.resolve([5]); await request
    expect(resource.data.value).toBe(null)
  })
})
