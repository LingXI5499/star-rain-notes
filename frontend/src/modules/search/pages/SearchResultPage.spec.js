/**
 * SearchResultPage.spec.js：搜索模块的自动化测试。
 * 职责范围：搜索文档投影、索引重建和关键词查询。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vue-router、vitest、./SearchResultPage.vue。
 * 阅读入口：setup（setup的处理函数）。
 * 验证场景：English site search；passes the English filter to the API and renders readable types and correct links；switches from all results to English and resets the page；keeps vocabulary query parameters when linking within the account path tree。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import SearchResultPage from './SearchResultPage.vue'

const api = vi.hoisted(() => ({ search: vi.fn() }))
vi.mock('../api/searchApi', () => api)
let wrapper
beforeEach(() => {
  vi.clearAllMocks()
  api.search.mockResolvedValue({ items: [
    { contentType: 'ENGLISH_VOCABULARY_WORD', contentId: '12', title: 'record', summary: '记录', routePath: '/english/vocabulary/4?q=record' },
    { contentType: 'ENGLISH_WRITING', contentId: '12', title: 'My article', routePath: '/english/writing/author/writing-12' },
  ], total: 2, page: 1, pageSize: 20 })
})
afterEach(() => wrapper?.unmount())
/**
 * setup的处理函数。
 * 主要协作：createRouter、createMemoryHistory、router.push、mount、flushPromises。
 * @param path = '/search?q=record&type=ENGLISH' 路径搜索q记录类型；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function setup(path = '/search?q=record&type=ENGLISH') {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/search', component: SearchResultPage },
    { path: '/search', component: SearchResultPage },
    { path: '/:rest(.*)', component: { template: '<div />' } },
  ] })
  await router.push(path)
  wrapper = mount(SearchResultPage, { global: { plugins: [router] } })
  await flushPromises()
  return router
}
describe('English site search', () => {
  it('passes the English filter to the API and renders readable types and correct links', async () => {
    await setup()
    expect(api.search).toHaveBeenCalledWith('record', 'ENGLISH', 1, 20, expect.any(AbortSignal))
    expect(wrapper.text()).toContain('英语单词')
    expect(wrapper.text()).toContain('英语原创写作')
    expect(wrapper.find('a[href="/english/vocabulary/4?q=record"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/english/writing/author/writing-12"]').exists()).toBe(true)
  })
  it('switches from all results to English and resets the page', async () => {
    const router = await setup('/search?q=record&page=3')
    const english = wrapper.findAll('button').find(button => button.text() === '英语')
    await english.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ q: 'record', type: 'ENGLISH' })
    expect(api.search).toHaveBeenLastCalledWith('record', 'ENGLISH', 1, 20, expect.any(AbortSignal))
  })
  it('keeps vocabulary query parameters when linking within the account path tree', async () => {
    await setup('/search?q=record&type=ENGLISH')
    expect(wrapper.find('a[href="/english/vocabulary/4?q=record"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/english/writing/author/writing-12"]').exists()).toBe(true)
  })
})
