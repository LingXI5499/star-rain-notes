import { get, post } from '../../../shared/http'

export const recordPageView = (routeKey, referrer) => post('/public/analytics/page-view', { routeKey, referrer })
export const getTrafficSummary = () => get('/admin/analytics/summary')
export const getTrafficTrend = (params) => get('/admin/analytics/trend', params)
export const getHotContent = (params) => get('/admin/analytics/hot-content', params)
export const getReferrers = (params) => get('/admin/analytics/referrers', params)
