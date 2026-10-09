import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PublicPagination from './PublicPagination.vue'
import PublicSelect from './PublicSelect.vue'
import TutorialCatalogPage from '../../modules/tutorial/pages/public/TutorialCatalogPage.vue'
import EnglishDocumentsPage from '../../modules/english/pages/public/EnglishDocumentsPage.vue'
import SearchResultPage from '../../modules/search/pages/SearchResultPage.vue'
import BlogListPage from '../../modules/blog/pages/BlogListPage.vue'
import BlogArchivePage from '../../modules/blog/pages/BlogArchivePage.vue'
import BlogTopicPage from '../../modules/blog/pages/BlogTopicPage.vue'
const api = vi.hoisted(() => ({ listPublicTutorials: vi.fn(), listPublicCategories: vi.fn(), listReading: vi.fn(), search: vi.fn(), listArchive: vi.fn(), listPublicPosts: vi.fn(), listArchiveMonths: vi.fn(), listPublicTags: vi.fn(), listPublicTopics: vi.fn(), getPublicBlogStats: vi.fn(), listArchiveDays: vi.fn(), getPublicTopic: vi.fn() }))
vi.mock('../../modules/tutorial/api/tutorialApi', () => api)
vi.mock('../../modules/english/api/englishApi', () => api)
vi.mock('../../modules/search/api/searchApi', () => api)
vi.mock('../../modules/blog/api/blogApi', () => api)
vi.mock('../../shared/viewMode', () => ({ VIEW_MODE: { ACCOUNT: 'ACCOUNT' }, resolveViewMode: path => path.startsWith('/useradmin') ? 'ACCOUNT' : 'PUBLIC', accountPath: path => '/useradmin' + path, useViewMode: () => ({ contentPath: path => path }) }))
const mounted = []
async function render(component, url, props = {}) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] }); await router.push(url)
  const w = mount(component, { props, global: { plugins: [router], stubs: { BlogSidebar: true, BlogTimeline: true, BlogColumnNav: true, BlogArchiveCalendar: true, EnglishTopicDirectory:true, EnglishArticleFilters:true } } }); mounted.push(w); await flushPromises(); return { w, router }
}
beforeEach(() => {
  vi.clearAllMocks()
  api.listPublicCategories.mockResolvedValue([{ id: 'c', slug: 'java', name: 'Java' }])
  api.listPublicTutorials.mockResolvedValue({ total: 25, items: Array.from({ length: 25 }, (_, n) => ({ id: String(n), title: 'Java 教程 ' + n, slug: 'java-' + n, categoryId: 'c', chapterCount: 3 })) })
  api.listReading.mockImplementation(async query => ({ items: [], total: 65, page: query.page, pageSize: query.size }))
  api.search.mockResolvedValue({ items: [], total: 65, pageSize: 20 })
  api.listPublicPosts.mockResolvedValue({ items: [], total: 65 }); api.listArchive.mockResolvedValue({ items: [], total: 65 }); api.getPublicTopic.mockResolvedValue({ topic: { name: '专题' }, posts: { items: [], total: 65 } })
  for (const name of ['listArchiveMonths', 'listPublicTags', 'listPublicTopics', 'listArchiveDays']) api[name].mockResolvedValue([])
  api.getPublicBlogStats.mockResolvedValue({}); vi.stubGlobal('matchMedia', () => ({ matches: true })); vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
})
afterEach(() => { mounted.splice(0).forEach(w => w.unmount()); vi.unstubAllGlobals(); vi.restoreAllMocks() })
describe('public list integration', () => {
  it('keeps blog columns and sidebar without reintroducing the removed top filter bar', async () => {
    const { w } = await render(BlogListPage, '/blog')
    expect(w.find('.public-filters').exists()).toBe(false)
    expect(w.findComponent({ name: 'BlogColumnNav' }).exists()).toBe(true)
    expect(w.findComponent({ name: 'BlogSidebar' }).exists()).toBe(true)
    expect(w.findComponent(PublicPagination).exists()).toBe(true)
  })
  it('paginates filtered tutorial metadata and preserves the category when sizing and going back', async () => {
    const { w, router } = await render(TutorialCatalogPage, '/tutorials?categorySlug=java&page=2')
    expect(w.findAll('.tutorial-card')).toHaveLength(12)
    w.getComponent(PublicPagination).vm.$emit('page-size', 24); await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ categorySlug: 'java', pageSize: '24' }); expect(w.findAll('.tutorial-card')).toHaveLength(24)
    router.back(); await flushPromises(); expect(w.findAll('.tutorial-card')).toHaveLength(12); expect(w.getComponent(PublicPagination).props('page')).toBe(2)
  })
  it('uses selectable reading sizes in the real query and resets the page without losing search', async () => {
    const { w, router } = await render(EnglishDocumentsPage, '/english/reading?search=story&page=2')
    expect(api.listReading).toHaveBeenLastCalledWith({ page: 2, size: 12, search: 'story' })
    w.getComponent(PublicPagination).vm.$emit('page-size', 24); await flushPromises()
    expect(api.listReading).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 24, search: 'story' })); expect(router.currentRoute.value.query.search).toBe('story')
  })
  it('shows one reading pager and no retired writing sections or grade badges', async () => {
    api.listReading.mockResolvedValue({items:[{id:'1',slug:'reading-1',title:'Real prose',summary:'Summary',cefrLevel:'C2',difficultyLevel:3}],total:1})
    const {w}=await render(EnglishDocumentsPage,'/english/reading')
    expect(w.findAllComponents(PublicPagination)).toHaveLength(1);expect(w.text()).not.toContain('写作素材');expect(w.text()).not.toContain('写作任务');expect(w.text()).not.toContain('C2');expect(w.text()).not.toContain('难度')
  })
  it('sends the selected search page size to the API and retains content type and keyword', async () => {
    const { w, router } = await render(SearchResultPage, '/search?q=Java&type=TUTORIAL&page=2')
    w.getComponent(PublicPagination).vm.$emit('page-size', 50); await flushPromises()
    expect(api.search).toHaveBeenLastCalledWith('Java', 'TUTORIAL', 1, 50, expect.any(AbortSignal)); expect(router.currentRoute.value.query).toEqual({ q: 'Java', type: 'TUTORIAL', pageSize: '50' })
  })
  it.each([[BlogListPage, '/blog?tag=java&topic=tech&page=2', 'listPublicPosts'], [BlogArchivePage, '/blog/archive?tag=java&year=2026&month=10&page=2', 'listArchive'], [BlogTopicPage, '/blog/topics/tech?page=2', 'getPublicTopic']])('changes blog sizes with the same retained filters', async (component, url, method) => {
    const { w, router } = await render(component, url)
    const before = { ...router.currentRoute.value.query }
    w.getComponent(PublicPagination).vm.$emit('page-size', 20); await flushPromises()
    const args = api[method].mock.lastCall, query = method === 'getPublicTopic' ? args[1] : args[0]
    expect(query).toMatchObject({ page: 1, pageSize: 20 }); expect(router.currentRoute.value.query).toEqual({ ...before, page: undefined, pageSize: '20' })
    if (method !== 'getPublicTopic') expect(query.tag).toBe('java')
  })
})
