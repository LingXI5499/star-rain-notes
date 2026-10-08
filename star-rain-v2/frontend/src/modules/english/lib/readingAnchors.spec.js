import { webcrypto } from 'node:crypto'
import { beforeAll,describe,expect,it } from 'vitest'
import { applyAnchors,mobileParagraphOrder,readingBlocks,selectionAnchor,sha256,visibleText } from './readingAnchors'
beforeAll(()=>Object.defineProperty(globalThis,'crypto',{value:webcrypto,configurable:true}))
const make=html=>{const node=document.createElement('div');node.innerHTML=html;return node}
async function row(text,source,extra={}){return {paragraphIndex:0,rangeStart:0,rangeEnd:text.length,expectedText:text,paragraphHash:await sha256(text),sourceMarkdownHash:await sha256(source),status:'ACTIVE',groupKey:'one',...extra}}
describe('manual reading anchors',()=>{
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
