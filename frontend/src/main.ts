import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { bindElementPlus } from './plugins/element-plus'
import { useThemeStore } from './stores/theme'

import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App)

// Element Plus is registered lazily (TASK-011) so the Admin bundle is not
// part of the public initial load; see plugins/element-plus.ts.
bindElementPlus(app)
app.use(createPinia())
app.use(router)

// Apply the persisted theme before first paint and keep System mode reactive.
useThemeStore().apply()
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  const theme = useThemeStore()
  if (theme.mode === 'system') {
    theme.apply()
  }
})

app.mount('#app')
