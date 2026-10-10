import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Pagination from './PublicPagination.vue'

let wrapper
beforeEach(() => { vi.spyOn(window, 'scrollTo').mockImplementation(() => {}) })
afterEach(() => { wrapper?.unmount(); document.body.innerHTML = ''; vi.restoreAllMocks() })
const open = props => wrapper = mount(Pagination, { props: { page: 1, pageSize: 12, total: 36, ...props } })
describe('pagination scroll behavior', () => {
  it('scrolls to the top after the next page finishes loading', async () => {
    open()
    await wrapper.find('[aria-label="下一页"]').trigger('click')
    expect(wrapper.emitted('change')[0]).toEqual([2]); expect(window.scrollTo).not.toHaveBeenCalled()
    await wrapper.setProps({ page: 2, loading: true }); await flushPromises()
    expect(window.scrollTo).not.toHaveBeenCalled()
    await wrapper.setProps({ loading: false }); await flushPromises()
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' })
  })
  it('scrolls the admin content container instead of the window', async () => {
    const container = document.createElement('main'); container.className = 'admin-shell__content'
    container.scrollTo = vi.fn(); document.body.append(container)
    open(); await wrapper.find('[aria-label="下一页"]').trigger('click')
    await wrapper.setProps({ page: 2 }); await flushPromises()
    expect(container.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' })
    expect(window.scrollTo).not.toHaveBeenCalled()
  })
  it('does not scroll for disabled, unchanged or failed pagination', async () => {
    open({ loading: true }); await wrapper.find('[aria-label="下一页"]').trigger('click')
    expect(wrapper.emitted('change')).toBeUndefined()
    await wrapper.setProps({ loading: false }); await wrapper.find('[aria-current="page"]').trigger('click')
    expect(window.scrollTo).not.toHaveBeenCalled()
    await wrapper.find('[aria-label="下一页"]').trigger('click')
    await wrapper.setProps({ loading: true }); await wrapper.setProps({ loading: false }); await flushPromises()
    expect(window.scrollTo).not.toHaveBeenCalled()
  })
  it('returns to the top after page size changes', async () => {
    open({ page: 2 })
    wrapper.findComponent({ name: 'PublicSelect' }).vm.$emit('update:model-value', 24)
    expect(wrapper.emitted('page-size')[0]).toEqual([24])
    await wrapper.setProps({ page: 1, pageSize: 24 }); await flushPromises()
    expect(window.scrollTo).toHaveBeenCalledOnce()
  })
})
