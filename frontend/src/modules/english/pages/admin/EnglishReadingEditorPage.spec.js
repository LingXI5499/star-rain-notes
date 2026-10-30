import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import Page from './EnglishReadingEditorPage.vue'
const api=vi.hoisted(()=>({createReading:vi.fn(),getReading:vi.fn(),updateReading:vi.fn(),getEnhancements:vi.fn(),saveEnhancements:vi.fn()}))
vi.mock('../../api/englishApi',()=>({createReading:api.createReading,getReading:api.getReading,updateReading:api.updateReading}))
vi.mock('../../api/englishRwApi',()=>({getEnhancements:api.getEnhancements,saveEnhancements:api.saveEnhancements}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({currentUser:{id:'1'}})}))
vi.mock('../../../../shared/http',()=>({post:vi.fn(),errorMessage:error=>error.message}))
const Editor=defineComponent({props:['modelValue'],setup(props,{expose}){expose({getMarkdown:()=>props.modelValue});return()=>null}})
let wrapper
const saved={id:'5',title:'雨后',bodyMarkdown:'After rain.',translationZhMarkdown:'雨后。',rowVersion:1,publishStatus:'DRAFT'}
beforeEach(()=>{vi.clearAllMocks();api.createReading.mockResolvedValue(saved);api.updateReading.mockResolvedValue(saved);api.getEnhancements.mockResolvedValue([]);api.getReading.mockResolvedValue(saved)})
afterEach(()=>wrapper?.unmount())
async function open(id='new'){
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/manage/editor/world/:documentId',component:Page}]});await router.push('/english/manage/editor/world/'+id)
 wrapper=mount(Page,{global:{plugins:[router],stubs:{MarkdownEditor:Editor,EnglishTaxonomyFields:true,EnglishArticleParallel:true,EnglishBilingualProse:true,EnglishRevisionPanel:true}}});await flushPromises()
}
it('shows title, original and translation without a summary field',async()=>{await open();expect(wrapper.text()).not.toContain('来源与公开权限');expect(wrapper.text()).not.toContain('版权核查');expect(wrapper.text()).not.toContain('摘要');expect(wrapper.text()).toContain('中文翻译');expect(wrapper.text()).toContain('英文原文')})
it('saves newly authored world articles as original',async()=>{
 await open();await wrapper.findAll('button').find(button=>button.text()==='保存正文').trigger('click');await flushPromises()
 expect(api.createReading).toHaveBeenCalledWith(expect.objectContaining({contentOrigin:'ORIGINAL',sourceName:'',sourceUrl:'',rights:expect.objectContaining({rightsStatus:'CLEARED'})}))
})
it('applies the original-content rule on explicit save of an old record',async()=>{
 api.getReading.mockResolvedValue({...saved,contentOrigin:'EXTERNAL',rights:{rightsStatus:'PENDING'}});await open('5');await wrapper.findAll('button').find(button=>button.text()==='保存正文').trigger('click');await flushPromises()
 expect(api.updateReading).toHaveBeenCalledWith('5',expect.objectContaining({contentOrigin:'ORIGINAL',rowVersion:1,rights:expect.objectContaining({rightsStatus:'CLEARED'})}))
})
