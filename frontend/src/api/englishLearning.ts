import type { PublishStatus } from './englishMeta'
import { http } from './http'
import { useAuthStore } from '@/stores/auth'
import { guestLearning } from '@/lib/learning-storage'

/**
 * Learning-record types (方案 §9.7). The full learning-record slice lands in a
 * later phase; these types define the shape the shared frontend cards consume
 * so the view layer never depends on DB column names.
 *
 * 阶段四：进度按登录状态分流 —— 未登录游客只读写浏览器 localStorage；
 * 登录管理员走 /account/english API。游客从不写服务器。
 */
export interface LearningRecord {
  id: number
  contentRef: string
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'
  score: number | null
  timeSpentSeconds: number | null
  attemptCount: number
  weakPoints: string[]
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

export interface LearningActivityDay {
  date: string
  attempts: number
  completed: number
  timeSpentSeconds: number
}

export interface LearningModuleInsight {
  contentType: 'GRAMMAR' | 'READING' | 'LISTENING' | 'WRITING'
  total: number
  completed: number
  averageMastery: number | null
  timeSpentSeconds: number
}

export interface LearningRecommendation {
  contentType: LearningModuleInsight['contentType']
  contentId: number
  slug: string
  title: string
  route: string
  reason: string
  cefrLevel: string | null
  mastery: number | null
  nextReviewAt: string | null
  recommendationType: 'REVIEW' | 'CONTINUE' | 'BUNDLE_NEXT' | 'PAIRED' | 'TAG_MATCH' | 'STARTER'
  priority: number
  sourceTitle: string | null
}

export interface LearningInsights {
  totalTimeSeconds: number
  totalAttempts: number
  activeDays14: number
  currentStreak: number
  averageScore: number | null
  averageMastery: number | null
  activity: LearningActivityDay[]
  modules: LearningModuleInsight[]
  recommendations: LearningRecommendation[]
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

// ---------------------------------------------------------------------------
// auth 分流
// ---------------------------------------------------------------------------

function isAuthenticated(): boolean {
  return useAuthStore().isAuthenticated
}

function normalizeType(type: string): string {
  return (type || '').toUpperCase()
}

function recordKey(type: string, contentId: number): string {
  return `${normalizeType(type)}:${contentId}`
}

function wordCount(text: string): number {
  const v = (text || '').trim()
  return v ? v.split(/\s+/).length : 0
}

function parseWeak(value: unknown): string[] {
  if (Array.isArray(value)) return value.filter((x): x is string => typeof x === 'string')
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.filter((x): x is string => typeof x === 'string')
    } catch {
      // ignore
    }
  }
  return []
}

function normalizeRecord(value: unknown): LearningRecord | null {
  if (!value || typeof value !== 'object') return null
  const v = value as Record<string, unknown>
  if (typeof v.status !== 'string') return null
  return {
    id: Number(v.id) || 0,
    contentRef: String(v.contentRef ?? ''),
    status: v.status as LearningRecord['status'],
    score: v.score == null ? null : Number(v.score),
    timeSpentSeconds: v.timeSpentSeconds == null ? null : Number(v.timeSpentSeconds),
    attemptCount: Number(v.attemptCount) || 0,
    weakPoints: parseWeak(v.weakPoints),
    mastery: v.mastery == null ? null : Number(v.mastery),
    nextReviewAt: v.nextReviewAt == null ? null : String(v.nextReviewAt),
    updatedAt: v.updatedAt == null ? '' : String(v.updatedAt),
  }
}

// ---------------------------------------------------------------------------
// 账号侧（/account/english）辅助：把后端学习记录(DTO)映射到前端 LearningRecord
// ---------------------------------------------------------------------------

