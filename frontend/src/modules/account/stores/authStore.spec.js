import { beforeEach, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import { useAuthStore } from './authStore'
import * as api from '../api/accountApi'
import * as drafts from '../../../shared/editor/recovery/draftStore'
vi.mock('../api/accountApi', () => ({ currentAccount: vi.fn(), login: vi.fn(), logout: vi.fn() }))
vi.mock('../../../shared/editor/recovery/draftStore', () => ({
  flushDrafts: vi.fn(), accountDrafts: vi.fn(), clearAccountDrafts: vi.fn(), allowDraftWrites: vi.fn(),
}))
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
beforeEach(() => {
  setActivePinia(createPinia()); vi.resetAllMocks()
  drafts.accountDrafts.mockResolvedValue([])
  api.logout.mockResolvedValue()
  window.confirm = vi.fn(() => true)
})
it('AUTH-23: old initialization cannot overwrite a more recent login', async () => {
  const store = useAuthStore(), old = deferred()
  api.currentAccount.mockReturnValue(old.promise)
  const initializing = store.initialize(); await flushPromises()
  api.login.mockResolvedValue({ id: 2 })
  await store.login('new', 'private')
  old.resolve({ id: 1 }); await initializing
  expect(store.currentUser.id).toBe(2)
})
it('AUTH-23: old initialization failure cannot erase a recent login', async () => {
  const store = useAuthStore(), old = deferred()
  api.currentAccount.mockReturnValue(old.promise)
  const initializing = store.initialize(); await flushPromises()
  api.login.mockResolvedValue({ id: 2 }); await store.login('new', 'private')
  old.reject({ response: { status: 401 } }); await initializing
  expect(store.currentUser.id).toBe(2)
})
it('AUTH-23: logout follows an in-flight server login and ignores its late result', async () => {
  const store = useAuthStore(), pending = deferred()
  api.login.mockReturnValue(pending.promise)
  const login = store.login('new', 'private'); await flushPromises()
  const logout = store.logout(); await flushPromises()
  expect(api.logout).not.toHaveBeenCalled()
  pending.resolve({ id: 2 }); expect(await login).toBe(false)
  expect(await logout).toBe(true)
  expect(api.logout).toHaveBeenCalledOnce(); expect(store.currentUser).toBeNull()
})
it('AUTH-23: a late profile response cannot restore a logged out user', async () => {
  const store = useAuthStore(), pending = deferred(); store.currentUser = { id: 1 }
  const update = store.updateCurrent(() => pending.promise)
  await store.logout(); pending.resolve({ id: 1, displayName: 'old' })
  expect(await update).toBe(false); expect(store.currentUser).toBeNull()
})
it('AUTH-26: cancelling draft deletion preserves both the server and local identity', async () => {
  const store = useAuthStore(); store.currentUser = { id: 1 }
  drafts.accountDrafts.mockResolvedValue([{}]); window.confirm.mockReturnValue(false)
  await expect(store.logout()).rejects.toThrow('已取消退出')
  expect(api.logout).not.toHaveBeenCalled(); expect(store.currentUser.id).toBe(1)
})
it('AUTH-27: failed inspection requires a choice and failed cleanup stays retryable after logout', async () => {
  const store = useAuthStore(); store.currentUser = { id: 1 }
  drafts.accountDrafts.mockRejectedValue(new Error('SecurityError'))
  drafts.clearAccountDrafts.mockRejectedValueOnce(new Error('blocked'))
  await store.logout()
  expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('本地备份可能残留'))
  expect(store.currentUser).toBeNull(); expect(store.cleanupError).toContain('可能仍有本地副本')
  await store.retryDraftCleanup()
  expect(store.cleanupError).toBe(''); expect(store.cleanupAccounts).toEqual([])
})
it('AUTH-27: cancel after storage inspection failure never sends logout', async () => {
  const store = useAuthStore(); store.currentUser = { id: 1 }
  drafts.accountDrafts.mockRejectedValue(new Error('blocked')); window.confirm.mockReturnValue(false)
  await expect(store.logout()).rejects.toThrow('取消'); expect(api.logout).not.toHaveBeenCalled()
})
it('AUTH-23: duplicate login and logout cannot issue duplicate server mutations', async () => {
  const store = useAuthStore(), pending = deferred(); api.login.mockReturnValue(pending.promise)
  const one = store.login('user', 'private'); expect(await store.login('user', 'private')).toBe(false)
  pending.resolve({ id: 1 }); await one
  const first = store.logout(); expect(await store.logout()).toBe(false); await first
  expect(api.login).toHaveBeenCalledOnce(); expect(api.logout).toHaveBeenCalledOnce()
})
it('a failed server logout keeps identity and allows retry', async () => {
  const store = useAuthStore(); store.currentUser = { id: 1 }
  api.logout.mockRejectedValueOnce(new Error('offline'))
  await expect(store.logout()).rejects.toThrow('offline')
  expect(store.currentUser.id).toBe(1); expect(drafts.allowDraftWrites).toHaveBeenLastCalledWith(1)
  await store.logout(); expect(store.currentUser).toBeNull()
})
