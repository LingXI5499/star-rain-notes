import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, ref } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useEmailCode } from './useEmailCode'
import { useLatestAccountQuery } from './useLatestAccountQuery'
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
let wrapper
beforeEach(() => { setActivePinia(createPinia()); vi.useFakeTimers() })
afterEach(() => { wrapper?.unmount(); vi.useRealTimers() })
function query() {
  let state
  wrapper = mount(defineComponent({ setup() {
    const data = ref(null), error = ref(''), loading = ref(false)
    state = { data, error, loading, load: useLatestAccountQuery({ data, error, loading }) }
    return () => null
  } }))
  return state
}
it('AUTH-24: a stale success and finally cannot replace the latest query', async () => {
  const state = query(), one = deferred(), two = deferred()
  const first = state.load(() => one.promise), second = state.load(() => two.promise)
  one.resolve('old'); await first
  expect(state.data.value).toBeNull(); expect(state.loading.value).toBe(true)
  two.resolve('new'); await second
  expect(state.data.value).toBe('new'); expect(state.loading.value).toBe(false)
})
it('AUTH-24: stale failures cannot replace latest data or error', async () => {
  const state = query(), one = deferred()
  const first = state.load(() => one.promise)
  await state.load(() => Promise.resolve('new'))
  one.reject(new Error('old failure')); await first
  expect(state.data.value).toBe('new'); expect(state.error.value).toBe('')
})
it('AUTH-24: unmount and identity changes invalidate pending queries', async () => {
  const state = query(), one = deferred()
  const first = state.load(() => one.promise); useAuthStore().expireSession()
  one.resolve('private data'); await first; expect(state.data.value).toBeNull()
  const two = deferred(), second = state.load(() => two.promise)
  wrapper.unmount(); two.resolve('unmounted'); await second; expect(state.data.value).toBeNull()
})
it('AUTH-25: email request finishing after unmount never starts a timer', async () => {
  let hook
  wrapper = mount(defineComponent({ setup() { hook = useEmailCode(); return () => null } }))
  const pending = deferred(), result = hook.send(() => pending.promise)
  wrapper.unmount(); pending.resolve({ resendAfterSeconds: 60 })
  expect(await result).toBe(false); expect(vi.getTimerCount()).toBe(0)
})
it('AUTH-25: email failures after unmount are ignored and duplicate sends are blocked', async () => {
  let hook
  wrapper = mount(defineComponent({ setup() { hook = useEmailCode(); return () => null } }))
  const pending = deferred(), action = vi.fn(() => pending.promise), result = hook.send(action)
  expect(await hook.send(action)).toBe(false); expect(action).toHaveBeenCalledOnce()
  wrapper.unmount(); pending.reject(new Error('offline')); expect(await result).toBe(false)
})
it('email cooldown expires and an active failed send can be retried', async () => {
  let hook
  wrapper = mount(defineComponent({ setup() { hook = useEmailCode(); return () => null } }))
  await expect(hook.send(() => Promise.reject(new Error('offline')))).rejects.toThrow('offline')
  await hook.send(() => Promise.resolve({ resendAfterSeconds: 1 })); expect(hook.remaining.value).toBe(1)
  await vi.advanceTimersByTimeAsync(1001); await flushPromises()
  expect(hook.remaining.value).toBe(0); expect(vi.getTimerCount()).toBe(0)
})