function mapSharedRecord(data: Record<string, unknown>): LearningRecord {
  const contentType = normalizeType(String(data.contentType ?? ''))
  const contentId = Number(data.contentId) || 0
  return {
    id: Number(data.id) || contentId,
    contentRef: `${contentType}:${contentId}`,
    status: String(data.status ?? 'NOT_STARTED') as LearningRecord['status'],
    score: data.score == null ? null : Number(data.score),
    timeSpentSeconds: data.timeSpentSeconds == null ? null : Number(data.timeSpentSeconds),
    attemptCount: Number(data.attemptCount) || 0,
    weakPoints: parseWeak(data.weakPoints),
    mastery: data.mastery == null ? null : Number(data.mastery),
    nextReviewAt: data.nextReviewAt == null ? null : String(data.nextReviewAt),
    updatedAt: data.updatedAt == null ? '' : String(data.updatedAt),
  }
}

function mapAccountRecord(data: Record<string, unknown>): LearningRecord {
  const contentType = normalizeType(String(data.content_type ?? ''))
  const contentId = Number(data.content_id) || 0
  return {
    id: Number(data.id) || contentId,
    contentRef: `${contentType}:${contentId}`,
    status: String(data.completion_status ?? 'NOT_STARTED') as LearningRecord['status'],
    score: data.score == null ? null : Number(data.score),
    timeSpentSeconds: data.time_spent_seconds == null ? null : Number(data.time_spent_seconds),
    attemptCount: Number(data.attempts) || 0,
    weakPoints: parseWeak(data.weak_points_json),
    mastery: data.mastery_level == null ? null : Number(data.mastery_level),
    nextReviewAt: data.next_review_at == null ? null : String(data.next_review_at),
    updatedAt: data.updated_at == null ? '' : String(data.updated_at),
  }
}

// ---------------------------------------------------------------------------
// 游客本地聚合（summary / insights），仅统计 localStorage 中的记录
// ---------------------------------------------------------------------------

function localRecords(): LearningRecord[] {
  return Object.values(guestLearning.readAllRecords())
    .map(normalizeRecord)
    .filter((r): r is LearningRecord => !!r)
}

function summarizeLocal(): LearningSummary {
  const list = localRecords()
  let inProgress = 0
  let completed = 0
  let dueForReview = 0
  const completedByType: Record<string, number> = {}
  for (const r of list) {
    if (r.status === 'COMPLETED') completed++
    else if (r.status === 'IN_PROGRESS') inProgress++
    if (r.nextReviewAt && new Date(r.nextReviewAt) <= new Date()) dueForReview++
    if (r.status === 'COMPLETED') {
      const type = r.contentRef.split(':')[0]
      completedByType[type] = (completedByType[type] ?? 0) + 1
    }
  }
  const recent = [...list]
    .sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
    .slice(0, 8)
  return { total: list.length, inProgress, completed, dueForReview, completedByType, recent }
}

