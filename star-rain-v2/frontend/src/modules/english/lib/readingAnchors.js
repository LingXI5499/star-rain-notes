// Range and string offsets use UTF-16. Code, math and interface text have no anchors.
const omitted = 'pre,code,button,mjx-container,.katex,[aria-hidden="true"]'
export function textNodes(block) {
 const walk = document.createTreeWalker(block, NodeFilter.SHOW_TEXT), nodes = []
 let node
 while ((node = walk.nextNode())) if (!node.parentElement?.closest(omitted)) nodes.push(node)
 return nodes
}
export function visibleText(block) { return textNodes(block).map(n => n.data).join('') }
export function readingBlocks(root) {
 return [...root.querySelectorAll('p,li,h1,h2,h3,h4,h5,h6,td,th')].filter(n => !n.closest(omitted) && !n.querySelector('p,li,h1,h2,h3,h4,h5,h6,td,th'))
}
export async function sha256(text) {
 const bytes = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(text || ''))
 return Array.from(new Uint8Array(bytes), b => b.toString(16).padStart(2, '0')).join('')
}
export async function selectionAnchor(root, markdown, language, selection = window.getSelection()) {
 if (!selection || selection.isCollapsed || !selection.rangeCount) throw new Error('请先选择一段正文。')
 const range = selection.getRangeAt(0), blocks = readingBlocks(root)
 const index = blocks.findIndex(b => b.contains(range.startContainer) && b.contains(range.endContainer))
 if (index < 0) throw new Error('请在同一个正文段落内选择文本。')
 const block = blocks[index], prefix = range.cloneRange(); prefix.selectNodeContents(block);prefix.setEnd(range.startContainer, range.startOffset)
 const box = document.createElement('div');box.append(prefix.cloneContents());const start = visibleText(box).length
 const selected = document.createElement('div');selected.append(range.cloneContents());const expectedText = visibleText(selected)
 if (!expectedText.trim()) throw new Error('选区没有可用正文。')
 return { language, paragraphIndex: index, rangeStart: start, rangeEnd: start + expectedText.length, expectedText, paragraphHash: await sha256(visibleText(block)), sourceMarkdownHash: await sha256(markdown) }
}
export async function applyAnchors(root, rows, markdown) {
 for (const span of root.querySelectorAll('[data-rw-anchor]')) span.replaceWith(...span.childNodes)
 root.normalize()
 const blocks = readingBlocks(root), sourceHash = await sha256(markdown), valid = []
 for (const row of rows) {
  const block = blocks[row.paragraphIndex]
  if (row.status !== 'ACTIVE' || !block || row.sourceMarkdownHash !== sourceHash) continue
  const text = visibleText(block)
  if (!Number.isInteger(row.rangeStart) || row.rangeStart < 0 || row.rangeEnd <= row.rangeStart || row.rangeEnd > text.length || text.slice(row.rangeStart, row.rangeEnd) !== row.expectedText || await sha256(text) !== row.paragraphHash) continue
  valid.push(row)
 }
 for (let index = 0; index < blocks.length; index++) {
  const matches = valid.filter(r => r.paragraphIndex === index)
  if (!matches.length) continue
  let offset = 0
  for (const node of textNodes(blocks[index])) {
   const start = offset, end = start + node.length;offset = end
   const boundaries = [...new Set([start,end,...matches.flatMap(r => [Math.max(start,r.rangeStart),Math.min(end,r.rangeEnd)]).filter(p => p > start && p < end)])].sort((a,b) => a-b)
   const fragment = document.createDocumentFragment()
   for (let n = 1; n < boundaries.length; n++) {
    const a = boundaries[n-1], b = boundaries[n], active = matches.filter(r => r.rangeStart < b && r.rangeEnd > a)
    const text = document.createTextNode(node.data.slice(a-start,b-start))
    if (!active.length) fragment.append(text)
    else { const span = document.createElement('span');span.dataset.rwAnchor = '';span.tabIndex = 0
     span.dataset.groups = active.map(r => r.groupKey).filter(Boolean).join(' ')
     span.dataset.notes = active.filter(r => r.analysisMarkdown).map(r => r.id).join(' ')
     span.className = 'rw-anchor'; if (span.dataset.notes) span.classList.add('rw-anchor--note')
     span.setAttribute('aria-label', span.dataset.notes ? '查看精选精读' : '双语对应片段');span.append(text);fragment.append(span)
    }
   }
   node.replaceWith(fragment)
  }
 }
 return valid
}
export function mobileParagraphOrder(enRoot, zhRoot, rows) {
 const flow = root => {
  while (root.children.length===1 && root.firstElementChild.tagName==='DIV' && !root.firstElementChild.className) {
   root=root.firstElementChild;root.dataset.rwFlow=''
  }
  return root
 }
 enRoot=flow(enRoot);zhRoot=flow(zhRoot)
 const en = readingBlocks(enRoot), zh = readingBlocks(zhRoot), groups = new Map()
 for (const r of rows) { const g = groups.get(r.groupKey) || { EN:[], ZH:[] };g[r.language]?.push(r.paragraphIndex);groups.set(r.groupKey,g) }
 for (const [index, block] of [...enRoot.children].entries()) block.style.order = String(index * 2)
 for (const [index, block] of [...zhRoot.children].entries()) block.style.order = String(enRoot.children.length * 2 + index * 2 + 1)
 for (const g of groups.values()) {
  if (!g.EN.length || !g.ZH.length) continue
  const a = en[Math.max(...g.EN)]; let top = a;while (top?.parentElement !== enRoot && top?.parentElement) top = top.parentElement
  const order = [...enRoot.children].indexOf(top) * 2 + 1
  for (const i of g.ZH) { let block = zh[i];while (block?.parentElement !== zhRoot && block?.parentElement) block = block.parentElement
   if (block?.parentElement === zhRoot) block.style.order = String(order)
  }
 }
}
