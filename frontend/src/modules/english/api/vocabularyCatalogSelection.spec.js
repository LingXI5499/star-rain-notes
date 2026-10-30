import { afterEach, expect, it } from 'vitest'
import { createSelection, loadSelection, saveSelection, toggleSelected } from './vocabularyLearningApi'
afterEach(() => sessionStorage.clear())
it('persists cross-topic selections without a legacy theme filter and retains historical theme drafts', () => {
  const selection = toggleSelected(toggleSelected(createSelection(null), '7'), '8')
  saveSelection(selection)
  expect(loadSelection('')).toMatchObject({ themeId: null, wordIds: ['7', '8'], selectAllMatched: false })
  const old = toggleSelected(createSelection('3'), '10'); saveSelection(old)
  expect(loadSelection('3')).toMatchObject({ themeId: '3', wordIds: ['10'] })
  expect(loadSelection(null).wordIds).toEqual(['7', '8'])
})
it('keeps the current library draft global when its filter uses a preserved source theme', () => {
  const selection = {...createSelection('3'),selectAllMatched:true}
  saveSelection(selection, null)
  expect(loadSelection(null)).toMatchObject({themeId:'3',selectAllMatched:true})
  expect(loadSelection('3')).toMatchObject({themeId:'3',selectAllMatched:false})
})
