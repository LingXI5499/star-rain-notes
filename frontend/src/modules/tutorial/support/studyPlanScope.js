const contains = (ids, id) => (ids || []).some(value => String(value) === String(id))

export function selectedScopeChapters(groups, scope) {
 return (groups || []).flatMap(group => (group.chapters || []).filter(chapter =>
  scope.entireTutorial || contains(scope.groupIds, group.id) || contains(scope.chapterIds, chapter.id)))
}

export function chapterPlanRestriction(chapter, plans = [], excludePlanId) {
 if (!(Number(chapter.cardCount) > 0)) return { reason: '尚无启用且已发布的知识卡片，可先阅读章节。' }
 const conflict = plans.find(plan => ['DRAFT', 'ACTIVE', 'PAUSED'].includes(plan.status)
  && String(plan.id) !== String(excludePlanId)
  && (plan.chapters || []).some(item => String(item.chapterId) === String(chapter.id)))
 return conflict ? { reason: `已在未结束计划「${conflict.name}」中。`, planId: conflict.id } : null
}
