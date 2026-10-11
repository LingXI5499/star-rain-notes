/**
 * HomePage.spec.js：站点模块的自动化测试。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vitest、./HomePage.vue、../api/siteApi。
 * 阅读入口：open（打开当前操作）；home（首页的处理函数）。
 * 验证场景：home progressive loading；shows configured brand, actions and navigation during a slow request；keeps navigation on failure and replaces content exactly once after retry；honors a disabled hero after receiving section settings；ignores a response after unmounting。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import HomePage from './HomePage.vue'
import { getPublicSiteHome } from '../api/siteApi'

vi.mock('../api/siteApi', () => ({ getPublicSiteHome: vi.fn() }))
vi.mock('../support/siteBranding', () => ({ useSiteBranding: () => ({ siteName: 'Configured brand', tagline: 'Configured tagline', homeIntro: 'Configured introduction' }) }))
vi.mock('../../../shared/viewMode', () => ({ useViewMode: () => ({ contentPath: path => path }) }))
const wrappers = []
/**
 * 打开当前操作。
 * 主要协作：mount、wrappers.push。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const open = () => {
  const wrapper = mount(HomePage, { global: { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' }, ThemeHero: true } } })
  wrappers.push(wrapper)
  return wrapper
}
/**
 * 首页的处理函数。
 * @param siteName = 'Updated brand' 站点名称更新brand；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const home = (siteName = 'Updated brand') => ({ config: { siteName }, sections: [{ code: 'HERO' }, { code: 'LATEST', title: 'Latest', data: [{ id: '1', type: 'BLOG', title: 'Only once', routePath: '/blog/posts/one' }] }] })
beforeEach(() => vi.clearAllMocks())
afterEach(() => wrappers.splice(0).forEach(wrapper => wrapper.unmount()))

describe('home progressive loading', () => {
  it('shows configured brand, actions and navigation during a slow request', async () => {
    let resolve
    getPublicSiteHome.mockReturnValue(new Promise(done => { resolve = done }))
    const wrapper = open()
    expect(wrapper.find('h1').text()).toBe('Configured brand')
    expect(wrapper.findAll('.home-rail a')).toHaveLength(4)
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
    resolve(home()); await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Updated brand')
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(false)
  })
  it('keeps navigation on failure and replaces content exactly once after retry', async () => {
    getPublicSiteHome.mockRejectedValueOnce({ response: { status: 500 } }).mockResolvedValueOnce(home())
    const wrapper = open(); await flushPromises()
    expect(wrapper.findAll('.home-rail a')).toHaveLength(4)
    await wrapper.find('button').trigger('click'); await flushPromises()
    expect(wrapper.findAll('.home-updates h3')).toHaveLength(1)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
  it('honors a disabled hero after receiving section settings', async () => {
    getPublicSiteHome.mockResolvedValue({ config: { siteName: 'Brand' }, sections: [] })
    const wrapper = open(); await flushPromises()
    expect(wrapper.find('.home-hero').exists()).toBe(false)
    expect(wrapper.find('.home-rail').exists()).toBe(true)
  })
  it('ignores a response after unmounting', async () => {
    let resolve
    getPublicSiteHome.mockReturnValue(new Promise(done => { resolve = done }))
    const wrapper = open(); wrapper.unmount()
    resolve(home()); await flushPromises()
    expect(wrapper.exists()).toBe(false)
  })
})
