import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import PublicSelect from './PublicSelect.vue'
import PublicPagination from './PublicPagination.vue'
import PublicSearch from './PublicSearch.vue'
import { publicPage, publicPageSize, sizeQuery } from '../composables/publicListState'
const mounted = []
function render(component, props) { const wrapper = mount(component, { props, attachTo: document.body }); mounted.push(wrapper); return wrapper }
afterEach(() => { mounted.splice(0).forEach(w => w.unmount()); vi.useRealTimers() })
describe('public browsing controls', () => {
  it('allows keyboard selection, restores trigger focus and closes on an outside click', async () => {
    const w = render(PublicSelect, { label: '作品形态', modelValue: '', options: [{ value: '', label: '全部' }, { value: 'web', label: 'Web 应用' }] })
    const trigger = w.get('[role=combobox]'); await trigger.trigger('keydown', { key: 'ArrowDown' }); await flushPromises()
    expect(trigger.attributes('aria-expanded')).toBe('true')
    await trigger.trigger('keydown', { key: 'ArrowDown' }); await trigger.trigger('keydown', { key: 'Enter' })
    expect(w.emitted('update:modelValue')).toEqual([['web']]); expect(document.activeElement).toBe(trigger.element)
    await trigger.trigger('click'); document.body.dispatchEvent(new Event('pointerdown', { bubbles: true })); await flushPromises()
    expect(trigger.attributes('aria-expanded')).toBe('false')
  })
  it('searches long option lists and gives an empty state without changing the selection', async () => {
    const w = render(PublicSelect, { label: '标签', modelValue: '0', options: Array.from({ length: 12 }, (_, n) => ({ value: String(n), label: '标签 ' + n })) })
    await w.get('[role=combobox]').trigger('click'); await flushPromises()
    const search = document.querySelector('input[aria-label="搜索标签选项"]')
    search.value = '标签 11'; search.dispatchEvent(new Event('input', { bubbles: true })); await flushPromises()
    expect(document.querySelectorAll('[role=option]')).toHaveLength(1)
    document.querySelector('[role=option]').click(); await flushPromises(); expect(w.emitted('update:modelValue')).toEqual([['11']])
    await w.get('[role=combobox]').trigger('click'); await flushPromises()
    const emptySearch = document.querySelector('input[aria-label="搜索标签选项"]'); emptySearch.value = '不存在'; emptySearch.dispatchEvent(new Event('input', { bubbles: true })); await flushPromises()
    expect(document.body.textContent).toContain('没有匹配的选项'); await w.get('[role=combobox]').trigger('keydown', { key: 'Escape' }); expect(w.emitted('update:modelValue')).toHaveLength(1)
  })
  it('shows the last partial range and disables the correct pagination boundaries', async () => {
    const w = render(PublicPagination, { total: 25, page: 3, pageSize: 12 })
    expect(w.text()).toContain('25–25 / 共 25 项'); expect(w.get('[aria-label=下一页]').element.disabled).toBe(true)
    await w.get('[aria-label=上一页]').trigger('click'); expect(w.emitted('change')).toEqual([[2]])
    await w.setProps({ loading: true }); await w.get('[aria-label="第 1 页"]').trigger('click'); expect(w.emitted('change')).toHaveLength(1)
    await w.setProps({ loading: false, total: 0, page: 1 }); expect(w.text()).toContain('0 / 共 0 项'); expect(w.get('[role=combobox]').exists()).toBe(true)
  })
  it('keeps page numbers bounded while exposing page size changes', async () => {
    const w = render(PublicPagination, { total: 1200, page: 50, pageSize: 12 })
    expect(w.findAll('.public-pagination__pages button')).toHaveLength(7); expect(w.text()).toContain('…')
    await w.get('[role=combobox]').trigger('click'); await flushPromises()
    const option = [...document.querySelectorAll('[role=option]')].find(item => item.textContent.includes('24')); option.click(); await flushPromises()
    expect(w.emitted('page-size')).toEqual([[24]])
  })
  it('debounces search, suppresses IME composition and clears pending work on unmount', async () => {
    vi.useFakeTimers(); const w = render(PublicSearch, { modelValue: '', label: '关键词' })
    const input = w.get('input'); await input.trigger('compositionstart'); input.element.value = '教程'; await input.trigger('input', { isComposing: true }); await w.setProps({ modelValue: '教程' }); await vi.advanceTimersByTimeAsync(500)
    expect(w.emitted('search')).toBeUndefined(); await input.trigger('compositionend'); await vi.advanceTimersByTimeAsync(350); expect(w.emitted('search')).toEqual([['教程']])
    await input.setValue('新内容'); expect(w.emitted('search')).toHaveLength(1); w.unmount(); mounted.pop(); expect(vi.getTimerCount()).toBe(0)
  })
  it('accepts only sensible page sizes and preserves filters when resetting the page', () => {
    expect(publicPage('-1')).toBe(1); expect(publicPage('1.5')).toBe(1); expect(publicPage('Infinity')).toBe(1)
    expect(publicPageSize('1000000', 24, [12, 24, 48])).toBe(24)
    expect(sizeQuery({ tag: 'java', page: '8', month: '2026-10' }, 20, 10)).toEqual({ tag: 'java', month: '2026-10', page: undefined, pageSize: '20' })
  })
})
