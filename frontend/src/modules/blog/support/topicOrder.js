/**
 * topicOrder.js：博客模块的业务支持工具。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：reorderTopicIds（调整顺序专题标识集合）。
 */

export
  /**
   * 调整顺序专题标识集合。
   * 主要协作：ids.indexOf、ordered.splice。
   * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
   * @param draggedId dragged标识；默认值、解构字段与可选参数见函数签名。
   * @param targetId 被处理或审核的目标对象标识；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function reorderTopicIds(ids, draggedId, targetId) {
  const from = ids.indexOf(draggedId)
  const to = ids.indexOf(targetId)
  if (from < 0 || to < 0 || from === to) return [...ids]
  const ordered = [...ids]
  ordered.splice(from, 1)
  ordered.splice(to, 0, draggedId)
  return ordered
}
