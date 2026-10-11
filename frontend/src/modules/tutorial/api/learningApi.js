/**
 * learningApi.js：教程与学习模块的接口封装。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 * 主要依赖：../../../shared/http。
 * 阅读入口：getChapterProgress（读取章节进度）；saveChapterProgress（保存章节进度）；completeChapter（完成章节）；getTutorialProgress（读取教程进度）；getRecentLearning（读取Recent学习）。
 */

import { get, post, put, patch } from '../../../shared/http'
const id = encodeURIComponent
const base = '/account/learning'
/**
 * 读取章节进度。
 * 请求入口：get(`${base}/progress/chapters/${id(chapterId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param chapterId 章节标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getChapterProgress = (chapterId) => get(`${base}/progress/chapters/${id(chapterId)}`)
/**
 * 保存章节进度。
 * 请求入口：put(`${base}/progress/chapters/${id(chapterId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param chapterId 章节标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveChapterProgress = (chapterId, payload) => put(`${base}/progress/chapters/${id(chapterId)}`, payload)
/**
 * 完成章节。
 * 请求入口：post(`${base}/progress/chapters/${id(chapterId)}/complete`)；路径参数的编码和请求体结构保持接口契约。
 * @param chapterId 章节标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const completeChapter = (chapterId) => post(`${base}/progress/chapters/${id(chapterId)}/complete`)
/**
 * 读取教程进度。
 * 请求入口：get(`${base}/progress/tutorials/${id(tutorialId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param tutorialId 教程标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getTutorialProgress = (tutorialId) => get(`${base}/progress/tutorials/${id(tutorialId)}`)
/**
 * 读取Recent学习。
 * 请求入口：get(`${base}/recent`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getRecentLearning = () => get(`${base}/recent`)
/**
 * 读取Own答案。
 * 请求入口：get(`${base}/questions/${id(questionId)}/answer`)；路径参数的编码和请求体结构保持接口契约。
 * @param questionId 问题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getOwnAnswer = (questionId) => get(`${base}/questions/${id(questionId)}/answer`)
/**
 * 保存Own答案。
 * 请求入口：post(`${base}/questions/${id(questionId)}/versions`)；路径参数的编码和请求体结构保持接口契约。
 * @param questionId 问题标识；默认值、解构字段与可选参数见函数签名。
 * @param answerText 用户提交的回答文本；默认值、解构字段与可选参数见函数签名。
 * @param answerPhase = 'BEFORE_REFERENCE' 答案Phase；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const saveOwnAnswer = (questionId, answerText, answerPhase = 'BEFORE_REFERENCE') => post(`${base}/questions/${id(questionId)}/versions`, { answerText, answerPhase })
/**
 * 读取答案Versions。
 * 请求入口：get(`${base}/questions/${id(questionId)}/versions`)；路径参数的编码和请求体结构保持接口契约。
 * @param questionId 问题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getAnswerVersions = (questionId) => get(`${base}/questions/${id(questionId)}/versions`)
/**
 * 读取Own引用答案。
 * 请求入口：get(`${base}/questions/${id(questionId)}/reference-answer`)；路径参数的编码和请求体结构保持接口契约。
 * @param questionId 问题标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getOwnReferenceAnswer = (questionId) => get(`${base}/questions/${id(questionId)}/reference-answer`)
/**
 * 列出学习Plans。
 * 请求入口：get(`${base}/plans`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listStudyPlans = () => get(`${base}/plans`, { currentOnly: true })
/**
 * 读取学习计划。
 * 请求入口：get(`${base}/plans/${id(planId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getStudyPlan = (planId) => get(`${base}/plans/${id(planId)}`)
/**
 * 预览学习计划。
 * 请求入口：post(`${base}/plans/preview${planId ? `?excludePlanId=${id(planId)}` : ''}`)；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const previewStudyPlan = (payload, planId) => post(`${base}/plans/preview${planId ? `?excludePlanId=${id(planId)}` : ''}`, payload)
/**
 * 创建学习计划。
 * 请求入口：post(`${base}/plans`)；路径参数的编码和请求体结构保持接口契约。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createStudyPlan = (payload) => post(`${base}/plans`, payload)
/**
 * 更新学习计划。
 * 请求入口：patch(`${base}/plans/${id(planId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @param payload 本次提交的数据；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const updateStudyPlan = (planId, payload) => patch(`${base}/plans/${id(planId)}`, payload)
/**
 * transition学习计划的处理函数。
 * 请求入口：post(`${base}/plans/${id(planId)}/${action}`)；路径参数的编码和请求体结构保持接口契约。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const transitionStudyPlan = (planId, action) => post(`${base}/plans/${id(planId)}/${action}`)
/**
 * 列出计划Tasks。
 * 请求入口：get(`${base}/plans/${id(planId)}/tasks`)；路径参数的编码和请求体结构保持接口契约。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listPlanTasks = (planId) => get(`${base}/plans/${id(planId)}/tasks`)
/**
 * 读取学习任务。
 * 请求入口：get(`${base}/tasks/${id(taskId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param taskId 任务标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getStudyTask = (taskId) => get(`${base}/tasks/${id(taskId)}`)
/**
 * 开始学习任务的处理函数。
 * 请求入口：post(`${base}/tasks/${id(taskId)}/start`)；路径参数的编码和请求体结构保持接口契约。
 * @param taskId 任务标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const startStudyTask = (taskId) => post(`${base}/tasks/${id(taskId)}/start`)
/**
 * 读取学习状态。
 * 请求入口：get(`${base}/chapters/${id(chapterId)}/study-state`)；路径参数的编码和请求体结构保持接口契约。
 * @param chapterId 章节标识；默认值、解构字段与可选参数见函数签名。
 * @param planId 计划标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getStudyState = (chapterId, planId) => get(`${base}/chapters/${id(chapterId)}/study-state`, planId ? { planId } : undefined)
/**
 * 开始Initial会话的处理函数。
 * 请求入口：post(`${base}/chapters/${id(chapterId)}/sessions${taskId ? `?taskId=${id(taskId)}` : ''}`)；路径参数的编码和请求体结构保持接口契约。
 * @param chapterId 章节标识；默认值、解构字段与可选参数见函数签名。
 * @param taskId 任务标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const startInitialSession = (chapterId, taskId) => post(`${base}/chapters/${id(chapterId)}/sessions${taskId ? `?taskId=${id(taskId)}` : ''}`)
/**
 * 读取学习会话。
 * 请求入口：get(`${base}/sessions/${id(sessionId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param sessionId 会话标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getLearningSession = (sessionId) => get(`${base}/sessions/${id(sessionId)}`)
/**
 * reveal卡片的处理函数。
 * 请求入口：post(`${base}/sessions/${id(sessionId)}/cards/${id(cardId)}/reveal`)；路径参数的编码和请求体结构保持接口契约。
 * @param sessionId 会话标识；默认值、解构字段与可选参数见函数签名。
 * @param cardId 卡片标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const revealCard = (sessionId, cardId) => post(`${base}/sessions/${id(sessionId)}/cards/${id(cardId)}/reveal`)
/**
 * rate卡片的处理函数。
 * 请求入口：post(`${base}/sessions/${id(sessionId)}/cards/${id(cardId)}/rate`)；路径参数的编码和请求体结构保持接口契约。
 * @param sessionId 会话标识；默认值、解构字段与可选参数见函数签名。
 * @param cardId 卡片标识；默认值、解构字段与可选参数见函数签名。
 * @param rating 评分；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const rateCard = (sessionId, cardId, rating) => post(`${base}/sessions/${id(sessionId)}/cards/${id(cardId)}/rate`, { rating })
/**
 * 读取审核摘要。
 * 请求入口：get(`${base}/reviews/summary`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getReviewSummary = () => get(`${base}/reviews/summary`)
/**
 * 读取当前审核。
 * 请求入口：get(`${base}/reviews/sessions/current`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getCurrentReview = () => get(`${base}/reviews/sessions/current`)
/**
 * 创建审核会话。
 * 请求入口：post(`${base}/reviews/sessions`)；路径参数的编码和请求体结构保持接口契约。
 * @param count 数量；默认值、解构字段与可选参数见函数签名。
 * @param mode = 'RECOMMENDED' mode；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const createReviewSession = (count, mode = 'RECOMMENDED') => post(`${base}/reviews/sessions`, { count, mode })
/**
 * 读取审核会话。
 * 请求入口：get(`${base}/reviews/sessions/${id(sessionId)}`)；路径参数的编码和请求体结构保持接口契约。
 * @param sessionId 会话标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getReviewSession = (sessionId) => get(`${base}/reviews/sessions/${id(sessionId)}`)
/**
 * abandon审核会话的处理函数。
 * 请求入口：post(`${base}/reviews/sessions/${id(sessionId)}/abandon`)；路径参数的编码和请求体结构保持接口契约。
 * @param sessionId 会话标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const abandonReviewSession = (sessionId) => post(`${base}/reviews/sessions/${id(sessionId)}/abandon`)
/**
 * 列出掌握。
 * 请求入口：get(`${base}/mastery`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const listMastery = () => get(`${base}/mastery`)
/**
 * 读取学习历史。
 * 请求入口：get(`${base}/history`)；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getLearningHistory = (params) => get(`${base}/history`, params)
/**
 * 读取学习统计信息。
 * 请求入口：get(`${base}/statistics`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getLearningStatistics = () => get(`${base}/statistics`)

/**
 * 读取掌握分页。
 * 请求入口：get(`${base}/mastery/page`)；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getMasteryPage = (params) => get(`${base}/mastery/page`, params)
/**
 * 读取掌握选项。
 * 请求入口：get(`${base}/mastery/options`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getMasteryOptions = () => get(`${base}/mastery/options`)
/**
 * 读取历史选项。
 * 请求入口：get(`${base}/history/options`)；路径参数的编码和请求体结构保持接口契约。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getHistoryOptions = () => get(`${base}/history/options`)
/**
 * 读取计划历史。
 * 请求入口：get(`${base}/plans/history`)；路径参数的编码和请求体结构保持接口契约。
 * @param params 查询参数；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 请求完成后的业务数据；接口失败沿共享 HTTP 客户端的错误路径传递。
 */
export const getPlanHistory = (params) => get(`${base}/plans/history`, params)
