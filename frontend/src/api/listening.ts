import { http } from './http'
import type { ContentReview } from './account'
import type { PublishStatus } from './englishMeta'

export interface ListeningTagRef { id: number; name: string; slug: string; dimension: string; role: string }
export interface ListeningSegment { id: number; itemId: number; startMs: number; endMs: number; transcriptText: string; translationText: string | null; sortOrder: number; updatedAt: string }
export interface ReadingPairRef { readingArticleId: number; readingTitle: string; readingSlug: string; relationType: string }
export interface ListeningLink { title: string; slug: string }

export interface ListeningItem {
  id: number
  title: string
  slug: string
  summary: string
  transcriptMarkdown: string | null
  cefrLevel: string
  listeningLevel: number
  audioMediaId: number | null
  audioUrl: string | null
  coverMediaId: number | null
  coverUrl: string | null
  durationSeconds: number
  sourceName: string | null
  sourceUrl: string | null
  copyrightNote: string | null
  publishStatus: PublishStatus
  sortOrder: number
  publishedAt: string | null
  updatedAt: string
  tags: ListeningTagRef[]
  segments: ListeningSegment[]
  readingPairs: ReadingPairRef[]
  previous: ListeningLink | null
  next: ListeningLink | null
}

export interface ListeningSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  cefrLevel: string
  listeningLevel: number
  durationSeconds: number
  publishStatus: PublishStatus
  exerciseCount: number
  segmentCount: number
  updatedAt: string
  tags: ListeningTagRef[]
}
export interface ListeningAdminStats { total: number; published: number; draft: number; withdrawn: number; missingAudio: number; byLevel: Record<number, number> }
export interface ListeningPage { items: ListeningSummary[]; page: number; pageSize: number; total: number; totalPages: number; stats: ListeningAdminStats | null }
export interface ListeningHome { total: number; byLevel: Record<number, number>; byCefr: Record<string, number>; topics: ListeningTagRef[]; scenes: ListeningTagRef[]; formats: ListeningTagRef[] }

export interface ListeningItemPayload {
  title: string; slug: string; summary: string; transcriptMarkdown?: string | null
  cefrLevel: string; listeningLevel: number; audioMediaId?: number | null; coverMediaId?: number | null
  durationSeconds?: number; sourceName?: string | null; sourceUrl?: string | null; copyrightNote?: string | null
  sortOrder?: number | null; topicTagIds?: number[] | null; sceneTagIds?: number[] | null; formatTagIds?: number[] | null
  abilityTagIds?: number[] | null; functionTagIds?: number[] | null
}
export interface ListeningSegmentPayload { startMs: number; endMs: number; transcriptText: string; translationText?: string | null }
export interface PronunciationRule {
  id: number; ruleType: string; title: string; slug: string; summary: string; bodyMarkdown: string
  audioMediaId: number | null; audioUrl: string | null; publishStatus: PublishStatus; sortOrder: number
  publishedAt: string | null; updatedAt: string; previous: ListeningLink | null; next: ListeningLink | null
}

export type ListeningExercisePublic = { id: number; questionType: string; promptMarkdown: string; config: Record<string, unknown>; scoreValue: number; sortOrder: number }

export async function fetchListenings(params: Record<string, string | number | undefined>): Promise<ListeningPage> {
  return (await http.get<ListeningPage>('/admin/english/listening/items', { params })).data
}
export async function fetchListening(id: number): Promise<ListeningItem> {
  return (await http.get<ListeningItem>(`/admin/english/listening/items/${id}`)).data
}
export async function createListening(payload: ListeningItemPayload): Promise<ListeningItem> {
  return (await http.post<ListeningItem>('/admin/english/listening/items', payload)).data
}
export async function updateListening(id: number, payload: ListeningItemPayload): Promise<ListeningItem | ContentReview> {
  return (await http.put<ListeningItem | ContentReview>(`/admin/english/listening/items/${id}`, payload)).data
}
export async function deleteListening(id: number): Promise<void> { await http.delete(`/admin/english/listening/items/${id}`) }
export async function publishListening(id: number): Promise<ListeningItem> { return (await http.post<ListeningItem>(`/admin/english/listening/items/${id}/publish`)).data }
export async function withdrawListening(id: number): Promise<ListeningItem> { return (await http.post<ListeningItem>(`/admin/english/listening/items/${id}/withdraw`)).data }

