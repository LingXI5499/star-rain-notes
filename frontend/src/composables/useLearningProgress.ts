import { computed } from 'vue'
import { accountLearning, guestLearning, type LearningProgressRepository } from '@/lib/learning-storage'

/**
 * 根据登录状态选择学习进度仓库：未登录用游客 localStorage 仓库；
 * 登录后使用账号型仓库（API 后端），并在本机存在游客数据时仅提示一次导入。
 */
export function useLearningProgress(isAuthenticated: boolean) {
  const repository = computed<LearningProgressRepository>(() =>
    isAuthenticated ? accountLearning : guestLearning,
  )
  return { repository }
}
