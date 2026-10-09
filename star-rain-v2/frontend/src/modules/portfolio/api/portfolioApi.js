import http, { get, patch, post, put } from '../../../shared/http'

const workPath = (id) => `/admin/portfolio/works/${encodeURIComponent(id)}`

export const listPublicWorks = (params) => get('/public/portfolio/works', params)
export const getPublicWork = (slug) => get(`/public/portfolio/works/${encodeURIComponent(slug)}`)
export const listAdminWorks = (params) => get('/admin/portfolio/works', params)
export const getAdminWork = (id) => get(workPath(id))
export const createWork = (payload) => post('/admin/portfolio/works', payload)
export const updateWork = (id, payload) => patch(workPath(id), payload)
export const updateWorkBody = (id, bodyMarkdown) => put(`${workPath(id)}/body`, { bodyMarkdown })
export const updateWorkDetail = (id, detail) => put(`${workPath(id)}/detail`, detail)
export const addWorkMedia = (id, payload) => post(`${workPath(id)}/media`, payload)
export const updateWorkMedia = (mediaId, payload) => patch(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`, payload)
export const removeWorkMedia = async (mediaId) => (await http.delete(`/admin/portfolio/media/${encodeURIComponent(mediaId)}`)).data.data
export const orderWorkMedia = (id, ids) => put(`${workPath(id)}/media/order`, { ids })
export const addWorkLink = (id, payload) => post(`${workPath(id)}/links`, payload)
export const updateWorkLink = (linkId, payload) => patch(`/admin/portfolio/links/${encodeURIComponent(linkId)}`, payload)
export const removeWorkLink = async (linkId) => (await http.delete(`/admin/portfolio/links/${encodeURIComponent(linkId)}`)).data.data
export const orderWorkLinks = (id, ids) => put(`${workPath(id)}/links/order`, { ids })
export const publishWork = (id) => post(`${workPath(id)}/publish`)
export const withdrawWork = (id) => post(`${workPath(id)}/withdraw`)
export const restoreWork = (id) => post(`${workPath(id)}/restore`)
export const deleteWork = async (id) => (await http.delete(workPath(id))).data.data

export const getWorkTaxonomy = () => get('/public/portfolio/taxonomy')
export const getWorkTemplates = () => get('/admin/portfolio/templates')
export const createWorkSection = (id, payload) => post(`${workPath(id)}/sections`, payload)
export const updateWorkSection = (id, sectionId, payload) => put(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`, payload)
export const removeWorkSection = async (id, sectionId) => (await http.delete(`${workPath(id)}/sections/${encodeURIComponent(sectionId)}`)).data.data
export const orderWorkSections = (id, ids) => put(`${workPath(id)}/sections/order`, { ids })

export const bindWorkPrototype = (id, payload) => put(`${workPath(id)}/prototype`, payload)
