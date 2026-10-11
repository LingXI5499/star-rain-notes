/**
 * TutorialWorkspace.spec.js：教程与学习模块的自动化测试。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue、vitest、./TutorialCurriculumPage.vue、./ChapterEditPage.vue、../../api/tutorialApi。
 * 阅读入口：curriculum（课程目录的处理函数）；open（打开当前操作）；button（button的处理函数）。
 * 验证场景：tutorial workspace interactions；saves title, summary and body in one versioned request；retains local input on HTTP 409 without reporting success；does not withdraw when the confirmation is declined；withdraws only the selected chapter and reports preserved history。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Curriculum from './TutorialCurriculumPage.vue'
import ChapterEdit from './ChapterEditPage.vue'
import * as api from '../../api/tutorialApi'

const state = vi.hoisted(() => ({ route: null, push: vi.fn(), replace: vi.fn(), ask: vi.fn(), permissions: new Set() }))
vi.mock('vue-router', () => ({ useRoute: () => state.route, useRouter: () => ({ push: state.push, replace: state.replace }), onBeforeRouteLeave: vi.fn(), onBeforeRouteUpdate: vi.fn() }))
vi.mock('../../../account/stores/authStore', () => ({ useAuthStore: () => ({ hasPermission: code => state.permissions.has(code) }) }))
vi.mock('../../api/tutorialApi', () => ({ listPendingTutorialOperations: vi.fn(), getAdminCurriculum: vi.fn(), getAdminChapter: vi.fn(), createChapter: vi.fn(), updateChapter: vi.fn(), updateChapterBody: vi.fn(), createGroup: vi.fn(), deleteChapter: vi.fn(), deleteGroup: vi.fn(), publishChapter: vi.fn(), withdrawChapter: vi.fn(), reorderChapters: vi.fn(), reorderGroups: vi.fn(), updateGroup: vi.fn(), submitTutorialReview: vi.fn() }))
vi.mock('../../../media/support/useMediaPicker', () => ({ useMediaPicker: () => ({ pickerOpen: false, pick: vi.fn(), settle: vi.fn() }) }))
/**
 * 课程目录的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const curriculum = () => ({ tutorial: { id: '1', title: 'Java', publicationStatus: 'PUBLISHED', editingStatus: 'DRAFT' }, groups: [{ id: '3', title: 'Group', status: 'ACTIVE', chapters: [{ id: '4', title: 'Intro', status: 'PUBLISHED', rowVersion: 2 }] }] })
const stubs = {
 AdminConfirmDialog: { setup(_, { expose }) { expose({ ask: state.ask }) }, template: '<div />' },
 MarkdownEditor: { props: ['modelValue'], emits: ['update:modelValue'], setup(props, { expose }) { expose({ getMarkdown: () => props.modelValue }) }, template: '<textarea aria-label="正文" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' },
 ChapterAuthorNav: true, MediaPicker: true,
}
const wrappers = []
import { afterEach } from 'vitest'
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()) })
/**
 * 打开当前操作。
 * 主要协作：mount、wrappers.push。
 * @param component component；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function open(component) { const wrapper = mount(component, { global: { stubs } }); wrappers.push(wrapper); return wrapper }
/**
 * button的处理函数。
 * 主要协作：.find、wrapper.findAll、item.text。
 * @param wrapper wrapper；默认值、解构字段与可选参数见函数签名。
 * @param text 文本；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function button(wrapper, text) { return wrapper.findAll('button').find(item => item.text() === text) }
beforeEach(() => {
 vi.clearAllMocks(); state.route = reactive({ params: { tutorialId: '1', chapterId: '4' }, query: {} }); state.ask.mockResolvedValue(true)
 state.permissions = new Set(['tutorial:publish', 'tutorial:withdraw'])
 api.getAdminCurriculum.mockResolvedValue(curriculum())
 api.listPendingTutorialOperations.mockResolvedValue([])
 api.getAdminChapter.mockResolvedValue({ id: '4', tutorialId: '1', groupId: '3', title: 'Intro', summary: 'Summary', bodyMarkdown: 'Body', rowVersion: 2 })
 api.updateChapter.mockResolvedValue({ rowVersion: 3 })
})
describe('tutorial workspace interactions', () => {
 it('saves title, summary and body in one versioned request', async () => {
  const wrapper = open(ChapterEdit); await flushPromises()
  await wrapper.find('input[aria-label="章节标题"]').setValue('Edited title')
  await wrapper.find('textarea[aria-label="正文"]').setValue('Edited body')
  await button(wrapper, '保存修改').trigger('click'); await flushPromises()
  expect(api.updateChapter).toHaveBeenCalledWith('4', { title: 'Edited title', summary: 'Summary', bodyMarkdown: 'Edited body', rowVersion: 2 })
  expect(api.updateChapterBody).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('修改已保存')
 })
 it('retains local input on HTTP 409 without reporting success', async () => {
  api.updateChapter.mockRejectedValue({ response: { status: 409, data: { message: '章节版本冲突，请重新加载后合并' } } })
  const wrapper = open(ChapterEdit); await flushPromises()
  await wrapper.find('input[aria-label="章节标题"]').setValue('Unsaved local title')
  await button(wrapper, '保存修改').trigger('click'); await flushPromises()
  expect(wrapper.find('input[aria-label="章节标题"]').element.value).toBe('Unsaved local title')
  expect(wrapper.text()).toContain('章节版本冲突'); expect(wrapper.text()).not.toContain('修改已保存')
 })
 it('does not withdraw when the confirmation is declined', async () => {
  state.ask.mockResolvedValue(false); const wrapper = open(Curriculum); await flushPromises()
  await button(wrapper, '撤回').trigger('click'); await flushPromises()
  expect(api.withdrawChapter).not.toHaveBeenCalled()
 })
 it('withdraws only the selected chapter and reports preserved history', async () => {
  const wrapper = open(Curriculum); await flushPromises()
  await button(wrapper, '撤回').trigger('click'); await flushPromises()
  expect(api.withdrawChapter).toHaveBeenCalledWith('4'); expect(wrapper.text()).toContain('历史学习记录已保留')
 })
 it('keeps one state-dependent action for editors and reports pending review without changing publication state', async () => {
  state.permissions = new Set(['tutorial:edit', 'tutorial:submit']); const wrapper = open(Curriculum); await flushPromises()
  api.withdrawChapter.mockResolvedValue({ reviewPending: true, reviewRequestId: '9' })
  expect(button(wrapper, '发布')).toBeUndefined(); expect(button(wrapper, '撤回')).toBeDefined()
  await button(wrapper, '撤回').trigger('click'); await flushPromises()
  expect(api.withdrawChapter).toHaveBeenCalledWith('4'); expect(wrapper.text()).toContain('通过后才会执行')
  expect(wrapper.find('.curriculum-admin__status').text()).toBe('已发布')
 })
 it('preserves draft creation as an approval proposal without navigating to an undefined chapter', async () => {
  state.route.params.chapterId = 'new'; api.createChapter.mockResolvedValue({ reviewPending: true })
  const wrapper = open(ChapterEdit); await flushPromises()
  await wrapper.find('input[aria-label="章节标题"]').setValue('Draft')
  await wrapper.find('textarea[aria-label="正文"]').setValue('Draft body')
  await button(wrapper, '保存修改').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('已提交超级管理员审核'); expect(state.replace).not.toHaveBeenCalled()
 })
 it('disables publication actions while a related operation is pending', async () => {
  api.listPendingTutorialOperations.mockResolvedValue([{scopeKey:'tutorial:1'}])
  const wrapper = open(Curriculum); await flushPromises()
  expect(button(wrapper, '撤回').attributes('disabled')).toBeDefined()
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
