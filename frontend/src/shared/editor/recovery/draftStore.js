const DATABASE = 'star-rain-editor-drafts-v1',
  STORE = 'fields'
let opening
const flushers = new Set()
const blockedAccounts = new Set()
export const DRAFT_LIMITS = { points: 3, fields: 50 }
export function allowDraftWrites(account, allowed = true) {
  if (allowed) blockedAccounts.delete(String(account))
  else blockedAccounts.add(String(account))
}
export function registerDraftFlusher(flush) {
  flushers.add(flush)
  return () => flushers.delete(flush)
}
export async function flushDrafts() {
  await Promise.allSettled([...flushers].map((flush) => flush()))
}
export function draftKey(context) {
  if (context?.accountId == null || !context.module || context.documentId == null || !context.field)
    return null
  return JSON.stringify([
    String(context.accountId),
    context.module,
    String(context.documentId),
    context.field,
  ])
}
function open() {
  if (!opening)
    opening = new Promise((resolve, reject) => {
      if (!globalThis.indexedDB) {
        reject(new Error('IndexedDB unavailable'))
        return
      }
      const request = indexedDB.open(DATABASE, 1)
      request.onupgradeneeded = () =>
        request.result
          .createObjectStore(STORE, { keyPath: 'key' })
          .createIndex('account', 'account')
      request.onsuccess = () => {
        const database = request.result
        database.onversionchange = () => {
          database.close()
          opening = null
        }
        resolve(database)
      }
      request.onerror = () => {
        opening = null
        reject(request.error)
      }
      request.onblocked = () => {
        opening = null
        reject(new Error('Local database blocked'))
      }
    })
  return opening
}
const requestValue = (request) =>
  new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
async function transaction(mode, operation) {
  const database = await open(),
    tx = database.transaction(STORE, mode),
    store = tx.objectStore(STORE)
  const finished = new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
    tx.onabort = () => reject(tx.error || new Error('Draft transaction aborted'))
  })
  try {
    const result = await operation(store)
    await finished
    return result
  } catch (error) {
    try {
      tx.abort()
    } catch {}
    await finished.catch(() => {})
    throw error
  }
}
export const getDraft = (context) => {
  const key = draftKey(context)
  return key
    ? transaction('readonly', (store) => requestValue(store.get(key)))
    : Promise.resolve(null)
}
export const accountDrafts = (account) =>
  transaction('readonly', (store) => requestValue(store.index('account').getAll(String(account))))
export async function writeDraft(context, source, now = Date.now()) {
  const key = draftKey(context)
  if (!key) return
  if (blockedAccounts.has(String(context.accountId))) return
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(source))
  const hash = [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, '0'))
    .join('')
  return transaction('readwrite', async (store) => {
    const account = String(context.accountId),
      old = await requestValue(store.get(key))
    if (blockedAccounts.has(account)) return
    if (!old) {
      const fields = await requestValue(store.index('account').getAll(account))
      if (fields.length >= DRAFT_LIMITS.fields)
        throw new Error('已有 50 个字段的本地草稿，请先确认并清理旧草稿；当前输入仍可手动保存。')
    }
    const point = {
      source,
      baseline: context.baseline ?? '',
      serverVersion: context.serverVersion ?? null,
      time: now,
      hash,
      hashAlgorithm: 'SHA-256',
    }
    const points = old?.points?.filter((item) => item.source !== source) || []
    points.unshift(point)
    store.put({
      key,
      account,
      module: context.module,
      documentId: String(context.documentId),
      field: context.field,
      schemaVersion: 1,
      updated: now,
      points: points.slice(0, DRAFT_LIMITS.points),
    })
  })
}
export function removeDraft(context) {
  const key = draftKey(context)
  return key ? transaction('readwrite', (store) => store.delete(key)) : Promise.resolve()
}
export async function migrateDraft(previous, next) {
  const from = draftKey(previous),
    to = draftKey(next)
  if (!from || !to || from === to || String(previous.accountId) !== String(next.accountId)) return
  await transaction('readwrite', async (store) => {
    const record = await requestValue(store.get(from))
    if (!record) return
    const existing = await requestValue(store.get(to))
    const points = [...(record.points || []), ...(existing?.points || [])]
      .sort((a, b) => b.time - a.time)
      .filter(
        (point, index, all) => all.findIndex((item) => item.source === point.source) === index,
      )
      .slice(0, 3)
    store.put({ ...record, key: to, documentId: String(next.documentId), points })
    store.delete(from)
  })
}
export async function clearAccountDrafts(account) {
  await transaction('readwrite', async (store) => {
    const records = await requestValue(store.index('account').getAll(String(account)))
    records.forEach((record) => store.delete(record.key))
  })
}
