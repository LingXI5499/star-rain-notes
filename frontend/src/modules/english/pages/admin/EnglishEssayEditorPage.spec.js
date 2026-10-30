import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import Page from './EnglishEssayEditorPage.vue'
const api=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),put:vi.fn()}))
vi.mock('../../../../shared/http',()=>({...api,errorMessage:error=>error.message}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({currentUser:{id:'1'},hasPermission:()=>true})}))
const Editor=defineComponent({props:['modelValue','placeholder'],emits:['update:modelValue'],setup(props,{expose}){expose({getMarkdown:()=>props.modelValue});},template:'<textarea :aria-label="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\',$event.target.value)"/>'})
let wrapper, router
const article=(extra={})=>({id:'5',title:'随笔',bodyMarkdown:'English.',translationZhMarkdown:'中文。',rowVersion:1,state:'DRAFT',visibility:'PRIVATE',...extra})
beforeEach(()=>{vi.clearAllMocks();api.get.mockResolvedValue(article());api.put.mockImplementation(async(id,body)=>article({...body,rowVersion:body.rowVersion+1}));api.post.mockResolvedValue(article())})
afterEach(()=>wrapper?.unmount())
async function open(id='new'){
 router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/manage/essays/editor/:id',component:Page},{path:'/english/manage/essays',component:{template:'<p>随笔列表</p>'}}]})
 await router.push('/english/manage/essays/editor/'+id)
 wrapper=mount({template:'<RouterView/>'},{global:{plugins:[router],stubs:{MarkdownEditor:Editor,EnglishArticleParallel:true}}});await flushPromises()
}
const button=text=>wrapper.findAll('button').find(item=>item.text()===text)
it('opens with only title, original and translation without creating an empty database article',async()=>{await open();expect(api.post).not.toHaveBeenCalled();expect(wrapper.findAll('textarea')).toHaveLength(2);expect(wrapper.text()).not.toContain('摘要');expect(wrapper.text()).toContain('中文翻译');expect(wrapper.text()).toContain('英文原文')})
it('normalizes database nulls before creating Markdown editors',async()=>{api.get.mockResolvedValue(article({bodyMarkdown:null,translationZhMarkdown:null}));await open('5');expect(wrapper.findAllComponents(Editor).map(editor=>editor.props('modelValue'))).toEqual(['',''])})
it('creates only on save and keeps editing at the created route',async()=>{
 await open();await wrapper.get('input').setValue('雨后');await button('保存草稿').trigger('click');await flushPromises()
 expect(api.post).toHaveBeenCalledWith('/admin/english/essays',expect.objectContaining({title:'雨后'}));expect(router.currentRoute.value.params.id).toBe('5');expect(wrapper.get('input').element.value).toBe('雨后')
})
it('saves both source texts without requiring a summary',async()=>{
 await open();const editors=wrapper.findAllComponents(Editor)
 editors[0].vm.$emit('update:modelValue','雨停了。我们出发。');editors[1].vm.$emit('update:modelValue','Rain stopped. We set off.')
 await wrapper.get('input').setValue('雨后');await button('保存草稿').trigger('click');await flushPromises()
 const payload=api.post.mock.calls.find(([path])=>path==='/admin/english/essays')[1]
 expect(payload).toMatchObject({title:'雨后',bodyMarkdown:'Rain stopped. We set off.',translationZhMarkdown:'雨停了。我们出发。'});expect(payload).not.toHaveProperty('summary')
})
it('flushes current content before submitting the exact saved version',async()=>{
 await open('5');await wrapper.get('input').setValue('修改后的标题');await button('提交审核').trigger('click');await flushPromises()
 expect(api.put).toHaveBeenCalled();expect(api.post).toHaveBeenCalledWith('/admin/english/essays/5/submit',{rowVersion:2})
})
it('shows a read-only review and cancellation action while pending',async()=>{api.get.mockResolvedValue(article({state:'PENDING'}));await open('5');expect(wrapper.findComponent(Editor).exists()).toBe(false);expect(button('取消送审')).toBeDefined();expect(wrapper.get('fieldset').attributes('disabled')).toBeDefined()})
it('navigates back after saving without a silent route failure',async()=>{await open('5');await wrapper.get('input').setValue('新标题');await router.push('/english/manage/essays');await flushPromises();expect(wrapper.text()).toContain('随笔列表');expect(api.put).toHaveBeenCalled()})
it('leaves an empty initialized editor without creating a draft',async()=>{await open();for(const editor of wrapper.findAllComponents(Editor))editor.vm.$emit('update:modelValue','\n');await flushPromises();await router.push('/english/manage/essays');await flushPromises();expect(wrapper.text()).toContain('随笔列表');expect(api.post).not.toHaveBeenCalled()})
it('saves a new article while leaving without replacing the destination route',async()=>{await open();await wrapper.get('input').setValue('新随笔');await router.push('/english/manage/essays');await flushPromises();expect(wrapper.text()).toContain('随笔列表');expect(api.post).toHaveBeenCalledTimes(1)})
it('keeps content and explains blocked navigation when saving fails',async()=>{
 api.put.mockRejectedValue(new Error('连接中断'));await open('5');await wrapper.get('input').setValue('不能丢失的标题');await router.push('/english/manage/essays');await flushPromises()
 expect(wrapper.get('input').element.value).toBe('不能丢失的标题');expect(wrapper.get('[role="alertdialog"]').text()).toContain('正文尚未保存');expect(router.currentRoute.value.params.id).toBe('5')
 await button('放弃修改并离开').trigger('click');await flushPromises();expect(wrapper.text()).toContain('随笔列表')
})
