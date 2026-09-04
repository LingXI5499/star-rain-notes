import { describe, expect, it } from 'vitest'
import { resolveVocabularyVisibility, stableReviewDirection } from './vocabulary-display'
import type { VocabularyStudySettings } from './vocabulary-study-storage'

const settings: VocabularyStudySettings = {
  showEnglish: false, showChinese: true, reviewDirection: 'MIXED', dailyNewLimit: 20, dailyReviewLimit: 200,
}

describe('vocabulary display rules', () => {
  it('binds English, phonetics, examples and audio to one English visibility flag', () => {
    expect(resolveVocabularyVisibility('FOLLOW_GLOBAL', settings)).toEqual({ showEnglish: false, showChinese: true })
    expect(resolveVocabularyVisibility('ENGLISH_ONLY', settings)).toEqual({ showEnglish: true, showChinese: false })
    expect(resolveVocabularyVisibility('BILINGUAL', settings)).toEqual({ showEnglish: true, showChinese: true })
  })

  it('keeps mixed direction stable for a word throughout one day', () => {
    expect(stableReviewDirection(42, 'MIXED', 100)).toBe(stableReviewDirection(42, 'MIXED', 100))
    expect(stableReviewDirection(42, 'EN_TO_ZH', 100)).toBe('EN_TO_ZH')
    expect(stableReviewDirection(42, 'ZH_TO_EN', 100)).toBe('ZH_TO_EN')
  })
})
