/*
 * 游客本机学习进度（IndexedDB）。
 *
 * 键与结构照抄 V1 vocabulary-study-storage，导入账号时的请求体形状才不需要再翻译一次：
 *   库名 star-rain-vocabulary / 版本 1
 *   object store：settings（键值）、memory（keyPath wordId）、review_log（keyPath reviewSessionId）、
 *                 card_preferences（键值）、word_cache（键值）
 *   settings 里的 'vocabulary' 存学习设置，'legacy-v1-migrated' 存一次性迁移标记
 *
 * 未登录时的记忆、复习、显示覆盖、设置全部走这里；登录后由 api 层改走账号接口，
 * 并提供「导入本机进度」把这里的记忆与复习日志合并到账号。
 */

export const DEFAULT_VOCABULARY_SETTINGS = {
  showEnglish: true,
  showChinese: true,
  reviewDirection: 'EN_TO_ZH',
  dailyNewLimit: 20,
  dailyReviewLimit: 200,
}

/* 十阶段固定复习间隔（秒），必须与后端 VocabularyReviewPolicy 完全一致 */
export const REVIEW_INTERVAL_SECONDS = [300, 1800, 43200, 86400, 172800, 345600, 604800, 1296000, 2592000, 5184000]

const DB_NAME = 'star-rain-vocabulary'
const DB_VERSION = 1
const LEGACY_KEY = 'srn-english-learning-v1'
const MIGRATION_KEY = 'legacy-v1-migrated'
const SETTINGS_KEY = 'vocabulary'

function requestValue(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('浏览器学习数据读取失败'))
  })
}

function transactionDone(transaction) {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error ?? new Error('浏览器学习数据保存失败'))
    transaction.onabort = () => reject(transaction.error ?? new Error('浏览器学习数据保存已取消'))
  })
}

class VocabularyStudyStorage {
  constructor() {
    this.databasePromise = null
  }

  available() {
    return typeof indexedDB !== 'undefined'
  }

