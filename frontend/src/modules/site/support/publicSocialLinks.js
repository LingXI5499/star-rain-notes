/**
 * publicSocialLinks.js：站点模块的业务支持工具。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：vue、../../profile/api/profileApi。
 * 主要响应式状态：state。
 * 阅读入口：usePublicSocialLinks（use公开Social链接列表的处理函数）。
 */

import { reactive } from 'vue'
import { getPublicProfile } from '../../profile/api/profileApi'

/*
 * 公开站作者社交链接（页脚 CONNECT 区用）。
 *
 * 与 `siteBranding` 同一套缓存策略：整个会话只取一次公开作者资料，页脚在每一页都会挂载，
 * 不做单例缓存就会每次导航都多打一次 `/public/profile`。
 * 取不到时不重试、不报错，页脚回落到「公开链接待配置」。
 */
/**
 * 状态；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const state = reactive({ links: [] })
let pending

export
  /**
   * use公开Social链接列表的处理函数。
   * 主要协作：.catch、.then、getPublicProfile。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function usePublicSocialLinks() {
  if (!pending) {
    pending = getPublicProfile()
      .then((profile) => { state.links = profile?.socialLinks || [] })
      .catch(() => { pending = null })
  }
  return state
}
