import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ChapterExercises from './ChapterExercises.vue'
import { listCards, listQuestions, createCard, createQuestion, updateCard, updateQuestion, deleteCard } from '../api/tutorialApi'
const editorDrafts = vi.hoisted(() => ({}))
vi.mock('../api/tutorialApi', () => ({ listCards: vi.fn(), listQuestions: vi.fn(), createCard: vi.fn(), createQuestion: vi.fn(), updateCard: vi.fn(), updateQuestion: vi.fn(), deleteCard: vi.fn(), deleteQuestion: vi.fn(), reorderCards: vi.fn(), reorderQuestions: vi.fn() }))
vi.mock('../../../shared/editor/MarkdownEditor.vue', () => ({ default: {
 props: ['modelValue', 'placeholder'],
 setup(props, { expose }) { expose({ getMarkdown: () => editorDrafts[props.placeholder] ?? props.modelValue }) },
 template: '<div />',
} }))
describe('chapter learning content editor', () => {
 beforeEach(() => { vi.restoreAllMocks(); vi.clearAllMocks(); window.confirm = vi.fn(); Object.keys(editorDrafts).forEach(key => delete editorDrafts[key]); listCards.mockResolvedValue([]); listQuestions.mockResolvedValue([]) })
 it('saves the latest card Markdown even before the editor emits v-model', async () => {
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  await wrapper.find('textarea').setValue('Recall this')
  editorDrafts['知识卡片答案…'] = 'Latest editor text'
  expect(wrapper.vm.isDirty()).toBe(true)
  await wrapper.findAll('button').find(b => b.text() === '保存卡片').trigger('click'); await flushPromises()
  expect(createCard).toHaveBeenCalledWith('9', { frontText: 'Recall this', backMarkdown: 'Latest editor text', status: 'ENABLED' })
 })
 it('renders only the question form on its own page and saves multiple relations', async () => {
  listCards.mockResolvedValue([{ id: '1', frontText: 'Card one', status: 'ENABLED' }, { id: '2', frontText: 'Card two', status: 'ENABLED' }])
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9', kind: 'questions' } }); await flushPromises()
  const panels = wrapper.findAll('.chapter-exercises__columns > section')
  expect(panels).toHaveLength(1); expect(wrapper.text()).not.toContain('新增卡片')
  await panels[0].find('textarea').setValue('Explain both cards')
  for (const input of panels[0].findAll('input[type="checkbox"]')) await input.setValue(true)
  editorDrafts['章节问题参考答案…'] = 'Latest reference'
  await panels[0].findAll('button').find(b => b.text() === '保存问题').trigger('click'); await flushPromises()
  expect(createQuestion).toHaveBeenCalledWith('9', { questionText: 'Explain both cards', referenceAnswer: 'Latest reference', status: 'ENABLED', knowledgeCardIds: ['1', '2'] })
 })
 it('rejects empty content before making a request', async () => {
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  await wrapper.findAll('button').find(b => b.text() === '保存卡片').trigger('click'); await flushPromises()
  expect(createCard).not.toHaveBeenCalled(); expect(wrapper.text()).toContain('请填写回忆问题和卡片答案')
 })
 it('allows a question without any knowledge card association', async () => {
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9', kind: 'questions' } }); await flushPromises()
  expect(wrapper.findAll('form')).toHaveLength(1); expect(wrapper.text()).toContain('仍可独立编写问题')
  await wrapper.find('textarea').setValue('An independent question'); editorDrafts['章节问题参考答案…'] = 'Reference'
  await wrapper.findAll('button').find(b => b.text() === '保存问题').trigger('click'); await flushPromises()
  expect(createQuestion).toHaveBeenCalledWith('9', { questionText: 'An independent question', referenceAnswer: 'Reference', status: 'ENABLED', knowledgeCardIds: [] })
 })
 it('does not warn about the empty editor trailing newline when navigating', async () => {
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  editorDrafts['知识卡片答案…'] = '\n'
  expect(wrapper.vm.isDirty()).toBe(false)
 })
 it('selects a list item into the right editor and keeps it selected after saving', async () => {
  const rows = [{ id: '1', frontText: 'First', backMarkdown: 'Answer one', status: 'ENABLED' }, { id: '2', frontText: 'Second', backMarkdown: 'Answer two', status: 'ENABLED' }]
  listCards.mockResolvedValue(rows); updateCard.mockResolvedValue({ id: '2' })
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  expect(wrapper.find('textarea').element.value).toBe('First')
  await wrapper.findAll('.chapter-exercises__select')[1].trigger('click'); await flushPromises()
  expect(wrapper.find('textarea').element.value).toBe('Second')
  await wrapper.find('textarea').setValue('Second edited')
  await wrapper.findAll('button').find(b => b.text() === '保存卡片').trigger('click'); await flushPromises()
  expect(updateCard).toHaveBeenCalledWith('2', { frontText: 'Second edited', backMarkdown: 'Answer two', status: 'ENABLED' })
  expect(wrapper.find('textarea').element.value).toBe('Second edited')
  expect(wrapper.findAll('.chapter-exercises__select')[1].attributes('aria-pressed')).toBe('true')
  expect(wrapper.vm.isDirty()).toBe(false)
 })
 it('retains a dirty draft when switching is declined and opens a new editor when confirmed', async () => {
  listCards.mockResolvedValue([{ id: '1', frontText: 'Original', backMarkdown: 'Answer', status: 'ENABLED' }])
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  await wrapper.find('textarea').setValue('Unsaved')
  await wrapper.findAll('button').find(b => b.text() === '新增卡片').trigger('click'); await flushPromises()
  expect(wrapper.find('textarea').element.value).toBe('Unsaved')
  confirm.mockReturnValue(true)
  await wrapper.findAll('button').find(b => b.text() === '新增卡片').trigger('click'); await flushPromises()
  expect(wrapper.find('textarea').element.value).toBe(''); expect(wrapper.vm.isDirty()).toBe(false)
 })
 it('keeps a newly created item selected so another save updates it', async () => {
  createCard.mockResolvedValue({ id: '3' }); updateCard.mockResolvedValue({ id: '3' })
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  await wrapper.find('textarea').setValue('New'); editorDrafts['知识卡片答案…'] = 'Answer'
  await wrapper.findAll('button').find(b => b.text() === '保存卡片').trigger('click'); await flushPromises()
  await wrapper.find('textarea').setValue('New edited')
  await wrapper.findAll('button').find(b => b.text() === '保存卡片').trigger('click'); await flushPromises()
  expect(createCard).toHaveBeenCalledTimes(1); expect(updateCard).toHaveBeenCalledWith('3', expect.objectContaining({ frontText: 'New edited' }))
 })
 it('clears the editor after deleting the selected item', async () => {
  listCards.mockResolvedValueOnce([{ id: '1', frontText: 'Original', backMarkdown: 'Answer', status: 'ENABLED' }]).mockResolvedValue([])
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9' } }); await flushPromises()
  await wrapper.findAll('button').find(b => b.text() === '删除').trigger('click'); await flushPromises()
  expect(deleteCard).toHaveBeenCalledWith('1'); expect(wrapper.find('textarea').element.value).toBe('')
 })
 it('selects and updates a question independently from cards', async () => {
  listQuestions.mockResolvedValue([{ id: '4', questionText: 'Why?', referenceAnswer: 'Because', status: 'ENABLED', knowledgeCardIds: [] }]); updateQuestion.mockResolvedValue({ id: '4' })
  const wrapper = mount(ChapterExercises, { props: { chapterId: '9', kind: 'questions' } }); await flushPromises()
  await wrapper.find('textarea').setValue('Why exactly?')
  await wrapper.findAll('button').find(b => b.text() === '保存问题').trigger('click'); await flushPromises()
  expect(updateQuestion).toHaveBeenCalledWith('4', { questionText: 'Why exactly?', referenceAnswer: 'Because', status: 'ENABLED', knowledgeCardIds: [] }); expect(wrapper.vm.isDirty()).toBe(false)
 })
})
