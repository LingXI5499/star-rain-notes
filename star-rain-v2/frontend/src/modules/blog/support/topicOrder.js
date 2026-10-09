export function reorderTopicIds(ids, draggedId, targetId) {
  const from = ids.indexOf(draggedId)
  const to = ids.indexOf(targetId)
  if (from < 0 || to < 0 || from === to) return [...ids]
  const ordered = [...ids]
  ordered.splice(from, 1)
  ordered.splice(to, 0, draggedId)
  return ordered
}
