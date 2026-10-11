/**
 * ReviewDetailPage.spec.js：审核模块的自动化测试。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：@vue/test-utils、vitest、./ReviewDetailPage.vue、../api/reviewApi。
 * 阅读入口：detail（详情的处理函数）；open（打开当前操作）。
 * 验证场景：approved review application recovery；distinguishes approval from application and explicitly recovers without replaying approval；keeps the diagnostic visible to an administrator without exposing the recovery button；does not execute when recovery confirmation is declined。
 */

import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import Page from './ReviewDetailPage.vue'
import { getReview, recoverReview } from '../api/reviewApi'
const state = vi.hoisted(() => ({ canManage: true, ask: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { reviewId: '9' } }), RouterLink: true }))
vi.mock('../../account/stores/authStore', () => ({ useAuthStore: () => ({ canManage: () => state.canManage }) }))
vi.mock('../api/reviewApi', () => ({ cancelReview: vi.fn(), getReview: vi.fn(), recoverReview: vi.fn() }))
/**
 * 详情的处理函数。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const detail = status => ({ reviewId: '9', status: 'APPROVED', history: [], targetModule: 'TUTORIAL', targetType: 'OPERATION', targetView: { viewType: 'TUTORIAL_OPERATION', executionStatus: status, executionMessage: status === 'FAILED' ? '内容版本已改变' : null } })
const wrappers = []
/**
 * 打开当前操作。
 * 主要协作：mount、expose、wrappers.push。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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
