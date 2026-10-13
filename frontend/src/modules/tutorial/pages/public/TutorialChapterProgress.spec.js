/** Chapter changes must not wait for or display an old chapter's progress response. */
import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import TutorialChapterPage from './TutorialChapterPage.vue'
const mocks = vi.hoisted(() => ({ route: null, chapter: vi.fn(), tutorial: vi.fn(), get: vi.fn(), save: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, RouterLink: { template: '<a><slot /></a>' } }))
vi.mock('../../api/tutorialApi', () => ({ getPublicTutorial: mocks.tutorial, getPublicChapter: mocks.chapter }))
vi.mock('../../api/learningApi', () => ({ getChapterProgress: mocks.get, saveChapterProgress: mocks.save,
  getOwnAnswer: vi.fn(), getOwnReferenceAnswer: vi.fn(), saveOwnAnswer: vi.fn() }))
vi.mock('../../../account/stores/authStore', () => ({ useAuthStore: () => ({ currentUser: { id: 1 } }) }))
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
let wrapper, now
beforeEach(() => {
  vi.resetAllMocks(); now = 100000; vi.spyOn(Date, 'now').mockImplementation(() => now)
  mocks.route = reactive({ path: '/useradmin/tutorials/course/a', params: { tutorialSlug: 'course', chapterSlug: 'a' }, query: {} })
  mocks.tutorial.mockResolvedValue({ slug: 'course', title: 'Course', groups: [{ chapters: [] }] })
  mocks.chapter.mockImplementation(async (_, slug) => ({ id: slug.charCodeAt(0), slug, title: slug, bodyMarkdown: '' }))
  mocks.get.mockResolvedValue({ progressRatio: 0.1 }); mocks.save.mockResolvedValue({ progressRatio: 0.2 })
})
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks() })
async function start() {
  wrapper = mount(TutorialChapterPage, { global: { stubs: { BlogProse: true, ReadingAside: true, ChapterStudyPage: true } } })
  await flushPromises()
}
async function switchChapter(slug) { mocks.route.params.chapterSlug = slug; await flushPromises() }
it('loads B immediately even if saving A is delayed', async () => {
  await start(); const a = deferred(); mocks.save.mockReturnValueOnce(a.promise)
  now += 5000; await switchChapter('b')
  expect(wrapper.get('h1').text()).toBe('b')
  expect(mocks.save).toHaveBeenCalledWith(97, expect.objectContaining({ studySecondsDelta: 5 }))
  a.resolve({ progressRatio: 0.9 }); await flushPromises(); expect(wrapper.text()).toContain('阅读位置 10%')
})
it('ignores delayed progress reads from A after B loads', async () => {
  const a = deferred(); mocks.get.mockReturnValueOnce(a.promise).mockResolvedValue({ progressRatio: 0.2 })
  await start(); await switchChapter('b'); a.resolve({ progressRatio: 0.9 }); await flushPromises()
  expect(wrapper.get('h1').text()).toBe('b'); expect(wrapper.text()).toContain('阅读位置 20%')
})
it('twenty chapter switches attribute each elapsed delta exactly once', async () => {
  await start()
  for (let i = 1; i <= 20; i++) { now += 2000; await switchChapter(String.fromCharCode(97 + i)) }
  expect(mocks.save).toHaveBeenCalledTimes(20)
  mocks.save.mock.calls.forEach(([id, payload], index) => {
    expect(id).toBe(97 + index); expect(payload.studySecondsDelta).toBe(2)
  })
})
it('old failure cannot overwrite B and is not automatically replayed', async () => {
  await start(); const a = deferred(); mocks.save.mockReturnValueOnce(a.promise)
  await switchChapter('b'); a.reject(new Error('timeout')); await flushPromises()
  expect(wrapper.find('[role="alert"]').exists()).toBe(false); expect(mocks.save).toHaveBeenCalledTimes(1)
})
it('flushes only visible seconds on hiding, without counting the hidden interval', async () => {
  await start(); const hidden = vi.spyOn(document, 'visibilityState', 'get')
  now += 3000; hidden.mockReturnValue('hidden'); document.dispatchEvent(new Event('visibilitychange')); await flushPromises()
  expect(mocks.save).toHaveBeenCalledWith(97, expect.objectContaining({ studySecondsDelta: 3 }))
  now += 60000; hidden.mockReturnValue('visible'); document.dispatchEvent(new Event('visibilitychange'))
  now += 2000; await switchChapter('b')
  expect(mocks.save.mock.calls[1][1].studySecondsDelta).toBe(2)
})
it('does not update progress after unmount', async () => {
  await start(); const a = deferred(); mocks.save.mockReturnValueOnce(a.promise)
  const state = wrapper.vm.$.setupState; wrapper.unmount(); a.resolve({ progressRatio: 0.9 }); await flushPromises()
  expect(state.reading.progressRatio).toBe(0.1)
})
