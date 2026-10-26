import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { it, expect, vi, afterEach } from 'vitest'
import ChapterEditPage from './ChapterEditPage.vue'
const api = vi.hoisted(() => ({
  getAdminChapter: vi.fn(),
  getAdminCurriculum: vi.fn(),
  updateChapter: vi.fn(),
  createChapter: vi.fn(),
}))
vi.mock('../../api/tutorialApi', () => api)
vi.mock('../../support/operationResult', () => ({
  isReviewPending: (result) => result?.reviewPending === true,
  operationNotice: (_, message) => message,
}))
const Editor = defineComponent({
  props: ['modelValue', 'editorContext'],
  emits: ['update:modelValue'],
  setup(props, { emit, expose }) {
    expose({ getMarkdown: () => props.modelValue })
    return () =>
      h('textarea', {
        'aria-label': '正文',
        value: props.modelValue,
        onInput: (event) => emit('update:modelValue', event.target.value),
      })
  },
})
let wrapper
afterEach(() => {
  wrapper?.unmount()
  vi.clearAllMocks()
})
async function start() {
  api.getAdminCurriculum.mockResolvedValue({
    tutorial: { title: 'Java' },
    groups: [{ id: 1, status: 'ACTIVE' }],
  })
  api.getAdminChapter.mockResolvedValue({
    id: 2,
    title: 'Chapter',
    summary: '',
    groupId: 1,
    rowVersion: 1,
    bodyMarkdown: 'old',
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/tutorials/:tutorialId/chapters/:chapterId', component: ChapterEditPage },
    ],
  })
  await router.push('/tutorials/1/chapters/2')
  wrapper = mount(ChapterEditPage, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        MarkdownEditor: Editor,
        ChapterAuthorNav: true,
        AdminConfirmDialog: true,
        MediaPicker: true,
      },
    },
  })
  await flushPromises()
}
it('acknowledges the submitted chapter snapshot without overwriting newer input', async () => {
  let resolve
  api.updateChapter.mockReturnValue(
    new Promise((done) => {
      resolve = done
    }),
  )
  await start()
  await wrapper.get('[aria-label="正文"]').setValue('submitted')
  await wrapper
    .findAll('button')
    .find((button) => button.text() === '保存修改')
    .trigger('click')
  await wrapper.get('[aria-label="正文"]').setValue('newer input')
  resolve({ id: 2, rowVersion: 2 })
  await flushPromises()
  expect(wrapper.get('[aria-label="正文"]').element.value).toBe('newer input')
  expect(wrapper.findComponent(Editor).props('editorContext')).toMatchObject({
    baseline: 'submitted',
    serverVersion: 2,
  })
})
it('does not mark a review-pending proposal as an accepted server baseline', async () => {
  api.updateChapter.mockResolvedValue({ reviewPending: true })
  await start()
  await wrapper.get('[aria-label="正文"]').setValue('proposal')
  await wrapper
    .findAll('button')
    .find((button) => button.text() === '保存修改')
    .trigger('click')
  await flushPromises()
  expect(wrapper.findComponent(Editor).props('editorContext')).toMatchObject({
    baseline: 'old',
    serverVersion: 1,
  })
})
