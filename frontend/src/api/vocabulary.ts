import { http } from './http'
import { useAuthStore } from '@/stores/auth'
import { guestLearning } from '@/lib/learning-storage'

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
  translation: string
  inflections: string | null
  examples: VocabularyExample[]
  memoryCount: number
  lastMemoryAt: string | null
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
  inflections?: string | null
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
  for (const id of guestLearning.rememberedWordIds()) {
    const m = guestLearning.readVocabularyMemory(id)
    if (m) out[id] = { count: m.memoryCount, at: m.lastMemoryAt }
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

/** Compact display for the memory time (site timezone ISO string → date). */
export function formatMemoryTime(iso: string | null): string {
  if (!iso) return ''
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(iso)
  return match ? match[1] : iso
}
