import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { bindApp } from './plugins/app-context'
import { useThemeStore } from './stores/theme'

import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App)

// Heavy admin UI components are registered lazily on the first route that
// needs them. Only this tiny app reference bridge is part of the public shell.
bindApp(app)
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
