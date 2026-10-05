import { del, get, post, put } from '../../../shared/http'
import { useAuthStore } from '../../account/stores/authStore'
import {
  countLocalVocabularyProgress,
  vocabularyStudyStorage,
} from '../lib/vocabularyStudyStorage'
import { globalDisplayMode, stableReviewDirection } from '../lib/vocabularyDisplay'

/*
 * 英语模块 API。
 *
 * 词汇记忆体系按登录状态分流，分流点在 api 层（页面组件不判登录态）：
 *   游客   —— 记忆/复习/显示/设置都写浏览器 IndexedDB（见 lib/vocabularyStudyStorage）
 *   已登录 —— 走 /account/english/vocabulary，服务端按 account_id 隔离
 * 分流判断不能只依赖 Pinia：路由设计上**公开树刻意不初始化会话**，因此在 /english/** 下
 * authStore 永远是空的。这里的做法是先看 authStore 是否已初始化，未初始化时用一次
 * 「读账号设置」探测会话——401 即游客。探测结果缓存，并在 account-session-expired 时失效。
 */

let accountProbe = null

if (typeof window !== 'undefined') {
  window.addEventListener('account-session-expired', () => { accountProbe = null })
}

/*
 * 会话探测：authStore 已初始化就直接用它；否则用一次「读账号设置」判断。
 * 这里必须直接调 http helper，不能复用 getVocabularyStudySettings——
 * 那个函数本身要问 resolveVocabularyAccount，复用会形成无限递归。
 */
export async function resolveVocabularyAccount() {
  const auth = useAuthStore()
  if (auth.initialized) return Boolean(auth.currentUser)
  if (accountProbe !== null) return accountProbe
  try {
    await get('/account/english/vocabulary/settings')
    accountProbe = true
  } catch {
    accountProbe = false
  }
  return accountProbe
}

/* 登录/退出后由调用方显式失效一次，避免沿用上一次的探测结果 */
export function resetVocabularyAccountProbe() {
  accountProbe = null
}

/* ---------------- 内容（公开） ---------------- */

export const getEnglishOverview = () => get('/public/english/overview')
export const getAdminEnglishOverview = () => get('/admin/english/overview')
export const updateEnglishOverview = (payload) => put('/admin/english/overview', payload)

export const getVocabularyThemes = () => get('/public/english/vocabulary/themes')
export const getVocabularyWords = (params) => get('/public/english/vocabulary/words', params)
export const getVocabularyWordsByIds = (ids) => ids.length
  ? get('/public/english/vocabulary/words/batch', { ids: ids.join(',') })
  : Promise.resolve([])
export const createVocabularyTheme = (payload) => post('/admin/english/vocabulary/themes', payload)
export const updateVocabularyTheme = (id, payload) => put(`/admin/english/vocabulary/themes/${id}`, payload)
export const deleteVocabularyTheme = (id) => del(`/admin/english/vocabulary/themes/${id}`)
export const createVocabularyWord = (payload) => post('/admin/english/vocabulary/words', payload)
export const updateVocabularyWord = (id, payload) => put(`/admin/english/vocabulary/words/${id}`, payload)
export const deleteVocabularyWord = (id) => del(`/admin/english/vocabulary/words/${id}`)

/* ---------------- 学习设置 ---------------- */

export async function getVocabularyStudySettings() {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.settings()
  return get('/account/english/vocabulary/settings')
}

export async function saveVocabularyStudySettings(settings) {
  if (!settings.showEnglish && !settings.showChinese) throw new Error('英文和中文至少保留一组。')
  if (!(await resolveVocabularyAccount())) {
    await vocabularyStudyStorage.saveSettings(settings)
    return settings
  }
  return put('/account/english/vocabulary/settings', settings)
}

/* ---------------- 记忆状态 ---------------- */

/*
 * 批量状态。缺省项由调用方补 NEW，这里保证「每个请求到的词都有返回值」。
 */
export async function getVocabularyStates(wordIds) {
  if (!wordIds.length) return {}
  if (!(await resolveVocabularyAccount())) {
    const [memories, displays] = await Promise.all([
      vocabularyStudyStorage.memories(),
      vocabularyStudyStorage.displays(wordIds),
    ])
    const wanted = new Set(wordIds)
    const byId = new Map(memories.filter((row) => wanted.has(row.wordId)).map((row) => [row.wordId, row]))
    return Object.fromEntries(wordIds.map((wordId) => [wordId, {
      ...(byId.get(wordId) ?? {
        wordId,
        memoryCount: 0,
        reviewStep: 0,
        reviewCount: 0,
        firstLearnedAt: null,
        lastReviewedAt: null,
        nextReviewAt: null,
        learningStatus: 'NEW',
      }),
      displayMode: displays[wordId],
    }]))
  }
  const rows = await get('/account/english/vocabulary/states', { wordIds: wordIds.join(',') })
  return Object.fromEntries(rows.map((row) => [row.wordId, row]))
}