function localInsights(): LearningInsights {
  const list = localRecords()
  let totalTimeSeconds = 0
  let totalAttempts = 0
  let scoreSum = 0
  let scoreCount = 0
  let masterySum = 0
  let masteryCount = 0
  const perDay: Record<string, { attempts: number; completed: number; timeSpentSeconds: number }> = {}
  const byType: Record<string, { total: number; completed: number; masterySum: number; masteryCount: number; timeSpentSeconds: number }> = {}
  const now = new Date()
  for (const r of list) {
    totalTimeSeconds += r.timeSpentSeconds ?? 0
    totalAttempts += r.attemptCount || 1
    if (r.score != null) { scoreSum += r.score; scoreCount++ }
    if (r.mastery != null) { masterySum += r.mastery; masteryCount++ }
    if (r.updatedAt) {
      const day = r.updatedAt.slice(0, 10)
      const d = (perDay[day] ??= { attempts: 0, completed: 0, timeSpentSeconds: 0 })
      d.attempts += 1
      if (r.status === 'COMPLETED') d.completed += 1
      d.timeSpentSeconds += r.timeSpentSeconds ?? 0
    }
    const type = r.contentRef.split(':')[0] || 'GRAMMAR'
    const t = (byType[type] ??= { total: 0, completed: 0, masterySum: 0, masteryCount: 0, timeSpentSeconds: 0 })
    t.total += 1
    if (r.status === 'COMPLETED') t.completed += 1
    t.timeSpentSeconds += r.timeSpentSeconds ?? 0
    if (r.mastery != null) { t.masterySum += r.mastery; t.masteryCount++ }
  }

  // 近 14 天（含今天），缺失日补零
  const activity: LearningActivityDay[] = []
  for (let i = 13; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    const iso = d.toISOString().slice(0, 10)
    const v = perDay[iso]
    activity.push({ date: iso, attempts: v?.attempts ?? 0, completed: v?.completed ?? 0, timeSpentSeconds: v?.timeSpentSeconds ?? 0 })
  }
  const activeDays14 = activity.filter((a) => a.attempts > 0).length
  let streak = 0
  for (let i = activity.length - 1; i >= 0; i--) {
    if (activity[i].attempts > 0) streak++
    else if (i === activity.length - 1) continue
    else break
  }

  const modules: LearningModuleInsight[] = (['GRAMMAR', 'READING', 'LISTENING', 'WRITING'] as const).map((type) => {
    const t = byType[type]
    return {
      contentType: type,
      total: t?.total ?? 0,
      completed: t?.completed ?? 0,
      averageMastery: t && t.masteryCount ? t.masterySum / t.masteryCount : null,
      timeSpentSeconds: t?.timeSpentSeconds ?? 0,
    }
  })

  return {
    totalTimeSeconds,
    totalAttempts,
    activeDays14,
    currentStreak: streak,
    averageScore: scoreCount ? scoreSum / scoreCount : null,
    averageMastery: masteryCount ? masterySum / masteryCount : null,
    activity,
    modules,
    recommendations: [],
  }
}

// ---------------------------------------------------------------------------
// 对外函数：按登录状态分流
// ---------------------------------------------------------------------------

export async function fetchLearningSummary(): Promise<LearningSummary> {
  if (isAuthenticated()) {
    return (await http.get<LearningSummary>('/account/english/learning/summary')).data
  }
  return summarizeLocal()
}

export async function fetchLearningInsights(): Promise<LearningInsights> {
  if (isAuthenticated()) {
    return (await http.get<LearningInsights>('/account/english/learning/insights')).data
  }
  return localInsights()
}

export async function fetchLearningRecord(contentType: string, contentId: number): Promise<LearningRecord | null> {
  const type = normalizeType(contentType)
  if (isAuthenticated()) {
    try {
      const { data } = await http.get<Record<string, unknown>>(`/account/english/learning/records/${type}/${contentId}`)
      return mapAccountRecord(data)
    } catch {
      return null
    }
  }
  return normalizeRecord(guestLearning.readRecord(recordKey(type, contentId)))
}

export async function fetchLearningRecords(refs: Array<{ contentType: string; contentId: number }>): Promise<Record<string, LearningRecord>> {
  if (!refs.length) return {}
  if (isAuthenticated()) {
    const params = new URLSearchParams()
    refs.slice(0, 100).forEach((item) => params.append('ref', `${normalizeType(item.contentType)}:${item.contentId}`))
    const { data } = await http.get<Record<string, Record<string, unknown>>>(
      `/account/english/learning/records/batch?${params.toString()}`,
    )
    const result: Record<string, LearningRecord> = {}
    for (const [key, value] of Object.entries(data)) {
      result[key] = mapSharedRecord(value)
    }
    return result
  }
  const result: Record<string, LearningRecord> = {}
  for (const item of refs) {
    const key = recordKey(item.contentType, item.contentId)
    const rec = normalizeRecord(guestLearning.readRecord(key))
    if (rec) result[key] = rec
  }
  return result
}

