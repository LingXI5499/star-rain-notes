import type { VocabularyDisplayMode, VocabularyReviewDirection, VocabularyStudySettings } from './vocabulary-study-storage'

export function resolveVocabularyVisibility(mode: VocabularyDisplayMode | null | undefined, settings: VocabularyStudySettings) {
  const effective = mode && mode !== 'FOLLOW_GLOBAL' ? mode : 'FOLLOW_GLOBAL'
  return {
    showEnglish: effective === 'BILINGUAL' || effective === 'ENGLISH_ONLY'
      || (effective === 'FOLLOW_GLOBAL' && settings.showEnglish),
    showChinese: effective === 'BILINGUAL' || effective === 'CHINESE_ONLY'
      || (effective === 'FOLLOW_GLOBAL' && settings.showChinese),
  }
}

export function stableReviewDirection(wordId: number, setting: VocabularyReviewDirection, epochDay: number): 'EN_TO_ZH' | 'ZH_TO_EN' {
  if (setting !== 'MIXED') return setting
  return ((wordId + epochDay) & 1) === 0 ? 'EN_TO_ZH' : 'ZH_TO_EN'
}
