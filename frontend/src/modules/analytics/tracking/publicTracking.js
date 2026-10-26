/**
 * publicTracking.js：访问统计模块的应用配置与入口。
 * 职责范围：前台访问事件、按日汇总和统计查询。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 主要依赖：../api/analyticsApi。
 * 阅读入口：installPublicTracking（install公开Tracking的处理函数）。
 */

import { recordPageView } from '../api/analyticsApi'
import { isPrivateRoute } from '../../../shared/routeVisibility'

const allowed = new Set(['/', '/blog', '/blog/archive', '/tutorials', '/portfolio', '/messages', '/about', '/search'])

export
  /**
   * install公开Tracking的处理函数。
   * 主要协作：router.afterEach、to.path.startsWith、allowed.has、.catch、recordPageView。
   * @param router router；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function installPublicTracking(router) {
  let first = true
  router.afterEach((to, from, failure) => {
    if (failure || isPrivateRoute(to) || !to.meta?.publicPage) return
    const isEntry = first
    first = false
    if (!allowed.has(to.path)) return
    let referrer = ''
    if (isEntry && document.referrer) {
      try { referrer = new URL(document.referrer).origin } catch { /* 无效来源按直接访问处理 */ }
    }
    else if (!isEntry) referrer = window.location.origin
    recordPageView(to.path, referrer).catch(() => {})
  })
}
