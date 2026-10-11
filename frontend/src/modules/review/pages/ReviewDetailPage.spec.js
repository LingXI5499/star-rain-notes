import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import Page from './ReviewDetailPage.vue'
import { getReview, recoverReview } from '../api/reviewApi'
const state = vi.hoisted(() => ({ canManage: true, ask: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { reviewId: '9' } }), RouterLink: true }))
vi.mock('../../account/stores/authStore', () => ({ useAuthStore: () => ({ canManage: () => state.canManage }) }))
vi.mock('../api/reviewApi', () => ({ cancelReview: vi.fn(), getReview: vi.fn(), recoverReview: vi.fn() }))
const detail = status => ({ reviewId: '9', status: 'APPROVED', history: [], targetModule: 'TUTORIAL', targetType: 'OPERATION', targetView: { viewType: 'TUTORIAL_OPERATION', executionStatus: status, executionMessage: status === 'FAILED' ? '内容版本已改变' : null } })
const wrappers = []
const open = () => {
  const wrapper = mount(Page, { global: { stubs: {
    ReviewApproveDialog: true, ReviewHistoryTimeline: true, ReviewRejectDialog: true, ReviewStatusTag: true, ReviewTargetPreview: true,
    AppConfirmDialog: { setup(_, { expose }) { expose({ ask: state.ask }) }, template: '<div />' },
  } } }); wrappers.push(wrapper); return wrapper
}
beforeEach(() => { vi.clearAllMocks(); state.canManage = true; state.ask.mockResolvedValue(true); getReview.mockResolvedValue(detail('FAILED')); recoverReview.mockResolvedValue({}) })
afterEach(() => wrappers.splice(0).forEach(wrapper => wrapper.unmount()))
describe('approved review application recovery', () => {
  it('distinguishes approval from application and explicitly recovers without replaying approval', async () => {
    const wrapper = open(); await flushPromises()
    expect(wrapper.text()).toContain('审核已通过，业务变更尚未生效')
    expect(wrapper.text()).toContain('内容版本已改变')
    getReview.mockResolvedValue(detail('APPLIED'))
    await wrapper.findAll('button').find(button => button.text() === '重新核实并恢复').trigger('click'); await flushPromises()
    expect(recoverReview).toHaveBeenCalledWith('9')
    expect(wrapper.text()).not.toContain('业务变更尚未生效')
  })
  it('keeps the diagnostic visible to an administrator without exposing the recovery button', async () => {
    state.canManage = false; const wrapper = open(); await flushPromises()
    expect(wrapper.text()).toContain('内容版本已改变')
    expect(wrapper.findAll('button').some(button => button.text() === '重新核实并恢复')).toBe(false)
  })
  it('does not execute when recovery confirmation is declined', async () => {
    state.ask.mockResolvedValue(false); const wrapper = open(); await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '重新核实并恢复').trigger('click'); await flushPromises()
    expect(recoverReview).not.toHaveBeenCalled()
  })
})
