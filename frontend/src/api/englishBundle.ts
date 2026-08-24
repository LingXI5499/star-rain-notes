import { http } from './http'
import type { PublishStatus } from './englishMeta'

export interface LearningBundle {
  id: number
  title: string
  slug: string
  summary: string | null
  primaryCefr: string | null
  coverMediaId: number | null
  coverUrl: string | null
  publishStatus: PublishStatus
  sortOrder: number
  publishedAt: string | null
  updatedAt: string
}

export interface BundlePayload {
  title: string
  slug: string
  summary?: string | null
  primaryCefr?: string | null
  coverMediaId?: number | null
  sortOrder?: number | null
}

export async function fetchBundles(): Promise<LearningBundle[]> {
  return (await http.get<LearningBundle[]>('/admin/english/bundles')).data
}

export async function fetchBundle(id: number): Promise<LearningBundle> {
  return (await http.get<LearningBundle>(`/admin/english/bundles/${id}`)).data
}

export async function createBundle(payload: BundlePayload): Promise<LearningBundle> {
  return (await http.post<LearningBundle>('/admin/english/bundles', payload)).data
}

export async function updateBundle(id: number, payload: BundlePayload): Promise<LearningBundle> {
  return (await http.put<LearningBundle>(`/admin/english/bundles/${id}`, payload)).data
}

export async function deleteBundle(id: number): Promise<void> {
  await http.delete(`/admin/english/bundles/${id}`)
}

export async function publishBundle(id: number): Promise<LearningBundle> {
  return (await http.post<LearningBundle>(`/admin/english/bundles/${id}/publish`)).data
}

export async function withdrawBundle(id: number): Promise<LearningBundle> {
  return (await http.post<LearningBundle>(`/admin/english/bundles/${id}/withdraw`)).data
}

export async function fetchPublicBundle(slug: string): Promise<LearningBundle> {
  return (await http.get<LearningBundle>(`/public/english/bundles/${slug}`)).data
}
