import { defineStore } from 'pinia'
import { http, refreshCsrf } from '@/api/http'

export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

interface SessionView {
  authenticated: boolean
  username?: string | null
  role?: string | null
}

/**
 * V1 session authentication store (TASK-003):
 * fetchSession / login / logout / changePassword, all over the session cookie
 * + CSRF flow. 401 responses clear the store automatically (http interceptor).
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    status: 'unknown' as AuthStatus,
    username: '',
  }),
  getters: {
    isAuthenticated: (state) => state.status === 'authenticated',
  },
  actions: {
    async fetchSession() {
      try {
        const { data } = await http.get<SessionView>('/auth/session')
        if (data.authenticated) {
          this.status = 'authenticated'
          this.username = data.username ?? ''
        } else {
          this.reset()
        }
      } catch {
        this.reset()
      }
    },
    async fetchSetupRequired(): Promise<boolean> {
      const { data } = await http.get<{ setupRequired: boolean }>('/setup/status')
      return data.setupRequired
    },
    async login(username: string, password: string) {
      await http.post('/auth/login', { username, password })
      await refreshCsrf()
      await this.fetchSession()
    },
    async logout() {
      try {
        await http.post('/auth/logout')
      } finally {
        await refreshCsrf().catch(() => undefined)
        this.reset()
      }
    },
    async changePassword(currentPassword: string, newPassword: string) {
      await http.put('/auth/password', { currentPassword, newPassword })
      await refreshCsrf().catch(() => undefined)
      this.reset()
    },
    reset() {
      this.status = 'anonymous'
      this.username = ''
    },
  },
})
