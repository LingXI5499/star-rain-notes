/** Regression tests for the editor's persistence and navigation boundaries. */
import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { defineComponent, h } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BlogEditorPage from './BlogEditorPage.vue'

const api = vi.hoisted(() => Object.fromEntries([
  'createPost', 'createTag', 'deletePost', 'getAdminPost', 'listAdminTags',
  'publishPost', 'restorePost', 'updatePost', 'updatePostBody', 'withdrawPost', 'ask',
].map(key => [key, vi.fn()])))
vi.mock('../../api/blogApi', () => api)
const Editor = defineComponent({
  props: ['modelValue'], emits: ['update:modelValue'],
  setup(props, { emit, expose }) {
    let pending
    expose({ getMarkdown: () => pending ?? props.modelValue, typeBeforeCallback: value => { pending = value } })
    return () => h('textarea', { 'aria-label': '正文', value: props.modelValue,
      onInput: event => { pending = undefined; emit('update:modelValue', event.target.value) } })
  },
})
const Confirm = defineComponent({ setup(_, { expose }) { expose({ ask: api.ask }); return () => null } })
const article = (bodyMarkdown = 'A', overrides = {}) => ({ id: 1, title: 'Title', summary: '',
  tags: [], status: 'DRAFT', slug: 'title', bodyMarkdown, ...overrides })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
