/** Tag deletion requires confirmation; referenced tags keep their bindings. */
import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import Page from './BlogTaxonomyPage.vue'
const api = vi.hoisted(() => ({ createTag: vi.fn(), deleteTag: vi.fn(), disableTag: vi.fn(),
  enableTag: vi.fn(), listAdminTags: vi.fn(), updateTag: vi.fn(), ask: vi.fn() }))
vi.mock('../../api/blogApi', () => api)
const Confirm = defineComponent({ setup(_, { expose }) { expose({ ask: api.ask }); return () => null } })
let wrapper
beforeEach(() => {
  vi.resetAllMocks(); api.listAdminTags.mockResolvedValue({ items: [
    { id: 1, name: 'Unused', status: 'ENABLED', postCount: 0 },
    { id: 2, name: 'Bound', status: 'ENABLED', postCount: 3 },
  ], total: 2 }); api.deleteTag.mockResolvedValue(null)
})
afterEach(() => wrapper?.unmount())
async function start() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/useradmin/blog/taxonomy', component: Page }] })
  await router.push('/useradmin/blog/taxonomy')
  wrapper = mount(Page, { global: { plugins: [router], stubs: { AdminConfirmDialog: Confirm } } })
  await flushPromises()
}
it('deletes the unused tag after exact-name confirmation and refreshes the list', async () => {
  api.ask.mockResolvedValue(true); await start()
  await wrapper.findAll('button').find(button => button.text() === '删除').trigger('click'); await flushPromises()
  expect(api.ask).toHaveBeenCalledWith(expect.objectContaining({ requireName: 'Unused', danger: true }))
  expect(api.deleteTag).toHaveBeenCalledWith(1); expect(api.listAdminTags).toHaveBeenCalledTimes(2)
})
it('cancellation does not delete, and bound tags cannot be deleted', async () => {
  api.ask.mockResolvedValue(false); await start()
  const buttons = wrapper.findAll('button').filter(button => button.text() === '删除')
  expect(buttons[1].element.disabled).toBe(true)
  await buttons[0].trigger('click'); await flushPromises(); expect(api.deleteTag).not.toHaveBeenCalled()
})
it('shows a server-side reference conflict when a new binding appeared after loading', async () => {
  api.ask.mockResolvedValue(true); api.deleteTag.mockRejectedValue({ response: { status: 409, data: { message: '标签仍被文章引用' } } })
  await start(); await wrapper.findAll('button').find(button => button.text() === '删除').trigger('click'); await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('引用')
})
