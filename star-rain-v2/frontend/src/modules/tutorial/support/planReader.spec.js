import { describe, expect, it } from 'vitest'
import { currentPlans, planChapterRoute, nextPlanChapter } from './planReader'
describe('current plan navigation', () => {
 it('excludes ended plans and bounds active cards at five', () => {
  const rows = [{status:'COMPLETED'}, {status:'CANCELLED'}, ...Array.from({length:6}, () => ({status:'ACTIVE'}))]
  expect(currentPlans(rows)).toHaveLength(5); expect(currentPlans([{status:'PAUSED'}, {status:'DRAFT'}])).toHaveLength(2)
 })
 it('continues from the first incomplete chapter at the normal tutorial URL', () => {
  const plan = {id:'p',tutorialSlug:'course',chapters:[{chapterId:'1',chapterSlug:'done',completed:true},{chapterId:'2',chapterSlug:'next',completed:false}]}
  expect(planChapterRoute(plan,nextPlanChapter(plan))).toEqual({path:'/useradmin/tutorials/course/next',query:{plan:'p'}})
  expect(() => planChapterRoute(plan, {chapterId:'withdrawn'})).toThrow('章节已撤回')
 })
})
