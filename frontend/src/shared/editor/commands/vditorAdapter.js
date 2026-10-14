import { headingEdit, markdownBlocks, markdownSemantics } from './editorStructure'
import { normalizeVditorMarkdown } from './editorDocument'

const escape = value => value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
const clean = value => value.replace(/\u200b/g, '')
function elementOf(node) { return node?.nodeType === 1 ? node : node?.parentElement }

/** All version-specific toolbar selectors are contained here; content changes use public Vditor APIs. */
export function createVditorAdapter(getEditor, getHost) {
  let saved = null, snapshot = null, changingMode = false
  function selection() {
    const selection = window.getSelection()
    if (!selection?.rangeCount) return null
    const range = selection.getRangeAt(0), element = elementOf(range.startContainer)?.closest('[contenteditable="true"]')
    if (!element || !getHost()?.contains(element) || !element.contains(range.endContainer)) return null
    return { range, element }
  }
  function restore(range = saved) {
    if (!range) return false
    if (!getHost()?.contains(range.startContainer) || !getHost()?.contains(range.endContainer)) {
      if (range === saved && snapshot && getHost()?.contains(snapshot.element) && clean(snapshot.element.textContent) === snapshot.text) {
        selectOffsets(snapshot.element,snapshot.start,snapshot.end); return true
      }
      return false
    }
    const element = elementOf(range.startContainer)?.closest('[contenteditable="true"]')
    element?.focus()
    if (!getHost()?.contains(range.startContainer)) {
      if (range === saved && snapshot && clean(element.textContent) === snapshot.text) { selectOffsets(element,snapshot.start,snapshot.end); return true }
      return false
    }
    const selection = window.getSelection(); selection.removeAllRanges(); selection.addRange(range.cloneRange())
    return true
  }
  function mode() { return getHost()?.querySelector('[data-mode].vditor-menu--current')?.dataset.mode || 'ir' }
  function sourcePosition() {
    const current = selection()
    if (!current || mode() !== 'sv') throw new Error('此操作请在源码模式中执行。')
    const source = getEditor().getValue(), raw = clean(current.element.textContent)
    if (raw.trimEnd() !== source.trimEnd()) throw new Error('当前源码选区无法可靠定位，请重新选择；原文未改变。')
    const prefix = current.range.cloneRange(); prefix.selectNodeContents(current.element); prefix.setEnd(current.range.startContainer, current.range.startOffset)
    const end = prefix.cloneRange(); end.setEnd(current.range.endContainer, current.range.endOffset)
    return { ...current, source, start: clean(prefix.toString()).length, end: clean(end.toString()).length }
  }
  function selectOffsets(element, start, end) {
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
    let node, offset = 0, from, to
    while ((node = walker.nextNode())) {
      const size = clean(node.textContent).length
      function rawOffset(value) {
        let count = 0, index = 0
        while (index < node.textContent.length && count < value) { if (node.textContent[index] !== '\u200b') count++; index++ }
        return index
      }
      if (!from && start <= offset + size) from = [node, rawOffset(Math.max(0, start - offset))]
      if (end <= offset + size) { to = [node, rawOffset(Math.max(0, end - offset))]; break }
      offset += size
    }
    if (!from || !to) throw new Error('选区已经变化，请重新操作。')
    const range = document.createRange(); range.setStart(...from); range.setEnd(...to); restore(range)
    return range
  }
  function replace(edit) {
    const current = sourcePosition()
    selectOffsets(current.element, edit.start, edit.end)
    getEditor().updateValue(`<span style="white-space:pre-wrap">${escape(edit.replacement)}</span>`)
  }
  function button(selector) {
    const target = getHost()?.querySelector(selector)
    if (!target || target.classList.contains('vditor-menu--disabled')) throw new Error('此处暂不支持该格式操作。')
    target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  }
  function setMode(value) {
    const previous = mode(), source = getEditor().getValue()
    if (previous === value) return
    changingMode = true
    try {
      button(`.vditor-toolbar [data-mode="${value}"]`)
      if (markdownSemantics(normalizeVditorMarkdown(getEditor().getValue())) !== markdownSemantics(normalizeVditorMarkdown(source))) {
        button(`.vditor-toolbar [data-mode="${previous}"]`); getEditor().setValue(source)
        throw new Error('此文档包含当前 Vditor 模式转换无法完整保留的语法，已恢复原文；请使用原视图或阅读预览。')
      }
      saved = null; snapshot = null
    } finally { changingMode = false }
  }
  function heading(level) {
    const current = selection()
    if (!current) return false
    if (mode() === 'sv') { const position = sourcePosition(); replace(headingEdit(position.source, position.start, position.end, level)); return true }
    const start = elementOf(current.range.startContainer)?.closest('p,h1,h2,h3,h4,h5,h6')
    const end = elementOf(current.range.endContainer)?.closest('p,h1,h2,h3,h4,h5,h6')
    const container = start?.closest('pre,table,blockquote,li')
    if (!start || start !== end || (container && container !== current.element) || start.querySelector('[data-type*="math"],[data-type*="code-block"]')) {
      throw new Error('请选择单个普通段落；跨段落操作请切换源码模式。')
    }
    const { lines, protectedLines } = markdownBlocks(getEditor().getValue())
    const text = clean(start.textContent).replace(/^\s*#{1,6}\s*/, '').trim()
    if (lines.some((line, index) => protectedLines.has(index) && line.replace(/^\s*#{1,6}\s*/, '').trim() === text)) throw new Error('代码组、代码或公式中的文本不能转换标题。')
    if (level) button(`.vditor-toolbar [data-tag="h${level}"]`)
    else if (/^H[1-6]$/.test(start.tagName)) button('.vditor-toolbar [data-type="headings"]')
    return true
  }
  return {
    selection, sourcePosition, replace, mode, heading, restore, selectOffsets,
    remember() {
      const current = selection()
      if (current) {
        saved = current.range.cloneRange()
        const prefix = saved.cloneRange(); prefix.selectNodeContents(current.element); prefix.setEnd(saved.startContainer,saved.startOffset)
        const end = prefix.cloneRange(); end.setEnd(saved.endContainer,saved.endOffset)
        snapshot = {element:current.element,text:clean(current.element.textContent),start:clean(prefix.toString()).length,end:clean(end.toString()).length}
      }
      return saved
    },
    reset() { saved = null; snapshot = null },
    markdown() { return getEditor()?.getValue() || '' },
    markdownSelection() {
      const current = selection()
      if (current && mode() === 'wysiwyg') {
        const fragment = document.createElement('div'); fragment.append(current.range.cloneContents())
        return getEditor().html2md(fragment.innerHTML).trimEnd()
      }
      return getEditor()?.getSelection() || ''
    },
    shortcutTips(keys) { for (let level = 1; level <= 6; level++) { const item = getHost()?.querySelector(`[data-tag="h${level}"]`); if (item) item.textContent = `H${level} · ${keys[`heading${level}`]}` } },
    command(name) { button(`.vditor-toolbar [data-type="${name}"]`) },
    setMode,
    interceptMode(event) {
      const target = event.target.closest?.('.vditor-toolbar [data-mode]')
      if (!target || changingMode) return false
      event.preventDefault(); event.stopImmediatePropagation(); setMode(target.dataset.mode); return true
    },
    insert(value, range) { if (range && !restore(range)) throw new Error('文档选区已变化，请重新插入。'); getEditor()?.insertMD(value) },
  }
}
