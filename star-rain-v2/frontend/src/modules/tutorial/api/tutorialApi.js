import http, { get, patch, post, put } from '../../../shared/http'

const encoded = (value) => encodeURIComponent(value)
const del = async (path) => (await http.delete(path)).data.data

// V1 的“知识体系”是教程目录入口，编号由服务端派生，表单只传名称。
export const listAdminCategories = () => get('/admin/tutorial-categories')
export const createCategory = (name) => post('/admin/tutorial-categories', { name })
export const updateCategory = (id, name) => patch(`/admin/tutorial-categories/${encoded(id)}`, { name })
export const deleteCategory = (id) => del(`/admin/tutorial-categories/${encoded(id)}`)
export const reorderCategories = (ids) => put('/admin/tutorial-categories/order', { ids })

export const listAdminTutorials = (params) => get('/admin/tutorials', params)
export const getAdminTutorial = (id) => get(`/admin/tutorials/${encoded(id)}`)
export const previewTutorial = (id) => get(`/admin/tutorials/${encoded(id)}/preview`)
export const previewChapter = (id) => get(`/admin/tutorial-chapters/${encoded(id)}/preview`)
export const createTutorial = (payload) => post('/admin/tutorials', payload)
export const updateTutorial = (id, payload) => patch(`/admin/tutorials/${encoded(id)}`, payload)
export const deleteTutorial = (id) => del(`/admin/tutorials/${encoded(id)}`)
export const reorderTutorials = (categoryId, ids) =>
  put(`/admin/tutorial-categories/${encoded(categoryId)}/tutorials/order`, { ids })
export const submitTutorialReview = (id) => post(`/admin/tutorials/${encoded(id)}/submit-review`)
export const publishTutorial = (id) => post(`/admin/tutorials/${encoded(id)}/publish`)
export const withdrawTutorial = (id) => post(`/admin/tutorials/${encoded(id)}/withdraw`)
export const restoreTutorial = (id) => post(`/admin/tutorials/${encoded(id)}/restore-publication`)

export const getAdminCurriculum = (id) => get(`/admin/tutorials/${encoded(id)}/curriculum`)
export const createGroup = (tutorialId, title) =>
  post(`/admin/tutorials/${encoded(tutorialId)}/groups`, { title })
export const updateGroup = (groupId, title) => patch(`/admin/tutorial-groups/${encoded(groupId)}`, { title })
export const archiveGroup = (groupId) => post(`/admin/tutorial-groups/${encoded(groupId)}/archive`)
export const restoreGroup = (groupId) => post(`/admin/tutorial-groups/${encoded(groupId)}/restore`)
export const reorderGroups = (tutorialId, ids) => put(`/admin/tutorials/${encoded(tutorialId)}/groups/order`, { ids })
export const createChapter = (groupId, payload) =>
  post(`/admin/tutorial-groups/${encoded(groupId)}/chapters`, payload)
export const getAdminChapter = (chapterId) => get(`/admin/tutorial-chapters/${encoded(chapterId)}`)
export const updateChapter = (chapterId, payload) => patch(`/admin/tutorial-chapters/${encoded(chapterId)}`, payload)
export const updateChapterBody = (chapterId, bodyMarkdown) =>
  put(`/admin/tutorial-chapters/${encoded(chapterId)}/body`, { bodyMarkdown })
export const archiveChapter = (chapterId) => post(`/admin/tutorial-chapters/${encoded(chapterId)}/archive`)
export const restoreChapter = (chapterId) => post(`/admin/tutorial-chapters/${encoded(chapterId)}/restore`)
export const moveChapter = (chapterId, groupId) => post(`/admin/tutorial-chapters/${encoded(chapterId)}/move`, { groupId })
export const reorderChapters = (groupId, ids) =>
  put(`/admin/tutorial-groups/${encoded(groupId)}/chapters/order`, { ids })

export const listCards = (chapterId) => get(`/admin/tutorial-chapters/${encoded(chapterId)}/cards`)
export const createCard = (chapterId, payload) => post(`/admin/tutorial-chapters/${encoded(chapterId)}/cards`, payload)
export const updateCard = (id, payload) => patch(`/admin/tutorial-cards/${encoded(id)}`, payload)
export const deleteCard = (id) => del(`/admin/tutorial-cards/${encoded(id)}`)
export const reorderCards = (chapterId, ids) => put(`/admin/tutorial-chapters/${encoded(chapterId)}/cards/order`, { ids })
export const listQuestions = (chapterId) => get(`/admin/tutorial-chapters/${encoded(chapterId)}/questions`)
export const createQuestion = (chapterId, payload) => post(`/admin/tutorial-chapters/${encoded(chapterId)}/questions`, payload)
export const updateQuestion = (id, payload) => patch(`/admin/tutorial-questions/${encoded(id)}`, payload)
export const deleteQuestion = (id) => del(`/admin/tutorial-questions/${encoded(id)}`)
export const reorderQuestions = (chapterId, ids) => put(`/admin/tutorial-chapters/${encoded(chapterId)}/questions/order`, { ids })

export const listPublicCategories = () => get('/public/tutorial-categories/tree')
export const listPublicTutorials = (params) => get('/public/tutorials', params)
export const getPublicTutorial = (slug) => get(`/public/tutorials/${encoded(slug)}`)
export const getPublicChapter = (tutorialSlug, chapterSlug) =>
  get(`/public/tutorials/${encoded(tutorialSlug)}/chapters/${encoded(chapterSlug)}`)
export const getPublicQuestionAnswer = (tutorialSlug, chapterSlug, questionId) =>
  get(`/public/tutorials/${encoded(tutorialSlug)}/chapters/${encoded(chapterSlug)}/questions/${encoded(questionId)}/answer`)
