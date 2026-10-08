import { del, get, patch, post, put, errorMessage } from '../../../shared/http'
import { getVocabularyWords, resolveVocabularyAccount, vocabularyStudyStorage } from './englishApi'

const base = '/account/english/vocabulary'
export const masteryLabels = { BEGINNER: '初窥门径', LEARNER: '登堂入室', SKILLED: '炉火纯青', MASTERED: '出神入化' }
export const modeLabels = { BILINGUAL_PREVIEW: '双语预览', EN_TO_ZH: '英译中', ZH_TO_EN: '中译英', AUDIO_TO_BOTH: '听音辨词' }
export const modes = ['EN_TO_ZH', 'ZH_TO_EN', 'AUDIO_TO_BOTH']
export const modeBit = (mode) => 1 << modes.indexOf(mode)
export const learningMessage = (cause) => cause?.response ? errorMessage(cause) : cause?.message || '读取学习数据失败，请重试。'

async function requireAccount() {
  if (!(await resolveVocabularyAccount())) throw new Error('登录后可创建持久学习计划并跨设备复习；本机历史记录仍保留在学习进度页。')
}
export async function learningWords(filters) {
  if (await resolveVocabularyAccount()) return get(`${base}/learning/words`, filters)
  return getVocabularyWords({ themeId: filters.themeId, search: filters.q, page: filters.page, size: filters.size })
}
export async function learningStates(ids) {
  if (!ids.length) return {}
  if (await resolveVocabularyAccount()) {
    const data = await get(`${base}/learning/states`, { wordIds: ids.join(',') })
    return Object.fromEntries(data.map((item) => [String(item.wordId), item]))
  }
  // Guest histories remain local and unrated. No guest reviews are silently sent to an account.
  const [rows, displays] = await Promise.all([vocabularyStudyStorage.memories(), vocabularyStudyStorage.displays(ids.map(Number))])
  const wanted = new Set(ids.map(String))
  const states = Object.fromEntries(ids.map(id => [String(id), { wordId: String(id), memoryCount: 0, masteryRank: null, modeMemory: [], displayMode: displays[id] }]))
  for (const row of rows) if (wanted.has(String(row.wordId))) states[String(row.wordId)] = { ...states[String(row.wordId)], ...row }
  return states
}
export async function currentPlan() { await requireAccount(); return get(`${base}/plan`) }
export async function previewPlan(selection, page = 1) { await requireAccount(); return post(`${base}/plan/preview?page=${page}&size=24`, selection) }
export async function confirmPlan(selection) { await requireAccount(); return put(`${base}/plan`, selection) }
export async function planItems(revision, groupNo, after = 0, limit = 100) { await requireAccount(); return get(`${base}/plan/items`, { revision, groupNo, after, limit }) }
export async function planGroups(revision, after = 0) { await requireAccount(); return get(`${base}/plan/groups`, { revision, after, limit: 50 }) }
export async function resizePlan(expectedRevision, batchSize) { await requireAccount(); return patch(`${base}/plan/batch-size`, { expectedRevision, batchSize }) }
export async function cancelPlan(expectedRevision) { await requireAccount(); return del(`${base}/plan?expectedRevision=${expectedRevision}`) }
export async function skipMissingWord(wordId, expectedRevision) { await requireAccount(); return post(`${base}/plan/items/${wordId}/skip`, { expectedRevision }) }
export async function reviewSummary() { await requireAccount(); return get(`${base}/review-summary`) }
export async function dueCards(limit = 20, cursor) { await requireAccount(); return get(`${base}/review-queue`, { limit, cursor }) }
export async function rateWord(wordId, payload) { await requireAccount(); return post(`${base}/words/${wordId}/reviews`, payload) }

// The selection is an intent, never a downloaded whole dictionary. It survives pagination and refresh.
export function createSelection(themeId) {
  return { themeId: String(themeId), q: '', learned: 'ANY', mastery: 'ANY', inPlan: 'ANY', direction: 'ANY', lastRating: 'ANY',
    selectAllMatched: false, wordIds: [], excludedWordIds: [], batchSize: 20 }
}
export function selectedWord(selection, id) {
  id = String(id)
  return selection.wordIds.map(String).includes(id) || (selection.selectAllMatched && !selection.excludedWordIds.map(String).includes(id))
}
export function toggleSelected(selection, id) {
  id = String(id)
  const next = { ...selection, wordIds: selection.wordIds.map(String), excludedWordIds: selection.excludedWordIds.map(String) }
  if (selection.selectAllMatched) {
    next.excludedWordIds = selectedWord(selection, id) ? [...new Set([...next.excludedWordIds, id])] : next.excludedWordIds.filter((wordId) => wordId !== id)
    next.wordIds = next.wordIds.filter((wordId) => wordId !== id)
  } else {
    next.wordIds = selectedWord(selection, id) ? next.wordIds.filter((wordId) => wordId !== id) : [...next.wordIds, id]
  }
  delete next.previewFingerprint
  return next
}
export const draftKey = (themeId) => `sr-vocabulary-plan-selection:${themeId}`
export function saveSelection(selection) { sessionStorage.setItem(draftKey(selection.themeId), JSON.stringify(selection)) }
export function loadSelection(themeId) {
  try {
    const selection = JSON.parse(sessionStorage.getItem(draftKey(themeId)) || 'null')
    if (selection?.themeId === String(themeId) && Array.isArray(selection.wordIds) && Array.isArray(selection.excludedWordIds)) return selection
  } catch { /* An incomplete draft is harmless; the server validates the final selection. */ }
  return createSelection(themeId)
}
