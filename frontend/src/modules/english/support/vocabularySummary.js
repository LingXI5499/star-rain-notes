/**
 * vocabularySummary.js：英语学习模块的业务支持工具。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：../../../shared/http。
 * 阅读入口：loadVocabularySummary（加载词汇摘要）。
 */

import { get } from '../../../shared/http'

/*
 * 英语首页/词库页的四格学习统计。
 *
 * 与 V1 的 `fetchLearningSummary()` 同口径：completed / inProgress / dueForReview / total。
 * 这里直接调账号树的学习汇总接口，而不是走 `englishApi` 的封装，
 * 是为了让英语首页在**接口尚未就绪或未登录**时也能正常渲染：
 * 401/404/网络错误一律降级成全 0，页面只显示「暂无记录」，不弹错误、不白屏。
 * 学习数据本身在 /english/vocabulary/progress 里有完整展示，首页这里只是概览。
 */
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
