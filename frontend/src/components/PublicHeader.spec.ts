import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PublicHeader from './PublicHeader.vue'
import { useAppStore } from '@/stores/app'

vi.mock('@/api/site', () => ({
  fetchPublicSite: vi.fn(async () => ({
    siteName: '星雨笔录',
    tagline: '建立自己的知识世界',
    siteUrl: 'https://yulanlin.cn',
    footerText: null,
    githubUrl: null,
    timezone: 'Asia/Shanghai',
    logoUrl: null,
    faviconUrl: null,
  })),
}))

vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
  callback(0)
  return 1
})

async function mountHeader() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div />' } }],
  })
  await router.push('/')
  await router.isReady()
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(PublicHeader, {
    global: {
      plugins: [router, pinia],
      stubs: { GlobalSearch: true, ThemeControl: true },
    },
  })
  return { wrapper, pinia }
}

describe('PublicHeader brand', () => {
  const store = () => useAppStore()

  beforeEach(() => {
    document.head.innerHTML = ''
  })

  it('shows the brand mark next to the site name by default', async () => {
    const { wrapper } = await mountHeader()
    await flushPromises()
    const brand = wrapper.get('.site-header__brand')
    expect(brand.text()).toContain('星雨笔录')
    expect(brand.find('.brand-mark').exists()).toBe(true)
    expect(brand.find('.site-header__logo').exists()).toBe(false)
    wrapper.unmount()
  })

  it('switches to the configured logo image when the public site provides one', async () => {
    const { wrapper, pinia } = await mountHeader()
    const appStore = useAppStore(pinia)
    appStore.logoUrl = 'https://yulanlin.cn/uploads/site-logo.png'
    await flushPromises()
    const brand = wrapper.get('.site-header__brand')
    expect(brand.find('.site-header__logo').attributes('src')).toBe('https://yulanlin.cn/uploads/site-logo.png')
    expect(brand.find('.brand-mark').exists()).toBe(false)
    wrapper.unmount()
  })

  it('falls back to the static brand favicon when the public config is missing', async () => {
    await mountHeader()
    await flushPromises()
    const icon = document.querySelector<HTMLLinkElement>('link[data-brand-icon]')
    expect(icon?.getAttribute('href')).toBe('/brand/favicon.png')
  })
})
