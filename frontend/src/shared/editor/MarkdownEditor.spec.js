import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../../modules/account/stores/authStore'
const state = vi.hoisted(() => ({ options: null, insert: vi.fn(), upload: vi.fn() }))
vi.mock('../../modules/media/api/mediaApi', () => ({ uploadMedia: state.upload }))
vi.mock('../markdown/mathJax', () => ({ prepareMathJax: vi.fn().mockResolvedValue(undefined) }))
vi.mock('vditor', () => ({ default: class {
  constructor(host, options) { state.options = options; this.value = options.value; host.innerHTML = '<div contenteditable="true"><p>原文</p></div>' }
  getValue() { return this.value }
  setValue(value) { this.value = value }
  setTheme() {}
  insertMD(value) { state.insert(value) }
  destroy() {}
} }))
import MarkdownEditor from './MarkdownEditor.vue'
import LegacyMarkdownEditor from '../../modules/blog/components/admin/MarkdownEditor.vue'

describe('shared Markdown editor', () => {
  beforeEach(() => { vi.clearAllMocks(); state.options = null; setActivePinia(createPinia()); useAuthStore().currentUser = { permissions: ['media:upload'] } })
  it('keeps the legacy blog import pointing to the same editor', () => {
    expect(LegacyMarkdownEditor).toBe(MarkdownEditor)
  })
  async function editor() {
    const wrapper = mount(MarkdownEditor, { props: { modelValue: '原文' }, attachTo: document.body })
    await vi.waitFor(() => expect(state.options).toBeTruthy())
    state.options.after()
    return wrapper
  }
  function transfer(wrapper, type, files) {
    const event = new Event(type, { bubbles: true, cancelable: true })
    Object.defineProperty(event, type === 'paste' ? 'clipboardData' : 'dataTransfer', { value: { files } })
    wrapper.find('[contenteditable]').element.dispatchEvent(event)
    return event
  }
  it('uploads pasted and dropped images once each and inserts registered media URLs', async () => {
    state.upload.mockResolvedValue({ originalName: '截图.png', contentUrl: '/api/media/assets/19/content' })
    const wrapper = await editor()
    for (const type of ['paste', 'drop']) {
      const file = new File(['PNG'], '截图.png', { type: 'image/png' })
      expect(transfer(wrapper, type, [file]).defaultPrevented).toBe(true)
      await flushPromises()
      expect(state.upload).toHaveBeenLastCalledWith(file, 'PUBLIC', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    }
    expect(state.upload).toHaveBeenCalledTimes(2)
    expect(state.insert).toHaveBeenCalledTimes(2)
    expect(state.insert.mock.calls[0][0]).toContain('/api/media/assets/19/content')
    expect(wrapper.text()).toContain('加入媒体库')
    wrapper.unmount()
  })
  it('does not insert into another document after an upload finishes late', async () => {
    let resolve
    state.upload.mockReturnValue(new Promise(done => { resolve = done }))
    const wrapper = await editor()
    transfer(wrapper, 'paste', [new File(['PNG'], 'late.png')])
    await wrapper.setProps({ modelValue: '另一篇正文' })
    resolve({ originalName: 'late.png', contentUrl: '/api/media/assets/20/content' })
    await flushPromises()
    expect(state.insert).not.toHaveBeenCalled()
    wrapper.unmount()
  })
  it('keeps the document untouched on upload failure and on unmount', async () => {
    state.upload.mockRejectedValue({ response: { status: 500, data: { code: 'MEDIA_STORAGE_WRITE_FAILED' } } })
    const wrapper = await editor()
    transfer(wrapper, 'paste', [new File(['PNG'], 'fail.png')])
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('目录无法写入')
    expect(state.insert).not.toHaveBeenCalled()
    let resolve
    state.upload.mockReturnValue(new Promise(done => { resolve = done }))
    transfer(wrapper, 'drop', [new File(['PNG'], 'late.png')])
    wrapper.unmount()
    resolve({ originalName: 'late.png', contentUrl: '/api/media/assets/20/content' })
    await flushPromises()
    expect(state.insert).not.toHaveBeenCalled()
  })
  it('does not offer uploads or send files without media permission', async () => {
    useAuthStore().currentUser = { permissions: [] }
    const wrapper = await editor()
    expect(state.options.toolbar.some(item => item.name === 'image-upload')).toBe(false)
    transfer(wrapper, 'paste', [new File(['PNG'], 'private.png')])
    await flushPromises()
    expect(state.upload).not.toHaveBeenCalled()
    expect(state.insert).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('没有媒体上传权限')
    wrapper.unmount()
  })
})
