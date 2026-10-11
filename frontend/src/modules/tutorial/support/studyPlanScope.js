/**
 * studyPlanScope.js：教程与学习模块的业务支持工具。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：contains（contains的处理函数）；selectedScopeChapters（选中Scope章节列表的处理函数）；chapterPlanRestriction（章节计划Restriction的处理函数）。
 */

/**
 * contains的处理函数。
 * 主要协作：.some。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const contains = (ids, id) => (ids || []).some(value => String(value) === String(id))

export
  /**
   * 选中Scope章节列表的处理函数。
   * 主要协作：.flatMap、.filter、contains。
   * @param groups 分组列表；默认值、解构字段与可选参数见函数签名。
   * @param scope scope；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function selectedScopeChapters(groups, scope) {
 return (groups || []).flatMap(group => (group.chapters || []).filter(chapter =>
  scope.entireTutorial || contains(scope.groupIds, group.id) || contains(scope.chapterIds, chapter.id)))
}

export
  /**
   * 章节计划Restriction的处理函数。
   * 主要协作：plans.find、.includes、.some。
   * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
   * @param plans = [] plans；默认值、解构字段与可选参数见函数签名。
   * @param excludePlanId exclude计划标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function chapterPlanRestriction(chapter, plans = [], excludePlanId) {
 if (!(Number(chapter.cardCount) > 0)) return { reason: '尚无启用且已发布的知识卡片，可先阅读章节。' }
 const conflict = plans.find(plan => ['DRAFT', 'ACTIVE', 'PAUSED'].includes(plan.status)
  && String(plan.id) !== String(excludePlanId)
  && (plan.chapters || []).some(item => String(item.chapterId) === String(chapter.id)))
 return conflict ? { reason: `已在未结束计划「${conflict.name}」中。`, planId: conflict.id } : null
}
