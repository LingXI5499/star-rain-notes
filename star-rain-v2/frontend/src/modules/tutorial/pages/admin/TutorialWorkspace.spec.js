import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Curriculum from './TutorialCurriculumPage.vue'
import ChapterEdit from './ChapterEditPage.vue'
import * as api from '../../api/tutorialApi'

const state = vi.hoisted(() => ({ route: null, push: vi.fn(), replace: vi.fn(), ask: vi.fn(), permissions: new Set() }))
vi.mock('vue-router', () => ({ useRoute: () => state.route, useRouter: () => ({ push: state.push, replace: state.replace }), onBeforeRouteLeave: vi.fn(), onBeforeRouteUpdate: vi.fn() }))
vi.mock('../../../account/stores/authStore', () => ({ useAuthStore: () => ({ hasPermission: code => state.permissions.has(code) }) }))
vi.mock('../../api/tutorialApi', () => ({ getAdminCurriculum: vi.fn(), getAdminChapter: vi.fn(), createChapter: vi.fn(), updateChapter: vi.fn(), updateChapterBody: vi.fn(), createGroup: vi.fn(), deleteChapter: vi.fn(), deleteGroup: vi.fn(), publishChapter: vi.fn(), withdrawChapter: vi.fn(), reorderChapters: vi.fn(), reorderGroups: vi.fn(), updateGroup: vi.fn(), submitTutorialReview: vi.fn() }))
vi.mock('../../../media/support/useMediaPicker', () => ({ useMediaPicker: () => ({ pickerOpen: false, pick: vi.fn(), settle: vi.fn() }) }))
const curriculum = () => ({ tutorial: { id: '1', title: 'Java', publicationStatus: 'PUBLISHED', editingStatus: 'DRAFT' }, groups: [{ id: '3', title: 'Group', status: 'ACTIVE', chapters: [{ id: '4', title: 'Intro', status: 'PUBLISHED', rowVersion: 2 }] }] })
const stubs = {
 AdminConfirmDialog: { setup(_, { expose }) { expose({ ask: state.ask }) }, template: '<div />' },
 MarkdownEditor: { props: ['modelValue'], emits: ['update:modelValue'], setup(props, { expose }) { expose({ getMarkdown: () => props.modelValue }) }, template: '<textarea aria-label="正文" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' },
 ChapterAuthorNav: true, MediaPicker: true,
}
const wrappers = []
import { afterEach } from 'vitest'
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()) })
function open(component) { const wrapper = mount(component, { global: { stubs } }); wrappers.push(wrapper); return wrapper }
function button(wrapper, text) { return wrapper.findAll('button').find(item => item.text() === text) }
beforeEach(() => {
 vi.clearAllMocks(); state.route = reactive({ params: { tutorialId: '1', chapterId: '4' }, query: {} }); state.ask.mockResolvedValue(true)
 state.permissions = new Set(['tutorial:publish', 'tutorial:withdraw'])
 api.getAdminCurriculum.mockResolvedValue(curriculum())
 api.getAdminChapter.mockResolvedValue({ id: '4', tutorialId: '1', groupId: '3', title: 'Intro', summary: 'Summary', bodyMarkdown: 'Body', rowVersion: 2 })
 api.updateChapter.mockResolvedValue({ rowVersion: 3 })
})
describe('tutorial workspace interactions', () => {
 it('saves title, summary and body in one versioned request', async () => {
  const wrapper = open(ChapterEdit); await flushPromises()
  await wrapper.find('input[aria-label="章节标题"]').setValue('Edited title')
  await wrapper.find('textarea[aria-label="正文"]').setValue('Edited body')
  await button(wrapper, '保存工作区').trigger('click'); await flushPromises()
  expect(api.updateChapter).toHaveBeenCalledWith('4', { title: 'Edited title', summary: 'Summary', bodyMarkdown: 'Edited body', rowVersion: 2 })
  expect(api.updateChapterBody).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('已保存到工作区')
 })
 it('retains local input on HTTP 409 without reporting success', async () => {
  api.updateChapter.mockRejectedValue({ response: { status: 409, data: { message: '章节版本冲突，请重新加载后合并' } } })
  const wrapper = open(ChapterEdit); await flushPromises()
  await wrapper.find('input[aria-label="章节标题"]').setValue('Unsaved local title')
  await button(wrapper, '保存工作区').trigger('click'); await flushPromises()
  expect(wrapper.find('input[aria-label="章节标题"]').element.value).toBe('Unsaved local title')
  expect(wrapper.text()).toContain('章节版本冲突'); expect(wrapper.text()).not.toContain('已保存到工作区')
 })
 it('does not withdraw when the confirmation is declined', async () => {
  state.ask.mockResolvedValue(false); const wrapper = open(Curriculum); await flushPromises()
  await button(wrapper, '撤回公开章节').trigger('click'); await flushPromises()
  expect(api.withdrawChapter).not.toHaveBeenCalled()
 })
 it('withdraws only the selected chapter and reports preserved history', async () => {
  const wrapper = open(Curriculum); await flushPromises()
  await button(wrapper, '撤回公开章节').trigger('click'); await flushPromises()
  expect(api.withdrawChapter).toHaveBeenCalledWith('4'); expect(wrapper.text()).toContain('历史学习记录已保留')
 })
 it('sends ordinary editors through review and hides direct publication and withdrawal', async () => {
  state.permissions = new Set(['tutorial:edit', 'tutorial:submit']); const wrapper = open(Curriculum); await flushPromises()
  expect(button(wrapper, '发布改动')).toBeUndefined(); expect(button(wrapper, '撤回公开章节')).toBeUndefined()
  await button(wrapper, '提交审核').trigger('click'); await flushPromises()
  expect(api.submitTutorialReview).toHaveBeenCalledWith('1', '4'); expect(api.publishChapter).not.toHaveBeenCalled()
 })
 it('does not navigate on row double click and reloads on route changes', async () => {
  const wrapper = open(Curriculum); await flushPromises()
  await wrapper.find('.curriculum-admin__chapter').trigger('dblclick'); expect(state.push).not.toHaveBeenCalled()
  state.route.params.tutorialId = '2'; await flushPromises()
  expect(api.getAdminCurriculum).toHaveBeenLastCalledWith('2')
 })
 it('ignores a late response from the previous tutorial route', async () => {
  let resolveOld
  api.getAdminCurriculum.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
  const wrapper = open(Curriculum); await flushPromises()
  const next = curriculum(); next.tutorial.title = 'Next tutorial'
  api.getAdminCurriculum.mockResolvedValue(next); state.route.params.tutorialId = '2'; await flushPromises()
  resolveOld(curriculum()); await flushPromises()
  expect(wrapper.find('h1').text()).toBe('Next tutorial')
 })

})
