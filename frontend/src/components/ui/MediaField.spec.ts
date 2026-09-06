import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent } from 'vue'
import MediaField from './MediaField.vue'

const asset = (id: number, name = `asset-${id}.png`) => ({
  id,
  assetType: 'IMAGE',
  originalName: name,
  mimeType: 'image/png',
  extension: 'png',
  sizeBytes: 2048,
  width: 800,
  height: 600,
  publicUrl: `/uploads/${name}`,
  createdAt: '2026-01-01T00:00:00Z',
})

vi.mock('@/api/media', () => ({
  fetchMediaAssets: vi.fn(async ({ assetType }: { assetType?: string }) => ({
    items: [asset(5, 'avatar.png'), asset(6, 'brand.png')].filter((item) => item.assetType === assetType || !assetType),
    page: 1,
    pageSize: 200,
    total: 2,
    totalPages: 1,
  })),
  uploadMedia: vi.fn(),
  formatSize: (bytes: number) => `${bytes} B`,
  deleteMedia: vi.fn(),
}))

const PickerStub = defineComponent({
  name: 'MediaPicker',
  props: ['modelValue'],
  emits: ['update:modelValue', 'select'],
  template: '<div class="media-picker-stub" />',
})

describe('MediaField（图形化媒体字段）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads and previews the media referenced by an existing id', async () => {
    const wrapper = mount(MediaField, {
      props: { modelValue: 5, assetType: 'IMAGE' },
      global: { stubs: { MediaPicker: PickerStub, 'el-button': { template: '<button><slot /></button>' } } },
    })
    await flushPromises()
    expect(wrapper.get('.media-field__preview img').attributes('src')).toBe('/uploads/avatar.png')
    expect(wrapper.get('.media-field__name').text()).toBe('avatar.png')
  })

  it('shows the empty state when no media is selected', async () => {
    const wrapper = mount(MediaField, {
      props: { modelValue: null, assetType: 'IMAGE', emptyText: '未设置头像' },
      global: { stubs: { MediaPicker: PickerStub, 'el-button': { template: '<button><slot /></button>' } } },
    })
    await flushPromises()
    expect(wrapper.get('.media-field__empty').text()).toBe('未设置头像')
    expect(wrapper.find('.media-field__preview img').exists()).toBe(false)
  })

  it('applies a selected media asset and emits its id', async () => {
    const wrapper = mount(MediaField, {
      props: { modelValue: null, assetType: 'IMAGE' },
      global: { stubs: { MediaPicker: PickerStub, 'el-button': { template: '<button><slot /></button>' } } },
    })
    await flushPromises()
    await wrapper.get('.media-field__actions button').trigger('click')
    const picker = wrapper.getComponent(PickerStub)
    picker.vm.$emit('select', asset(6, 'brand.png'))
    await flushPromises()
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([6])
    expect(wrapper.get('.media-field__preview img').attributes('src')).toBe('/uploads/brand.png')
  })

  it('removes the selection via the remove button', async () => {
    const wrapper = mount(MediaField, {
      props: { modelValue: 5, assetType: 'IMAGE' },
      global: { stubs: { MediaPicker: PickerStub, 'el-button': { template: '<button><slot /></button>' } } },
    })
    await flushPromises()
    const buttons = wrapper.findAll('.media-field__actions button')
    await buttons[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([null])
    expect(wrapper.get('.media-field__empty').text()).toBe('尚未选择')
  })
})
