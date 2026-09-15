import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WritingDetailView from './WritingDetailView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    path: '/english/writing/resources/missing-slug',
    params: { slug: 'missing-slug' },
    name: 'english-writing-resource',
  },
  fetchPublicWritingResource: vi.fn(),
  fetchPublicWritingPrompt: vi.fn(),
  RouterLinkStub: {
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : \'\'" v-bind="$attrs"><slot /></a>',
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  RouterLink: mocks.RouterLinkStub,
}))

vi.mock('@/api/writing', () => ({
  fetchPublicWritingResource: (...args: unknown[]) => mocks.fetchPublicWritingResource(...args),
  fetchPublicWritingPrompt: (...args: unknown[]) => mocks.fetchPublicWritingPrompt(...args),
  fetchPublicWritingExercises: vi.fn(async () => []),
  checkWritingExercises: vi.fn(),
}))

vi.mock('@/api/englishLearning', () => ({
  fetchWritingSubmission: vi.fn(async () => null),
  saveWritingSubmission: vi.fn(),
  saveLearningRecord: vi.fn(),
}))

vi.mock('@/lib/seo', () => ({ applyPageMeta: vi.fn() }))

vi.mock('element-plus/es/components/index.mjs', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

describe('WritingDetailView page-internal back link', () => {
  beforeEach(() => {
    mocks.route.path = '/english/writing/resources/missing-slug'
    mocks.route.params = { slug: 'missing-slug' }
    mocks.route.name = 'english-writing-resource'
    mocks.fetchPublicWritingResource.mockReset().mockRejectedValue(new Error('not found'))
    mocks.fetchPublicWritingPrompt.mockReset().mockRejectedValue(new Error('not found'))
  })

  it('shows a main-content back link to /english/writing in the missing state', async () => {
    const wrapper = mount(WritingDetailView, {
      global: {
        components: { RouterLink: mocks.RouterLinkStub },
        stubs: {
          MarkdownRenderer: true,
          ReadingAside: true,
          ExerciseRunner: true,
          'el-button': true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('内容不存在或尚未发布')
    const back = wrapper.find('[data-testid="english-page-back"]')
    expect(back.exists()).toBe(true)
    expect(back.attributes('href')).toBe('/english/writing')
    expect(back.text()).toContain('写作中心')
  })
})
