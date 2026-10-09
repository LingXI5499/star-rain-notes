import { accountPath } from '../../../shared/viewMode'

export const activePlanStatuses = ['DRAFT', 'ACTIVE', 'PAUSED']
export const currentPlans = (plans) => plans.filter(p => activePlanStatuses.includes(p.status)).slice(0, 5)
export const nextPlanChapter = (plan) => plan.chapters?.find(c => !c.completed)
export function planChapterRoute(plan, chapter) {
 if (!plan.tutorialSlug || !chapter?.chapterSlug) throw new Error('章节已撤回或尚未公开，暂不能开始学习。')
 return { path: accountPath(`/tutorials/${plan.tutorialSlug}/${chapter.chapterSlug}`), query: { plan: String(plan.id) } }
}
export function scopedGroups(tutorial, plan) {
 if (!plan) return tutorial.groups || []
 const members = new Set((plan.chapters || []).map(c => String(c.chapterId)))
 return (tutorial.groups || []).map(g => ({ ...g, chapters: (g.chapters || []).filter(c => members.has(String(c.id))) })).filter(g => g.chapters.length)
}
