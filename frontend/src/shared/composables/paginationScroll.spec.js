/** A filter resetting page=1 must not be confused with deliberate pagination. */
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, computed, ref } from 'vue'
import { createMemoryHistory, createRouter, useRoute } from 'vue-router'
import { afterEach, expect, it, vi } from 'vitest'
import { useInitialListScroll, usePaginationScroll } from './paginationScroll'
let wrapper
afterEach(() => { wrapper?.unmount(); sessionStorage.clear(); vi.restoreAllMocks() })
it('preserves a filtered page reset but scrolls a subsequent page change', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  const Host = defineComponent({ setup() {
    const route = useRoute(), page = computed(() => Number(route.query.page || 1))
    usePaginationScroll(() => page.value)
    return () => null
  } })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/list', component: Host }] })
  await router.push('/list?page=3')
  wrapper = mount(Host, { global: { plugins: [router] } })
  await router.push('/list?keyword=b'); await flushPromises()
  expect(window.scrollTo).not.toHaveBeenCalled()
  await router.push('/list?keyword=b&page=2'); await flushPromises()
  expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' })
})
it('restores a reload once, after data finishes loading', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  vi.spyOn(performance, 'getEntriesByType').mockReturnValue([{ type: 'reload', name: 'http://localhost/list?tag=b' }])
  sessionStorage.setItem('star-rain:list-scroll', JSON.stringify({ path: '/list?tag=b', top: 800, left: 0, admin: false }))
  const loading = ref(true)
  const Host = defineComponent({ setup() { useInitialListScroll(() => loading.value); return () => null } })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/list', component: Host }] })
  await router.push('/list?tag=b'); wrapper = mount(Host, { global: { plugins: [router] } }); await flushPromises()
  expect(window.scrollTo).not.toHaveBeenCalled()
  loading.value = false; await flushPromises()
  expect(window.scrollTo).toHaveBeenCalledWith({ top: 800, left: 0, behavior: 'instant' })
  loading.value = true; await flushPromises(); loading.value = false; await flushPromises()
  expect(window.scrollTo).toHaveBeenCalledTimes(1)
})
it('does not apply a reload snapshot to a later SPA page', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  vi.spyOn(performance, 'getEntriesByType').mockReturnValue([{ type: 'reload', name: 'http://localhost/another-page' }])
  sessionStorage.setItem('star-rain:list-scroll', JSON.stringify({ path: '/list', top: 800, left: 0, admin: false }))
  const Host = defineComponent({ setup() { useInitialListScroll(); return () => null } })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/list', component: Host }] })
  await router.push('/list'); wrapper = mount(Host, { global: { plugins: [router] } }); await flushPromises()
  expect(window.scrollTo).not.toHaveBeenCalled()
})
