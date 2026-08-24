import type { App } from 'vue'

let currentApp: App | null = null

export function bindApp(app: App): void {
  currentApp = app
}

export function requireApp(): App {
  if (!currentApp) {
    throw new Error('Vue app has not been bound yet')
  }
  return currentApp
}
