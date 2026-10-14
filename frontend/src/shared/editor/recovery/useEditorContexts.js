import { useAuthStore } from '../../../modules/account/stores/authStore'

/** Called by business owners, with explicit document, field, accepted baseline and version. */
export function useEditorContexts(module) {
  const auth = useAuthStore(),
    newDocuments = new Map(),
    previous = new Map()
  return (documentId, field, serverVersion, baseline = '', scope = '') => {
    const accountId = auth.currentUser?.id,
      slot = `${accountId}:${scope}:${field}`
    let id = documentId
    if (id == null || id === '' || id === 'new') {
      if (!newDocuments.has(slot)) {
        const storageKey = `star-rain-editor-new:${module}:${slot}`
        let uuid
        try {
          uuid = sessionStorage.getItem(storageKey)
        } catch {}
        uuid ||= crypto.randomUUID()
        try {
          sessionStorage.setItem(storageKey, uuid)
        } catch {}
        newDocuments.set(slot, `new:${uuid}`)
      }
      id = newDocuments.get(slot)
    }
    const last = previous.get(slot),
      context = { accountId, module, documentId: String(id), field, serverVersion, baseline }
    if (last?.startsWith('new:') && !String(id).startsWith('new:'))
      context.previousDocumentId = last
    // Keep the migration hint until the owning editor acknowledges it; equal renders are harmless.
    if (!last || !last.startsWith('new:') || String(id).startsWith('new:'))
      previous.set(slot, String(id))
    return context
  }
}
