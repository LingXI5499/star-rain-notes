/**
 * routeLoadFailure.spec.js：跨模块基础模块的自动化测试。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./routeLoadFailure。
 * 验证场景：route asset failures；offers recovery for stale or unavailable chunks without automatically refreshing；does not label unrelated business errors as chunk failures。
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { handleRouteLoadError, reloadRoute, routeLoadFailed } from './routeLoadFailure'

beforeEach(() => { routeLoadFailed.value = false })
describe('route asset failures', () => {
  it('offers recovery for stale or unavailable chunks without automatically refreshing', () => {
    const reload = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
    handleRouteLoadError(new Error('Failed to fetch dynamically imported module'))
    expect(routeLoadFailed.value).toBe(true)
    expect(reload).not.toHaveBeenCalled()
    reloadRoute(); expect(reload).toHaveBeenCalledOnce()
    reload.mockRestore()
  })
  it('does not label unrelated business errors as chunk failures', () => {
    handleRouteLoadError(new Error('Access denied'))
    expect(routeLoadFailed.value).toBe(false)
  })
})
