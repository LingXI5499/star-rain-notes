import { reactive } from 'vue'
import { getPublicProfile } from '../../profile/api/profileApi'

/*
 * 公开站作者社交链接（页脚 CONNECT 区用）。
 *
 * 与 `siteBranding` 同一套缓存策略：整个会话只取一次公开作者资料，页脚在每一页都会挂载，
 * 不做单例缓存就会每次导航都多打一次 `/public/profile`。
 * 取不到时不重试、不报错，页脚回落到「公开链接待配置」。
 */
const state = reactive({ links: [] })
let pending

export function usePublicSocialLinks() {
  if (!pending) {
    pending = getPublicProfile()
      .then((profile) => { state.links = profile?.socialLinks || [] })
      .catch(() => { pending = null })
  }
  return state
}
