import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PortfolioManagePage from './PortfolioManagePage.vue'
vi.mock('../../api/portfolioApi', () => ({
 getWorkTaxonomy: vi.fn().mockResolvedValue({ categories: [] }),
 listAdminWorks: vi.fn().mockResolvedValue({ total: 2, items: [
  { id: '1', slug: 'blocks', title: '区块作品', status: 'DRAFT', workType: 'SOFTWARE', projectStatus: 'PLANNING', role: '全栈开发', typeDetail: null },
  { id: '2', slug: 'legacy', title: '旧作品', status: 'PUBLISHED', workType: 'SOFTWARE', typeDetail: { projectStage: 'ONLINE', role: '维护者', techStack: ['Java'] } },
 ] }),
 deleteWork: vi.fn(), publishWork: vi.fn(), restoreWork: vi.fn(), withdrawWork: vi.fn(),
}))
describe('portfolio management metadata compatibility', () => {
 it('renders new works without legacy type details and keeps the legacy fallback', async () => {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/useradmin/portfolio/manage', component: PortfolioManagePage }] })
  await router.push('/useradmin/portfolio/manage')
  const view = mount(PortfolioManagePage, { global: { plugins: [router], stubs: { AppConfirmDialog: true } } }); await flushPromises()
  expect(view.findAll('.portfolio-card')).toHaveLength(2)
  expect(view.text()).toContain('规划中'); expect(view.text()).toContain('全栈开发')
  expect(view.text()).toContain('已上线'); expect(view.text()).toContain('维护者')
 })
})
