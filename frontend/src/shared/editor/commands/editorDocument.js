import MarkdownIt from 'markdown-it'
import { markdownBlocks, outlineSections } from './editorStructure'
import { resolveMarkdownImageSize, stripMarkdownImageSizeToken } from '../../markdown/markdownImageSize'

const parser = new MarkdownIt({ html: false })
export function lineOffset(source, line) { return Math.min(source.length, source.split('\n').slice(0, line).reduce((length, value) => length + value.length + 1, 0)) }
export function tableCells(line) {
  const value = line.trim(), cells = []; let start = 0, slashes = 0, ticks = 0
  for (let index = 0; index < value.length; index++) {
    if (value[index] === '`' && slashes % 2 === 0) {
      const count = value.slice(index).match(/^`+/)[0].length
      if (!ticks) ticks = count; else if (ticks === count) ticks = 0
      index += count - 1; slashes = 0; continue
    }
    if (value[index] === '|' && slashes % 2 === 0 && !ticks) { cells.push(value.slice(start,index).trim()); start = index + 1 }
    slashes = value[index] === '\\' ? slashes + 1 : 0
  }
  cells.push(value.slice(start).trim())
  if (value.startsWith('|')) cells.shift()
  if (start === value.length) cells.pop()
  return cells
}
/** Lute permits unescaped pipes inside code cells; public GFM readers require escaped pipes. */
export function normalizeVditorMarkdown(source) {
  const { lines, tokens, protectedLines } = markdownBlocks(source)
  for (const table of tokens.filter(token => token.type === 'table_open' && token.map)) {
    for (let line = table.map[0]; line < table.map[1]; line++) {
      if (protectedLines.has(line)) continue
      const input = lines[line]; let output = '', ticks = 0, slashes = 0
      for (let index = 0; index < input.length; index++) {
        const char = input[index]
        if (char === '`' && slashes % 2 === 0) {
          const count = input.slice(index).match(/^`+/)[0].length
          if (!ticks) ticks = count; else if (ticks === count) ticks = 0
          output += '`'.repeat(count); index += count - 1; slashes = 0; continue
        }
        if (char === '|' && ticks && slashes % 2 === 0) output += '\\'
        output += char; slashes = char === '\\' ? slashes + 1 : 0
      }
      lines[line] = output
    }
  }
  return lines.join('\n')
}
export function tableAt(source, offset) {
  const line = source.slice(0, offset).split('\n').length - 1, tokens = markdownBlocks(source).tokens
  const index = tokens.findIndex(token => token.type === 'table_open' && token.map[0] <= line && token.map[1] > line)
  if (index < 0) throw new Error('请在源码模式将光标放到一个合法 Markdown 表格内。')
  const alignments = []
  for (const token of tokens.slice(index + 1)) {
    if (token.type === 'table_close') break
    if (token.type === 'th_open') alignments.push(token.attrGet('style')?.split(':')[1] || 'left')
  }
  const map = tokens[index].map
  const rows = source.split('\n').slice(...map).filter((line,index) => index !== 1).map(tableCells)
  if (!rows.length || rows.some(row => row.length !== rows[0].length) || rows.length > 51 || rows[0].length > 20) throw new Error('表格结构或尺寸不受支持，原文未改变。')
  return { start: lineOffset(source, map[0]), end: lineOffset(source, map[1]), rows, alignments, row: line === map[0] ? 0 : Math.max(1, line - map[0] - 1) }
}
export function serializeTable(rows, alignments) {
  if (rows.length > 51 || !rows.length || rows[0].length > 20 || !rows[0].length || rows.some(row => row.length !== rows[0].length)) throw new Error('表格最多 20 列、50 数据行，且至少有一个单元格。')
  if (rows.some(row => row.some(cell => /[\r\n]/.test(cell)))) throw new Error('不支持多行单元格，请先整理为单行。')
  const safeCell = cell => cell.replace(/(\\*)\|/g, (pipe,slashes) => slashes.length % 2 ? pipe : slashes + '\\|')
  const output = row => `| ${row.map(safeCell).join(' | ')} |`
  const separator = alignments.map(value => value === 'center' ? ':---:' : value === 'right' ? '---:' : ':---')
  return [output(rows[0]), output(separator), ...rows.slice(1).map(output)].join('\n') + '\n'
}
export function editTable(table, command, selectedColumn = 0) {
  const rows = table.rows.map(row => [...row]), alignments = [...table.alignments]
  const row = Math.min(table.row, rows.length - 1), column = Math.max(0, Math.min(selectedColumn, rows[0].length - 1))
  if (command === 'row-before' || command === 'row-after') rows.splice(Math.max(1, row + (command === 'row-after' ? 1 : 0)), 0, rows[0].map(() => ''))
  else if (command === 'remove-row') { if (!row) throw new Error('表头不可删除；请选择数据行。'); rows.splice(row, 1) }
  else if (command === 'column-before' || command === 'column-after') { const index = column + (command === 'column-after' ? 1 : 0); rows.forEach(row => row.splice(index, 0, '')); alignments.splice(index, 0, 'left') }
  else if (command === 'remove-column') { rows.forEach(row => row.splice(column, 1)); alignments.splice(column, 1) }
  else if (['left', 'center', 'right'].includes(command)) alignments[column] = command
  else throw new Error('未知表格操作。')
  return { start: table.start, end: table.end, replacement: serializeTable(rows, alignments) }
}
export function tsvTable(text) {
  const rows = text.replace(/\r\n?/g, '\n').replace(/\n$/, '').split('\n').map(line => line.split('\t').map(cell => cell.replace(/\\/g, '\\\\').replace(/\|/g, '\\|')))
  if (rows[0].length < 2 || rows.some(row => row.length !== rows[0].length)) throw new Error('TSV 各行列数必须一致，且至少两列。')
  return serializeTable(rows, rows[0].map(() => 'left'))
}
export function markdownImages(source) {
  const { protectedLines } = markdownBlocks(source), images = []
  let index = 0
  while (index < source.length) {
    if (source[index] === '`') {
      const fence = source.slice(index).match(/^`+/)[0], end = source.indexOf(fence, index + fence.length)
      index = end < 0 ? index + fence.length : end + fence.length; continue
    }
    if (source.slice(index, index + 2) !== '![' || (index && source[index - 1] === '\\')) { index++; continue }
    const start = index, line = source.slice(0, index).split('\n').length - 1
    if (protectedLines.has(line)) { index++; continue }
    let depth = 1; index += 2
    while (index < source.length && depth) { if (source[index] === '\\') { index += 2; continue } if (source[index] === '[') depth++; if (source[index] === ']') depth--; index++ }
    if (source[index] !== '(') continue
    let quote = null; depth = 1; index++
    while (index < source.length && depth) {
      const char = source[index]
      if (char === '\\') { index += 2; continue }
      if (quote) { if (char === quote) quote = null }
      else if (char === '"' || char === "'") quote = char
      else if (char === '(') depth++
      else if (char === ')') depth--
      index++
    }
    if (depth) continue
    const raw = source.slice(start, index), children = parser.parseInline(raw, {})[0]?.children
    if (children?.length !== 1 || children[0].type !== 'image') continue
    const token = children[0], title = token.attrGet('title') || ''
    images.push({ start, end: index, raw, alt: token.content, url: token.attrGet('src'), title: stripMarkdownImageSizeToken(title), size: resolveMarkdownImageSize(title) })
  }
  return images
}
export function imageEdit(image, fields) {
  const url = fields.url.trim()
  if (!url || (!/^https?:\/\//i.test(url) && !/^\/(?!\/)/.test(url)) || /[\s<>"\r\n]/.test(url)) throw new Error('图片地址必须是站内路径或 HTTP(S) 地址。')
  if (!['normal','wide','full'].includes(fields.size)) throw new Error('请选择现有图片尺寸。')
  const alt = fields.alt.replace(/\\/g, '\\\\').replace(/([\[\]])/g, '\\$1').replace(/[\r\n]/g, ' ')
  const title = [fields.title.trim(), fields.size].filter(Boolean).join(' ').replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/[\r\n]/g, ' ')
  return { start: image.start, end: image.end, replacement: `![${alt}](${url.replace(/[()]/g,encodeURIComponent)} "${title}")` }
}
export function moveSection(source, selectedLine, direction) {
  const headings = outlineSections(source), index = headings.findIndex(item => item.start === selectedLine), current = headings[index]
  if (!current) throw new Error('章节已变化，请重新选择。')
  let other
  if (direction === 'up') other = headings.slice(0,index).reverse().find(item => item.level <= current.level)
  else other = headings.slice(index + 1).find(item => item.level <= current.level)
  if (!other || other.level !== current.level || (direction === 'up' ? other.sectionEnd !== current.start : current.sectionEnd !== other.start)) throw new Error('只能与同一层级相邻章节交换。')
  const first = direction === 'up' ? other : current, second = direction === 'up' ? current : other
  const start = lineOffset(source, first.start), middle = lineOffset(source, second.start), end = lineOffset(source, second.sectionEnd)
  const part = source.slice(middle, end)
  return { start, end, replacement: part + (part.endsWith('\n') ? '' : '\n') + source.slice(start, middle) }
}
