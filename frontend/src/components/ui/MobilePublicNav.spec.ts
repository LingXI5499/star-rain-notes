import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import MobilePublicNav from './MobilePublicNav.vue'

vi.mock('@/api/tutorial', () => ({
  fetchPublicCategoryTree: vi.fn(async () => [
    { id: 1, name: '基础分类', slug: 'basics', children: [] },
  ]),
}))

vi.mock('@/api/blog', () => ({
  fetchPublicTags: vi.fn(async () => [
    { id: 1, name: '随笔', slug: 'essay', postCount: 3 },
  ]),
}))

async function mountNav(path = '/') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/tutorials', component: { template: '<div />' } },
      { path: '/tutorials/:slug', component: { template: '<div />' } },
      { path: '/blog', component: { template: '<div />' } },
      { path: '/blog/:slug', component: { template: '<div />' } },
      { path: '/english', component: { template: '<div />' } },
      { path: '/english/:rest(.*)*', component: { template: '<div />' } },
      { path: '/portfolio', component: { template: '<div />' } },
      { path: '/about', component: { template: '<div />' } },
      { path: '/search', component: { template: '<div />' } },
    ],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(MobilePublicNav, {
    attachTo: document.body,
    global: { plugins: [router] },
  })
  return { wrapper, router }
}

function primaryButton(wrapper: Awaited<ReturnType<typeof mountNav>>['wrapper'], label: string) {
  return wrapper.findAll('.mobile-public-nav button, .mobile-public-nav a').find((el) => el.text().includes(label))!
}

afterEach(() => {
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

describe('MobilePublicNav section retap', () => {
  beforeEach(() => {
    vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined)
  })

  it('scrolls to top when home is retapped on /', async () => {
    const { wrapper } = await mountNav('/')
    Object.defineProperty(window, 'scrollY', { configurable: true, value: 400, writable: true })
    await primaryButton(wrapper, '首页').trigger('click')
    expect(window.scrollTo).toHaveBeenCalled()
    expect(document.body.querySelector('[role="dialog"]')).toBeNull()
    wrapper.unmount()
  })

  it('opens english module panel when english is retapped inside /english/**', async () => {
    const { wrapper, router } = await mountNav('/english/reading')
    await primaryButton(wrapper, '英语').trigger('click')
    await flushPromises()
    const dialog = document.body.querySelector('[role="dialog"][aria-label="英语导航"]')
    expect(dialog).toBeTruthy()
    expect(dialog?.textContent).toContain('词汇')
    expect(dialog?.innerHTML).toContain('/english/vocabulary')
    expect(dialog?.innerHTML).toContain('/english/progress')
    expect(dialog?.innerHTML).not.toContain('/english/bundles')
    expect(router.currentRoute.value.path).toBe('/english/reading')
    wrapper.unmount()
  })

  it('navigates to tutorials root when leaving english without opening a panel', async () => {
    const { wrapper, router } = await mountNav('/english/reading')
    await primaryButton(wrapper, '教程').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/tutorials')
    expect(document.body.querySelector('[aria-label="教程导航"]')).toBeNull()
    wrapper.unmount()
  })

  it('opens tutorials panel with categorySlug links when retapped inside tutorials', async () => {
    const { wrapper, router } = await mountNav('/tutorials/some-course')
    await primaryButton(wrapper, '教程').trigger('click')
    await flushPromises()
    const dialog = document.body.querySelector('[aria-label="教程导航"]')
    expect(dialog).toBeTruthy()
    expect(dialog?.textContent).toContain('基础分类')
    const cat = dialog?.querySelector('a[href*="categorySlug=basics"]')
    expect(cat).toBeTruthy()
    expect(router.currentRoute.value.path).toBe('/tutorials/some-course')
    wrapper.unmount()
  })

  it('opens blog panel with tag query links when retapped inside blog', async () => {
    const { wrapper } = await mountNav('/blog/hello')
    await primaryButton(wrapper, '博客').trigger('click')
    await flushPromises()
    const dialog = document.body.querySelector('[aria-label="博客导航"]')
    expect(dialog).toBeTruthy()
    expect(dialog?.querySelector('a[href*="tag=essay"]')).toBeTruthy()
    wrapper.unmount()
  })

  it('closes section panel on Escape and does not keep more sheet open together', async () => {
    const { wrapper } = await mountNav('/english')
    await primaryButton(wrapper, '英语').trigger('click')
    await flushPromises()
    expect(document.body.querySelector('[aria-label="英语导航"]')).toBeTruthy()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(document.body.querySelector('[aria-label="英语导航"]')).toBeNull()
    wrapper.unmount()
  })
})

describe('MobilePublicNav more sheet', () => {
  it('opens and closes the accessible more sheet', async () => {
    const { wrapper } = await mountNav()
    const trigger = wrapper.findAll('button').find((button) => button.text().includes('更多'))!
    await trigger.trigger('click')
    expect(document.body.textContent).toContain('继续探索')
    expect(document.body.textContent).toContain('作品')

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(document.body.textContent).not.toContain('继续探索')
    expect(document.activeElement).toBe(trigger.element)
    wrapper.unmount()
  })
})
