/** Public detail requests must belong to the currently displayed route. */
import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BlogPostPage from './BlogPostPage.vue'
import WorkDetailPage from '../../portfolio/pages/WorkDetailPage.vue'
const mocks = vi.hoisted(() => ({ route: null, blog: vi.fn(), work: vi.fn(), admin: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, RouterLink: { template: '<a><slot /></a>' } }))
vi.mock('../api/blogApi', () => ({ getPublicPost: mocks.blog }))
vi.mock('../../portfolio/api/portfolioApi', () => ({ getPublicWork: mocks.work, getAdminWork: mocks.admin }))
vi.mock('../../../shared/viewMode', () => ({ useViewMode: () => ({ contentPath: path => path }) }))
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
let wrapper
beforeEach(() => { vi.resetAllMocks(); mocks.route = reactive({ params: { slug: 'a' } }) })
afterEach(() => wrapper?.unmount())
describe.each([
  ['blog', BlogPostPage, mocks.blog], ['work', WorkDetailPage, mocks.work],
])('%s request generations', (_, component, request) => {
  async function start() {
    const a = deferred(), b = deferred()
    request.mockReturnValueOnce(a.promise).mockReturnValueOnce(b.promise)
    wrapper = mount(component, { global: { stubs: { BlogProse: true, ReadingAside: true,
      ReadingProgress: true, ArticleOutline: true, WorkSectionRenderer: true } } })
    mocks.route.params.slug = 'b'; await flushPromises()
    expect(request).toHaveBeenCalledTimes(2)
    return { a, b }
  }
  it('slow A cannot overwrite fast B', async () => {
    const { a, b } = await start(); b.resolve({ title: 'B' }); await flushPromises()
    a.resolve({ title: 'A' }); await flushPromises(); expect(wrapper.get('h1').text()).toBe('B')
  })
  it('does not add a second main inside the public shell', async () => {
    const { a, b } = await start(); b.resolve({ title: 'B' }); a.resolve({ title: 'A' }); await flushPromises()
    expect(wrapper.find('main').exists()).toBe(false)
  })
  it('slow A cannot hide a fast B failure', async () => {
    const { a, b } = await start(); b.reject({ response: { status: 404 } }); await flushPromises()
    a.resolve({ title: 'A' }); await flushPromises(); expect(wrapper.find('h1').exists()).toBe(false)
    expect(wrapper.text()).toContain('不存在')
  })
  it('A failure cannot overwrite B success', async () => {
    const { a, b } = await start(); b.resolve({ title: 'B' }); await flushPromises()
    a.reject({ response: { status: 404 } }); await flushPromises(); expect(wrapper.get('h1').text()).toBe('B')
  })
  it('A completion cannot end B loading', async () => {
    const { a, b } = await start(); a.resolve({ title: 'A' }); await flushPromises()
    expect(wrapper.text()).toContain('正在加载'); b.resolve({ title: 'B' }); await flushPromises()
  })
  it('invalidates an in-flight request on unmount', async () => {
    const { a, b } = await start(); const state = wrapper.vm.$.setupState
    wrapper.unmount(); b.resolve({ title: 'B' }); a.resolve({ title: 'A' }); await flushPromises()
    expect(component === BlogPostPage ? state.post : state.work).toBeNull()
  })
})
it('keeps a denied admin preview denied after a public request finishes', async () => {
  const a = deferred(); mocks.work.mockReturnValue(a.promise)
  mocks.admin.mockRejectedValue({ response: { status: 403, data: { message: 'Forbidden' } } })
  wrapper = mount(WorkDetailPage, { global: { stubs: { ReadingAside: true, BlogProse: true } } })
  mocks.route.params = { workId: '2' }; await flushPromises(); a.resolve({ title: 'Public A' }); await flushPromises()
  expect(wrapper.find('h1').exists()).toBe(false); expect(wrapper.find('[role="alert"]').exists()).toBe(true)
})
