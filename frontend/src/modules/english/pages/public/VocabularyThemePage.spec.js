import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import VocabularyThemePage from './VocabularyThemePage.vue'
import { loadSelection } from '../../api/vocabularyLearningApi'

const api = vi.hoisted(() => ({
  resolveVocabularyAccount: vi.fn(), getVocabularyThemes: vi.fn(), getVocabularyStudySettings: vi.fn(),
  saveVocabularyStudySettings: vi.fn(), setVocabularyDisplay: vi.fn(),
  learningWords: vi.fn(), learningStates: vi.fn(), currentPlan: vi.fn(),
}))
vi.mock('../../api/englishApi', () => api)
vi.mock('../../api/vocabularyLearningApi', async () => ({ ...(await vi.importActual('../../api/vocabularyLearningApi')), ...api }))
vi.mock('../../../../shared/viewMode', () => ({ useViewMode: () => ({ contentPath: path => path }) }))
const mounted = []
const button = (wrapper, text) => wrapper.findAll('button').find(item => item.text().includes(text) || item.attributes('aria-label') === text)
async function setup(query = '') {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/english/vocabulary/:themeId', component: VocabularyThemePage }] })
  await router.push('/english/vocabulary/3' + query)
  const wrapper = mount(VocabularyThemePage, { global: { plugins: [router] } })
  mounted.push(wrapper); await flushPromises()
  return { wrapper, router }
}
beforeEach(() => {
  vi.clearAllMocks(); sessionStorage.clear()
  api.resolveVocabularyAccount.mockResolvedValue(true)
  api.getVocabularyThemes.mockResolvedValue([{ id: '3', name: '颜色与材料' }])
  api.getVocabularyStudySettings.mockResolvedValue({ showEnglish: true, showChinese: true })
  api.learningStates.mockResolvedValue({}); api.currentPlan.mockResolvedValue({ status: 'NONE' })
  api.learningWords.mockImplementation(async filters => ({ total: filters.q ? 1 : 50, items: [{ id: filters.page === 2 ? '2' : '1', word: filters.q || 'quality', translation: '质量' }] }))
})
afterEach(() => { mounted.splice(0).forEach(wrapper => wrapper.unmount()); vi.useRealTimers() })

describe('vocabulary toolbar and working filters', () => {
  it('debounces search, resets pagination and preserves the query on browser back', async () => {
    const { wrapper, router } = await setup('?page=3')
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    await wrapper.find('input[type=search]').setValue(' quality ')
    await vi.advanceTimersByTimeAsync(349); expect(api.learningWords).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(1); await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ q: 'quality' })
    expect(api.learningWords).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'quality', page: 1 }))
    expect(wrapper.text()).toContain('1 个匹配词汇')
    router.back(); await flushPromises()
    expect(wrapper.find('input[type=search]').element.value).toBe('')
    expect(api.learningWords).toHaveBeenLastCalledWith(expect.objectContaining({ q: '', page: 3 }))
  })

  it.each([['是否已学', 'learned', 'YES'], ['熟练度', 'mastery', 'UNRATED'], ['计划范围', 'inPlan', 'YES'], ['评价方向', 'direction', 'EN_TO_ZH'], ['最近评价', 'lastRating', 'FORGOT']])(
    'immediately applies %s to the server query', async (label, key, value) => {
      const { wrapper, router } = await setup('?page=2')
      await button(wrapper, '个人筛选').trigger('click')
      wrapper.findAllComponents({ name: 'PublicSelect' }).find(item => item.props('label') === label).vm.$emit('update:modelValue', value); await flushPromises()
      expect(router.currentRoute.value.query).toEqual({ [key]: value })
      expect(api.learningWords).toHaveBeenLastCalledWith(expect.objectContaining({ [key]: value, page: 1 }))
      await button(wrapper, '重置').trigger('click'); await flushPromises()
      expect(router.currentRoute.value.query).toEqual({})
      expect(wrapper.findAllComponents({ name: 'PublicSelect' }).find(item => item.props('label') === label).props('modelValue')).toBe('ANY')
    },
  )

  it('uses only the card action and retains explicit selection across pages and filters', async () => {
    const { wrapper, router } = await setup()
    expect(wrapper.findAll('input[type=checkbox]')).toHaveLength(0)
    await button(wrapper, '加入待选').trigger('click')
    expect(wrapper.find('.vocabulary-card--selected').exists()).toBe(true)
    expect(button(wrapper, '移出待选').attributes('aria-pressed')).toBe('true')
    await button(wrapper, '下一页').trigger('click'); await flushPromises()
    expect(loadSelection('3').wordIds).toEqual(['1'])
    await button(wrapper, '加入待选').trigger('click')
    expect(loadSelection('3').wordIds).toEqual(['1', '2'])
    await router.push({ query: { q: 'quality' } }); await flushPromises()
    expect(loadSelection('3').wordIds).toEqual(['1', '2'])
    await button(wrapper, '选择全部').trigger('click')
    expect(loadSelection('3').selectAllMatched).toBe(true)
    await router.push({ query: {} }); await flushPromises()
    expect(loadSelection('3').selectAllMatched).toBe(false)
    expect(loadSelection('3').wordIds).toEqual([])
  })

  it('does not filter intermediate Chinese composition text', async () => {
    const { wrapper } = await setup()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    const input = wrapper.find('input[type=search]')
    await input.trigger('compositionstart'); input.element.value = '颜色'
    await input.trigger('input', { isComposing: true })
    await vi.advanceTimersByTimeAsync(500); expect(api.learningWords).toHaveBeenCalledTimes(1)
    await input.trigger('compositionend'); await vi.advanceTimersByTimeAsync(350); await flushPromises()
    expect(api.learningWords).toHaveBeenLastCalledWith(expect.objectContaining({ q: '颜色' }))
  })

  it('gives guests a login link instead of unusable personal dropdowns', async () => {
    api.resolveVocabularyAccount.mockResolvedValue(false)
    const { wrapper } = await setup()
    await button(wrapper, '个人筛选').trigger('click')
    expect(wrapper.findAll('select')).toHaveLength(0)
    expect(wrapper.text()).toContain('搜索可直接使用')
    expect(wrapper.find('.theme-words__login a').attributes('href')).toContain('/useradmin/login?redirect=')
    await wrapper.find('input[type=search]').setValue('quality')
    await wrapper.find('form').trigger('submit'); await flushPromises()
    expect(api.learningWords).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'quality' }))
  })
})
