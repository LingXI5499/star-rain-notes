import { describe, it, expect, vi } from 'vitest'
import { createSelection, toggleSelected, selectedWord, saveSelection, loadSelection } from './vocabularyLearningApi'
vi.mock('./englishApi', () => ({ getVocabularyWords: vi.fn(), resolveVocabularyAccount: vi.fn(), vocabularyStudyStorage: {} }))
describe('cross-page selection intents', () => {
  it('keeps explicit identifiers as strings including values beyond the safe integer range', () => {
    let selection = createSelection('1')
    selection = toggleSelected(selection, '9007199254740993'); selection = toggleSelected(selection, '25')
    expect(selection.wordIds).toEqual(['9007199254740993', '25'])
    selection = toggleSelected(selection, '25'); expect(selectedWord(selection, '9007199254740993')).toBe(true)
  })
  it('whole-filter selection stores exclusions rather than all 10000 IDs', () => {
    let selection = { ...createSelection('1'), selectAllMatched: true }
    selection = toggleSelected(selection, '24'); expect(selection.wordIds).toEqual([])
    expect(selection.excludedWordIds).toEqual(['24']); expect(selectedWord(selection, '9999')).toBe(true)
    selection = toggleSelected(selection, '24'); expect(selection.excludedWordIds).toEqual([])
  })
  it('draft selection survives refresh while preserving its filter and group size', () => {
    const selection = { ...createSelection('1'), selectAllMatched: true, mastery: 'SKILLED', batchSize: 30, excludedWordIds: ['31'] }
    saveSelection(selection); expect(loadSelection('1')).toEqual(selection); expect(loadSelection('2').selectAllMatched).toBe(false)
  })
})