let wrapper, router
async function start(id = '1') {
  router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/useradmin/blog/editor/:postId', component: BlogEditorPage },
    { path: '/:pathMatch(.*)*', component: { template: '<p>Other page</p>' } },
  ] })
  await router.push(`/useradmin/blog/editor/${id}`)
  wrapper = mount(RouterView, { global: { plugins: [router], stubs: {
    MarkdownEditor: Editor, AdminConfirmDialog: Confirm, MediaPicker: true,
    BlogTagPicker: true, BlogStatusPill: true,
  } } })
  await flushPromises()
}
async function click(label) { await wrapper.findAll('button').find(button => button.text() === label).trigger('click'); await flushPromises() }
async function dirtyLeave(expected = true) {
  api.ask.mockResolvedValue(false)
  await router.push('/elsewhere')
  expect(api.ask).toHaveBeenCalledTimes(expected ? 1 : 0)
  expect(router.currentRoute.value.path).toBe(expected ? '/useradmin/blog/editor/1' : '/elsewhere')
}
beforeEach(() => {
  vi.resetAllMocks()
  api.getAdminPost.mockResolvedValue(article())
  api.listAdminTags.mockResolvedValue({ items: [], total: 0 })
  api.updatePost.mockImplementation(async (_, payload) => article('A', payload))
  api.updatePostBody.mockImplementation(async (_, body) => article(body))
  api.createPost.mockResolvedValue(article('', { id: 2 }))
  api.ask.mockResolvedValue(false)
})
afterEach(() => wrapper?.unmount())
describe('blog editor reliability', () => {
  it.each(['正文', '文章标题'])('guards navigation after editing %s', async label => {
    await start(); await wrapper.get(`[aria-label="${label}"]`).setValue('B'); await dirtyLeave()
  })
  it('syncs Vditor before a sidebar navigation', async () => {
    await start(); wrapper.findComponent(Editor).vm.typeBeforeCallback('B'); await dirtyLeave()
  })
  it('asks only once for the return button', async () => {
    await start(); await wrapper.get('[aria-label="正文"]').setValue('B')
    api.ask.mockResolvedValue(true); await click('返回列表')
    expect(api.ask).toHaveBeenCalledTimes(1); expect(router.currentRoute.value.path).toBe('/useradmin/blog/manage')
  })
  it('retains edits made during a body request', async () => {
    await start(); const request = deferred(); api.updatePostBody.mockReturnValue(request.promise)
    await click('保存正文'); await wrapper.get('[aria-label="正文"]').setValue('B')
    request.resolve(article('A')); await flushPromises(); await dirtyLeave()
  })
  it('body saving does not acknowledge an unsaved title', async () => {
    await start(); await wrapper.get('[aria-label="文章标题"]').setValue('New title')
    await click('保存正文'); await dirtyLeave()
  })
  it('metadata saving does not acknowledge an unsaved body', async () => {
    await start(); await wrapper.get('[aria-label="正文"]').setValue('B')
    await click('保存文章信息'); await dirtyLeave()
  })
  it('metadata response does not acknowledge newer metadata', async () => {
    await start(); const request = deferred(); api.updatePost.mockReturnValue(request.promise)
    await click('保存文章信息'); await wrapper.get('[aria-label="文章标题"]').setValue('B')
    request.resolve(article()); await flushPromises(); await dirtyLeave()
  })
  it('allows clean navigation and navigation after saving', async () => {
    await start(); await wrapper.get('[aria-label="正文"]').setValue('B'); await click('保存正文'); await dirtyLeave(false)
  })
  it('keeps the created draft and unsaved body on partial failure', async () => {
    await start('new'); await wrapper.get('[aria-label="文章标题"]').setValue('Draft')
    await wrapper.get('[aria-label="正文"]').setValue('B')
    api.updatePostBody.mockRejectedValue(new Error('offline')); await click('保存文章')
    expect(router.currentRoute.value.path).toBe('/useradmin/blog/editor/2')
    expect(wrapper.get('[aria-label="正文"]').element.value).toBe('B')
    expect(wrapper.get('[role="status"]').text()).toContain('草稿')
    expect(api.ask).not.toHaveBeenCalled()
    await router.push('/elsewhere'); expect(api.ask).toHaveBeenCalledTimes(1)
  })
  it('blocks another article while a save is in flight and prevents duplicate saves', async () => {
    await start(); const request = deferred(); api.updatePostBody.mockReturnValue(request.promise)
    await click('保存正文'); await router.push('/useradmin/blog/editor/2')
    expect(router.currentRoute.value.params.postId).toBe('1')
    expect(api.updatePostBody).toHaveBeenCalledTimes(1)
    request.resolve(article()); await flushPromises()
  })
  it('guards refresh with the current Markdown', async () => {
    await start(); wrapper.findComponent(Editor).vm.typeBeforeCallback('B')
    const event = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(true)
  })
  it('guards an article parameter update with dirty input', async () => {
    await start(); await wrapper.get('[aria-label="正文"]').setValue('B')
    await router.push('/useradmin/blog/editor/2')
    expect(api.ask).toHaveBeenCalledTimes(1); expect(router.currentRoute.value.params.postId).toBe('1')
  })
  it('captures body and metadata once for save all', async () => {
    await start(); const request = deferred(); api.updatePost.mockReturnValue(request.promise)
    await wrapper.get('[aria-label="正文"]').setValue('Submitted'); await click('保存文章')
    await wrapper.get('[aria-label="正文"]').setValue('New input')
    request.resolve(article()); await flushPromises()
    expect(api.updatePostBody).toHaveBeenCalledWith(1, 'Submitted'); await dirtyLeave()
  })
  it('requires read-back after a timeout and preserves newer input', async () => {
    await start(); api.updatePostBody.mockRejectedValue({ code: 'ECONNABORTED' })
    await click('保存正文'); expect(wrapper.get('[role="alert"]').text()).toContain('尚未确认')
    await click('保存正文'); expect(api.updatePostBody).toHaveBeenCalledTimes(1)
    await wrapper.get('[aria-label="正文"]').setValue('B'); await click('核实服务端保存结果')
    expect(wrapper.get('[aria-label="正文"]').element.value).toBe('B'); await dirtyLeave()
  })
  it('does not mutate a destroyed editor when the request resolves', async () => {
    await start(); const request = deferred(); api.updatePostBody.mockReturnValue(request.promise)
    await click('保存正文'); wrapper.unmount(); request.resolve(article('A')); await flushPromises()
    expect(api.updatePostBody).toHaveBeenCalledTimes(1)
  })
  it('does not carry the creation exception to later navigation', async () => {
    await start('new'); await wrapper.get('[aria-label="文章标题"]').setValue('Draft')
    await wrapper.get('[aria-label="正文"]').setValue('Body'); await click('创建草稿')
    expect(router.currentRoute.value.params.postId).toBe('2'); expect(api.ask).not.toHaveBeenCalled()
    await router.push('/elsewhere'); expect(api.ask).toHaveBeenCalledTimes(1)
  })
  it('guards summary and tag-only changes', async () => {
    await start(); await wrapper.get('[maxlength="1000"]').setValue('New summary'); await dirtyLeave()
  })
})
