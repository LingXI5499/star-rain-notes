import type { PublishStatus } from './englishMeta'

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
