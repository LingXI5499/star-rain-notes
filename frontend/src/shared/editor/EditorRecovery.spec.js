import {mount,flushPromises} from '@vue/test-utils'
import {beforeEach,afterEach,it,expect,vi} from 'vitest'
import EditorRecovery from './EditorRecovery.vue'
const store=vi.hoisted(()=>({draftGeneration:()=>0,getDraft:vi.fn(),writeDraft:vi.fn(),removeDraft:vi.fn(),migrateDraft:vi.fn(),registerDraftFlusher:vi.fn(()=>()=>{}),draftKey:context=>context?.accountId!=null?JSON.stringify([context.accountId,context.documentId,context.field]):null}))
vi.mock('./recovery/draftStore',()=>store)
const context={accountId:1,module:'blog',documentId:'1',field:'body',baseline:'server',serverVersion:4}
let wrapper
beforeEach(()=>{vi.useFakeTimers();vi.clearAllMocks();store.getDraft.mockResolvedValue(null);store.writeDraft.mockResolvedValue();store.removeDraft.mockResolvedValue()})
afterEach(()=>{wrapper?.unmount();vi.useRealTimers()})
it('requires an explicit decision before loading a version-mismatched local point',async()=>{
  store.getDraft.mockResolvedValue({points:[{source:'local',baseline:'old',serverVersion:2,time:1}]})
  wrapper=mount(EditorRecovery,{props:{context,source:'server'}});await flushPromises()
  expect(wrapper.emitted('restore')).toBeUndefined();expect(wrapper.text()).toContain('版本不同')
  await wrapper.findAll('button').find(button=>button.text()==='载入此恢复点到编辑器').trigger('click')
  expect(wrapper.emitted('restore')).toEqual([['local']]);expect(store.removeDraft).not.toHaveBeenCalled()
})
it('keeps new input and gives a visible quota failure without affecting the source',async()=>{
  wrapper=mount(EditorRecovery,{props:{context,source:'server'}});await flushPromises()
  store.writeDraft.mockRejectedValue(new Error('QuotaExceededError'))
  await wrapper.setProps({source:'new input'});await vi.advanceTimersByTimeAsync(1801);await flushPromises()
  expect(wrapper.text()).toContain('本地备份不可用');expect(wrapper.emitted('restore')).toBeUndefined();expect(store.writeDraft).toHaveBeenCalledWith(expect.objectContaining(context),'new input')
})
it('flushes the old document to its old owner during an identical-text account switch',async()=>{
  wrapper=mount(EditorRecovery,{props:{context,source:'server'}});await flushPromises();await wrapper.setProps({source:'unsaved'})
  await wrapper.setProps({context:{...context,accountId:2}});await flushPromises()
  expect(store.writeDraft).toHaveBeenCalledWith(expect.objectContaining(context),'unsaved');expect(store.getDraft).toHaveBeenLastCalledWith(expect.objectContaining({accountId:2}))
})
it('does not clear a newer local edit when an older submitted snapshot is acknowledged',async()=>{
  wrapper=mount(EditorRecovery,{props:{context,source:'server'}});await flushPromises()
  await wrapper.setProps({source:'newer'});await wrapper.setProps({context:{...context,baseline:'submitted',serverVersion:5}})
  await vi.advanceTimersByTimeAsync(1801);await flushPromises()
  expect(store.removeDraft).not.toHaveBeenCalled();expect(store.writeDraft).toHaveBeenCalledWith(expect.objectContaining({baseline:'submitted'}),'newer')
})
it('backs up current in-progress input before explicitly replacing it with an older recovery point',async()=>{
  store.getDraft.mockResolvedValue({points:[{source:'older local',baseline:'old',serverVersion:2,time:1}]})
  wrapper=mount(EditorRecovery,{props:{context,source:'server'}});await flushPromises()
  await wrapper.setProps({source:'latest unsaved input'})
  await wrapper.findAll('button').find(button=>button.text()==='载入此恢复点到编辑器').trigger('click');await flushPromises()
  expect(store.writeDraft).toHaveBeenCalledWith(expect.objectContaining(context),'latest unsaved input')
  expect(wrapper.emitted('restore')).toEqual([['older local']])
})