  database() {
    if (!this.available()) return Promise.reject(new Error('当前浏览器不支持 IndexedDB，学习进度无法保存。'))
    if (this.databasePromise) return this.databasePromise
    this.databasePromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION)
      request.onupgradeneeded = () => {
        const db = request.result
        if (!db.objectStoreNames.contains('settings')) db.createObjectStore('settings')
        if (!db.objectStoreNames.contains('memory')) {
          const store = db.createObjectStore('memory', { keyPath: 'wordId' })
          store.createIndex('nextReviewAt', 'nextReviewAt')
          store.createIndex('learningStatus', 'learningStatus')
        }
        if (!db.objectStoreNames.contains('review_log')) {
          const store = db.createObjectStore('review_log', { keyPath: 'reviewSessionId' })
          store.createIndex('reviewedAt', 'reviewedAt')
          store.createIndex('wordId', 'wordId')
        }
        if (!db.objectStoreNames.contains('card_preferences')) db.createObjectStore('card_preferences')
        if (!db.objectStoreNames.contains('word_cache')) db.createObjectStore('word_cache')
      }
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => {
        this.databasePromise = null
        reject(request.error ?? new Error('无法打开浏览器学习数据库。'))
      }
    })
    return this.databasePromise
  }

  /* 把 V1 更早的 localStorage 记忆一次性搬进结构化存储，只做一次 */
  async initialize() {
    const db = await this.database()
    const marker = await requestValue(db.transaction('settings').objectStore('settings').get(MIGRATION_KEY))
    if (marker) return
    let legacy = null
    try { legacy = JSON.parse(localStorage.getItem(LEGACY_KEY) ?? 'null') } catch { legacy = null }
    const vocabulary = legacy && typeof legacy === 'object' ? legacy.vocabulary : undefined
    const transaction = db.transaction(['settings', 'memory'], 'readwrite')
    if (vocabulary) {
      for (const [rawId, old] of Object.entries(vocabulary)) {
        const wordId = Number(rawId)
        if (!Number.isInteger(wordId) || wordId <= 0 || !old || Number(old.memoryCount) <= 0) continue
        const at = old.lastMemoryAt || new Date().toISOString()
        transaction.objectStore('memory').put({
          wordId,
          memoryCount: Number(old.memoryCount),
          reviewStep: 0,
          reviewCount: 0,
          firstLearnedAt: at,
          lastReviewedAt: at,
          nextReviewAt: new Date().toISOString(),
          learningStatus: 'ACTIVE',
        })
      }
    }
    transaction.objectStore('settings').put(true, MIGRATION_KEY)
    await transactionDone(transaction)
  }

  async settings() {
    if (!this.available()) return { ...DEFAULT_VOCABULARY_SETTINGS }
    await this.initialize()
    const db = await this.database()
    const stored = await requestValue(db.transaction('settings').objectStore('settings').get(SETTINGS_KEY))
    return stored ? { ...stored, reviewDirection: stored.reviewDirection === 'MIXED' ? 'EN_TO_ZH' : stored.reviewDirection } : { ...DEFAULT_VOCABULARY_SETTINGS }
  }

  async saveSettings(settings) {
    if (!settings.showEnglish && !settings.showChinese) throw new Error('英文和中文至少保留一组。')
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('settings', 'readwrite')
    transaction.objectStore('settings').put(settings, SETTINGS_KEY)
    await transactionDone(transaction)
    return settings
  }

  async memory(wordId) {
    await this.initialize()
    const db = await this.database()
    return (await requestValue(db.transaction('memory').objectStore('memory').get(wordId))) ?? null
  }

  async memories() {
    if (!this.available()) return []
    await this.initialize()
    const db = await this.database()
    return requestValue(db.transaction('memory').objectStore('memory').getAll())
  }

  async start(wordId) {
    const existing = await this.memory(wordId)
    const now = new Date().toISOString()
    const value = existing
      ? {
          ...existing,
          learningStatus: 'ACTIVE',
          firstLearnedAt: existing.firstLearnedAt ?? now,
          nextReviewAt: existing.nextReviewAt ?? now,
        }
      : {
          wordId,
          memoryCount: 0,
          reviewStep: 0,
          reviewCount: 0,
          firstLearnedAt: now,
          lastReviewedAt: null,
          nextReviewAt: now,
          learningStatus: 'ACTIVE',
        }
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    transaction.objectStore('memory').put(value)
    await transactionDone(transaction)
    return value
  }

  /* 整主题加入：一次事务写多行，避免几十次事务把 UI 卡住 */
  async startMany(wordIds) {
    await this.initialize()
    const uniqueIds = [...new Set(wordIds.filter((id) => Number.isInteger(id) && id > 0))]
    if (!uniqueIds.length) return 0
    const existingById = new Map((await this.memories()).map((memory) => [memory.wordId, memory]))
    const now = new Date().toISOString()
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    const store = transaction.objectStore('memory')
    for (const wordId of uniqueIds) {
      const existing = existingById.get(wordId)
      store.put(existing
        ? {
            ...existing,
            learningStatus: 'ACTIVE',
            firstLearnedAt: existing.firstLearnedAt ?? now,
            nextReviewAt: existing.nextReviewAt ?? now,
          }
        : {
            wordId,
            memoryCount: 0,
            reviewStep: 0,
            reviewCount: 0,
            firstLearnedAt: now,
            lastReviewedAt: null,
            nextReviewAt: now,
            learningStatus: 'ACTIVE',
          })
    }
    await transactionDone(transaction)
    return uniqueIds.length
  }

  /*
   * 完成一次复习：reviewSessionId 幂等，重放直接返回既有记录；
   * 间隔表与时机判定和后端同一口径，这样导入账号后两边数据一致。
   */
  async complete(wordId, reviewSessionId, direction) {
    await this.initialize()
    const db = await this.database()
    const oldReview = await requestValue(db.transaction('review_log').objectStore('review_log').get(reviewSessionId))
    if (oldReview) {
      if (oldReview.wordId !== wordId) throw new Error('本次复习标识已被另一个单词使用，请重新开始当前卡片。')
      const existingMemory = await this.memory(wordId)
      if (!existingMemory) throw new Error('复习记录存在，但单词进度缺失；请导出数据后重新加入记忆计划。')
      return { memory: existingMemory, review: oldReview, duplicate: true }
    }
    const old = await this.start(wordId)
    const reviewedAt = new Date()
    const reviewNumber = old.reviewCount + 1
    const intervalSeconds = REVIEW_INTERVAL_SECONDS[Math.min(reviewNumber, REVIEW_INTERVAL_SECONDS.length) - 1]
    const scheduledAt = old.nextReviewAt
    const timingStatus = !scheduledAt ? 'NEW'
      : reviewedAt.getTime() < new Date(scheduledAt).getTime() ? 'EARLY'
        : reviewedAt.getTime() > new Date(scheduledAt).getTime() + 86400000 ? 'OVERDUE' : 'ON_TIME'
    const memory = {
      ...old,
      memoryCount: old.memoryCount + 1,
      reviewStep: Math.min(reviewNumber, REVIEW_INTERVAL_SECONDS.length),
      reviewCount: reviewNumber,
      lastReviewedAt: reviewedAt.toISOString(),
      nextReviewAt: new Date(reviewedAt.getTime() + intervalSeconds * 1000).toISOString(),
    }
    const review = {
      reviewSessionId,
      wordId,
      direction,
      reviewNumber,
      scheduledAt,
      reviewedAt: reviewedAt.toISOString(),
      intervalSeconds,
      timingStatus,
    }
    const transaction = db.transaction(['memory', 'review_log'], 'readwrite')
    transaction.objectStore('memory').put(memory)
    transaction.objectStore('review_log').add(review)
    await transactionDone(transaction)
    return { memory, review, duplicate: false }
  }

  async reset(wordId) {
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('memory', 'readwrite')
    transaction.objectStore('memory').delete(wordId)
    await transactionDone(transaction)
  }

  async display(wordId) {
    await this.initialize()
    const db = await this.database()
    return (await requestValue(db.transaction('card_preferences').objectStore('card_preferences').get(wordId)))
      ?? 'FOLLOW_GLOBAL'
  }

  async displays(wordIds) {
    const entries = await Promise.all(wordIds.map(async (id) => [id, await this.display(id)]))
    return Object.fromEntries(entries)
  }

  async saveDisplay(wordId, mode) {
    await this.initialize()
    const db = await this.database()
    const transaction = db.transaction('card_preferences', 'readwrite')
    if (mode === 'FOLLOW_GLOBAL') transaction.objectStore('card_preferences').delete(wordId)
    else transaction.objectStore('card_preferences').put(mode, wordId)
    await transactionDone(transaction)
  }

  async reviews() {
    if (!this.available()) return []
    await this.initialize()
    const db = await this.database()
    const rows = await requestValue(db.transaction('review_log').objectStore('review_log').getAll())
    return rows.sort((a, b) => String(b.reviewedAt).localeCompare(String(a.reviewedAt)))
  }

  async exportJson() {
    return JSON.stringify({
      version: 1,
      exportedAt: new Date().toISOString(),
      settings: await this.settings(),
      memory: await this.memories(),
      reviewLog: await this.reviews(),
    }, null, 2)
  }

  async importJson(raw) {
    const parsed = JSON.parse(raw)
    if (!parsed || !Array.isArray(parsed.memory) || !Array.isArray(parsed.reviewLog)) {
      throw new Error('这不是有效的星雨笔录词汇学习数据。')
    }
    const db = await this.database()
    const transaction = db.transaction(['settings', 'memory', 'review_log'], 'readwrite')
    if (parsed.settings) transaction.objectStore('settings').put(parsed.settings, SETTINGS_KEY)
    for (const item of parsed.memory) if (Number.isInteger(item.wordId)) transaction.objectStore('memory').put(item)
    for (const item of parsed.reviewLog) if (item.reviewSessionId) transaction.objectStore('review_log').put(item)
    await transactionDone(transaction)
  }

  /* 导出成导入接口的形状：{ memory: [...], reviewLog: [...] } */
  async exportForAccountImport() {
    return { memory: await this.memories(), reviewLog: await this.reviews() }
  }
}

