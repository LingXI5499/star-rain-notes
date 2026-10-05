import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import { useListQuery } from './useListQuery'

const Probe = defineComponent({
  setup(_, { expose }) {
    const state = useListQuery({
      defaults: { keyword: '', status: '' },
      defaultPageSize: 10,
      pageSizes: [10, 20],
    })
    expose(state)
    return () => null
  },
})

async function fixture(url) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: Probe }] })
  await router.push(url)
  await router.isReady()
  const wrapper = mount(Probe, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('useListQuery', () => {
  it('reads a deep link and writes filters and pagination to browser history', async () => {
    const { router, wrapper } = await fixture('/?keyword=Java&page=3&pageSize=20')
    wrapper.vm.read()
    expect(wrapper.vm.filters.keyword).toBe('Java')
    expect(wrapper.vm.page).toBe(3)
    expect(wrapper.vm.pageSize).toBe(20)

    wrapper.vm.filters.status = 'PUBLISHED'
    wrapper.vm.page = 1
    await wrapper.vm.write()
    expect(router.currentRoute.value.query).toEqual({
      keyword: 'Java', status: 'PUBLISHED', pageSize: '20',
    })
    wrapper.unmount()
  })

  it('falls back from invalid pagination and resets filters', async () => {
    const { router, wrapper } = await fixture('/?keyword=test&page=-2&pageSize=999')
    wrapper.vm.read()
    expect(wrapper.vm.page).toBe(1)
    expect(wrapper.vm.pageSize).toBe(10)
    await wrapper.vm.reset()
    expect(router.currentRoute.value.query).toEqual({})
    wrapper.unmount()
  })
})
