import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import WorkDetailPage from './WorkDetailPage.vue'

const api = vi.hoisted(() => ({ getPublicWork: vi.fn(), getAdminWork: vi.fn() }))
vi.mock('../api/portfolioApi', () => api)
const base = { id: '1', slug: 'sample', title: '作品', summary: '简介', hasSections: false,
 bodyMarkdown: '# 创作过程\n\n![截图](/media/screenshot.png)', techStack: 'Vue，Java',
 coverUrl: '/media/cover.png', links: [], media: [], sections: [] }
beforeEach(() => { vi.clearAllMocks(); api.getPublicWork.mockResolvedValue(base) })
async function render(work = base, path = '/portfolio/sample') {
 api.getPublicWork.mockResolvedValue(work); api.getAdminWork.mockResolvedValue(work)
 const router = createRouter({ history: createMemoryHistory(), routes: [
  { path: '/portfolio/:slug', component: WorkDetailPage }, { path: '/portfolio/preview/:workId', component: WorkDetailPage },
 ] })
 await router.push(path)
 const view = mount(WorkDetailPage, { global: { plugins: [router], stubs: { ReadingAside: true, ReadingDrawer: true } } })
 await flushPromises()
 return view
}
describe('work reading', () => {
 it('exposes every enabled safe link with its action label and secure external target', async () => {
  const types = ['GITHUB', 'LIVE', 'DEMO', 'DOWNLOAD', 'VIDEO', 'BILIBILI', 'DOUYIN', 'DOC', 'ARTICLE', 'OTHER']
  const links = types.map((linkType, id) => ({ id, linkType, label: `名称${id}`, url: `https://example.com/${id}`, enabled: true }))
  const view = await render({ ...base, links: [...links,
   { id: 11, linkType: 'LIVE', enabled: false, url: 'https://example.com/off' },
   { id: 12, linkType: 'OTHER', enabled: true, url: 'javascript:alert(1)' },
   { id: 13, linkType: 'OTHER', enabled: true, url: 'https://user:password@example.com' },
   { id: 14, linkType: 'OTHER', enabled: true, url: '' },
  ] })
  const anchors = view.findAll('.work-detail__links a')
  expect(anchors).toHaveLength(10)
  expect(anchors.map(link => link.text().split(' · ')[0])).toEqual(['查看源码', '在线体验', '在线体验', '下载作品', '观看演示', '观看演示', '观看演示', '查看文档', '查看文档', '名称9 ↗'])
  for (const anchor of anchors) {
   expect(anchor.attributes('target')).toBe('_blank')
   expect(anchor.attributes('rel')).toBe('noopener noreferrer')
  }
  expect(view.findAll('.work-detail__tags span').map(tag => tag.text())).toEqual(['Vue', 'Java'])
 })
 it('does not automatically repeat covers or screenshots already used in Markdown, including reference images', async () => {
  const view = await render({ ...base, coverUrl: '/media/screenshot.png',
   bodyMarkdown: '![截图][screen]\n\n[screen]: /media/screenshot.png',
   media: [{ id: '2', usageType: 'SCREENSHOT', url: '/media/screenshot.png' },
    { id: '3', usageType: 'SCREENSHOT', url: '/media/other.png' }, { id: '4', usageType: 'SCREENSHOT', url: '/media/other.png' }] })
  expect(view.find('.work-detail__cover').exists()).toBe(false)
  expect(view.findAll('.work-detail__gallery img')).toHaveLength(1)
  expect(view.findAll('img[src="/media/screenshot.png"]')).toHaveLength(1)
 })
 it('shows a distinct cover before prose and retains audio and neighboring routes', async () => {
  const view = await render({ ...base, media: [{ id: '2', usageType: 'AUDIO', url: '/media/song.mp3' }],
   previousWork: { slug: 'before', title: '上一篇' }, nextWork: { slug: 'after', title: '下一篇' } })
  expect(view.find('.work-detail__cover img').attributes('src')).toBe('/media/cover.png')
  expect(view.find('audio').attributes('src')).toBe('/media/song.mp3')
  expect(view.findAll('.work-neighbors a').map(link => link.attributes('href'))).toEqual(['/portfolio/before', '/portfolio/after'])
 })
 it('keeps all-hidden legacy blocks authoritative instead of revealing the old body', async () => {
  const view = await render({ ...base, hasSections: true, bodyMarkdown: 'OLD SECRET BODY',
   sections: [{ id: '2', sectionType: 'MARKDOWN', content: 'HIDDEN BLOCK', visible: false }] }, '/portfolio/preview/1')
  expect(api.getAdminWork).toHaveBeenCalledWith('1')
  expect(view.find('.work-sections').exists()).toBe(true)
  expect(view.find('.markdown-body').exists()).toBe(false)
  expect(view.text()).not.toContain('OLD SECRET BODY')
  expect(view.text()).not.toContain('HIDDEN BLOCK')
 })
 it('renders existing blocks and suppresses a matching automatic cover without changing authored media', async () => {
  const view = await render({ ...base, hasSections: true,
   sections: [{ id: '2', sectionType: 'IMAGE', visible: true, media: [{ url: '/media/cover.png', caption: '原图' }] }] })
  expect(view.find('.work-detail__cover').exists()).toBe(false)
  expect(view.find('.work-sections img').attributes('src')).toBe('/media/cover.png')
 })
 it('keeps raw HTML out of the public DOM', async () => {
  const view = await render({ ...base, bodyMarkdown: '<img src=x onerror="alert(1)"><script>alert(1)</script>' })
  expect(view.find('.markdown-body script').exists()).toBe(false)
  expect(view.find('.markdown-body [onerror]').exists()).toBe(false)
 })
})
