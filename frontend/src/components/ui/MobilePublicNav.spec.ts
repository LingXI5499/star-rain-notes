import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import MobilePublicNav from './MobilePublicNav.vue'

async function mountNav() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/tutorials', component: { template: '<div />' } },
      { path: '/blog', component: { template: '<div />' } },
      { path: '/english', component: { template: '<div />' } },
      { path: '/portfolio', component: { template: '<div />' } },
      { path: '/about', component: { template: '<div />' } },
      { path: '/search', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  await router.isReady()
  return mount(MobilePublicNav, { attachTo: document.body, global: { plugins: [router] } })
}

afterEach(() => { document.body.innerHTML = '' })

describe('MobilePublicNav', () => {
  it('opens and closes the accessible more sheet', async () => {
    const wrapper = await mountNav()
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
