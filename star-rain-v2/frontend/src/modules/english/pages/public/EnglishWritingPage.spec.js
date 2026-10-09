import { mount,flushPromises } from '@vue/test-utils'
import { createMemoryHistory,createRouter } from 'vue-router'
import { ref } from 'vue'
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest'
import EnglishWritingPage from './EnglishWritingPage.vue'
const mocked=vi.hoisted(()=>({listPublicWriting:vi.fn(),listMyWriting:vi.fn(),getTaxonomy:vi.fn(),account:false,user:null}))
vi.mock('../../api/englishRwApi',()=>({listPublicWriting:mocked.listPublicWriting,listMyWriting:mocked.listMyWriting,getTaxonomy:mocked.getTaxonomy}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({currentUser:mocked.user})}))
vi.mock('../../../../shared/viewMode',()=>({useViewMode:()=>({contentPath:p=>p,isAccount:ref(mocked.account)}),accountPath:p=>'/useradmin'+p}))
let wrapper
async function setup(path='/english/writing'){const router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/writing',component:EnglishWritingPage},{path:'/english/topics/:slug',component:{template:'<div />'}}]});await router.push(path);wrapper=mount(EnglishWritingPage,{global:{plugins:[router]}});await flushPromises();return router}
beforeEach(()=>{vi.clearAllMocks();mocked.account=false;mocked.user=null;mocked.listPublicWriting.mockResolvedValue({items:[],total:0});mocked.listMyWriting.mockResolvedValue({items:[{id:'99',title:'Private work',state:'DRAFT',visibility:'PRIVATE'}],total:1});mocked.getTaxonomy.mockResolvedValue([])})
afterEach(()=>wrapper?.unmount())
describe('writing center account boundary',()=>{
 it('does not request private articles or expose private counters on the public tree',async()=>{await setup();expect(mocked.listMyWriting).not.toHaveBeenCalled();expect(wrapper.text()).not.toContain('Private work');expect(wrapper.text()).not.toContain('我的写作')})
 it('uses only the original-writing list and never renders retired materials or tasks',async()=>{await setup();expect(mocked.listPublicWriting).toHaveBeenCalledTimes(1);expect(wrapper.text()).not.toContain('写作素材');expect(wrapper.text()).not.toContain('练习任务');expect(wrapper.find('a[href="/english/writing/legacy"]').exists()).toBe(false)})
 it('keeps private work available when the public list fails',async()=>{mocked.account=true;mocked.user={id:'7'};mocked.listPublicWriting.mockRejectedValue(new Error('public unavailable'));await setup();expect(wrapper.text()).toContain('Private work');expect(wrapper.findAll('[role="alert"]')).toHaveLength(1)})
 it('keeps public articles available when private work fails',async()=>{mocked.account=true;mocked.user={id:'7'};mocked.listPublicWriting.mockResolvedValue({items:[{id:'1',slug:'writing-1',title:'Public original'}],total:1});mocked.listMyWriting.mockRejectedValue(new Error('private unavailable'));await setup();expect(wrapper.text()).toContain('Public original');expect(wrapper.findAll('[role="alert"]')).toHaveLength(1)})
 it('requests private articles with taxonomy filters, completion state and independent pagination only in an authenticated account view',async()=>{mocked.account=true;mocked.user={id:'7'};await setup('/english/writing?topicId=16&genreId=80&purposeId=90&state=DRAFT&myPage=2&mySize=24');expect(mocked.listMyWriting).toHaveBeenCalledWith(expect.objectContaining({topicId:'16',genreId:'80',purposeId:'90',state:'DRAFT',page:2,size:24}));expect(wrapper.text()).toContain('Private work');expect(wrapper.find('a[href="/useradmin/english/writing/articles/99/edit"]').exists()).toBe(true)})
})
