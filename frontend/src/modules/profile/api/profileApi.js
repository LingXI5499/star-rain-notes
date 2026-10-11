import http, { get, patch, post, put } from '../../../shared/http'

const itemPath = (section, id) => `/admin/profile/${section}/${encodeURIComponent(id)}`
const del = async (path) => (await http.delete(path)).data.data

export const getPublicProfile = () => get('/public/profile')
export const getAdminProfile = () => get('/admin/profile')
export const updateProfile = (payload) => patch('/admin/profile', payload)
export const saveExperience = (id, payload) => id ? patch(itemPath('experiences', id), payload) : post('/admin/profile/experiences', payload)
export const removeExperience = (id) => del(itemPath('experiences', id))
export const orderExperiences = (ids) => put('/admin/profile/experiences/order', { ids })
export const saveSkill = (id, payload) => id ? patch(itemPath('skills', id), payload) : post('/admin/profile/skills', payload)
export const removeSkill = (id) => del(itemPath('skills', id))
export const orderSkills = (ids) => put('/admin/profile/skills/order', { ids })
export const saveSocial = (id, payload) => id ? patch(itemPath('social-links', id), payload) : post('/admin/profile/social-links', payload)
export const removeSocial = (id) => del(itemPath('social-links', id))
export const orderSocial = (ids) => put('/admin/profile/social-links/order', { ids })
export const setProfileMedia = (kind, mediaAssetId) => put(`/admin/profile/${kind}`, { mediaAssetId })
export const clearProfileMedia = (kind) => del(`/admin/profile/${kind}`)
export const addFeatured = (payload) => post('/admin/profile/featured', payload)
export const removeFeatured = (id) => del(itemPath('featured', id))
export const orderFeatured = (ids) => put('/admin/profile/featured/order', { ids })
