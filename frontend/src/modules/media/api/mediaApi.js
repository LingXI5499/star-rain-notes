import { get, patch, post, postForm } from '../../../shared/http'

/*
 * 媒体模块后端接口封装。
 *
 * 内容读取地址（contentUrl）由后端在 VO 里给出，前端不自己拼，
 * 避免路径规则变化时前后端不一致。
 */

// MED-002 查询媒体资产：分页 + 文件名 + 类型 + 状态 + 访问级别
export const listMedia = (params) => get('/admin/media/assets', params)

// 媒体详情（含引用数量）
export const getMedia = (id) => get(`/admin/media/assets/${encodeURIComponent(id)}`)

/*
 * MED-001 上传：走 multipart，不手动设置 Content-Type，
 * 让浏览器自己补 boundary。
 */
export function uploadMedia(file, accessLevel) {
  const form = new FormData()
  form.append('file', file)
  if (accessLevel) form.append('accessLevel', accessLevel)
  return postForm('/admin/media/assets', form)
}

// MED-006 归档 / 恢复，以及归档生命周期里的访问级别调整
export const archiveMedia = (id) => post(`/admin/media/assets/${encodeURIComponent(id)}/archive`)
export const restoreMedia = (id) => post(`/admin/media/assets/${encodeURIComponent(id)}/restore`)
export const changeAccessLevel = (id, accessLevel) =>
  patch(`/admin/media/assets/${encodeURIComponent(id)}/access-level`, { accessLevel })

// MED-007 查询媒体引用情况
export const listMediaReferences = (id) =>
  get(`/admin/media/assets/${encodeURIComponent(id)}/references`)
