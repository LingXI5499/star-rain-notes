import { get, post, put, patch } from '../../../shared/http'

const id = (value) => encodeURIComponent(value)

export const getChapterProgress = (chapterId) => get(`/account/learning/progress/chapters/${id(chapterId)}`)
export const saveChapterProgress = (chapterId, payload) => put(`/account/learning/progress/chapters/${id(chapterId)}`, payload)
export const completeChapter = (chapterId) => post(`/account/learning/progress/chapters/${id(chapterId)}/complete`)
export const getTutorialProgress = (tutorialId) => get(`/account/learning/progress/tutorials/${id(tutorialId)}`)
export const getRecentLearning = () => get('/account/learning/recent')

export const getOwnAnswer = (questionId) => get(`/account/learning/questions/${id(questionId)}/answer`)
export const saveOwnAnswer = (questionId, answerText) => put(`/account/learning/questions/${id(questionId)}/answer`, { answerText })
export const getOwnReferenceAnswer = (questionId) => get(`/account/learning/questions/${id(questionId)}/reference-answer`)

export const listStudyPlans = () => get('/account/learning/plans')
export const getStudyPlan = (planId) => get(`/account/learning/plans/${id(planId)}`)
export const createStudyPlan = (payload) => post('/account/learning/plans', payload)
export const updateStudyPlan = (planId, payload) => patch(`/account/learning/plans/${id(planId)}`, payload)
export const transitionStudyPlan = (planId, action) => post(`/account/learning/plans/${id(planId)}/${action}`)
export const listPlanTasks = (planId) => get(`/account/learning/plans/${id(planId)}/tasks`)
export const listTodayStudyTasks = () => get('/account/learning/tasks/today')
export const startStudyTask = (taskId) => post(`/account/learning/tasks/${id(taskId)}/start`)
export const completeStudyTask = (taskId) => post(`/account/learning/tasks/${id(taskId)}/complete`)
export const skipStudyTask = (taskId) => post(`/account/learning/tasks/${id(taskId)}/skip`)

export const listTodayReviews = () => get('/account/learning/reviews/today')
export const getReviewTask = (taskId) => get(`/account/learning/reviews/${id(taskId)}`)
export const getReviewBack = (taskId) => get(`/account/learning/reviews/${id(taskId)}/back`)
export const completeReview = (taskId, rating) => post(`/account/learning/reviews/${id(taskId)}/complete`, { rating })
export const listMastery = () => get('/account/learning/mastery/cards')
export const setMasterySelfRating = (cardId, level) => put(`/account/learning/mastery/cards/${id(cardId)}/self-rating`, { level })

export const getLearningHistory = (params) => get('/account/learning/history', params)
export const getLearningStatistics = () => get('/account/learning/statistics')
