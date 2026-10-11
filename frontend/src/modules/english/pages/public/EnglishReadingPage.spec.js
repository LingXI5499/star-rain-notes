/**
 * EnglishReadingPage.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue-router、vitest、./EnglishReadingPage.vue。
 * 阅读入口：setup（setup的处理函数）。
 * 验证场景：reading page independent content loading；keeps the body and successful auxiliary content when alignments return 500；shows the body before a slow auxiliary request resolves；never requests reading auxiliary endpoints for original writing and disables missing Chinese modes；does not expose historical reading level fields in the header。
 */

import { mount,flushPromises } from '@vue/test-utils'
import { createMemoryHistory,createRouter } from 'vue-router'
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest'
import EnglishReadingPage from './EnglishReadingPage.vue'
const api=vi.hoisted(()=>({getReading:vi.fn(),getPublicWriting:vi.fn(),getEnhancements:vi.fn(),getTaxonomy:vi.fn()}))
vi.mock('../../api/englishApi',()=>({getReading:api.getReading}))
vi.mock('../../api/englishRwApi',()=>api)
vi.mock('../../../profile/api/profileApi',()=>({getPublicProfile:()=>Promise.resolve({})}))
vi.mock('../../../../shared/viewMode',()=>({useViewMode:()=>({contentPath:path=>path})}))
let wrapper
beforeEach(()=>{vi.clearAllMocks();api.getReading.mockResolvedValue({title:'Real article',slug:'reading-one',bodyMarkdown:'Continuous English body.',translationZhMarkdown:'中文。'});api.getPublicWriting.mockResolvedValue({title:'Original writing',slug:'writing-one',bodyMarkdown:'Own English body.'});api.getEnhancements.mockResolvedValue([]);api.getTaxonomy.mockResolvedValue([])})
afterEach(()=>wrapper?.unmount())
/**
 * setup的处理函数。
 * 主要协作：createRouter、createMemoryHistory、router.push、mount、flushPromises。
 * @param kind='reading' kind阅读；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function setup(kind='reading'){
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/reading/:slug',component:EnglishReadingPage},{path:'/english/:rest(.*)',component:{template:'<div />'}}]})
 await router.push('/english/reading/reading-one')
 wrapper=mount(EnglishReadingPage,{props:{kind},global:{plugins:[router],stubs:{EnglishBilingualProse:{props:['english'],template:'<div class="prose">{{english}}</div>'}}}})
 await flushPromises();return router
}
describe('reading page independent content loading',()=>{
 it('keeps the body and successful auxiliary content when alignments return 500',async()=>{
  api.getEnhancements.mockImplementation((slug,kind)=>kind==='alignments'?Promise.reject(new Error('500')):Promise.resolve(kind==='vocabulary'?[{id:'3',word:'body',partOfSpeech:'NOUN',meaningZh:'正文'}]:[]))
  await setup();expect(wrapper.text()).toContain('Continuous English body.');expect(wrapper.text()).toContain('双语对齐暂时无法加载');expect(wrapper.text()).toContain('正文');expect(wrapper.find('[role="alert"]').exists()).toBe(false)
 })
 it('shows the body before a slow auxiliary request resolves',async()=>{
  let complete;api.getTaxonomy.mockReturnValue(new Promise(resolve=>{complete=resolve}));await setup();expect(wrapper.text()).toContain('Continuous English body.');expect(wrapper.text()).not.toContain('正在读取文章');complete([]);await flushPromises()
 })
 it('never requests reading auxiliary endpoints for original writing and disables missing Chinese modes',async()=>{
  await setup('writing-articles');expect(api.getEnhancements).not.toHaveBeenCalled();expect(api.getReading).not.toHaveBeenCalled();expect(wrapper.text()).toContain('Own English body.')
  for(const button of wrapper.findAll('button').filter(b=>['中文','双语对照'].includes(b.text())))expect(button.attributes('disabled')).toBeDefined()
 })
 it('does not expose historical reading level fields in the header',async()=>{
  api.getReading.mockResolvedValue({title:'Real article',slug:'reading-one',bodyMarkdown:'Body.',cefrLevel:'C2',difficultyLevel:3,levelAssessed:true});await setup();expect(wrapper.text()).not.toContain('C2');expect(wrapper.text()).not.toContain('难度')
 })
})
