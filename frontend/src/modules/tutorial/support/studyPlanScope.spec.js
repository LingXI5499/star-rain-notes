import { describe, expect, it } from 'vitest'
import { chapterPlanRestriction, selectedScopeChapters } from './studyPlanScope'
describe('study plan scope eligibility', () => {
 it('requires published enabled cards but permits zero questions', () => {
  expect(chapterPlanRestriction({ id: '1', cardCount: 0, questionCount: 2 }).reason).toContain('知识卡片')
  expect(chapterPlanRestriction({ id: '1', cardCount: 1, questionCount: 0 })).toBeNull()
 })
 it('blocks chapters in unended plans, while excluding the draft being edited and ended plans', () => {
  for (const status of ['DRAFT', 'ACTIVE', 'PAUSED']) {
   const plans = [{ id: 'p1', name: 'Existing', status, chapters: [{ chapterId: 1 }] }]
   expect(chapterPlanRestriction({ id: '1', cardCount: 2 }, plans).planId).toBe('p1')
   expect(chapterPlanRestriction({ id: '1', cardCount: 2 }, plans, 'p1')).toBeNull()
  }
  for (const status of ['COMPLETED', 'CANCELLED']) expect(chapterPlanRestriction({ id: '1', cardCount: 2 }, [{ id: 'p1', status, chapters: [{ chapterId: '1' }] }])).toBeNull()
 })
 it('expands overlapping whole-group and individual selections once', () => {
  const groups = [{ id: 2, chapters: [{ id: 1 }, { id: 3 }] }]
  expect(selectedScopeChapters(groups, { groupIds: ['2'], chapterIds: ['1'] })).toEqual([{ id: 1 }, { id: 3 }])
 })
})
