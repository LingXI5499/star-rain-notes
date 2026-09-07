import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import ChapterView from './ChapterView.vue'

/**
 * 导航稳定性 (upgrade plan §3): switching to a sibling chapter must keep the
 * sidebar (curriculum) mounted — same DOM node, no curriculum refetch — so
 * its scroll position survives navigation.
 */

const mocks = vi.hoisted(() => ({
  route: { params: { tutorialSlug: 'java', chapterSlug: 'ch-1' } } as {
    params: { tutorialSlug: string; chapterSlug: string }
  },
  fetchPublicChapter: vi.fn(),
  fetchPublicTutorialDetail: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/api/tutorial', () => ({
  fetchPublicChapter: (...args: unknown[]) => mocks.fetchPublicChapter(...args),
  fetchPublicTutorialDetail: (...args: unknown[]) => mocks.fetchPublicTutorialDetail(...args),
}))

function chapter(slug: string) {
  return {
    chapterId: 1,
    chapterSlug: slug,
    chapterTitle: `章节 ${slug}`,
    tutorialId: 1,
    tutorialSlug: 'java',
    tutorialTitle: 'Java 基础',
    tutorialSummary: '',
    bodyMarkdown: `# ${slug}`,
    summary: null,
    breadcrumbs: [],
    previous: null,
    next: null,
    publishedAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
  }
}

const detail = {
  id: 1,
  title: 'Java 基础',
  slug: 'java',
  summary: '',
  coverUrl: null,
  categoryPath: [],
  publishedChapterCount: 2,
  firstChapter: null,
  curriculum: [
    {
      id: 10,
      type: 'GROUP',
      title: '基本语法',
      slug: null,
      children: [
        { id: 11, type: 'CHAPTER', title: '章节一', slug: 'ch-1', children: [] },
        { id: 12, type: 'CHAPTER', title: '章节二', slug: 'ch-2', children: [] },
      ],
    },
  ],
  seoTitle: null,
  seoDescription: null,
  publishedAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
}

describe('ChapterView 导航稳定性', () => {
  beforeEach(() => {
    mocks.route = reactive({ params: { tutorialSlug: 'java', chapterSlug: 'ch-1' } })
    mocks.fetchPublicChapter
      .mockReset()
      .mockImplementation(async (_tutorial: string, chapterSlug: string) => chapter(chapterSlug))
    mocks.fetchPublicTutorialDetail.mockReset().mockResolvedValue(detail)
  })

  it('keeps the sidebar mounted (same DOM node) and never refetches the curriculum when switching chapters', async () => {
    const wrapper = shallowMount(ChapterView)
    await flushPromises()

    const sidebarBefore = wrapper.get('.reader__sidebar').element
    expect(wrapper.get('.reader__title').text()).toBe('章节 ch-1')
    const detailCallsAfterMount = mocks.fetchPublicTutorialDetail.mock.calls.length

    mocks.route.params.chapterSlug = 'ch-2'
    await flushPromises()

    // Article swapped in place …
    expect(wrapper.get('.reader__title').text()).toBe('章节 ch-2')
    // … while the sidebar is the exact same DOM node (scroll position survives)
    expect(wrapper.get('.reader__sidebar').element).toBe(sidebarBefore)
    // … and the curriculum tree was not requested again.
    expect(mocks.fetchPublicTutorialDetail.mock.calls.length).toBe(detailCallsAfterMount)
    wrapper.unmount()
  })

  it('dims the article while a sibling chapter loads instead of unmounting the layout', async () => {
    const wrapper = shallowMount(ChapterView)
    await flushPromises()

    let release: (() => void) | undefined
    mocks.fetchPublicChapter.mockImplementationOnce(
      (_tutorial: string, chapterSlug: string) =>
        new Promise((resolve) => {
          release = () => resolve(chapter(chapterSlug))
        }),
    )

    mocks.route.params.chapterSlug = 'ch-2'
    await flushPromises()

    // Layout stays; the pending swap is only signalled on the article.
    expect(wrapper.find('.reader__sidebar').exists()).toBe(true)
    expect(wrapper.get('.reader__article').classes()).toContain('is-loading')
    expect(wrapper.get('.reader__title').text()).toBe('章节 ch-1')

    release?.()
    await flushPromises()
    expect(wrapper.get('.reader__article').classes()).not.toContain('is-loading')
    expect(wrapper.get('.reader__title').text()).toBe('章节 ch-2')
    wrapper.unmount()
  })
})
