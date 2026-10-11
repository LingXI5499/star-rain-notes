/**
 * siteApi.js：站点模块的接口封装。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：getPublicSiteConfig（读取公开站点配置）；getPublicSiteHome（读取公开站点首页）；getAdminSiteDashboard（读取后台站点概览）；listHomeSections（列出首页Sections）；orderHomeSections（顺序首页Sections的处理函数）。
 */

import { del, get, patch, put } from '../../../shared/http'

/**
 * 读取公开站点配置。
 * 请求入口：get('/public/site/config')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicSiteConfig = () => get('/public/site/config')
/**
 * 读取公开站点首页。
 * 请求入口：get('/public/site/home')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPublicSiteHome = () => get('/public/site/home')
/**
 * 读取后台站点概览。
 * 请求入口：get('/admin/site/dashboard')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAdminSiteDashboard = () => get('/admin/site/dashboard')
/**
 * 列出首页Sections。
 * 请求入口：get('/admin/site/home-sections')；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listHomeSections = () => get('/admin/site/home-sections')
/**
 * 顺序首页Sections的处理函数。
 * 请求入口：put('/admin/site/home-sections/order')；路径参数的编码和请求体结构保持接口契约。
 * @param sectionCodes 区块Codes；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const orderHomeSections = (sectionCodes) => put('/admin/site/home-sections/order', { sectionCodes })
/**
 * patch首页区块的处理函数。
 * 请求入口：patch(`/admin/site/home-sections/${sectionCode}`)；路径参数的编码和请求体结构保持接口契约。
 * @param sectionCode 区块代码；默认值、解构字段与可选参数见函数签名。
 * @param changes changes；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const patchHomeSection = (sectionCode, changes) => patch(`/admin/site/home-sections/${sectionCode}`, changes)
/**
 * patch站点配置的处理函数。
 * 请求入口：patch('/admin/site/config')；路径参数的编码和请求体结构保持接口契约。
 * @param changes changes；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const patchSiteConfig = (changes) => patch('/admin/site/config', changes)
/**
 * 设置站点媒体。
 * 请求入口：put(`/admin/site/${kind}`)；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @param mediaAssetId 关联的媒体资产标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const setSiteMedia = (kind, mediaAssetId) => put(`/admin/site/${kind}`, { mediaAssetId })
/**
 * 清理站点媒体。
 * 请求入口：del(`/admin/site/${kind}`)；路径参数的编码和请求体结构保持接口契约。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const clearSiteMedia = (kind) => del(`/admin/site/${kind}`)