export const vocabularyStudyStorage = new VocabularyStudyStorage()

/*
 * 游客本机进度的四格统计，字段与后端 /summary 完全一致。
 * 英语首页与词库页共用：未登录时不去问后端，直接本地算。
 * IndexedDB 不可用时返回全 0，首页不会因为浏览器限制而报错。
 */
export async function countLocalVocabularyProgress() {
  const empty = { completed: 0, inProgress: 0, dueForReview: 0, total: 0 }
  try {
    const [memory, reviews] = await Promise.all([vocabularyStudyStorage.memories(), vocabularyStudyStorage.reviews()])
    const now = Date.now()
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const active = memory.filter((row) => row.learningStatus === 'ACTIVE')
    return {
      completed: active.filter((row) => Number(row.reviewStep) >= REVIEW_INTERVAL_SECONDS.length).length,
      inProgress: active.filter((row) => Number(row.reviewStep) < REVIEW_INTERVAL_SECONDS.length).length,
      dueForReview: active.filter((row) => row.nextReviewAt && new Date(row.nextReviewAt).getTime() <= now).length,
      total: memory.length,
      completedToday: reviews.filter((row) => new Date(row.reviewedAt).getTime() >= today.getTime()).length,
      totalReviews: reviews.length,
    }
  } catch {
    return empty
  }
}