export async function saveLearningRecord(contentType: string, contentId: number, payload: {
  status: LearningStatus
  score?: number | null
  timeSpentSeconds?: number
  weakPoints?: string[]
  mastery?: number | null
}): Promise<LearningRecord> {
  const type = normalizeType(contentType)
  const key = recordKey(type, contentId)
  const previous = normalizeRecord(guestLearning.readRecord(key))
  const updatedAt = new Date().toISOString()
  if (isAuthenticated()) {
    await http.put(`/account/english/learning/records/${type}/${contentId}`, {
      completionStatus: payload.status,
      timeSpentSeconds: payload.timeSpentSeconds ?? 0,
    })
    // 账号侧写入服务器；前端用本地构造返回给视图渲染
    return {
      id: previous?.id ?? contentId,
      contentRef: key,
      status: payload.status,
      score: payload.score ?? null,
      timeSpentSeconds: payload.timeSpentSeconds ?? null,
      attemptCount: (previous?.attemptCount ?? 0) + 1,
      weakPoints: payload.weakPoints ?? [],
      mastery: payload.mastery ?? null,
      nextReviewAt: null,
      updatedAt,
    }
  }
  const record: LearningRecord = {
    id: previous?.id ?? contentId,
    contentRef: key,
    status: payload.status,
    score: payload.score ?? null,
    timeSpentSeconds: payload.timeSpentSeconds ?? null,
    attemptCount: (previous?.attemptCount ?? 0) + 1,
    weakPoints: payload.weakPoints ?? [],
    mastery: payload.mastery ?? null,
    nextReviewAt: null,
    updatedAt,
  }
  guestLearning.writeRecord(key, record)
  return record
}

export async function fetchWritingSubmission(promptId: number): Promise<WritingSubmission | null> {
  if (isAuthenticated()) {
    try {
      const { data } = await http.get<Record<string, unknown>>(`/account/english/writing-submissions/${promptId}`)
      return {
        id: Number(data.id) || promptId,
        promptId,
        bodyText: String(data.body_text ?? ''),
        wordCount: Number(data.word_count) || 0,
        status: String(data.submission_status ?? 'DRAFT') as WritingSubmission['status'],
        selfScore: data.self_score == null ? null : Number(data.self_score),
        submittedAt: data.submitted_at == null ? null : String(data.submitted_at),
        updatedAt: data.updated_at == null ? '' : String(data.updated_at),
      }
    } catch {
      return null
    }
  }
  const raw = guestLearning.readRecord(`WRITING:${promptId}`)
  if (!raw || typeof raw !== 'object') return null
  const s = raw as Record<string, unknown>
  return {
    id: Number(s.id) || promptId,
    promptId,
    bodyText: String(s.bodyText ?? ''),
    wordCount: Number(s.wordCount) || 0,
    status: String(s.status ?? 'DRAFT') as WritingSubmission['status'],
    selfScore: s.selfScore == null ? null : Number(s.selfScore),
    submittedAt: s.submittedAt == null ? null : String(s.submittedAt),
    updatedAt: String(s.updatedAt ?? ''),
  }
}

export async function saveWritingSubmission(promptId: number, bodyText: string, status: 'DRAFT' | 'SUBMITTED', selfScore?: number | null): Promise<WritingSubmission> {
  const updatedAt = new Date().toISOString()
  if (isAuthenticated()) {
    await http.put(`/account/english/writing-submissions/${promptId}`, {
      bodyText,
      status,
      selfScore: selfScore ?? null,
    })
    return {
      id: promptId,
      promptId,
      bodyText,
      wordCount: wordCount(bodyText),
      status,
      selfScore: selfScore ?? null,
      submittedAt: status === 'SUBMITTED' ? new Date().toISOString() : null,
      updatedAt,
    }
  }
  const submission: WritingSubmission = {
    id: promptId,
    promptId,
    bodyText,
    wordCount: wordCount(bodyText),
    status,
    selfScore: selfScore ?? null,
    submittedAt: status === 'SUBMITTED' ? new Date().toISOString() : null,
    updatedAt,
  }
  guestLearning.writeRecord(`WRITING:${promptId}`, submission)
  return submission
}

// ---------------------------------------------------------------------------
// 草稿本地存储（跨会话保留，与账号/游客无关）
// ---------------------------------------------------------------------------

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
