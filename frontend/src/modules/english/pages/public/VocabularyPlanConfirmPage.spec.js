import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import VocabularyPlanConfirmPage from './VocabularyPlanConfirmPage.vue'
import { createSelection, saveSelection } from '../../api/vocabularyLearningApi'

const api = vi.hoisted(() => ({ previewPlan: vi.fn(), confirmPlan: vi.fn() }))
vi.mock('../../api/vocabularyLearningApi', async () => ({ ...(await vi.importActual('../../api/vocabularyLearningApi')), ...api }))
vi.mock('../../../../shared/viewMode', () => ({ useViewMode: () => ({ contentPath: path => path }) }))
const mounted = []
const preview = { sourceName: '颜色与材料', totalWords: 2, learnedWords: 1, totalGroups: 1, totalPages: 1,
  expectedRevision: 7, previewFingerprint: 'verified-selection', existingPlan: { status: 'ACTIVE', name: '旧学习计划' },
  samples: [{ word: 'quality' }, { word: 'compare' }], items: [{ id: '1', word: 'quality', translation: '质量' }, { id: '2', word: 'compare', translation: '比较' }] }
async function setup() {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/english/vocabulary/plan/confirm', component: VocabularyPlanConfirmPage },
    { path: '/english/vocabulary/plan', component: { template: '<div>学习计划</div>' } },
    { path: '/english/vocabulary/:themeId', component: { template: '<div>词库</div>' } },
  ] })
  await router.push('/english/vocabulary/plan/confirm?themeId=3')
  const wrapper = mount(VocabularyPlanConfirmPage, { global: { plugins: [router] } })
  mounted.push(wrapper); await flushPromises()
  return { wrapper, router }
}
beforeEach(() => {
  vi.clearAllMocks(); sessionStorage.clear()
  saveSelection({ ...createSelection('3'), wordIds: ['1', '2'] })
  api.previewPlan.mockResolvedValue(structuredClone(preview)); api.confirmPlan.mockResolvedValue({})
})
afterEach(() => mounted.splice(0).forEach(wrapper => wrapper.unmount()))

describe('plan confirmation card', () => {
  it('shows replacement consequences and requires acknowledgement before sending the verified selection', async () => {
    const { wrapper, router } = await setup()
    expect(wrapper.find('.plan-confirm__warning').text()).toContain('旧学习计划')
    expect(wrapper.find('.plan-confirm__warning').text()).toContain('评分历史与今日复习保留')
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
    await wrapper.find('form').trigger('submit'); expect(api.confirmPlan).not.toHaveBeenCalled()
    await wrapper.find('input[type=checkbox]').setValue(true)
    expect(wrapper.find('.plan-confirm__accept').classes()).toContain('is-checked')
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(false)
    await wrapper.find('form').trigger('submit'); await flushPromises()
    expect(api.confirmPlan).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ wordIds: ['1', '2'], expectedRevision: 7, previewFingerprint: 'verified-selection' }))
    expect(router.currentRoute.value.path).toBe('/english/vocabulary/plan')
  })

  it('presents creation copy when there is no current plan', async () => {
    api.previewPlan.mockResolvedValue({ ...preview, existingPlan: { status: 'NONE' } })
    const { wrapper } = await setup()
    expect(wrapper.find('.plan-confirm__warning').exists()).toBe(false)
    expect(wrapper.find('.plan-confirm__submit').text()).toBe('确认创建计划')
    expect(wrapper.find('.plan-confirm__accept').text()).toContain('确认创建这个学习计划')
  })

  it('requires a fresh acknowledgement after name or batch changes and does not preview invalid group sizes', async () => {
    const { wrapper } = await setup()
    const checkbox = wrapper.find('input[type=checkbox]')
    await checkbox.setValue(true)
    await wrapper.find('input[maxlength]').setValue('新的计划')
    expect(checkbox.element.checked).toBe(false)
    await checkbox.setValue(true)
    await wrapper.find('input[type=number]').setValue(0); await flushPromises()
    expect(wrapper.text()).toContain('请输入 5–100 之间的整数')
    expect(checkbox.element.checked).toBe(false)
    expect(checkbox.element.disabled).toBe(true)
    expect(api.previewPlan).toHaveBeenCalledTimes(1)
    await wrapper.find('form').trigger('submit'); expect(api.confirmPlan).not.toHaveBeenCalled()
    await wrapper.find('input[type=number]').setValue(30); await flushPromises()
    expect(api.previewPlan).toHaveBeenCalledTimes(2)
    expect(api.previewPlan).toHaveBeenLastCalledWith(expect.objectContaining({ batchSize: 30 }), 1, 24)
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
  })

  it('locks confirmation during a new preview and after all selected words have been removed', async () => {
    const { wrapper } = await setup()
    await wrapper.find('input[type=checkbox]').setValue(true)
    let resolvePreview
    api.previewPlan.mockImplementationOnce(() => new Promise(resolve => { resolvePreview = resolve }))
    await wrapper.find('button[aria-label="移除 quality"]').trigger('click')
    expect(wrapper.find('input[type=checkbox]').element.checked).toBe(false)
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
    await wrapper.find('form').trigger('submit'); expect(api.confirmPlan).not.toHaveBeenCalled()
    resolvePreview({ ...preview, totalWords: 0, totalGroups: 0, totalPages: 0, items: [], samples: [] }); await flushPromises()
    expect(wrapper.text()).toContain('待选词汇已清空')
    expect(wrapper.find('input[type=checkbox]').element.disabled).toBe(true)
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
  })

  it('resets acknowledgement after a conflict and enables confirmation only after a fresh preview', async () => {
    api.confirmPlan.mockRejectedValueOnce(new Error('计划已变化，请重新预览'))
    const { wrapper } = await setup()
    await wrapper.find('input[type=checkbox]').setValue(true)
    await wrapper.find('form').trigger('submit'); await flushPromises()
    expect(wrapper.find('[role=alert]').text()).toContain('计划已变化')
    expect(wrapper.find('input[type=checkbox]').element.checked).toBe(false)
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
    await wrapper.find('[role=alert] button').trigger('click'); await flushPromises()
    expect(wrapper.find('[role=alert]').exists()).toBe(false)
    expect(wrapper.find('input[type=checkbox]').element.disabled).toBe(false)
    expect(wrapper.find('.plan-confirm__submit').element.disabled).toBe(true)
  })
})
