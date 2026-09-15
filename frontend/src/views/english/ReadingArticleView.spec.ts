import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReadingArticleView from './ReadingArticleView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { slug: '__no_such_nav_ac__' },
    name: 'english-reading-detail',
    query: {},
  },
  fetchPublicReading: vi.fn(),
  fetchPublicReadingExercises: vi.fn(),
  RouterLinkStub: {
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : \'\'" v-bind="$attrs"><slot /></a>',
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  RouterLink: mocks.RouterLinkStub,
}))

vi.mock('@/api/reading', () => ({
  fetchPublicReading: (...args: unknown[]) => mocks.fetchPublicReading(...args),
  fetchPublicReadingExercises: (...args: unknown[]) => mocks.fetchPublicReadingExercises(...args),
  fetchReading: vi.fn(),
  checkReadingAnswers: vi.fn(),
}))

vi.mock('@/api/englishLearning', () => ({
  fetchLearningRecord: vi.fn(async () => null),
  saveLearningRecord: vi.fn(),
}))

vi.mock('@/lib/seo', () => ({ applyPageMeta: vi.fn() }))

vi.mock('element-plus/es/components/index.mjs', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

describe('ReadingArticleView page-internal back link', () => {
  beforeEach(() => {
    mocks.route.params = { slug: '__no_such_nav_ac__' }
    mocks.route.name = 'english-reading-detail'
    mocks.fetchPublicReading.mockReset().mockRejectedValue(new Error('not found'))
    mocks.fetchPublicReadingExercises.mockReset().mockRejectedValue(new Error('not found'))
  })

  it('shows a main-content back link to /english/reading in the notFound state', async () => {
    const wrapper = mount(ReadingArticleView, {
      global: {
        components: { RouterLink: mocks.RouterLinkStub },
        stubs: {
          MarkdownRenderer: true,
          ArticleOutline: true,
          CefrBadge: true,
          ExerciseRunner: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('文章不存在或未发布')
    const back = wrapper.find('[data-testid="english-page-back"]')
    expect(back.exists()).toBe(true)
    expect(back.attributes('href')).toBe('/english/reading')
    expect(back.text()).toContain('阅读中心')
  })
})
