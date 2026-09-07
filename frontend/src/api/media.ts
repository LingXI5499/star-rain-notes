import { http } from './http'

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
  const { data } = await http.post<MediaAsset>('/admin/media-assets', form)
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
