import { afterEach,describe,expect,it,vi } from 'vitest'
import { createWritingAutosave } from './writingAutosave'
afterEach(()=>vi.useRealTimers())
describe('private writing autosave',()=>{
 it('debounces edits and does not create revision snapshots',async()=>{
  vi.useFakeTimers();let body='first',version=0;const save=vi.fn(async p=>({rowVersion:p.rowVersion+1})),states=[]
  const saver=createWritingAutosave({read:()=>({bodyMarkdown:body,rowVersion:version}),save,applyVersion:r=>version=r.rowVersion,onState:s=>states.push(s)})
  saver.change();body='second';saver.change();await vi.advanceTimersByTimeAsync(999);expect(save).not.toHaveBeenCalled()
  await vi.advanceTimersByTimeAsync(1);expect(save).toHaveBeenCalledExactlyOnceWith({bodyMarkdown:'second',rowVersion:0});expect(version).toBe(1);expect(states.at(-1)).toBe('saved');saver.dispose()
 })
 it('queues a newer edit after an in-flight save using its returned version',async()=>{
  let body='first',version=0,resolve
  const save=vi.fn().mockImplementationOnce(()=>new Promise(r=>{resolve=r})).mockImplementation(async p=>({rowVersion:p.rowVersion+1}))
  const saver=createWritingAutosave({read:()=>({bodyMarkdown:body,rowVersion:version}),save,applyVersion:r=>version=r.rowVersion,onState:()=>{}})
  saver.change();const first=saver.flush();body='newer';saver.change();resolve({rowVersion:1});await first
  expect(save.mock.calls.map(c=>c[0])).toEqual([{bodyMarkdown:'first',rowVersion:0},{bodyMarkdown:'newer',rowVersion:1}]);expect(version).toBe(2);saver.dispose()
 })
 it('retains the draft and stops on 409 until explicit merge resumes saving',async()=>{
  let version=2,body='my draft';const states=[],save=vi.fn().mockRejectedValueOnce({response:{status:409}}).mockResolvedValue({rowVersion:4})
  const saver=createWritingAutosave({read:()=>({bodyMarkdown:body,rowVersion:version}),save,applyVersion:r=>version=r.rowVersion,onState:s=>states.push(s)})
  saver.change();expect(await saver.flush()).toBe(false);expect(body).toBe('my draft');expect(version).toBe(2);expect(states.at(-1)).toBe('conflict')
  body='merged draft';saver.change();await saver.flush();expect(save).toHaveBeenCalledTimes(1);version=3;await saver.resume();expect(save).toHaveBeenLastCalledWith({bodyMarkdown:'merged draft',rowVersion:3});saver.dispose()
 })
 it('reports network failure and preserves pending content',async()=>{
  const state=vi.fn(),saver=createWritingAutosave({read:()=>({bodyMarkdown:'unsaved'}),save:vi.fn().mockRejectedValue(new Error('offline')),applyVersion:vi.fn(),onState:state})
  saver.change();expect(await saver.flush()).toBe(false);expect(saver.isDirty()).toBe(true);expect(state).toHaveBeenLastCalledWith('error',expect.any(Error));saver.dispose();expect(saver.isDirty()).toBe(false)
 })
})
