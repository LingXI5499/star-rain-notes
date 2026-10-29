import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, beforeEach, it, expect, vi } from 'vitest'
import EnglishLibraryPage from './EnglishLibraryPage.vue'
const api=vi.hoisted(()=>({getLibraryTaxonomy:vi.fn(),getLibraryCounts:vi.fn(),getLibraryOverview:vi.fn(),getLibraryWords:vi.fn(),listReading:vi.fn(),getVocabularyThemes:vi.fn()}))
vi.mock('../../api/englishLibraryApi',()=>api)
vi.mock('../../api/englishApi',()=>({listReading:api.listReading,getVocabularyThemes:api.getVocabularyThemes}))
const tree=[{id:'1',code:'T01',name:'人体',children:[{id:'2',code:'T01-01',name:'器官'}]},{id:'3',code:'T02',name:'生活',children:[{id:'4',code:'T02-01',name:'日常'}]}]
const word={id:'20717',themeId:'1',legacyIndex:0,word:'physical',translation:'身体上的',sceneMeaning:'身体的',migrationStatus:'confirmed',examples:'[]'}
let wrapper,router
beforeEach(()=>{vi.clearAllMocks();api.getLibraryTaxonomy.mockResolvedValue(tree);api.getLibraryCounts.mockResolvedValue({T01:1,'T01-01':1});api.getLibraryOverview.mockResolvedValue({vocabularyRecords:7053,confirmedRecords:1});api.getLibraryWords.mockResolvedValue({items:[word],total:1});api.listReading.mockResolvedValue({items:[],total:0});vi.spyOn(window,'scrollTo').mockImplementation(()=>{})})
afterEach(()=>{wrapper?.unmount();vi.restoreAllMocks()})
async function open(path, module='vocabulary'){
 router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/vocabulary/source/:themeId',component:EnglishLibraryPage,props:{module}},{path:'/english/'+module+'/:category?/:topic?',component:EnglishLibraryPage,props:{module}},{path:'/:rest(.*)',component:{template:'<div />'}}]})
 await router.push(path);await router.isReady();wrapper=mount(EnglishLibraryPage,{props:{module},global:{plugins:[router]}});await flushPromises()
}
it('shows source word cards and filters by the actual two-level taxonomy',async()=>{
 await open('/english/vocabulary?q=physical&size=48')
 expect(wrapper.text()).toContain('physical');expect(wrapper.text()).toContain('身体上的')
 expect(api.getLibraryWords).toHaveBeenLastCalledWith(expect.objectContaining({q:'physical',status:'confirmed',size:48}))
 await router.push('/english/vocabulary/t01/t01-01?q=physical&display=en');await flushPromises()
 expect(api.getLibraryWords).toHaveBeenLastCalledWith(expect.objectContaining({category:'T01',topic:'T01-01'}))
 expect(wrapper.find('[aria-label="中文信息"]').exists()).toBe(false)
 expect(wrapper.find('[aria-label="英文信息"]').exists()).toBe(true)
 const calls=api.getLibraryWords.mock.calls.length
 await wrapper.get('[aria-label="词卡显示语言"]').findAll('button')[2].trigger('click');await flushPromises()
 expect(api.getLibraryWords).toHaveBeenCalledTimes(calls)
 expect(wrapper.find('[aria-label="英文信息"]').exists()).toBe(false)
 expect(wrapper.find('[aria-label="中文信息"]').exists()).toBe(true)
})
it('rejects a topic belonging to a different parent without reading that topic',async()=>{
 await open('/english/vocabulary/t01/t02-01')
 expect(api.getLibraryWords).not.toHaveBeenCalled();expect(wrapper.get('[role="alert"]').text()).toMatch(/不存在|无效/)
})
it('does not let a late response replace the current category results',async()=>{
 let resolve;api.getLibraryWords.mockImplementationOnce(()=>new Promise(done=>{resolve=done})).mockResolvedValue({items:[{...word,id:'2',word:'current'}],total:1})
 await open('/english/vocabulary');await router.push('/english/vocabulary/t01');await flushPromises()
 resolve({items:[{...word,word:'stale'}],total:1});await flushPromises()
 expect(wrapper.text()).toContain('current');expect(wrapper.text()).not.toContain('stale')
})
it('renders the genuine empty world without inserting old example articles',async()=>{
 api.getLibraryTaxonomy.mockResolvedValue([{id:'8',code:'R01',name:'自然科学',children:[]}])
 await open('/english/world','world')
 expect(wrapper.get('h1').text()).toBe('世界');expect(wrapper.text()).toContain('该主题正在整理内容')
 expect(wrapper.findAll('.library-article-grid article')).toHaveLength(0)
})
it('keeps unconfirmed search results reachable through the all-source view',async()=>{
 await open('/english/vocabulary?status=all&q=back')
 expect(api.getLibraryWords).toHaveBeenLastCalledWith(expect.objectContaining({q:'back',status:''}))
 expect(wrapper.text()).toContain('全部来源记录')
})
it('uses the new cards for preserved source themes without losing unmapped words',async()=>{
 api.getVocabularyThemes.mockResolvedValue([{id:'1',name:'来源主题'}])
 await open('/english/vocabulary/source/1')
 expect(api.getLibraryWords).toHaveBeenLastCalledWith(expect.objectContaining({theme:'1',status:''}))
 expect(wrapper.findAll('.library-word')).toHaveLength(1)
})
