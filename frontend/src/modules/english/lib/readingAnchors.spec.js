import { webcrypto } from 'node:crypto'
import { beforeAll,describe,expect,it } from 'vitest'
import { applyAnchors,applyHoverSegments,clearMarks,mobileParagraphOrder,readingBlocks,selectionAnchor,sentenceRanges,sha256,visibleText } from './readingAnchors'
beforeAll(()=>Object.defineProperty(globalThis,'crypto',{value:webcrypto,configurable:true}))
const make=html=>{const node=document.createElement('div');node.innerHTML=html;return node}
async function row(text,source,extra={}){return {paragraphIndex:0,rangeStart:0,rangeEnd:text.length,expectedText:text,paragraphHash:await sha256(text),sourceMarkdownHash:await sha256(source),status:'ACTIVE',groupKey:'one',...extra}}
describe('manual reading anchors',()=>{
 it('segments unmapped prose inline while preserving emphasis, links, excluded code and UTF-16 selections',async()=>{
  const source='Hello **world**. Next 😀 [link](https://example.test).',root=make('<p>Hello <strong>world</strong>. Next 😀 <a href="https://example.test">link</a>.<code>skip</code></p><pre>Never highlight</pre>')
  applyHoverSegments(root,[],'EN')
  expect(new Set([...root.querySelectorAll('[data-rw-local-segment]')].map(n=>n.dataset.rwLocalSegment)).size).toBe(2)
  expect(root.querySelector('strong').textContent).toBe('world');expect(root.querySelector('a').getAttribute('href')).toBe('https://example.test')
  expect(root.querySelector('code [data-rw-local-segment]')).toBeNull();expect(root.querySelector('pre [data-rw-local-segment]')).toBeNull()
  const range=document.createRange();range.selectNodeContents(root.querySelector('strong'))
  const selected=await selectionAnchor(root,source,'EN',{isCollapsed:false,rangeCount:1,getRangeAt:()=>range})
  expect(selected.rangeStart).toBe(6);expect(selected.expectedText).toBe('world')
  clearMarks(root);expect(root.innerHTML).toBe('<p>Hello <strong>world</strong>. Next 😀 <a href="https://example.test">link</a>.<code>skip</code></p><pre>Never highlight</pre>')
 })
 it('keeps local hover spans outside real anchors and never saves fabricated mappings',async()=>{
  const root=make('<p>Mapped. Local.</p>'),source='Mapped. Local.',anchor=await row('Mapped.',source,{paragraphHash:await sha256(source)})
  const valid=await applyAnchors(root,[anchor],source);applyHoverSegments(root,valid,'EN')
  expect(root.querySelector('[data-rw-anchor] [data-rw-local-segment]')).toBeNull()
  expect(root.querySelector('[data-rw-local-segment]').textContent).toBe(' Local.')
  expect(valid).toHaveLength(1);expect(valid[0].groupKey).toBe('one')
  await applyAnchors(root,[anchor],source);applyHoverSegments(root,valid,'EN')
  expect(root.querySelectorAll('[data-rw-local-segment]')).toHaveLength(1)
 })
 it('protects decimal numbers and falls back to paragraphs for abbreviations or ambiguous quotes',()=>{
  const text='It costs 3.14 dollars. Next?'
  expect(sentenceRanges(text).map(r=>text.slice(r.start,r.end))).toEqual(['It costs 3.14 dollars.',' Next?'])
  for(const value of ['Dr. Lee is here. Next.','A. Smith arrived. Next.','He said “hello. Next.'])expect(sentenceRanges(value)).toEqual([{start:0,end:value.length}])
  const chinese='第一句。第二句！';expect(sentenceRanges(chinese,'ZH')).toEqual([{start:0,end:4},{start:4,end:8}])
 })
 it('orders real renderer wrappers as paragraph groups on mobile, including one-to-many translations',()=>{
  const en=make('<div><p>One.</p><p>Two.</p></div>'),zh=make('<div><p>一。</p><p>第一。</p><p>二。</p></div>')
  mobileParagraphOrder(en,zh,[{groupKey:'one',language:'EN',paragraphIndex:0},{groupKey:'one',language:'ZH',paragraphIndex:0},{groupKey:'one',language:'ZH',paragraphIndex:1},{groupKey:'two',language:'EN',paragraphIndex:1},{groupKey:'two',language:'ZH',paragraphIndex:2}])
  expect([...en.querySelectorAll('p')].map(p=>p.style.order)).toEqual(['0','2'])
  expect([...zh.querySelectorAll('p')].map(p=>p.style.order)).toEqual(['1','1','3'])
  expect(en.firstElementChild.hasAttribute('data-rw-flow')).toBe(true)
 })
 it('wraps text nodes across bold and links without damaging the original DOM',async()=>{
  const root=make('<p>Hello <strong>world</strong> <a href="https://example.test">today</a>.</p>'),source='Hello **world** [today](https://example.test).'
  const text=visibleText(root.querySelector('p')),anchor=await row(text,source)
  await applyAnchors(root,[anchor],source)
  expect(root.querySelectorAll('[data-rw-anchor]').length).toBe(5)
  expect(root.querySelector('strong').textContent).toBe('world');expect(root.querySelector('a').getAttribute('href')).toBe('https://example.test')
  expect(root.textContent).toBe(text);await applyAnchors(root,[anchor],source);expect(root.querySelectorAll('[data-rw-anchor]').length).toBe(5)
 })
 it('never highlights stale source, wrong ranges or changed paragraph hashes',async()=>{
  const source='Alpha.',root=make('<p>Alpha.</p>'),valid=await row(source,source)
  for(const invalid of [{...valid,status:'STALE'},{...valid,expectedText:'Wrong.'},{...valid,paragraphHash:await sha256('Beta.')},{...valid,sourceMarkdownHash:await sha256('old')},{...valid,rangeEnd:100}]){expect(await applyAnchors(root,[invalid],source)).toEqual([]);expect(root.querySelector('[data-rw-anchor]')).toBeNull()}
 })
 it('records UTF-16 offsets over formatted emoji selections and omits code/UI content',async()=>{
  const root=make('<p>A😀 <strong>bold</strong> end <code>skip</code><button>Copy</button></p><pre>code</pre>')
  const p=root.querySelector('p'),range=document.createRange();range.setStart(p.firstChild,1);range.setEnd(p.querySelector('strong').firstChild,4)
  const selection={isCollapsed:false,rangeCount:1,getRangeAt:()=>range},anchor=await selectionAnchor(root,'A😀 **bold** end','EN',selection)
  expect(anchor.rangeStart).toBe(1);expect(anchor.rangeEnd).toBe(8);expect(anchor.expectedText).toBe('😀 bold');expect(readingBlocks(root)).toHaveLength(1)
  expect(visibleText(p)).toBe('A😀 bold end ')
 })
 it('preserves overlapping notes and one-to-many alignment identifiers on shared text',async()=>{
  const root=make('<p>One two.</p>'),source='One two.',base=await row(source,source)
  await applyAnchors(root,[base,{...base,groupKey:'two',rangeStart:4,expectedText:'two.',analysisMarkdown:'note',id:'17'}],source)
  expect(root.querySelector('[data-notes="17"]').textContent).toBe('two.')
  expect(root.querySelector('[data-notes="17"]').dataset.groups).toBe('one two')
 })
})
