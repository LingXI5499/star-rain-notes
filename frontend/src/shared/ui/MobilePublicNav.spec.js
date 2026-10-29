/**
 * MobilePublicNav.spec.js：跨模块基础模块的自动化测试。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue-router、vitest、./MobilePublicNav.vue、../../modules/tutorial/api/tutorialApi。
 * 阅读入口：open（打开当前操作）；button（button的处理函数）。
 * 验证场景：mobile public navigation；returns to the top when tapping the active home entry；uses canonical navigation from every public page；opens the active section menu and closes it after navigation；offers secondary pages and the account center from the homepage。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import MobileNav from './MobilePublicNav.vue'
import { listPublicCategories } from '../../modules/tutorial/api/tutorialApi'
vi.mock('../../modules/tutorial/api/tutorialApi', () => ({ listPublicCategories: vi.fn().mockResolvedValue([{id:'2',name:'Java',slug:'java'}]) }))
vi.mock('../../modules/blog/api/blogApi', () => ({ listPublicTags: vi.fn().mockResolvedValue([{id:'3',name:'开发',slug:'development'}]) }))
let wrapper, router
/**
 * 打开当前操作。
 * 主要协作：createRouter、createMemoryHistory、router.push、router.isReady、mount。
 * @param path 路径；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function open(path) {
  router = createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{template:'<div />'}}]})
  await router.push(path); await router.isReady()
  wrapper = mount(MobileNav,{attachTo:document.body,global:{plugins:[router]}}); await flushPromises()
}
/**
 * button的处理函数。
 * 主要协作：.find、wrapper.findAll、.endsWith、item.text。
 * @param label 标签；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const button = label => wrapper.findAll('button').find(item => item.text().endsWith(label))
afterEach(() => { wrapper?.unmount(); document.body.innerHTML=''; document.body.style.overflow=''; vi.clearAllMocks() })
describe('mobile public navigation', () => {
  it('returns to the top when tapping the active home entry', async () => {
    const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    await open('/'); await button('首页').trigger('click')
    expect(scroll).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }))
    scroll.mockRestore()
  })
  it('uses canonical navigation from every public page', async () => {
    await open('/english')
    await button('博客').trigger('click'); await flushPromises()
    expect(router.currentRoute.value.path).toBe('/blog')
  })
  it('opens the active section menu and closes it after navigation', async () => {
    await open('/english/vocabulary')
    await button('英语').trigger('click'); await flushPromises()
    const sheet=document.querySelector('[role="dialog"]')
    expect(sheet.getAttribute('aria-label')).toBe('英语导航')
    const reading=sheet.querySelector('a[href="/english/world"]')
    reading.click(); await flushPromises()
    expect(router.currentRoute.value.path).toBe('/english/world')
    expect(document.querySelector('[role="dialog"]')).toBeNull(); expect(document.body.style.overflow).toBe('')
  })
  it('offers secondary pages and the account center from the homepage', async () => {
    await open('/'); await button('更多').trigger('click'); await flushPromises()
    const sheet=document.querySelector('[role="dialog"]')
    for(const path of ['/portfolio','/messages','/about','/search']) expect(sheet.querySelector(`a[href="${path}"]`)).not.toBeNull()
    expect(sheet.querySelector('a[href="/center"]')).not.toBeNull()
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
