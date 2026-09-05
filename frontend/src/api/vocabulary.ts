import { http } from './http'
import { useAuthStore } from '@/stores/auth'
import { guestLearning } from '@/lib/learning-storage'
import {
  vocabularyStudyStorage,
  type LocalVocabularyMemory,
  type VocabularyDisplayMode,
  type VocabularyReviewDirection,
  type VocabularyStudySettings,
} from '@/lib/vocabulary-study-storage'
import { stableReviewDirection } from '@/lib/vocabulary-display'

/**
 * Vocabulary API (approved English vocabulary module): public browsing by
 * layer/theme, personal memory +1, and admin word/example management.
 *
 * 阶段四：个人"记忆次数"按登录状态分流 —— 未登录游客只写浏览器
 * localStorage；登录管理员走 /account/english/vocabulary。词汇词条的全局
 * memory_count 仅供后台内容管理，不再作为游客个人记忆存储。
 */

export interface VocabularyTheme {
  id: number
  name: string
  wordCount: number
}

export interface VocabularyLayer {
  layer: string
  layerOrder: number
  themes: VocabularyTheme[]
}

export interface VocabularyExample {
  sentence: string
  translation?: string | null
}

export interface VocabularyWord {
  id: number
  themeId: number
  partOfSpeech: string
  word: string
  phoneticUs: string | null
  phoneticUk: string | null
  translation: string
  inflections: string | null
  examples: VocabularyExample[]
  memoryCount: number
  lastMemoryAt: string | null
  audios: VocabularyAudio[]
  wordFamilies: Array<{ id: number; headWord: string; slug: string }>
}

export interface VocabularyAudio {
  id: number
  accent: 'UK' | 'US' | string
  mediaAssetId: number
  publicUrl: string
  provider: string
  sourceUrl?: string | null
  licenseNote: string
  primary: boolean
}

export interface VocabularyMemoryState extends LocalVocabularyMemory {
  displayMode?: VocabularyDisplayMode | null
}

export interface VocabularyStudyCard {
  word: VocabularyWord
  memory: VocabularyMemoryState
  direction: Exclude<VocabularyReviewDirection, 'MIXED'>
  newWord: boolean
}

export interface VocabularyQueue {
  items: VocabularyStudyCard[]
  dueCount: number
  newCount: number
  generatedAt: string
}

export interface VocabularyReviewHistory {
  id?: number
  reviewSessionId?: string
  wordId: number
  word?: string
  reviewNumber: number
  direction: string
  scheduledAt: string | null
  reviewedAt: string
  intervalSeconds: number
  timingStatus: string
}

export interface VocabularyProgressSummary {
  activeWords: number
  dueWords: number
  completedToday: number
  totalReviews: number
  totalMemoryCount: number
  recentReviews: VocabularyReviewHistory[]
}

