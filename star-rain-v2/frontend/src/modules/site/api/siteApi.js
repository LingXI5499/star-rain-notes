import { del, get, patch, put } from '../../../shared/http'

export const getPublicSiteConfig = () => get('/public/site/config')
export const getPublicSiteHome = () => get('/public/site/home')
export const getAdminSiteDashboard = () => get('/admin/site/dashboard')
export const listHomeSections = () => get('/admin/site/home-sections')
export const orderHomeSections = (sectionCodes) => put('/admin/site/home-sections/order', { sectionCodes })
export const patchHomeSection = (sectionCode, changes) => patch(`/admin/site/home-sections/${sectionCode}`, changes)
export const patchSiteConfig = (changes) => patch('/admin/site/config', changes)
export const setSiteMedia = (kind, mediaAssetId) => put(`/admin/site/${kind}`, { mediaAssetId })
export const clearSiteMedia = (kind) => del(`/admin/site/${kind}`)
