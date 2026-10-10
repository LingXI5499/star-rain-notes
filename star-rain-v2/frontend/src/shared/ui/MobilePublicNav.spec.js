import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import MobileNav from './MobilePublicNav.vue'
import { listPublicCategories } from '../../modules/tutorial/api/tutorialApi'
vi.mock('../../modules/tutorial/api/tutorialApi', () => ({ listPublicCategories: vi.fn().mockResolvedValue([{id:'2',name:'Java',slug:'java'}]) }))
vi.mock('../../modules/blog/api/blogApi', () => ({ listPublicTags: vi.fn().mockResolvedValue([{id:'3',name:'开发',slug:'development'}]) }))
let wrapper, router
async function open(path) {
  router = createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{template:'<div />'}}]})
  await router.push(path); await router.isReady()
  wrapper = mount(MobileNav,{attachTo:document.body,global:{plugins:[router]}}); await flushPromises()
}
const button = label => wrapper.findAll('button').find(item => item.text().endsWith(label))
afterEach(() => { wrapper?.unmount(); document.body.innerHTML=''; document.body.style.overflow=''; vi.clearAllMocks() })
describe('mobile public navigation', () => {
  it('returns to the top when tapping the active home entry', async () => {
    const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    await open('/useradmin'); await button('首页').trigger('click')
    expect(scroll).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }))
    scroll.mockRestore()
  })
  it('keeps account-tree navigation in the account tree', async () => {
    await open('/useradmin/english')
    await button('博客').trigger('click'); await flushPromises()
    expect(router.currentRoute.value.path).toBe('/useradmin/blog')
  })
  it('opens the active section menu and closes it after navigation', async () => {
    await open('/useradmin/english/vocabulary')
    await button('英语').trigger('click'); await flushPromises()
    const sheet=document.querySelector('[role="dialog"]')
    expect(sheet.getAttribute('aria-label')).toBe('英语导航')
    const reading=sheet.querySelector('a[href="/useradmin/english/reading"]')
    reading.click(); await flushPromises()
    expect(router.currentRoute.value.path).toBe('/useradmin/english/reading')
    expect(document.querySelector('[role="dialog"]')).toBeNull(); expect(document.body.style.overflow).toBe('')
  })
  it('offers all secondary pages without exposing an account entry in public mode', async () => {
    await open('/'); await button('更多').trigger('click'); await flushPromises()
    const sheet=document.querySelector('[role="dialog"]')
    for(const path of ['/portfolio','/messages','/about','/search']) expect(sheet.querySelector(`a[href="${path}"]`)).not.toBeNull()
    expect(sheet.textContent).not.toContain('用户中心')
  })
  it('restores focus and the previous body scrolling mode on Escape', async () => {
    document.body.style.overflow='scroll'
    await open('/'); const more=button('更多'); more.element.focus(); await more.trigger('click'); await flushPromises()
    expect(document.body.style.overflow).toBe('hidden')
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true})); await flushPromises()
    expect(document.body.style.overflow).toBe('scroll'); expect(document.activeElement).toBe(more.element)
  })
  it('provides category shortcuts and a fallback when loading fails', async () => {
    await open('/tutorials'); await button('教程').trigger('click'); await flushPromises()
    expect(document.querySelector('a[href="/tutorials?categorySlug=java"]')).not.toBeNull()
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true})); await flushPromises()
    listPublicCategories.mockRejectedValueOnce(new Error('offline'))
    await button('教程').trigger('click'); await flushPromises()
    expect(document.querySelector('[role="alert"]').textContent).toContain('导航加载失败')
    expect(document.querySelector('a[href="/tutorials"]')).not.toBeNull()
  })
})
