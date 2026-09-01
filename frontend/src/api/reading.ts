import { http } from './http'
import type { ContentReview } from './account'
import type { PublishStatus, TaxonomyTerm } from './englishMeta'

export interface ReadingTagRef {
  id: number
  name: string
  slug: string
  dimension: string
  role: string
}
export interface ReadingGrammarRef { id: number; title: string; slug: string }

export interface ReadingArticle {
  id: number
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  coverMediaId: number | null
  coverUrl: string | null
  readingLevel: number
  cefrLevel: string
  sourceName: string | null
  sourceUrl: string | null
  copyrightNote: string | null
  wordCount: number
  uniqueWordCount: number
  averageSentenceWords: number
  maxSentenceWords: number
  estimatedMinutes: number
  publishStatus: PublishStatus
  sortOrder: number
  publishedAt: string | null
  updatedAt: string
  tags: ReadingTagRef[]
  grammarLessons: ReadingGrammarRef[]
  previous: { title: string; slug: string } | null
  next: { title: string; slug: string } | null
}

export interface ReadingArticleSummary {
  id: number
  title: string
  slug: string
  summary: string
  coverUrl: string | null
  readingLevel: number
  cefrLevel: string
  wordCount: number
  estimatedMinutes: number
  publishStatus: PublishStatus
  hasExercises: boolean
  updatedAt: string
  tags: ReadingTagRef[]
}

export interface ReadingAdminStats {
  total: number
  published: number
  draft: number
  withdrawn: number
  missingExercise: number
  byCefr: Record<string, number>
}

export interface ReadingPage {
  items: ReadingArticleSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
  stats: ReadingAdminStats | null
}

export interface ReadingArticlePayload {
  title: string
  slug?: string
  summary: string
  bodyMarkdown: string
  coverMediaId?: number | null
  readingLevel: number
  cefrLevel: string
  sourceName?: string | null
  sourceUrl?: string | null
  copyrightNote?: string | null
  sortOrder?: number | null
  topicTagIds?: number[] | null
  genreTagIds?: number[] | null
  abilityTagIds?: number[] | null
  grammarLessonIds?: number[] | null
}

export interface ReadingExercise {
  id: number
  articleId: number
  questionType: string
  promptMarkdown: string
  config: Record<string, unknown>
  explanationMarkdown: string | null
  scoreValue: number
  sortOrder: number
  publishStatus: PublishStatus
  updatedAt: string
}
export interface ReadingExercisePayload {
  questionType: string
  promptMarkdown: string
  configJson: string
  explanationMarkdown?: string | null
  scoreValue: number
  publishStatus?: PublishStatus
}
export interface ReadingExercisePublic {
  id: number
  questionType: string
  promptMarkdown: string
  config: Record<string, unknown>
  scoreValue: number
  sortOrder: number
}
export interface ReadingCheckResult {
  score: number
  total: number
  items: { exerciseId: number; correct: boolean; earned: number; scoreValue: number; explanationMarkdown: string | null }[]
}

export async function fetchReadings(params: Record<string, string | number | undefined>): Promise<ReadingPage> {
  return (await http.get<ReadingPage>('/admin/english/reading/articles', { params })).data
}
export async function fetchReading(id: number): Promise<ReadingArticle> {
  return (await http.get<ReadingArticle>(`/admin/english/reading/articles/${id}`)).data
}
export async function createReading(payload: ReadingArticlePayload): Promise<ReadingArticle> {
  return (await http.post<ReadingArticle>('/admin/english/reading/articles', payload)).data
}
export async function updateReading(id: number, payload: ReadingArticlePayload): Promise<ReadingArticle | ContentReview> {
  return (await http.put<ReadingArticle | ContentReview>(`/admin/english/reading/articles/${id}`, payload)).data
}
export async function deleteReading(id: number): Promise<void> {
  await http.delete(`/admin/english/reading/articles/${id}`)
}
export async function publishReading(id: number): Promise<ReadingArticle> {
  return (await http.post<ReadingArticle>(`/admin/english/reading/articles/${id}/publish`)).data
}
export async function withdrawReading(id: number): Promise<ReadingArticle> {
  return (await http.post<ReadingArticle>(`/admin/english/reading/articles/${id}/withdraw`)).data
}

export async function fetchReadingExercises(articleId: number): Promise<ReadingExercise[]> {
  return (await http.get<ReadingExercise[]>(`/admin/english/reading/articles/${articleId}/exercises`)).data
}
export async function createReadingExercise(articleId: number, payload: ReadingExercisePayload): Promise<ReadingExercise> {
  return (await http.post<ReadingExercise>(`/admin/english/reading/articles/${articleId}/exercises`, payload)).data
}
export async function updateReadingExercise(articleId: number, exerciseId: number, payload: ReadingExercisePayload): Promise<ReadingExercise> {
  return (await http.put<ReadingExercise>(`/admin/english/reading/articles/${articleId}/exercises/${exerciseId}`, payload)).data
}
export async function moveReadingExercise(articleId: number, exerciseId: number, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/reading/articles/${articleId}/exercises/${exerciseId}/move`, { targetIndex })
}
export async function deleteReadingExercise(articleId: number, exerciseId: number): Promise<void> {
  await http.delete(`/admin/english/reading/articles/${articleId}/exercises/${exerciseId}`)
}

export async function fetchPublicReadings(params: Record<string, string | number | undefined>): Promise<ReadingPage> {
  return (await http.get<ReadingPage>('/public/english/reading/articles', { params })).data
}
export async function fetchPublicReadingHome(): Promise<{ total: number; byLevel: Record<number, number>; byCefr: Record<string, number> }> {
  return (await http.get('/public/english/reading')).data
}
export async function fetchPublicReading(slug: string): Promise<ReadingArticle> {
  return (await http.get<ReadingArticle>(`/public/english/reading/articles/${slug}`)).data
}
export async function fetchPublicReadingExercises(slug: string): Promise<ReadingExercisePublic[]> {
  return (await http.get<ReadingExercisePublic[]>(`/public/english/reading/articles/${slug}/exercises`)).data
}
export async function checkReadingAnswers(slug: string, answers: { exerciseId: number; answer: unknown }[]): Promise<ReadingCheckResult> {
  return (await http.post<ReadingCheckResult>(`/public/english/reading/articles/${slug}/check`, { answers })).data
}
