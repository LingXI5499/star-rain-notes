import 'fake-indexeddb/auto'
import { beforeEach, describe, it, expect } from 'vitest'
import {
  getDraft,
  writeDraft,
  draftKey,
  accountDrafts,
  migrateDraft,
  clearAccountDrafts,
  allowDraftWrites,
  draftGeneration,
} from './draftStore'
const owner = {
  accountId: 1,
  module: 'blog',
  documentId: '12',
  field: 'bodyMarkdown',
  serverVersion: 3,
  baseline: 'server',
}
beforeEach(async () => {
  allowDraftWrites(1)
  allowDraftWrites(2)
  await clearAccountDrafts(1)
  await clearAccountDrafts(2)
})
describe('real IndexedDB transaction recovery', () => {
  it('keeps three distinct recovery points and their accepted versions', async () => {
    for (let i = 0; i < 5; i++) await writeDraft(owner, `draft${i}`, i)
    const record = await getDraft(owner)
    expect(record.points.map((point) => point.source)).toEqual(['draft4', 'draft3', 'draft2'])
    expect(record.points[0].serverVersion).toBe(3)
  })
  it('isolates accounts, documents and English/Chinese fields', async () => {
    await writeDraft(owner, 'private')
    expect(await getDraft({ ...owner, accountId: 2 })).toBeUndefined()
    expect(await getDraft({ ...owner, field: 'translationZhMarkdown' })).toBeUndefined()
    expect(await getDraft({ ...owner, documentId: '13' })).toBeUndefined()
    expect(draftKey({ module: 'blog' })).toBeNull()
  })
  it('atomically migrates a temporary draft and preserves newer text', async () => {
    const temporary = { ...owner, documentId: 'new:uuid' }
    await writeDraft(temporary, 'newer', 10)
    await writeDraft(owner, 'older', 5)
    await migrateDraft(temporary, owner)
    expect(await getDraft(temporary)).toBeUndefined()
    expect((await getDraft(owner)).points.map((point) => point.source)).toEqual(['newer', 'older'])
  })
  it('never evicts the only dirty copy to add a fifty-first field', async () => {
    for (let i = 0; i < 50; i++) await writeDraft({ ...owner, documentId: String(i) }, `only${i}`)
    await expect(writeDraft({ ...owner, documentId: '51' }, 'unsaved')).rejects.toThrow('50')
    expect(await accountDrafts(1)).toHaveLength(50)
    expect((await getDraft({ ...owner, documentId: '0' })).points[0].source).toBe('only0')
  })
  it('account cleanup never removes another account', async () => {
    await writeDraft(owner, 'a')
    await writeDraft({ ...owner, accountId: 2 }, 'b')
    await clearAccountDrafts(1)
    expect(await accountDrafts(1)).toHaveLength(0)
    expect(await accountDrafts(2)).toHaveLength(1)
  })
  it('does not recreate cleared drafts from a late callback during logout', async () => {
    await writeDraft(owner, 'old')
    allowDraftWrites(1, false)
    await clearAccountDrafts(1)
    await writeDraft(owner, 'late')
    expect(await accountDrafts(1)).toHaveLength(0)
    allowDraftWrites(1)
    await writeDraft(owner, 'new session')
    expect(await accountDrafts(1)).toHaveLength(1)
  })
  it('AUTH-28: an old editor generation cannot recreate drafts after the same account logs in again', async () => {
    const old = { ...owner, draftGeneration: draftGeneration(1) }
    allowDraftWrites(1, false); await clearAccountDrafts(1); allowDraftWrites(1)
    await writeDraft(old, 'late from previous session')
    expect(await accountDrafts(1)).toHaveLength(0)
    await writeDraft({ ...owner, draftGeneration: draftGeneration(1) }, 'new session')
    expect(await accountDrafts(1)).toHaveLength(1)
  })
  it('AUTH-28: another tab blocks old writes through the shared browser generation', async () => {
    const old = { ...owner, draftGeneration: draftGeneration(1) }
    localStorage.setItem('star-rain-draft-owner:1', JSON.stringify({ generation: old.draftGeneration + 1, blocked: true }))
    await writeDraft(old, 'stale tab')
    expect(await accountDrafts(1)).toHaveLength(0)
    allowDraftWrites(1)
  })
})
