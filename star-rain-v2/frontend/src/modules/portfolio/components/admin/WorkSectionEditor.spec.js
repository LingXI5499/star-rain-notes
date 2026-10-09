import { beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import WorkSectionEditor from './WorkSectionEditor.vue'
const api = vi.hoisted(() => ({ createWorkSection: vi.fn(), updateWorkSection: vi.fn(), removeWorkSection: vi.fn(), orderWorkSections: vi.fn(), getAdminWork: vi.fn() }))
vi.mock('../../api/portfolioApi', () => api)
const first = { id: '10', sectionType: 'CODE', title: '第一段', content: 'const x = 1', data: { language: 'javascript' }, visible: true, media: [] }
const second = { id: '20', sectionType: 'QUOTE', title: '第二段', content: 'text', data: {}, visible: true, media: [] }
function view() { return mount(WorkSectionEditor, { props: { work: { id: '1', sections: [first, second] } }, global: { stubs: { MarkdownEditor: true, MediaPicker: true, AppConfirmDialog: true } } }) }
beforeEach(() => { vi.clearAllMocks(); api.getAdminWork.mockResolvedValue({ id: '1', sections: [first, second] }); api.updateWorkSection.mockResolvedValue(first); api.createWorkSection.mockResolvedValue({ ...first, id: '30' }) })
describe('work block authoring', () => {
 it('selects an existing block into a separate edit area', async () => {
  const editor = view(); await editor.findAll('.section-list__select')[1].trigger('click'); await flushPromises()
  expect(editor.find('.section-compose input').element.value).toBe('第二段')
  expect(editor.find('.section-compose textarea').element.value).toBe('text')
 })
 it('creates no server draft when choosing a block type', async () => {
  const editor = view(); await editor.find('.section-list__add select').setValue('STATS'); await editor.find('.section-list__add button').trigger('click'); await flushPromises()
  expect(api.createWorkSection).not.toHaveBeenCalled(); expect(editor.text()).toContain('新增区块')
  await editor.find('.section-compose>button').trigger('click'); expect(editor.findAll('.section-item')).toHaveLength(1)
 })
 it('saves the selected block with its work and section ownership', async () => {
  const editor = view(); await editor.find('.section-compose input').setValue('修改标题'); await editor.find('.section-save').trigger('click'); await flushPromises()
  expect(api.updateWorkSection).toHaveBeenCalledWith('1','10',expect.objectContaining({ title: '修改标题', sectionType: 'CODE' }))
  expect(editor.emitted('updated')).toHaveLength(1)
 })
 it('sends the full work order when moving a block', async () => {
  const editor = view(); await editor.findAll('[aria-label="下移区块"]')[0].trigger('click'); await flushPromises()
  expect(api.orderWorkSections).toHaveBeenCalledWith('1',['20','10'])
 })
 it('keeps media IDs for selected Markdown images', async () => {
  const editor = view()
  await editor.find('.section-list__add select').setValue('MARKDOWN')
  await editor.find('.section-list__add button').trigger('click'); await flushPromises()
  const markdown = editor.findComponent({ name: 'MarkdownEditor' })
  const picking = markdown.props('pickImage')()
  editor.findComponent({ name: 'MediaPicker' }).vm.$emit('select', { id: '42', contentUrl: '/media/42', originalName: 'test.png' })
  await picking
  markdown.vm.$emit('update:modelValue', '![test](/media/42)'); await flushPromises()
  await editor.find('.section-save').trigger('click'); await flushPromises()
  expect(api.createWorkSection).toHaveBeenCalledWith('1', expect.objectContaining({ media: [{ mediaAssetId: '42', caption: 'test.png' }] }))
 })
 it('drops the archive protection reference when a Markdown image is removed', async () => {
  const block = { ...first, sectionType: 'MARKDOWN', data: {}, content: '![test](/media/42)', media: [{ mediaAssetId: '42', url: '/media/42' }] }
  const editor = mount(WorkSectionEditor, { props: { work: { id: '1', sections: [block] } }, global: { stubs: { MarkdownEditor: true, MediaPicker: true, AppConfirmDialog: true } } })
  editor.findComponent({ name: 'MarkdownEditor' }).vm.$emit('update:modelValue', 'Only text remains'); await flushPromises()
  await editor.find('.section-save').trigger('click'); await flushPromises()
  expect(api.updateWorkSection).toHaveBeenCalledWith('1','10',expect.objectContaining({ media: [], content: 'Only text remains' }))
 })
})
