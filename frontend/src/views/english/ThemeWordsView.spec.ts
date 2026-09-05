import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ThemeWordsView from './ThemeWordsView.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  fetchAllThemeWords: vi.fn(),
  fetchVocabularyMemory: vi.fn(),
  startVocabularyWords: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { themeId: '8' }, query: {} }),
  useRouter: () => ({ push: mocks.push }),
  RouterLink: { template: '<a><slot /></a>' },
}))

const word = (id: number) => ({
  id, themeId: 8, partOfSpeech: 'n.', word: `word-${id}`, phoneticUs: null, phoneticUk: null,
  translation: `释义-${id}`, inflections: null, examples: [], memoryCount: 0, lastMemoryAt: null,
  audios: [], wordFamilies: [],
})

vi.mock('@/api/vocabulary', () => ({
  fetchVocabularyLayers: vi.fn(async () => [{ layer: '基础通用词层B', layerOrder: 1, themes: [{ id: 8, name: '天气', wordCount: 2 }] }]),
  fetchThemeWords: vi.fn(async () => ({ items: [word(1), word(2)], total: 2, page: 1, pageSize: 24, totalPages: 1 })),
  fetchVocabularyStates: vi.fn(async () => ({})),
  fetchVocabularySettings: vi.fn(async () => ({ showEnglish: true, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200 })),
  fetchVocabularyWordsByIds: vi.fn(async () => []),
  fetchAllThemeWords: (...args: unknown[]) => mocks.fetchAllThemeWords(...args),
  fetchVocabularyMemory: (...args: unknown[]) => mocks.fetchVocabularyMemory(...args),
  startVocabularyWords: (...args: unknown[]) => mocks.startVocabularyWords(...args),
  saveVocabularySettings: vi.fn(async (value) => value),
  startVocabularyWord: vi.fn(),
  setVocabularyDisplay: vi.fn(),
  resetVocabularyProgress: vi.fn(),
}))

describe('ThemeWordsView', () => {
  beforeEach(() => {
    mocks.push.mockReset().mockResolvedValue(undefined)
    mocks.fetchAllThemeWords.mockReset().mockResolvedValue([word(1), word(2)])
    mocks.fetchVocabularyMemory.mockReset().mockResolvedValue({ 1: { count: 1, at: '' } })
    mocks.startVocabularyWords.mockReset().mockImplementation(async (_ids, onProgress) => {
      onProgress?.(1, 1)
      return 1
    })
  })

  it('starts every not-yet-planned word in the theme and then enters study', async () => {
    const wrapper = shallowMount(ThemeWordsView)
    await flushPromises()

    expect(wrapper.find('.words__display input').exists()).toBe(false)
    expect(wrapper.findAll('.words__display-options button').map((button) => button.text()))
      .toEqual(['中英双语', '只看英文', '只看中文'])

    await wrapper.get('.words__study').trigger('click')
    await flushPromises()

    expect(mocks.startVocabularyWords).toHaveBeenCalledWith([2], expect.any(Function))
    expect(mocks.push).toHaveBeenCalledWith({ path: '/english/vocabulary/study', query: { themeId: '8' } })
  })
})
