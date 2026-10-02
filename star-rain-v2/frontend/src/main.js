import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import { useAuthStore } from './modules/account/stores/authStore'

const pinia = createPinia()
createApp(App).use(pinia).use(router).mount('#app')
window.addEventListener('account-session-expired', () => {
  const auth = useAuthStore(pinia)
  auth.currentUser = null
  if (router.currentRoute.value.meta.requiresAuth) {
    router.replace({ path: '/login', query: { expired: '1', redirect: router.currentRoute.value.fullPath } })
  }
})

