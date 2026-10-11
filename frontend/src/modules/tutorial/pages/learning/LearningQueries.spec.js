import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasteryPage from './MasteryPage.vue'
import LearningHistoryPage from './LearningHistoryPage.vue'
import { getMasteryPage, getMasteryOptions, getLearningHistory, getHistoryOptions, getPlanHistory } from '../../api/learningApi'
vi.mock('../../api/learningApi', () => ({ getMasteryPage: vi.fn(), getMasteryOptions: vi.fn(), getLearningHistory: vi.fn(), getHistoryOptions: vi.fn(), getPlanHistory: vi.fn() }))
async function render(component) {
 const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/useradmin/learning/:pathMatch(.*)*', component: { template: '<div />' } }] })
 await router.push('/useradmin/learning/history'); await router.isReady()
 const w = mount(component, { global: { plugins: [router], stubs: { LearningNav: true } } }); await flushPromises(); return w
}
const button = (w, text) => w.findAll('button').find(b => b.text() === text || b.attributes('aria-label') === text)
describe('learning query pagination', () => {
 beforeEach(() => { vi.resetAllMocks(); for (const api of [getMasteryPage, getLearningHistory, getPlanHistory]) api.mockResolvedValue({ items: [], total: 43 }); getMasteryOptions.mockResolvedValue([{ id: 't', title: 'Tutorial' }]); getHistoryOptions.mockResolvedValue([{ id: 't', title: 'Tutorial' }]) })
 it('queries mastery with conditions and resets page one when conditions change', async () => {
  const w = await render(MasteryPage); await button(w, '下一页').trigger('click'); await flushPromises(); expect(getMasteryPage).toHaveBeenLastCalledWith({ page: 2, pageSize: 20 })
  await w.get('input').setValue('Recall'); w.findAllComponents({ name: 'PublicSelect' }).find(c => c.props('label') === '状态').vm.$emit('update:modelValue', 'LEARNING'); await flushPromises()
  expect(getMasteryPage).toHaveBeenLastCalledWith({ page: 1, pageSize: 20, keyword: 'Recall', status: 'LEARNING' }); expect(w.findAllComponents({ name: 'PublicSelect' }).find(c => c.props('label') === '教程').props('options')).toContainEqual({ value: 't', label: 'Tutorial' }); w.unmount()
 })
 it('sends history dates and type before paginating and keeps those conditions', async () => {
  const w = await render(LearningHistoryPage); w.findAllComponents({ name: 'PublicSelect' }).find(c => c.props('label') === '学习类型').vm.$emit('update:modelValue', 'INITIAL_STUDY'); await flushPromises(); await w.findAll('input[type="date"]')[0].setValue('2026-10-01'); await flushPromises()
  await button(w, '下一页').trigger('click'); await flushPromises()
  expect(getLearningHistory).toHaveBeenLastCalledWith({ page: 2, pageSize: 20, sessionType: 'INITIAL_STUDY', fromDate: '2026-10-01' }); w.unmount()
 })
 it('keeps ended plans accessible in history with their own paginated query', async () => {
  const w = await render(LearningHistoryPage); await button(w, '已结束计划').trigger('click'); await flushPromises()
  expect(getPlanHistory).toHaveBeenLastCalledWith({ page: 1, pageSize: 20 }); expect(w.findAll('input[type="date"]')).toHaveLength(0)
  w.findAllComponents({ name: 'PublicSelect' }).find(c => c.props('label') === '状态').vm.$emit('update:modelValue', 'COMPLETED'); await flushPromises()
  expect(getPlanHistory).toHaveBeenLastCalledWith({ page: 1, pageSize: 20, status: 'COMPLETED' }); w.unmount()
 })
})
