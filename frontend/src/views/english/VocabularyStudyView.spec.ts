import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import VocabularyStudyView from './VocabularyStudyView.vue'

const completeReview = vi.fn()
const saveSettings = vi.fn(async (value) => value)

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { themeId: '1' } }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/api/vocabulary', () => ({
  fetchVocabularySettings: vi.fn(async () => ({ showEnglish: true, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200 })),
  saveVocabularySettings: (value: unknown) => saveSettings(value),
  fetchVocabularyReviewQueue: vi.fn(async () => ({
    dueCount: 1, newCount: 0, generatedAt: new Date().toISOString(),
    items: [{
      direction: 'EN_TO_ZH', newWord: false,
      memory: { wordId: 1, memoryCount: 1, reviewStep: 1, reviewCount: 1, firstLearnedAt: null, lastReviewedAt: null, nextReviewAt: null, learningStatus: 'ACTIVE' },
      word: { id: 1, themeId: 1, partOfSpeech: 'n.', word: 'rain', phoneticUs: '/reɪn/', phoneticUk: '/reɪn/', translation: '雨', inflections: null, examples: [], memoryCount: 0, lastMemoryAt: null, audios: [], wordFamilies: [] },
    }],
  })),
  completeVocabularyReview: (...args: unknown[]) => completeReview(...args),
}))

describe('VocabularyStudyView', () => {
  beforeEach(() => {
    completeReview.mockReset().mockResolvedValue({ intervalSeconds: 1800 })
    saveSettings.mockClear()
  })

  it('uses direct buttons instead of a select for the review direction', async () => {
    const wrapper = mount(VocabularyStudyView)
    await flushPromises()

    expect(wrapper.find('.study__direction select').exists()).toBe(false)
    const directionButtons = wrapper.findAll('.study__direction button')
    expect(directionButtons.map((button) => button.text())).toEqual(['随机混合', '英译中', '中译英'])

    await directionButtons[1].trigger('click')
    await flushPromises()
    expect(saveSettings).toHaveBeenCalledWith(expect.objectContaining({ reviewDirection: 'EN_TO_ZH' }))
  })

  it('does not expose completion before the answer is revealed and saves only once on rapid clicks', async () => {
    const wrapper = mount(VocabularyStudyView)
    await flushPromises()
    expect(wrapper.text()).toContain('rain')
    expect(wrapper.text()).not.toContain('雨')
    expect(wrapper.findAll('button').some((button) => button.text().includes('完成本次记忆'))).toBe(false)

    await wrapper.get('button.primary').trigger('click')
    expect(wrapper.text()).toContain('雨')
    const complete = wrapper.get('button.primary')
    await Promise.all([complete.trigger('click'), complete.trigger('click')])
    await flushPromises()
    expect(completeReview).toHaveBeenCalledTimes(1)
  })
})
