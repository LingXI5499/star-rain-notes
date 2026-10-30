import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, it, expect, vi } from 'vitest'
import EnglishLibraryPage from './EnglishLibraryPage.vue'
const api=vi.hoisted(()=>({user:null,getLibraryTaxonomy:vi.fn(),getLibraryCounts:vi.fn(),getLibraryOverview:vi.fn(),getLibraryWords:vi.fn(),listReading:vi.fn(),getVocabularyThemes:vi.fn(),learningWords:vi.fn(),learningStates:vi.fn(),currentPlan:vi.fn()}))
vi.mock('../../api/englishLibraryApi',()=>api)
vi.mock('../../api/englishApi',()=>({listReading:api.listReading,getVocabularyThemes:api.getVocabularyThemes}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({get currentUser(){return api.user}})}))
vi.mock('../../api/vocabularyLearningApi',async original=>({...await original(),learningWords:api.learningWords,learningStates:api.learningStates,currentPlan:api.currentPlan}))
const tree=[{id:'1',code:'T01',name:'人体',children:[{id:'2',code:'T01-01',name:'器官'}]},{id:'3',code:'T02',name:'生活',children:[{id:'4',code:'T02-01',name:'日常'}]}]
const word={id:'20717',themeId:'1',legacyIndex:0,word:'physical',translation:'身体上的',sceneMeaning:'身体的',migrationStatus:'confirmed',examples:'[]'}
let wrapper,router
beforeEach(()=>{vi.clearAllMocks();sessionStorage.clear();api.user=null;api.getLibraryTaxonomy.mockResolvedValue(tree);api.getLibraryCounts.mockResolvedValue({T01:1,'T01-01':1});api.getLibraryOverview.mockResolvedValue({vocabularyRecords:7053,confirmedRecords:1});api.getLibraryWords.mockResolvedValue({items:[word],total:1});api.learningWords.mockResolvedValue({items:[word],total:1});api.learningStates.mockResolvedValue({[word.id]:{memoryCount:4,masteryRank:'LEARNER'}});api.currentPlan.mockResolvedValue({status:'NONE'});api.listReading.mockResolvedValue({items:[],total:0});vi.spyOn(window,'scrollTo').mockImplementation(()=>{})})
afterEach(()=>{wrapper?.unmount();setActivePinia(undefined);vi.restoreAllMocks()})
async function open(path, module='vocabulary'){
 router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/vocabulary/source/:themeId',component:EnglishLibraryPage,props:{module}},{path:'/english/'+module+'/:category?/:topic?',component:EnglishLibraryPage,props:{module}},{path:'/:rest(.*)',component:{template:'<div />'}}]})
 await router.push(path);await router.isReady();wrapper=mount(EnglishLibraryPage,{props:{module},global:{plugins:[createPinia(),router]}});await flushPromises()
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
const button = name => wrapper.findAll('button').find(item=>item.text()===name)
it('uses current taxonomy and personal filters for server-wide selection with exclusions',async()=>{
 api.user={id:'1'};api.learningWords.mockResolvedValue({items:[word],total:18})
 await open('/english/vocabulary/t01/t01-01?learned=NO')
 expect(api.getLibraryWords).not.toHaveBeenCalled()
 expect(api.learningWords).toHaveBeenCalledWith(expect.objectContaining({category:'T01',topic:'T01-01',libraryStatus:'confirmed',learned:'NO'}))
 expect(wrapper.get('.library-word-memory').text()).toBe('4 次记忆 · 登堂入室')
 await button('全选当前筛选结果').trigger('click');await flushPromises()
 expect(wrapper.get('.selection-bar').text()).toContain('筛选全选 18 词')
 await wrapper.get('[aria-label="选择单词 physical"]').trigger('click')
 expect(wrapper.get('.selection-bar').text()).toContain('筛选全选 17 词')
 expect(JSON.parse(sessionStorage.getItem('sr-vocabulary-plan-selection:all'))).toMatchObject({themeId:null,category:'T01',topic:'T01-01',learned:'NO',learnableOnly:true,selectAllMatched:true,excludedWordIds:[word.id]})
 await button('查看待选并设置计划').trigger('click');await flushPromises()
 expect(router.currentRoute.value.path).toBe('/english/vocabulary/plan/confirm')
})
it('clears filter-wide selection when its scope changes and preserves manual cross-topic choices',async()=>{
 api.user={id:'1'};await open('/english/vocabulary/t01')
 await button('全选当前筛选结果').trigger('click');await flushPromises()
 await router.push('/english/vocabulary/t02');await flushPromises()
 expect(wrapper.text()).toContain('原全选已清除')
 expect(wrapper.get('.selection-bar').text()).toContain('已选 0 词')
 await wrapper.get('[aria-label="选择单词 physical"]').trigger('click')
 await router.push('/english/vocabulary/t01');await flushPromises()
 expect(wrapper.get('.selection-bar').text()).toContain('已选 1 词')
})
it('keeps incomplete words browsable but out of memory selection',async()=>{
 api.getLibraryWords.mockResolvedValue({items:[{...word,translation:''}],total:1})
 await open('/english/vocabulary')
 expect(wrapper.text()).toContain('释义待补充')
 expect(wrapper.get('[aria-label="选择单词 physical"]').attributes('disabled')).toBeDefined()
 await button('反选当前页').trigger('click');expect(wrapper.get('.selection-bar').text()).toContain('已选 0 词')
})
it('does not show incomplete words as selected when all eligible matches are chosen',async()=>{
 api.user={id:'1'}
 api.learningWords.mockImplementation(filters=>Promise.resolve({items:filters.learnableOnly?[word]:[word,{...word,id:'8',word:'pending',translation:''}],total:filters.learnableOnly?1:2}))
 await open('/english/vocabulary');await button('全选当前筛选结果').trigger('click');await flushPromises()
 const pending=wrapper.get('[aria-label="选择单词 pending"]')
 expect(pending.attributes('disabled')).toBeDefined();expect(pending.attributes('aria-pressed')).toBe('false')
 expect(pending.text()).toBe('加入记忆待选');expect(wrapper.get('.selection-bar').text()).toContain('筛选全选 1 词')
})
it('takes guest selections through login before plan confirmation',async()=>{
 await open('/english/vocabulary');await wrapper.get('[aria-label="选择单词 physical"]').trigger('click')
 await button('查看待选并设置计划').trigger('click');await flushPromises()
 expect(router.currentRoute.value).toMatchObject({path:'/login',query:{redirect:'/english/vocabulary/plan/confirm'}})
 expect(JSON.parse(sessionStorage.getItem('sr-vocabulary-plan-selection:all')).wordIds).toEqual([word.id])
})
it('preserves catalog browsing if auxiliary personal records fail',async()=>{
 api.user={id:'1'};api.learningStates.mockRejectedValue(new Error('unavailable'))
 await open('/english/vocabulary');expect(wrapper.findAll('.library-word')).toHaveLength(1)
 expect(wrapper.text()).toContain('个人记忆记录暂时无法读取')
})
