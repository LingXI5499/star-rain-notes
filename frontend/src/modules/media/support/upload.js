import { errorMessage } from '../../../shared/http'

export const IMAGE_ACCEPT = '.png,.jpg,.jpeg,.webp,.gif,.bmp,.tiff,.tif,.avif'
export const IMAGE_MAX_BYTES = 20 * 1024 * 1024

export function validateEditorImage(file) {
  const extension = `.${String(file.name).split('.').pop().toLowerCase()}`
  if (!IMAGE_ACCEPT.split(',').includes(extension)) return '请选择 PNG、JPEG、WebP、GIF、BMP、TIFF 或 AVIF 图片。'
  if (!file.size) return '不能上传空图片。'
  if (file.size > IMAGE_MAX_BYTES) return '图片不能超过 20 MB。'
  return ''
}

export function mediaUploadError(cause) {
  // 使用固定文案，兼容旧服务端可能包含磁盘路径的错误响应。
  if (cause?.response?.data?.code === 'MEDIA_STORAGE_WRITE_FAILED') {
    return '媒体存储目录无法写入。请检查宝塔 Java 项目运行用户对媒体目录的写入权限，以及磁盘剩余空间。'
  }
  if (['ECONNABORTED', 'ETIMEDOUT'].includes(cause?.code)) return '上传超时，请先检查媒体库是否已经收到文件，再决定是否重试。'
  if (cause?.response?.status === 413) return '文件超过服务器上传限制，请缩小文件，或检查宝塔网站的上传大小限制。'
  return errorMessage(cause)
}

export function imageMarkdown(asset) {
  const url = asset.contentUrl || asset.url
  if (!url || !/^(\/api\/media\/assets\/[^/]+\/content|https?:\/\/)/.test(url)) throw new Error('图片地址不可用')
  const alt = String(asset.originalName || asset.name || '图片').replace(/[\\[\]\r\n]/g, ' ').trim() || '图片'
  return `![${alt}](<${url.replace(/[<>\s]/g, value => encodeURIComponent(value))}>)`
}