export async function fetchSegments(id: number): Promise<ListeningSegment[]> { return (await http.get<ListeningSegment[]>(`/admin/english/listening/items/${id}/segments`)).data }
export async function createSegment(id: number, payload: ListeningSegmentPayload): Promise<ListeningSegment> { return (await http.post<ListeningSegment>(`/admin/english/listening/items/${id}/segments`, payload)).data }
export async function updateSegment(id: number, segmentId: number, payload: ListeningSegmentPayload): Promise<ListeningSegment> { return (await http.put<ListeningSegment>(`/admin/english/listening/items/${id}/segments/${segmentId}`, payload)).data }
export async function deleteSegment(id: number, segmentId: number): Promise<void> { await http.delete(`/admin/english/listening/items/${id}/segments/${segmentId}`) }
export async function moveSegment(id: number, segmentId: number, targetIndex: number): Promise<void> { await http.post(`/admin/english/listening/items/${id}/segments/${segmentId}/move`, { targetIndex }) }
export async function batchSegments(id: number, segments: ListeningSegmentPayload[]): Promise<ListeningSegment[]> { return (await http.put<ListeningSegment[]>(`/admin/english/listening/items/${id}/segments/batch`, { segments })).data }
export async function fetchReadingPairs(id: number): Promise<ReadingPairRef[]> { return (await http.get<ReadingPairRef[]>(`/admin/english/listening/items/${id}/reading-pairs`)).data }
export async function addReadingPair(id: number, readingArticleId: number, relationType: string): Promise<void> { await http.post(`/admin/english/listening/items/${id}/reading-pairs`, { readingArticleId, relationType }) }
export async function removeReadingPair(id: number, readingArticleId: number): Promise<void> { await http.delete(`/admin/english/listening/items/${id}/reading-pairs/${readingArticleId}`) }

export async function fetchPublicListenings(params: Record<string, string | number | undefined>): Promise<ListeningPage> {
  return (await http.get<ListeningPage>('/public/english/listening/items', { params })).data
}
export async function fetchPublicListeningHome(): Promise<ListeningHome> { return (await http.get('/public/english/listening')).data }
export async function fetchPublicListening(slug: string): Promise<ListeningItem> { return (await http.get<ListeningItem>(`/public/english/listening/items/${slug}`)).data }
export async function fetchPublicListeningExercises(slug: string): Promise<ListeningExercisePublic[]> { return (await http.get<ListeningExercisePublic[]>(`/public/english/listening/items/${slug}/exercises`)).data }
export interface ListeningCheckItem { exerciseId: number; correct: boolean; earned: number; scoreValue: number; explanationMarkdown: string | null }
export async function checkListeningAnswers(slug: string, answers: { exerciseId: number; answer: unknown }[]): Promise<{ score: number; total: number; items: ListeningCheckItem[] }> {
  return (await http.post(`/public/english/listening/items/${slug}/check`, { answers })).data
}

export async function fetchPronunciationRules(): Promise<PronunciationRule[]> { return (await http.get<PronunciationRule[]>('/admin/english/listening/pronunciation')).data }
export async function createPronunciationRule(payload: Record<string, unknown>): Promise<PronunciationRule> { return (await http.post('/admin/english/listening/pronunciation', payload)).data }
export async function updatePronunciationRule(id: number, payload: Record<string, unknown>): Promise<PronunciationRule | ContentReview> { return (await http.put(`/admin/english/listening/pronunciation/${id}`, payload)).data }
export async function deletePronunciationRule(id: number): Promise<void> { await http.delete(`/admin/english/listening/pronunciation/${id}`) }
export async function publishPronunciationRule(id: number): Promise<PronunciationRule> { return (await http.post(`/admin/english/listening/pronunciation/${id}/publish`)).data }
export async function withdrawPronunciationRule(id: number): Promise<PronunciationRule> { return (await http.post(`/admin/english/listening/pronunciation/${id}/withdraw`)).data }
export async function fetchPublicPronunciationRules(): Promise<PronunciationRule[]> { return (await http.get<PronunciationRule[]>('/public/english/listening/pronunciation')).data }
export async function fetchPublicPronunciationRule(slug: string): Promise<PronunciationRule> { return (await http.get<PronunciationRule>(`/public/english/listening/pronunciation/${slug}`)).data }

export interface ListeningExercisePayload { questionType: string; promptMarkdown: string; configJson: string; explanationMarkdown?: string | null; scoreValue: number; publishStatus?: PublishStatus }
export async function fetchListeningExercises(itemId: number): Promise<import('@/api/reading').ReadingExercise[]> {
  return (await http.get<import('@/api/reading').ReadingExercise[]>(`/admin/english/listening/items/${itemId}/exercises`)).data
}
export async function createListeningExercise(itemId: number, payload: ListeningExercisePayload): Promise<import('@/api/reading').ReadingExercise> {
  return (await http.post<import('@/api/reading').ReadingExercise>(`/admin/english/listening/items/${itemId}/exercises`, payload)).data
}
export async function updateListeningExercise(itemId: number, exerciseId: number, payload: ListeningExercisePayload): Promise<import('@/api/reading').ReadingExercise> {
  return (await http.put<import('@/api/reading').ReadingExercise>(`/admin/english/listening/items/${itemId}/exercises/${exerciseId}`, payload)).data
}
export async function deleteListeningExercise(itemId: number, exerciseId: number): Promise<void> {
  await http.delete(`/admin/english/listening/items/${itemId}/exercises/${exerciseId}`)
}
export async function moveListeningExercise(itemId: number, exerciseId: number, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/listening/items/${itemId}/exercises/${exerciseId}/move`, { targetIndex })
}
