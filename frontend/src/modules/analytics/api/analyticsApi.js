/**
 * analyticsApi.js：访问统计模块的接口封装。
 * 职责范围：前台访问事件、按日汇总和统计查询。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：recordPageView（记录分页View）；getTrafficSummary（读取Traffic摘要）；getTrafficTrend（读取TrafficTrend）；getHotContent（读取Hot内容）；getReferrers（读取Referrers）。
 */

import { get, post } from '../../../shared/http'

/**
 * 记录分页View。
 * 请求入口：post('/public/analytics/page-view')；路径参数的编码和请求体结构保持接口契约。
 * @param routeKey 路由键；默认值、解构字段与可选参数见函数签名。
 * @param referrer referrer；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const recordPageView = (routeKey, referrer) => post('/public/analytics/page-view', { routeKey, referrer })
/**
 * 读取Traffic摘要。
 * 请求入口：get('/admin/analytics/summary')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getTrafficSummary = () => get('/admin/analytics/summary')
/**
 * 读取TrafficTrend。
 * 请求入口：get('/admin/analytics/trend')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getTrafficTrend = (params) => get('/admin/analytics/trend', params)
/**
 * 读取Hot内容。
 * 请求入口：get('/admin/analytics/hot-content')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getHotContent = (params) => get('/admin/analytics/hot-content', params)
/**
 * 读取Referrers。
 * 请求入口：get('/admin/analytics/referrers')；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getReferrers = (params) => get('/admin/analytics/referrers', params)
