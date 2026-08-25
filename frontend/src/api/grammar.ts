import { http } from './http'
import type { ContentReview } from './account'

export type PublishStatus = 'DRAFT' | 'PUBLISHED' | 'WITHDRAWN'

export interface GrammarCourse {
  id: number
  title: string
  subtitle: string | null
  summary: string | null
  introduction: string | null
  roadmapMarkdown: string | null
  coverMediaId: number | null
  coverUrl: string | null
  seoTitle: string | null
  seoDescription: string | null
  publishStatus: PublishStatus
  publishedAt: string | null
  updatedAt: string
}

export interface GrammarLessonSummary {
  id: number
  sectionId: number
  title: string
  slug: string
  summary: string | null
  publishStatus: PublishStatus
  sortOrder: number
  updatedAt: string
}

export interface GrammarSection {
  id: number
  title: string
  sortOrder: number
  lessonCount: number
  publishedCount: number
  lessons: GrammarLessonSummary[]
}

export interface GrammarCurriculum { course: GrammarCourse; sections: GrammarSection[] }
export interface GrammarLessonLink { title: string; slug: string }
export interface GrammarLessonDetail extends GrammarLessonSummary {
  sectionTitle: string
  bodyMarkdown: string
  publishedAt: string | null
  previous: GrammarLessonLink | null
  next: GrammarLessonLink | null
}

export interface GrammarCoursePayload {
  title: string; subtitle: string | null; summary: string | null; introduction: string | null
  roadmapMarkdown: string | null; coverMediaId: number | null; seoTitle: string | null; seoDescription: string | null
}
export interface GrammarLessonPayload {
  sectionId: number; title: string; slug: string; summary: string | null; bodyMarkdown: string
}

export async function fetchPublicGrammar(): Promise<GrammarCurriculum> {
  return (await http.get<GrammarCurriculum>('/public/english/grammar')).data
}
export async function fetchPublicGrammarLesson(slug: string): Promise<GrammarLessonDetail> {
  return (await http.get<GrammarLessonDetail>(`/public/english/grammar/lessons/${slug}`)).data
}
export async function fetchGrammarCurriculum(): Promise<GrammarCurriculum> {
  return (await http.get<GrammarCurriculum>('/admin/english/grammar/curriculum')).data
}
export async function updateGrammarCourse(payload: GrammarCoursePayload): Promise<GrammarCourse> {
  return (await http.put<GrammarCourse>('/admin/english/grammar', payload)).data
}
export async function publishGrammarCourse(): Promise<GrammarCourse> {
  return (await http.post<GrammarCourse>('/admin/english/grammar/publish')).data
}
export async function withdrawGrammarCourse(): Promise<GrammarCourse> {
  return (await http.post<GrammarCourse>('/admin/english/grammar/withdraw')).data
}
export async function createGrammarSection(title: string): Promise<GrammarSection> {
  return (await http.post<GrammarSection>('/admin/english/grammar/sections', { title })).data
}
export async function updateGrammarSection(id: number, title: string): Promise<GrammarSection> {
  return (await http.put<GrammarSection>(`/admin/english/grammar/sections/${id}`, { title })).data
}
export async function deleteGrammarSection(id: number): Promise<void> {
  await http.delete(`/admin/english/grammar/sections/${id}`)
}
export async function moveGrammarSection(id: number, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/grammar/sections/${id}/move`, { targetIndex })
}
export async function fetchGrammarLesson(id: number): Promise<GrammarLessonDetail> {
  return (await http.get<GrammarLessonDetail>(`/admin/english/grammar/lessons/${id}`)).data
}
export async function createGrammarLesson(payload: GrammarLessonPayload): Promise<GrammarLessonDetail> {
  return (await http.post<GrammarLessonDetail>('/admin/english/grammar/lessons', payload)).data
}
export async function updateGrammarLesson(id: number, payload: GrammarLessonPayload): Promise<GrammarLessonDetail | ContentReview> {
  return (await http.put<GrammarLessonDetail | ContentReview>(`/admin/english/grammar/lessons/${id}`, payload)).data
}
export async function deleteGrammarLesson(id: number): Promise<void> {
  await http.delete(`/admin/english/grammar/lessons/${id}`)
}
export async function setGrammarLessonPublished(id: number, published: boolean): Promise<GrammarLessonDetail> {
  return (await http.post<GrammarLessonDetail>(`/admin/english/grammar/lessons/${id}/${published ? 'publish' : 'withdraw'}`)).data
}
export async function moveGrammarLesson(id: number, targetIndex: number): Promise<void> {
  await http.post(`/admin/english/grammar/lessons/${id}/move`, { targetIndex })
}
export async function reassignGrammarLesson(id: number, targetSectionId: number): Promise<void> {
  await http.post(`/admin/english/grammar/lessons/${id}/reassign`, { targetSectionId })
}
