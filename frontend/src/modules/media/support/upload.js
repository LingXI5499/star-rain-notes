/**
 * upload.js：媒体模块的业务支持工具。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：../../../shared/http。
 * 阅读入口：validateEditorImage（校验Editor图片）；mediaUploadError（媒体上传错误的处理函数）；imageMarkdown（图片Markdown的处理函数）。
 */

import { errorMessage } from '../../../shared/http'

export const IMAGE_ACCEPT = '.png,.jpg,.jpeg,.webp,.gif,.bmp,.tiff,.tif,.avif'
export const IMAGE_MAX_BYTES = 20 * 1024 * 1024

export
  /**
   * 校验Editor图片。
   * 主要协作：.toLowerCase、.pop、.split、.includes、IMAGE_ACCEPT.split。
   * @param file 待处理的上传文件；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function validateEditorImage(file) {
  const extension = `.${String(file.name).split('.').pop().toLowerCase()}`
  if (!IMAGE_ACCEPT.split(',').includes(extension)) return '请选择 PNG、JPEG、WebP、GIF、BMP、TIFF 或 AVIF 图片。'
  if (!file.size) return '不能上传空图片。'
  if (file.size > IMAGE_MAX_BYTES) return '图片不能超过 20 MB。'
  return ''
}

export
  /**
   * 媒体上传错误的处理函数。
   * 主要协作：.includes、errorMessage。
   * @param cause 原始异常，用于保留失败原因；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function mediaUploadError(cause) {
  const response = cause?.response
  const code = response?.data?.code
  // 使用固定文案，兼容旧服务端可能包含磁盘路径的错误响应。
  if (code === 'MEDIA_STORAGE_WRITE_FAILED') {
    return '媒体存储目录无法写入。请检查宝塔 Java 项目运行用户对媒体目录的写入权限，以及磁盘剩余空间。（MEDIA_STORAGE_WRITE_FAILED）'
  }
  if (code === 'MEDIA_CONTENT_TYPE_MISMATCH') {
    return '文件内容或声明类型与扩展名不一致；Markdown 和 TXT 请使用 UTF-8 编码。（MEDIA_CONTENT_TYPE_MISMATCH）'
  }
  if (['ECONNABORTED', 'ETIMEDOUT'].includes(cause?.code)) return '上传超时，请先检查媒体库是否已经收到文件，再决定是否重试。'
  if (response?.status === 413) return '文件超过服务器上传限制，请缩小文件，或检查宝塔网站的上传大小限制。'
  if (response?.status >= 500) {
    // 只显示约定的错误码，不展示服务端原始消息、路径或任意响应正文。
    const detail = ['INTERNAL_ERROR', 'INTERNAL_SERVER_ERROR'].includes(code) ? ` / ${code}` : ''
    return `上传失败（HTTP ${response.status}${detail}），请查看 Java 项目日志中此次上传的异常。`
  }
  return errorMessage(cause)
}

export
  /**
   * 图片Markdown的处理函数。
   * 主要协作：.test、.trim、.replace、url.replace、encodeURIComponent。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @param asset 资产；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function imageMarkdown(asset) {
  const url = asset.contentUrl || asset.url
  if (!url || !/^(\/api\/media\/assets\/[^/]+\/content|https?:\/\/)/.test(url)) throw new Error('图片地址不可用')
  const alt = String(asset.originalName || asset.name || '图片').replace(/[\\[\]\r\n]/g, ' ').trim() || '图片'
  return `![${alt}](<${url.replace(/[<>\s]/g, value => encodeURIComponent(value))}>)`
}
