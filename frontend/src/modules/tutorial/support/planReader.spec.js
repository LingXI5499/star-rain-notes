/**
 * planReader.spec.js：教程与学习模块的自动化测试。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./planReader。
 * 验证场景：current plan navigation；excludes ended plans and bounds active cards at five；continues from the first incomplete chapter at the normal tutorial URL。
 */

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
