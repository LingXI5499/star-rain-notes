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

export async function loadVocabularySummary() {
  try {
    const data = await get('/account/english/vocabulary/summary')
    return { ...EMPTY, ...(data || {}) }
  } catch {
    return { ...EMPTY }
  }
}
