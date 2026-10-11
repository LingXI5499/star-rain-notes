import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import Page from './TutorialManagePage.vue'
import * as api from '../../api/tutorialApi'
const state = vi.hoisted(() => ({ route: null, replace: vi.fn(), push: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => state.route, useRouter: () => state, RouterLink: true }))
vi.mock('../../../account/stores/authStore', () => ({ useAuthStore: () => ({ hasPermission: () => true }) }))
vi.mock('../../api/tutorialApi', () => Object.fromEntries(['createCategory', 'deleteCategory', 'deleteTutorial', 'listAdminCategories', 'listAdminTutorials', 'listPendingTutorialOperations', 'publishTutorial', 'reorderCategories', 'reorderTutorials', 'updateCategory', 'updateTutorial', 'withdrawTutorial', 'listTutorialOrderItems'].map(name => [name, vi.fn()])))
const wrappers = []
const row = (id, title) => ({ id, title, categoryId: '1', sortOrder: Number(id), publicationStatus: 'PUBLISHED', chapterCount: 1 })
const open = () => {
  const wrapper = mount(Page, { global: { stubs: { AdminConfirmDialog: true, PublicPagination: { props: ['page', 'total'], emits: ['change'], template: '<button data-page-two @click="$emit(\'change\', 2)">Page 2</button>' } } } })
  wrappers.push(wrapper); return wrapper
}
beforeEach(() => {
  vi.clearAllMocks(); state.route = reactive({ query: {} })
  api.listAdminCategories.mockResolvedValue([{ id: '1', name: 'One', tutorialCount: 1000, chapterCount: 1200 }, { id: '2', name: 'Two', tutorialCount: 3, chapterCount: 3 }])
  api.listPendingTutorialOperations.mockResolvedValue([])
  api.listAdminTutorials.mockResolvedValue({ items: [row('1', 'First')], total: 1000 })
  api.listTutorialOrderItems.mockResolvedValue([row('1', 'First'), row('2', 'Hidden'), row('3', 'Last')])
  api.reorderTutorials.mockResolvedValue({})
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()); vi.useRealTimers() })
describe('bounded tutorial administration', () => {
  it('requests one SQL-backed page even with a thousand tutorials', async () => {
    const wrapper = open(); await flushPromises()
    expect(api.listAdminTutorials).toHaveBeenCalledOnce()
    expect(api.listAdminTutorials).toHaveBeenLastCalledWith({ page: 1, pageSize: 20, categoryId: '1', keyword: undefined })
    expect(wrapper.text()).toContain('体系共 1200 个章节')
    expect(api.listTutorialOrderItems).not.toHaveBeenCalled()
    await wrapper.find('[data-page-two]').trigger('click'); await flushPromises()
    expect(api.listAdminTutorials).toHaveBeenLastCalledWith({ page: 2, pageSize: 20, categoryId: '1', keyword: undefined })
  })
  it('debounces search and resets paging without fetching all pages', async () => {
    const wrapper = open(); await flushPromises(); vi.useFakeTimers()
    await wrapper.find('input[type="search"]').setValue('J')
    await wrapper.find('input[type="search"]').setValue('Java')
    expect(api.listAdminTutorials).toHaveBeenCalledOnce()
    await vi.advanceTimersByTimeAsync(300); await flushPromises()
    expect(api.listAdminTutorials).toHaveBeenCalledTimes(2)
    expect(api.listAdminTutorials.mock.lastCall[0]).toMatchObject({ page: 1, keyword: 'Java' })
  })
  it('ignores a stale result when switching categories quickly', async () => {
    let oldResolve
    api.listAdminTutorials.mockReturnValueOnce(new Promise(resolve => { oldResolve = resolve }))
      .mockResolvedValueOnce({ items: [{ ...row('2', 'New category result'), categoryId: '2' }], total: 3 })
    const wrapper = open(); await flushPromises()
    await wrapper.findAll('.tutorial-manage__system')[1].trigger('click'); await flushPromises()
    oldResolve({ items: [row('1', 'Stale result')], total: 1000 }); await flushPromises()
    expect(wrapper.text()).toContain('New category result')
    expect(wrapper.text()).not.toContain('Stale result')
  })
  it('loads the complete lightweight order only on demand and includes hidden-page IDs when sorting', async () => {
    const wrapper = open(); await flushPromises()
    const toggle = wrapper.findAll('button').find(button => button.text() === '调整顺序')
    await toggle.trigger('click'); await flushPromises()
    expect(api.listTutorialOrderItems).toHaveBeenCalledWith('1')
    const items = wrapper.findAll('.tutorial-manage__order-item')
    await items[2].trigger('dragstart'); await items[0].trigger('drop'); await flushPromises()
    expect(api.reorderTutorials).toHaveBeenCalledWith('1', ['3', '1', '2'])
  })
})
