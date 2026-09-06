import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import ImageZoomOverlay from './ImageZoomOverlay.vue'

describe('ImageZoomOverlay（作品原图放大）', () => {
  let wrapper: VueWrapper | null = null
  let opener: HTMLButtonElement

  beforeEach(() => {
    document.body.innerHTML = ''
    opener = document.createElement('button')
    document.body.appendChild(opener)
    opener.focus()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
    document.body.style.overflow = ''
    document.body.innerHTML = ''
  })

  it('locks background scroll and focuses the close button while open', async () => {
    wrapper = mount(ImageZoomOverlay, {
      props: { open: true, src: '/uploads/mock.png', alt: '作品截图' },
      attachTo: document.body,
    })
    await nextTick()
    const overlay = document.querySelector('.image-zoom')
    expect(overlay).not.toBeNull()
    expect(overlay?.getAttribute('role')).toBe('dialog')
    expect(document.body.style.overflow).toBe('hidden')
    expect(document.activeElement?.classList.contains('image-zoom__close')).toBe(true)
    const img = overlay?.querySelector('img')
    expect(img?.getAttribute('src')).toBe('/uploads/mock.png')
  })

  it('closes on Escape and restores focus and scroll', async () => {
    wrapper = mount(ImageZoomOverlay, {
      props: { open: true, src: '/uploads/mock.png', alt: '作品截图' },
      attachTo: document.body,
    })
    await nextTick()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(wrapper.emitted('close')).toHaveLength(1)
    await wrapper.setProps({ open: false })
    await nextTick()
    expect(document.body.style.overflow).toBe('')
    expect(document.activeElement).toBe(opener)
    expect(document.querySelector('.image-zoom')).toBeNull()
  })

  it('closes via the close button', async () => {
    wrapper = mount(ImageZoomOverlay, {
      props: { open: true, src: '/uploads/mock.png', alt: '作品截图' },
      attachTo: document.body,
    })
    await nextTick()
    const close = document.querySelector('.image-zoom__close') as HTMLButtonElement
    close.click()
    expect(wrapper.emitted('close')).toHaveLength(1)
  })
})
