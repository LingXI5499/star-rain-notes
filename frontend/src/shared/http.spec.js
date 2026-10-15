/**
 * http.spec.js：跨模块基础模块的自动化测试。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./http。
 * 阅读入口：token（令牌的处理函数）。
 * 验证场景：CSRF single flight and user errors；fetches one token for ten concurrent writes and reuses it；does not let an old token repopulate state after logout；allows retry after token fetch failure and never fetches for reads；clears invalid tokens without automatically replaying writes。
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'
const state = vi.hoisted(() => ({ request: null, responseError: null, get: vi.fn() }))
vi.mock('axios', () => ({ default: {
  get: state.get,
  isCancel: error => error?.code === 'ERR_CANCELED',
  CanceledError: class extends Error { code = 'ERR_CANCELED' },
  create: () => ({ interceptors: { request: { use: callback => { state.request = callback } }, response: { use: (_, callback) => { state.responseError = callback } } } }),
} }))
import { clearCsrf, errorMessage, isRequestCanceled, invalidateAccountRequests } from './http'
/**
 * 令牌的处理函数。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const token = value => ({ data: { data: { token: value } } })
beforeEach(() => { clearCsrf(); vi.clearAllMocks() })
describe('CSRF single flight and user errors', () => {
  it('fetches one token for ten concurrent writes and reuses it', async () => {
    state.get.mockResolvedValue(token('current'))
    const configs = await Promise.all(Array.from({ length: 10 }, () => state.request({ method: 'post', headers: {} })))
    expect(state.get).toHaveBeenCalledOnce()
    expect(configs.every(config => config.headers['X-XSRF-TOKEN'] === 'current')).toBe(true)
    await state.request({ method: 'patch' }); expect(state.get).toHaveBeenCalledOnce()
  })
  it('does not let an old token repopulate state after logout', async () => {
    let resolve
    state.get.mockReturnValueOnce(new Promise(done => { resolve = done })).mockResolvedValueOnce(token('fresh'))
    const old = state.request({ method: 'post' }).catch(error => error)
    clearCsrf()
    const current = await state.request({ method: 'post' })
    resolve(token('stale')); expect(isRequestCanceled(await old)).toBe(true)
    expect(current.headers['X-XSRF-TOKEN']).toBe('fresh')
    expect((await state.request({ method: 'delete' })).headers['X-XSRF-TOKEN']).toBe('fresh')
    expect(state.get).toHaveBeenCalledTimes(2)
  })
  it('allows retry after token fetch failure and never fetches for reads', async () => {
    state.get.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(token('retry'))
    await expect(state.request({ method: 'post' })).rejects.toThrow('offline')
    await state.request({ method: 'get' }); expect(state.get).toHaveBeenCalledOnce()
    expect((await state.request({ method: 'post' })).headers['X-XSRF-TOKEN']).toBe('retry')
  })
  it('clears invalid tokens without automatically replaying writes', async () => {
    state.get.mockResolvedValueOnce(token('one')).mockResolvedValueOnce(token('two'))
    await state.request({ method: 'post' })
    const error = { config: await state.request({ method: 'get' }), response: { status: 403, data: { code: 'CSRF_INVALID' } } }
    await expect(state.responseError(error)).rejects.toBe(error)
    expect(state.get).toHaveBeenCalledOnce()
    expect((await state.request({ method: 'post' })).headers['X-XSRF-TOKEN']).toBe('two')
  })
  it('broadcasts expired sessions and clears token state on unauthorized responses', async () => {
    const listener = vi.fn(); window.addEventListener('account-session-expired', listener)
    const error = { config: await state.request({ method: 'get' }), response: { status: 401, data: { code: 'UNAUTHORIZED' } } }
    await expect(state.responseError(error)).rejects.toBe(error)
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener('account-session-expired', listener)
  })
  it('AUTH-23: a late unauthorized response from an old identity cannot expire a new login', async () => {
    const listener = vi.fn(); window.addEventListener('account-session-expired', listener)
    const config = await state.request({ method: 'get' })
    invalidateAccountRequests()
    const error = { config, response: { status: 401, data: { code: 'UNAUTHORIZED' } } }
    await expect(state.responseError(error)).rejects.toBe(error)
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener('account-session-expired', listener)
  })
  it('distinguishes cancellations, timeout, offline, permissions, business and server errors', () => {
    expect(errorMessage({ code: 'ERR_CANCELED' })).toBe('')
    expect(errorMessage({ name: 'AbortError' })).toBe('')
    expect(errorMessage({ code: 'ECONNABORTED' })).toContain('超时')
    expect(errorMessage(new Error('offline'))).toContain('网络')
    expect(errorMessage({ response: { status: 403 } })).toContain('权限')
    expect(errorMessage({ response: { status: 401, data: { code: 'INVALID_CREDENTIALS' } } })).toContain('用户名或密码错误')
    expect(errorMessage({ response: { status: 500, data: { message: 'SQL stack secret' } } })).not.toContain('SQL')
    expect(errorMessage({ response: { status: 409, data: { message: '内容版本冲突' } } })).toBe('内容版本冲突')
  })
})
