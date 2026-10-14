import MarkdownIt from 'markdown-it'

const parser = new MarkdownIt({ html: false })
/** Source maps belong to Markdown tokens; protected containers are never interpreted as prose. */
export function markdownBlocks(source) {
  const lines = String(source).split('\n')
  const protectedLines = new Set()
  let fence = null, math = false, group = false
  lines.forEach((line, index) => {
    const marker = line.match(/^ {0,3}(`{3,}|~{3,})/)
    if (fence) {
      protectedLines.add(index)
      if (marker && marker[1][0] === fence[0] && marker[1].length >= fence.length && !line.slice(marker[0].length).trim()) fence = null
      return
    }
    if (marker) { fence = marker[1]; protectedLines.add(index); return }
    if (/^\s*:::/.test(line)) { protectedLines.add(index); group = !group; return }
    if (group) { protectedLines.add(index); return }
    if (/^\s*\$\$\s*$/.test(line)) { protectedLines.add(index); math = !math; return }
    if (math || /^ {4}|^\t/.test(line)) protectedLines.add(index)
  })
  const tokens = parser.parse(source, {})
  const blocks = tokens.filter(token => token.level === 0 && token.map && ['paragraph_open', 'heading_open'].includes(token.type))
    .map(token => ({ start: token.map[0], end: token.map[1], level: token.type === 'heading_open' ? Number(token.tag.slice(1)) : 0 }))
    .filter(block => !Array.from({ length: block.end - block.start }, (_, index) => block.start + index).some(line => protectedLines.has(line)))
  return { lines, blocks, protectedLines, tokens }
}

/** A single contiguous replacement, preserving every byte outside explicitly selected ordinary blocks. */
export function headingEdit(source, start, end, level) {
  const { lines, blocks } = markdownBlocks(source)
  const firstLine = source.slice(0, start).split('\n').length - 1
  const lastLine = source.slice(0, end > start ? end - 1 : end).split('\n').length - 1
  const targets = blocks.filter(block => block.start <= lastLine && block.end > firstLine)
  if (!targets.length) throw new Error('请选择普通段落；代码、公式、表格和代码组不能转换为标题。')
  const from = targets[0].start, to = targets.at(-1).end
  const covered = new Set(targets.flatMap(block => Array.from({ length: block.end - block.start }, (_, i) => block.start + i)))
  if (lines.slice(from, to).some((line, i) => line.trim() && !covered.has(from + i))) throw new Error('选区包含复杂块，请分段操作。')
  const converted = lines.slice(from, to)
  for (const block of targets) {
    const offset = block.start - from
    const content = lines.slice(block.start, block.end).join('\n').replace(/^ {0,3}#{1,6}(?:\s+|$)/, '')
    if (block.end - block.start > 1) throw new Error('多行段落或 Setext 标题请先整理为单行再切换级别，原文未改变。')
    converted[offset] = `${'#'.repeat(level)}${level ? ' ' : ''}${content}`
  }
  const offset = lines.slice(0, from).reduce((size, line) => size + line.length + 1, 0)
  const old = lines.slice(from, to).join('\n')
  return { start: offset, end: offset + old.length, replacement: converted.join('\n') }
}

export function outlineSections(source) {
  const { blocks, lines } = markdownBlocks(source)
  return blocks.filter(block => block.level).map((block, index, all) => {
    const next = all.slice(index + 1).find(item => item.level <= block.level)
    return { ...block, title: lines[block.start].replace(/^\s*#{1,6}\s*/, '').replace(/\s+#+\s*$/, ''), sectionEnd: next?.start ?? lines.length }
  })
}
