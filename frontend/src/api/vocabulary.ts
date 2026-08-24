import { http } from './http'

/**
 * Vocabulary API (approved English vocabulary module): public browsing by
 * layer/theme, personal memory +1, and admin word/example management.
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

/** Personal memory +1 from the public card (server-side record). */
export async function incrementMemory(wordId: number): Promise<VocabularyWord> {
  const { data } = await http.post<VocabularyWord>(`/public/vocabulary/words/${wordId}/memory`)
  return data
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
