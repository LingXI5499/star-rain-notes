import http from '../../../shared/http'

export async function search(q, type, page = 1, pageSize = 20, signal) {
  const response = await http.get('/public/search', { params: { q, type: type || undefined, page, pageSize }, signal })
  return response.data.data
}

export async function quickSearch(q, signal) {
  const response = await http.get('/public/search/quick', { params: { q, limit: 8 }, signal })
  return response.data.data
}

export async function suggestions(q, signal) {
  const response = await http.get('/public/search/suggestions', { params: { q, limit: 8 }, signal })
  return response.data.data
}
