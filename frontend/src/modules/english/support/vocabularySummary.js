/**
 * vocabularySummary.js：英语学习模块的业务支持工具。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：../../../shared/http。
 * 阅读入口：loadVocabularySummary（加载词汇摘要）。
 */

import { get } from '../../../shared/http'

// 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
const EMPTY = { completed: 0, inProgress: 0, dueForReview: 0, total: 0 }

export
  /**
   * 加载词汇摘要。
   * 请求入口：get('/account/english/vocabulary/summary')；路径参数的编码和请求体结构保持接口契约。
   * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
   */
  async function loadVocabularySummary() {
  try {
    const data = await get('/account/english/vocabulary/summary')
    return { ...EMPTY, ...(data || {}) }
  } catch {
    return { ...EMPTY }
  }
}
