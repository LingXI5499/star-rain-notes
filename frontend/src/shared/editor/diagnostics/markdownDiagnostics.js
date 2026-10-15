import { markdownBlocks } from '../commands/editorStructure'
import { markdownImages, tableCells } from '../commands/editorDocument'
import { parseCodeGroupLabels } from '../../markdown/markdownCodeGroup'
export function diagnoseMarkdown(source) {
  const { lines, protectedLines, tokens } = markdownBlocks(source),
    issues = [],
    titles = new Set()
  let previous = 0,
    fence = null,
    math = null,
    group = null
  const add = (rule, line, message, extra = {}) => {
    if (issues.length < 300) issues.push({ rule, line: line + 1, message, ...extra })
  }
  for (const token of tokens) {
    if (
      token.type === 'paragraph_open' &&
      token.map &&
      !protectedLines.has(token.map[0]) &&
      lines.slice(...token.map).join('\n').length > 1500
    )
      add('MD006', token.map[0], '连续段落超过 1500 字符，可按语义分段。')
    if (token.type === 'heading_open' && !protectedLines.has(token.map[0])) {
      const level = Number(token.tag.slice(1)),
        title = lines[token.map[0]].replace(/^\s*#{1,6}\s*/, '').trim()
      if (previous && level > previous + 1) add('MD001', token.map[0], '标题跨级，建议逐级组织。')
      previous = level
      if (titles.has(title)) add('MD007', token.map[0], '标题重复，请确认大纲含义。')
      titles.add(title)
    }
    if (token.type === 'table_open') {
      const rows = lines.slice(...token.map),
        columns = tableCells(rows[0]).length
      rows.forEach((line, index) => {
        if (tableCells(line).length !== columns)
          add('MD005', token.map[0] + index, '表格列数不一致，请检查转义管道符。')
      })
    }
  }
  lines.forEach((line, index) => {
    const marker = line.match(/^ {0,3}(`{3,}|~{3,})(.*)$/)
    if (fence) {
      if (
        marker &&
        marker[1][0] === fence.marker[0] &&
        marker[1].length >= fence.marker.length &&
        !marker[2].trim()
      )
        fence = null
      return
    }
    if (marker) {
      fence = { marker: marker[1], line: index }
      return
    }
    if (/^\s*:::/.test(line)) {
      if (group !== null) {
        group = null
      } else {
        group = index
      }
      return
    }
    if (math && ((math.kind === '$$' && /^\s*\$\$\s*$/.test(line)) || (math.kind === '\\[' && /^\s*\\\]\s*$/.test(line)))) {
      math = null
      return
    }
    if (!math && /^\s*(?:\$\$|\\\[)\s*$/.test(line)) {
      math = {line:index,kind:line.trim()}
      return
    }
    if (protectedLines.has(index)) return
    if (line.length > 500) add('MD006', index, '普通文本行超过 500 字符，可按语义分段。')
    if (/\]\(\s*(?:javascript|vbscript|data|file):/i.test(line))
      add('MD003', index, '链接使用不受支持的协议，请改用 HTTPS 或站内地址。')
    if (/\[[^\]\n]*\]\(\s*\)/.test(line)) add('MD003', index, '链接地址为空，请补充目标地址。')
  })
  if (fence) add('MD004', fence.line, '代码围栏未闭合。')
  if (math !== null) add('MD004', math.line, '块公式未闭合。')
  if (group !== null) add('MD009', group, '代码组容器未闭合。')
  for (const image of markdownImages(source)) {
    const line = source.slice(0, image.start).split('\n').length - 1
    if (!image.alt.trim()) add('MD002', line, '图片缺少替代文本。')
    const id = image.url.match(/\/api\/media\/assets\/(\d+)\/content/)?.[1]
    if (id) add('MD008', line, '站内素材权限待核实。', { mediaId: id })
  }
  // Only actual group delimiters outside fences participate in the completeness check.
  let inside = false,
    count = 0,
    start = 0,
    inFence = null,
    labels = []
  lines.forEach((line, index) => {
    const marker = line.match(/^ {0,3}(`{3,}|~{3,})(.*)$/)
    if (marker) {
      if (!inFence) {
        inFence = marker[1]
        if (inside) count++
      } else if (
        marker[1][0] === inFence[0] &&
        marker[1].length >= inFence.length &&
        !marker[2].trim()
      )
        inFence = null
      return
    }
    if (inFence) return
    if (/^\s*::: code-group(?:\s|$)/.test(line)) {
      inside = true
      count = 0
      start = index
      labels = parseCodeGroupLabels(line.trim().slice(4)) || []
    } else if (inside && /^\s*:::\s*$/.test(line)) {
      if (count < 2) add('MD009', start, '代码组需要至少两个完整代码块。')
      if (labels.length && labels.length !== count)
        add('MD009', start, '代码组标签数量与代码块数量不一致。')
      inside = false
    }
  })
  return issues.sort((a, b) => a.line - b.line)
}
