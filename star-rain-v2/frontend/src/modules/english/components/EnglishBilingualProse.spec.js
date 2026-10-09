import { webcrypto } from 'node:crypto'
import { mount,flushPromises } from '@vue/test-utils'
import { afterEach,beforeAll,beforeEach,describe,expect,it,vi } from 'vitest'
import { defineComponent } from 'vue'
import EnglishBilingualProse from './EnglishBilingualProse.vue'
import { sha256 } from '../lib/readingAnchors'
let wrapper
beforeAll(()=>Object.defineProperty(globalThis,'crypto',{value:webcrypto,configurable:true}))
beforeEach(()=>vi.stubGlobal('matchMedia',()=>({matches:false})))
afterEach(()=>{wrapper?.unmount();vi.unstubAllGlobals();document.body.innerHTML=''})
const Prose=defineComponent({props:['markdown'],template:'<div class="markdown-body"><div><p v-for="(line,index) in markdown.split(\'\\n\\n\')" :key="index">{{line}}</p></div></div>'})
async function row(language,text,source,index=0,groupKey='group'){return{language,paragraphIndex:index,rangeStart:0,rangeEnd:text.length,expectedText:text,paragraphHash:await sha256(text),sourceMarkdownHash:await sha256(source),status:'ACTIVE',groupKey}}
async function until(check){for(let i=0;i<100;i++){await flushPromises();if(check())return;await new Promise(resolve=>setTimeout(resolve,5))}throw new Error('Anchors did not finish rendering')}
async function setup(mode='EN',extra={}){
 const english='English one.\n\nLocal two.',chinese='中文一。\n\n另一译文。'
 const alignments=[await row('EN','English one.',english),await row('ZH','中文一。',chinese),await row('ZH','另一译文。',chinese,1)]
 wrapper=mount(EnglishBilingualProse,{attachTo:document.body,props:{english,chinese,alignments,mode,...extra},global:{stubs:{BlogProse:Prose}}})
 await until(()=>wrapper.find('[data-rw-local-segment]').exists());return wrapper
}
describe('continuous bilingual prose interactions',()=>{
 it('highlights only visible English fragments without a popup and clears on Escape',async()=>{
  await setup();await wrapper.find('[lang="en"] [data-rw-anchor]').trigger('focusin')
  expect(wrapper.findAll('[lang="en"] .is-active')).toHaveLength(1)
  expect(wrapper.findAll('[lang="zh"] .is-active')).toHaveLength(0)
  expect(document.querySelector('[role="dialog"]')).toBeNull()
  document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));await flushPromises();expect(wrapper.findAll('.is-active')).toHaveLength(0)
 })
 it('highlights Chinese without a popup, then synchronizes one-to-many bilingual groups',async()=>{
  await setup('ZH');await wrapper.find('[lang="zh"] [data-rw-anchor]').trigger('pointerover')
  expect(wrapper.findAll('[lang="zh"] .is-active')).toHaveLength(2)
  expect(wrapper.findAll('[lang="en"] .is-active')).toHaveLength(0)
  expect(document.querySelector('[role="dialog"]')).toBeNull()
  await wrapper.setProps({mode:'BOTH'});expect(wrapper.findAll('.is-active')).toHaveLength(0)
  await wrapper.find('[lang="zh"] [data-rw-anchor]').trigger('pointerover')
  expect(wrapper.findAll('.is-active')).toHaveLength(3);expect(document.querySelector('[role="dialog"]')).toBeNull()
  expect([...wrapper.element.querySelectorAll('[lang="zh"] p')].map(p=>p.style.order)).toEqual(['1','1'])
 })
 it('keeps keyboard-pinned highlighting while its anchor remains visible during scrolling',async()=>{
  await setup();const anchor=wrapper.find('[lang="en"] [data-rw-anchor]')
  vi.spyOn(anchor.element,'getBoundingClientRect').mockReturnValue({top:80,bottom:120,left:20,right:200,width:180,height:40})
  await anchor.trigger('keydown',{key:'Enter'});window.dispatchEvent(new Event('scroll'));await flushPromises()
  expect(anchor.classes()).toContain('is-active');expect(document.querySelector('[role="dialog"]')).toBeNull()
  document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));await flushPromises();expect(wrapper.findAll('.is-active')).toHaveLength(0)
 })
 it('highlights unmapped text locally and toggles/clears it on touch and outside click',async()=>{
  await setup();const local=wrapper.find('[lang="en"] [data-rw-local-segment]')
  await local.trigger('click',{pointerType:'touch'});expect(document.querySelector('[role="dialog"]')).toBeNull()
  expect(wrapper.findAll('.is-active')).toHaveLength(1)
  await local.trigger('click',{pointerType:'touch'});expect(wrapper.findAll('.is-active')).toHaveLength(0)
  await local.trigger('click',{pointerType:'touch'});document.body.dispatchEvent(new Event('pointerdown',{bubbles:true}));await flushPromises();expect(wrapper.findAll('.is-active')).toHaveLength(0)
 })
 it('keeps unpaired bilingual columns explicit and never creates a fallback popup',async()=>{
  await setup('BOTH',{alignments:[]});expect(wrapper.text()).toContain('暂无人工双语对齐');expect(wrapper.find('.rw-bilingual--mapped').exists()).toBe(false)
  await wrapper.setProps({mode:'EN'});await wrapper.find('[lang="en"] [data-rw-local-segment]').trigger('pointerover');expect(wrapper.findAll('.is-active')).toHaveLength(1);expect(document.querySelector('[role="dialog"]')).toBeNull()
 })
 it('invalidates an entire one-to-many group when one source hash changes',async()=>{
  await setup();await wrapper.setProps({chinese:'Changed Chinese.'});await until(()=>!wrapper.find('[data-rw-anchor]').exists() && wrapper.find('[lang="en"] [data-rw-local-segment]').exists())
  await wrapper.find('[lang="en"] [data-rw-local-segment]').trigger('focusin');expect(wrapper.findAll('[lang="en"] .is-active')).toHaveLength(1);expect(wrapper.findAll('[lang="zh"] .is-active')).toHaveLength(0);expect(document.querySelector('[role="dialog"]')).toBeNull()
 })
 it('disables highlighting in annotation selection mode and preserves note clicks',async()=>{
  const english='English one.'
  const note={...await row('EN',english,english),groupKey:undefined,id:'note-1',analysisMarkdown:'A manually authored note.'}
  await setup('EN',{english,annotations:[note]})
  const anchor=wrapper.find('[data-notes="note-1"]')
  await anchor.trigger('click');expect(wrapper.emitted('note')[0]).toEqual(['note-1'])
  await wrapper.setProps({selecting:true});await anchor.trigger('pointerover')
  expect(wrapper.findAll('.is-active')).toHaveLength(0)
  expect(document.querySelector('[role="dialog"]')).toBeNull()
 })
 it('keeps one hundred paragraphs stable and removes global handlers when unmounted',async()=>{
  const remove=vi.spyOn(document,'removeEventListener');await setup('EN',{english:Array.from({length:100},(_,i)=>'Paragraph '+i+'.').join('\n\n'),alignments:[]})
  expect(wrapper.findAll('[lang="en"] p')).toHaveLength(100);const count=wrapper.findAll('[data-rw-local-segment]').length;await flushPromises();expect(wrapper.findAll('[data-rw-local-segment]')).toHaveLength(count)
  wrapper.unmount();expect(remove).toHaveBeenCalledWith('pointerdown',expect.any(Function));expect(remove).toHaveBeenCalledWith('keydown',expect.any(Function));remove.mockRestore();wrapper=null
 })
})
