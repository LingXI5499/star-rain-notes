/**
 * WorkLibrary.spec.js：作品模块的自动化测试。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、@vue/test-utils、vue-router、./WorkLibrary.vue。
 * 阅读入口：render（渲染当前操作）。
 * 验证场景：featured works and the lower archive；shows independently loaded selected works above filters, cards and pagination；changes only the archive query and resets its page when filtering in the account tree；keeps ordinary works in the archive when no selected works have been configured。
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import WorkLibrary from './WorkLibrary.vue'

const api = vi.hoisted(() => ({ listPublicWorks: vi.fn(), getWorkTaxonomy: vi.fn() }))
vi.mock('../api/portfolioApi', () => api)
const chosen = { id: '1', slug: 'chosen', title: '精选作品 A', summary: '精选简介', featured: true }
const ordinary = { id: '2', slug: 'ordinary', title: '普通作品 B', summary: '普通简介' }
beforeEach(() => {
 vi.clearAllMocks()
 api.getWorkTaxonomy.mockResolvedValue({ categories: [{ id: '3', name: '工具' }], formats: [], tags: [] })
 api.listPublicWorks.mockImplementation(params => Promise.resolve(params.pageSize === 3 ? { items: [chosen], total: 1 } : { items: [chosen, ordinary], total: 25 }))
})
/**
 * 渲染当前操作。
 * 主要协作：createRouter、createMemoryHistory、router.push、mount、flushPromises。
 * @param url = '/portfolio' 地址作品；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function render(url = '/portfolio') {
 const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/portfolio', component: WorkLibrary }, { path: '/portfolio', component: WorkLibrary }] })
 await router.push(url)
 const view = mount(WorkLibrary, { global: { plugins: [router] } }); await flushPromises()
 return { view, router }
}
describe('featured works and the lower archive', () => {
 it('alternates up to three featured rows and keeps the archive count including featured works', async () => {
  api.listPublicWorks.mockImplementation(params => Promise.resolve(params.pageSize === 3
   ? { items: [chosen, { ...chosen, id: '3' }, { ...chosen, id: '4' }, { ...chosen, id: '5' }], total: 4 }
   : { items: [chosen, ordinary], total: 25 }))
  const { view } = await render()
  const cards = view.findAll('.works-featured .works-card')
  expect(cards).toHaveLength(3)
  expect(cards.map(card => card.classes().includes('works-card--reverse'))).toEqual([false, true, false])
  expect(view.findAll('.works-archive .works-card')).toHaveLength(2)
  expect(view.find('.public-filters__summary').text()).toContain('25 件作品')
  expect(view.find('.works-secondary-filters').attributes('open')).toBeUndefined()
 })
 it('shows a text cover placeholder and at most three technologies on ordinary cards', async () => {
  api.listPublicWorks.mockResolvedValue({ items: [{ ...ordinary, techStack: 'Vue, Java，MySQL、Python' }], total: 1 })
  const { view } = await render()
  const card = view.find('.works-archive .works-card')
  expect(card.find('.works-card__cover span').text()).toBe('普')
  expect(card.findAll('.works-card__tags span').map(tag => tag.text())).toEqual(['Vue', 'Java', 'MySQL'])
  expect(card.attributes('href')).toBe('/portfolio/ordinary')
 })
 it('shows independently loaded selected works above filters, cards and pagination', async () => {
  const { view } = await render('/portfolio?categoryId=3&formatId=2&page=2')
  expect(api.listPublicWorks).toHaveBeenCalledWith({ page: 1, pageSize: 3, featured: true })
  expect(view.find('.works-featured').text()).toContain('精选作品 A')
  expect(view.find('.works-featured').text()).not.toContain('普通作品 B')
  expect(view.find('.works-featured .works-filters').exists()).toBe(false)
  expect(view.find('.works-archive [aria-current=page]').text()).toBe('2')
  expect(view.find('.works-library').element.children[1].className).toBe('works-featured')
  expect(view.find('.works-library').element.children[2].className).toBe('works-archive')
 })
 it('changes only the archive query and resets its page when filtering in the account tree', async () => {
  const { view, router } = await render('/portfolio?page=2&formatId=2')
  await view.findAll('.public-filter-tabs button').find(button => button.text() === '工具').trigger('click'); await flushPromises()
  expect(router.currentRoute.value.path).toBe('/portfolio')
  expect(router.currentRoute.value.query).toEqual({ categoryId: '3', formatId: '2' })
  await view.find('[aria-label="下一页"]').trigger('click'); await flushPromises()
  expect(api.listPublicWorks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, pageSize: 12, categoryId: '3', formatId: '2' }))
  expect(api.listPublicWorks.mock.calls.filter(([params]) => params.pageSize === 3)).toHaveLength(1)
  expect(view.find('.works-featured a').attributes('href')).toBe('/portfolio/chosen')
 })
 it('keeps ordinary works in the archive when no selected works have been configured', async () => {
  api.listPublicWorks.mockImplementation(params => Promise.resolve(params.pageSize === 3 ? { items: [], total: 0 } : { items: [ordinary], total: 1 }))
  const { view } = await render()
  expect(view.find('.works-featured').text()).toContain('精选作品正在整理中')
  expect(view.find('.works-featured .works-card').exists()).toBe(false)
  expect(view.find('.works-archive').text()).toContain('普通作品 B')
 })
})
