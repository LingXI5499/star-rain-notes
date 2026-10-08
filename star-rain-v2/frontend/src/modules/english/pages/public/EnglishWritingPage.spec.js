import { mount,flushPromises } from '@vue/test-utils'
import { createMemoryHistory,createRouter } from 'vue-router'
import { ref } from 'vue'
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest'
import EnglishWritingPage from './EnglishWritingPage.vue'
const mocked=vi.hoisted(()=>({listEnglishDocuments:vi.fn(),listMyWriting:vi.fn(),getTaxonomy:vi.fn(),account:false,user:null}))
vi.mock('../../api/englishApi',()=>({listEnglishDocuments:mocked.listEnglishDocuments}))
vi.mock('../../api/englishRwApi',()=>({listMyWriting:mocked.listMyWriting,getTaxonomy:mocked.getTaxonomy}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({currentUser:mocked.user})}))
vi.mock('../../../../shared/viewMode',()=>({useViewMode:()=>({contentPath:p=>p,isAccount:ref(mocked.account)}),accountPath:p=>'/useradmin'+p}))
let wrapper
async function setup(path='/english/writing'){const router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/writing',component:EnglishWritingPage},{path:'/english/topics/:slug',component:{template:'<div />'}}]});await router.push(path);wrapper=mount(EnglishWritingPage,{global:{plugins:[router]}});await flushPromises();return router}
beforeEach(()=>{vi.clearAllMocks();mocked.account=false;mocked.user=null;mocked.listEnglishDocuments.mockResolvedValue({items:[],total:0});mocked.listMyWriting.mockResolvedValue({items:[{id:'99',title:'Private work',state:'DRAFT',visibility:'PRIVATE'}],total:1});mocked.getTaxonomy.mockResolvedValue([])})
afterEach(()=>wrapper?.unmount())
describe('writing center account boundary',()=>{
 it('does not request private articles or expose private counters on the public tree',async()=>{await setup();expect(mocked.listMyWriting).not.toHaveBeenCalled();expect(wrapper.text()).not.toContain('Private work');expect(wrapper.text()).not.toContain('我的写作')})
 it('requests private articles with taxonomy filters, completion state and independent pagination only in an authenticated account view',async()=>{mocked.account=true;mocked.user={id:'7'};await setup('/english/writing?topicId=16&genreId=80&purposeId=90&state=DRAFT&myPage=2&mySize=24');expect(mocked.listMyWriting).toHaveBeenCalledWith(expect.objectContaining({topicId:'16',genreId:'80',purposeId:'90',state:'DRAFT',page:2,size:24}));expect(wrapper.text()).toContain('Private work');expect(wrapper.find('a[href="/useradmin/english/writing/articles/99/edit"]').exists()).toBe(true)})
})
