import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AdminContentActions from './AdminContentActions.vue'

const stubs = {
  'el-dropdown': { template: '<div><slot /><slot name="dropdown" /></div>' },
  'el-button': { template: '<button><slot /></button>' },
  'el-dropdown-menu': { template: '<div><slot /></div>' },
}

describe('AdminContentActions', () => {
  it('shows the more trigger when a more slot is provided', () => {
    const wrapper = mount(AdminContentActions, {
      slots: { default: '<button>编辑</button>', more: '<div>复制</div><div>删除</div>' },
      global: { stubs },
    })

    expect(wrapper.text()).toContain('更多')
    expect(wrapper.text()).toContain('复制')
    expect(wrapper.text()).toContain('删除')
  })

  it('does not render an empty more trigger', () => {
    const wrapper = mount(AdminContentActions, {
      slots: { default: '<button>编辑</button>' },
      global: { stubs },
    })

    expect(wrapper.text()).not.toContain('更多')
  })
})
