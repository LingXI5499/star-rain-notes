import { mount,flushPromises } from '@vue/test-utils'
import { expect,it,vi } from 'vitest'
import EnglishDocumentsManagePage from './EnglishDocumentsManagePage.vue'
const listReading=vi.hoisted(()=>vi.fn())
vi.mock('../../api/englishApi',()=>({listReading,deleteReading:vi.fn(),setReadingPublished:vi.fn()}))
vi.mock('../../../../shared/viewMode',()=>({accountPath:path=>'/useradmin'+path}))
it('keeps reading management and its specific editor without old writing types or level badges',async()=>{
 listReading.mockResolvedValue({items:[{id:'1',title:'Reading title',summary:'Reading summary',publishStatus:'DRAFT',cefrLevel:'C2',difficultyLevel:3}],total:1})
 const wrapper=mount(EnglishDocumentsManagePage,{global:{stubs:{RouterLink:{props:['to'],template:'<a :href="to"><slot /></a>'}}}})
 await flushPromises();expect(wrapper.text()).toContain('Reading title');expect(wrapper.text()).not.toContain('C2');expect(wrapper.text()).not.toContain('难度');expect(wrapper.text()).not.toContain('写作素材');expect(wrapper.find('a[href="/useradmin/english/manage/editor/reading/new"]').exists()).toBe(true);expect(wrapper.find('a[href="/useradmin/english/manage/editor/reading/1"]').exists()).toBe(true);wrapper.unmount()
})
