import type { PublishStatus } from './englishMeta'
import { http } from './http'

/**
 * Learning-record types (方案 §9.7). The full learning-record slice lands in a
 * later phase; these types define the shape the shared frontend cards consume
 * so the view layer never depends on DB column names.
 */
export interface LearningRecord {
  id: number
  contentRef: string
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'
  score: number | null
  timeSpentSeconds: number | null
  attemptCount: number
  weakPoints: number[]
  mastery: number | null
  nextReviewAt: string | null
  updatedAt: string
}

export interface LearningSummary {
  total: number
  inProgress: number
  completed: number
  dueForReview: number
  completedByType: Record<string, number>
  recent: LearningRecord[]
}

export interface WritingSubmission {
  id: number
  promptId: number
  bodyText: string
  wordCount: number
  status: 'DRAFT' | 'SUBMITTED'
  selfScore: number | null
  submittedAt: string | null
  updatedAt: string
}

const LEARNER_KEY = 'srn-english-learner-key-v1'

export function learnerKey(): string {
  let key = localStorage.getItem(LEARNER_KEY)
  if (!key) {
    key = typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`
    localStorage.setItem(LEARNER_KEY, key)
  }
  return key
}

function learnerHeaders() {
  return { 'X-Learner-Key': learnerKey() }
}

export async function fetchLearningSummary(): Promise<LearningSummary> {
  return (await http.get<LearningSummary>('/public/english/learning/summary', { headers: learnerHeaders() })).data
}

export async function fetchLearningRecord(contentType: string, contentId: number): Promise<LearningRecord | null> {
  return (await http.get<LearningRecord | null>(`/public/english/learning/records/${contentType}/${contentId}`, { headers: learnerHeaders() })).data || null
}

export async function saveLearningRecord(contentType: string, contentId: number, payload: {
  status: LearningStatus
  score?: number | null
  timeSpentSeconds?: number
  weakPoints?: string[]
  mastery?: number | null
}): Promise<LearningRecord> {
  return (await http.put<LearningRecord>(`/public/english/learning/records/${contentType}/${contentId}`, payload, { headers: learnerHeaders() })).data
}

export async function fetchWritingSubmission(promptId: number): Promise<WritingSubmission | null> {
  return (await http.get<WritingSubmission | null>(`/public/english/learning/writing-submissions/${promptId}`, { headers: learnerHeaders() })).data || null
}

export async function saveWritingSubmission(promptId: number, bodyText: string, status: 'DRAFT' | 'SUBMITTED', selfScore?: number | null): Promise<WritingSubmission> {
  return (await http.put<WritingSubmission>(`/public/english/learning/writing-submissions/${promptId}`, { bodyText, status, selfScore }, { headers: learnerHeaders() })).data
}

export interface WritingSubmissionDraft {
  promptId: number
  content: string
  updatedAt: string
}

export const DRAFT_STORAGE_KEY = 'srn-english-writing-draft-v1'

export function readLocalDraft(): WritingSubmissionDraft | null {
  try {
    const raw = localStorage.getItem(DRAFT_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<WritingSubmissionDraft>
    if (typeof parsed.promptId !== 'number' || typeof parsed.content !== 'string') return null
    return parsed as WritingSubmissionDraft
  } catch {
    return null
  }
}

export function writeLocalDraft(draft: WritingSubmissionDraft): void {
  try {
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft))
  } catch {
    // localStorage unavailable (private mode / quota) — ignore silently
  }
}

export function clearLocalDraft(): void {
  try {
    localStorage.removeItem(DRAFT_STORAGE_KEY)
  } catch {
    // ignore
  }
}

export type LearningStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

export function learningStatusMeta(status: LearningStatus): { label: string; tone: 'muted' | 'accent' | 'primary' } {
  switch (status) {
    case 'COMPLETED':
      return { label: '已完成', tone: 'primary' }
    case 'IN_PROGRESS':
      return { label: '学习中', tone: 'accent' }
    default:
      return { label: '未开始', tone: 'muted' }
  }
}

export type { PublishStatus }
