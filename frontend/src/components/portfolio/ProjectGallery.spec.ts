import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import type { ProjectMediaItem } from '@/api/portfolio'
import ProjectGallery from './ProjectGallery.vue'

function galleryItems(count: number): ProjectMediaItem[] {
  return Array.from({ length: count }, (_, index) => ({
    id: index + 1,
    mediaAssetId: index + 1,
    url: `/uploads/project-${index + 1}.webp`,
    title: `截图 ${index + 1}`,
    description: null,
    altText: null,
    deviceType: 'desktop',
    sortOrder: index,
    width: 1440,
    height: 900,
    srcSet: null,
  }))
}

describe('ProjectGallery lightbox navigation', () => {
  it('shows previous/next buttons and wraps through images', async () => {
    const wrapper = mount(ProjectGallery, { props: { items: galleryItems(3) } })

    await wrapper.get('.project-gallery__frame').trigger('click')
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-1.webp')
    expect(wrapper.get('.project-gallery__lightbox-count').text()).toBe('01 / 03')

    await wrapper.get('.project-gallery__lightbox-nav--next').trigger('click')
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-2.webp')

    await wrapper.get('.project-gallery__lightbox-nav--prev').trigger('click')
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-1.webp')

    await wrapper.get('.project-gallery__lightbox-nav--prev').trigger('click')
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-3.webp')
    wrapper.unmount()
  })

  it('switches lightbox images with the left and right arrow keys', async () => {
    const wrapper = mount(ProjectGallery, { props: { items: galleryItems(2) } })

    await wrapper.get('.project-gallery__frame').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-2.webp')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.get('.project-gallery__lightbox img').attributes('src')).toBe('/uploads/project-1.webp')
    wrapper.unmount()
  })

  it('does not render lightbox navigation for a single image', async () => {
    const wrapper = mount(ProjectGallery, { props: { items: galleryItems(1) } })

    await wrapper.get('.project-gallery__frame').trigger('click')
    expect(wrapper.find('.project-gallery__lightbox-nav').exists()).toBe(false)
    wrapper.unmount()
  })
})
