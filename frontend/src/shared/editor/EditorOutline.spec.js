import {mount,flushPromises} from '@vue/test-utils'
import {afterEach,expect,it,vi} from 'vitest'
import EditorOutline from './EditorOutline.vue'
import * as structure from './commands/editorStructure'
let wrapper
afterEach(()=>{wrapper?.unmount();vi.useRealTimers();vi.restoreAllMocks()})
it('waits for editor initialization and cancels parsing when readiness is lost',async()=>{
 vi.useFakeTimers();const read=vi.fn(()=>{throw new Error('parser is not ready')})
 wrapper=mount(EditorOutline,{props:{ready:false,source:'# Loading',adapter:{markdown:read}}})
 await wrapper.setProps({source:'# Loaded'});await vi.advanceTimersByTimeAsync(350);expect(read).not.toHaveBeenCalled()
 read.mockReturnValue('# Loaded');await wrapper.setProps({ready:true});expect(read).toHaveBeenCalledOnce();read.mockClear()
 await wrapper.setProps({source:'# New'});await wrapper.setProps({ready:false});await vi.advanceTimersByTimeAsync(350)
 expect(read).not.toHaveBeenCalled();expect(wrapper.findAll('nav button')).toHaveLength(0)
})
it('renders heading levels and debounces repeated document parsing',async()=>{
 let source='# One\n\n## Two';const parse=vi.spyOn(structure,'outlineSections')
 wrapper=mount(EditorOutline,{props:{ready:true,source,adapter:{markdown:()=>source}}});expect(wrapper.text()).toContain('One');expect(wrapper.text()).toContain('Two');parse.mockClear();vi.useFakeTimers()
 source='# New';await wrapper.setProps({source});source='# Newer';await wrapper.setProps({source});expect(parse).not.toHaveBeenCalled()
 await vi.advanceTimersByTimeAsync(350);expect(parse).toHaveBeenCalledOnce();expect(wrapper.text()).toContain('Newer')
})
it('does not navigate with stale source offsets',async()=>{
 let source='# Original';const adapter={markdown:()=>source,setMode:vi.fn()};wrapper=mount(EditorOutline,{props:{source,adapter,ready:true}})
 source='# Changed';await wrapper.get('nav button').trigger('click');expect(wrapper.text()).toContain('请重新选择');expect(adapter.setMode).not.toHaveBeenCalled()
})
it('bounds large-outline DOM work while searching all headings',async()=>{
 const source=Array.from({length:1000},(_,i)=>'# Heading '+i).join('\n\n');wrapper=mount(EditorOutline,{props:{source,adapter:{markdown:()=>source},ready:true}})
 expect(wrapper.findAll('nav button')).toHaveLength(200);await wrapper.get('[aria-label="搜索大纲标题"]').setValue('Heading 999');expect(wrapper.findAll('nav button')).toHaveLength(1);expect(wrapper.get('nav').text()).toContain('Heading 999')
})
it('releases pending outline work when hidden or unmounted',async()=>{
 let source='# One';const parse=vi.spyOn(structure,'outlineSections');wrapper=mount(EditorOutline,{props:{source,adapter:{markdown:()=>source},ready:true}});vi.useFakeTimers();parse.mockClear();source='# Two';await wrapper.setProps({source});wrapper.unmount();wrapper=null;await vi.advanceTimersByTimeAsync(500);expect(parse).not.toHaveBeenCalled()
})
