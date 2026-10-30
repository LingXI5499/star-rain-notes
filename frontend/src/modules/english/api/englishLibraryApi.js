import { get, post, put } from '../../../shared/http'

export const getLibraryOverview = () => get('/public/english/library/overview')
export const getLibraryTaxonomy = module => get(`/public/english/library/${module}/taxonomy`)
export const getLibraryCounts = module => get(`/public/english/library/${module}/counts`)
export const getLibraryWords = params => get('/public/english/library/vocabulary/words', params)
export const updateLibraryWord = (id, value) => put(`/admin/english/library/vocabulary/${id}`, value)
export const createLibraryWord = value => post('/admin/english/library/vocabulary', value)
