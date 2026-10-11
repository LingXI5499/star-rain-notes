/**
 * planReader.js：教程与学习模块的业务支持工具。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：../../../shared/viewMode。
 * 阅读入口：currentPlans（当前Plans的处理函数）；nextPlanChapter（下一项计划章节的处理函数）；planChapterRoute（计划章节路由的处理函数）；scopedGroups（scoped分组列表的处理函数）。
 */

import { accountPath } from '../../../shared/viewMode'

export const activePlanStatuses = ['DRAFT', 'ACTIVE', 'PAUSED']
/**
 * 当前Plans的处理函数。
 * 主要协作：.slice、plans.filter、activePlanStatuses.includes。
 * @param plans plans；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const currentPlans = (plans) => plans.filter(p => activePlanStatuses.includes(p.status)).slice(0, 5)
/**
 * 下一项计划章节的处理函数。
 * 主要协作：plan.chapters.find。
 * @param plan 计划；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const nextPlanChapter = (plan) => plan.chapters?.find(c => !c.completed)
export
  /**
   * 计划章节路由的处理函数。
   * 主要协作：accountPath。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @param plan 计划；默认值、解构字段与可选参数见函数签名。
   * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function planChapterRoute(plan, chapter) {
 if (!plan.tutorialSlug || !chapter?.chapterSlug) throw new Error('章节已撤回或尚未公开，暂不能开始学习。')
 return { path: accountPath(`/tutorials/${plan.tutorialSlug}/${chapter.chapterSlug}`), query: { plan: String(plan.id) } }
}
export
  /**
   * scoped分组列表的处理函数。
   * 主要协作：.map、.filter、members.has。
   * @param tutorial 教程；默认值、解构字段与可选参数见函数签名。
   * @param plan 计划；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function scopedGroups(tutorial, plan) {
 if (!plan) return tutorial.groups || []
 const members = new Set((plan.chapters || []).map(c => String(c.chapterId)))
 return (tutorial.groups || []).map(g => ({ ...g, chapters: (g.chapters || []).filter(c => members.has(String(c.id))) })).filter(g => g.chapters.length)
}
