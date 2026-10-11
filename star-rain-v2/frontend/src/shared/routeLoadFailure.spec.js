import { beforeEach, describe, expect, it, vi } from 'vitest'
import { handleRouteLoadError, reloadRoute, routeLoadFailed } from './routeLoadFailure'

beforeEach(() => { routeLoadFailed.value = false })
describe('route asset failures', () => {
  it('offers recovery for stale or unavailable chunks without automatically refreshing', () => {
    const reload = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
    handleRouteLoadError(new Error('Failed to fetch dynamically imported module'))
    expect(routeLoadFailed.value).toBe(true)
    expect(reload).not.toHaveBeenCalled()
    reloadRoute(); expect(reload).toHaveBeenCalledOnce()
    reload.mockRestore()
  })
  it('does not label unrelated business errors as chunk failures', () => {
    handleRouteLoadError(new Error('Access denied'))
    expect(routeLoadFailed.value).toBe(false)
  })
})
