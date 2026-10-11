/**
 * main.js：前端应用模块的应用配置与入口。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 创建 Pinia、路由和应用实例，并装配访问统计及 SEO 元信息更新。
 * 账号会话失效时清理用户状态；当前路由需要认证时跳转到带原始返回地址的登录页。
 * 主要依赖：vue、pinia、./App.vue、./router、./style.css、./styles/theme-transition.css。
 * 主要响应式状态：auth。
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import './styles/theme-transition.css'
import './styles/interactions.css'
import { useAuthStore } from './modules/account/stores/authStore'
import { accountPath } from './shared/viewMode'
import { installPublicTracking } from './modules/analytics/tracking/publicTracking'
import { installSeoMeta } from './modules/seo/seoMeta'

const pinia = createPinia()
installPublicTracking(router)
installSeoMeta(router)
createApp(App).use(pinia).use(router).mount('#app')
window.addEventListener('account-session-expired', () => {
  /**
   * 认证；通过 useAuthStore 建立。
   */
  const auth = useAuthStore(pinia)
  auth.currentUser = null
  /*
   * 会话失效只可能发生在需要登录态的路由（全部在账号树），因此这里直接把登录页写成
   * accountPath('/login')。若将来公开树也出现需要登录态的页面，这里要改成按 to.path 解析模式。
   */
  if (router.currentRoute.value.meta.requiresAuth) {
    router.replace({
      path: accountPath('/login'),
      query: { expired: '1', redirect: router.currentRoute.value.fullPath },
    })
  }
})

