import http, { get, post } from '../../../shared/http'

const path = (id) => `/admin/messages/${encodeURIComponent(id)}`

export const listPublicMessages = (params) => get('/public/messages', params)
export const submitMessage = (payload) => post('/public/messages', payload)
export const listAdminMessages = (params) => get('/admin/messages', params)
export const listPendingMessages = (params) => get('/admin/messages/pending', params)
export const listMessageActions = (id) => get(`${path(id)}/actions`)
export const approveMessage = (id) => post(`${path(id)}/approve`)
export const rejectMessage = (id, reason) => post(`${path(id)}/reject`, { reason })
export const hideMessage = (id) => post(`${path(id)}/hide`)
export const restoreMessage = (id) => post(`${path(id)}/restore`)
export const deleteMessage = async (id) => (await http.delete(path(id))).data.data
