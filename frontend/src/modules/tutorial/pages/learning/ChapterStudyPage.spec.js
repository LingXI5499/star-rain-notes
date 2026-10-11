/**
 * ChapterStudyPage.spec.js：教程与学习模块的自动化测试。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue-router、vitest、./ChapterStudyPage.vue、../../api/learningApi。
 * 阅读入口：plan（计划的处理函数）；state（状态的处理函数）；mountReader（挂载Reader）。
 * 验证场景：plan scoped normal reader；shows only selected chapters in the shared reader and links the next selected chapter；starts recall against the owned plan task rather than a forged query task；rejects a chapter outside the plan before loading study content；unlocks questions after all current cards have actual initial evidence。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ChapterStudyPage from './ChapterStudyPage.vue'
import { getStudyPlan, getStudyState, startInitialSession, saveChapterProgress } from '../../api/learningApi'
vi.mock('../../api/learningApi', () => ({ getStudyPlan: vi.fn(), getStudyTask: vi.fn(), getStudyState: vi.fn(), startInitialSession: vi.fn(), saveChapterProgress: vi.fn() }))
vi.mock('../../api/tutorialApi', () => ({ getPublicTutorial: vi.fn() }))
const tutorial = { id: 't', title: 'Tutorial', slug: 'tutorial', groups: [{ id: 'g', title: 'Group', chapters: [{ id: 'c1', slug: 'first', title: 'First' }, { id: 'outside', slug: 'outside', title: 'Outside' }, { id: 'c2', slug: 'second', title: 'Second' }] }] }
/**
 * 计划的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const plan = () => ({ id: 'p', tutorialId: 't', tutorialSlug: 'tutorial', name: 'Plan', status: 'ACTIVE', completedChapters: 0, totalChapters: 2, chapters: [{ chapterId: 'c1', chapterSlug: 'first', completed: false }, { chapterId: 'c2', chapterSlug: 'second', completed: false }], tasks: [{ id: 'task', chapters: [{ chapterId: 'c1' }, { chapterId: 'c2' }] }] })
/**
 * 状态的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const state = () => ({ chapterId: 'c1', title: 'First', bodyMarkdown: '# Body', requiredCardIds: ['card'], initialCardIds: [], completed: false, questions: [{ id: 'question' }], session: null })
/**
 * 挂载Reader。
 * 主要协作：createRouter、createMemoryHistory、router.push、router.isReady、mount。
 * @param id = 'c1' 标识c1；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function mountReader(id = 'c1') {
 const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/useradmin/tutorials/:tutorialSlug/:chapterSlug', component: { template: '<div />' } }, { path: '/useradmin/learning/:pathMatch(.*)*', component: { template: '<div />' } }] })
 await router.push('/useradmin/tutorials/tutorial/first?plan=p&task=forged'); await router.isReady()
 const wrapper = mount(ChapterStudyPage, { props: { chapterId: id, tutorial }, global: { plugins: [router], stubs: { BlogProse: true, ReadingAside: true, RecallSession: true, QuestionAnswers: true } } }); await flushPromises()
 return wrapper
}
describe('plan scoped normal reader', () => {
 beforeEach(() => { vi.resetAllMocks(); getStudyPlan.mockResolvedValue(plan()); getStudyState.mockResolvedValue(state()); saveChapterProgress.mockResolvedValue({}) })
 it('shows only selected chapters in the shared reader and links the next selected chapter', async () => {
  const w = await mountReader(); expect(w.find('.tutorial-reader__sidebar').exists()).toBe(true)
  expect(w.text()).not.toContain('Outside'); expect(w.get('nav[aria-label="章节导航"]').text()).toContain('Second')
  expect(w.findComponent({ name: 'QuestionAnswers' }).props('disabled')).toBe(true)
  w.unmount()
 })
 it('starts recall against the owned plan task rather than a forged query task', async () => {
  const w = await mountReader(); startInitialSession.mockResolvedValue({ id: 'session', items: [] })
  await w.findAll('button').find(b => b.text() === '开始知识回忆').trigger('click'); await flushPromises()
  expect(startInitialSession).toHaveBeenCalledWith('c1', 'task'); w.unmount()
 })
 it('rejects a chapter outside the plan before loading study content', async () => {
  const w = await mountReader('outside'); expect(w.text()).toContain('这个章节不在当前学习计划中')
  expect(getStudyState).not.toHaveBeenCalled(); expect(startInitialSession).not.toHaveBeenCalled(); w.unmount()
 })
 it('unlocks questions after all current cards have actual initial evidence', async () => {
  getStudyState.mockResolvedValue({ ...state(), initialCardIds: ['card'], session: { id: 's', status: 'IN_PROGRESS', items: [{ knowledgeCardId: 'card', status: 'COMPLETED' }] } })
  const w = await mountReader(); expect(w.findComponent({ name: 'QuestionAnswers' }).props('disabled')).toBe(false); w.unmount()
 })
 it('shows completion and a return to active plans after the last answer is saved', async () => {
  const w = await mountReader()
  getStudyState.mockResolvedValue({ ...state(), completed: true, initialCardIds: ['card'] })
  getStudyPlan.mockResolvedValue({ ...plan(), status: 'COMPLETED', completedChapters: 2 })
  w.findComponent({ name: 'QuestionAnswers' }).vm.$emit('saved'); await flushPromises()
  expect(w.text()).toContain('学习计划已完成'); expect(w.text()).toContain('计划已退出当前学习列表'); w.unmount()
 })
 it('prevents card evaluation and answers in a paused plan', async () => {
  getStudyPlan.mockResolvedValue({ ...plan(), status: 'PAUSED' }); getStudyState.mockResolvedValue({ ...state(), session: { id: 's', items: [] } })
  const w = await mountReader(); expect(w.findComponent({ name: 'RecallSession' }).props('disabled')).toBe(true); expect(w.findComponent({ name: 'QuestionAnswers' }).props('disabled')).toBe(true); w.unmount()
 })
})
