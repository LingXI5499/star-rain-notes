import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import AppConfirmDialog from './AppConfirmDialog.vue'

const originalShowModal = HTMLDialogElement.prototype.showModal
const originalClose = HTMLDialogElement.prototype.close

beforeEach(() => {
  HTMLDialogElement.prototype.showModal = vi.fn()
  HTMLDialogElement.prototype.close = vi.fn()
})

afterEach(() => {
  HTMLDialogElement.prototype.showModal = originalShowModal
  HTMLDialogElement.prototype.close = originalClose
})

describe('AppConfirmDialog', () => {
  it('keeps the existing text and options API for account and blog pages', async () => {
    const wrapper = mount(AppConfirmDialog)
    const cancelled = wrapper.vm.ask('确认删除？', { title: '删除内容', confirmText: '删除' })
    await nextTick()
    expect(wrapper.find('h2').text()).toBe('删除内容')
    expect(wrapper.find('p').text()).toBe('确认删除？')
    await wrapper.findAll('button')[0].trigger('click')
    expect(await cancelled).toBe(false)

    const accepted = wrapper.vm.ask('继续操作？')
    await nextTick()
    await wrapper.findAll('button')[1].trigger('click')
    expect(await accepted).toBe(true)
    wrapper.unmount()
  })

  it('requires the object name for irreversible actions', async () => {
    const wrapper = mount(AppConfirmDialog)
    const result = wrapper.vm.ask({ message: '删除后无法恢复。', requireName: '测试文章', danger: true })
    await nextTick()
    const confirm = wrapper.findAll('button')[1]
    expect(confirm.attributes('disabled')).toBeDefined()
    await wrapper.find('input').setValue('测试文章')
    expect(confirm.attributes('disabled')).toBeUndefined()
    await confirm.trigger('click')
    expect(await result).toBe(true)
    wrapper.unmount()
  })
})
