/** Admin filters must keep their content scroll; navigating to another page resets it. */
import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, expect, it, vi } from 'vitest'
import Shell from './AdminShell.vue'
vi.mock('../../modules/account/stores/authStore', () => ({ useAuthStore: () => ({
  currentUser: { username: 'Fixture', roles: ['SUPER_ADMIN'], permissions: [] }, initialized: true,
  hasPermission: () => true, canManage: () => true,
}) }))
let wrapper
afterEach(() => wrapper?.unmount())
it('does not reset the admin content when filters change', async () => {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<p />' } }] })
  await router.push('/blog/manage?page=3')
  wrapper = mount(Shell, { global: { plugins: [router] } }); await flushPromises()
  const content = wrapper.get('.admin-shell__content').element; content.scrollTo = vi.fn()
  await router.push('/blog/manage?tag=2'); await flushPromises()
  expect(content.scrollTo).not.toHaveBeenCalled()
  await router.push('/blog/taxonomy'); await flushPromises()
  expect(content.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'auto' })
})
