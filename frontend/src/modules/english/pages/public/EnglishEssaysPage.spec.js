import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, beforeEach, it, expect, vi } from 'vitest'
import EnglishEssaysPage from './EnglishEssaysPage.vue'
const http=vi.hoisted(()=>({get:vi.fn(),errorMessage:e=>e.message}))
vi.mock('../../../../shared/http',()=>http)
let wrapper,router
beforeEach(()=>{http.get.mockReset();http.get.mockImplementation(path=>Promise.resolve(path.endsWith('/facets')?{tags:[],months:[],genres:[],total:0}:{items:[],total:0}));vi.spyOn(window,'scrollTo').mockImplementation(()=>{})})
afterEach(()=>{wrapper?.unmount();vi.restoreAllMocks()})
async function open(path='/english/essays') {router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/essays',component:EnglishEssaysPage}]});await router.push(path);wrapper=mount(EnglishEssaysPage,{global:{plugins:[router]}});await flushPromises()}
it('uses the static journal design with real empty counts and no private writing workspace',async()=>{
 await open();expect(wrapper.text()).toContain('A new page is waiting.');expect(wrapper.text()).toContain('No published months yet.');expect(wrapper.findAll('.journal-facets button')).toHaveLength(0);expect(wrapper.text()).not.toContain('我的写作')
})
it('requests title, tag and month filters together through the public essay API',async()=>{
 await open('/english/essays?q=life&tag=reflection&month=2026-10')
 expect(http.get).toHaveBeenCalledWith('/public/english/essays',expect.objectContaining({q:'life',tag:'reflection',month:'2026-10',page:1,size:10}))
 await wrapper.get('.journal-results button').trigger('click');await flushPromises();expect(router.currentRoute.value.query).toEqual({})
})
