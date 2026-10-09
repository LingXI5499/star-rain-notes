import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkSectionRenderer from './WorkSectionRenderer.vue'
import { blockPayload, emptyBlock, safeWorkUrl } from '../support/workBlocks'
describe('work content blocks', () => {
 it('renders structured blocks and omits hidden content', () => {
  const view = mount(WorkSectionRenderer, { props: { sections: [
   { id: '1', sectionType: 'STATS', title: '成果', data: { items: [{ label: '测试用例', value: '18' }] } },
   { id: '2', sectionType: 'TIMELINE', title: '开发过程', data: { items: [{ date: '2026-10', title: '上线', description: '可交付版本' }] } },
   { id: '3', sectionType: 'QUOTE', content: 'hidden secret', visible: false, data: {} },
  ] } })
  expect(view.text()).toContain('测试用例'); expect(view.text()).toContain('上线'); expect(view.text()).not.toContain('hidden secret')
 })
 it('treats code and quotation as text and rejects unsafe links', () => {
  const view = mount(WorkSectionRenderer, { props: { sections: [
   { id: '1', sectionType: 'CODE', content: '<img src=x onerror=alert(1)>', data: { language: 'html' } },
   { id: '2', sectionType: 'LINKS', data: { items: [{ url: 'javascript:alert(1)', label: 'unsafe' }, { url: 'https://example.org', label: 'safe' }] } },
  ] } })
  expect(view.find('img').exists()).toBe(false); expect(view.find('code').text()).toContain('<img')
  expect(view.findAll('a')).toHaveLength(1); expect(view.find('a').attributes('rel')).toContain('noopener')
 })
 it('serializes media IDs without persisting display URLs', () => {
  const block = emptyBlock('GALLERY'); block.media.push({ mediaAssetId: '42', caption: '封面', url: 'http://local/media' })
  expect(blockPayload(block).media).toEqual([{ mediaAssetId: '42', caption: '封面' }])
  expect(safeWorkUrl('https://user:password@example.org')).toBe(false)
  expect(safeWorkUrl('data:text/html,x')).toBe(false)
 })
 it('renders media, feature, technology and text blocks with captions, native audio and sanitized markup', () => {
  const view = mount(WorkSectionRenderer, { props: { sections: [
   { id: '1', sectionType: 'MARKDOWN', content: '**项目背景** <script>alert(1)</script>', data: {} },
   { id: '2', sectionType: 'CUSTOM', content: '**自定义说明**', data: {} },
   { id: '3', sectionType: 'IMAGE', media: [{ mediaAssetId: '31', url: '/media/31', caption: '单图说明' }] },
   { id: '4', sectionType: 'GALLERY', media: [{ mediaAssetId: '41', url: '/media/41', caption: '第一张' }, { mediaAssetId: '42', url: '/media/42', caption: '第二张' }] },
   { id: '5', sectionType: 'AUDIO', media: [{ mediaAssetId: '51', url: '/media/51', caption: '音频说明' }] },
   { id: '6', sectionType: 'FEATURE_LIST', data: { items: [{ title: '检索', description: '全文查找' }] } },
   { id: '7', sectionType: 'TECH_STACK', data: { items: [{ name: 'Java', group: '服务端' }] } },
   { id: '8', sectionType: 'QUOTE', content: '复盘结论', data: { author: '作者' } },
  ] } })
  expect(view.find('script').exists()).toBe(false)
  expect(view.findAll('strong').map(el => el.text())).toContain('自定义说明')
  expect(view.findAll('img').map(el => el.attributes('src'))).toEqual(['/media/31', '/media/41', '/media/42'])
  expect(view.findAll('img').map(el => el.attributes('alt'))).toEqual(['单图说明', '第一张', '第二张'])
  expect(view.find('audio').attributes()).toEqual(expect.objectContaining({ src: '/media/51', controls: '', preload: 'metadata' }))
  expect(view.text()).toContain('音频说明'); expect(view.text()).toContain('全文查找'); expect(view.text()).toContain('服务端')
  expect(view.find('cite').text()).toContain('作者')
 })
})
