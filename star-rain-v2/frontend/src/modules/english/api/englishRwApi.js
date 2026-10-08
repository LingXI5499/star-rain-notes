import { get, post, put, del } from '../../../shared/http'
const writing = '/account/english/writing/articles'
const reading = '/admin/english/content/reading'
export const getTaxonomy = (admin = false) => get('/' + (admin ? 'admin' : 'public') + '/english/taxonomy')
export const getTopic = slug => get('/public/english/taxonomy/' + slug)
export const createTerm = value => post('/admin/english/taxonomy', value)
export const updateTerm = (id, value) => put('/admin/english/taxonomy/' + id, value)
export const disableTerm = id => post('/admin/english/taxonomy/' + id + '/disable')
export const listKnowledge = params => get('/public/english/knowledge/articles', params)
export const listMyWriting = params => get(writing, params)
export const createWriting = value => post(writing, value)
export const getWriting = id => get(writing + '/' + id)
export const saveWriting = (id, value) => put(writing + '/' + id, value)
export const completeWriting = (id, rowVersion) => post(writing + '/' + id + '/complete', { rowVersion })
export const deleteWriting = id => del(writing + '/' + id)
export const publishWriting = (id, rowVersion, publish) => post('/admin/english/content/writing-articles/' + id + '/' + (publish ? 'publish' : 'withdraw'), { rowVersion })
export const getEnhancements = (id, kind, admin = false) => get('/' + (admin ? 'admin' : 'public') + '/english/content/reading/' + id + '/' + kind)
export const saveEnhancements = (id, kind, rowVersion, items) => put(reading + '/' + id + '/' + kind, { rowVersion, items })
const versions = (kind, id) => (kind === 'reading' ? reading : writing) + '/' + id + '/revisions'
export const listRevisions = (kind, id, params) => get(versions(kind, id), params)
export const getRevision = (kind, id, no) => get(versions(kind, id) + '/' + no)
export const saveRevision = (kind, id, rowVersion, changeNote) => post(versions(kind, id), { rowVersion, changeNote })
export const restoreRevision = (kind, id, no, rowVersion) => post(versions(kind, id) + '/' + no + '/restore', { rowVersion })
