import { describe, expect, it } from 'vitest'
import { bilingualDocument, bilingualRows } from './bilingualBlocks'

describe('authored bilingual correspondence',()=>{
 it('accepts empty database fields',()=>{expect(bilingualRows(null,undefined).rows).toEqual([])})
 it('pairs each authored sentence without guessing punctuation',()=>{
  const {rows}=bilingualRows('雨停了。\n\n我们出发。','The rain stopped.\n\nWe set off.')
  expect(rows).toHaveLength(2);expect(rows[0].chinese).toContain('雨停了。');expect(rows[0].english).toContain('The rain stopped.');expect(rows[1].english).toContain('We set off.')
 })
 it('keeps missing translations visible',()=>{expect(bilingualRows('一句。','One.\n\nTwo.').rows[1].chinese).toBe('')})
 it('pairs sentences inside ordinary paragraphs even when the translation uses separate paragraphs',()=>{
  const {rows}=bilingualRows('雨停了。\n\n我们出发！','The rain stopped. We set off!')
  expect(rows).toHaveLength(2);expect(rows[0].english).toContain('The rain stopped.');expect(rows[0].english).not.toContain('We set off');expect(rows[1].chinese).toContain('我们出发！')
 })
 it('preserves inline formatting and links when a sentence boundary crosses an element',()=>{
  const {blocks}=bilingualDocument('**First sentence. Second sentence.** [Third sentence.](https://example.com)','en-')
  expect(blocks).toHaveLength(3);expect(blocks[0]).toContain('<strong>First sentence.');expect(blocks[1]).toContain('<strong>Second sentence.');expect(blocks[2]).toContain('href="https://example.com"')
  const host=document.createElement('div');host.innerHTML=blocks.join('');expect(host.textContent).toBe('First sentence. Second sentence. Third sentence.')
 })
 it('keeps titles, initials, decimal numbers and closing quotes with their sentence',()=>{
  const {blocks}=bilingualDocument('Dr. Smith paid 3.14 dollars. “Really?” she asked. Then J. Smith left.','en-')
  expect(blocks).toHaveLength(3);expect(blocks[0]).toContain('Dr. Smith paid 3.14 dollars.');expect(blocks[1]).toContain('“Really?” she asked.');expect(blocks[2]).toContain('J. Smith left.')
 })
 it('preserves whole tables and inline code without splitting their syntax',()=>{
  const {blocks}=bilingualDocument('Use `a.b`. Keep it.\n\n| Name | Note |\n| --- | --- |\n| Dr. A | Hello. Yes. |','en-')
  expect(blocks).toHaveLength(2);expect(blocks[0]).toContain('<code>a.b</code>');expect(blocks[1]).toContain('<table>')
 })
 it('keeps fenced code and lists as complete blocks',()=>{
  const {blocks}=bilingualDocument('Intro\n\n```js\nconst a=1\n\nconst b=2\n```\n\n- one\n- two','en-')
  expect(blocks).toHaveLength(3);expect(blocks[1]).toContain('const');expect(blocks[2]).toContain('<ul>')
 })
 it('preserves document-wide references and unique heading anchors',()=>{
  const {blocks,outline}=bilingualDocument('# Rain\n\n# Rain\n\n[Read][book]\n\n[book]: https://example.com','en-')
  expect(outline[0].id).not.toBe(outline[1].id);expect(outline[0].id).toMatch(/^en-/);expect(blocks.join('')).toContain('href="https://example.com"')
 })
 it('sanitizes both columns with the existing Markdown security policy',()=>{
  const {rows}=bilingualRows('<img src=x onerror=alert(1)>','[click](javascript:alert(1))')
  const host=document.createElement('div');host.innerHTML=rows.map(row=>row.chinese+row.english).join('');expect(host.querySelector('[onerror],script,a[href^="javascript:"]')).toBeNull()
 })
})
