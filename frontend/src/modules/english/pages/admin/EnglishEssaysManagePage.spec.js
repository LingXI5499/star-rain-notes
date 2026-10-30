import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import Page from './EnglishEssaysManagePage.vue'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
const api=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),del:vi.fn()}))
vi.mock('../../../../shared/http',()=>({...api,errorMessage:error=>error.message}))
vi.mock('../../../account/stores/authStore',()=>({useAuthStore:()=>({hasPermission:()=>true})}))
let wrapper,router
beforeEach(()=>{vi.clearAllMocks();api.get.mockResolvedValue({items:[],total:0})})
afterEach(()=>wrapper?.unmount())
async function open(query=''){
 router=createRouter({history:createMemoryHistory(),routes:[{path:'/english/manage/essays',component:Page}]});await router.push('/english/manage/essays'+query)
 wrapper=mount(Page,{global:{plugins:[router]}});await flushPromises()
}
it('restores state, page size and search from the URL',async()=>{
 api.get.mockResolvedValue({items:[],total:45});await open('?q=rain&state=PENDING&page=2&pageSize=20')
 expect(api.get).toHaveBeenLastCalledWith('/admin/english/essays',{search:'rain',state:'PENDING',page:2,size:20})
})
it('returns to page one after a filter change',async()=>{
 api.get.mockResolvedValue({items:[],total:45});await open('?page=2');await wrapper.get('select').setValue('REJECTED');await flushPromises()
 expect(api.get).toHaveBeenLastCalledWith('/admin/english/essays',expect.objectContaining({state:'REJECTED',page:1}))
})
it('paginates the filtered server query',async()=>{
 api.get.mockResolvedValue({items:[],total:45});await open('?state=PENDING');wrapper.findComponent(PublicPagination).vm.$emit('change',2);await flushPromises()
 expect(api.get).toHaveBeenLastCalledWith('/admin/english/essays',expect.objectContaining({state:'PENDING',page:2}))
})
it('retries a search even when the URL does not change',async()=>{
 await open();api.get.mockClear();wrapper.findComponent(PublicSearch).vm.$emit('search');await flushPromises();expect(api.get).toHaveBeenCalledTimes(1)
})
it('returns from a now empty last page to the previous page',async()=>{
 api.get.mockResolvedValue({items:[{id:'9',title:'Rain',state:'DRAFT',visibility:'PRIVATE'}],total:11});await open('?page=2')
 await wrapper.findAll('button').find(item=>item.text()==='删除').trigger('click');api.get.mockResolvedValue({items:[],total:10});await wrapper.findAll('button').find(item=>item.text()==='确认删除').trigger('click');await flushPromises()
 expect(api.del).toHaveBeenCalledWith('/admin/english/essays/9');expect(api.get).toHaveBeenLastCalledWith('/admin/english/essays',expect.objectContaining({page:1}))
})