/*
 * 记忆集合：值为 { count, at }，键是 wordId。
 * 「全部 / 已加入计划」筛选与「学习整主题」都要用它判断哪些词已经在计划里。
 */
export async function getVocabularyMemory() {
  const result = {}
  if (!(await resolveVocabularyAccount())) {
    for (const row of await vocabularyStudyStorage.memories()) {
      if (row.learningStatus === 'ACTIVE') result[row.wordId] = { count: row.memoryCount, at: row.lastReviewedAt ?? '' }
    }
    return result
  }
  for (const row of await get('/account/english/vocabulary/memory')) {
    result[row.wordId] = { count: row.memoryCount, at: row.lastMemoryAt ?? '' }
  }
  return result
}

export async function startVocabularyWord(wordId) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.start(Number(wordId))
  return post(`/account/english/vocabulary/words/${wordId}/start`)
}

/*
 * 整主题加入。登录态走服务端，每词一次请求；并发 6 条，够快又不会把连接池打满。
 * 游客态一次事务写多行。
 */
export async function startVocabularyWords(wordIds, onProgress) {
  const uniqueIds = [...new Set(wordIds.filter((id) => Number.isInteger(Number(id)) && Number(id) > 0))]
  if (!uniqueIds.length) return 0
  if (!(await resolveVocabularyAccount())) {
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

export async function resetVocabularyProgress(wordId) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.reset(Number(wordId))
  return del(`/account/english/vocabulary/words/${wordId}/progress`)
}

export async function setVocabularyDisplay(wordId, displayMode) {
  if (!(await resolveVocabularyAccount())) return vocabularyStudyStorage.saveDisplay(Number(wordId), displayMode)
  if (displayMode === 'FOLLOW_GLOBAL') return del(`/account/english/vocabulary/words/${wordId}/display`)
  return put(`/account/english/vocabulary/words/${wordId}/display`, { displayMode })
}

/* ---------------- 学习 / 复习 ---------------- */

export async function getVocabularyReviewQueue(themeId) {
  if (await resolveVocabularyAccount()) {
    return get('/account/english/vocabulary/review-queue', themeId ? { themeId } : undefined)
  }
  /* 游客态：到期词优先，其次主题新词，逻辑与后端 VocabularyStudyQueryServiceImpl.queue 对齐 */
  const [settings, allMemory] = await Promise.all([
    vocabularyStudyStorage.settings(),
    vocabularyStudyStorage.memories(),
  ])
  const now = Date.now()
  const due = allMemory
    .filter((row) => row.learningStatus === 'ACTIVE' && row.nextReviewAt
      && new Date(row.nextReviewAt).getTime() <= now)
    .sort((a, b) => String(a.nextReviewAt).localeCompare(String(b.nextReviewAt)))
    .slice(0, settings.dailyReviewLimit)
  const ids = due.map((row) => row.wordId)

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const introducedToday = allMemory.filter((row) => row.firstLearnedAt
    && new Date(row.firstLearnedAt).getTime() >= today.getTime()).length
  const remainingNew = Math.max(0, settings.dailyNewLimit - introducedToday)
  const newIds = []
  if (themeId && remainingNew > 0) {
    const existing = new Set(allMemory.map((row) => row.wordId))
    let page = 1
    while (newIds.length < remainingNew && page <= 100) {
      const result = await getVocabularyWords({ themeId: String(themeId), page, size: 50 })
      newIds.push(...result.items.map((item) => Number(item.id))
        .filter((id) => !existing.has(id) && !ids.includes(id))
        .slice(0, remainingNew - newIds.length))
      if (page >= Math.max(1, Math.ceil(result.total / 50))) break
      page += 1
    }
  }

  const orderedIds = [...ids, ...newIds]
  const words = await getVocabularyWordsByIds(orderedIds)
  const wordById = new Map(words.map((word) => [Number(word.id), word]))
  const memoryById = new Map(allMemory.map((row) => [row.wordId, row]))
  const epochDay = Math.floor(now / 86400000)
  const items = orderedIds
    .map((id) => {
      const word = wordById.get(Number(id))
      if (!word) return null
      const memory = memoryById.get(Number(id)) ?? {
        wordId: Number(id),
        memoryCount: 0,
        reviewStep: 0,
        reviewCount: 0,
        firstLearnedAt: null,
        lastReviewedAt: null,
        nextReviewAt: null,
        learningStatus: 'NEW',
      }
      return {
        word,
        memory,
        direction: stableReviewDirection(Number(id), settings.reviewDirection, epochDay),
        newWord: memory.learningStatus !== 'ACTIVE',
      }
    })
    .filter(Boolean)
  return { items, dueCount: due.length, newCount: newIds.length, generatedAt: new Date().toISOString() }
}

export async function completeVocabularyReview(wordId, reviewSessionId, direction) {
  if (!(await resolveVocabularyAccount())) {
    return vocabularyStudyStorage.complete(Number(wordId), reviewSessionId, direction)
  }
  return post(`/account/english/vocabulary/words/${wordId}/reviews`, { reviewSessionId, direction })
}

export async function getVocabularyProgress() {
  if (await resolveVocabularyAccount()) return get('/account/english/vocabulary/progress')
  const [memory, reviews] = await Promise.all([
    vocabularyStudyStorage.memories(),
    vocabularyStudyStorage.reviews(),
  ])
  const now = Date.now()
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return {
    activeWords: memory.filter((row) => row.learningStatus === 'ACTIVE').length,
    dueWords: memory.filter((row) => row.learningStatus === 'ACTIVE' && row.nextReviewAt
      && new Date(row.nextReviewAt).getTime() <= now).length,
    completedToday: reviews.filter((row) => new Date(row.reviewedAt).getTime() >= today.getTime()).length,
    totalReviews: reviews.length,
    totalMemoryCount: memory.reduce((sum, row) => sum + Number(row.memoryCount ?? 0), 0),
    recentReviews: reviews.slice(0, 100),
  }
}

/*
 * 四格学习统计（英语首页 / 词库页入口卡）。
 * 字段与后端 /account/english/vocabulary/summary 一致：completed / inProgress / dueForReview / total。
 * 未登录时不去问后端，直接读浏览器本地的游客进度。
 */
export async function getVocabularySummary() {
  if (await resolveVocabularyAccount()) return get('/account/english/vocabulary/summary')
  return countLocalVocabularyProgress()
}

/* 登录后把浏览器本地进度合并到账号；只增不减，重复调用安全 */
export async function importLocalVocabularyProgress(payload) {
  if (!(await resolveVocabularyAccount())) throw new Error('登录后才能把本机进度合并到账号。')
  const body = payload ?? await vocabularyStudyStorage.exportForAccountImport()
  return post('/account/english/vocabulary/import-local', body)
}

export { countLocalVocabularyProgress, vocabularyStudyStorage, globalDisplayMode }

/* ---------------- 其它英语方向 ---------------- */

export const getGrammarCurriculum = (admin = false) => get(`${admin ? '/admin' : '/public'}/english/grammar`)
export const getGrammarLesson = (idOrSlug, admin = false) => get(`${admin ? '/admin' : '/public'}/english/grammar/lessons/${idOrSlug}`)
export const updateGrammarCourse = (payload) => put('/admin/english/grammar', payload)
export const setGrammarCoursePublished = (published) => post(`/admin/english/grammar/${published ? 'publish' : 'withdraw'}`)
export const createGrammarSection = (payload) => post('/admin/english/grammar/sections', payload)
export const updateGrammarSection = (id, payload) => put(`/admin/english/grammar/sections/${id}`, payload)
export const deleteGrammarSection = (id) => del(`/admin/english/grammar/sections/${id}`)
export const createGrammarLesson = (payload) => post('/admin/english/grammar/lessons', payload)
export const updateGrammarLesson = (id, payload) => put(`/admin/english/grammar/lessons/${id}`, payload)
export const setGrammarLessonPublished = (id, published) => post(`/admin/english/grammar/lessons/${id}/${published ? 'publish' : 'withdraw'}`)
export const deleteGrammarLesson = (id) => del(`/admin/english/grammar/lessons/${id}`)

export const listEnglishDocuments = (kind, params, admin = false) => get(`${admin ? '/admin' : '/public'}/english/content/${kind}`, params)
export const getEnglishDocument = (kind, idOrSlug, admin = false) => get(`${admin ? '/admin' : '/public'}/english/content/${kind}/${idOrSlug}`)
export const createEnglishDocument = (kind, payload) => post(`/admin/english/content/${kind}`, payload)
export const updateEnglishDocument = (kind, id, payload) => put(`/admin/english/content/${kind}/${id}`, payload)
export const setEnglishDocumentPublished = (kind, id, published) => post(`/admin/english/content/${kind}/${id}/${published ? 'publish' : 'withdraw'}`)
export const deleteEnglishDocument = (kind, id) => del(`/admin/english/content/${kind}/${id}`)

export const getListeningSegments = (idOrSlug, admin = false) => get(`${admin ? '/admin' : '/public'}/english/listening/${idOrSlug}/segments`)
export const createListeningSegment = (id, payload) => post(`/admin/english/listening/${id}/segments`, payload)
export const updateListeningSegment = (id, segmentId, payload) => put(`/admin/english/listening/${id}/segments/${segmentId}`, payload)
export const deleteListeningSegment = (id, segmentId) => del(`/admin/english/listening/${id}/segments/${segmentId}`)
