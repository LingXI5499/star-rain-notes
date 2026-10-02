/*
 * 媒体模块的展示辅助。
 *
 * 分类口径与后端 MediaType 枚举一一对应：前端按这个分类渲染与筛选，
 * 图片/音频/视频可直接在浏览器播放，文档与压缩包走下载入口。
 */

export const MEDIA_TYPES = [
  { value: 'IMAGE', label: '图片', hint: 'png / jpg / webp / gif / bmp / tiff / avif' },
  { value: 'DOCUMENT', label: '文档', hint: 'pdf / md / txt / doc(x) / xls(x) / ppt(x)' },
  { value: 'AUDIO', label: '音频', hint: 'mp3 / m4a / aac / ogg / wav / flac' },
  { value: 'VIDEO', label: '视频', hint: 'mp4 / mov / webm / mkv / avi' },
  { value: 'ARCHIVE', label: '压缩包', hint: 'zip / rar / 7z / tar / gz' },
]

export const MEDIA_TYPE_CODES = MEDIA_TYPES.map((item) => item.value)

const typeLabels = Object.fromEntries(MEDIA_TYPES.map((item) => [item.value, item.label]))

export const mediaTypeLabel = (code) => typeLabels[code] || code || '未知'

export const accessLevelLabel = (code) => ({ PUBLIC: '公开', PROTECTED: '受保护' })[code] || code

export const mediaStatusLabel = (code) => ({ ACTIVE: '可用', ARCHIVED: '已归档' })[code] || code

// 浏览器能直接播放/显示的类型；其余类型给下载入口，避免渲染未知格式
export function isBrowserPlayable(mediaType) {
  return mediaType === 'IMAGE' || mediaType === 'AUDIO' || mediaType === 'VIDEO'
}

// 文件类型在卡片上的角标文案
export function badgeText(asset) {
  const extension = (asset?.fileExtension || '').toUpperCase()
  return extension || mediaTypeLabel(asset?.mediaType).slice(0, 4)
}

export function formatSize(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value < 0) return '—'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`
}

// 后端返回的是无时区的 LocalDateTime，与 account 模块保持同一约定
export function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : value + 'Z')
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

/*
 * 上传白名单的真正来源是后端 MediaFileType + 配置；
 * 这里的 accept 只是给文件选择器一个提示，不作为校验依据。
 */
export const ACCEPT_ATTRIBUTE = [
  'image/*', 'application/pdf', 'text/markdown', 'text/plain',
  'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'audio/*', 'video/*', 'application/zip', 'application/x-7z-compressed',
  'application/vnd.rar', 'application/x-tar', 'application/gzip',
  '.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp', '.tiff', '.tif', '.avif',
  '.pdf', '.md', '.markdown', '.txt', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
  '.mp3', '.m4a', '.aac', '.ogg', '.oga', '.wav', '.flac',
  '.mp4', '.m4v', '.mov', '.webm', '.mkv', '.avi', '.ogv',
  '.zip', '.rar', '.7z', '.tar', '.gz', '.tgz',
].join(',')

// 各类型的大小上限与后端配置一致，界面上提前提示，避免白等一次上传
export const SIZE_HINTS = [
  { label: '图片', limit: '20MB' },
  { label: '文档', limit: '50MB' },
  { label: '音频', limit: '100MB' },
  { label: '视频', limit: '500MB' },
  { label: '压缩包', limit: '200MB' },
]
