import {afterEach,expect,it,vi} from 'vitest'
import {mount,flushPromises} from '@vue/test-utils'
import EditorMenuBar from './EditorMenuBar.vue'
let wrapper
afterEach(()=>{wrapper?.unmount();document.body.innerHTML=''})
it('uses keyboard navigation and closes on Escape before emitting an action',async()=>{
  wrapper=mount(EditorMenuBar,{props:{ready:true,headingKeys:{heading2:'Ctrl+Shift+H'}},attachTo:document.body})
  const file=wrapper.find('[data-group=file]')
  await file.trigger('keydown',{key:'ArrowDown'});await flushPromises()
  expect(document.activeElement.textContent).toContain('导入 Markdown')
  document.activeElement.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true}))
  await flushPromises()
  expect(document.querySelector('[role=menu]').getAttribute('aria-label')).toBe('编辑菜单')
  document.activeElement.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))
  await flushPromises()
  expect(document.querySelector('[role=menu]')).toBeNull()
  expect(document.activeElement.dataset.group).toBe('edit')
  await wrapper.find('[data-group=paragraph]').trigger('click')
  expect(document.querySelector('[role=menu]').textContent).toContain('Ctrl+Shift+H')
})
it('disables changes in read-only mode and removes global listeners on unmount',async()=>{
  const remove=vi.spyOn(document,'removeEventListener')
  wrapper=mount(EditorMenuBar,{props:{ready:true,readOnly:true,canUpload:false,canPick:true},attachTo:document.body})
  await wrapper.find('[data-group=format]').trigger('click')
  const items=[...document.querySelectorAll('[role=menu] button')]
  expect(items.find(element=>element.textContent.includes('上传图片'))).toBeUndefined()
  expect(items.find(element=>element.textContent.includes('媒体库')).disabled).toBe(true)
  expect(items.find(element=>element.textContent.includes('公开检查')).disabled).toBe(false)
  const outside=document.createElement('button');document.body.append(outside)
  outside.dispatchEvent(new Event('pointerdown',{bubbles:true}));await flushPromises()
  expect(document.querySelector('[role=menu]')).toBeNull()
  wrapper.unmount();wrapper=null
  expect(remove.mock.calls.some(([name])=>name==='pointerdown')).toBe(true)
  remove.mockRestore()
})
