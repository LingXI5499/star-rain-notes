/** Merge vocabulary API layers like「基础通用词层A/B」into parent families for UI filters. */

const LAYER_SUFFIX = /层[ABC]$/

export function vocabularyFamilyName(layer: string): string {
  const trimmed = layer.trim()
  return LAYER_SUFFIX.test(trimmed) ? trimmed.replace(LAYER_SUFFIX, '') : trimmed
}

/** Short tag for a card, e.g.「基础通用词层B」→「层B」. */
export function vocabularyLayerTag(layer: string): string {
  const match = layer.trim().match(/层([ABC])$/)
  return match ? `层${match[1]}` : layer.trim()
}