export interface VocabularyPage {
  items: VocabularyWord[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface UpdateVocabularyWordPayload {
  translation: string
  phoneticUs?: string | null
  phoneticUk?: string | null
  inflections?: string | null
}

export function vocabularyUsesAccount(): boolean {
  return isAuthenticated()
}

export async function fetchVocabularySettings(): Promise<VocabularyStudySettings> {
  if (!isAuthenticated()) return vocabularyStudyStorage.settings()
  const { data } = await http.get<VocabularyStudySettings>('/account/english/vocabulary/settings')
  return data
}

export async function saveVocabularySettings(settings: VocabularyStudySettings): Promise<VocabularyStudySettings> {
  if (!settings.showEnglish && !settings.showChinese) throw new Error('英文和中文至少保留一组。')
  if (!isAuthenticated()) {
    await vocabularyStudyStorage.saveSettings(settings)
    return settings
  }
  const { data } = await http.put<VocabularyStudySettings>('/account/english/vocabulary/settings', settings)
  return data
}

export async function fetchVocabularyStates(wordIds: number[]): Promise<Record<number, VocabularyMemoryState>> {
  if (!wordIds.length) return {}
  if (!isAuthenticated()) {
    const memories = await vocabularyStudyStorage.memories()
    const wanted = new Set(wordIds)
    const displays = await vocabularyStudyStorage.displays(wordIds)
    const memoryById = new Map(memories.filter((m) => wanted.has(m.wordId)).map((m) => [m.wordId, m]))
    return Object.fromEntries(wordIds.map((wordId) => [wordId, {
      ...(memoryById.get(wordId) ?? { wordId, memoryCount: 0, reviewStep: 0, reviewCount: 0, firstLearnedAt: null, lastReviewedAt: null, nextReviewAt: null, learningStatus: 'NEW' as const }),
      displayMode: displays[wordId],
    }]))
  }
  const { data } = await http.get<VocabularyMemoryState[]>('/account/english/vocabulary/states', { params: { wordId: wordIds } })
  return Object.fromEntries(data.map((m) => [m.wordId, m]))
}

export async function startVocabularyWord(wordId: number): Promise<VocabularyMemoryState> {
  if (!isAuthenticated()) return vocabularyStudyStorage.start(wordId)
  const { data } = await http.post<VocabularyMemoryState>(`/account/english/vocabulary/words/${wordId}/start`)
  return data
}

export async function startVocabularyWords(
  wordIds: number[],
  onProgress?: (completed: number, total: number) => void,
): Promise<number> {
  const uniqueIds = [...new Set(wordIds.filter((wordId) => Number.isInteger(wordId) && wordId > 0))]
  if (!uniqueIds.length) return 0
  if (!isAuthenticated()) {
    const completed = await vocabularyStudyStorage.startMany(uniqueIds)
    onProgress?.(completed, uniqueIds.length)
    return completed
  }

  let completed = 0
  const pending = [...uniqueIds]
  const workers = Array.from({ length: Math.min(6, pending.length) }, async () => {
    while (pending.length) {
      const wordId = pending.shift()
      if (wordId === undefined) return
      await startVocabularyWord(wordId)
      completed += 1
      onProgress?.(completed, uniqueIds.length)
    }
  })
  await Promise.all(workers)
  return completed
}

export async function setVocabularyDisplay(wordId: number, displayMode: VocabularyDisplayMode): Promise<void> {
  if (!isAuthenticated()) return vocabularyStudyStorage.saveDisplay(wordId, displayMode)
  if (displayMode === 'FOLLOW_GLOBAL') await http.delete(`/account/english/vocabulary/words/${wordId}/display`)
  else await http.put(`/account/english/vocabulary/words/${wordId}/display`, { displayMode })
}

export async function fetchVocabularyWordsByIds(ids: number[]): Promise<VocabularyWord[]> {
  if (!ids.length) return []
  const { data } = await http.get<VocabularyWord[]>('/public/vocabulary/words/batch', { params: { ids: ids.join(',') } })
  return data
}

export async function fetchVocabularyReviewQueue(themeId?: number): Promise<VocabularyQueue> {
  if (isAuthenticated()) {
    const { data } = await http.get<VocabularyQueue>('/account/english/vocabulary/review-queue', { params: { themeId } })
    return data
  }
  const [settings, allMemory] = await Promise.all([vocabularyStudyStorage.settings(), vocabularyStudyStorage.memories()])
  const now = Date.now()
  const due = allMemory.filter((m) => m.learningStatus === 'ACTIVE' && !!m.nextReviewAt && new Date(m.nextReviewAt).getTime() <= now)
    .sort((a, b) => (a.nextReviewAt ?? '').localeCompare(b.nextReviewAt ?? '')).slice(0, settings.dailyReviewLimit)
  const ids = due.map((m) => m.wordId)
  let newIds: number[] = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const introducedToday = allMemory.filter((memory) => memory.firstLearnedAt
    && new Date(memory.firstLearnedAt).getTime() >= today.getTime()).length
  const remainingNewLimit = Math.max(0, settings.dailyNewLimit - introducedToday)
  if (themeId && remainingNewLimit > 0) {
    const existing = new Set(allMemory.map((m) => m.wordId))
    let candidatePage = 1
    while (newIds.length < remainingNewLimit) {
      const result = await fetchThemeWords(themeId, { page: candidatePage, pageSize: 50 })
      newIds.push(...result.items.map((w) => w.id).filter((id) => !existing.has(id)).slice(0, remainingNewLimit - newIds.length))
      if (candidatePage >= result.totalPages) break
      candidatePage += 1
    }
    ids.push(...newIds.filter((id) => !ids.includes(id)))
  }
  const words = await fetchVocabularyWordsByIds(ids)
  const memoryById = new Map(allMemory.map((m) => [m.wordId, m]))
  return {
    items: words.map((word) => ({
      word,
      memory: memoryById.get(word.id) ?? { wordId: word.id, memoryCount: 0, reviewStep: 0, reviewCount: 0, firstLearnedAt: null, lastReviewedAt: null, nextReviewAt: null, learningStatus: 'NEW' },
      direction: stableReviewDirection(word.id, settings.reviewDirection, Math.floor(Date.now() / 86_400_000)),
      newWord: !memoryById.has(word.id),
    })),
    dueCount: due.length, newCount: newIds.length, generatedAt: new Date().toISOString(),
  }
}

export async function completeVocabularyReview(wordId: number, reviewSessionId: string, direction: 'EN_TO_ZH' | 'ZH_TO_EN') {
  if (!isAuthenticated()) return vocabularyStudyStorage.complete(wordId, reviewSessionId, direction)
  const { data } = await http.post(`/account/english/vocabulary/words/${wordId}/reviews`, { reviewSessionId, direction })
  return data
}

export async function resetVocabularyProgress(wordId: number): Promise<void> {
  if (!isAuthenticated()) return vocabularyStudyStorage.reset(wordId)
  await http.delete(`/account/english/vocabulary/words/${wordId}/progress`)
}

export async function fetchVocabularyProgress(): Promise<VocabularyProgressSummary> {
  if (isAuthenticated()) {
    const { data } = await http.get<VocabularyProgressSummary>('/account/english/vocabulary/statistics')
    return data
  }
  const [memory, reviews] = await Promise.all([vocabularyStudyStorage.memories(), vocabularyStudyStorage.reviews()])
  const now = Date.now()
  const today = new Date(); today.setHours(0, 0, 0, 0)
  return {
    activeWords: memory.filter((m) => m.learningStatus === 'ACTIVE').length,
    dueWords: memory.filter((m) => m.learningStatus === 'ACTIVE' && !!m.nextReviewAt && new Date(m.nextReviewAt).getTime() <= now).length,
    completedToday: reviews.filter((r) => new Date(r.reviewedAt).getTime() >= today.getTime()).length,
    totalReviews: reviews.length,
    totalMemoryCount: memory.reduce((sum, m) => sum + m.memoryCount, 0),
    recentReviews: reviews.slice(0, 100),
  }
}

export async function importLocalVocabularyProgress(payload: Record<string, unknown>): Promise<void> {
  if (!isAuthenticated()) throw new Error('登录后才能把本机进度合并到账号。')
  await http.post('/account/english/vocabulary/import-local', payload)
}

export interface VocabularyMemoryEntry {
  count: number
  at: string
}

function isAuthenticated(): boolean {
  return useAuthStore().isAuthenticated
}

export async function fetchVocabularyLayers(): Promise<VocabularyLayer[]> {
  const { data } = await http.get<VocabularyLayer[]>('/public/vocabulary/themes')
  return data
}

export async function fetchThemeWords(
  themeId: number,
  params: { page?: number; pageSize?: number; remembered?: boolean },
  signal?: AbortSignal,
): Promise<VocabularyPage> {
  const { data } = await http.get<VocabularyPage>(`/public/vocabulary/themes/${themeId}/words`, { params, signal })
  return data
}

/** 拉取某主题的全部词条（用于客户端"已记忆"筛选与记忆叠加）。 */
export async function fetchAllThemeWords(themeId: number, signal?: AbortSignal): Promise<VocabularyWord[]> {
  const all: VocabularyWord[] = []
  let page = 1
  const pageSize = 50
  // 安全上限：主题词条数量通常远小于 5000，此处仅供防失控。
  while (page <= 100) {
    const result = await fetchThemeWords(themeId, { page, pageSize }, signal)
    all.push(...result.items)
    if (page >= result.totalPages) break
    page++
  }
  return all
}

/** 当前记忆集合（存量 0 的词条也返回，便于前端还原 lastMemoryAt）。 */
export async function fetchVocabularyMemory(): Promise<Record<number, VocabularyMemoryEntry>> {
  if (isAuthenticated()) {
    const { data } = await http.get<Array<{ word_id: number; memory_count: number; last_memory_at: string | null }>>(
      '/account/english/vocabulary/memory',
    )
    const out: Record<number, VocabularyMemoryEntry> = {}
    for (const row of data) out[row.word_id] = { count: row.memory_count, at: row.last_memory_at ?? '' }
    return out
  }
  const out: Record<number, VocabularyMemoryEntry> = {}
  for (const memory of await vocabularyStudyStorage.memories()) {
    if (memory.learningStatus === 'ACTIVE') out[memory.wordId] = { count: memory.memoryCount, at: memory.lastReviewedAt ?? '' }
  }
  return out
}

/** 个人记忆 +1（游客写本地 / 登录写账号），返回叠加后的词条以供视图刷新。 */
export async function incrementVocabularyMemory(word: VocabularyWord): Promise<VocabularyWord> {
  if (isAuthenticated()) {
    const next = Math.max(0, (word.memoryCount || 0)) + 1
    await http.put(`/account/english/vocabulary/words/${word.id}/memory`, { memoryCount: next })
    return { ...word, memoryCount: next, lastMemoryAt: new Date().toISOString() }
  }
  const next = Math.max(0, (word.memoryCount || 0)) + 1
  guestLearning.setVocabularyMemory(word.id, next)
  return { ...word, memoryCount: next, lastMemoryAt: new Date().toISOString() }
}

/**
 * 记忆叠加：把游客/账号个人记忆写回词条内存（memoryCount/lastMemoryAt），
 * 使其只反映"属于当前浏览器/账号"的记忆次数；全局列不再作为个人记忆来源。
 */
export function overlayVocabularyMemory(words: VocabularyWord[], memory: Record<number, VocabularyMemoryEntry>): VocabularyWord[] {
  return words.map((w) => {
    const m = memory[w.id]
    return m ? { ...w, memoryCount: m.count, lastMemoryAt: m.at || null } : { ...w, memoryCount: 0, lastMemoryAt: null }
  })
}

export async function fetchAdminWords(params: {
  themeId?: number
  q?: string
  page?: number
  pageSize?: number
}): Promise<VocabularyPage> {
  const { data } = await http.get<VocabularyPage>('/admin/vocabulary/words', { params })
  return data
}

export async function updateAdminWord(wordId: number, payload: UpdateVocabularyWordPayload): Promise<VocabularyWord> {
  const { data } = await http.put<VocabularyWord>(`/admin/vocabulary/words/${wordId}`, payload)
  return data
}

export async function addAdminExample(
  wordId: number,
  payload: { sentence: string; translation?: string | null },
): Promise<VocabularyWord> {
  const { data } = await http.post<VocabularyWord>(`/admin/vocabulary/words/${wordId}/examples`, payload)
  return data
}

export async function removeAdminExample(wordId: number, index: number): Promise<VocabularyWord> {
  const { data } = await http.delete<VocabularyWord>(`/admin/vocabulary/words/${wordId}/examples/${index}`)
  return data
}

export async function setAdminMemory(wordId: number, memoryCount: number): Promise<VocabularyWord> {
  const { data } = await http.put<VocabularyWord>(`/admin/vocabulary/words/${wordId}/memory`, { memoryCount })
  return data
}

export async function addAdminWordAudio(wordId: number, payload: {
  accent: 'UK' | 'US'; mediaAssetId: number; provider: 'UPLOADED'; licenseNote: string; primary: boolean
}): Promise<VocabularyAudio> {
  const { data } = await http.post<VocabularyAudio>(`/admin/vocabulary/words/${wordId}/audio`, payload)
  return data
}

export async function deleteAdminWordAudio(wordId: number, audioId: number): Promise<void> {
  await http.delete(`/admin/vocabulary/words/${wordId}/audio/${audioId}`)
}

export async function setAdminWordPrimaryAudio(wordId: number, audioId: number): Promise<VocabularyAudio> {
  const { data } = await http.put<VocabularyAudio>(`/admin/vocabulary/words/${wordId}/audio/${audioId}/primary`)
  return data
}

/** Compact display for the memory time (site timezone ISO string → date). */
export function formatMemoryTime(iso: string | null): string {
  if (!iso) return ''
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(iso)
  return match ? match[1] : iso
}
