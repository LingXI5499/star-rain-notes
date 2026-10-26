import { beforeEach, describe, expect, it, vi } from 'vitest'
import { installPublicTracking } from './publicTracking'
import { recordPageView } from '../api/analyticsApi'
vi.mock('../api/analyticsApi', () => ({ recordPageView: vi.fn().mockResolvedValue() }))
beforeEach(() => vi.clearAllMocks())

describe('public analytics route boundary', () => {
  it('never records authenticated, auth, console or failed navigation', () => {
    let navigate
    installPublicTracking({ afterEach: hook => { navigate = hook } })
    for (const meta of [{ requiresAuth: true }, { authPage: true }, { console: true }, { permission: 'read' }, { superAdminOnly: true }, { noindex: true }]) {
      // A stale publicPage marker or public-looking URL must not bypass the private boundary.
      navigate({ path: '/blog', meta: { publicPage: true, ...meta } })
    }
    navigate({ path: '/blog', meta: { publicPage: true } }, {}, new Error('cancelled'))
    navigate({ path: '/unknown', meta: {} })
    expect(recordPageView).not.toHaveBeenCalled()
    navigate({ path: '/', meta: { publicPage: true } })
    navigate({ path: '/blog', meta: { publicPage: true } })
    expect(recordPageView.mock.calls.map(args => args[0])).toEqual(['/', '/blog'])
  })
})
