import { http } from './http'

export const PROTOTYPE_ARCHIVE_MAX_BYTES = 25 * 1024 * 1024

export function prototypeArchiveFileError(file: File): string | null {
  if (!file.name.toLowerCase().endsWith('.zip')) return '请选择 ZIP 压缩包。'
  if (file.size > PROTOTYPE_ARCHIVE_MAX_BYTES) {
    return `ZIP 原型包最大 25 MB；当前文件为 ${formatSize(file.size)}。请只压缩项目运行所需的 HTML、CSS、JavaScript、图片与字体。`
  }
  return null
}

export interface MediaAsset {
  id: number
  assetType: string
  originalName: string
  mimeType: string
  extension: string
  sizeBytes: number
  width: number | null
  height: number | null
  publicUrl: string
  srcSet: string | null
  createdAt: string
}

export interface MediaPage {
  items: MediaAsset[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface MediaSummary {
  total: number
  images: number
  audio: number
  documents: number
  archives: number
}

export async function fetchMediaSummary(): Promise<MediaSummary> {
  const { data } = await http.get<MediaSummary>('/admin/media-assets/summary')
  return data
}

export async function fetchMediaAssets(params: {
  page?: number
  pageSize?: number
  q?: string
  assetType?: string
}): Promise<MediaPage> {
  const { data } = await http.get<MediaPage>('/admin/media-assets', { params })
  return data
}

export async function uploadMedia(file: File): Promise<MediaAsset> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<MediaAsset>('/admin/media-assets', form, { timeout: 60000 })
  return data
}

export async function deleteMedia(id: number): Promise<void> {
  await http.delete(`/admin/media-assets/${id}`)
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
