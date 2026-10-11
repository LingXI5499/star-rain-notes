/**
 * EnglishWritingEditorPage.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue-router、vue、vitest、./EnglishWritingEditorPage.vue。
 * 阅读入口：article（article的处理函数）。
 * 验证场景：writing editor persistence boundary；reads the editor current Markdown before flushing, even before its delayed input callback。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { defineComponent } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import EnglishWritingEditorPage from './EnglishWritingEditorPage.vue'
const api=vi.hoisted(()=>({getWriting:vi.fn(),saveWriting:vi.fn(),latest:'',savedVersion:0}))
vi.mock('../../api/englishRwApi',()=>({getWriting:api.getWriting,saveWriting:api.saveWriting,createWriting:vi.fn(),completeWriting:vi.fn(),publishWriting:vi.fn(),deleteWriting:vi.fn()}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({hasPermission:()=>false,currentUser:{roles:[]}})}))
vi.mock('../../../../shared/viewMode',()=>({accountPath:p=>'/useradmin'+p}))
const Editor=defineComponent({props:['modelValue'],setup(props,{expose}){expose({getMarkdown:()=>props.modelValue==='old body'?api.latest:props.modelValue});return()=>null}})
let wrapper
/**
 * article的处理函数。
 * @param body='old body' 正文old正文；默认值、解构字段与可选参数见函数签名。
 * @param version=0 版本0；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function article(body='old body',version=0){return {id:'2',title:'Draft',bodyMarkdown:body,translationZhMarkdown:'',rowVersion:version,state:'DRAFT',visibility:'PRIVATE',keywords:[]}}
afterEach(()=>{wrapper?.unmount();vi.clearAllMocks()})
describe('writing editor persistence boundary',()=>{
 it('reads the editor current Markdown before flushing, even before its delayed input callback',async()=>{
  api.latest='the latest input';api.getWriting.mockResolvedValue(article());api.saveWriting.mockImplementation(async (id,body)=>{api.savedVersion=body.rowVersion+1;return article(body.bodyMarkdown,api.savedVersion)})
  const router=createRouter({history:createMemoryHistory(),routes:[{path:'/useradmin/english/writing/articles/:id/edit',component:EnglishWritingEditorPage}]});await router.push('/useradmin/english/writing/articles/2/edit')
  wrapper=mount(EnglishWritingEditorPage,{global:{plugins:[router],stubs:{MarkdownEditor:Editor,EnglishTaxonomyFields:true,EnglishRevisionPanel:true,EnglishBilingualProse:true}}});await flushPromises()
  const button=wrapper.findAll('button').find(b=>b.text()==='保存草稿');await button.trigger('click');await flushPromises()
  expect(api.saveWriting).toHaveBeenCalledWith('2',expect.objectContaining({bodyMarkdown:'the latest input',rowVersion:0}))
  expect(wrapper.get('[role="status"]').text()).toContain('已自动保存')
 })
})
