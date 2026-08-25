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

export interface BundleItem {
  contentType: 'READING' | 'LISTENING' | 'WRITING'
  contentId: number
  title: string
  slug: string
  summary: string | null
  cefrLevel: string | null
  coverUrl: string | null
  publishStatus: PublishStatus
  sortOrder: number
}

export interface BundleCatalogItem extends BundleItem {
  selected: boolean
}

export interface BundleCatalogPage {
  items: BundleCatalogItem[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface BundleReadiness {
  totalItems: number
  publishedItems: number
  moduleCount: number
  moduleCounts: Record<BundleItem['contentType'], number>
  ready: boolean
  issues: Array<'SUMMARY_REQUIRED' | 'CEFR_REQUIRED' | 'MINIMUM_ITEMS_REQUIRED' | 'MULTIPLE_MODULES_REQUIRED' | 'UNPUBLISHED_ITEMS_PRESENT'>
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

export async function fetchPublicBundles(): Promise<LearningBundle[]> {
  return (await http.get<LearningBundle[]>('/public/english/bundles')).data
}

export async function fetchBundleItems(id: number): Promise<BundleItem[]> {
  return (await http.get<BundleItem[]>(`/admin/english/bundles/${id}/items`)).data
}
export async function fetchBundleCatalog(id: number, params: Record<string, string | number | undefined>): Promise<BundleCatalogPage> {
  return (await http.get<BundleCatalogPage>(`/admin/english/bundles/${id}/catalog`, { params })).data
}
export async function fetchBundleReadiness(id: number): Promise<BundleReadiness> {
  return (await http.get<BundleReadiness>(`/admin/english/bundles/${id}/readiness`)).data
}
export async function addBundleItem(id: number, contentType: BundleItem['contentType'], contentId: number): Promise<BundleItem> {
  return (await http.post<BundleItem>(`/admin/english/bundles/${id}/items`, { contentType, contentId })).data
}
export async function moveBundleItem(id: number, item: BundleItem, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/bundles/${id}/items/${item.contentType}/${item.contentId}/move`, { targetIndex })
}
export async function removeBundleItem(id: number, item: BundleItem): Promise<void> {
  await http.delete(`/admin/english/bundles/${id}/items/${item.contentType}/${item.contentId}`)
}
export async function fetchPublicBundleItems(slug: string): Promise<BundleItem[]> {
  return (await http.get<BundleItem[]>(`/public/english/bundles/${slug}/items`)).data
}
