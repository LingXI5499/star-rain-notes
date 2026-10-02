import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import { useAuthStore } from './modules/account/stores/authStore'
import { accountPath } from './shared/viewMode'

const pinia = createPinia()
createApp(App).use(pinia).use(router).mount('#app')
window.addEventListener('account-session-expired', () => {
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

