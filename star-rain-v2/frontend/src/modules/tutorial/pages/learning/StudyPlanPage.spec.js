import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudyPlanPage from './StudyPlanPage.vue'
import { getPublicTutorial, listPublicTutorials } from '../../api/tutorialApi'
import { listStudyPlans, getStudyPlan, previewStudyPlan, createStudyPlan, transitionStudyPlan, startStudyTask } from '../../api/learningApi'
vi.mock('../../api/tutorialApi', () => ({ getPublicTutorial: vi.fn(), listPublicTutorials: vi.fn() }))
vi.mock('../../api/learningApi', () => ({ listStudyPlans: vi.fn(), getStudyPlan: vi.fn(), previewStudyPlan: vi.fn(), createStudyPlan: vi.fn(), updateStudyPlan: vi.fn(), transitionStudyPlan: vi.fn(), startStudyTask: vi.fn() }))
const tutorial = { id: 't1', title: 'Tutorial', slug: 'tutorial', groups: [{ id: 'g1', title: 'Group', chapters: [{ id: 'c1', title: 'Ready', cardCount: 2, questionCount: 0 }, { id: 'c2', title: 'Reading', cardCount: 0, questionCount: 1 }] }] }
async function mountPage(planId = 'new') {
 const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/useradmin/learning/plans/:planId?', component: { template: '<div />' } }, { path: '/useradmin/tutorials/:tutorialSlug/:chapterSlug', component: { template: '<div />' } }, { path: '/useradmin/learning/history', component: { template: '<div />' } }] })
 await router.push(`/useradmin/learning/plans${planId ? `/${planId}` : ''}`); await router.isReady()
 const wrapper = mount(StudyPlanPage, { global: { plugins: [router], stubs: { LearningNav: true } } }); await flushPromises()
 return { wrapper, router }
}
async function fillPlan(wrapper) {
 await wrapper.get('input[placeholder="例如：Java 集合强化"]').setValue('My plan')
 await wrapper.find('select').setValue('t1'); await flushPromises()
 await wrapper.find('.scope-picker__chapter button').trigger('click'); await flushPromises()
}
describe('study plan creation workflow', () => {
 beforeEach(() => {
  vi.resetAllMocks(); listStudyPlans.mockResolvedValue([]); listPublicTutorials.mockResolvedValue({ items: [{ id: 't1', title: 'Tutorial', slug: 'tutorial' }] }); getPublicTutorial.mockResolvedValue(tutorial)
  previewStudyPlan.mockResolvedValue({ tasks: [{ sequenceNo: 1, cardCount: 2, questionCount: 0, chapters: [{ chapterTitle: 'Ready' }] }] })
  getStudyPlan.mockResolvedValue({ id: 'p1', name: 'My plan', tutorialId: 't1', status: 'DRAFT', targetCardsPerTask: 12, chapters: [{ chapterId: 'c1' }], tasks: [] })
 })
 it('renders only unended plan cards and directly starts at the next tutorial chapter', async () => {
  listStudyPlans.mockResolvedValue([{ id: 'p1', name: 'Active plan', status: 'ACTIVE', completedChapters: 1, totalChapters: 2 }, { id: 'done', name: 'Ended plan', status: 'COMPLETED' }])
  getStudyPlan.mockResolvedValue({ id: 'p1', status: 'ACTIVE', tutorialSlug: 'tutorial', chapters: [{ chapterId: 'c1', chapterSlug: 'first', completed: true }, { chapterId: 'c2', chapterSlug: 'second', completed: false }], tasks: [{ id: 'task', chapters: [{ chapterId: 'c2' }] }] })
  startStudyTask.mockResolvedValue({ id: 'task', status: 'IN_PROGRESS' })
  const { wrapper, router } = await mountPage('')
  expect(wrapper.findAll('.learning-plan-card')).toHaveLength(1); expect(wrapper.text()).not.toContain('Ended plan')
  await wrapper.findAll('button').find(b => b.text() === '继续学习').trigger('click'); await flushPromises()
  expect(startStudyTask).toHaveBeenCalledWith('task'); expect(router.currentRoute.value.path).toBe('/useradmin/tutorials/tutorial/second'); expect(router.currentRoute.value.query.plan).toBe('p1')
 })
 it('refreshes and removes a plan completed elsewhere before continuing study', async () => {
  listStudyPlans.mockResolvedValueOnce([{ id: 'p1', status: 'ACTIVE', name: 'Finishing plan' }]).mockResolvedValue([])
  getStudyPlan.mockResolvedValue({ id: 'p1', status: 'COMPLETED', chapters: [{ chapterId: 'c1', completed: true }] })
  const { wrapper } = await mountPage(''); await wrapper.findAll('button').find(b => b.text() === '继续学习').trigger('click'); await flushPromises()
  expect(wrapper.findAll('.learning-plan-card')).toHaveLength(0); expect(startStudyTask).not.toHaveBeenCalled(); expect(wrapper.text()).toContain('已经完成')
 })
 it('reuses a completed plan for the next round and enters its scoped reader', async () => {
  getStudyPlan.mockResolvedValue({ id: 'p1', name: 'My plan', tutorialId: 't1', status: 'COMPLETED', studyRound: 1, completedChapters: 1, totalChapters: 1, chapters: [{ chapterId: 'c1', completed: true }], tasks: [] })
  transitionStudyPlan.mockResolvedValueOnce({ id: 'p1', status: 'DRAFT', studyRound: 2 }).mockResolvedValueOnce({ id: 'p1', status: 'ACTIVE', studyRound: 2, tutorialSlug: 'tutorial', chapters: [{ chapterId: 'c1', chapterSlug: 'ready', completed: false }], tasks: [{ id: 'round2task', chapters: [{ chapterId: 'c1' }] }] })
  startStudyTask.mockResolvedValue({ id: 'round2task', status: 'IN_PROGRESS' })
  const { wrapper, router } = await mountPage('p1')
  expect(wrapper.findAll('a').find(a => a.text() === '选择分组学习').attributes('href')).toContain('tutorialId=t1')
  await wrapper.findAll('button').find(b => b.text() === '再学一次').trigger('click'); await flushPromises()
  expect(transitionStudyPlan.mock.calls).toEqual([['p1', 'restart'], ['p1', 'activate']])
  expect(createStudyPlan).not.toHaveBeenCalled(); expect(startStudyTask).toHaveBeenCalledWith('round2task')
  expect(router.currentRoute.value.path).toBe('/useradmin/tutorials/tutorial/ready'); expect(router.currentRoute.value.query.plan).toBe('p1')
 })
 it('blocks an empty or ineligible range and previews a valid chapter with no questions', async () => {
  const { wrapper } = await mountPage()
  expect(wrapper.find('button[type="submit"]').element.disabled).toBe(true)
  await fillPlan(wrapper)
  expect(wrapper.findAll('.scope-picker__chapter button')[1].element.disabled).toBe(true)
  await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(previewStudyPlan).toHaveBeenCalledWith(expect.objectContaining({ chapterIds: ['c1'], groupIds: [], entireTutorial: false }), undefined)
  expect(wrapper.text()).toContain('任务预览')
 })
 it('preserves the form after a rejected preview and refreshes availability without reloading the page', async () => {
  previewStudyPlan.mockRejectedValue({ response: { status: 409, data: { message: '范围发生变化' } } })
  const { wrapper } = await mountPage(); await fillPlan(wrapper)
  listStudyPlans.mockResolvedValue([{ id: 'other', name: 'Other plan', status: 'ACTIVE', chapters: [{ chapterId: 'c1' }] }])
  await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(wrapper.get('input[placeholder="例如：Java 集合强化"]').element.value).toBe('My plan')
  expect(wrapper.find('select').element.value).toBe('t1'); expect(wrapper.text()).toContain('Other plan'); expect(wrapper.text()).not.toContain('重新加载')
  expect(wrapper.find('button[type="submit"]').element.disabled).toBe(true)
 })
 it('retains the saved draft and navigates to it if activation fails', async () => {
  createStudyPlan.mockResolvedValue({ id: 'p1', status: 'DRAFT' }); transitionStudyPlan.mockRejectedValue({ response: { data: { message: '章节内容已变化' } } })
  const { wrapper, router } = await mountPage(); await fillPlan(wrapper)
  await wrapper.find('form').trigger('submit'); await flushPromises()
  await wrapper.findAll('button').find(button => button.text() === '保存并启动').trigger('click'); await flushPromises()
  expect(createStudyPlan).toHaveBeenCalledTimes(1); expect(router.currentRoute.value.params.planId).toBe('p1')
  expect(wrapper.text()).toContain('草稿已保存，但启动失败'); expect(wrapper.text()).toContain('无需重新创建')
  expect(wrapper.findAll('button').some(button => button.text() === '启动')).toBe(true)
 })
 it('keeps the saved plan when entering its first chapter fails', async () => {
  createStudyPlan.mockResolvedValue({ id: 'p1', status: 'DRAFT' })
  transitionStudyPlan.mockResolvedValue({ id: 'p1', status: 'ACTIVE', tutorialSlug: 'tutorial', chapters: [{ chapterId: 'c1', chapterSlug: 'ready', completed: false }], tasks: [{ id: 'task', chapters: [{ chapterId: 'c1' }] }] })
  startStudyTask.mockRejectedValue({ response: { data: { message: '暂时无法启动任务' } } })
  const { wrapper, router } = await mountPage(); await fillPlan(wrapper); await wrapper.find('form').trigger('submit'); await flushPromises()
  await wrapper.findAll('button').find(b => b.text() === '保存并启动').trigger('click'); await flushPromises()
  expect(createStudyPlan).toHaveBeenCalledTimes(1); expect(router.currentRoute.value.params.planId).toBe('p1'); expect(wrapper.text()).toContain('进入学习失败'); expect(wrapper.find('form').exists()).toBe(false)
 })
 it('does not treat the edited draft as another plan occupying its own chapter', async () => {
  listStudyPlans.mockResolvedValue([{ id: 'p1', name: 'My plan', status: 'DRAFT', chapters: [{ chapterId: 'c1' }] }])
  const { wrapper } = await mountPage('p1')
  await wrapper.findAll('button').find(button => button.text() === '编辑草稿').trigger('click'); await flushPromises()
  expect(wrapper.find('button[type="submit"]').element.disabled).toBe(false)
 })
 it('blocks creation at the five-plan quota before sending a preview request', async () => {
  listStudyPlans.mockResolvedValue(Array.from({ length: 5 }, (_, index) => ({ id: String(index), status: 'DRAFT', chapters: [] })))
  const { wrapper } = await mountPage(); await fillPlan(wrapper)
  expect(wrapper.find('button[type="submit"]').element.disabled).toBe(true)
  await wrapper.find('form').trigger('submit'); await flushPromises()
  expect(previewStudyPlan).not.toHaveBeenCalled(); expect(wrapper.text()).toContain('请先完成或取消一个计划')
 })
 it('does not display a stale preview after navigating to another plan while the request is in flight', async () => {
  let finishPreview
  previewStudyPlan.mockImplementation(() => new Promise(resolve => { finishPreview = resolve }))
  const { wrapper, router } = await mountPage(); await fillPlan(wrapper)
  await wrapper.find('form').trigger('submit'); await flushPromises()
  await router.push('/useradmin/learning/plans/p1'); await flushPromises()
  finishPreview({ tasks: [{ sequenceNo: 1, cardCount: 2, questionCount: 0, chapters: [{ chapterTitle: 'Ready' }] }] }); await flushPromises()
  expect(wrapper.text()).not.toContain('任务预览'); expect(wrapper.text()).not.toContain('保存并启动')
 })
})
