/**
 * learningLabels.js：教程与学习模块的业务支持工具。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 */

export const masteryLabels = { UNLEARNED: '未学习', LEARNING: '学习中', BASIC_MASTERED: '基本掌握', STABLE_MASTERED: '稳定掌握' }
export const planLabels = { DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', CANCELLED: '已取消' }
export const taskLabels = { PENDING: '待学习', IN_PROGRESS: '学习中', COMPLETED: '已完成', SKIPPED: '已跳过' }
export const sessionLabels = { INITIAL_STUDY: '首次学习', REVIEW: '知识复习', STABLE_AUDIT: '稳定知识抽查' }
export const sessionStatusLabels = { IN_PROGRESS: '进行中', COMPLETED: '已完成', ABANDONED: '已放弃' }
