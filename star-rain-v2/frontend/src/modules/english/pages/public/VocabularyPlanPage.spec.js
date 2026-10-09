import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import VocabularyPlanPage from './VocabularyPlanPage.vue'
const api = vi.hoisted(() => ({ currentPlan: vi.fn(), planItems: vi.fn(), rateWord: vi.fn() }))
vi.mock('../../api/vocabularyLearningApi', async () => ({ ...(await vi.importActual('../../api/vocabularyLearningApi')), ...api }))
vi.mock('../../../../shared/viewMode', () => ({ useViewMode: () => ({ contentPath: path => path }) }))
const button = (wrapper, label) => wrapper.findAll('button').find(item => item.text() === label)
const plan = { name: 'Probe', revision: 3, status: 'ACTIVE', totalWords: 2, totalGroups: 1, completedRatings: 1, batchSize: 20,
  nextDefaultEntry: { groupNo: 1, direction: 'EN_TO_ZH', wordId: '2' } }
beforeEach(() => {
  vi.clearAllMocks(); api.currentPlan.mockResolvedValue(structuredClone(plan))
  api.planItems.mockResolvedValue([{ wordId: '1', word: { id: '1', word: 'one', translation: '一' }, doneMask: 1 }, { wordId: '2', word: { id: '2', word: 'two', translation: '二' }, doneMask: 0 }])
})
describe('plan entry and idempotent recovery', () => {
  it('enters preview without rating and starts the server-derived incomplete word', async () => {
    const wrapper = mount(VocabularyPlanPage, { global: { stubs: { RouterLink: true } } }); await flushPromises()
    expect(wrapper.text()).toContain('自由预览不计次数'); expect(api.rateWord).not.toHaveBeenCalled()
    await button(wrapper, '开始默认训练').trigger('click'); await flushPromises()
    expect(wrapper.text()).toContain('two'); expect(wrapper.findAll('.recall-card__translation')).toHaveLength(0)
  })
  it('network retry preserves UUID, rating, direction and revision', async () => {
    api.rateWord.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ memory: {}, plan: { ...plan, nextDefaultEntry: null } })
    const wrapper = mount(VocabularyPlanPage, { global: { stubs: { RouterLink: true } } }); await flushPromises()
    await button(wrapper, '开始默认训练').trigger('click'); await flushPromises()
    await button(wrapper, '揭晓答案').trigger('click'); await button(wrapper, '忘记').trigger('click'); await flushPromises()
    const first = JSON.parse(JSON.stringify(api.rateWord.mock.calls[0])); expect(first[1]).toMatchObject({ rating: 'FORGOT', direction: 'EN_TO_ZH', source: 'PLAN', planRevision: 3 })
    await button(wrapper, '重试原评分').trigger('click'); await flushPromises()
    expect(api.rateWord.mock.calls[1]).toEqual(first); expect(wrapper.text()).toContain('本计划已完成')
  })
})
