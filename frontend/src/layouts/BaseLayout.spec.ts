import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import BaseLayout from './BaseLayout.vue'

vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
  callback(0)
  return 1
})

describe('BaseLayout reading progress', () => {
  it('renders progress only for routes that explicitly opt in', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        { path: '/article', component: { template: '<div />' }, meta: { readingProgress: true } },
      ],
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(BaseLayout, {
      global: {
        plugins: [router],
        stubs: { PublicHeader: true, PublicFooter: true, MobilePublicNav: true },
      },
    })

    expect(wrapper.find('.reading-progress').exists()).toBe(false)
    await router.push('/article')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.reading-progress').exists()).toBe(true)
    wrapper.unmount()
  })
})
