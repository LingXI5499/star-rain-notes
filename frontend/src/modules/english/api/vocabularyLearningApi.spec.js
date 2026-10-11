/**
 * vocabularyLearningApi.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./vocabularyLearningApi。
 * 验证场景：cross-page selection intents；keeps explicit identifiers as strings including values beyond the safe integer range；whole-filter selection stores exclusions rather than all 10000 IDs；draft selection survives refresh while preserving its filter and group size。
 */

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
