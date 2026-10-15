import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import AdminAccountsPage from './AdminAccountsPage.vue'
import AdminInvitationsPage from './AdminInvitationsPage.vue'
import AccountAuditsPage from './AccountAuditsPage.vue'
import * as api from '../api/accountApi'
vi.mock('../api/accountApi', () => ({ listAccounts: vi.fn(), listInvitations: vi.fn(), listAudits: vi.fn() }))
vi.mock('../../../shared/composables/paginationScroll', () => ({ usePaginationScroll: () => {} }))
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
let wrapper
beforeEach(() => { setActivePinia(createPinia()); vi.resetAllMocks() })
afterEach(() => wrapper?.unmount())
it.each([
  [AdminAccountsPage, 'listAccounts'], [AdminInvitationsPage, 'listInvitations'], [AccountAuditsPage, 'listAudits'],
])('AUTH-24: %s keeps only its latest result and ignores an earlier failure', async (component, method) => {
  const first = deferred(), second = deferred()
  api[method].mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
  wrapper = mount(component, { global: { stubs: { RouterLink: true } } })
  await flushPromises()
  const loadingLatest = wrapper.vm.load()
  second.resolve({ items: [], total: 42, page: 2, pageSize: 20 })
  await loadingLatest; await flushPromises()
  expect(wrapper.text()).toContain('共 42 条')
  first.reject(new Error('old network failure')); await flushPromises()
  expect(wrapper.text()).toContain('共 42 条'); expect(wrapper.find('[role="alert"]').exists()).toBe(false)
})
